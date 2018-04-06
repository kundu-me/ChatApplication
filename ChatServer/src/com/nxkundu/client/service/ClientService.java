package com.nxkundu.client.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;

import com.nxkundu.server.bo.DataPacket;
import com.nxkundu.server.bo.Server;

/**
 * 
 * @author nxkundu
 *
 */
public class ClientService {
	
	public ClientService() {
		super();
	}


	public void sendPacket() {
		
		Server server = null;

		try {
			
			server = Server.getInstance();
			server.connectToServer();
			
		} 
		catch(SocketException e) {
			
			e.printStackTrace();
		} 
		catch (UnknownHostException e) {
			
			e.printStackTrace();
		}
		
		DataPacket dataPacket = new DataPacket(DataPacket.ACTION_TYPE_MESSAGE);
		dataPacket.setMessageType(DataPacket.MESSAGE_TYPE_MESSAGE);
		dataPacket.setMessage(new Date().toString());
		
		try {
			
			byte[] data = dataPacket.toJSON().getBytes();
			DatagramPacket datagramPacket = new DatagramPacket(data, data.length, server.getInetAddress(), server.getPort());
			server.getDatagramSocket().send(datagramPacket);
			
		} 
		catch (IOException e) {
		
			e.printStackTrace();
		}
		
	}

}
