/*
 * This program was taken from here. I do not claim to have made it.
 * Hitesh said we were allowed to use it.
 * I modified it in order to encrpt and decrypt messages
 * 	Methods I added:
 * 		encrypt()
 * 		doEcrypt()
 * 		decrypt()
 * 		doDecrypt()
 * 		getUsername()
 *      resetKey()
 *      updateKey()
 *      
 * http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/
 */
import java.net.*;
import java.io.*;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
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
	private ObjectInputStream sInput; // to read from the socket
	private ObjectOutputStream sOutput; // to write on the socket
	private SSLSocket socket;
	private SSLContext context;
	private SSLSocketFactory sslSocketFactory;
	public static TrustManager[] myTrustManager;
	public static KeyStore keystore;
	public static KeyManagerFactory keyManFact;
	private ClientGUI cg;

	// the server, the port and the username
	private String server, username;
	private int port;
	private String defaultKey = "Bar12345Bar12345";
	private SecretKey key;

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
		key = new SecretKeySpec(defaultKey.getBytes(), "AES");
		char[] password = "itsasecret".toCharArray();
		String keyStoreName = "supersecretkeystore";
		/*
		 * Much like the server's implementationThis long try catch statement sets up
		 * the key store and the SSL sockets using X509 certificates
		 * To set up the keystore I had to enter in a 'keytool' command and make a 
		 * keystore file from which the public and private keys are stored for both
		 * the clients and the server.
		 * If this were a proper chat room, then each client would have its own keystore
		 * the same would apply to the server.
		 */
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
	/**
	 * Returns the username of the client.
	 * @return - String of the username
	 */
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
		String msg = "Connection accepted " + socket.getInetAddress() + ":"+ socket.getPort();
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
			//System.out.println(e+"kk");
			//String d = doDecrypt(encryptionKey,e);
			//System.out.println(d+"kk");
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
			if (sInput != null) sInput.close();
		} catch (Exception e) {	e.printStackTrace();}
		try {
			if (sOutput != null) sOutput.close();
		} catch (Exception e) { e.printStackTrace();}
		try {
			if (socket != null) socket.close();
		} catch (Exception e) {	e.printStackTrace();}

		// inform the GUI
		if (cg != null)	cg.connectionFailed();
	}

	/**
	 * Public method for calling from outside of the client class,
	 * adds another level of security.
	 * @param message - the message to encrypt.
	 * @return - an encrypted version of the message
	 * @throws Exception
	 */
	public String doEncrypt(String message) throws Exception {
		return encrypt(message);
	}

	/**
	 * Private method for calling from outside of the client class,
	 * adds another level of security.
	 * @param message - the message to encrypt.
	 * @return - an encrypted version of the message
	 * @throws Exception
	 */
	private String encrypt(String plainText) throws Exception{
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes()));
	}

	/**
	 * Public method for calling from outside of the client class,
	 * adds another level of security.
	 * param message - the message to decrypt.
	 * @return - an decrypted version of the message
	 * @throws Exception
	 */
	public String doDecrypt(String cipher) throws Exception {
		return decrypt(cipher);
	}

	/**
	 * Private method for calling from outside of the client class,
	 * adds another level of security. It uses AES decryption techniques
	 * in order to encrypt a message. 
	 * I used java's standard encryption methods.
	 * param message - the message to decrypt.
	 * @return - an decrypted version of the message
	 * @throws Exception
	 */
	private String decrypt(String cipherText){
		String encryptedValue = "";
		try {
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, key,cipher.getParameters());
			encryptedValue = new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)));

		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			encryptedValue =  "Sorry you're not allowed see this message";
		} catch (InvalidKeyException e) {
			encryptedValue =  "Sorry you're not allowed see this message";
		} catch (IllegalBlockSizeException e) {
			encryptedValue =  "Sorry you're not allowed see this message";
		} catch (BadPaddingException e) {
			encryptedValue =  "Sorry you're not allowed see this message";
		} catch (InvalidAlgorithmParameterException e) {
			encryptedValue =  "Sorry you're not allowed see this message";
		}
		return encryptedValue;
	}

	/**
	 * Resets a client's key back to the original value.
	 */
	public void resetKey(){
		key = new SecretKeySpec(defaultKey.getBytes(), "AES/CTR/NoPadding");
	}

	/**
	 * Updates a client's key to that of the group's AES key
	 * @param newKey - the new key to add
	 */
	public void updateKey(SecretKey newKey){
		key = newKey;
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
					String newAes = msg.substring(0, 4);
					// if the message does not contain this value, then it is a
					// normal message and we just want to send the message part
					// and not the prefix
					if(!newAes.equals("AES:")){
						String[] parts = msg.split(" ");

						String newMsg = parts[2];
						newMsg = newMsg.substring(0,newMsg.length()-1);
						String msg1 = doDecrypt(newMsg);
						// if console mode print the message and add back the prompt
						if (cg == null) {
							System.out.print("> ");
						} 
						else {
							try {
								cg.append(parts[0] + " " + parts[1] + " " + msg1+"\n");
							} catch (Exception e) {	e.printStackTrace(); }
						}
					}
					// Else if it equals the reset message, then reset the keys.
					else if(msg.equals("AES:Reset")){
						resetKey();
					}
					// otherwise update the key with teh new value.
					else{
						String newKey = msg.substring(4, msg.length());
						byte[] decodedKey = Base64.getDecoder().decode(newKey);
						updateKey(new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES"));
					}

				} catch (IOException e) {
					display("Server has close the connection: " + e);
					if (cg != null)	cg.connectionFailed();
					break;
				}

				// can't happen with a String object but need the catch anyhow
				catch (ClassNotFoundException e2) {
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		}
	}
}
