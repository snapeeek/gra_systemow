import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server
{
    static final int PORT = 5005;
    static final int interval = 1*1000; //1000ms = 1s

    public static void main(String[] args)
    {
        System.out.println("Server is currently running.");
        try
        {
            ServerSocket serverSocket = new ServerSocket(PORT);
            Socket socket = serverSocket.accept();
            Graphics graphics = new Graphics("Server");
            Handler handler = new Handler(socket);
            handler.start();

        } catch (IOException e)
        {
            e.printStackTrace();
        }
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
