import Maze.Cell;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Bot extends Thread
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
            graphics.setTextArea("Wspolrzedne: (" + location.x +", " + location.y + ")\nCarried: " + carried + "\nDeaths: " + death);

            ScheduledExecutorService executorService = Executors.newScheduledThreadPool(50);

            DataOutputStream finalDos = dos;
            DataInputStream finalDis = dis;
            //final String[] lastDirection = {null};
            Runnable sendAndReceive = () ->
            {
                try
                {
                    ArrayList<String> moves = checkMoves();
                    SecureRandom random = new SecureRandom();
                    String msg;
                    msg = moves.get(random.nextInt(moves.size()));
                    //lastDirection[0] = msg;
                    System.out.println(msg);

                    finalDos.writeUTF(msg);
                    graphics.resetCom();
                    if (finalDis.readUTF().equals("mapa"))
                    {
                        Cell[][] temporary = (Cell[][]) ois.readUnshared();
                        cells = temporary.clone();
                    }
                    int x = finalDis.readInt();
                    int y = finalDis.readInt();
                    location.setLocation(x,y);
                    carried = finalDis.readInt();
                    death = finalDis.readInt();
                    graphics.setArray(cells);
                    graphics.repaintBoard();
                    graphics.setTextArea("Wspolrzedne: (" + location.x +", " + location.y + ")\nCarried: " + carried + "\nDeaths: " + death);
                    finalDos.flush();

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

    ArrayList<String> checkMoves()
    {
        ArrayList<String> possible = new ArrayList<>();
        if (location.x+1 >= 0 && cells[location.x-1][location.y].getType() != Cell.Type.WALL)
            possible.add("left");

        if (location.x+1 < 60 && cells[location.x+1][location.y].getType() != Cell.Type.WALL)
            possible.add("right");

        if (location.y - 1 >= 0 && cells[location.x][location.y-1].getType() != Cell.Type.WALL)
            possible.add("up");

        if (location.y + 1 < 30 && cells[location.x][location.y+1].getType() != Cell.Type.WALL)
            possible.add("down");

        return possible;
    }

    /*String getOpposite(String dir)
    {
        if (dir.equals("up"))
            return "down";
        else if (dir.equals("down"))
            return "up";
        else if (dir.equals("left"))
            return "right";
        else if (dir.equals("right"))
            return "right";
        else
            return "nothing";
    }*/
}

