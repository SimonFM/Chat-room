import javax.swing.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTextField;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class Options implements Runnable{
	private JFrame frame;
	TextArea consolePanel;
	Server server;
	ArrayList<String> clients;
	
	/** @wbp.parser.entryPoint */
	public void run(){
		this.initialize();
		server.start();
	}

	/** Initialize the contents of the frame.*/
	private void initialize(){
		server = new Server(12345);
		clients = new ArrayList<String>();
		consolePanel = new TextArea();
		consolePanel.setEditable(false);
		
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 542);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		frame.setVisible(true);
		frame.setResizable(false);
		
		consolePanel.setFont(new Font("Arial", Font.PLAIN, 12));
		consolePanel.setForeground(Color.BLACK);
		consolePanel.setBackground(Color.WHITE);
		consolePanel.setBounds(0, 343, 444, 170);
		consolePanel.setText("");
		frame.getContentPane().add(consolePanel);
		
		List memberList = new List();
		memberList.setBounds(10, 41, 170, 285);
		frame.getContentPane().add(memberList);

		TextField kickTextField = new TextField();
		kickTextField.setBounds(280, 41, 138, 22);
		frame.getContentPane().add(kickTextField);

		Button kickButton = new Button("Kick");
		kickButton.setBounds(197, 41, 70, 22);
		frame.getContentPane().add(kickButton);

		Label loggedInLabel = new Label("Logged in");
		loggedInLabel.setBounds(63, 13, 62, 22);
		frame.getContentPane().add(loggedInLabel);

		TextField addTextField = new TextField();
		addTextField.setBounds(280, 82, 138, 22);
		frame.getContentPane().add(addTextField);

		Button addButton = new Button("Add");
		addButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				String s = addTextField.getText();
				if(s.equals("") || s.equals(" ") ){
					
				}
				else{
					new ClientGUI("localhost", 12345,s);
					clients.add(s);
					memberList.add(s);
					System.out.println("Hey!");
				}
			}
		});
		
		addButton.setBounds(197, 82, 70, 22);
		frame.getContentPane().add(addButton);
		
		Button enableEncryption = new Button("Enable");
		enableEncryption.setBounds(197, 175, 70, 22);
		frame.getContentPane().add(enableEncryption);
		
		Button disableEncryption = new Button("Disable");
		disableEncryption.setBounds(281, 175, 70, 22);
		frame.getContentPane().add(disableEncryption);
		
		TextField encryptionTextField = new TextField();
		encryptionTextField.setBounds(280, 219, 138, 22);
		frame.getContentPane().add(encryptionTextField);
		
		Button addToEncrypt = new Button("Add");
		
		addToEncrypt.setBounds(197, 219, 70, 22);
		frame.getContentPane().add(addToEncrypt);
		
		Label encryptionLabel = new Label("Encryption");
		encryptionLabel.setBounds(244, 139, 62, 22);
		frame.getContentPane().add(encryptionLabel);
		
		Label managementLabel = new Label("Management");
		managementLabel.setBounds(244, 13, 62, 22);
		frame.getContentPane().add(managementLabel);
	}
}
