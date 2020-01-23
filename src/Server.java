import Maze.Cell;
import Maze.MazeGenerator;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

//TODO dodanie obsługi dodawania bestii

public class Server
{
    static ServerSocket serverSocket;
    static final int PORT = 5005;
    static final int interval = 1000; //1000ms = 1s
    public static final int CELL_WIDTH = 10;
    public static final int CELL_HEIGTH = 15;
    public static final int BOARD_WIDTH = 60;
    public static final int BOARD_HEIGHT = 30;
    HashMap<Integer, Handler> players = new HashMap<>();
    static Cell[][] cells;
    static Graphics graphics;
    static AtomicInteger playerCount = new AtomicInteger(0);
    static Semaphore cellsOps = new Semaphore(1);
    static Semaphore isPlayingOps = new Semaphore(1);
    boolean[] isPlaying = {false, false, false, false};
    static ScheduledExecutorService serverExec;
    static String com = "";

    Server()
    {
        MazeGenerator mazeGenerator = new MazeGenerator(BOARD_WIDTH, BOARD_HEIGHT);
        cells = mazeGenerator.getCells();

        System.out.println("Server is currently running.");
        try
        {
            graphics = new Graphics("Server", cells);
            serverSocket = new ServerSocket(PORT);
            System.out.println("Serwer czeka na dwóch graczy");
            Socket socket = serverSocket.accept();
            Handler handler = new Handler(socket, cellsOps, false);


            System.out.println("Serwer czeka na jeszcze jednego gracza");
            Socket botsocket = serverSocket.accept();
            Handler handler1 = new Handler(botsocket, cellsOps, false);


            handler.start();
            handler1.start();
            serverExec = Executors.newScheduledThreadPool(10);

            Runnable check = () ->
            {
                com = graphics.getCom();
                switch (com)
                {
                    case "coin":
                        addSth(Cell.Ocup.COIN);
                        break;
                    case "treas":
                        addSth(Cell.Ocup.TREAS);
                        break;
                    case "bigt":
                        addSth(Cell.Ocup.BIGT);
                        break;
                    case "beast":
                        addSth(Cell.Ocup.BEAST);
                        break;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("Gracz\tType\tCords\tStartPoint\tCarried\tBrought\tDeaths\n");
                for (int i = 1 ; i <= 4; i++)
                {
                    if (players.containsKey(i))
                    {
                        Handler help = players.get(i);
                        sb.append(help.playerNumber).append("\t").append(help.type).append("\t").append("(").append(help.location.x).append(",").append(help.location.y).append(")").append("\t").append("(" + help.start.x + "," + help.start.y + ")").append("\t").append(help.carried).append("\t").append(help.brought).append("\t").append(help.deaths);
                    }
                    else
                    {
                        sb.append(i).append("\t").append("---").append("\t").append("---").append("\t").append("---").append("\t").append("---").append("\t").append("---").append("\t").append("---");
                    }
                    sb.append("\n");
                }

                graphics.setTextArea(sb.toString());

                try
                {
                    if (players.size() < 4)
                    {
                        serverSocket.setSoTimeout(50);
                        Socket sock = serverSocket.accept();
                        if (sock != null)
                        {
                            Handler newHandler = new Handler(sock, cellsOps, false);
                            newHandler.start();
                        }
                    }
                } catch (SocketException e)
                {
                    System.out.println("Co jest kurwa");
                }catch (SocketTimeoutException e)
                {
                    System.out.println("well ye, noone connected");
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            };

            serverExec.scheduleAtFixedRate(check, interval, interval, TimeUnit.MILLISECONDS);


        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        var Server = new Server();
    }

    void addSth(Cell.Ocup ocup)
    {
        Random rand = new Random();
        int i, j;
        do
        {
            i = rand.nextInt(BOARD_WIDTH);
            j = rand.nextInt(BOARD_HEIGHT);
        } while (!(cells[i][j].getType() == Cell.Type.PATH && cells[i][j].getOcup() == Cell.Ocup.NOTHING));
        cellsOps.tryAcquire();
        if (ocup == Cell.Ocup.COIN)
            cells[i][j].setOcup(Cell.Ocup.COIN);
        else if (ocup == Cell.Ocup.TREAS)
            cells[i][j].setOcup(Cell.Ocup.TREAS);
        else if (ocup == Cell.Ocup.BIGT)
            cells[i][j].setOcup(Cell.Ocup.BIGT);
        else if (ocup == Cell.Ocup.BEAST)
        {
            var beast = new Beast();
            beast.start();
            Socket beastSocket = null;
            try
            {
                beastSocket = serverSocket.accept();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            Handler beastHandler = new Handler(beastSocket, cellsOps, true);
            beastHandler.start();
        }
        cellsOps.release();
        graphics.resetCom();
        graphics.repaintBoard();
    }

    class Handler extends Thread
    {
        final Socket socket;
        String type;
        DataInputStream dis;
        DataOutputStream dos;
        Point location;
        Point start;
        int[] cords = {-2,-1,0,1,2};
        int playerNumber;
        Semaphore semaphore;
        int carried = 0, deaths = 0, brought = 0;
        boolean hasChanged = false;
        boolean inBushes = false;
        int bushesTime = 0;
        ScheduledExecutorService executorService;
        boolean ded = false;
        Point deathLoc = null;

        Handler(Socket socket, Semaphore sem, boolean isBeast)
        {
            this.socket = socket;
            this.semaphore = sem;

            if (!isBeast)
            {
                isPlayingOps.tryAcquire();
                for (int i = 0; i < isPlaying.length; i++)
                {
                    if (!isPlaying[i])
                    {
                        playerNumber = i + 1;
                        isPlaying[i] = true;
                        break;
                    }
                }
                isPlayingOps.release();
                playerCount.addAndGet(1);
                players.put(playerNumber, this);
            }
            try
            {
                this.dis = new DataInputStream(socket.getInputStream());
                this.dos = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e)
            {
                e.printStackTrace();
            }

            if (isBeast)
                location = searchForCords(true);
            else
                location = searchForCords(false);
            start = new Point(location.x, location.y);

            executorService = Executors.newScheduledThreadPool(10);
        }

        @Override
        public void run()
        {
            try
            {
                type = dis.readUTF();

                dos.writeInt(location.x);
                dos.writeInt(location.y);

                Cell[][] toSend = generateChunk(location);
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeObject(toSend);

                ScheduledExecutorService finalExecutorService = executorService;

                Runnable sendAndReceive = () ->
                {
                    hasChanged = false;
                    String moveMsg;
                    try
                    {
                        moveMsg = dis.readUTF();
                        if (moveMsg.equals("exit") && !type.equals("beast"))
                        {
                            executorService.shutdown();
                            isPlayingOps.tryAcquire();
                            isPlaying[playerNumber - 1] = false;
                            isPlayingOps.release();
                            players.remove(playerNumber);
                            playerCount.addAndGet(-1);
                            if (playerCount.get() <= 0)
                            {
                                serverExec.shutdown();
                                System.out.println("Koniec gry");
                                graphics.dispatchEvent(new WindowEvent(graphics, WindowEvent.WINDOW_CLOSING));
                            }
                            cellsOps.tryAcquire();
                            cells[location.x][location.y].setOcup(Cell.Ocup.NOTHING);
                            cellsOps.release();
                        }
                        if (ded)
                        {
                            death();
                        }
                        else
                            playerAction(moveMsg);


                        if (hasChanged || ded || type.equals("player"))
                        {
                            ded = false;
                            dos.writeUTF("mapa");

                            Cell[][] sending = generateChunk(location);

                            oos.reset();
                            oos.writeObject(sending.clone());

                        } else
                        {
                            dos.writeUTF("nie");
                        }

                        hasChanged = false;

                        dos.writeInt(location.x);
                        dos.writeInt(location.y);

                        if (type.equals("player") || type.equals("bot"))
                        {
                            dos.writeInt(carried);
                            dos.writeInt(brought);
                            dos.writeInt(deaths);
                        }
                        //dis.reset();
                        dos.flush();
                    }
                    catch (EOFException e)
                    {
                        System.out.println(type);
                        e.printStackTrace();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                        System.out.println("FORCE EXIT. SERVBR IS SHUTTING DOWN");
                        finalExecutorService.shutdown();
                        serverExec.shutdown();
                        graphics.dispatchEvent(new WindowEvent(graphics, WindowEvent.WINDOW_CLOSING));
                    }
                };
                executorService.scheduleAtFixedRate(sendAndReceive, interval, interval, TimeUnit.MILLISECONDS);

            }
            catch (IOException e)
            {
                e.printStackTrace();
                System.out.println("FORCE EXIT. SERVER IS SHUTTING DOWN");
                executorService.shutdown();
                serverExec.shutdown();
                graphics.dispatchEvent(new WindowEvent(graphics, WindowEvent.WINDOW_CLOSING));
            }
        }

        Point searchForCords(boolean isBeast)
        {
            Random random = new Random();
            int x, y;
            semaphore.tryAcquire();
            do
            {
                x = random.nextInt(BOARD_WIDTH);
                y = random.nextInt(BOARD_HEIGHT);

            } while (cells[x][y].getType() != Cell.Type.PATH);
            if (!isBeast)
            {
                cells[x][y].setOcup(Cell.Ocup.PLAYER);
                cells[x][y].setPlayerNum(playerNumber);
            }
            else
                cells[x][y].setOcup(Cell.Ocup.BEAST);

            semaphore.release();
            return new Point(x,y);
        }

        Cell[][] generateChunk(Point location)
        {
            semaphore.tryAcquire();
            Cell[][] toSend = new Cell[BOARD_WIDTH][BOARD_HEIGHT];
            for (int i = 0; i < BOARD_WIDTH; i++)
            {
                for (int j = 0; j < BOARD_HEIGHT; j++)
                {
                    toSend[i][j] = new Cell(Cell.Type.UNSEEN, Cell.Ocup.NOTHING, i * CELL_WIDTH, j * CELL_HEIGTH);
                }
            }

            for (int i : cords)
            {
                for (int j : cords)
                {
                    if (location.x + i >= 0 && location.x + i < BOARD_WIDTH && location.y + j >= 0 && location.y + j < BOARD_HEIGHT)
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
                        if (location.y - 1 >= 0 && cells[location.x][location.y - 1].getType() == Cell.Type.PATH || cells[location.x][location.y - 1].getType() == Cell.Type.BUSHES)
                        {
                            if (cells[location.x][location.y-1].getOcup() == Cell.Ocup.PLAYER)
                            {
                                Handler help = players.get(cells[location.x][location.y-1].getPlayerNum());
                                help.deathLoc = (Point)location.clone();
                                help.ded = true;
                                if (type.equals("player") || type.equals("bot"))
                                    death();
                                hasChanged = true;
                            }
                            else
                            {
                                semaphore.tryAcquire();
                                cells[location.x][location.y].setOcup(Cell.Ocup.NOTHING);
                                if (type.equals("player") || type.equals("bot"))
                                {
                                    if (cells[location.x][location.y - 1].getOcup() == Cell.Ocup.COIN)
                                        carried++;
                                    else if (cells[location.x][location.y - 1].getOcup() == Cell.Ocup.TREAS)
                                        carried += 10;
                                    else if (cells[location.x][location.y - 1].getOcup() == Cell.Ocup.BIGT)
                                        carried += 50;
                                    else if (cells[location.x][location.y - 1].getOcup() == Cell.Ocup.DEAD)
                                    {
                                        carried += cells[location.x][location.y - 1].getCoins();
                                        cells[location.x][location.y - 1].setCoins(0);
                                    } else if (cells[location.x][location.y - 1].isCamp())
                                    {
                                        brought += carried;
                                        carried = 0;
                                    }
                                    else if (cells[location.x][location.y - 1].getOcup() == Cell.Ocup.BEAST)
                                    {
                                        death();
                                        break;
                                    }

                                    cells[location.x][location.y - 1].setOcup(Cell.Ocup.PLAYER);
                                    cells[location.x][location.y - 1].setPlayerNum(playerNumber);
                                }
                                else cells[location.x][location.y - 1].setOcup(Cell.Ocup.BEAST);

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

                        if (location.x + 1 < BOARD_WIDTH && cells[location.x + 1][location.y].getType() == Cell.Type.PATH || cells[location.x + 1][location.y].getType() == Cell.Type.BUSHES)
                        {
                            if (cells[location.x + 1][location.y].getOcup() == Cell.Ocup.PLAYER)
                            {
                                Handler help = players.get(cells[location.x+1][location.y].getPlayerNum());
                                help.deathLoc = (Point)location.clone();
                                help.ded = true;
                                if (type.equals("player") || type.equals("bot"))
                                    death();
                                hasChanged = true;
                            } else
                            {
                                semaphore.tryAcquire();
                                cells[location.x][location.y].setOcup(Cell.Ocup.NOTHING);
                                if (type.equals("player") || type.equals("bot"))
                                {
                                    if (cells[location.x + 1][location.y].getOcup() == Cell.Ocup.COIN)
                                        carried++;
                                    else if (cells[location.x + 1][location.y].getOcup() == Cell.Ocup.TREAS)
                                        carried += 10;
                                    else if (cells[location.x + 1][location.y].getOcup() == Cell.Ocup.BIGT)
                                        carried += 50;
                                    else if (cells[location.x + 1][location.y].getOcup() == Cell.Ocup.DEAD)
                                    {
                                        carried += cells[location.x + 1][location.y].getCoins();
                                        cells[location.x + 1][location.y].setCoins(0);
                                    } else if (cells[location.x + 1][location.y].isCamp())
                                    {
                                        brought += carried;
                                        carried = 0;
                                    }
                                    else if (cells[location.x + 1][location.y].getOcup() == Cell.Ocup.BEAST)
                                    {
                                        death();
                                        break;
                                    }

                                    cells[location.x + 1][location.y].setOcup(Cell.Ocup.PLAYER);
                                    cells[location.x + 1][location.y].setPlayerNum(playerNumber);
                                }
                                else cells[location.x + 1][location.y].setOcup(Cell.Ocup.BEAST);

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

                        if (location.y + 1 < BOARD_HEIGHT && cells[location.x][location.y + 1].getType() == Cell.Type.PATH || cells[location.x][location.y + 1].getType() == Cell.Type.BUSHES)
                        {
                            if (cells[location.x][location.y+1].getOcup() == Cell.Ocup.PLAYER)
                            {
                                Handler help = players.get(cells[location.x][location.y+1].getPlayerNum());
                                help.deathLoc = (Point)location.clone();
                                help.ded = true;
                                if (type.equals("player") || type.equals("bot"))
                                    death();
                                hasChanged = true;
                            } else
                            {
                                semaphore.tryAcquire();
                                cells[location.x][location.y].setOcup(Cell.Ocup.NOTHING);
                                if (type.equals("player") || type.equals("bot"))
                                {
                                    if (cells[location.x][location.y + 1].getOcup() == Cell.Ocup.COIN)
                                        carried++;
                                    else if (cells[location.x][location.y + 1].getOcup() == Cell.Ocup.TREAS)
                                        carried += 10;
                                    else if (cells[location.x][location.y + 1].getOcup() == Cell.Ocup.BIGT)
                                        carried += 50;
                                    else if (cells[location.x][location.y + 1].getOcup() == Cell.Ocup.DEAD)
                                    {
                                        carried += cells[location.x][location.y + 1].getCoins();
                                        cells[location.x][location.y + 1].setCoins(0);
                                    } else if (cells[location.x][location.y + 1].isCamp())
                                    {
                                        brought += carried;
                                        carried = 0;
                                    }
                                    else if (cells[location.x][location.y + 1].getOcup() == Cell.Ocup.BEAST)
                                    {
                                        death();
                                        break;
                                    }

                                    cells[location.x][location.y + 1].setOcup(Cell.Ocup.PLAYER);
                                    cells[location.x][location.y + 1].setPlayerNum(playerNumber);
                                }
                                else cells[location.x][location.y + 1].setOcup(Cell.Ocup.BEAST);


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
                                help.deathLoc = (Point)location.clone();
                                help.ded = true;
                                if (type.equals("player") || type.equals("bot"))
                                    death();
                                hasChanged = true;
                            }
                            else
                            {
                                semaphore.tryAcquire();
                                cells[location.x][location.y].setOcup(Cell.Ocup.NOTHING);
                                if (type.equals("player") || type.equals("bot"))
                                {
                                    if (cells[location.x - 1][location.y].getOcup() == Cell.Ocup.COIN)
                                        carried++;
                                    else if (cells[location.x - 1][location.y].getOcup() == Cell.Ocup.TREAS)
                                        carried += 10;
                                    else if (cells[location.x - 1][location.y].getOcup() == Cell.Ocup.BIGT)
                                        carried += 50;
                                    else if (cells[location.x - 1][location.y].getOcup() == Cell.Ocup.DEAD)
                                    {
                                        carried += cells[location.x - 1][location.y].getCoins();
                                        cells[location.x - 1][location.y].setCoins(0);
                                    } else if (cells[location.x - 1][location.y].isCamp())
                                    {
                                        brought += carried;
                                        carried = 0;
                                    }
                                    else if (cells[location.x - 1][location.y].getOcup() == Cell.Ocup.BEAST)
                                    {
                                        death();
                                        break;
                                    }

                                    cells[location.x - 1][location.y].setOcup(Cell.Ocup.PLAYER);
                                    cells[location.x - 1][location.y].setPlayerNum(playerNumber);
                                }
                                else cells[location.x - 1][location.y].setOcup(Cell.Ocup.BEAST);


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
            if (deathLoc != null)
            {
                cells[location.x][location.y].setOcup(Cell.Ocup.NOTHING);
                cells[deathLoc.x][deathLoc.y].setOcup(Cell.Ocup.DEAD);
                int temp = cells[deathLoc.x][deathLoc.y].getCoins();
                cells[deathLoc.x][deathLoc.y].setCoins(carried + temp);
                deathLoc = null;
            }
            else
            {

                cells[location.x][location.y].setOcup(Cell.Ocup.DEAD);
                int temp = cells[location.x][location.y].getCoins();
                cells[location.x][location.y].setCoins(carried + temp);
            }
            carried = 0;
            location.setLocation(start.x, start.y);
            cells[location.x][location.y].setOcup(Cell.Ocup.PLAYER);
            cells[location.x][location.y].setPlayerNum(playerNumber);
            semaphore.release();
        }
    }
}