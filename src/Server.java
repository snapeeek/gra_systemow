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

    public static void main(String[] args)
    {
        cells = generatingCells();
        //System.out.println(cells[0].length);
        System.out.println("Server is currently running.");
        try
        {
            ServerSocket serverSocket = new ServerSocket(PORT);
            Socket socket = serverSocket.accept();
            Graphics graphics = new Graphics("Server", cells);
            Handler handler = new Handler(socket);
            handler.start();

        } catch (IOException e)
        {
            e.printStackTrace();
        }
        System.out.println(cells.length);
    }

    static Cell[][] generatingCells()
    {
        Cell[][] cells = new Cell[60][30];
        Random random = new Random();
        for (int i = 0; i < 60; i++)
        {
            for (int j = 0; j < 30; j++)
            {
                int temp = random.nextInt(3);
                if (temp == 0)
                    cells[i][j] = new Cell(Cell.Type.PATH, Cell.Ocup.NOTHING, i* CELL_WIDTH, j* CELL_HEIGTH);
                else if (temp == 1)
                    cells[i][j] = new Cell(Cell.Type.WALL, Cell.Ocup.NOTHING, i*CELL_WIDTH, j* CELL_HEIGTH);
                else
                    cells[i][j] = new Cell(Cell.Type.BUSHES, Cell.Ocup.NOTHING, i*CELL_WIDTH, j* CELL_HEIGTH);
            }
        }
        return cells;
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
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
