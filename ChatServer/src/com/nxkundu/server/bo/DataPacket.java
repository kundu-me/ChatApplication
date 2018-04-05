package com.nxkundu.server.bo;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

public class DataPacket {
	
	private byte[] data;
	private InetAddress inetAddress;
	private int port;
	
	private DatagramPacket datagramPacket;
	
	public DataPacket(byte[] data, InetAddress inetAddress, int port) {
		super();
		
		this.data = data;
		this.inetAddress = inetAddress;
		this.port = port;
		
		this.datagramPacket = new DatagramPacket(data, data.length, inetAddress, port);
	}
	
	public DataPacket() {
		
		this.data = new byte[1024*60];
		this.datagramPacket = new DatagramPacket(data, data.length);
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public InetAddress getInetAddress() {
		return inetAddress;
	}

	public void setInetAddress(InetAddress inetAddress) {
		this.inetAddress = inetAddress;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public DatagramPacket getDatagramPacket() {
		return datagramPacket;
	}

	public void setDatagramPacket(DatagramPacket datagramPacket) {
		this.datagramPacket = datagramPacket;
	}

	@Override
	public String toString() {
		return "DataPacket [data=" + Arrays.toString(data) + ", inetAddress=" + inetAddress + ", port=" + port
				+ ", datagramPacket=" + datagramPacket + "]";
	}
	
	
	

}
