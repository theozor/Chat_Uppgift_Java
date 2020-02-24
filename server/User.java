/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import common.UserInfo;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Elev
 */
public class User extends UserInfo implements Runnable {
    
    private final ObjectOutputStream oos;
    private final ObjectInputStream ois;
    private final Socket socket;
    private ArrayList<User> users;
    private boolean status = true;
    
    public User(ObjectOutputStream oos, ObjectInputStream ois, Socket socket, ArrayList<User> users, String name) {
        super(name);
        this.oos = oos;
        this.ois = ois;
        this.socket = socket;
        this.users = users;
    }
    
    public boolean getStatus() {
        return status;
    }
    
    public void setStatus(boolean status) {
        this.status = status;
    }
    
    public ObjectOutputStream getObjectOutputStream() {
        return oos;
    }
    
    public ObjectInputStream getObjectInputStream() {
        return ois;
    }
    
    public Socket getSocket() {
        return socket;
    }
    
    private String readMessage() {
        try {
            String in = (String) ois.readObject();
            return doCommand(in);
            
        } catch (IOException | ClassNotFoundException ex) {
            status = false;
            Server.removeBadConnections();
            return null;
        }
    }
    
    private void sendMessage(String in) {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        
        in = "[" + getName() + "] " + dateFormat.format(cal.getTime()) + ":\n" + in + "\n";
        Server.showMessage("\n"+in);
        for (int i = 0; i < users.size(); i++) {
            try {
                if (i == users.indexOf(this)) {
                    users.get(i).oos.writeObject("\n(You)" + in);
                    users.get(i).oos.flush();
                } else {
                    users.get(i).oos.writeObject("\n"+in);
                    users.get(i).oos.flush();
                }
            } catch (IOException ex) {
                users.get(i).status = false;
                Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
    }
    
    private String doCommand(String line) {
        String com;
        
        if (line.indexOf('/') == 0) {
            try {
                com = line.substring(line.indexOf('/'), line.indexOf(' '));
            } catch (IndexOutOfBoundsException e) {
                com = "";
            }
            
            switch (com) {
                case "/name":
                    String oldName = getName();
                    try {
                        System.out.println(line.substring(line.indexOf(' ') + 1));
                        setName(line.substring(line.indexOf(' ') + 1));
                    } catch (IndexOutOfBoundsException e) {
                        setName(oldName);
                    }
                    Server.sendUsernames();
                    return oldName + " är numera känd som " + getName() + "!\n";
                case "/whisper":
                    DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                    Calendar cal = Calendar.getInstance();
                    line = line.substring(9);
                    System.out.println(line);
                    User reciever;
                    try {
                        reciever = users.get(Character.getNumericValue(line.charAt(0)));
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.out.println("AIOOB whisper" + ": " +line.charAt(0));
                        return "";
                    }
                    
                    try {
                        reciever.getObjectOutputStream().writeObject("[" + getName() + "] " + dateFormat.format(cal.getTime()) + ":\n" + "(Whispers)\n" + line.substring(2) + "\n");
                        oos.writeObject("[" + getName() + "] " + dateFormat.format(cal.getTime()) + ":\n" + "(Whispers to " + reciever.getName() + ")\n" + line.substring(2) + "\n");
                        Server.showMessage(getName() + " whispered to " + reciever.getName() + "\n");
                        System.out.println("Succes whisper");
                    } catch (IOException ex) {
                        Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    return "";
                default:
                    return "";
            }
        } else {
            return line;
        }
    }
    
    @Override
    public void run() {
        while (true) {
            String in = readMessage();
            System.out.println(in);
            if (in == null) {
                status = false;
                return;
            } else if (in.length() != 0) {
                /*DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                Calendar cal = Calendar.getInstance();
                Server.showMessage("\n[" + getName() + "] " + dateFormat.format(cal.getTime()) + ":\n" + in);*/
                sendMessage(in);
            }
            
        }
    }
}
