package com.nxkundu.client.bo;

import java.io.Serializable;
import java.net.DatagramSocket;
import java.net.SocketException;

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
	
	public Server(int port) throws SocketException {
		super();
		
		this.port = port;
		this.datagramSocket = new DatagramSocket(port);
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

	@Override
	public String toString() {
		return "Server [datagramSocket=" + datagramSocket + ", port=" + port + "]";
	}
	
	

}
