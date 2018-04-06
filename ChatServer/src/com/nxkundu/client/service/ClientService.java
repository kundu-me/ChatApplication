package com.nxkundu.client.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.gson.Gson;
import com.nxkundu.server.bo.Client;
import com.nxkundu.server.bo.DataPacket;
import com.nxkundu.server.bo.Server;

/**
 * 
 * @author nxkundu
 *
 */
public class ClientService implements Runnable{

	private Server server;

	private Thread threadService;
	private boolean isLoggedIn;

	private Thread threadReceive;
	private Thread threadProcessReceivedData;

	private ConcurrentLinkedQueue<DatagramPacket> qReceivedPacket;

	public ClientService() {
		super();

		isLoggedIn = false;

		qReceivedPacket = new ConcurrentLinkedQueue<>();

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
	}

	@Override
	public void run() {

		recievePacket();

		processReceivedDatagramPacket();

	}

	public void login(String userName) {

		try {

			DataPacket dataPacket = new DataPacket(DataPacket.ACTION_TYPE_LOGIN);
			dataPacket.setUserName(userName);

			byte[] data = dataPacket.toJSON().getBytes();
			DatagramPacket datagramPacket = new DatagramPacket(data, data.length, server.getInetAddress(), server.getPort());
			server.getDatagramSocket().send(datagramPacket);

		} 
		catch (IOException e) {

			e.printStackTrace();
		}

		isLoggedIn = true;
		threadService = new Thread(this, "ClientStart");
		threadService.start();
	}

	public void logout(String userName) {

		try {

			DataPacket dataPacket = new DataPacket(DataPacket.ACTION_TYPE_LOGOUT);
			dataPacket.setUserName(userName);

			byte[] data = dataPacket.toJSON().getBytes();
			DatagramPacket datagramPacket = new DatagramPacket(data, data.length, server.getInetAddress(), server.getPort());
			server.getDatagramSocket().send(datagramPacket);

		} 
		catch (IOException e) {

			e.printStackTrace();
		}

		isLoggedIn = true;
	}

	public void sendPacket(String toClientUserName) {

		try {

			DataPacket dataPacket = new DataPacket(DataPacket.ACTION_TYPE_MESSAGE);
			dataPacket.setMessageType(DataPacket.MESSAGE_TYPE_MESSAGE);
			dataPacket.setMessage(new Date().toString());

			Client toClient = new Client(toClientUserName);
			dataPacket.setToClient(toClient);

			byte[] data = dataPacket.toJSON().getBytes();
			DatagramPacket datagramPacket = new DatagramPacket(data, data.length, server.getInetAddress(), server.getPort());
			server.getDatagramSocket().send(datagramPacket);

		} 
		catch (IOException e) {

			e.printStackTrace();
		}

	}

	public void recievePacket() {

		threadReceive = new Thread("RecievePacket"){

			@Override
			public void run() {

				while(isLoggedIn) {

					System.out.println("recievePacket");
					byte[] data = new byte[1024*60];
					DatagramPacket datagramPacket = new DatagramPacket(data, data.length);

					try {

						server.getDatagramSocket().receive(datagramPacket);
						qReceivedPacket.add(datagramPacket);

					} 
					catch (IOException e) {

						e.printStackTrace();
					}
					
					try {

						Thread.sleep(100);
					}
					catch(Exception e) {

						e.printStackTrace();
					}
				}
			}
		};

		threadReceive.start();
	}

	private void processReceivedDatagramPacket() {

		threadProcessReceivedData = new Thread("ProcessRecievePacket"){

			@Override
			public void run() {

				while(isLoggedIn) {

					if(!qReceivedPacket.isEmpty()) {

						DatagramPacket datagramPacket = qReceivedPacket.poll();
						String received = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
						DataPacket dataPacket = new Gson().fromJson(received, DataPacket.class);
						System.out.println(dataPacket);
					}
				
					try {
	
						Thread.sleep(100);
					}
					catch(Exception e) {
	
						e.printStackTrace();
					}
				}
			}
		};

		threadProcessReceivedData.start();
	}

}
