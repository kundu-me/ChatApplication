package com.nxkundu.server.bo;

import java.io.Serializable;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * 
 * @author nxkundu
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

	public static Server server;

	private Server() throws UnknownHostException {
		super();

		this.hostName = "localhost";
		this.inetAddress = InetAddress.getByName(this.hostName);
		this.port = 8005;

	}

	public static Server getInstance() throws UnknownHostException {

		if(server == null) {
			server = new Server();
		}

		return server;
	}

	public void startServer() throws SocketException {

		this.datagramSocket = new DatagramSocket(this.port);
	}

	public void connectToServer() throws SocketException {

		if(this.datagramSocket == null) {
			this.datagramSocket = new DatagramSocket();
		}
	}

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
