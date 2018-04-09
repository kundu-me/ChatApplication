package com.nxkundu.client.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import javax.imageio.ImageIO;

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

	private Thread threadReceivePacketUDP;
	private Thread threadReceivePacketTCP;
	private Thread threadOnlineStatus;

	private ConcurrentMap<String, Client> mapAllClients;
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

	private static ClientService clientService;

	private ClientService() {

		super();

		isLoggedIn = false;

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
		catch (IOException e) {
			
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

		recievePacketUDP();
		
		recievePacketTCP();

		sendPacketOnlineStatus();

	}

	public void login(String userName) {

		try {

			client = new Client(userName);
			DataPacket dataPacket = new DataPacket(client, DataPacket.ACTION_TYPE_LOGIN);

			sendPacket(dataPacket);

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

			sendPacket(dataPacket);

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
			else if(DataPacket.MESSAGE_TYPE_BROADCAST_IMAGE.equalsIgnoreCase(messageType)) {

				dataPacket.setMessageType(DataPacket.MESSAGE_TYPE_BROADCAST_IMAGE);
				System.out.println(message);
				System.out.println(new File(message).exists());
				BufferedImage bufferedImage = ImageIO.read(new File(message));
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();        
				ImageIO.write(bufferedImage, "jpg", byteArrayOutputStream);
				byteArrayOutputStream.flush();
				dataPacket.setByteImage(byteArrayOutputStream.toByteArray());
				
				
				isValid = true;
			}
			else if(DataPacket.MESSAGE_TYPE_IMAGE_MESSAGE.equalsIgnoreCase(messageType)) {

				dataPacket.setMessageType(DataPacket.MESSAGE_TYPE_IMAGE_MESSAGE);
				System.out.println(message);
				System.out.println(new File(message).exists());
				BufferedImage bufferedImage = ImageIO.read(new File(message));
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();        
				ImageIO.write(bufferedImage, "jpg", byteArrayOutputStream);
				byteArrayOutputStream.flush();
				dataPacket.setByteImage(byteArrayOutputStream.toByteArray());
				
				Client toClient = new Client(toClientUserName);
				dataPacket.setToClient(toClient);

				isValid = true;
			}


			if(isValid) {

				//addClientSendReceiveDataPacket(dataPacket);

				sendPacket(dataPacket);
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
		mapClientSendReceiveDataPacket.put(dataPacket.getFromClient().getUserName(), qClientSendReceive);

	}

	public void recievePacketUDP() {

		threadReceivePacketUDP = new Thread("RecievePacketUDP"){

			@Override
			public void run() {

				while(isLoggedIn) {

					byte[] data = new byte[1024*60];
					DatagramPacket datagramPacket = new DatagramPacket(data, data.length);

					try {

						server.getDatagramSocket().receive(datagramPacket);
						
						String received = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
						DataPacket dataPacket = new Gson().fromJson(received, DataPacket.class);
						System.out.println(dataPacket);
						
						processReceivedDatagramPacket(dataPacket);

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

		threadReceivePacketUDP.start();
	}
	
	

	private void processReceivedDatagramPacket(DataPacket dataPacket) {

		switch(dataPacket.getAction()) {

		case DataPacket.ACTION_TYPE_ONLINE:

			Type type = new TypeToken<HashMap<String, Client>>(){}.getType();
			mapAllClients =  new ConcurrentHashMap<>(new Gson().fromJson(dataPacket.getMessage(), type));

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
				
			case DataPacket.MESSAGE_TYPE_BROADCAST_IMAGE:

				addClientSendReceiveDataPacket(dataPacket);
				break;
				
			case DataPacket.MESSAGE_TYPE_IMAGE_MESSAGE:

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
								
						sendPacket(dataPacket);

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
	
	public void recievePacketTCP() {

		threadReceivePacketTCP = new Thread("RecievePacketTCP"){

			@Override
			public void run() {

				while(isLoggedIn) {

					byte[] data = new byte[1024*60];

					try {
						
						InputStream in = server.getClientSocket().getInputStream();

						in.read(data, 0, data.length);
						String received = new String(data, 0, data.length).trim();
						DataPacket dataPacket = new Gson().fromJson(received, DataPacket.class);
						System.out.println(dataPacket);
						
						processReceivedDatagramPacket(dataPacket);


					} 
					catch (Exception e) {

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

		threadReceivePacketTCP.start();
	}

	public void sendPacketByTCP(DataPacket dataPacket) throws IOException {
		
		Socket socket = server.getClientSocket();
		OutputStream out = socket.getOutputStream();
		out.write(dataPacket.toJSON().getBytes());
		out.flush();
	}

	public void sendPacketByUDP(DataPacket dataPacket) throws IOException {
	
		InetAddress inetAddress = server.getInetAddress();
		int port = server.getPort();
		byte[] data = dataPacket.toJSON().getBytes();
		DatagramPacket datagramPacket = new DatagramPacket(data, data.length, inetAddress, port);

		server.getDatagramSocket().send(datagramPacket);
	}
	
	public void sendPacket(DataPacket dataPacket) throws IOException {

		sendPacketByUDP(dataPacket);
		
//		if(dataPacket.getAction().equals(DataPacket.ACTION_TYPE_MESSAGE)) {
//			
//			sendPacketByTCP(dataPacket);
//		}
//		else if(dataPacket.getAction().equals(DataPacket.ACTION_TYPE_LOGIN)) {
//			
//			sendPacketByTCP(dataPacket);
//			
//		}
//		else {
//			
//			sendPacketByUDP(dataPacket);
//		}

	}


}
