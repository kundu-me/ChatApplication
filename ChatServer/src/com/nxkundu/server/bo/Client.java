package com.nxkundu.server.bo;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;

/**
 * 
 * @author nxkundu
 * 
 * This is the Client BO (Business Object) Java beans
 * Which holds the information about the client who logs in
 * This client object is (broadcast) / sent in
 * MessageTo Field
 * MessageFrom Field
 * as both will be client
 *
 */
public class Client implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String userName;
	private String id;
	private String name;
	private String password;
	
	private InetAddress inetAddress;
	private int port;

	private long lastSeenTimestamp;

	/****************************** Constructors ******************************/
	
	public Client(String userName) {
		super();
		this.userName = userName;
	}
	
	public Client(String userName, String password) {
		super();
		this.userName = userName;
		this.setPassword(password);
	}
	
	public Client(String userName, String id, String name, InetAddress inetAddress, int port) {
		super();
		this.id = id;
		this.name = name;
		this.inetAddress = inetAddress;
		this.port = port;
		this.userName = userName;
	}


	/****************************** Object Methods ******************************/
	
	/**
	 * This method is used to calculate when this client was last seen (last online)
	 * by calculating from the lastTimestamp field
	 * 
	 * Conditions for lastSeen:
	 * 
	 * lastSeenTimestamp < 10 seconds => ONLINE
	 * 
	 * lastSeenTimestamp > 10 seconds => OFFLINE and 
	 * returns the lastTimestamp of the client in human readable format
	 * 
	 * @return
	 */
	public String lastSeen() {
		
		long dt = new Date().getTime();
		
		long diff = dt - lastSeenTimestamp;
		
		long diffSeconds = diff / 1000 % 60;
		long diffMinutes = diff / (60 * 1000) % 60;
		long diffHours = diff / (60 * 60 * 1000) % 24;
		long diffDays = diff / (24 * 60 * 60 * 1000);
		
		String lastSeen = "online";
		
		if(diffDays > 0) {
			lastSeen = diffDays + " days ago";
		}
		else if(diffHours > 0) {
			lastSeen = diffHours + " hours ago";
		}
		else if(diffMinutes == 1) {
			lastSeen = diffMinutes + " minute ago";
		}
		else if(diffMinutes > 1) {
			lastSeen = diffMinutes + " minutes ago";
		}
		else if(diffSeconds > 10) {
			lastSeen = diffMinutes + " seconds ago";
		}
		
		return lastSeen;
		
	}

	/****************************** toString *************************************/
	@Override
	public String toString() {
		return "Client [userName=" + userName + ", id=" + id + ", name=" + name + ", inetAddress=" + inetAddress
				+ ", port=" + port + ", lastSeenTimestamp=" + lastSeenTimestamp + "]";
	}
	
	/****************************** Getters Setters ******************************/
	
	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public InetAddress getInetAddress() {
		return inetAddress;
	}


	public void setInetAddress(InetAddress inetAddress) {
		this.inetAddress = inetAddress;
	}


	public int getPort() {
		return port;
	}


	public void setPort(int port) {
		this.port = port;
	}


	public String getUserName() {
		return userName;
	}


	public void setUserName(String userName) {
		this.userName = userName;
	}
	
	public long getLastSeenTimestamp() {
		return lastSeenTimestamp;
	}

	public void setLastSeenTimestamp(long lastSeenTimestamp) {
		this.lastSeenTimestamp = lastSeenTimestamp;
	}

	

	public boolean isOnline() {
		long dt = new Date().getTime();
		
		if(dt - lastSeenTimestamp <= 9999) {
			return true;
		}
		return false;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	
	
}
