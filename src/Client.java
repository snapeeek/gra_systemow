import Maze.Cell;

import javax.net.ssl.SSLSocket;
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


//TODO obsługa klawiatury + wysyłanie komunikatów
//TODO odbieranie tablicy komórek
public class Client extends Thread implements KeyListener
{
    final String IP = "127.0.0.1";
    final int PORT = 5005;
    Cell[][] cells;
    Point location;

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
            Socket finalSocket = socket;
            Thread myThread =
                    new Thread(){
                        public void run()
                        {
                            try
                            {
                                ObjectInputStream ois = new ObjectInputStream(finalSocket.getInputStream());
                                cells = (Cell[][])ois.readObject();
                            } catch (IOException | ClassNotFoundException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    };
            myThread.run();
            try
            {
                myThread.join();
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            Graphics graphics = new Graphics("Player", cells);


        }
        catch (IOException e)
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

    @Override
    public void keyTyped(KeyEvent e)
    {

    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        if (e.getKeyCode() == KeyEvent.VK_UP)
            System.out.println(e.getKeyCode());

    }

    @Override
    public void keyReleased(KeyEvent e)
    {

    }
}
