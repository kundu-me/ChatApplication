package com.nxkundu.server.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
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
 * @email nxk161830@utdallas.edu
 * @name Nirmallya Kundu
 * 
 * ServerService - This service is initialized on the Server Side
 * When the Server is started
 * 
 * 1> startServer()
 * - This method starts the server
 * based on the credentials on the Server object
 * 
 * 2> sendPacket() 
 * - This method runs the thread threadSend and 
 * continuously send DataPacket that were added to the Queue qSendPacket 
 * to the respective client (toClient Address in the DataPacket)
 * - 
 * 
 * 3> processReceivedDatagramPacket()
 * - This method takes the action on the 
 * received DataPacket based on the action field in the DataPacket
 * 
 * 
 * 4> sendClientStatus() 
 * - This method sends to the client(s)
 * the list of all the clients
 * Who are ONLINE and who are OFFLINE
 * 
 * 5> broadcastClientStatus()
 * - This method runs the thread threadBroadcastClientStatus and 
 * continuously send Online DataPacket (containing mapAllClients)
 * to all the clients to notify all that the client who are ONLINE and who are OFFLINE. 
 * 
 * 6> recievePacketUDP()
 * - This method runs the thread threadReceivePacketUDP
 * and continuously receives UDP DataPacket from all the logged in client
 * and process them to find the content of the packet and perform the necessary action
 * 
 * 7> sendPacketByUDP()
 * - This methods sends the DataPacket to the 
 * respective client based on UDP DatagramPacket
 * the DataPacket contains the address of the ToClient
 *
 * 
 * 8> resendDataPacketIfNoACKReceived()
 * - This method runs the 
 * thread threadResendDataPacketIfNoACKReceived 
 * which Resends the data for which no ACK is received 
 * from the respective Client after a predefined amount of time
 * 
 * 9> updateClientOn()
 * - When the client Comes ONLINE
 * read all the messages from the Database
 * that were sent to the client while they were OFFLINE
 * 
 * 10> updateClientOff()
 * - When the client goes OFFLINE
 * write all the messages to the Database
 * that were sent and received by the client 
 * while they were ONLINE
 * 11> writeToFile()
 * - This method write to the file
 * all the user credentials that is in the mapUserCred
 * 
 * 12> readFromFile()
 * - This method reads from the file
 * all the user credentials that were saved
 * 
 */
public class ServerService implements Runnable{

	/*
	 * private Server server - holds the information about the server
	 * 
	 * private boolean isService - when the server starts this flag is set to true
	 * and as long as the server is in service this holds True
	 * 
	 * private Thread threadService - This is the main thread which runs on the Server Side
	 * 
	 * private Thread threadSend- This thread continuously sends 
	 * DataPacket to all clients based on the ToClient address on the DataPacket
	 * 
	 * private Thread threadReceivePacketUDP - This thread continuously receives 
	 * UDP DataPacket from all clients and process them to find the content of the 
	 * packet and perform the necessary action
	 * 
	 * private Thread threadBroadcastClientStatus - This thread continuously  
	 * Broadcast the Map of all the clients (ONLINE, OFFLINE) in a 
	 * DataPacket to all the logged in clients 
	 * 
	 * private Thread threadResendDataPacketIfNoACKReceived - This thread Resends the data for which no ACK 
	 * is received from the client the DataPacket was sent, after a predefined amount of time
	 * 
	 * private ConcurrentMap<String, Client> mapAllClients - This map stores the list of all clients 
	 * this is basically from where we the ONLINE clients and OFFLINE clients are stored
	 * 
	 * private ConcurrentLinkedQueue<DataPacket> qSendPacket - Whenever the server wants to send or forward 
	 * or broadcast a message it adds a DataPacket to this queue
	 * (containing the ToClient and FromClient Address)
	 * 
	 * private ConcurrentMap<UUID, DataPacket> mapSentDataPacket - This map stores all the DataPackets 
	 * that was sent to the client, and when the server receives the ACK for the DataPacket, 
	 * the respective DataPacket is removed from the map
	 * 
	 * private ConcurrentMap<UUID, DataPacket> mapBufferedDataPacket- This map stores all the DataPackets
	 * that was to be sent to a client but the client is offline
	 * 
	 * private ConcurrentMap<UUID, DataPacket> mapReceivedDataPacket - This map stores all the DataPackets
	 * received from the client so that the server can send ACK to the respective client that it has 
	 * successfully received the DataPacket
	 * 
	 * private ConcurrentMap<String, String> mapUserCred - This map stores the user credentials
	 * This map is filled when a client sign up
	 * And when a client logs in this map is referred 
	 * to verify the user credentials and allow them to login
	 * 
	 * private static final String FILENAME_USER_CREDENTIALS = "UserCred.txt"
	 * This variable stores the file name which is used to write the 
	 * user credentials to the file system
	 * 
	 */
	
	private Server server;
	private boolean isService;

	private Thread threadService;
	private Thread threadSend;
	private Thread threadReceivePacketUDP;
	private Thread threadBroadcastClientStatus;
	private Thread threadResendDataPacketIfNoACKReceived;

	private ConcurrentMap<String, Client> mapAllClients;

	private ConcurrentLinkedQueue<DataPacket> qSendPacket;

	private ConcurrentMap<UUID, DataPacket> mapSentDataPacket;
	private ConcurrentMap<UUID, DataPacket> mapBufferedDataPacket;
	private ConcurrentMap<UUID, DataPacket> mapReceivedDataPacket;
	
	private ConcurrentMap<String, String> mapUserCred;
	
	private static final String FILENAME_USER_CREDENTIALS = "UserCred.txt";

	/****************************** Constructors ******************************/
	
	public ServerService(){

		isService = false;

		mapAllClients = new ConcurrentHashMap<>();

		mapSentDataPacket = new ConcurrentHashMap<>();
		mapBufferedDataPacket = new ConcurrentHashMap<>();
		mapReceivedDataPacket = new ConcurrentHashMap<>();

		qSendPacket = new ConcurrentLinkedQueue<>();
		
		mapUserCred = new ConcurrentHashMap<>();
	}
	
	/****************************** Object Methods ******************************/

	/**
	 * startServer() - This method starts the server
	 * based on the credentials on the Server object
	 * 
	 */
	public void startServer() {

		System.out.println("Starting Server ...");
		
		/*
		 * The user credentials are read from the 
		 * file FILENAME_USER_CREDENTIALS when the server starts
		 */
		readFromFile();
		
		System.out.println("List of Regeistered User: ");
		System.out.println(mapUserCred);

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
		} catch (IOException e) {

			e.printStackTrace();
		}

		threadService = new Thread(this,"StartServer");

		System.out.println("Server Started Successfully...");

		threadService.start();
	}


	@Override
	public void run() {

		isService = true;

		/*
		 * recievePacketUDP() - This method runs the thread threadReceivePacketUDP
		 * and continuously receives UDP DataPacket from all the logged in client
		 * and process them to find the content of the packet and perform the necessary action
		 */
		recievePacketUDP();

		/*
		 * sendPacket() - This method runs the thread threadSend and 
		 * continuously send DataPacket that were added to the Queue qSendPacket 
		 * to the respective client (toClient Address in the DataPacket)
		 */ 
		sendPacket();

		/*
		 * broadcastClientStatus() - This method runs the thread threadBroadcastClientStatus and 
		 * continuously send Online DataPacket (containing mapAllClients)
		 * to all the clients to notify all that the client who are ONLINE and who are OFFLINE. 
		 */ 
		broadcastClientStatus();

		/*
		 * resendDataPacketIfNoACKReceived() - This method runs the 
		 * thread threadResendDataPacketIfNoACKReceived 
		 * which Resends the data for which no ACK is received 
		 * from the respective Client after a predefined amount of time
		 */
		resendDataPacketIfNoACKReceived();

	}

	/**
	 * sendPacket() - This method runs the thread threadSend and 
	 * continuously send DataPacket that were added to the Queue qSendPacket 
	 * to the respective client (toClient Address in the DataPacket)
	 */ 
	private void sendPacket() {

		threadSend = new Thread("SendPacket"){

			@Override
			public void run() {

				while(isService) {

					try {

						if(!qSendPacket.isEmpty()) {

							DataPacket dataPacket = qSendPacket.poll();

							if(dataPacket.getAction().equals(DataPacket.ACTION_TYPE_LOGIN_SUCCESS)
									|| dataPacket.getAction().equals(DataPacket.ACTION_TYPE_LOGIN_FAILED)
									|| dataPacket.getAction().equals(DataPacket.ACTION_TYPE_SIGNUP_FAILED)) {

								System.out.println(dataPacket);
								sendPacket(dataPacket);
							}
							else {
								
								if((mapAllClients.containsKey(dataPacket.getToClient().getUserName()))
										&& (mapAllClients.get(dataPacket.getToClient().getUserName())).isOnline()) {

									sendPacket(dataPacket);
								}
								else {

									mapBufferedDataPacket.put(dataPacket.getId(), dataPacket);
									System.out.println("Buffered Packets : " + mapBufferedDataPacket);
									//mapSentDataPacket.put(dataPacket.getId(), dataPacket);
									
									//TODO WRITE TO DB
									
									/*
									 * When the Client is OFFLINE
									 * Add the Message to the Database
									 * So that when the client comes ONLINE the next time
									 * All the messages that the client received while offline
									 * can be viewed by them
									 *  
									 */
								}
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

	/**
	 * processReceivedDatagramPacket() - This method takes the action on the 
	 * received DataPacket based on the action field in the DataPacket
	 * 
	 * @param dataPacket
	 * @param fromClient
	 */
	private void processReceivedDatagramPacket(DataPacket dataPacket, Client fromClient) {

		System.out.println("Received Packet = " + dataPacket);

		switch(dataPacket.getAction()) {

		case DataPacket.ACTION_TYPE_LOGIN:

			fromClient.setLastSeenTimestamp(new Date().getTime());

			String username = dataPacket.getFromClient().getUserName();
			String password = dataPacket.getFromClient().getPassword();

			boolean isLoginSuccess = false;
			String loginMessage = "";
			if(mapAllClients.get(username) != null && mapUserCred.get(username).equals(password)) {
				
				isLoginSuccess = true;
				loginMessage = "Login Successful";
			}
			else if(mapAllClients.get(username) == null) {
				
				isLoginSuccess = false;
				loginMessage = "Failed! Email Not Registered";
				fromClient.setLastSeenTimestamp(0);
			}
			else if(mapAllClients.get(username) != null && mapUserCred.get(username).equals(password) == false) {
				
				isLoginSuccess = false;
				loginMessage = "Failed! Incorrect Password";
				fromClient.setLastSeenTimestamp(0);
			}

			fromClient.setPassword("");
			
			Client serverToClientLoginACK = new Client(Server.SERVER_USERNAME);
			DataPacket loginACKDataPacket = null;

			if(isLoginSuccess) {

				System.out.println("Login Success ... for " + fromClient);
				mapAllClients.put(fromClient.getUserName(), fromClient);

				loginACKDataPacket = new DataPacket(serverToClientLoginACK, DataPacket.ACTION_TYPE_LOGIN_SUCCESS);
				loginACKDataPacket.setToClient(fromClient);
				loginACKDataPacket.setMessage(loginMessage);
			}
			else {

				System.out.println("Login Failed ... for " + fromClient);
				loginACKDataPacket = new DataPacket(serverToClientLoginACK, DataPacket.ACTION_TYPE_LOGIN_FAILED);
				loginACKDataPacket.setToClient(fromClient);
				loginACKDataPacket.setMessage(loginMessage);
			}

			qSendPacket.add(loginACKDataPacket);

			if(isLoginSuccess) {

				updateClientOn(fromClient);
				sendClientStatus(true, fromClient);
			}
			break;

		case DataPacket.ACTION_TYPE_SIGNUP:

			fromClient.setLastSeenTimestamp(new Date().getTime());

			String usernameSignUp = dataPacket.getFromClient().getUserName();
			String passwordSignUp = dataPacket.getFromClient().getPassword();
			
			boolean isSignUpSuccess = false;
			String signUpMessage = "";
			
			if(mapAllClients.get(usernameSignUp) == null) {
				
				isSignUpSuccess = true;
				signUpMessage = "Signup Successful! Logging in";
				mapAllClients.put(fromClient.getUserName(), fromClient);
				mapUserCred.put(usernameSignUp, passwordSignUp);
				
				writeToFile();
			}
			else {
				
				isSignUpSuccess = false;
				signUpMessage = "Failed! Email Exists";
				fromClient.setLastSeenTimestamp(0);
			}

			fromClient.setPassword("");
			
			Client serverToClientSignUpACK = new Client(Server.SERVER_USERNAME);
			DataPacket signupACKDataPacket = null;

			if(isSignUpSuccess) {

				System.out.println("Signup Success ... for " + fromClient + " .. Loggin in..");

				signupACKDataPacket = new DataPacket(serverToClientSignUpACK, DataPacket.ACTION_TYPE_LOGIN_SUCCESS);
				signupACKDataPacket.setToClient(fromClient);
				signupACKDataPacket.setMessage(signUpMessage);
			}
			else {

				System.out.println("Signup Failed ... for " + fromClient);
				signupACKDataPacket = new DataPacket(serverToClientSignUpACK, DataPacket.ACTION_TYPE_SIGNUP_FAILED);
				signupACKDataPacket.setToClient(fromClient);
				signupACKDataPacket.setMessage(signUpMessage);
			}

			qSendPacket.add(signupACKDataPacket);

			if(isSignUpSuccess) {

				updateClientOn(fromClient);
				sendClientStatus(true, fromClient);
			}
			break;

		case DataPacket.ACTION_TYPE_LOGOUT:

			mapAllClients.remove(fromClient.getUserName());
			
			updateClientOff(fromClient);
			sendClientStatus(true, fromClient);
			break;

		case DataPacket.ACTION_TYPE_ONLINE:

			fromClient.setLastSeenTimestamp(new Date().getTime());
			
			if(mapAllClients.get(fromClient.getUserName()) != null) {
				
				mapAllClients.put(fromClient.getUserName(), fromClient);
			}
			
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

			Client serverToClientACK = new Client(Server.SERVER_USERNAME);
			DataPacket dataPacketACK = new DataPacket(serverToClientACK, DataPacket.ACTION_TYPE_ACK);
			dataPacketACK.setToClient(fromClient);
			dataPacketACK.setFromClient(fromClient);
			dataPacketACK.setMessage(dataPacket.getId().toString());
			qSendPacket.add(dataPacketACK);

			if(mapReceivedDataPacket.containsKey(dataPacket.getId())) {

				break;
			}

			mapReceivedDataPacket.put(dataPacket.getId(), dataPacket);

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
					DataPacket dataPacketBroadCast;
					try {

						dataPacketBroadCast = (DataPacket) dataPacket.clone();
						dataPacketBroadCast.setFromClient(fromClient);
						dataPacketBroadCast.setToClient(toClient);
						qSendPacket.add(dataPacketBroadCast);
					} 
					catch (CloneNotSupportedException e) {

						e.printStackTrace();
					}
					catch (Exception e) {

						e.printStackTrace();
					}

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

	/**
	 * 
	 * updateClientOn() - 
	 * 
	 * When the client Comes ONLINE
	 * read all the messages from the Database
	 * that were sent to the client while they were OFFLINE
	 * 
	 * @param client
	 */
	private void updateClientOn(Client client) {

		System.out.println("Client Logged In : " + client.toString());
		//TODO handle when the Client comes ONLINE
		
		/*
		 * When the client Comes ONLINE
		 * read all the messages from the Database
		 * that were sent to the client while they were OFFLINE
		 */
	}

	/**
	 * 
	 * updateClientOff() - 
	 * When the client goes OFFLINE
	 * write all the messages to the Database
	 * that were sent and received by the client 
	 * while they were ONLINE
	 * 
	 * @param client
	 */
	private void updateClientOff(Client client) {

		System.out.println("Client Logged Out : " + client.toString());
		//TODO handle when the Client comes OFFLINE
		
		/*
		 * When the client goes OFFLINE
		 * write all the messages to the Database
		 * that were sent and received by the client 
		 * while they were ONLINE
		 */
	}

	/**
	 * sendClientStatus() -
	 * This method sends to the client(s)
	 * the list of all the clients
	 * Who are ONLINE and who are OFFLINE
	 * 
	 * @param isSendToAllClient
	 * @param specificClient
	 */
	public void sendClientStatus(boolean isSendToAllClient, Client specificClient) {

		Client severToClient = new Client(Server.SERVER_USERNAME);

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

				DataPacket dataPacket = new DataPacket(severToClient, DataPacket.ACTION_TYPE_ONLINE);
				dataPacket.setToClient(toClient);

				String allClientData = new Gson().toJson(mapAllClients);
				dataPacket.setMessage(allClientData);

				qSendPacket.add(dataPacket);
			}
		}

	}

	/**
	 * 
	 * broadcastClientStatus() - This method runs the thread threadBroadcastClientStatus and 
	 * continuously send Online DataPacket (containing mapAllClients)
	 * to all the clients to notify all that the client who are ONLINE and who are OFFLINE. 
	 * 
	 */ 
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

	/**
	 * recievePacketUDP() - This method runs the thread threadReceivePacketUDP
	 * and continuously receives UDP DataPacket from all the logged in client
	 * and process them to find the content of the packet and perform the necessary action
	 * 
	 */
	private void recievePacketUDP() {

		threadReceivePacketUDP = new Thread("RecievePacketUDP"){

			@Override
			public void run() {

				while(isService) {

					byte[] data = new byte[1024*60];
					DatagramPacket datagramPacket = new DatagramPacket(data, data.length);

					try {

						server.getDatagramSocket().receive(datagramPacket);


						String received = new String(datagramPacket.getData(), 0, datagramPacket.getLength());

						DataPacket dataPacket = new Gson().fromJson(received, DataPacket.class);
						System.out.println(dataPacket);

						InetAddress inetAddress = datagramPacket.getAddress();
						int port = datagramPacket.getPort();

						System.out.println(dataPacket);
						String userName = dataPacket.getFromClient().getUserName();
						String id = "";
						String name = "";

						Client fromClient = new Client(userName, id, name, inetAddress, port);

						System.out.println(fromClient);
						processReceivedDatagramPacket(dataPacket, fromClient);

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

	/**
	 * sendPacketByUDP() - This methods sends the DataPacket to the 
	 * respective client based on UDP DatagramPacket
	 * the DataPacket contains the address of the ToClient
	 * 
	 * @param dataPacket
	 * @throws IOException
	 */
	public void sendPacketByUDP(DataPacket dataPacket) throws IOException {

		InetAddress inetAddress = dataPacket.getToClient().getInetAddress();
		int port = dataPacket.getToClient().getPort();
		byte[] data = dataPacket.toJSON().getBytes();
		DatagramPacket datagramPacket = new DatagramPacket(data, data.length, inetAddress, port);

		server.getDatagramSocket().send(datagramPacket);

	}

	/**
	 * sendPacket() - This method decides on
	 * which method to use to send the DataPacket to the client
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

	/**
	 * resendDataPacketIfNoACKReceived() - This method runs the 
	 * thread threadResendDataPacketIfNoACKReceived 
	 * which Resends the data for which no ACK is received 
	 * from the respective Client after a predefined amount of time
	 * 
	 */
	public void resendDataPacketIfNoACKReceived() {

		threadResendDataPacketIfNoACKReceived = new Thread("ResendDataPacketIfNoACKReceived"){

			@Override
			public void run() {

				while(isService) {

					if(mapSentDataPacket.size() > 0) {

						for(UUID sentDataPacketId : mapSentDataPacket.keySet()) {

							DataPacket sentDataPacket = mapSentDataPacket.get(sentDataPacketId);

							if(sentDataPacket.getTimestamp() - new Date().getTime() > 5000) {

								sentDataPacket.setTimestamp(new Date().getTime());
								sentDataPacket.incrementTimesResentDataPacket();
								mapSentDataPacket.put(sentDataPacketId, sentDataPacket);

								qSendPacket.add(sentDataPacket);
							}
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
	 * readFromFile() - 
	 * This method write to the file
	 * all the user credentials that is in the mapUserCred
	 */
	private synchronized void writeToFile() {

		try (BufferedWriter bwBufferedWriter = new BufferedWriter(new FileWriter(FILENAME_USER_CREDENTIALS))) {

			for(String username : mapUserCred.keySet()) {
				
				String content = username + "\t" + mapUserCred.get(username);

				bwBufferedWriter.write(content);
				bwBufferedWriter.newLine();
			}
			System.out.println("Write Successfully Completed");

		} catch (IOException e) {

			e.printStackTrace();

		}

	}
	
	/**
	 * readFromFile() - 
	 * This method reads from the file
	 * all the user credentials that were saved
	 */
	private synchronized void readFromFile() {

		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(FILENAME_USER_CREDENTIALS))) {

			String strCurrentLine;

			while ((strCurrentLine = bufferedReader.readLine()) != null) {
				
				String[] arrCurrentLine = strCurrentLine.split("\t");
				
				String userName = arrCurrentLine[0];
				String password = arrCurrentLine[1];
				
				Client savedClient = new Client(userName);
				mapAllClients.put(userName, savedClient);
				mapUserCred.put(userName, password);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	/****************************** Getters Setters ******************************/
	
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
}
