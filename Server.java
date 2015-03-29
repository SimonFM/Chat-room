/*
 * This program was taken from here. I do not claim to have made it.
 * I modified it to add encryption to messages.
 * http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/
 */

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/*
 * The server that can be run both as a console application or a GUI
 */
public class Server {
	// a unique ID for each connection
	private static int uniqueId;
	// an ArrayList to keep the list of the Client
	private ArrayList<ClientThread> al;
	// to display time
	private SimpleDateFormat sdf;
	// the port number to listen for connection
	private int port;
	// the boolean that will be turned of to stop the server
	private boolean keepGoing;
	private Group myGroup;
	private boolean encryptionEnabled;
	public static TrustManager[] trustManager;
	public static KeyStore keystore;
	public static KeyManagerFactory keyManFact;

	private SSLSocket socket;
	private SSLContext context;
	private SSLServerSocketFactory sslSocketFactory;

	/*
	 * server constructor that receive the port to listen to for connection as
	 * parameter in console
	 */
	public Server(int port) {
		this.port = port;
		sdf = new SimpleDateFormat("HH:mm:ss");
		al = new ArrayList<ClientThread>();
		myGroup = new Group();
		encryptionEnabled = false;
		String keyName = "clientkeystore";
		char[] password = "123456".toCharArray();
		try {
			keystore = KeyStore.getInstance("JKS");
			keystore.load(new FileInputStream(keyName), password);
			keyManFact = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManFact.init(keystore, password);
			context = SSLContext.getInstance("TLS");
			context.init(keyManFact.getKeyManagers(), trustManager, null);
			sslSocketFactory = context.getServerSocketFactory();
			trustManager = new TrustManager[] { new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}
				public void checkClientTrusted(
						java.security.cert.X509Certificate[] certs, String authType) {
				}
				public void checkServerTrusted(
						java.security.cert.X509Certificate[] certs, String authType) {
				}
			} };
		} catch (UnrecoverableKeyException e) {e.printStackTrace();}
		  catch (KeyStoreException e) {e.printStackTrace();}
		  catch (NoSuchAlgorithmException e) {e.printStackTrace();}
		  catch (CertificateException e) {e.printStackTrace();}
		  catch (FileNotFoundException e) {e.printStackTrace();}
		  catch (IOException e) {e.printStackTrace();}
		  catch (KeyManagementException e) {e.printStackTrace();
		}
	}

	public void start() {
		keepGoing = true;
		/* create socket server and wait for connection requests */
		try {
			// the socket used by the server
			ServerSocket serverSocket = (SSLServerSocket) sslSocketFactory
					.createServerSocket(12345);

			// infinite loop to wait for connections
			while (keepGoing) {
				socket = (SSLSocket) serverSocket.accept(); // accept connection
				// if I was asked to stop
				if (!keepGoing)
					break;
				ClientThread t = new ClientThread(socket); // make a thread of
															// it
				al.add(t); // save it in the ArrayList
				t.start();
			}
			// I was asked to stop
			try {
				serverSocket.close();
				for (int i = 0; i < al.size(); ++i) {
					ClientThread tc = al.get(i);
					try {
						tc.sInput.close();
						tc.sOutput.close();
						tc.socket.close();
					} catch (IOException ioE) {}
				}
			} catch (Exception e) {}
		}
		// something went bad
		catch (IOException e) {
			String msg = sdf.format(new Date())+ " Exception on new ServerSocket: " + e + "\n";
		}
	}

	public void enableEncryption(){encryptionEnabled = true;}

	public void disableEncryption(){encryptionEnabled = false;}

	public void addToGroup(String name){myGroup.addMember(name);}

	public void removeFromGroup(String name){myGroup.removeMember(name);}

	/*
	 * For the GUI to stop the server
	 */
	protected void stop() throws IOException {
		keepGoing = false;
		// connect to myself as Client to exit statement
		ServerSocket serverSocket = new ServerSocket(100);
		Socket socket = serverSocket.accept();
		try {
			new Socket("localhost", 100);
		} catch (Exception e) {e.printStackTrace();}
	}

	/*
	 * to broadcast a message to all Clients
	 */
	private synchronized void broadcast(String message) {
		// add HH:mm:ss and \n to the message
		String time = sdf.format(new Date());
		String messageLf = time + " " + message + "\n";

		// we loop in reverse order in case we would have to remove a Client
		// because it has disconnected
		for (int i = al.size(); --i >= 0;) {
			ClientThread ct = al.get(i);
			// try to write to the Client if it fails remove it from the list
			if (!ct.writeMsg(messageLf)) al.remove(i);
		}
	}

	// for a client who logged off using the LOGOUT message
	synchronized void remove(int id) {
		// scan the array list until we found the Id
		for (int i = 0; i < al.size(); ++i) {
			ClientThread ct = al.get(i);
			// found it
			if (ct.id == id) {
				al.remove(i);
				return;
			}
		}
	}
	/** One instance of this thread will run for each client */
	class ClientThread extends Thread {
		// the socket where to listen/talk
		Socket socket;
		ObjectInputStream sInput;
		ObjectOutputStream sOutput;
		int id;
		String username;
		ChatMessage cm;
		String date;

		// Constructor
		ClientThread(Socket socket) {
			// a unique id
			id = ++uniqueId;
			this.socket = socket;
			/* Creating both Data Stream */
			// System.out.println("Thread trying to create Object Input/Output Streams");
			try {
				// create output first
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput = new ObjectInputStream(socket.getInputStream());
				// read the username
				username = (String) sInput.readObject();
			} catch (IOException e) {
				System.out.println("Exception creating new Input/output Streams: "+ e);
				return;
			}
			// have to catch ClassNotFoundException
			// but I read a String, I am sure it will work
			catch (ClassNotFoundException e) {}
			date = new Date().toString() + "\n";
		}

		// what will run forever
		public void run() {
			// to loop until LOGOUT
			boolean keepGoing = true;
			while (keepGoing) {
				// read a String (which is an object)
				try {
					cm = (ChatMessage) sInput.readObject();
				} catch (IOException e){break;} 
				  catch (ClassNotFoundException e2){break;}
				// the messaage part of the ChatMessage
				String message = cm.getMessage();
				// Switch on the type of message receive
				switch (cm.getType()) {
				case ChatMessage.MESSAGE:
					broadcast(username + ": " + message);
					break;
				case ChatMessage.LOGOUT:
					keepGoing = false;
					break;
				case ChatMessage.WHOISIN:
					writeMsg("List of the users connected at "+ sdf.format(new Date()) + "\n");
					// scan al the users connected
					for (int i = 0; i < al.size(); ++i) {
						ClientThread ct = al.get(i);
						writeMsg((i + 1) + ") " + ct.username + " since "+ ct.date);
					}
					break;
				}
			}
			// remove myself from the arrayList containing the list of the
			// connected Clients
			remove(id);
			close();
		}

		// try to close everything
		private void close() {
			// try to close the connection
			try {
				if (sOutput != null) sOutput.close();
			} catch (Exception e) {}
			try {
				if (sInput != null)	sInput.close();
			} catch (Exception e) {}
			;
			try {
				if (socket != null) socket.close();
			} catch (Exception e) {}
		}

		/*
		 * Write a String to the Client output stream
		 */
		private boolean writeMsg(String msg) {
			// if Client is still connected send the message to it
			if (!socket.isConnected()) {
				close();
				return false;
			}
			// write the message to the stream
			try {
				sOutput.writeObject(msg);
			}
			// if an error occurs, do not abort just inform the user
			catch (IOException e){}
			return true;
		}
	}
}
