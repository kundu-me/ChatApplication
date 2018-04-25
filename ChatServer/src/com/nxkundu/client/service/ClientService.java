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
 * @email nxk161830@utdallas.edu
 * @name Nirmallya Kundu
 * 
 * ClientService - This service is initialized on the Client Side
 * When the user opens the Chat Application
 * This is a singleton class
 * 
 * These are the below methods:
 * 
 * 1> getInstance()
 * - ClientService() - singleton class
 * This Service is initialized at each individual Client Side 
 * When the client access to the Client Chat Application 
 * 
 * 2> login()
 * - This method is called by the UI Chat Application
 * when the client wants to Login
 * This basically sends the LOGIN DataPacket to the
 * server, requesting to login with username and password
 * 
 * 3> signup()
 * - This method is called by the UI Chat Application
 * when the client wants to Signup for the first Time
 * This basically sends the SIGNUP DataPacket to the
 * server, requesting to SIGNUP AND LOGIN with username and password
 * 
 * 4> logout()
 * - This method is called by the UI Chat Application
 * when the client wants to Logout
 * This basically sends the Logout DataPacket to the
 * server, requesting to Logout with username and password
 * 
 * 5> sendPacket()
 * - This method creates the DataPacket
 * by the parameters of the DataPacket and 
 * send the DataPacket to the method sendPacket(DataPacket dataPacket)
 * which handles the sending of the DataPacket to the server
 * 
 * 6> resendDataPacketIfNoACKReceived()
 * - This method runs the 
 * thread threadResendDataPacketIfNoACKReceived 
 * which Resends the data for which no ACK is received 
 * from the server after a predefined amount of time
 * 
 * 7> addReceivedDataPacket()
 * - As soon as the client receives any DataPacket
 * After reading the FROM_CLIENT in the DataPacket 
 * the DataPacket is added to the queue of the respective queue
 * which is in-turn stored in the mapClientReceivedDataPacket
 * 
 * 8> recievePacketUDP()
 * - This method runs the thread threadReceivePacketUDP
 * and continuously receives UDP DataPacket from the server and process 
 * them to find the content of the packet and perform the necessary action
 * 
 * 9> processReceivedDatagramPacket()
 * - This method takes the action on the 
 * received DataPacket based on the action field in the DataPacket
 * 
 * 10> sendPacketOnlineStatus() 
 * - This method runs the thread threadOnlineStatus and 
 * continuously send Online DataPacket to the server to notify that the client is ONLINE. 
 * When the server does not receive the  Online DataPacketfrom a client for more than 3 cycle, 
 * the server assumes that the client is OFFLINE
 * 
 * 11> sendPacketByUDP()
 *  - This methods sends the DataPacket to the 
 * server based on UDP DatagramPacket
 * 
 */
public class ClientService implements Runnable{

	/**
	 * Variable declarations
	 */
	
	/*
	 *  private Server server - holds the information about the server 
	 *  this is used when the client on the client side
	 *  connects to the server
	 *  
	 * private Client client - holds the client information
	 * 
	 * public static boolean isLoggedIn - This variable is changed to true when the server
	 * responds with Successful Logged In message
	 * 
	 * private Thread threadService - This is the main thread which runs on the Client Side
	 * 
	 * private Thread threadReceivePacketUDP - This thread continuously receives 
	 * UDP DataPacket from the server and process them to find the content of the 
	 * packet and perform the necessary action
	 * 
	 * private Thread threadOnlineStatus - THis thread continuously send Online DataPacket to the server
	 * to notify that the client is ONLINE. When the server does not receive the  Online DataPacket
	 * from a client for more than 3 cycle, the server assumes that the client is OFFLINE
	 * 
	 * private Thread threadResendDataPacketIfNoACKReceived - This thread Resends the data for which no ACK 
	 * is received from the server after a predefined amount of time
	 * 
	 * private ConcurrentMap<String, Client> mapAllClients - This map stores the list of all clients 
	 * received from the server, this is basically from where we receive the ONLINE clients and OFFLINE clients
	 * 
	 * private ConcurrentMap<UUID, DataPacket> mapSentDataPacket - This map stores all the DataPackets 
	 * that was sent to the server, and when the client receives the ACK for the DataPacket, 
	 * the respective DataPacket is removed from the map
	 * 
	 * private ConcurrentMap<UUID, DataPacket> mapReceivedDataPacket - This map stores all the DataPackets
	 * received from the server so that the client can send ACK to the server that it has 
	 * successfully received the DataPacket
	 * 
	 * private ConcurrentLinkedQueue<DataPacket> qSignupLoginLogoutDataPacket - This queue stores the
	 * LOGIN and LOGOUT DataPacket that the client sends to the server while login and logout respectively
	 * 
	 * private ConcurrentMap<String, ConcurrentLinkedQueue<DataPacket>> mapClientReceivedDataPacket - This map
	 * stores the queue of DataPacket received from the server for each individual client
	 * 
	 * private static ClientService clientService - this is used to make the ClientService class a singleton class.
	 * 
	 * 
	 */
	private Server server;
	private Client client;
	public static boolean isLoggedIn;

	private Thread threadService;
	private Thread threadReceivePacketUDP;
	private Thread threadOnlineStatus;
	private Thread threadResendDataPacketIfNoACKReceived;

	private ConcurrentMap<String, Client> mapAllClients;

	private ConcurrentMap<UUID, DataPacket> mapSentDataPacket;
	private ConcurrentMap<UUID, DataPacket> mapReceivedDataPacket;

	private ConcurrentLinkedQueue<DataPacket> qSignupLoginLogoutDataPacket;
	
	private ConcurrentMap<String, ConcurrentLinkedQueue<DataPacket>> mapClientReceivedDataPacket;
	
	private static ClientService clientService;


	/****************************** Constructors ******************************/

	private ClientService() {

		super();
		
		isLoggedIn = false;

		mapClientReceivedDataPacket = new ConcurrentHashMap<>();

		mapSentDataPacket = new ConcurrentHashMap<>();
		mapReceivedDataPacket = new ConcurrentHashMap<>();

		qSignupLoginLogoutDataPacket = new ConcurrentLinkedQueue<>();

		try {

			/*
			 * server - gives a reference to the server object
			 * 
			 * server.connectToServer() - connects to the server for the first time
			 * to send LOGIN DataPackets and the other following DataPackets
			 *  
			 */
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

	/****************************** Object Methods ******************************/
	
	/**
	 * ClientService() - singleton class
	 * This Service is initialized at each individual Client Side 
	 * When the client access to the Client Chat Application 
	 */
	public static ClientService getInstance() {

		if(clientService == null) {
			clientService = new ClientService();
		}
		
		isLoggedIn = true;
		return clientService;
	}

	@Override
	public void run() {

		/*
		 * recievePacketUDP - This method runs the thread threadReceivePacketUDP
		 * and continuously receives UDP DataPacket from the server and process 
		 * them to find the content of the packet and perform the necessary action
		 */
		recievePacketUDP();

		/*
		 * sendPacketOnlineStatus() - This method runs the thread threadOnlineStatus and 
		 * continuously send Online DataPacket to the server to notify that the client is ONLINE. 
		 * When the server does not receive the  Online DataPacketfrom a client for more than 3 cycle, 
		 * the server assumes that the client is OFFLINE
		 */ 
		sendPacketOnlineStatus();

		/*
		 * resendDataPacketIfNoACKReceived() - This method runs the 
		 * thread threadResendDataPacketIfNoACKReceived 
		 * which Resends the data for which no ACK is received 
		 * from the server after a predefined amount of time
		 */
		resendDataPacketIfNoACKReceived();

	}

	/**
	 * This method is called by the UI Chat Application
	 * when the client wants to Login
	 * This basically sends the LOGIN DataPacket to the
	 * server, requesting to login with username and password
	 * 
	 * @param userName
	 * @param password
	 */
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

	/**
	 * This method is called by the UI Chat Application
	 * when the client wants to Signup for the first Time
	 * This basically sends the SIGNUP DataPacket to the
	 * server, requesting to SIGNUP AND LOGIN with username and password
	 * 
	 * @param userName
	 * @param password
	 */
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

	/**
	 * This method is called by the UI Chat Application
	 * when the client wants to Logout
	 * This basically sends the Logout DataPacket to the
	 * server, requesting to Logout with username and password
	 * 
	 */
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
	
	/**
	 * sendPacket() - This method creates the DataPacket
	 * by the parameters of the DataPacket and 
	 * send the DataPacket to the method sendPacket(DataPacket dataPacket)
	 * which handles the sending of the DataPacket to the server
	 * 
	 * @param messageType
	 * @param message
	 * @param toClientUserName
	 */
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
	
	/**
	 * recievePacketUDP - This method runs the thread threadReceivePacketUDP
	 * and continuously receives UDP DataPacket from the server and process 
	 * them to find the content of the packet and perform the necessary action
	 */
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
	
	/**
	 * As soon as the client receives any DataPacket
	 * After reading the FROM_CLIENT in the DataPacket 
	 * the DataPacket is added to the queue of the respective queue
	 * which is in-turn stored in the mapClientReceivedDataPacket
	 * 
	 * @param dataPacket
	 */
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

	/**
	 * resendDataPacketIfNoACKReceived() - This method runs the 
	 * thread threadResendDataPacketIfNoACKReceived 
	 * which Resends the data for which no ACK is received 
	 * from the server after a predefined amount of time
	 */
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

	/**
	 * processReceivedDatagramPacket() - This method takes the action on the 
	 * received DataPacket based on the action field in the DataPacket
	 * 
	 * @param dataPacket
	 */
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

	/**
	 * sendPacketOnlineStatus() - This method runs the thread threadOnlineStatus and 
	 * continuously send Online DataPacket to the server to notify that the client is ONLINE. 
	 * When the server does not receive the  Online DataPacketfrom a client for more than 3 cycle, 
	 * the server assumes that the client is OFFLINE
	 */ 
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

	/**
	 * sendPacketByUDP() - This methods sends the DataPacket to the 
	 * server based on UDP DatagramPacket
	 * 
	 * @param dataPacket
	 * @throws IOException
	 */
	public void sendPacketByUDP(DataPacket dataPacket) throws IOException {

		InetAddress inetAddress = server.getInetAddress();
		int port = server.getPort();
		byte[] data = dataPacket.toJSON().getBytes();
		DatagramPacket datagramPacket = new DatagramPacket(data, data.length, inetAddress, port);

		server.getDatagramSocket().send(datagramPacket);
	}

	/**
	 * sendPacket() - This method decides on
	 * which method to use to send the DataPacket to the server
	 * 
	 * @param dataPacket
	 * @throws IOException
	 */
	public void sendPacket(DataPacket dataPacket) throws IOException {

		if(dataPacket.getAction().equals(DataPacket.ACTION_TYPE_MESSAGE)) {
			mapSentDataPacket.put(dataPacket.getId(), dataPacket);
		}

		sendPacketByUDP(dataPacket);

	}
	
	/****************************** Getters Setters ******************************/

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

	public void setMapAllClients(ConcurrentMap<String, Client> mapAllClients) {
		this.mapAllClients = mapAllClients;
	}

	public ConcurrentLinkedQueue<DataPacket> getqSignupLoginLogoutDataPacket() {
		return qSignupLoginLogoutDataPacket;
	}

	public void setqSignupLoginLogoutDataPacket(ConcurrentLinkedQueue<DataPacket> qSignupLoginLogoutDataPacket) {
		this.qSignupLoginLogoutDataPacket = qSignupLoginLogoutDataPacket;
	}


}
