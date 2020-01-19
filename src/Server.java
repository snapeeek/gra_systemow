import Maze.Cell;
import Maze.MazeGenerator;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

//TODO dodanie obsługi dodawania bestii i pieniążków
//FIXME ogarnąć dodawanie do czterech socketów
//FIXME kolizje

public class Server
{
    ServerSocket serverSocket;
    static final int PORT = 5005;
    static final int interval = 1000; //1000ms = 1s
    public static final int CELL_WIDTH = 10;
    public static final int CELL_HEIGTH = 15;
    HashMap<Integer, Handler> players = new HashMap<>();
    HashMap<Integer, Handler> sockets = new HashMap<>();
    static Cell[][] cells;
    static Graphics graphics;
    static AtomicInteger playerCount = new AtomicInteger(0);
    static Semaphore cellsOps = new Semaphore(1);
    String com = "";

    Server()
    {
        MazeGenerator mazeGenerator = new MazeGenerator(60, 30);
        cells = mazeGenerator.getCells();

        System.out.println("Server is currently running.");
        try
        {
            graphics = new Graphics("Server", cells);
            serverSocket = new ServerSocket(PORT);
            Socket socket = serverSocket.accept();
            Handler handler = new Handler(socket, cellsOps);
            handler.start();

            Socket botsocket = serverSocket.accept();
            Handler handler1 = new Handler(botsocket, cellsOps);
            handler1.start();

        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        var Server = new Server();
    }

    class Handler extends Thread
    {
        final Socket socket;
        DataInputStream dis;
        DataOutputStream dos;
        Point location;
        Point start;
        int[] cords = {-2,-1,0,1,2};
        int playerNumber;
        Semaphore semaphore;
        int carried = 0, deaths = 0;
        boolean hasChanged = false;
        boolean inBushes = false;
        int bushesTime = 0;
        ScheduledExecutorService executorService = null;
        boolean ded = false;

        Handler(Socket socket, Semaphore sem)
        {
            this.socket = socket;
            this.semaphore = sem;
            playerNumber = playerCount.addAndGet(1);
            try
            {
                this.dis = new DataInputStream(socket.getInputStream());
                this.dos = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e)
            {
                e.printStackTrace();
            }

            location = searchForCords();
            start = new Point(location.x, location.y);
            players.put(playerNumber, this);
        }

        @Override
        public void run()
        {
            try
            {
                System.out.println(dis.readUTF());
                dos.writeUTF("Odebrano komunikat");

                dos.writeInt(location.x);
                dos.writeInt(location.y);

                Cell[][] toSend = generateChunk(location);
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeObject(toSend);


                executorService = Executors.newScheduledThreadPool(50);
                ScheduledExecutorService finalExecutorService = executorService;
                Runnable sendAndReceive = () ->
                {
                    hasChanged = false;
                    String msg;
                    try
                    {


                        msg = dis.readUTF();
                        if (ded)
                            death();
                        else
                            playerAction(msg);


                        if (hasChanged)
                        {
                            dos.writeUTF("mapa");

                            Cell[][] sending = generateChunk(location);

                            //System.out.println("Wysylam mape");
                            oos.reset();
                            oos.writeObject(sending.clone());

                        } else
                        {
                            dos.writeUTF("nie");
                        }
                        hasChanged = false;

                        dos.writeInt(location.x);
                        dos.writeInt(location.y);

                        dos.writeInt(carried);
                        dos.writeInt(deaths);
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                        finalExecutorService.shutdown();
                        graphics.dispatchEvent(new WindowEvent(graphics, WindowEvent.WINDOW_CLOSING));
                    }
                };
                executorService.scheduleAtFixedRate(sendAndReceive, interval, interval, TimeUnit.MILLISECONDS);

            }
            catch (IOException e)
            {
                e.printStackTrace();
                executorService.shutdown();
                graphics.dispatchEvent(new WindowEvent(graphics, WindowEvent.WINDOW_CLOSING));
            }
        }

        Point searchForCords()
        {
            Random random = new Random();
            int x, y;
            semaphore.tryAcquire();
            do
            {
                x = random.nextInt(60);
                y = random.nextInt(30);

            } while (cells[x][y].getType() != Cell.Type.PATH);
            cells[x][y].setOcup(Cell.Ocup.PLAYER);
            cells[x][y].setPlayerNum(playerNumber);

            semaphore.release();
            return new Point(x,y);
        }

        Cell[][] generateChunk(Point location)
        {
            semaphore.tryAcquire();
            Cell[][] toSend = new Cell[60][30];
            for (int i = 0; i < 60; i++)
            {
                for (int j = 0; j < 30; j++)
                {
                    toSend[i][j] = new Cell(Cell.Type.UNSEEN, Cell.Ocup.NOTHING, i * CELL_WIDTH, j * CELL_HEIGTH);
                }
            }

            for (int i : cords)
            {
                for (int j : cords)
                {
                    if (location.x + i >= 0 && location.x + i < 60 && location.y + j >= 0 && location.y + j < 30)
                    {
                        toSend[location.x + i][location.y + j] = cells[location.x + i][location.y + j];
                    }
                }
            }
            semaphore.release();
            return toSend;
        }

        void playerAction(String message)
        {
            if (inBushes && bushesTime == 0)
                bushesTime++;
            else
            {
                switch (message)
                {
                    case "up":
                        if (location.y - 1 >= 0 && cells[location.x][location.y - 1].getType() == Cell.Type.PATH || cells[location.x][location.y - 1].getType() == Cell.Type.BUSHES && cells[location.x][location.y - 1].getOcup() == Cell.Ocup.NOTHING)
                        {
                            if (cells[location.x][location.y-1].getOcup() == Cell.Ocup.PLAYER)
                            {
                                Handler help = players.get(cells[location.x][location.y-1].getPlayerNum());
                                help.ded = true;
                                death();
                                hasChanged = true;
                            }
                            else
                            {
                                semaphore.tryAcquire();
                                cells[location.x][location.y].setOcup(Cell.Ocup.NOTHING);
                                if (cells[location.x][location.y - 1].getOcup() == Cell.Ocup.COIN)
                                    carried++;
                                else if (cells[location.x][location.y - 1].getOcup() == Cell.Ocup.TREAS)
                                    carried += 10;
                                else if (cells[location.x][location.y - 1].getOcup() == Cell.Ocup.BIGT)
                                    carried += 50;
                                else if (cells[location.x][location.y - 1].getOcup() == Cell.Ocup.DEAD)
                                    carried += cells[location.x][location.y - 1].getCoins();

                                cells[location.x][location.y - 1].setOcup(Cell.Ocup.PLAYER);
                                cells[location.x][location.y - 1].setPlayerNum(playerNumber);

                                if (cells[location.x][location.y - 1].getType() == Cell.Type.BUSHES)
                                {
                                    inBushes = true;
                                    bushesTime = 0;
                                }
                                location.setLocation(location.x, location.y - 1);
                                hasChanged = true;
                                semaphore.release();
                            }
                        }

                        break;
                    case "right":

                        if (location.x + 1 < 60 && cells[location.x + 1][location.y].getType() == Cell.Type.PATH || cells[location.x + 1][location.y].getType() == Cell.Type.BUSHES)
                        {
                            if (cells[location.x + 1][location.y].getOcup() == Cell.Ocup.PLAYER)
                            {
                                Handler help = players.get(cells[location.x+1][location.y].getPlayerNum());
                                help.ded = true;
                                death();
                                hasChanged = true;
                            } else
                            {
                                semaphore.tryAcquire();
                                cells[location.x][location.y].setOcup(Cell.Ocup.NOTHING);
                                if (cells[location.x + 1][location.y].getOcup() == Cell.Ocup.COIN)
                                    carried++;
                                else if (cells[location.x + 1][location.y].getOcup() == Cell.Ocup.TREAS)
                                    carried += 10;
                                else if (cells[location.x + 1][location.y].getOcup() == Cell.Ocup.BIGT)
                                    carried += 50;
                                else if (cells[location.x+1][location.y].getOcup() == Cell.Ocup.DEAD)
                                    carried += cells[location.x][location.y - 1].getCoins();

                                cells[location.x + 1][location.y].setOcup(Cell.Ocup.PLAYER);
                                cells[location.x + 1][location.y].setPlayerNum(playerNumber);

                                if (cells[location.x + 1][location.y].getType() == Cell.Type.BUSHES)
                                {
                                    inBushes = true;
                                    bushesTime = 0;
                                }
                                location.setLocation(location.x + 1, location.y);
                                hasChanged = true;
                                semaphore.release();
                            }
                        }

                        break;
                    case "down":

                        if (location.y + 1 < 30 && cells[location.x][location.y + 1].getType() == Cell.Type.PATH || cells[location.x][location.y + 1].getType() == Cell.Type.BUSHES)
                        {
                            if (cells[location.x][location.y+1].getOcup() == Cell.Ocup.PLAYER)
                            {
                                Handler help = players.get(cells[location.x][location.y+1].getPlayerNum());
                                help.ded = true;
                                death();
                                hasChanged = true;
                            } else
                            {
                                semaphore.tryAcquire();
                                cells[location.x][location.y].setOcup(Cell.Ocup.NOTHING);
                                if (cells[location.x][location.y + 1].getOcup() == Cell.Ocup.COIN)
                                    carried++;
                                else if (cells[location.x][location.y + 1].getOcup() == Cell.Ocup.TREAS)
                                    carried += 10;
                                else if (cells[location.x][location.y + 1].getOcup() == Cell.Ocup.BIGT)
                                    carried += 50;
                                else if (cells[location.x][location.y + 1].getOcup() == Cell.Ocup.DEAD)
                                    carried += cells[location.x][location.y + 1].getCoins();

                                cells[location.x][location.y + 1].setOcup(Cell.Ocup.PLAYER);
                                cells[location.x][location.y + 1].setPlayerNum(playerNumber);

                                if (cells[location.x][location.y + 1].getType() == Cell.Type.BUSHES)
                                {
                                    inBushes = true;
                                    bushesTime = 0;
                                }
                                location.setLocation(location.x, location.y + 1);
                                hasChanged = true;
                                semaphore.release();
                            }
                        }

                        break;
                    case "left":

                        if (location.x - 1 >= 0 && cells[location.x - 1][location.y].getType() == Cell.Type.PATH || cells[location.x - 1][location.y].getType() == Cell.Type.BUSHES )
                        {
                            if (cells[location.x - 1][location.y].getOcup() == Cell.Ocup.PLAYER)
                            {
                                Handler help = players.get(cells[location.x-1][location.y].getPlayerNum());
                                help.ded = true;
                                death();
                                hasChanged = true;
                            }
                            else
                            {
                                semaphore.tryAcquire();
                                cells[location.x][location.y].setOcup(Cell.Ocup.NOTHING);
                                if (cells[location.x - 1][location.y].getOcup() == Cell.Ocup.COIN)
                                    carried++;
                                else if (cells[location.x - 1][location.y].getOcup() == Cell.Ocup.TREAS)
                                    carried += 10;
                                else if (cells[location.x - 1][location.y].getOcup() == Cell.Ocup.BIGT)
                                    carried += 50;
                                else if (cells[location.x-1][location.y].getOcup() == Cell.Ocup.DEAD)
                                    carried += cells[location.x-1][location.y].getCoins();

                                cells[location.x - 1][location.y].setOcup(Cell.Ocup.PLAYER);
                                cells[location.x - 1][location.y].setPlayerNum(playerNumber);

                                if (cells[location.x - 1][location.y].getType() == Cell.Type.BUSHES)
                                {
                                    inBushes = true;
                                    bushesTime = 0;
                                }
                                location.setLocation(location.x - 1, location.y);
                                hasChanged = true;
                                semaphore.release();
                            }
                        }

                        break;
                }
                if (bushesTime == 1)
                {
                    inBushes = false;
                    bushesTime = 0;
                }
                graphics.setArray(cells);
                graphics.repaintBoard();
            }
        }

        private void death()
        {
            deaths++;

            semaphore.tryAcquire();

            cells[location.x][location.y].setOcup(Cell.Ocup.DEAD);
            cells[location.x][location.y].setCoins(carried);
            carried = 0;
            location.setLocation(start.x, start.y);
            cells[location.x][location.y].setOcup(Cell.Ocup.PLAYER);
            cells[location.x][location.y].setPlayerNum(playerNumber);
            semaphore.release();
            ded = false;
        }


    }
}
