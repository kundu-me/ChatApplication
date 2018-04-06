package com.nxkundu.client.main;

import com.nxkundu.client.service.ClientService;

/**
 * 
 * @author nxkundu
 *
 */
public class Main2 {
	
	public static void main(String[] args) {
		
		ClientService service = new ClientService();
		service.login("nxkundu2@gmail.com");
	}

}
