package com.nxkundu.server.main;

import com.nxkundu.server.service.ServerService;
/**
 * 
 * 
 * @author nxkundu
 * 
 * @email nxk161830@utdallas.edu
 * @name Nirmallya Kundu
 * 
 * This is the Main method that starts the Server 
 */
public class Main {
	
	public static void main(String[] args) {
		
		new ServerService().startServer();
	}

}
