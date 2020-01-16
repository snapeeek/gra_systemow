import javax.net.ssl.SSLSocket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class Client
{
    static final String IP = "127.0.0.1";
    static final int PORT = 5005;

    public static void main(String[] args)
    {
        Socket socket;
        do
        {
            System.out.println("Trying to connect");
            socket = tryConnect();
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
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    static Socket tryConnect()
    {
        Socket sock = new Socket();
        SocketAddress socketAddress = new InetSocketAddress(IP, PORT);
        try
        {
            sock.connect(socketAddress);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return sock;
    }
}
