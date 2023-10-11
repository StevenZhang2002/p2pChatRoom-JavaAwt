import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Client extends JFrame implements ActionListener {
    public static void main(String[] args) {
        Client f = new Client("Client");
        f.setLayout(new FlowLayout());

        f.setSize(600, 620);
        f.setResizable(false);
        f.setVisible(true);
    }

    Socket socket;

    ServerSocket ClientServer;
    JLabel NameHint, MsgHint, StatusHint, taHint, IPHint, PortHint, jtf3;
    JTextField UserNameInput;
    JTextField InputContent;
    static JTextField IPinput;
    JTextField PortInput;

    String ServerPort;
    static TextArea ta;
    InetAddress host;
    int port = 5000;
    Thread t = null;
    JButton SendMessageButton, ConnectButton, DisConnectButton;
    ArrayList<String> UserList = new ArrayList<>();

    DataInputStream inClient;
    DataOutputStream outClient;

    TextArea DisplayUsers;

    JScrollPane DisplayContainer;

    String UserName;

    Socket PrivateFromUser;
    DataInputStream InputP;
    DataOutputStream OutputP;

    JSONArray JsonArray_Json;

    ArrayList<String> CommandsRecords = new ArrayList<>();

    Client(String s) {
        super(s);

        myAdapter a = new myAdapter(this);
        addWindowListener(a);

        IPHint = new JLabel("Enter IP : ");
        add(IPHint);
        IPinput = new JTextField(15);
        add(IPinput);
        IPinput.setText("127.0.0.1");


        add(new JLabel("                   "));

        PortHint = new JLabel("Server Port : ");
        add(PortHint);
        PortInput = new JTextField(15);
        add(PortInput);
        PortInput.setText("5001");

        NameHint = new JLabel("User Name:");
        add(NameHint);
        UserNameInput = new JTextField(15);
        add(UserNameInput);

        ConnectButton = new JButton("Connect");
        add(ConnectButton);
        ConnectButton.addActionListener(this);

        add(new JLabel("                                                                    "));

        MsgHint = new JLabel("   Message : ");
        add(MsgHint);
        InputContent = new JTextField(34);
        add(new JScrollPane(InputContent));
        InputContent.setEditable(false);
        IPinput.setEditable(false);

        SendMessageButton = new JButton("Send Message");
        add(SendMessageButton);
        SendMessageButton.addActionListener(this);
        SendMessageButton.setEnabled(false);
        StatusHint = new JLabel("Status : ");
        add(StatusHint);
        jtf3 = new JLabel("Not connected to the server...\n");
        add(jtf3);
        DisplayUsers = new TextArea("", 10, 60);
        DisplayUsers.setBackground(Color.BLACK);
        DisplayUsers.setForeground(Color.GREEN);
        DisplayUsers.append("UserList");
        DisplayUsers.setEditable(false);
        //DisplayUsers.setBackground(Color.CYAN);
        add(new JScrollPane(DisplayUsers));
        taHint = new JLabel("Recieved Messages : ");
        add(taHint);
        ta = new TextArea("", 15, 60);
        add(new JScrollPane(ta));
        ta.setFont(Font.getFont("verdana"));
        ta.setBackground(Color.ORANGE);
        ta.setEditable(false);

        jtf3.setText("Not connected to Server, click connect");
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        try {
            String str = ae.getActionCommand();

            if (str.equals("Send Message")) {
                Thread MsgController = new Thread(new SendController());
                MsgController.start();
            }

            if (str.equals("Connect")) {
                try {
                    if (!UserNameInput.getText().equals("")) {
                        //once connect, initialize the serversocket and socket
                        UserName = UserNameInput.getText();
                        ServerPort = PortInput.getText();
                        InetAddress inetAddress = null;
                        try {
                            inetAddress = InetAddress.getLocalHost();
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                        try {
                            socket = new Socket("127.0.0.1", 5000);
                            System.out.println(socket);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        InputContent.setEditable(true);
                        SendMessageButton.setEnabled(true);
                        ConnectButton.setEnabled(false);
//                        DisConnectButton.setEnabled(true);
                        IPinput.setEditable(false);
                        PortInput.setEditable(false);
                        UserNameInput.setEditable(false);
                        //start the connection thread
                        Thread ConnectToServer = new Thread(new ConnectToServer());
                        ConnectToServer.start();
                        //start messageListener
                        Thread MessageListener = new Thread(new MessageListener());
                        MessageListener.start();
                        //start ClientServer
                        Thread ClientServer = new Thread(new ClientServer());
                        ClientServer.start();
                        jtf3.setText("Connect Successfully");
                        StatusHint.setText("              Status:");
                    }
                } catch (Exception e) {
                    jtf3.setText(" Could not connect to Server, connect again");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            jtf3.setText("Action Error");
        }
    }

    static class myAdapter extends WindowAdapter {
        Client f;

        public myAdapter(Client j) {
            f = j;
        }

        public void windowClosing(WindowEvent we) {
            f.setVisible(false);
            try {
                f.socket.close();
                f.dispose();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.exit(0);
        }
    }

    class ConnectToServer extends Thread {
        @Override
        public void run() {
            try {
                //initialize the input and output stream
                inClient = new DataInputStream(socket.getInputStream());
                outClient = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //create the msg and send to the server, informing the userName and user ID to the server
            JSONObject Online = new JSONObject();
            Online.put("User", UserName);
            Online.put("Port", ServerPort);
            System.out.println("OnExecute");
            try {
                //send the msg
                outClient.writeUTF(Online.toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    /*
    This is the MessageListener, it is used to handle the message from the server
     */
    class MessageListener extends Thread {
        String data;

        @Override
        public void run() {
            try {
                while (true) {
                    try {
                        //read data from the server
                        data = inClient.readUTF();
                        System.out.printf(data);
                        JSONObject Data = JSONObject.fromObject(data.toString());
                        if (Data.get("Status").toString().equals("Online")) {
                            //if the status from the server is online,
                            //update the list for the clients and update the display area
                            UserList.clear();
                            DisplayUsers.setText("");
                            DisplayUsers.append("UserList\n");
                            JSONArray jsonArray = Data.getJSONArray("Userlist");
                            JsonArray_Json = jsonArray;
                            System.out.println(jsonArray);
                            for (int i = 0; i < jsonArray.size(); i++) {
                                JSONObject object = (JSONObject) jsonArray.get(i);
//                               refill the display area
                                DisplayUsers.append("User" + object.get("name").toString() + "\n");
                                UserList.add("User " + jsonArray.get(i).toString() + "\n");
                            }
                            //append the msg
                            ta.append(Data.get("Content").toString() + "\n");
                        } else if (Data.get("Status").toString().equals("Broadcast")) {
                            //if it is broadcast, append the msg directly
                            ta.append(Data.get("Content").toString() + "\n");
                        } else if (Data.get("Status").toString().equals("OfflineInform")) {
                            //if it is offlineInform
                            //update the list and update the display area
                            UserList.clear();
                            DisplayUsers.setText("");
                            DisplayUsers.append("UserList\n");
                            JsonArray_Json = Data.getJSONArray("Userlist");
                            for (int i = 0; i < JsonArray_Json.size(); i++) {
                                JSONObject object = (JSONObject) JsonArray_Json.get(i);
                                DisplayUsers.append("User " + object.get("name").toString() + "\n");
                                UserList.add("User " + JsonArray_Json.get(i).toString() + "\n");
                            }
                            //append the msg, informing the offline msg
                            ta.append(Data.get("Content").toString());
                        } else if (Data.get("Status").toString().equals("BeingKick")) {
                            //if it is beingKick,first global inform the msg
                            ta.append(Data.get("UserKick").toString()+" has been removed\n");
                            //self-check if they are the person who is going to be kick
                            if (Data.get("UserKick").toString().equals(UserName)) {
                                //if it matches the ID, close connections and exit in 5 seconds
                                ta.append(Data.get("ContentForKick").toString());
                                Thread.sleep(5000);
                                socket.close();
                                ClientServer.close();
                                System.exit(EXIT_ON_CLOSE);
                            }
                            //update the list and the display area
                            DisplayUsers.setText("");
                            DisplayUsers.append("UserList\n");
                            for(int i=0;i<JsonArray_Json.size();i++){
                                JSONObject js= (JSONObject) JsonArray_Json.get(i);
                                if(js.get("name").toString().equals(Data.get("UserKick"))){
                                    JsonArray_Json.remove(i);
                                }
                            }
                            for (int i = 0; i < JsonArray_Json.size(); i++) {
                                JSONObject object = (JSONObject) JsonArray_Json.get(i);
                                DisplayUsers.append("User " + object.get("name").toString() + "\n");
                            }
                        } else if (Data.get("Status").toString().equals("requestSTATSfromUser")) {
                            //if it is requestSTATSfromUser
                            //iterate through the list
                            for (int i = 0; i < JsonArray_Json.size(); i++) {
                                JSONObject object = (JSONObject) JsonArray_Json.get(i);
                                //find the person who request STATS
                                if (object.get("name").equals(Data.get("User"))) {
                                    try {
                                        //build connection and initilize values with the target serverSocket
                                        socket = new Socket("127.0.0.1", Integer.parseInt(object.get("socket").toString()));
                                        InputP = new DataInputStream(socket.getInputStream());
                                        OutputP = new DataOutputStream(socket.getOutputStream());
                                        //create msg and put the command list into the msg, send directly to the target client
                                        JSONObject Message = new JSONObject();
                                        Message.put("User", UserName);
                                        Message.put("Status", "STATSProvide");
                                        Message.put("STATS", CommandsRecords);
                                        OutputP.writeUTF(Message.toString());
                                        //inform that someone is requesting the command list
                                        ta.append(Data.get("User") + " has requested your command list\n");
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }
            } catch (
                    Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
    this is the sendController which mainly in charge of the sending detection (used to detect command and having different operations)
     */

    class SendController implements Runnable {
        @Override
        public void run() {
            String msg = InputContent.getText().toString();
            InputContent.setText("");
            //split the msg
            String[] msgDetail = msg.split("_");
            //private talk ver.
            if (msgDetail[0].equals("MESSAGE")) {
                //add to command =record
                CommandsRecords.add("MESSAGE");
                //iterate through the list and find the target port
                for (int i = 0; i < JsonArray_Json.size(); i++) {
                    JSONObject object = (JSONObject) JsonArray_Json.get(i);
                    if (object.get("name").equals(msgDetail[1])) {
                        try {
                            //build socket using the target port
                            socket = new Socket("127.0.0.1", Integer.parseInt(object.get("socket").toString()));
                            InputP = new DataInputStream(socket.getInputStream());
                            OutputP = new DataOutputStream(socket.getOutputStream());
                            //create msg and set User, Status and content
                            JSONObject Message = new JSONObject();
                            Message.put("User", UserName);
                            Message.put("Status", "Private");
                            Message.put("Content", msgDetail[2]);
                            //write to the target serversocket
                            OutputP.writeUTF(Message.toString());
                            ta.append(UserName + "(Private to " + msgDetail[1] + "): " + msgDetail[2] + "\n");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

            } else if (msgDetail[0].equals("STOP")) {
                //add to the record
                CommandsRecords.add("STOP");
                //create obj and set status,content and user
                JSONObject Message = new JSONObject();
                Message.put("Status", "BroadcastStop");
                Message.put("Content", UserName + " has leave the chatting channel\n");
                Message.put("User", UserName);
                try {
                    //write to the server, inform of the exit
                    outClient.writeUTF(Message.toString());
                    ta.append("You will leave the room in 5 seconds\n");
                    //5seconds waiting
                    Thread.sleep(5000);
                    //exit from the system, close connection
                    System.exit(EXIT_ON_CLOSE);
                    ClientServer.close();
                    socket.close();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else if (msgDetail[0].equals("KICK")) {
                //add to the command record
                CommandsRecords.add("KICK");
                //create new obj and set the target user, status and username
                JSONObject Message = new JSONObject();
                Message.put("UserKick", msgDetail[1]);
                Message.put("User", UserName);
                Message.put("Status", "Kick");
                try {
                    //send to the user and inform of the ongoing kick
                    outClient.writeUTF(Message.toString());
                    ta.append("Please wait, " + msgDetail[1] + " would be removed in 5 seconds\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            } else if (msgDetail[0].equals("LIST")) {
                //add command to the records
                CommandsRecords.add("LIST");
                //iterate the list in client and display in the screen
                for (int i = 0; i < JsonArray_Json.size(); i++) {
                    JSONObject object = (JSONObject) JsonArray_Json.get(i);
                    ta.append("User" + object.get("name").toString() + " is online\n");
                }
            } else if (msgDetail[0].equals("STATS")) {
                //add to the command list
                CommandsRecords.add("STATS");
                //create msg and set username, status and targetID
                JSONObject Message = new JSONObject();
                Message.put("User", UserName);
                Message.put("Status", "RequestSTATS");
                Message.put("RequestID", msgDetail[1]);
                ta.append("You are requesting " + msgDetail[1] + "'s Command List\n");
                try {
                    //send directly to the server
                    outClient.writeUTF(Message.toString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }else if(msgDetail[0].equals("BROADCAST")){
                //add to the commands record
                CommandsRecords.add("BROADCAST");
                //set content,status and username
                JSONObject Message = new JSONObject();
                Message.put("Status", "Broadcast");
                Message.put("Content", msgDetail[1]);
                Message.put("User", UserName);
                try {
                    //send to the server
                    outClient.writeUTF(Message.toString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
            else {
                //by default it is broadcast
                CommandsRecords.add("BROADCAST");
                //set content and status
                JSONObject Message = new JSONObject();
                Message.put("Status", "Broadcast");
                Message.put("Content", msg);
                Message.put("User", UserName);
                try {
                    //send to the server
                    outClient.writeUTF(Message.toString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }

    /*
    this is the clientserver. It is mainly used to handle the p2p connection with other clients
     */

    class ClientServer extends Thread {
        @Override
        public void run() {
            try {
                //initiate the server
                ClientServer = new ServerSocket(Integer.parseInt(ServerPort));
                while (true) {
                    //keep listening and accept socket connection
                    PrivateFromUser = ClientServer.accept();
                    if (PrivateFromUser != null) {
                        //initialize the inputstream and outputstram
                        InputP = new DataInputStream(PrivateFromUser.getInputStream());
                        OutputP = new DataOutputStream(PrivateFromUser.getOutputStream());
                        //read the received msg
                        String data = InputP.readUTF();
                        JSONObject Data = JSONObject.fromObject(data.toString());
                        if (Data.get("Status").equals("Private")) {
                            //if it is private talk, append to the screen
                            ta.append("Private talk from" + Data.get("User") + ": " + Data.get("Content") + "\n");
                        } else if (Data.get("Status").equals("STATSProvide")) {
                            //if it is a request reply from other cleints of the stats
                            ta.append("request");
                            //diplay the reveived list
                            JSONArray CommandReceive = Data.getJSONArray("STATS");
                            for (int i = 0; i < CommandReceive.size(); i++) {
                                ta.append("Command used by " + Data.get("User") + "is: " + CommandReceive.get(i)+"\n");
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
