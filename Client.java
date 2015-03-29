/*
 * This program was taken from here. I do not claim to have made it.
 * Hitesh said to a class mate that we were allowed to use it.
 * I modified it in order to encrpt and decrypt messages
 * http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/
 */
import java.net.*;
import java.io.*;
import java.security.Key;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/*
 * The Client that can be run both as a console or a GUI
 */
public class Client {

	// for I/O
	private ObjectInputStream sInput; // to read from the socket
	private ObjectOutputStream sOutput; // to write on the socket
	// private Socket socket;
	private SSLSocket socket;
	private SSLContext context;
	private SSLSocketFactory sslSocketFactory;
	public static TrustManager[] myTrustManager;
	public static KeyStore keystore;
	public static KeyManagerFactory keyManFact;
	
	// if I use a GUI or not
	private ClientGUI cg;

	// the server, the port and the username
	private String server, username;
	private int port;

	/*
	 * Constructor called by console mode server: the server address port: the
	 * port number username: the username
	 */
	Client(String server, int port, String username) {
		// which calls the common constructor with the GUI set to null
		this(server, port, username, null);
	}

	/*
	 * Constructor call when used from a GUI in console mode the ClienGUI
	 * parameter is null
	 */
	Client(String server, int port, String username, ClientGUI cg) {
		this.server = server;
		this.port = port;
		this.username = username;
		// save if we are in GUI mode or not
		this.cg = cg;
		char[] password = "123456".toCharArray();
		String keyStoreName = "clientkeystore";
		try {
			myTrustManager = new TrustManager[] { new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}

				public void checkClientTrusted(
					java.security.cert.X509Certificate[] certs, String authType) {}

				public void checkServerTrusted(
					java.security.cert.X509Certificate[] certs, String authType) {}
			} };
			keystore = KeyStore.getInstance("JKS");
			keystore.load(new FileInputStream(keyStoreName), password);
			keyManFact = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManFact.init(keystore, password);
			context = SSLContext.getInstance("TLS");
			context.init(keyManFact.getKeyManagers(), myTrustManager, null);
			sslSocketFactory = context.getSocketFactory();
			
		} catch (NoSuchAlgorithmException e) {e.printStackTrace();
		} catch (KeyManagementException e) {e.printStackTrace();
		} catch (KeyStoreException e) {e.printStackTrace();
		} catch (CertificateException e) {e.printStackTrace();
		} catch (FileNotFoundException e) {e.printStackTrace();
		} catch (IOException e) {e.printStackTrace();
		} catch (UnrecoverableKeyException e) {e.printStackTrace();
		}

	}

	String getUsername() {return this.username;}

	/*
	 * To start the dialog
	 */
	public boolean start() {
		// try to connect to the server
		try {
			socket = (SSLSocket) sslSocketFactory.createSocket("localhost", 12345);
		}
		// if it failed not much I can so
		catch (Exception ec) {
			display("Error connectiong to server:" + ec);
			return false;
		}

		String msg = "Connection accepted " + socket.getInetAddress() + ":"
				+ socket.getPort();
		display(msg);

		/* Creating both Data Stream */
		try {
			sInput = new ObjectInputStream(socket.getInputStream());
			sOutput = new ObjectOutputStream(socket.getOutputStream());
		} catch (IOException eIO) {
			display("Exception creating new Input/output Streams: " + eIO);
			return false;
		}

		// creates the Thread to listen from the server
		new ListenFromServer().start();
		// Send our username to the server this is the only message that we
		// will send as a String. All other messages will be ChatMessage objects
		try {
			sOutput.writeObject(username);
		} catch (IOException eIO) {
			display("Exception doing login : " + eIO);
			disconnect();
			return false;
		}
		// success we inform the caller that it worked
		return true;
	}

	/*
	 * To send a message to the console or the GUI
	 */
	private void display(String msg) {
		if (cg == null) {}// println in console mode
		else cg.append(msg + "\n"); // append to the ClientGUI JTextArea 
	}

	/*
	 * To send a message to the server
	 */
	void sendMessage(ChatMessage msg_chat) throws Exception {
		try {
			String e = doEncrypt(msg_chat.getMessage());
			// String d = doDecrypt(e);
			sOutput.writeObject(new ChatMessage(msg_chat.getType(), e));
		} catch (IOException e) {
			display("Exception writing to server: " + e);
		}
	}

	/*
	 * When something goes wrong Close the Input/Output streams and disconnect
	 * not much to do in the catch clause
	 */
	private void disconnect() {
		try {
			if (sInput != null)
				sInput.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			if (sOutput != null)
				sOutput.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			if (socket != null)
				socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// inform the GUI
		if (cg != null)
			cg.connectionFailed();
	}

	public String doEncrypt(String message) throws Exception {
		return encrypt(message);
	}

	// A method that returns a String that is an encrypted version of the
	// original
	private String encrypt(String plainText) throws Exception {
		String encryptionKey = "Bar12345Bar12345";
		Key aesKey = new SecretKeySpec(encryptionKey.getBytes(), "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, aesKey);

		return new String(cipher.doFinal(plainText.getBytes()));
	}

	public String doDecrypt(String cipher) throws Exception {
		return decrypt(cipher);
	}

	
	private String decrypt(String cipherText) throws Exception {
		String encryptionKey = "Bar12345Bar12345";
		Key aesKey = new SecretKeySpec(encryptionKey.getBytes(), "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, aesKey);

		return new String(cipher.doFinal(cipherText.getBytes()));
	}

	/*
	 * A class that waits for the message from the server and append them to the
	 * JTextArea if we have a GUI or simply System.out.println() it in console
	 * mode
	 */
	class ListenFromServer extends Thread {

		public void run() {
			while (true) {
				try {
					String msg = (String) sInput.readObject();
					// if console mode print the message and add back the prompt
					if (cg == null) {
						// System.out.println(msg);
						System.out.print("> ");
					} else {
						try {
							cg.append(msg);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} catch (IOException e) {
					display("Server has close the connection: " + e);
					if (cg != null)
						cg.connectionFailed();
					break;
				}
				// can't happen with a String object but need the catch anyhow
				catch (ClassNotFoundException e2) {
				}
			}
		}
	}
}
