package client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.text.DefaultCaret;
import common.UserInfo;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;

/**
 *
 * @author Theofil Eriksson Petrovski
 */
public class Client extends JFrame implements Runnable {

    private JPanel listPanel;
    private JTextField inputText;
    private JTextPane outputText;
    private JScrollPane outputTextScroll;
    private JMenuBar menubar;
    private JMenu file, settings, chat;
    private JMenuItem exit, disconnect, connections, userNameItem, clearChat, tcolor, bcolor, bigChat;
    private JCheckBoxMenuItem usehtml;
    private JList<UserInfo> peopleList;
    private DefaultListModel<UserInfo> lm;
    private JPopupMenu listPopup;
    private JMenuItem listPopupWhisper;
    private int port = 2776, brgbColor[] = {255, 255, 255}, trgbColor[] = {0, 0, 0};
    private String host = "127.0.0.1", name = "User", viewText = "";
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private boolean status = false;
    private Thread runThread;
    private Color currentBColor, currentTextColor;

    public Client() {
        buildGUI();
        fixEvents();
        runThread = new Thread(this);

    }

    public static void main(String[] args) {
        new Client();
    }

    @Override
    public void run() {
        status = true;
        try {
            showMessage("Connecting to server...\n");
            connectToServer();
            setupStreams();
        } catch (IOException e) {
            showMessage("Connection error:\n" + e.getMessage());

            disableServerBtn();
            disconnect();

            return;
        }
        showMessage("Connection successfull!\n");

        enableServerBtn();
        sendMessage(name);

        while (true) {
            Object input = readMessage();
            String in;
            //Ser om klienten får ett meddelande eller något annat
            UserInfo[] inputList = null;
            if (input instanceof String) {
                in = (String) input;
                showMessage(in);
            } else if (input instanceof UserInfo[]) {
                //Uppdaterar listan med användare om en sådan inkommit
                inputList = (UserInfo[]) input;
                //Rensar listan och populerar den
                lm.clear();
                for (int i = 0; i < inputList.length; i++) {
                    //Ibland sker en nullpointer i awt tråden.
                    try {
                        lm.add(i, inputList[i]);
                    } catch (NullPointerException ex) {
                    }
                }
            } else {
                System.out.println("Unknown object recieved");
            }
            /*
            Updaterar listan även vid mottagning utav Strängar eftersom
            awt-tråden är oförustägbar. Om JList enbart updateras när dess
            ListModel förändras finns en chans att JList.updateUI() körs innan
            förändringarana i ListModel är genomförda.
             */
            peopleList.updateUI();
            if (!status) {
                disconnect();
                return;
            }
        }

    }

    /**
     * Ändar klientens läga till offline
     */
    private void disableServerBtn() {
        inputText.setEnabled(false);
        bigChat.setEnabled(false);
        disconnect.setEnabled(false);
    }

    /**
     * Ändar klientens läga till online
     */
    private void enableServerBtn() {
        inputText.setEnabled(true);
        bigChat.setEnabled(true);
        disconnect.setEnabled(true);
    }

    /**
     * Stänger strömmar och socklar, väntar på att trådar skall köras klart och
     * ändrar klientens utseende.
     */
    private void disconnect() {

        if (!status) {
            return;
        }
        /*try { //Om den redan är stängd
            if (socket.isClosed()) {
                status = false;
                lm.clear();
                peopleList.updateUI();
                return;
            }
        } catch (NullPointerException e) {
            status = false;

            disableServerBtn();
            try {
                runThread.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
            runThread = new Thread(this);
            lm.clear();
            peopleList.updateUI();
            return;
        }*/

        showMessage("Disconnecting...\n");
        try {
            //Stänger strömmar
            socket.close();
            ois.close();
            oos.close();
        } catch (IOException ex) {
            System.out.println("Unable to close socket and streams\n");
        }
        //Ändrar klientens utseende och väntar på slutkörning utav trådar.
        status = false;
        disableServerBtn();
        runThread = new Thread(this);
        showMessage("Disconnected!\n");
        //Tömmer användarlistan
        lm.clear();
        peopleList.updateUI();
    }

    private Object readMessage() {
        try {
            Object in = ois.readObject();
            return in;
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            disconnect();
            return null;
        }

    }

    private void connectToServer() throws IOException, UnknownHostException {
        long startTime = System.currentTimeMillis() / 1000;
        while (System.currentTimeMillis() / 1000 - startTime < 10) {
            try {
                socket = new Socket(host, port);
                return;
            } catch (ConnectException e) {

            }
        }
        showMessage("Connection timeout");
        disconnect();
        throw new ConnectException("Connection timed out");
    }

    private void setupStreams() throws IOException {
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());
    }

    private void fixEvents() {
        inputText.addActionListener((ActionEvent e) -> {
            String in = inputText.getText();
            inputText.setText("");
            sendMessage(in);
        });
        exit.addActionListener((ActionEvent e) -> {
            System.exit(0);
        });
        disconnect.addActionListener((ActionEvent e) -> {
            disconnect();
        });
        connections.addActionListener((ActionEvent e) -> {
            showConnect();
        });
        userNameItem.addActionListener((ActionEvent e) -> {
            name = JOptionPane.showInputDialog(this, "Set name", name);
            if (status) {
                sendMessage("/name " + name);
            }
        });
        clearChat.addActionListener((ActionEvent e) -> {
            viewText = "";
            showMessage("");
        });
        usehtml.addActionListener((ActionEvent e) -> {
            if (usehtml.isSelected()) {
                outputText.setContentType("text/html");
            } else {
                outputText.setContentType("text/plain");
            }
            bcolor.setEnabled(usehtml.isSelected());
            tcolor.setEnabled(usehtml.isSelected());
            showMessage("");
        });
        bcolor.addActionListener((ActionEvent e) -> {
            currentBColor = JColorChooser.showDialog(this, "Choose a background-color", currentBColor);
            brgbColor[0] = currentBColor.getRed();
            brgbColor[1] = currentBColor.getGreen();
            brgbColor[2] = currentBColor.getBlue();
            showMessage("");
        });
        tcolor.addActionListener((ActionEvent e) -> {
            currentTextColor = JColorChooser.showDialog(this, "Choose a text-color", currentTextColor);
            trgbColor[0] = currentTextColor.getRed();
            trgbColor[1] = currentTextColor.getGreen();
            trgbColor[2] = currentTextColor.getBlue();
            showMessage("");

        });
        bigChat.addActionListener((ActionEvent e) -> {
            sendMessage(showSend());
        });

        listPopupWhisper.addActionListener((ActionEvent e) -> {
            sendMessage("/whisper " + peopleList.getSelectedIndex() + " " + showSend());
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

    private void buildGUI() {
        inputText = new JTextField(5);
        outputText = new JTextPane();
        outputText.setContentType("text/plain");
        outputTextScroll = new JScrollPane(outputText);
        DefaultCaret caret = (DefaultCaret) outputText.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        menubar = new JMenuBar();

        file = new JMenu("File");
        settings = new JMenu("Settings");
        chat = new JMenu("Chat");

        clearChat = new JMenuItem("Clear chat");
        tcolor = new JMenuItem("Chat text color");
        tcolor.setEnabled(false);
        bcolor = new JMenuItem("Chat background color");
        bcolor.setEnabled(false);

        exit = new JMenuItem("Exit");
        disconnect = new JMenuItem("Disconnect");
        disconnect.setEnabled(false);
        connections = new JMenuItem("Connect");
        userNameItem = new JMenuItem("Username");
        bigChat = new JMenuItem("Write Message");
        usehtml = new JCheckBoxMenuItem("Parse html");

        listPanel = new JPanel();
        lm = new DefaultListModel<>();
        peopleList = new JList<>(lm);
        listPanel.add(peopleList);

        listPopup = new JPopupMenu();
        listPopupWhisper = new JMenuItem("Whisper");
        listPopup.add(listPopupWhisper);

        disableServerBtn();

        file.add(connections);
        file.add(disconnect);
        file.add(exit);

        settings.add(userNameItem);
        settings.add(usehtml);

        chat.add(clearChat);
        chat.add(tcolor);
        chat.add(bcolor);

        menubar.add(file);
        menubar.add(settings);
        menubar.add(chat);
        menubar.add(bigChat);
        menubar.add(new JMenuItem());
        setJMenuBar(menubar);

        add(outputTextScroll);
        add(inputText, BorderLayout.SOUTH);
        add(listPanel, BorderLayout.EAST);
        outputText.setEditable(false);
        setVisible(true);
        setSize(400, 300);
        setTitle("Client");
        setDefaultCloseOperation(3);

    }

    /**
     * Gör en ny tråd och skickar meddelande. Ny tråd görs för att inte störa
     * awt-tråden.
     *
     * @param in Meddelande som skall skickas.
     */
    private void sendMessage(String in) {
        if (in.length() == 0) {
            return;
        }
        //Ny tråd i lambda-uttryck
        Runnable sendThread = () -> {
            try {
                oos.writeObject(in);
            } catch (IOException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
        };
        new Thread(sendThread).start();
    }

    /**
     * Updaterar texten i log-rutan. Formaterar även till html om bestämt.
     *
     * @param in Meddelande som skall läggas till.
     */
    private void showMessage(String in) {
        viewText += in;
        if (usehtml.isSelected()) {
            //Byter ut alla radbryt mot html-radbryt.
            String htmlText = viewText.replace("\r\n", "<br />");
            htmlText = htmlText.replace("\r", "<br />");
            htmlText = htmlText.replace("\n", "<br />");
            //Formaterar html och ändrar attribut till användarvalda färger.
            outputText.setText("<html><head><style>body {color: rgb("
                    + trgbColor[0] + ","
                    + trgbColor[1] + ","
                    + trgbColor[2]
                    + ",0); background-color: rgb("
                    + brgbColor[0] + ","
                    + brgbColor[1] + ","
                    + brgbColor[2]
                    + ",0);}</style></head><body>"
                    + htmlText);
        } else {
            outputText.setText(viewText);
        }
    }

    /**
     * Öppnar en dialogruta för att ansluta till en server. Dialogrutan
     * innehåller fält för ip och port.
     */
    private void showConnect() {
        JPanel p = new JPanel(new BorderLayout(5, 5));

        JPanel labels = new JPanel(new GridLayout(0, 1, 2, 2));
        labels.add(new JLabel("IP:", SwingConstants.RIGHT));
        labels.add(new JLabel("Port:", SwingConstants.RIGHT));
        p.add(labels, BorderLayout.WEST);

        JPanel controls = new JPanel(new GridLayout(0, 1, 2, 2));
        JTextField ip = new JTextField("127.0.0.1"); //Default ip är localhost
        controls.add(ip);
        JTextField portField = new JTextField("2776");
        controls.add(portField);
        p.add(controls, BorderLayout.CENTER);
        int retval = JOptionPane.showConfirmDialog(
                this, p, "Log In", JOptionPane.OK_CANCEL_OPTION);
        //Om användaren inte tryckt på ok skall ingen anslutning genomföras.
        if (retval != JOptionPane.OK_OPTION) {
            return;
        }
        host = ip.getText();
        try {
            port = Integer.parseInt(portField.getText());
        } catch (NumberFormatException e) {
            port = 2776;
        }
        //Ser till att tidigare server kopplas ifrån.
        disconnect();
        runThread.start();
    }

    /**
     * Öppnar en dialogruta för att skicka längre meddelanden.
     */
    private String showSend() {
        JPanel p = new JPanel(new BorderLayout(5, 5));

        JPanel labels = new JPanel(new GridLayout(0, 1, 2, 2));
        labels.add(new JLabel("Message:", SwingConstants.RIGHT));
        p.add(labels, BorderLayout.WEST);

        JTextPane messageArea = new JTextPane();
        JScrollPane messageScroll = new JScrollPane(messageArea);
        //messageScroll.setSize(300, 600);
        p.add(messageScroll, BorderLayout.CENTER);

        JOptionPane pane;
        JDialog dialog;
        int retval = -1;
        String inText = "";
        while (retval != JOptionPane.OK_OPTION) {
            messageArea.setEditable(true);
            messageArea.setContentType("text/plain");
            messageArea.setText(inText);
            pane = new JOptionPane(p);
            dialog = pane.createDialog(this, "Input Message");
            dialog.setSize(300, 250);
            dialog.setVisible(true);
            inText = messageArea.getText();
            if (messageArea.getText().length() == 0) {
                retval = JOptionPane.CANCEL_OPTION;
            } else {
                messageArea.setEditable(false);
                if (usehtml.isSelected()) {
                    messageArea.setContentType("text/html");
                    String htmlText = inText;
                    //Formaterar html och ändrar attribut till användarvalda färger.
                    messageArea.setText("<html><head><style>body {color: rgb("
                            + trgbColor[0] + ","
                            + trgbColor[1] + ","
                            + trgbColor[2]
                            + ",0); background-color: rgb("
                            + brgbColor[0] + ","
                            + brgbColor[1] + ","
                            + brgbColor[2]
                            + ",0);}</style></head><body>"
                            + htmlText);
                } else {
                    messageArea.setText(inText);
                }
                retval = JOptionPane.showConfirmDialog(
                        this, p, "Send?", JOptionPane.OK_CANCEL_OPTION);
            }
            if (retval != JOptionPane.OK_OPTION && messageArea.getText().length() == 0) {
                return "";
            }
        }
        return inText;
    }

}
