package com.nxkundu.server.bo;

import com.google.gson.Gson;

/**
 * 
 * @author nxkundu
 *
 */
public class DataPacket {
	
	public static final String ACTION_TYPE_MESSAGE = "MESSAGE";
	public static final String ACTION_TYPE_SIGNUP = "SIGNUP";
	public static final String ACTION_TYPE_LOGIN = "LOGIN";
	public static final String ACTION_TYPE_LOGOUT = "LOGOUT";
	
	public static final String MESSAGE_TYPE_MESSAGE = "MESSAGE";
	
	private String action;
	
	private String messageType;
	
	private Client fromClient;
	private Client toClient;
	
	private String message;
	
	private String userName;
	private String password;

	public DataPacket(String action) {
		super();
		this.action = action;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	public Client getFromClient() {
		return fromClient;
	}

	public void setFromClient(Client fromClient) {
		this.fromClient = fromClient;
	}

	public Client getToClient() {
		return toClient;
	}

	public void setToClient(Client toClient) {
		this.toClient = toClient;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	public String toJSON() {
		
		Gson gson = new Gson();
		String strJSON = gson.toJson(this);
		return strJSON;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public String toString() {
		return "DataPacket [action=" + action + ", messageType=" + messageType + ", fromClient=" + fromClient
				+ ", toClient=" + toClient + ", message=" + message + ", userName=" + userName + ", password="
				+ password + "]";
	}
	
	
	

}
