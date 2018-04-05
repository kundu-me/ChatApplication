package com.nxkundu.server.main;

import com.nxkundu.server.service.ServerService;

public class Main {
	
	public static void main(String[] args) {
		
		new ServerService().startServer(8005);
	}

}
