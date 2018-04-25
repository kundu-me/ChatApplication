package com.nxkundu.server.bo;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 
 * @author nxkundu
 * 
 * @email nxk161830@utdallas.edu
 * @name Nirmallya Kundu
 * 
 * Server Class holds all the server information
 * This is a singleton class
 * As we need to create the Server object only once
 * and whenever we need server information we call get the
 * reference to the server object and retrieve the server information
 *
 * Methods:
 * 
 * 1> getInstance() - This method returns the server object if already created
 * else, create a object and return it
 * 
 * 2> startServer() -  This method starts the server at the defined port
 * 
 * 3> connectToServer() -  This method allows the Client to
 * Connect to the server
 * 
 */
public class Server implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private DatagramSocket datagramSocket;

	private int port;
	private String hostName;
	private InetAddress inetAddress;
	
	public static final String SERVER_USERNAME = "SERVER";

	public static Server server;
	
	/****************************** Constructors ********************************************/

	private Server() throws UnknownHostException {
		super();

		this.hostName = "localhost";
		this.inetAddress = InetAddress.getByName(this.hostName);
		this.port = 8005;

	}
	
	/****************************** Object Methods ******************************************/

	/**
	 * This method returns the server object if already created
	 * else, create a object and return it
	 * @return
	 * @throws UnknownHostException
	 */
	public static Server getInstance() throws UnknownHostException {

		if(server == null) {
			server = new Server();
		}

		return server;
	}

	/**
	 * This method starts the server at the defined port
	 * @throws IOException
	 */
	public void startServer() throws IOException {
		
		System.out.println("Server Started @ Port = " + this.port);
		this.datagramSocket = new DatagramSocket(this.port);
	}

	/**
	 * This method allows the Client to
	 * Connect to the server
	 * @throws IOException
	 */
	public void connectToServer() throws IOException {
		
		if(this.datagramSocket == null) {
			
			this.datagramSocket = new DatagramSocket();
		}
	}

	
	/****************************** Getters and Setters *************************************/
	
	public DatagramSocket getDatagramSocket() {
		return datagramSocket;
	}

	public void setDatagramSocket(DatagramSocket datagramSocket) {
		this.datagramSocket = datagramSocket;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public InetAddress getInetAddress() {
		return inetAddress;
	}

	public void setInetAddress(InetAddress inetAddress) {
		this.inetAddress = inetAddress;
	}
}
