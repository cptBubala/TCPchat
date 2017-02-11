/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import ChatMessage.ChatMessage;

/**
 * 
 * @author brom
 */
public class ClientConnection extends Thread {

	static double TRANSMISSION_FAILURE_RATE = 0.3;

	private String m_name;
	private Socket m_socket;
	private ObjectOutputStream serverOutputStream = null;
	private ObjectInputStream serverInputStream = null;
	private ChatMessage inMsg = null;
	private ChatMessage outMsg = null;
	private ArrayList<String> commands = new ArrayList<String>();
	private ArrayList<String> msgtoday = new ArrayList<String>();

	public ClientConnection(Socket socket) {
		super("ServerThread");
		this.m_socket = socket;
		addCommands();
	}

	// Adds the commands to array so it's easy to display with /help in chat
	private void addCommands() {
		commands.add("To join chat -> if you wish to join again after you left: Shut down chat window and then restart client program");
		commands.add("Broadcast message to all -> type message and press enter without any command first");
		commands.add("/tell [insert username] [message] -> writes a private message to the user you've choosen");
		commands.add("/list -> lists all the participants in chat");
		commands.add("/leave [insert farewell message (optional)] -> you leave the chat");
		commands.add("/msgtoday -> shows an inspirational message in chat window");
	}

	// Translates week day int to string
	private String weekdays(int day) {
		String s_day = "";
		switch (day) {
		case 1:
			s_day = "Sunday";
			break;
		case 2:
			s_day = "Monday";
			break;
		case 3:
			s_day = "Tuesday";
			break;
		case 4:
			s_day = "Wednesday";
			break;
		case 5:
			s_day = "Thursday";
			break;
		case 6:
			s_day = "Friday";
			break;
		case 7:
			s_day = "Saturday";
			break;
		default:
			s_day = "Oops, something went wrong";
			break;
		}
		return s_day;
	}

	// Adds message of the day to arraylist so it's easy to display when
	// /msgtoday
	private String msgOfToday() {
		msgtoday.add("''A fish on the table is better than an ocean in the tub!''");
		msgtoday.add("''Many times when you are sad, you are just hungry. Eat something.''");
		msgtoday.add("''Dancing in the dark is almost like dancing in the dark.''");
		msgtoday.add("''If you know yourself, you should take time to play tennis.''");
		msgtoday.add("''Green tomatoes - Chinese adverb (324 BC).''");
		msgtoday.add("''Often times when you think there's no oxygen in a room, it's just warm.''");
		msgtoday.add("''Vasko is maybe cool''");

		Calendar c = Calendar.getInstance();
		// ArrayList is from 0-6 but weekdays 1-7, hence the -1
		return msgtoday.get(c.get(Calendar.DAY_OF_WEEK) - 1);
	}

	// Run-method for thread
	@Override
	public void run() {
		if (!handshake()) {
			try {
				m_socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// return; // if handshake doesn't work, this breaks out from method
		}
		listenForClientMessages();
		return; // When disconnected this is needed to break out from method
	}

	// Used to establish connection with client
	public boolean handshake() {
		boolean handshake = false;
		try {
			// Opens input- and outputstream
			serverOutputStream = new ObjectOutputStream(m_socket.getOutputStream());
			serverInputStream = new ObjectInputStream(m_socket.getInputStream());
			// Creates a new chatmessage ("OK" command for connected) and sends
			// it using writeObject
			outMsg = new ChatMessage("Server", "OK", "Welcome");
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			// Receives the msg containing handshake from client
			inMsg = (ChatMessage) serverInputStream.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (inMsg.getCommand().equals("Handshake")) {
			try {
				// Sends the "OK" command using writeObject
				serverOutputStream.writeObject(outMsg);
				// Sets clients name and adds client
				m_name = inMsg.getName();
				if (Server.addClient(this)) {
					handshake = true;
					// Lets everyone in chat know a new client has joined
					Server.broadcast("has joined the chat!", m_name);
					// Displays introduction message to newly connected client
					Server.sendPrivateMessage("Use /help for commands!", m_name, m_name, true);
				} else {
					handshake = false;
					System.out.println("Denied - User name already exists");
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return handshake;
	}

	private void listenForClientMessages() {
		do {
			Calendar c = Calendar.getInstance();
			boolean exist = false;
			try {
				inMsg = (ChatMessage) serverInputStream.readObject();
			} catch (ClassNotFoundException | IOException e) {
				// If inputstream is gone, this means client is gone
				// Removes client and broadcast information to all
				Server.removeClient(m_name);
				Server.broadcast("has left the chat", m_name);
				return;
			}

			// 1 means broadcast
			if (inMsg.getCommand().equals("1")) {
				Server.broadcast(inMsg.getMessage(), inMsg.getName());
				// 2 means private message
			} else if (inMsg.getCommand().equals("2")) {
				for (int i = 0; i < Server.getClients().size(); i++) {
					// Checks if receiver of the private message exists by
					// checking username
					if (Server.getClients().get(i).hasName(inMsg.getReceiver())) {
						// Sends private message to the intended receiver
						Server.sendPrivateMessage(inMsg.getMessage(), inMsg.getName(), inMsg.getReceiver(), false);
						// Send message to it self, so it can show it on
						// chat window
						sendMessage(inMsg.getMessage(), inMsg.getName(), inMsg.getReceiver(), false);
						exist = true;
					}
				}
				
				if(!exist){
					sendMessage("User doesn't exists", inMsg.getName(), inMsg.getReceiver(), true);
				}
				
				// 3 means /list (lists all participants)
			} else if (inMsg.getCommand().equals("3")) {
				Server.sendPrivateMessage("In chat now:", inMsg.getName(), inMsg.getName(), true);
				for (int i = 0; i < Server.getClients().size(); i++) {
					Server.sendPrivateMessage(Server.getClients().get(i).getMName(), inMsg.getName(), inMsg.getName(),
							true);
				}
				// 4 means /leave
			} else if (inMsg.getCommand().equals("4")) {
				Server.removeClient(m_name);
				String[] leaveArr = inMsg.getMessage().split(" ");
				String leaveTemp = "";
				for (int i = 1; i < leaveArr.length; i++) {
					leaveTemp += leaveArr[i] + " ";
				}
				// There might be a leave-message. This i broadcasted as well as
				// notice that client has left
				String leaveMsg = "has left the chat - " + leaveTemp;
				Server.broadcast(leaveMsg, m_name);
				return;
				// 5 means /help. This lists all commands
			} else if (inMsg.getCommand().equals("5")) {
				Server.sendPrivateMessage("Commands in chat:", inMsg.getName(), inMsg.getName(), true);
				for (int i = 0; i < commands.size(); i++) {
					Server.sendPrivateMessage(commands.get(i), inMsg.getName(), inMsg.getName(), true);
				}
				// 6 means /msgtoday and sends the message to the client
				// Which message is decided based on week day
			} else if (inMsg.getCommand().equals("6")) {
				Server.sendPrivateMessage(
						"It's " + weekdays(c.get(Calendar.DAY_OF_WEEK)) + " and this is the message of the day:",
						inMsg.getName(), inMsg.getName(), true);
				Server.sendPrivateMessage(msgOfToday(), inMsg.getName(), inMsg.getName(), true);
			}

		} while (true);
	}

	public void sendMessage(String message, String name, String receiver, boolean isList) {
		// if receiver is empty - it's a broadcast message
		if (receiver.isEmpty()) {
			outMsg = new ChatMessage(name, "1", receiver, message);
			// This means some sort of list (/list or /help)
		} else if (isList) {
			outMsg = new ChatMessage(name, "3", receiver, message);
			// This means private message
		} else {
			outMsg = new ChatMessage(name, "2", receiver, message);
		}

		try {
			serverOutputStream.writeObject(outMsg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean hasName(String testName) {
		return testName.equals(m_name);
	}

	// Used because getName() is method in Thread
	public String getMName() {
		return m_name;
	}
}
