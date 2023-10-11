import java.net.Socket;

public class User {
    String Name;

    String Port;
    Socket socket;

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public String getPort() {
        return Port;
    }

    public void setPort(String port) {
        Port = port;
    }

    public User(String name, String port, Socket socket) {
        Name = name;
        Port = port;
        this.socket = socket;
    }
}
