package ChatMessage;

import java.io.Serializable;

import javax.swing.JFrame;
import org.json.simple.JSONObject;

public class ChatMessage extends JFrame implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2291789599632942554L;
	private JSONObject obj = new JSONObject();

	@SuppressWarnings("unchecked")
	public ChatMessage(String name, String command, String message) {
		obj.put("name", name);
		obj.put("command", command);
		obj.put("message", message);
		obj.put("timestamp", System.currentTimeMillis());
	}

	@SuppressWarnings("unchecked")
	public ChatMessage(String name, String command, String receiver, String message) {
		obj.put("name", name);
		obj.put("command", command);
		obj.put("receiver", receiver);
		obj.put("message", message);
		obj.put("timestamp", System.currentTimeMillis());
	}

	public String getName() {
		return (String) obj.get("name");
	}

	public String getCommand() {
		return (String) obj.get("command");
	}

	public String getReceiver() {
		return (String) obj.get("receiver");
	}

	public String getMessage() {
		return (String) obj.get("message");
	}

	public String getTimeStamp() {
		return obj.get("timestamp").toString();
	}
}
