package com.nxkundu.client.service;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
	private Thread threadOnlineStatus;

	private ConcurrentMap<String, Client> mapAllClients;
	private ConcurrentSkipListSet<String> setOnClients;
	private ConcurrentSkipListSet<String> setOffClients;
	private ConcurrentMap<String, ConcurrentLinkedQueue<DataPacket>> mapClientSendReceiveDataPacket;
	
	
	public ConcurrentMap<String, ConcurrentLinkedQueue<DataPacket>> getMapClientSendReceiveDataPacket() {
		return mapClientSendReceiveDataPacket;
	}

	public void setMapClientSendReceiveDataPacket(
			ConcurrentMap<String, ConcurrentLinkedQueue<DataPacket>> mapClientSendReceiveDataPacket) {
		this.mapClientSendReceiveDataPacket = mapClientSendReceiveDataPacket;
	}

	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public Thread getThreadService() {
		return threadService;
	}

	public void setThreadService(Thread threadService) {
		this.threadService = threadService;
	}

	public boolean isLoggedIn() {
		return isLoggedIn;
	}

	public void setLoggedIn(boolean isLoggedIn) {
		this.isLoggedIn = isLoggedIn;
	}

	public Thread getThreadReceive() {
		return threadReceive;
	}

	public void setThreadReceive(Thread threadReceive) {
		this.threadReceive = threadReceive;
	}

	public Thread getThreadOnlineStatus() {
		return threadOnlineStatus;
	}

	public void setThreadOnlineStatus(Thread threadOnlineStatus) {
		this.threadOnlineStatus = threadOnlineStatus;
	}

	public static ClientService getClientService() {
		return clientService;
	}

	public static void setClientService(ClientService clientService) {
		ClientService.clientService = clientService;
	}

	public ConcurrentMap<String, Client> getMapAllClients() {
		return mapAllClients;
	}

	public void setMapAllClients(ConcurrentMap<String, Client> mapAllClients) {
		this.mapAllClients = mapAllClients;
	}

	public ConcurrentSkipListSet<String> getSetOnClients() {
		return setOnClients;
	}

	public void setSetOnClients(ConcurrentSkipListSet<String> setOnClients) {
		this.setOnClients = setOnClients;
	}

	public ConcurrentSkipListSet<String> getSetOffClients() {
		return setOffClients;
	}

	public void setSetOffClients(ConcurrentSkipListSet<String> setOffClients) {
		this.setOffClients = setOffClients;
	}

	private static ClientService clientService;

	private ClientService() {

		super();

		isLoggedIn = false;

		setOnClients = new ConcurrentSkipListSet<>();
		setOffClients = new ConcurrentSkipListSet<>();
		mapClientSendReceiveDataPacket = new ConcurrentHashMap<>();

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
	
	public static ClientService getInstance() {
		
		if(clientService == null) {
			clientService = new ClientService();
		}
		return clientService;
	}

	@Override
	public void run() {

		recievePacket();
		
		sendPacketOnlineStatus();

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
				
				//addClientSendReceiveDataPacket(dataPacket);
				
				byte[] data = dataPacket.toJSON().getBytes();
				DatagramPacket datagramPacket = new DatagramPacket(data, data.length, server.getInetAddress(), server.getPort());
				server.getDatagramSocket().send(datagramPacket);
			}

		} 
		catch (IOException e) {

			e.printStackTrace();
		}

	}

	private void addClientSendReceiveDataPacket(DataPacket dataPacket) {
		
		ConcurrentLinkedQueue<DataPacket> qClientSendReceive = null;
		if(mapClientSendReceiveDataPacket.containsKey(dataPacket.getFromClient().getUserName())) {
			qClientSendReceive = mapClientSendReceiveDataPacket.get(dataPacket.getFromClient().getUserName());
		}
		else {
			qClientSendReceive = new ConcurrentLinkedQueue<>();
		}
		
		qClientSendReceive.add(dataPacket);
		
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
						processReceivedDatagramPacket(datagramPacket);

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

		threadReceive.start();
	}

	
	private void processReceivedDatagramPacket(DatagramPacket datagramPacket) {
		
		String received = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
		DataPacket dataPacket = new Gson().fromJson(received, DataPacket.class);

		switch(dataPacket.getAction()) {

		case DataPacket.ACTION_TYPE_ONLINE:

			Type type = new TypeToken<HashMap<String, Client>>(){}.getType();
			mapAllClients =  new ConcurrentHashMap<>(new Gson().fromJson(dataPacket.getMessage(), type));
			
			for(String userName : mapAllClients.keySet()) {
				
				if(!mapClientSendReceiveDataPacket.containsKey(userName)) {
					
					ConcurrentLinkedQueue<DataPacket> qSendReceiveDataPacket = new ConcurrentLinkedQueue<>();
					mapClientSendReceiveDataPacket.put(userName, qSendReceiveDataPacket);
				}
				
				if(mapAllClients.get(userName).isOnline()) {
					
					setOnClients.add(userName);
					setOffClients.remove(userName);
				}
				else {
					
					setOnClients.remove(userName);
					setOffClients.add(userName);
				}
			}
			
			System.out.println("OnClient :=> " + setOnClients);
			System.out.println("OffClient :=> " + setOffClients);
			
			break;

		case DataPacket.ACTION_TYPE_MESSAGE:

			System.out.println(dataPacket.getFromClient().getUserName() + " => " + dataPacket.getMessage());
			
			switch (dataPacket.getMessageType()) {
			
			case DataPacket.MESSAGE_TYPE_MESSAGE:

				addClientSendReceiveDataPacket(dataPacket);
				break;
				
			case DataPacket.MESSAGE_TYPE_BROADCAST_MESSAGE:

				addClientSendReceiveDataPacket(dataPacket);
				break;
			
			case DataPacket.MESSAGE_TYPE_MULTICAST_MESSAGE:


				break;
				
			}

			break;
		}

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

						Thread.sleep(4000);
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
