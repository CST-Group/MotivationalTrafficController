package br.unicamp.mtcontroller.comunication;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Created by Du on 04/04/16.
 */
public class CommunicationAgent implements Runnable {

    private String name;
    private Socket socket;
    private PrintStream out;
    private DataInputStream in;
    private boolean bStopped = false;
    private Message receivedMessage;
    private Message sentMessage;
    private List<String> agents;
    private boolean bServerVersion = false;
    private int port = 4011;


    public CommunicationAgent(String name, int port){
        this.setName(name);
        this.setPort(port);
        this.OpenConnection();

    }

    @Override
    public void run() {
        while(!isbStopped()){
            readCurrentMessage();
        }
    }

    public Message readCurrentMessage(){
        Message message =  getReceivedMessage();
        setReceivedMessage(message);

        return message;
    }

    public void OpenConnection(){
        try {
            setSocket(new Socket(InetAddress.getLocalHost(), getPort()));
            setOut(new PrintStream(getSocket().getOutputStream()));
            setIn(new DataInputStream(getSocket().getInputStream()));

            Message read = readMessage();

            Message message =  new Message(getName(), "SERVER", getName());
            sendMessage(message);
            System.out.println("Client -> Agent: " +getName()+ " - Connected");

        } catch (UnknownHostException e) {
            System.out.println("Unknown host.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public synchronized void sendMessage(Message objMessage){
        Gson gson = new GsonBuilder().create();
        setSentMessage(objMessage);
        String message = gson.toJson(objMessage);
        getOut().println(message);
    }

    public synchronized Message readMessage(){
        Gson gson = new GsonBuilder().create();
        Message message = null;
        try {
            message = gson.fromJson(getIn().readLine(), Message.class);
            setReceivedMessage(message);
        } catch (IOException e) {
            setReceivedMessage(null);
            e.printStackTrace();
        }

        return message;
    }

    public synchronized Socket getSocket() {
        return socket;
    }

    public synchronized void setSocket(Socket socket) {
        this.socket = socket;
    }

    public synchronized PrintStream getOut() {
        return out;
    }

    public synchronized void setOut(PrintStream out) {
        this.out = out;
    }

    public synchronized DataInputStream getIn() {
        return in;
    }

    public synchronized void setIn(DataInputStream in) {
        this.in = in;
    }

    public boolean isbStopped() {
        return bStopped;
    }

    public void setbStopped(boolean bStopped) {
        this.bStopped = bStopped;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public synchronized Message getReceivedMessage() {
        return receivedMessage;
    }

    public synchronized void setReceivedMessage(Message receivedMessage) {
        this.receivedMessage = receivedMessage;
    }

    public synchronized List<String> getAgents() {
        return agents;
    }

    public void setAgents(List<String> agents) {
        this.agents = agents;
    }

    public boolean isbServerVersion() {
        return bServerVersion;
    }

    public void setbServerVersion(boolean bServerVersion) {
        this.bServerVersion = bServerVersion;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Message getSentMessage() {
        return sentMessage;
    }

    public void setSentMessage(Message sentMessage) {
        this.sentMessage = sentMessage;
    }
}
