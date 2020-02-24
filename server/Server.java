package server;

import common.UserInfo;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;

/**
 *
 * @klass TE16B
 * @author Theofil Eriksson Petrovski
 */
public class Server {

    private static JFrame frame;
    private static JTextField inputText;
    private static JTextArea outputText;
    private static JScrollPane scrollPane;
    private static int port = 2776;
    private static ServerSocket serverSocket;
    private static ObjectOutputStream oos;
    private static ObjectInputStream ois;
    private static ArrayList<User> users;
    private static JList<UserInfo> peopleList;
    private static DefaultListModel<UserInfo> lm;
    private static JPopupMenu listPopup;
    private static JMenuItem listPopupKick;

    public static void main(String[] args) {
        buildGUI();
        fixEvents();
        Timer userUpdate = new Timer(10, (ActionEvent) -> {
            try {
                peopleList.updateUI();
            } catch (NullPointerException e) {
                try {
                lm.clear();
                for(int i = 0; i < users.size() && i < lm.size(); i++) {
                    lm.add(i, users.get(i));
                }
                } catch (NullPointerException e2) {
                    
                }
            }
        });
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(0);
        }
        users = new ArrayList<>();
        userUpdate.start();
        while (true) {
            removeBadConnections();
            try {
                Socket socket = serverSocket.accept();
                showMessage("Connection from: " + socket.getInetAddress()+"\n");

                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                String name = (String) in.readObject();
                User newUser = new User(out, in, socket, users, name);
                Thread userThread = new Thread(newUser);
                users.add(newUser);
                userThread.start();
                sendUsernames();
                broadcastMessage("User " + name + " connected!");
                try {
                    lm.addElement(newUser);
                    peopleList.updateUI();
                } catch (NullPointerException e) {
                    //Ibland är awt-tråden för snabb
                }
            } catch (IOException | ClassNotFoundException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    public static void sendUsernames() {
        UserInfo[] userArray = new UserInfo[users.size()];
        for (int i = 0; i < userArray.length; i++) {
            userArray[i] = users.get(i).getCopy();
        }
        for (User usr : users) {
            try {
                usr.getObjectOutputStream().writeObject(userArray);
                usr.getObjectOutputStream().flush();
            } catch (IOException ex) {
                removeBadConnections();
            }
        }
    }

    public static void removeBadConnections() {
        ArrayList<User> bad = new ArrayList<>();
        for (User user : users) {
            if (!user.getStatus()) {
                bad.add(user);
            }
        }
        if (bad.isEmpty()) {
            //sendUsernames();
            return;
        }
        for (User badUser : bad) {
            users.remove(badUser);
            lm.removeElement(badUser);
            try {
                badUser.getObjectInputStream().close();
                badUser.getObjectOutputStream().close();
                badUser.getSocket().close();
            } catch (IOException e) {
            }
            broadcastMessage("User " + badUser.getName() + " disconnected.");
        }
        sendUsernames();
        peopleList.updateUI();
    }

    private static void fixEvents() {
        inputText.addActionListener((ActionEvent e) -> {
            String in = inputText.getText();
            broadcastMessage(in);
            inputText.setText("");
        });

        listPopupKick.addActionListener((ActionEvent e) -> {
            users.get(peopleList.getSelectedIndex()).setStatus(false);
            removeBadConnections();
        });

        peopleList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    listPopup.show(e.getComponent(),
                            e.getX(), e.getY());
                }
            }
        });
    }

    private static void buildGUI() {
        frame = new JFrame();
        inputText = new JTextField();
        outputText = new JTextArea();
        scrollPane = new JScrollPane(outputText);
        
        lm = new DefaultListModel<>();
        peopleList = new JList(lm);
        listPopup = new JPopupMenu();
        listPopupKick = new JMenuItem("Kick");
        listPopup.add(listPopupKick);
        
        frame.add(scrollPane);
        frame.add(inputText, BorderLayout.SOUTH);
        frame.add(peopleList, BorderLayout.EAST);
        outputText.setEditable(false);
        frame.setVisible(true);
        frame.setSize(400, 300);
        frame.setTitle("Server");
        frame.setDefaultCloseOperation(3);

    }

    public static void showMessage(String in) {
        outputText.append(in);
    }

    private static void broadcastMessage(String in) {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Calendar cal = Calendar.getInstance();

        in = "\n[SERVER]" + dateFormat.format(cal.getTime()) + ":\n" + in + "\n";
        showMessage(in);
        for (int i = 0; i < users.size(); i++) {
            try {
                users.get(i).getObjectOutputStream().writeObject(in);
                users.get(i).getObjectOutputStream().flush();
            } catch (IOException ex) {
                removeBadConnections();
                Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

}
