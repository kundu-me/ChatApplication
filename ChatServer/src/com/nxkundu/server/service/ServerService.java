package com.nxkundu.server.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import com.google.gson.Gson;
import com.nxkundu.server.bo.Client;
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
	private Thread threadBroadcastClientStatus;

	private ConcurrentMap<String, Client> mapAllClients;

	private ConcurrentLinkedQueue<DataPacket> qSendPacket;

	public ServerService(){

		isService = false;

		mapAllClients = new ConcurrentHashMap<>();

		qSendPacket = new ConcurrentLinkedQueue<>();
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

		sendPacket();
		
		broadcastClientStatus();

	}

	private void sendPacket() {

		threadSend = new Thread("SendPacket"){

			@Override
			public void run() {

				while(isService) {

					try {

						if(!qSendPacket.isEmpty()) {

							DataPacket dataPacket = qSendPacket.poll();

							if((mapAllClients.containsKey(dataPacket.getToClient().getUserName()))
									&& (mapAllClients.get(dataPacket.getToClient().getUserName())).isOnline()) {

								byte[] data = dataPacket.toJSON().getBytes();
								DatagramPacket datagramPacket = new DatagramPacket(data, data.length, 
										dataPacket.getToClient().getInetAddress(), dataPacket.getToClient().getPort());

								server.getDatagramSocket().send(datagramPacket);
							}
							else {

								//TODO
								//write to DB
							}

						}
					} 
					catch (IOException e) {

						e.printStackTrace();
					}

					try {

						Thread.sleep(500);
					}
					catch(Exception e) {

						e.printStackTrace();
					}
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


					try {

						Thread.sleep(500);
					}
					catch(Exception e) {

						e.printStackTrace();
					}
				}
			}
		};

		threadReceive.start();
	}

	private void processReceivedDatagramPacket(DatagramPacket datagramPacket) {

		String received = new String(datagramPacket.getData(), 0, datagramPacket.getLength());

		DataPacket dataPacket = new Gson().fromJson(received, DataPacket.class);
		System.out.println(dataPacket);

		InetAddress inetAddress = datagramPacket.getAddress();
		int port = datagramPacket.getPort();

		String userName = dataPacket.getFromClient().getUserName();
		String id = "";
		String name = "";

		Client fromClient = new Client(userName, id, name, inetAddress, port);
		
		System.out.println(fromClient);

		switch(dataPacket.getAction()) {

		case DataPacket.ACTION_TYPE_LOGIN:

			fromClient.setLastSeenTimestamp(new Date().getTime());
			
			mapAllClients.put(fromClient.getUserName(), fromClient);
			
			updateClientOn(fromClient);
			//sendClientStatus(false, fromClient);
			sendClientStatus(true, fromClient);
			break;

		case DataPacket.ACTION_TYPE_LOGOUT:

			updateClientOff(fromClient);
			//sendClientStatus(false, fromClient);
			sendClientStatus(true, fromClient);
			break;

		case DataPacket.ACTION_TYPE_ONLINE:

			fromClient.setLastSeenTimestamp(new Date().getTime());
			mapAllClients.put(fromClient.getUserName(), fromClient);
			break;

		case DataPacket.ACTION_TYPE_MESSAGE:

			dataPacket.setFromClient(fromClient);

			switch (dataPacket.getMessageType()) {

			case DataPacket.MESSAGE_TYPE_MESSAGE:

				if(mapAllClients.containsKey((dataPacket.getToClient().getUserName()))) {


					Client toClient = mapAllClients.get(dataPacket.getToClient().getUserName());
					dataPacket.setToClient(toClient);
					qSendPacket.add(dataPacket);

				}
				break;

			case DataPacket.MESSAGE_TYPE_BROADCAST_MESSAGE:

				for(String key : mapAllClients.keySet()) {

					Client toClient = mapAllClients.get(key);
					if(toClient.getUserName().equalsIgnoreCase(fromClient.getUserName())) {
						continue;
					}
					DataPacket dataPacketBroadCast = new Gson().fromJson(received, DataPacket.class);
					dataPacketBroadCast.setFromClient(fromClient);
					dataPacketBroadCast.setToClient(toClient);
					qSendPacket.add(dataPacketBroadCast);
				}

				break;
				
			case DataPacket.MESSAGE_TYPE_IMAGE_MESSAGE:

				if(mapAllClients.containsKey((dataPacket.getToClient().getUserName()))) {


					Client toClient = mapAllClients.get(dataPacket.getToClient().getUserName());
					dataPacket.setToClient(toClient);
					qSendPacket.add(dataPacket);

				}
				
				break;
			}

			break;
		}

	}

	private void updateClientOn(Client client) {

		System.out.println("Client Logged In : " + client.toString());
		//TODO handle 
	}

	private void updateClientOff(Client client) {

		System.out.println("Client Logged Out : " + client.toString());
		//TODO handle 
	}

	public void sendClientStatus(boolean isSendToAllClient, Client specificClient) {
		
		try {

			Client client = new Client("server@" + server.getPort() + "");
			
			Set<String> setAllClientEmail = null;
			if(!isSendToAllClient) {
				setAllClientEmail = new HashSet<>();
				setAllClientEmail.add(specificClient.getUserName());
			}
			else {
				setAllClientEmail = mapAllClients.keySet();
			}
			
			for(String key : setAllClientEmail) {

				Client toClient = mapAllClients.get(key);
				
				if(toClient.isOnline()) {
					
					DataPacket dataPacket = new DataPacket(client, DataPacket.ACTION_TYPE_ONLINE);
					dataPacket.setToClient(toClient);

					String allClientData = new Gson().toJson(mapAllClients);
					dataPacket.setMessage(allClientData);

					byte[] data = dataPacket.toJSON().getBytes();
					DatagramPacket datagramPacket = new DatagramPacket(data, data.length, toClient.getInetAddress(), toClient.getPort());
					server.getDatagramSocket().send(datagramPacket);
				}
			}
		} 
		catch (IOException e) {

			e.printStackTrace();
		}

	}
	
	private void broadcastClientStatus() {

		threadBroadcastClientStatus = new Thread("BroadcastClientStatus"){

			@Override
			public void run() {

				while(isService) {

					Client client = new Client("server@server.com");
					sendClientStatus(true, client);

					try {

						Thread.sleep(5000);
					}
					catch(Exception e) {

						e.printStackTrace();
					}
				}
			}
		};

		threadBroadcastClientStatus.start();
	}

}
