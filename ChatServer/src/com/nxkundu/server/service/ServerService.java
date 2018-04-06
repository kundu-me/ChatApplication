package com.nxkundu.server.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.nxkundu.server.bo.DataPacket;
import com.nxkundu.server.bo.Server;
/**
 * 
 * @author nxkundu
 *
 */
public class ServerService implements Runnable{
	
	private Server server;
	
	private Thread threadService;
	private boolean isService;
	
	private Thread threadSend;
	private Thread threadReceive;
	
	public ServerService(){
		
		this.isService = false;
	}
	
	public void startServer() {
		
		System.out.println("Starting Server ...");
		
		try {
			
			server = Server.getInstance();
			server.startServer();
		} 
		catch (SocketException e) {
			
			e.printStackTrace();
			System.out.println("SocketException in creating the instance of server! Exiting...");
			System.exit(0);
		} 
		catch (UnknownHostException e) {
			
			e.printStackTrace();
		}
		
		threadService = new Thread(this,"StartServer");
		
		System.out.println("Server Started Successfully...");
		
		threadService.start();
	}
	
	
	@Override
	public void run() {
		
		isService = true;
		
		recievePacket();
		
	}
	
	private void sendPacket(DatagramPacket datagramPacket) {
		
		threadSend = new Thread("SendPacket"){
			
			@Override
			public void run() {
				
				try {
					
					server.getDatagramSocket().send(datagramPacket);
				} 
				catch (IOException e) {
					
					e.printStackTrace();
				}
			}
		};
		
		threadSend.start();
	}
	
	private void recievePacket() {
		
		threadReceive = new Thread("RecievePacket"){
			
			@Override
			public void run() {
				
				while(isService) {
					
					byte[] data = new byte[1024*60];
					DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
					
					try {
						
						server.getDatagramSocket().receive(datagramPacket);
						
					} 
					catch (IOException e) {

						e.printStackTrace();
					}
					
					processReceivedDatagramPacket(datagramPacket);
				}
			}
		};
		
		threadReceive.start();
	}
	
	private void processReceivedDatagramPacket(DatagramPacket datagramPacket) {
		
		String received = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
		System.out.println(received);
		System.out.println(datagramPacket.getAddress());
		System.out.println(datagramPacket.getPort());
		System.out.println(datagramPacket);
		
	}

}
