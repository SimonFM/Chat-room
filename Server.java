/*
 * This program was taken from here. I do not claim to have made it.
 * Hitesh said we were allowed to use it.
 * I modified it to add encryption to messages and a group implementation.
 * Methods I added:
 * 		lookUp()
 * 		addToGroup()
 *		removeFromGroup()
 * 		sendNewAESKey()
 * 		resetAESKey()
 * 
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
import java.security.Key;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
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
	public static TrustManager[] trustManager;
	public static KeyStore keystore;
	public static KeyManagerFactory keyManFact;
	private SSLSocket socket;
	private SSLContext context;
	private SSLServerSocketFactory sslSocketFactory;
	private SecretKey defaultKey, groupKey;
	private boolean groupDeclared;

	/*		
	 * server constructor that receive the port to listen to for connection as
	 * parameter in console
	 */
	public Server(int port) {
		this.port = port;
		sdf = new SimpleDateFormat("HH:mm:ss");
		al = new ArrayList<ClientThread>();
		myGroup = new Group(); //  A group in the server
		groupDeclared = false;

		/* this is a key generator that creates a default key and then a
		   group key, if I wanted more groups then i could simply add in 
	       an array list of keys or have this app connect to a database of 
		   some kind. It uses AES encryption.
		 */
		KeyGenerator KeyGen;
		try {
			KeyGen = KeyGenerator.getInstance("AES");
			KeyGen.init(128); // initial it with 128 bit key
			defaultKey = KeyGen.generateKey();
			groupKey = KeyGen.generateKey();
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		}


		// the name of the keystore
		String keyName = "supersecretkeystore";
		// the password of the keystore
		char[] password = "itsasecret".toCharArray();

		/*
		 * This long try catch statement sets up the key store and the SSL sockets
		 * using X509 certificates
		 * To set up the keystore I had to enter in a 'keytool' command and make a 
		 * keystore file from which the public and private keys are stored for both
		 * the clients and the server.
		 * If this were a proper chat room, then each client would have its own keystore
		 * the same would apply to the server.
		 */
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
			ServerSocket serverSocket = (SSLServerSocket) sslSocketFactory.createServerSocket(12345);

			// infinite loop to wait for connections
			while (keepGoing) {
				socket = (SSLSocket) serverSocket.accept(); // accept connection
				// if I was asked to stop
				if (!keepGoing)
					break;
				ClientThread t = new ClientThread(socket); // make a thread of it
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
	
	/**
	 * Looks up a client if there is a thread with that username
	 * @param name
	 * @return true or false
	 */
	boolean lookUp(String name){
		for(ClientThread t : al){
			if(t.username.equals(name)) return true;
		}
		return false;
	}
	/**
	 * This method adds users to a group that in made in the server
	 * @param name - name of the user to add to the group
	 */
	public void addToGroup(String name){
		if(lookUp(name)){
			myGroup.addMember(name);
			for(ClientThread t : al){
				if(name.equals(t.username)) sendNewAESKey(t.id);
			}
		}
		else{
			System.out.println("That person does not exist...");
		}
		
	}

	/**
	 * This method removes users from a group that in made in the server
	 * @param name - name of the user to add to the group
	 */
	public void removeFromGroup(String name){
		myGroup.removeMember(name);
		for(ClientThread t : al){
			if(name.equals(t.username)){
				resetAESKey(t.id);
			}
		}
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
	/**
	 * This method sends the client a new AES key, which is that of the new group.
	 * It is sent as a message, but since it is using SSL socket I do not have to 
	 * worry about sending public and private keys. They are taken care of by the 
	 * constructor.
	 * @param id
	 */
	public void sendNewAESKey(int id){
		String encodedKey = Base64.getEncoder().encodeToString(groupKey.getEncoded());
		ClientThread ct;
		encodedKey = "AES:" + encodedKey;
		for (ClientThread t : al){
			if(t.id == id){
				ct = t;
				t.writeMsg(encodedKey);
			}
		}
	}
	/**
	 * This method resets the client's AES key to the default one inside the
	 * client class. 
	 * @param id - Client you want to reset
	 */
	public void resetAESKey(int id){
		ClientThread ct;
		String encodedKey = "AES:Reset";
		for (ClientThread t : al){
			if(t.id == id){
				ct = t;
				t.writeMsg(encodedKey);
			}
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
				myGroup.removeMember(ct.getName());
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
