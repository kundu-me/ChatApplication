package com.nxkundu.server.bo;

import java.io.Serializable;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import com.google.gson.Gson;

/**
 * 
 * @author nxkundu
 *
 */
public class DataPacket implements Serializable, Cloneable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final String ACTION_TYPE_MESSAGE = "MESSAGE";
	public static final String ACTION_TYPE_SIGNUP = "SIGNUP";
	public static final String ACTION_TYPE_LOGIN = "LOGIN";
	public static final String ACTION_TYPE_LOGOUT = "LOGOUT";
	public static final String ACTION_TYPE_ONLINE = "ONLINE";
	public static final String ACTION_TYPE_ACK = "ACK";
	
	public static final String MESSAGE_TYPE_MESSAGE = "SINGLE_MESSAGE";
	public static final String MESSAGE_TYPE_MULTICAST_MESSAGE = "MULTICAST_MESSAGE";
	public static final String MESSAGE_TYPE_BROADCAST_MESSAGE = "BROADCAST_MESSAGE";
	public static final String MESSAGE_TYPE_IMAGE_MESSAGE = "IMAGE_MESSAGE";
	public static final String MESSAGE_TYPE_BROADCAST_IMAGE = "BROADCAST_IMAGE";
	
	private UUID id;
	
	private String action;
	
	private String messageType;
	
	private Client fromClient;
	private Client toClient;
	
	private String message;
	
	private String stringImage;
	
	private long timestamp;
	
	private boolean isACK;

	public String getStringImage() {
		return stringImage;
	}

	public void setStringImage(String stringImage) {
		this.stringImage = stringImage;
	}

	public boolean isACK() {
		return isACK;
	}

	public void setACK(boolean isACK) {
		this.isACK = isACK;
	}
	
	public DataPacket(Client fromClient, String action) {
		super();
		this.action = action;
		this.setId(UUID.randomUUID());
		this.setACK(false);
		this.setTimestamp(new Date().getTime());
		this.fromClient = fromClient;
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

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return "DataPacket [id=" + id + ", action=" + action + ", messageType=" + messageType + ", fromClient="
				+ fromClient + ", toClient=" + toClient + ", message=" + message + ", stringImage=" + stringImage
				+ ", timestamp=" + timestamp + ", isACK=" + isACK + "]";
	}

	public byte[] getByteImage() {
		
		return Base64.getDecoder().decode(stringImage);
	}

	public void setByteImage(byte[] byteImage) {
		
		this.stringImage = Base64.getEncoder().encodeToString(byteImage);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
}
