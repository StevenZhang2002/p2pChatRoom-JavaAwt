import net.sf.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;

public class Server extends JFrame {
    //List of online users
    ArrayList<User> clientList = new ArrayList<User>();
    //Info Display
    private JTextArea InfoDisplay = new JTextArea();
    //Server Socket input
    private JTextField ServerSocketInput = new JTextField();
    private final String Port = null;
    //user object(name,port,socket)
    private User user = null;
    //output stream from server
    DataOutputStream output = null;
    //input stream from server
    DataInputStream input = null;

    private User_port userPort;

    //a list that records user's port (userName,Port)
    ArrayList<User_port>PortList=new ArrayList<>();

    public Server() {
        setLayout(new BorderLayout());
        add(new JScrollPane(InfoDisplay), BorderLayout.CENTER);
        InfoDisplay.setEditable(false);
        InfoDisplay.setFont(new Font("", 0, 18));
        ServerSocketInput.setFont(new Font("", 0, 17));
        InfoDisplay.setBackground(Color.BLACK);
        InfoDisplay.setForeground(Color.GREEN);
        ServerSocketInput.setText("5000");
        final JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        JLabel jLabel = new JLabel("Enter the starting port,press ENTER to confirm");
        jLabel.setFont(new Font("", 0, 15));
        p.add(jLabel, BorderLayout.WEST);
        p.add(ServerSocketInput, BorderLayout.CENTER);
        ServerSocketInput.setHorizontalAlignment(JTextField.LEFT);
        ServerSocketInput.addActionListener(new PortListener());
        add(p, BorderLayout.SOUTH);
        setTitle("Server");
        setSize(700, 400);
        setLocation(200, 300);
        setVisible(true); //
    }

    private class PortListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            Thread Listener = new Thread(new ServerStart());
            Listener.start();
            ServerSocketInput.setEnabled(false);
        }
    }

    private class ServerStart extends Thread {
        @Override
        public void run() {
            try {
                ServerSocket serverSocket = new ServerSocket(Integer.parseInt(ServerSocketInput.getText()));
                InfoDisplay.append("Server startup time: " + new Date() + "\n\n");
                while (true) {
                    Socket socket = serverSocket.accept();
                    if (socket != null) {
                        input = new DataInputStream(socket.getInputStream());
                        String json = input.readUTF();
                        JSONObject data = JSONObject.fromObject(json.toString());
                        InfoDisplay.append("Client: " + data.getString("User") + "has logged in at:" + new Date());
                        InetAddress inetAddress = socket.getInetAddress();
                        InfoDisplay.append("His/Her IP address is" + inetAddress.getHostAddress() + "\n\n");
                        user = new User(data.getString("User"), data.getString("Port"), socket);
                        userPort=new User_port(data.getString("User"), data.getString("Port"));
                        clientList.add(user);
                        PortList.add(userPort);
                        System.out.println(clientList);
                        //usernamelist.add(data.getString("User"));
                    }
                    JSONObject online = new JSONObject();
                    online.put("Userlist", PortList);
                    online.put("Status", "Online");
                    online.put("Content", user.getName() + " logged in");
                    //inform all users of the new-comer
                    for (int i = 0; i < clientList.size(); i++) {
                        try {
                            User user = clientList.get(i);
                            output = new DataOutputStream(user.getSocket().getOutputStream());
                            output.writeUTF(online.toString());
                        } catch (IOException ex) {
                            System.err.println(ex);
                        }
                    }
                    Thread ServerController=new Thread(new ServerController(socket));
                    ServerController.start();
                }
            } catch (IOException ex) {
                System.err.println(ex);
            }
        }
    }

    class ServerController implements Runnable {
        Socket socket;

        ServerController(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                DataInputStream inputFromClient = new DataInputStream(socket.getInputStream());
                while (true) {
                    String data = inputFromClient.readUTF();
                    System.out.println(data);
                    //receive the new Data object
                    JSONObject Data = JSONObject.fromObject(data.toString());
                    //receive the msg
                    if (Data.get("Status").equals("Broadcast")) {
                        //broadcast message
                        JSONObject BroadMessage = new JSONObject();
                        //put the content
                        BroadMessage.put("User", Data.get("User"));
                        BroadMessage.put("Content", Data.get("User") + ":" + Data.get("Content"));
                        //set status
                        BroadMessage.put("Status", "Broadcast");
                        //iterate the clientList
                        for (int i = 0; i < clientList.size(); i++) {
                            try {
                                User user = clientList.get(i);
                                //Get each user socket, get the output stream
                                output = new DataOutputStream(user.getSocket().getOutputStream());
                                //Send data to each client
                                output.writeUTF(BroadMessage.toString());
                            } catch (IOException ex) {
                                System.err.println(ex);
                            }
                        }
                    } else if (Data.get("Status").equals("BroadcastStop")) {
                        //Stop msg
                        JSONObject BroadCastStop=new JSONObject();
                        //put content and status
                        BroadCastStop.put("Status","OfflineInform");
                        BroadCastStop.put("Content",Data.get("Content"));
                        BroadCastStop.put("StopPerson",Data.get("User"));
                        //remove the person who exit from the system
                        //iterate the list
                        for(int i=0;i<clientList.size();i++){
                            User user=clientList.get(i);
                            //remove the person with the matching name from the client list
                            if(user.getName().equals(Data.get("User"))){
                                clientList.remove(i);
                            }
                        }
                        for(int i=0;i<PortList.size();i++){
                            //remove the person with the matching name from the portlist
                            User_port user=PortList.get(i);
                            if(user.getName().equals(Data.get("User"))){
                                PortList.remove(i);
                            }
                        }
                        BroadCastStop.put("Userlist",PortList);
                        for (int i = 0; i < clientList.size(); i++) {
                            try {
                                User user = clientList.get(i);
                                //Get each user socket, get the output stream
                                output = new DataOutputStream(user.getSocket().getOutputStream());
                                //Send data to each client
                                output.writeUTF(BroadCastStop.toString());
                            } catch (IOException ex) {
                                System.err.println(ex);
                            }
                        }
                    } else if (Data.get("Status").equals("Kick")) {
                        //create new msg and put content, set status
                        JSONObject Message=new JSONObject();
                        Message.put("UserKick",Data.get("UserKick"));
                        Message.put("Status","BeingKick");
                        Message.put("Content",Data.get("UserKick")+" has been removed by "+Data.get("User"));
                        Message.put("ContentForKick",Data.get("User")+" has kicked you out of the chatting room,you will be removed in 5 seconds");
                        for(int i=0; i<clientList.size();i++){
                            //group send the kick msg
                            User user=clientList.get(i);
                            output=new DataOutputStream(user.socket.getOutputStream());
                            output.writeUTF(Message.toString());
                        }
                        for(int i=0;i<clientList.size();i++){
                            //remove the person from the clientList
                            if(clientList.get(i).getName().equals(Data.get("UserKick"))){
                                clientList.remove(i);
                            }
                        }
                        for(int i=0;i<PortList.size();i++){
                            //remove the person from the portList
                            if(PortList.get(i).getName().equals(Data.get("UserKick"))){
                                PortList.remove(i);
                            }
                        }
                    }
                    //find the status is the request
                    else if(Data.get("Status").equals("RequestSTATS")){
                        //create a new msg and put content
                        JSONObject Message=new JSONObject();
                        Message.put("User",Data.get("User"));
                        Message.put("Status","requestSTATSfromUser");
                        for(int i=0; i<clientList.size();i++){
                            //find the target person by iterating
                            User user=clientList.get(i);
                            if(user.getName().equals(Data.get("RequestID"))){
                                //find the target user and send the msg
                                output=new DataOutputStream(user.socket.getOutputStream());
                                output.writeUTF(Message.toString());
                                System.out.println("Message Content is"+Message.toString());
                            }
                        }

                    }
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }



    public static void main(String[] args) {
        Server server = new Server();
    }

}
