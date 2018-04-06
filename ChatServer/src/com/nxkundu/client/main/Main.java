package com.nxkundu.client.main;

import com.nxkundu.client.service.ClientService;

/**
 * 
 * @author nxkundu
 *
 */
public class Main {
	
	public static void main(String[] args) {
		
		new ClientService().sendPacket();
	}

}
