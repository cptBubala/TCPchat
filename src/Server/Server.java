package Server;

import java.io.IOException;

//
// Source file for the server side. 
//
// Created by Sanny Syberfeldt
// Maintained by Marcus Brohede
//

import java.net.*;
//import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

public class Server {

	private static ArrayList<ClientConnection> m_connectedClients = new ArrayList<ClientConnection>();
	
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: java Server portnumber");
			System.exit(1);
		}

		int portNumber = Integer.parseInt(args[0]);
		boolean listening = true;
		System.out.println("Waiting for client messages... ");
		try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
			while (listening) {
				// Starts a new thread for each new client
				new Thread(new ClientConnection(serverSocket.accept())).start();
			}
		} catch (IOException e) {
			System.err.println("Could not listen on port " + portNumber);
			System.exit(-1);
		}
	}

	public static synchronized void broadcast(String message, String name) {
		// Sends message to all clients in ArrayList
		for (int i = 0; i < m_connectedClients.size(); i++) {
			m_connectedClients.get(i).sendMessage(message, name, "", false);
		}
	}

	public static synchronized boolean addClient(ClientConnection client) {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			if (c.hasName(client.getMName())) {
				return false; // Already exists a client with this name
			}
		}
		m_connectedClients.add(client);
		return true;
	}

	// isList is true when /list or /help is used (in sendMessage() in server
	// boolean is used to decide which message to send)
	public static synchronized void sendPrivateMessage(String message, String name, String receiver, boolean isList) {
		for (int i = 0; i < getClients().size(); i++) {
			if (getClients().get(i).hasName(receiver)) {
				m_connectedClients.get(i).sendMessage(message, name, receiver, isList);
			}
		}
	}

	public static synchronized ArrayList<ClientConnection> getClients() {
		return m_connectedClients;
	}

	public static synchronized void removeClient(String name) {
		for (int i = 0; i < getClients().size(); i++) {
			if (getClients().get(i).hasName(name)) {
				m_connectedClients.remove(i);
			}
		}
	}

}
