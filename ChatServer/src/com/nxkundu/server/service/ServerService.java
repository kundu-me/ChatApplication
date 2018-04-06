package com.nxkundu.server.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
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

	private ConcurrentMap<String, Client> mapOnClients;
	private ConcurrentMap<String, Client> mapOffClients;
	private ConcurrentMap<String, Client> mapAllClients;

	private ConcurrentLinkedQueue<DataPacket> qSendPacket;

	public ServerService(){

		isService = false;

		mapOnClients = new ConcurrentHashMap<>();
		mapOffClients = new ConcurrentHashMap<>();
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

	}

	private void sendPacket() {

		threadSend = new Thread("SendPacket"){

			@Override
			public void run() {

				while(isService) {

					try {

						if(!qSendPacket.isEmpty()) {

							DataPacket dataPacket = qSendPacket.poll();
							byte[] data = dataPacket.toJSON().getBytes();
							DatagramPacket datagramPacket = new DatagramPacket(data, data.length, 
									dataPacket.getToClient().getInetAddress(), dataPacket.getToClient().getPort());

							server.getDatagramSocket().send(datagramPacket);

						}
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

	private void processReceivedDatagramPacket(DatagramPacket datagramPacket) {

		String received = new String(datagramPacket.getData(), 0, datagramPacket.getLength());

		DataPacket dataPacket = new Gson().fromJson(received, DataPacket.class);
		System.out.println(dataPacket);

		InetAddress inetAddress = datagramPacket.getAddress();
		int port = datagramPacket.getPort();

		String userName = dataPacket.getUserName();
		String id = "";
		String name = "";

		Client fromClient = new Client(userName, id, name, inetAddress, port);
		
		switch(dataPacket.getAction()) {

		case DataPacket.ACTION_TYPE_LOGIN:

			updateClientOn(fromClient);
			break;

		case DataPacket.ACTION_TYPE_LOGOUT:

			updateClientOff(fromClient);
			break;

		case DataPacket.ACTION_TYPE_MESSAGE:

			dataPacket.setFromClient(fromClient);

			if(mapOnClients.containsKey((dataPacket.getToClient().getUserName()))) {
				

				Client toClient = mapOnClients.get(dataPacket.getToClient().getUserName());
				dataPacket.setToClient(toClient);
				qSendPacket.add(dataPacket);

			}
		}

	}

	private void updateClientOn(Client client) {

		System.out.println("Client Logged In : " + client.toString());

		mapOnClients.put(client.getUserName(), client);

		mapOffClients.remove(client.getUserName());
	}

	private void updateClientOff(Client client) {

		System.out.println("Client Logged Out : " + client.toString());

		mapOffClients.put(client.getUserName(), client);

		mapOnClients.remove(client.getUserName());
	}
}
