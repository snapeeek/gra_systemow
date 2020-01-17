import Maze.Cell;
import Maze.Maze;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

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

    public static void main(String[] args)
    {
        Maze maze = new Maze(60, 30);
        cells = maze.getCells();
        //System.out.println(cells[0].length);
        System.out.println("Server is currently running.");
        try
        {
            ServerSocket serverSocket = new ServerSocket(PORT);
            Socket socket = serverSocket.accept();
            graphics = new Graphics("Server", cells);
            Handler handler = new Handler(socket);
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

        Handler(Socket socket)
        {
            this.socket = socket;
            try
            {
                this.dis = new DataInputStream(socket.getInputStream());
                this.dos = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void run()
        {
            try
            {
                System.out.println(dis.readUTF());
                dos.writeUTF("Odebrano komunikat");
                Thread.sleep(5000);
            } catch (IOException | InterruptedException e)
            {
                e.printStackTrace();
            }


            /*cells = generatingCells();
            graphics.setArray(cells);
            graphics.repaintBoard();*/
        }
    }
}
