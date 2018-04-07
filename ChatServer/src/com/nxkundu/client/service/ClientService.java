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
	private Client client;

	private Thread threadService;
	private boolean isLoggedIn;

	private Thread threadReceive;
	private Thread threadProcessReceivedData;
	private Thread threadOnlineStatus;

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
		
		sendPacketOnlineStatus();

		processReceivedDatagramPacket();

	}

	public void login(String userName) {

		try {

			client = new Client(userName);
			DataPacket dataPacket = new DataPacket(client, DataPacket.ACTION_TYPE_LOGIN);

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

	public void logout() {

		try {

			DataPacket dataPacket = new DataPacket(client, DataPacket.ACTION_TYPE_LOGOUT);

			byte[] data = dataPacket.toJSON().getBytes();
			DatagramPacket datagramPacket = new DatagramPacket(data, data.length, server.getInetAddress(), server.getPort());
			server.getDatagramSocket().send(datagramPacket);

		} 
		catch (IOException e) {

			e.printStackTrace();
		}

		isLoggedIn = false;
	}

	public void sendPacket(String messageType, String message, String toClientUserName) {

		try {

			DataPacket dataPacket = new DataPacket(client, DataPacket.ACTION_TYPE_MESSAGE);
			dataPacket.setMessage(message);
			
			boolean isValid = false;
			if(DataPacket.MESSAGE_TYPE_MESSAGE.equalsIgnoreCase(messageType)) {
				
				dataPacket.setMessageType(DataPacket.MESSAGE_TYPE_MESSAGE);
				Client toClient = new Client(toClientUserName);
				dataPacket.setToClient(toClient);
				
				isValid = true;
			}
			else if(DataPacket.MESSAGE_TYPE_BROADCAST_MESSAGE.equalsIgnoreCase(messageType)) {
				
				dataPacket.setMessageType(DataPacket.MESSAGE_TYPE_BROADCAST_MESSAGE);
				isValid = true;
			}
			

			if(isValid) {
				
				byte[] data = dataPacket.toJSON().getBytes();
				DatagramPacket datagramPacket = new DatagramPacket(data, data.length, server.getInetAddress(), server.getPort());
				server.getDatagramSocket().send(datagramPacket);
			}

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
						System.out.println(dataPacket.getFromClient().getUserName() + " => " + dataPacket.getMessage());
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

	private void sendPacketOnlineStatus() {

		threadOnlineStatus = new Thread("SendOnlineStatus"){

			@Override
			public void run() {

				while(isLoggedIn) {
					
					try {

						DataPacket dataPacket = new DataPacket(client, DataPacket.ACTION_TYPE_ONLINE);

						byte[] data = dataPacket.toJSON().getBytes();
						DatagramPacket datagramPacket = new DatagramPacket(data, data.length, server.getInetAddress(), server.getPort());
						server.getDatagramSocket().send(datagramPacket);

					} 
					catch (IOException e) {

						e.printStackTrace();
					}

					try {

						Thread.sleep(5000);
					}
					catch(Exception e) {

						e.printStackTrace();
					}
				}
			}
		};

		threadOnlineStatus.start();
	}

}
