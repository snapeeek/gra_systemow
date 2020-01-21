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
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Bot extends Thread
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
            assert dis != null;
            dos.writeUTF("bot");


            location = new Point(dis.readInt(), dis.readInt());
            start = new Point(location.x, location.y);
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            cells = (Cell[][]) ois.readObject();
            Graphics graphics = new Graphics("Bot", cells);
            graphics.setTextArea("Wspolrzedne: (" + location.x +", " + location.y +")\nWspolrzedne startowe: ( " + start.x + ", " + start.y + ")\nCarried: " + carried
                    + "\nBrought: " + brought + "\nDeaths: " + death + "\n\n\n" + legend);

            executorService = Executors.newScheduledThreadPool(50);

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

                    String graphMsg = graphics.getCom();

                    if (graphMsg.equals("exit"))
                    {
                        finalDos.writeUTF(graphMsg);
                        graphics.dispatchEvent(new WindowEvent(graphics, WindowEvent.WINDOW_CLOSING));
                        executorService.shutdown();
                    }
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
                    brought = finalDis.readInt();
                    death = finalDis.readInt();
                    graphics.setArray(cells);
                    graphics.repaintBoard();
                    graphics.setTextArea("Wspolrzedne: (" + location.x +", " + location.y +")\nWspolrzedne startowe: ( " + start.x + ", " + start.y + ")\nCarried: " + carried
                            + "\nBrought: " + brought + "\nDeaths: " + death + "\n\n\n" + legend);
                    finalDos.flush();

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

    ArrayList<String> checkMoves()
    {
        ArrayList<String> possible = new ArrayList<>();
        if (location.x - 1 >= 0 && cells[location.x-1][location.y].getType() != Cell.Type.WALL)
            possible.add("left");

        if (location.x + 1 < 60 && cells[location.x+1][location.y].getType() != Cell.Type.WALL)
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

