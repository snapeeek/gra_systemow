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
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Beast extends Thread
{
    final String IP = "127.0.0.1";
    final int PORT = 5005;
    final long period = 1000;
    Cell[][] cells = null;
    Point location;
    int hauntedPlayer = -1;
    ArrayList<String> nextCell = new ArrayList<>();
    ArrayList<String> possibleMoves;
    String komunikat;
    Graphics graphics;

    Beast()
    {
        graphics = new Graphics("beast", cells);
        graphics.setVisible(true);
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
            dos.writeUTF("beast");


            location = new Point(dis.readInt(), dis.readInt());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            cells = (Cell[][]) ois.readObject();

            ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);

            DataOutputStream finalDos = dos;
            DataInputStream finalDis = dis;
            Runnable sendAndReceive = () ->
            {
                try
                {
                    if (isPlayerVisible() && nextCell.isEmpty())
                    {
                        nextCell.clear();
                        pursuit(location.x, location.y, 0);
                    }

                    possibleMoves = checkMoves();

                    if (nextCell.size() > 0)
                    {
                        finalDos.writeUTF(nextCell.remove(nextCell.size()-1));
                    }
                    else
                    {
                        Random rand = new Random();
                        finalDos.writeUTF(possibleMoves.get(rand.nextInt(possibleMoves.size())));
                    }

                    if (finalDis.readUTF().equals("mapa"))
                    {
                        Cell[][] temporary = (Cell[][]) ois.readUnshared();
                        cells = temporary.clone();
                    }
                    int x = finalDis.readInt();
                    int y = finalDis.readInt();
                    location.setLocation(x,y);
                    graphics.setArray(cells);
                    graphics.repaintBoard();
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

    boolean isPlayerVisible()
    {
        int tab[] = {-2,-1,0,1,2};
        for (int i = -2; i <= 2; i++)
        {
            for (int j = -2; j <= 2; j++)
            {
                if (location.x + i >= 0 && location.x + i < 60 && location.y + j >= 0 && location.y + j < 30 && cells[location.x + i][location.y + j].getOcup() == Cell.Ocup.PLAYER)
                {
                    hauntedPlayer = cells[location.x + i][location.y + j].getPlayerNum();
                    return true;
                }
            }
        }
        hauntedPlayer = -1;
        return false;
    }

    boolean pursuit(int x, int y, int counter)
    {
        if (!(x >= 0 && x < 60 && y >= 0 && y < 30) || cells[x][y].getType() == Cell.Type.UNSEEN || cells[x][y].getType() == Cell.Type.WALL || counter > 5 || cells[x][y].isMarked())
        {
            cells[x][y].makeMarked();
            return false;
        }
        if (cells[x][y].getOcup() == Cell.Ocup.PLAYER && cells[x][y].getPlayerNum() == hauntedPlayer)
            return true;
        if (pursuit(x - 1, y,counter+ 1))
        {
            nextCell.add("left");
            return true;
        }
        if (pursuit(x, y - 1,counter + 1))
        {
            nextCell.add("up");
            return true;
        }
        if (pursuit(x + 1, y, counter + 1))
        {
            nextCell.add("right");
            return true;
        }
        if (pursuit(x, y + 1, counter + 1))
        {
            nextCell.add("down");
            return true;
        }
        cells[x][y].makeMarked();
        return false;
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

}

