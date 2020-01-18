import Maze.Cell;
import Maze.MazeGenerator;

import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

//TODO dodanie obsługi klawiatury
//TODO całe zarządzanie socketami XDDDD

public class Server
{
    static final int PORT = 5005;
    static final int interval = 1000; //1000ms = 1s
    public static final int CELL_WIDTH = 10;
    public static final int CELL_HEIGTH = 15;
    static Cell[][] cells;
    static Graphics graphics;
    static Cell[][] fullUnseen = new Cell[60][30];
    static AtomicInteger playerCount = new AtomicInteger(0);
    static Semaphore cellsOps = new Semaphore(1);


    public static void main(String[] args)
    {
        MazeGenerator mazeGenerator = new MazeGenerator(60, 30);
        cells = mazeGenerator.getCells();

        for (int i = 0; i < 60; i++)
        {
            for (int j = 0; j < 30; j++)
            {
                fullUnseen[i][j] = new Cell(Cell.Type.UNSEEN, Cell.Ocup.NOTHING, i*CELL_WIDTH, j*CELL_HEIGTH);
            }
        }

        System.out.println("Server is currently running.");
        try
        {
            ServerSocket serverSocket = new ServerSocket(PORT);
            Socket socket = serverSocket.accept();
            graphics = new Graphics("Server", cells);
            Handler handler = new Handler(socket, cellsOps);
            handler.start();

        } catch (IOException e)
        {
            e.printStackTrace();
        }
        System.out.println(cells.length);
    }

    static class Handler extends Thread
    {
        final Socket socket;
        DataInputStream dis;
        DataOutputStream dos;
        Point location;
        int[] cords = {-2,-1,0,1,2};
        int playerNumber;
        Semaphore semaphore;

        Handler(Socket socket, Semaphore sem)
        {
            this.socket = socket;
            this.semaphore = sem;
            try
            {
                this.dis = new DataInputStream(socket.getInputStream());
                this.dos = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e)
            {
                e.printStackTrace();
            }

            location = searchForCords();
            playerNumber = playerCount.addAndGet(1);
        }

        @Override
        public void run()
        {
            try
            {
                System.out.println(dis.readUTF());
                dos.writeUTF("Odebrano komunikat");
                Thread.sleep(5000);
                dos.writeInt(location.x);
                dos.writeInt(location.y);

                Cell[][] toSend = generateChunk();
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeObject(toSend);
            } catch (IOException | InterruptedException e)
            {
                e.printStackTrace();
            }


            /*cells = generatingCells();
            graphics.setArray(cells);
            graphics.repaintBoard();*/
        }

         Point searchForCords()
        {
            Random random = new Random();
            int x, y;
            try
            {
                semaphore.acquire();
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
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

        Cell[][] generateChunk()
        {
            try
            {
                semaphore.acquire();
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            Cell[][] toSend = fullUnseen.clone();
            for (int i : cords)
            {
                for (int j : cords)
                {
                    if (location.x + i >= 0 && location.x + i < 60 && location.y + j >= 0 && location.y + j < 30)
                        toSend[location.x + i][location.y + j] = cells[location.x + i][location.y + j];
                }
            }
            semaphore.release();
            return toSend;
        }
    }
}
