package com.nxkundu.server.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;

import com.nxkundu.server.bo.DataPacket;
import com.nxkundu.server.bo.Server;

public class ServerService implements Runnable{
	
	private Server server;
	
	private Thread threadService;
	private boolean isService;
	
	private Thread threadSend;
	private Thread threadReceive;
	
	public ServerService(){
		
		this.isService = false;
	}
	
	public void startServer(int port) {
		
		System.out.println("Starting Server ...");
		
		try {
			
			this.server = new Server(port);
		} 
		catch (SocketException e) {
			
			e.printStackTrace();
			System.out.println("SocketException in creating the instance of server! Exiting...");
			System.exit(0);
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
					
					DatagramPacket datagramPacket = new DataPacket().getDatagramPacket();
					
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
		
		System.out.println(datagramPacket);
		
	}

}
