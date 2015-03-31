/*
 * This program was taken from here. I do not claim to have made it.
 * Hitesh said we were allowed to use it.
 * http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/
 */
import javax.swing.*;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.awt.*;
import java.awt.event.*;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;


/*
 * The Client with its GUI
 */
public class ClientGUI extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	// will first hold "Username:", later on "Enter message"
	private JLabel label;
	// to hold the Username and later on the messages
	private JTextField tf;
	// to hold the server address an the port number
	private JTextField tfServer, tfPort;
	// for the chat room
	private JTextArea ta;
	// if it is for connection
	private boolean connected;
	// the Client object
	private Client client;
	// the default port number
	private int defaultPort;
	private String defaultHost;
	private String username;

	// Constructor connection receiving a socket number
	ClientGUI(String host, int port, String s){
		super("Logged in as: "+s);
		defaultPort = port;
		defaultHost = host;
		username = s;
		this.setResizable(false);
		// The NorthPanel with:
		JPanel northPanel = new JPanel(new GridLayout(3,1));
		// the server name anmd the port number
		JPanel serverAndPort = new JPanel(new GridLayout(1,5, 1, 3));
		// the two JTextField with default value for server address and port number
		tfServer = new JTextField(host);
		tfPort = new JTextField("" + port);
		tfPort.setHorizontalAlignment(SwingConstants.RIGHT);

		// the Label and the TextField
		label = new JLabel("Enter your username below", SwingConstants.CENTER);
		northPanel.add(label);
		tf = new JTextField();
		tf.setBackground(Color.WHITE);
		northPanel.add(tf);
		add(northPanel, BorderLayout.NORTH);

		// The CenterPanel which is the chat room
		ta = new JTextArea("Welcome to the Chat room\n", 80, 80);
		JPanel centerPanel = new JPanel(new GridLayout(1,1));
		centerPanel.add(new JScrollPane(ta));
		ta.setEditable(false);
		add(centerPanel, BorderLayout.CENTER);


		//setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(600, 600);
		setVisible(true);
		// ok it is a connection request
		// empty username ignore it
		if(username.length() == 0) return;
		// empty serverAddress ignore it

		// try creating a new Client with GUI
		client = new Client(username, defaultPort, username, this);
		// test if we can start the Client
		if(!client.start()) return;
		//			tf.setText("");
		label.setText("Enter your message below");
		connected = true;

		// disable the Server and Port JTextField
		tfServer.setEditable(false);
		tfPort.setEditable(false);
		// Action listener for when the user enter a message
		tf.addActionListener(this);
		ta.setSize(this.getWidth(),this.getHeight());
		try {
//			client.sendPublicKey();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// called by the Client to append text in the TextArea 
	void append(String str) {
		ta.append(str);
		ta.setCaretPosition(ta.getText().length() - 1);
	}
	// called by the GUI is the connection failed
	// we reset our buttons, label, textfield
	void connectionFailed() {
		// reset port number and host name as a construction time
		tfPort.setText("" + defaultPort);
		tfServer.setText(defaultHost);
		// let the user change them
		tfServer.setEditable(false);
		tfPort.setEditable(false);
		// don't react to a <CR> after the username
		tf.removeActionListener(this);
		connected = false;
	}

	/*
	 * Button or JTextField clicked
	 */
	public void actionPerformed(ActionEvent e) {
		// ok it is coming from the JTextField
		if(connected){

			// just have to send the message
			String toEncrypt = tf.getText();
			if(toEncrypt.equals("") || toEncrypt.equalsIgnoreCase(" ")){}
			else{
				try {
					client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, toEncrypt));				
					tf.setText("");
					return;
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		}
	}
}
