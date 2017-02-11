package Client;

import java.awt.event.*;

import Server.ClientConnection;
//import java.io.*;

public class Client implements ActionListener {

	private String m_name = null;
	private final ChatGUI m_GUI;
	private ServerConnection m_connection = null;
	ClientConnection c;

	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("Usage: java Client serverhostname serverportnumber username");
			System.exit(-1);
		}

		try {
			Client instance = new Client(args[2]);
			instance.connectToServer(args[0], Integer.parseInt(args[1]));
		} catch (NumberFormatException e) {
			System.err.println("Error: port number must be an integer.");
			System.exit(-1);
		}
	}

	private Client(String userName) {
		m_name = userName;

		// Start up GUI (runs in its own thread)
		m_GUI = new ChatGUI(this, m_name);
	}

	// Creates a new server connection
	private void connectToServer(String hostName, int port) {
		m_connection = new ServerConnection(hostName, port);
		if (m_connection.handshake(m_name)) {
			listenForServerMessages();
		} else {
			System.err.println("Unable to connect to server");
		}
	}

	private void listenForServerMessages() {
		do {
			// Receives message and displays it
			String in = m_connection.receiveChatMessage();
			m_GUI.displayMessage(in);
		} while (true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		m_connection.sendChatMessage(m_GUI.getInput());
		m_GUI.clearInput();
	}
}
