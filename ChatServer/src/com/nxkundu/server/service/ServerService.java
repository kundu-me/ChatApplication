package com.nxkundu.server.service;

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
 */
public class ServerService implements Runnable{

	private Server server;

	private Thread threadService;
	private boolean isService;

	private Thread threadSend;
	private Thread threadReceivePacketUDP;
	private Thread threadBroadcastClientStatus;
	private Thread threadResendDataPacketIfNoACKReceived;

	private ConcurrentMap<String, Client> mapAllClients;

	private ConcurrentLinkedQueue<DataPacket> qSendPacket;

	private ConcurrentMap<UUID, DataPacket> mapSentDataPacket;
	private ConcurrentMap<UUID, DataPacket> mapReceivedDataPacket;

	public ServerService(){

		isService = false;

		mapAllClients = new ConcurrentHashMap<>();

		mapSentDataPacket = new ConcurrentHashMap<>();
		mapReceivedDataPacket = new ConcurrentHashMap<>();

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

		recievePacketUDP();

		sendPacket();

		broadcastClientStatus();

		resendDataPacketIfNoACKReceived();

	}

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

									//TODO
									//write to DB
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


	private void processReceivedDatagramPacket(DataPacket dataPacket, Client fromClient) {

		System.out.println("Received Packet = " + dataPacket);
		
		dataPacket.setFromClient(fromClient);

		switch(dataPacket.getAction()) {

		case DataPacket.ACTION_TYPE_LOGIN:

			fromClient.setLastSeenTimestamp(new Date().getTime());

			String username = dataPacket.getFromClient().getUserName();
			String password = dataPacket.getFromClient().getPassword();

			boolean isLoginSuccess = false;
			String loginMessage = "";
			if(mapAllClients.get(username) != null) {
				
				isLoginSuccess = true;
				loginMessage = "Login Successful";
			}
			else {
				
				isLoginSuccess = false;
				loginMessage = "Failed! Email Not Registered";
				fromClient.setLastSeenTimestamp(0);
			}

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
			}
			else {
				
				isSignUpSuccess = false;
				signUpMessage = "Failed! Email Exists";
				fromClient.setLastSeenTimestamp(0);
			}

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

	private void updateClientOn(Client client) {

		System.out.println("Client Logged In : " + client.toString());
		//TODO handle 
	}

	private void updateClientOff(Client client) {

		System.out.println("Client Logged Out : " + client.toString());
		//TODO handle 
	}

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

	public void sendPacketByUDP(DataPacket dataPacket) throws IOException {

		InetAddress inetAddress = dataPacket.getToClient().getInetAddress();
		int port = dataPacket.getToClient().getPort();
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
}
