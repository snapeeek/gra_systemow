import Maze.Cell;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;


//TODO obsługa klawiatury + wysyłanie komunikatów

public class Client extends Thread
{
    final String legend = "Legend: \n" + "1234 - players\n" + "c - 1 coin\n" + "t - 10 coins\n" + "T - 50 coins\n" + "* - beast\n" + "# - bushes (slowdown)";
    final String IP = "127.0.0.1";
    final int PORT = 5005;
    final long period = 1000;
    Cell[][] cells;
    Point location, start;
    int death = 0, carried = 0, brought = 0;
    ScheduledExecutorService executorService;

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
            assert dis != null;
            dos.writeUTF("player");


            location = new Point(dis.readInt(), dis.readInt());
            start = new Point(location.x, location.y);

            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            cells = (Cell[][]) ois.readObject();
            Graphics graphics = new Graphics("Player", cells);
            graphics.setTextArea("Wspolrzedne: (" + location.x +", " + location.y +")\nWspolrzedne startowe: ( " + start.x + ", " + start.y + ")\nCarried: " + carried
                    + "\nBrought: " + brought + "\nDeaths: " + death + "\n\n\n" + legend);

            executorService = Executors.newScheduledThreadPool(50);
            Socket finalSocket = socket;
            DataOutputStream finalDos = dos;
            DataInputStream finalDis = dis;
            Runnable sendAndReceive = () ->
            {
                try
                {
                    String msg = graphics.getCom();
                    System.out.println(msg);
                    finalDos.writeUTF(msg);
                    if (msg.equals("exit"))
                    {
                        graphics.dispatchEvent(new WindowEvent(graphics, WindowEvent.WINDOW_CLOSING));
                        executorService.shutdown();
                    }
                    graphics.resetCom();
                    //System.out.println("Obieram mape");
                    if (finalDis.readUTF().equals("mapa"))
                    {
                        Cell[][] temporary = (Cell[][]) ois.readUnshared();
                        cells = temporary.clone();
                    }
                    int x = finalDis.readInt();
                    int y = finalDis.readInt();
                    location.setLocation(x,y);
                    carried = finalDis.readInt();
                    brought = finalDis.readInt();
                    death = finalDis.readInt();
                    graphics.setArray(cells);
                    graphics.repaintBoard();
                    graphics.setTextArea("Wspolrzedne: (" + location.x +", " + location.y +")\nWspolrzedne startowe: ( " + start.x + ", " + start.y + ")\nCarried: " + carried
                            + "\nBrought: " + brought + "\nDeaths: " + death + "\n\n\n" + legend);

                } catch (IOException | ClassNotFoundException e)
                {
                    executorService.shutdown();
                    e.printStackTrace();
                }

            };

            executorService.scheduleAtFixedRate(sendAndReceive, period, period, TimeUnit.MILLISECONDS);
        }
        catch (IOException | ClassNotFoundException e)
        {
            executorService.shutdown();
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
