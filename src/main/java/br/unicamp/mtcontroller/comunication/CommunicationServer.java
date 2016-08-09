package br.unicamp.mtcontroller.comunication;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Du on 05/04/16.
 */
public class CommunicationServer implements Runnable {

    private String name;
    private ServerSocket serverSocket;
    private boolean bStopped = false;
    private boolean bEndConnections = false;
    private int iPort = 4011;
    private Map<String, Socket> agents;

    public CommunicationServer(String name, int port) {
        this.setName(name);
        this.setAgents(new HashMap<String, Socket>());
        this.setiPort(port);
    }

    public void startServer() {
        try {
            setServerSocket(new ServerSocket(getiPort()));
            Thread currentThread = new Thread(this);
            currentThread.start();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }


    @Override
    public void run() {

        try {
            System.out.println("Initializing Communication Server on " + InetAddress.getLocalHost().getHostAddress() + getiPort());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        Socket clientSocket = null;
        DataInputStream in = null;
        PrintStream out = null;
        Gson gson = new GsonBuilder().create();


        while (!isbEndConnections() && !isbStopped()) {

            try {
                clientSocket = this.getServerSocket().accept();

                Message outputMessage = new Message(getName(), "UNKNOWN", "CONNECTED");

                sendMessage(clientSocket, outputMessage);

                Message inputMessage = readMessage(clientSocket);
                getAgents().put(inputMessage.getValue(), clientSocket);

                System.out.println("Server -> Agent: " + inputMessage.getValue() + " - Connected");


            } catch (IOException e) {
                throw new RuntimeException(
                        "Error accepting client connection.", e);
            }
        }

        while (!isbStopped()) {

            try {
                clientSocket = this.getServerSocket().accept();

                Message inputMessage = readMessage(clientSocket);

                System.out.println("Server: Message: " + inputMessage.getValue() + " -> From:" + inputMessage.getAgentIdSender() + " To:" + inputMessage.getAgentIdReceiver());

                Map.Entry<String, Socket> clientReceiver = getAgents().entrySet().stream().filter(x -> x.getKey() == inputMessage.getAgentIdSender()).findFirst().get();
                Socket clientSocketReceiver = clientReceiver.getValue();

                sendMessage(clientSocketReceiver, inputMessage);


            } catch (IOException e) {
                throw new RuntimeException(
                        "Error in communication with client.", e);
            }
        }
    }


    public void setEndConnections(boolean value) {
        this.bEndConnections = value;
    }


    public synchronized void sendMessage(Socket client, Message objMessage) {

        try {
            PrintStream out = new PrintStream(client.getOutputStream());
            Gson gson = new GsonBuilder().create();

            String message = gson.toJson(objMessage);
            out.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public synchronized Message readMessage(Socket client) {

        Message message = null;
        try {

            DataInputStream in = new DataInputStream(client.getInputStream());
            Gson gson = new GsonBuilder().create();
            message = gson.fromJson(in.readLine(), Message.class);

        } catch (IOException e) {

            e.printStackTrace();
        }

        return message;
    }

    public synchronized String getName() {
        return name;
    }

    public synchronized void setName(String name) {
        this.name = name;
    }

    public synchronized boolean isbStopped() {
        return bStopped;
    }

    public synchronized void setbStopped(boolean bStopped) {
        this.bStopped = bStopped;
    }


    public synchronized Map<String, Socket> getAgents() {
        return agents;
    }

    public void setAgents(Map<String, Socket> agents) {
        this.agents = agents;
    }

    public int getiPort() {
        return iPort;
    }

    public void setiPort(int iPort) {
        this.iPort = iPort;
    }

    public boolean isbEndConnections() {
        return bEndConnections;
    }

    public void setbEndConnections(boolean bEndConnections) {
        this.bEndConnections = bEndConnections;
    }
}
