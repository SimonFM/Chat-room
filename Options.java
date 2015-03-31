/*
 * This class acts like an Management console for the Server
 * I made this GUI to manage the server, so from adding clients to groups 
 * and from removing them from a group.
 */

import javax.swing.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JTextField;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.*;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Options implements Runnable{
	private JFrame frame;
	Server server;
	ArrayList<ClientGUI> clients;

	/** @wbp.parser.entryPoint */
	public void run(){
		this.initialize();
		server.start();
		
	}

	/** Initialize the contents of the frame.*/
	private void initialize(){
		server = new Server(12345);
		clients = new ArrayList<ClientGUI>();

		frame = new JFrame("Management Console");
		frame.setBounds(100, 100, 450, 292);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		frame.setVisible(true);
		frame.setResizable(false);

		List memberList = new List();
		memberList.setBounds(10, 41, 170, 189);

		TextField kickTextField = new TextField();
		kickTextField.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				String s = kickTextField.getText();
				if(s.equals("") || s.equals(" ") ){}
				else{
					clients.remove(s);
					server.removeFromGroup(s);
					memberList.remove(s);
					kickTextField.setText(" ");
				}

			}});
		kickTextField.setBounds(280, 41, 138, 22);
		frame.getContentPane().add(kickTextField);

		Button kickButton = new Button("Kick");
		kickButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				String s = kickTextField.getText();
				if(s.equals("") || s.equals(" ") ){}
				else{
					clients.remove(s);
					server.removeFromGroup(s);
					memberList.remove(s);
					kickTextField.setText(" ");
				}
			}
		});
		kickButton.setBounds(197, 41, 70, 22);

		Label loggedInLabel = new Label("Logged in");
		loggedInLabel.setBounds(63, 13, 62, 22);

		TextField addTextField = new TextField();
		addTextField.setBounds(280, 82, 138, 22);
		addTextField.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				String s = addTextField.getText();
				if(s.equals("") || s.equals(" ") ){}// do nothing
				else{
					clients.add(new ClientGUI("localhost", 12345,s));
					memberList.add(s);
					addTextField.setText("");
				}

			}});

		Button addButton = new Button("Add");
		addButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				String s = addTextField.getText();
				if(s.equals("") || s.equals(" ") ){} // do nothing
				else{
					clients.add(new ClientGUI("localhost", 12345,s));
					memberList.add(s);
					addTextField.setText("");
				}
			}
		});

		addButton.setBounds(197, 82, 70, 22);
		
		TextField encryptionTextField = new TextField();
		encryptionTextField.setBounds(197, 188, 221, 22);
		encryptionTextField.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				String s = encryptionTextField.getText();
				server.addToGroup(s);
				encryptionTextField.setText(" ");
			}});

		

		Button addToEncrypt = new Button("Add");
		addToEncrypt.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				String s = encryptionTextField.getText();
				server.addToGroup(s);
				encryptionTextField.setText(" ");
			}
		});

		addToEncrypt.setBounds(197, 147, 70, 22);

		Label managementLabel = new Label("Management");
		

		managementLabel.setBounds(197, 13, 107, 22);
		frame.getContentPane().add(kickButton);
		frame.getContentPane().add(memberList);
		frame.getContentPane().add(addToEncrypt);
		frame.getContentPane().add(loggedInLabel);
		frame.getContentPane().add(addTextField);
		frame.getContentPane().add(addButton);
		frame.getContentPane().add(managementLabel);
		frame.getContentPane().add(encryptionTextField);
		
		Button RemoveFromGroupButton = new Button("Remove");
		RemoveFromGroupButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String s = encryptionTextField.getText();
				server.removeFromGroup(s);
				encryptionTextField.setText(" ");
			}
		});
		RemoveFromGroupButton.setBounds(286, 147, 71, 22);
		frame.getContentPane().add(RemoveFromGroupButton);
		frame.setResizable(false);

	}
}
