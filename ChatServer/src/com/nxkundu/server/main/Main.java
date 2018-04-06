package com.nxkundu.server.main;

import com.nxkundu.server.service.ServerService;
/**
 * 
 * @author nxkundu
 *
 */
public class Main {
	
	public static void main(String[] args) {
		
		new ServerService().startServer();
	}

}
