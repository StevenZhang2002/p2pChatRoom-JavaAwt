public class User_port {
    String name;
    String socket;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSocket() {
        return socket;
    }

    public void setSocket(String socket) {
        this.socket = socket;
    }

    public User_port(String name, String socket) {
        this.name = name;
        this.socket = socket;
    }
}
