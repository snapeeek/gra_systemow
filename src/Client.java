import Maze.Cell;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;


//TODO obsługa klawiatury + wysyłanie komunikatów

public class Client extends Thread
{
    final String IP = "127.0.0.1";
    final int PORT = 5005;
    final long period = 1000;
    Cell[][] cells;
    Point location;
    int death = 0, carried = 0;
    String komunikat;

    public static void main(String[] args)
    {
        var client = new Client();
        client.start();
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
            cells = (Cell[][])ois.readObject();
            Graphics graphics = new Graphics("Player", cells);
            graphics.setTextArea("");

            ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
            Socket finalSocket = socket;
            DataOutputStream finalDos = dos;
            Runnable sendAndReceive = () ->
            {
                try
                {
                    String msg = graphics.getCom();
                    System.out.println(msg);
                    finalDos.writeUTF(msg);
                    graphics.resetCom();
                    System.out.println("Obieram mape");
                    cells = (Cell[][])ois.readObject();
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
