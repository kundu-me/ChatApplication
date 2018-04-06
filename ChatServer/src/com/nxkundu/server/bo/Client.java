package com.nxkundu.server.bo;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * 
 * @author nxkundu
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
	
	private InetAddress inetAddress;
	private int port;
	
	public Client(String userName) {
		super();
		this.userName = userName;
	}
	
	public Client(String userName, String id, String name, InetAddress inetAddress, int port) {
		super();
		this.id = id;
		this.name = name;
		this.inetAddress = inetAddress;
		this.port = port;
		this.userName = userName;
	}


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

	@Override
	public String toString() {
		return "Client [userName=" + userName + ", id=" + id + ", name=" + name + ", inetAddress=" + inetAddress
				+ ", port=" + port + "]";
	}
	
}
