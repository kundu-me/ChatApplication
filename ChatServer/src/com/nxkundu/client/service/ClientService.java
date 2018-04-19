package com.nxkundu.client.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
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
	public static boolean isLoggedIn;

	private Thread threadReceivePacketUDP;
	private Thread threadOnlineStatus;
	private Thread threadResendDataPacketIfNoACKReceived;

	private ConcurrentMap<String, Client> mapAllClients;

	private ConcurrentMap<String, ConcurrentLinkedQueue<DataPacket>> mapClientReceivedDataPacket;

	private ConcurrentMap<UUID, DataPacket> mapSentDataPacket;
	private ConcurrentMap<UUID, DataPacket> mapReceivedDataPacket;

	private ConcurrentLinkedQueue<DataPacket> qSignupLoginLogoutDataPacket;




	public ConcurrentMap<UUID, DataPacket> getMapSentDataPacket() {
		return mapSentDataPacket;
	}

	public void setMapSentDataPacket(ConcurrentMap<UUID, DataPacket> mapSentDataPacket) {
		this.mapSentDataPacket = mapSentDataPacket;
	}

	public ConcurrentMap<UUID, DataPacket> getMapReceivedDataPacket() {
		return mapReceivedDataPacket;
	}

	public void setMapReceivedDataPacket(ConcurrentMap<UUID, DataPacket> mapReceivedDataPacket) {
		this.mapReceivedDataPacket = mapReceivedDataPacket;
	}

	public ConcurrentMap<String, ConcurrentLinkedQueue<DataPacket>> getMapClientReceivedDataPacket() {
		return mapClientReceivedDataPacket;
	}

	public void setMapClientReceivedDataPacket(
			ConcurrentMap<String, ConcurrentLinkedQueue<DataPacket>> mapClientReceivedDataPacket) {
		this.mapClientReceivedDataPacket = mapClientReceivedDataPacket;
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

		mapClientReceivedDataPacket = new ConcurrentHashMap<>();

		mapSentDataPacket = new ConcurrentHashMap<>();
		mapReceivedDataPacket = new ConcurrentHashMap<>();

		qSignupLoginLogoutDataPacket = new ConcurrentLinkedQueue<>();

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

		threadService = new Thread(this, "ClientStart");
		threadService.start();
	}

	public static ClientService getInstance() {

		if(clientService == null) {
			clientService = new ClientService();
		}
		
		isLoggedIn = true;
		return clientService;
	}

	@Override
	public void run() {

		recievePacketUDP();

		sendPacketOnlineStatus();

		resendDataPacketIfNoACKReceived();

	}

	public void login(String userName, String password) {

		try {

			client = new Client(userName, password);
			DataPacket dataPacket = new DataPacket(client, DataPacket.ACTION_TYPE_LOGIN);

			sendPacket(dataPacket);

		} 
		catch (IOException e) {

			e.printStackTrace();
		}
	}

	public void signup(String userName, String password) {

		try {

			client = new Client(userName, password);
			DataPacket dataPacket = new DataPacket(client, DataPacket.ACTION_TYPE_SIGNUP);

			sendPacket(dataPacket);

		} 
		catch (IOException e) {

			e.printStackTrace();
		}
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

	public void resendDataPacketIfNoACKReceived() {

		threadResendDataPacketIfNoACKReceived = new Thread("ResendDataPacketIfNoACKReceived"){

			@Override
			public void run() {

				while(true) {

					if(isLoggedIn) {

						try {

							if(mapSentDataPacket.size() > 0) {

								for(UUID sentDataPacketId : mapSentDataPacket.keySet()) {

									DataPacket sentDataPacket = mapSentDataPacket.get(sentDataPacketId);

									if(sentDataPacket.getTimestamp() - new Date().getTime() > 5000) {

										sentDataPacket.setTimestamp(new Date().getTime());
										sentDataPacket.incrementTimesResentDataPacket();
										mapSentDataPacket.put(sentDataPacketId, sentDataPacket);

										sendPacket(sentDataPacket);
									}
								}
							}
						} 
						catch (IOException e) {

							e.printStackTrace();
						}
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

		threadResendDataPacketIfNoACKReceived.start();
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

	private void addReceivedDataPacket(DataPacket dataPacket) {

		ConcurrentLinkedQueue<DataPacket> qClientReceived = null;
		if(mapClientReceivedDataPacket.containsKey(dataPacket.getFromClient().getUserName())) {
			qClientReceived = mapClientReceivedDataPacket.get(dataPacket.getFromClient().getUserName());
		}
		else {
			qClientReceived = new ConcurrentLinkedQueue<>();
		}

		qClientReceived.add(dataPacket);
		mapClientReceivedDataPacket.put(dataPacket.getFromClient().getUserName(), qClientReceived);

	}

	public void recievePacketUDP() {

		threadReceivePacketUDP = new Thread("RecievePacketUDP"){

			@Override
			public void run() {

				while(true) {

					if(isLoggedIn) {

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

		case DataPacket.ACTION_TYPE_LOGIN_SUCCESS:
			System.out.println("Received Login Packet Success!");
			isLoggedIn = true;
			qSignupLoginLogoutDataPacket.add(dataPacket);
			break;

		case DataPacket.ACTION_TYPE_LOGIN_FAILED:
			System.out.println("Received Login Packet Failed!");
			isLoggedIn = false;
			qSignupLoginLogoutDataPacket.add(dataPacket);
			break;

		case DataPacket.ACTION_TYPE_SIGNUP_FAILED:
			System.out.println("Received Signup Packet Failed!");
			isLoggedIn = false;
			qSignupLoginLogoutDataPacket.add(dataPacket);
			break;

		case DataPacket.ACTION_TYPE_ONLINE:

			Type type = new TypeToken<HashMap<String, Client>>(){}.getType();
			mapAllClients =  new ConcurrentHashMap<>(new Gson().fromJson(dataPacket.getMessage(), type));

			break;

		case DataPacket.ACTION_TYPE_ACK:

			UUID dataPacketACKId = UUID.fromString(dataPacket.getMessage());

			if(mapSentDataPacket.containsKey(dataPacketACKId)) {

				mapSentDataPacket.remove(dataPacketACKId);
			}
			else {

				//Not Possible
			}

			break;


		case DataPacket.ACTION_TYPE_MESSAGE:

			DataPacket dataPacketACK = new DataPacket(dataPacket.getFromClient(), DataPacket.ACTION_TYPE_ACK);
			dataPacketACK.setMessage(dataPacket.getId().toString());

			if(mapReceivedDataPacket.containsKey(dataPacket.getId())) {

				try {

					sendPacket(dataPacketACK);

				} 
				catch (IOException e) {
					e.printStackTrace();
				}
				break;
			}

			mapReceivedDataPacket.put(dataPacket.getId(), dataPacket);
			System.out.println(dataPacket.getFromClient().getUserName() + " => " + dataPacket.getMessage());

			switch (dataPacket.getMessageType()) {

			case DataPacket.MESSAGE_TYPE_MESSAGE:

				addReceivedDataPacket(dataPacket);
				break;

			case DataPacket.MESSAGE_TYPE_BROADCAST_MESSAGE:

				addReceivedDataPacket(dataPacket);
				break;

			case DataPacket.MESSAGE_TYPE_IMAGE_MESSAGE:

				addReceivedDataPacket(dataPacket);
				break;

			}

			try {

				sendPacket(dataPacketACK);

			} 
			catch (IOException e) {
				e.printStackTrace();
			}

			break;
		}

	}

	private void sendPacketOnlineStatus() {

		threadOnlineStatus = new Thread("SendOnlineStatus"){

			@Override
			public void run() {

				while(true) {

					if(isLoggedIn) {
						
						try {

							DataPacket dataPacket = new DataPacket(client, DataPacket.ACTION_TYPE_ONLINE);

							sendPacket(dataPacket);

						} 
						catch (IOException e) {

							e.printStackTrace();
						}
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

	public void sendPacketByUDP(DataPacket dataPacket) throws IOException {

		InetAddress inetAddress = server.getInetAddress();
		int port = server.getPort();
		byte[] data = dataPacket.toJSON().getBytes();
		DatagramPacket datagramPacket = new DatagramPacket(data, data.length, inetAddress, port);

		server.getDatagramSocket().send(datagramPacket);
	}

	public void sendPacket(DataPacket dataPacket) throws IOException {

		if(dataPacket.getAction().equals(DataPacket.ACTION_TYPE_MESSAGE)) {
			mapSentDataPacket.put(dataPacket.getId(), dataPacket);
		}

		sendPacketByUDP(dataPacket);

	}

	public ConcurrentLinkedQueue<DataPacket> getqSignupLoginLogoutDataPacket() {
		return qSignupLoginLogoutDataPacket;
	}

	public void setqSignupLoginLogoutDataPacket(ConcurrentLinkedQueue<DataPacket> qSignupLoginLogoutDataPacket) {
		this.qSignupLoginLogoutDataPacket = qSignupLoginLogoutDataPacket;
	}


}
