/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import ChatMessage.ChatMessage;

/**
 *
 * @author brom
 */
public class ServerConnection {

	// Artificial failure rate of 30% packet loss
	static double TRANSMISSION_FAILURE_RATE = 0.3;

	private String m_name;
	private Socket m_socket = null;
	private InetAddress m_serverAddress = null;
	private int m_serverPort = -1;
	private ObjectOutputStream clientOutputStream = null;
	private ObjectInputStream clientInputStream = null;
	private ChatMessage inMsg = null;
	private ChatMessage outMsg = null;

	public ServerConnection(String hostName, int port) {
		m_serverPort = port;
		try {
			m_serverAddress = InetAddress.getByName(hostName);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// Used to establish connection to server
	public boolean handshake(String name) {
		m_name = name;
		try {
			// Opens socket
			m_socket = new Socket(m_serverAddress, m_serverPort);
			// Opens input- and outputstream
			clientOutputStream = new ObjectOutputStream(m_socket.getOutputStream());
			clientInputStream = new ObjectInputStream(m_socket.getInputStream());
			// Creates a new chatmessage (handshake for connection) and sends it using writeObject
			outMsg = new ChatMessage(name, "Handshake", "Hoppas");
			clientOutputStream.writeObject(outMsg);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		try {
			// Receives the msg containing handshake from server
			inMsg = (ChatMessage) clientInputStream.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Command "OK" means client has been added to server
		if (inMsg.getCommand().equals("OK")) {
			System.out.println("Connected");
			return true;
		} else {
			System.out.println("Sket sig");
			return false;
		}
	}

	public String receiveChatMessage() {
		String msg = "";
		try {
			// Msg received from server
			inMsg = (ChatMessage) clientInputStream.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Either someone joined chat (if) or (else) it's a broadcast
		if (inMsg.getCommand().equals("1")) {
			if (inMsg.getMessage().equals("has joined the chat!")) {
				msg = inMsg.getName() + " " + inMsg.getMessage();
			} else {
				msg = inMsg.getName() + ": " + inMsg.getMessage();
			}
			// 2 means private message
		} else if (inMsg.getCommand().equals("2")) {
			String test1 = inMsg.getMessage();
			String[] test = test1.split(" ");
			test1 = "";
			for (int i = 2; i < test.length; i++) {
				test1 += test[i] + " ";
			}
			msg = inMsg.getName() + " whispers to " + inMsg.getReceiver() + ": " + test1;
		}
		// 3 means it's a list-message from server (e.g /help listing commands)
		else if (inMsg.getCommand().equals("3")) {
			msg = inMsg.getMessage();
		}

		else if(inMsg.getMessage().contains("has left the chat")){
			try {
				m_socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// Update to return message contents
		return msg;
	}

	public void sendChatMessage(String message) {
		// Splits message to string array.
		String[] splitMsg = message.split("\\s+");
		// message.length() > 0 to allow enter in empty chat input box
		if (message.length() > 0) {
			// If incoming message starts with a / it means that it might be a
			// command
			// This is what is checked here.
			if (message.substring(0, 1).equals("/")) {
				if (splitMsg[0].equals("/tell") || splitMsg[0].equals("/Tell")) {
					String receiver = splitMsg[1];
					outMsg = new ChatMessage(m_name, "2", receiver, message);
				} else if (splitMsg[0].equals("/list") || splitMsg[0].equals("/List")) {
					outMsg = new ChatMessage(m_name, "3", message);
				} else if (splitMsg[0].equals("/leave") || splitMsg[0].equals("/Leave")) {
					outMsg = new ChatMessage(m_name, "4", message);
				} else if (splitMsg[0].equals("/help") || splitMsg[0].equals("/Help")) {
					outMsg = new ChatMessage(m_name, "5", message);
				} else if (splitMsg[0].equals("/msgtoday") || splitMsg[0].equals("/Msgtoday")) {
					outMsg = new ChatMessage(m_name, "6", message);
				} else {
					outMsg = new ChatMessage(m_name, "1", message);
				}

				// This is the broadcast
			} else {
				outMsg = new ChatMessage(m_name, "1", message);
			}
		}
		try {
			// writeObject sends chat message to server
			clientOutputStream.writeObject(outMsg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
