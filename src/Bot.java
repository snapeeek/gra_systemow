import Maze.Cell;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import Maze.Cell;

//TODO obsługa klawiatury + wysyłanie komunikatów

public class Bot extends Thread
{
    final String IP = "127.0.0.1";
    final int PORT = 5005;
    final long period = 1500;
    Cell[][] cells;
    Point location;
    int death = 0, carried = 0;
    String komunikat;

    public static void main(String[] args)
    {
        var bot = new Bot();
        bot.start();
    }

    @Override
    public void run()
    {
        Socket socket;
        int counter = 1;
        do
        {
            System.out.println("Trying to connect. Tries - " + counter);
            socket = tryConnect();
            counter++;
        } while (!socket.isConnected());

        DataOutputStream dos = null;
        DataInputStream dis = null;

        try
        {
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        try
        {
            assert dos != null;
            dos.writeUTF("Komunikat");
            assert dis != null;
            System.out.println(dis.readUTF());

            location = new Point(dis.readInt(), dis.readInt());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            cells = (Cell[][]) ois.readObject();
            Graphics graphics = new Graphics("Bot", cells);
            graphics.setTextArea("hello from the other program");

            ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
            Socket finalSocket = socket;
            DataOutputStream finalDos = dos;
            DataInputStream finalDis = dis;
            Runnable sendAndReceive = () ->
            {
                try
                {
                    Random random = new Random();
                    int rand = random.nextInt(4);
                    String msg;
                    if (rand == 0)
                        msg = "up";
                    else if (rand == 1)
                        msg = "right";
                    else if (rand == 2)
                        msg = "down";
                    else msg = "left";
                    System.out.println(msg);
                    finalDos.writeUTF(msg);
                    graphics.resetCom();
                    if (finalDis.readUTF().equals("mapa"))
                    {
                        Cell[][] temporary = (Cell[][]) ois.readUnshared();
                        cells = temporary.clone();
                    }
                    graphics.setArray(cells);
                    graphics.repaintBoard();

                } catch (IOException | ClassNotFoundException e)
                {
                    e.printStackTrace();
                }

            };

            executorService.scheduleAtFixedRate(sendAndReceive, period, period, TimeUnit.MILLISECONDS);
        }
        catch (IOException | ClassNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    Socket tryConnect()
    {
        Socket sock = new Socket();
        SocketAddress socketAddress = new InetSocketAddress(IP, PORT);
        try
        {
            sock.connect(socketAddress);
        } catch (IOException e)
        {
            //e.printStackTrace();
        }
        return sock;
    }
}

