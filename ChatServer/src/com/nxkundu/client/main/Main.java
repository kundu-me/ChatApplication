package com.nxkundu.client.main;

import java.util.Scanner;

import com.nxkundu.client.service.ClientService;
import com.nxkundu.server.bo.DataPacket;

/**
 * 
 * @author nxkundu
 *
 */
public class Main {
	
	public static void main(String[] args) {
		
		ClientService service = ClientService.getInstance();
		
		System.out.println("Enter your username: ");
		Scanner scanner = new Scanner(System.in);
		String userName = scanner.nextLine();
		service.login(userName.trim());
		
		while(true) {
			
			scanner = new Scanner(System.in);
			String line = scanner.nextLine();
			
			String command = line.substring(0, line.indexOf("@")).trim();
			String message = line.substring(line.indexOf("@") + 1, line.lastIndexOf("@")).trim();
			String to = line.substring(line.lastIndexOf("@") + 1).trim();
			
			System.out.println("[command = " + command + "] [message = " + message + "] [to = " + to + "]");
			
			if(command.equalsIgnoreCase(DataPacket.ACTION_TYPE_LOGOUT)) {
				System.out.println("Logout ...");
				service.logout();
				break;
			}
			else if(command.equalsIgnoreCase("M")) {
				service.sendPacket(DataPacket.MESSAGE_TYPE_MESSAGE, message, to);
			}
			else if(command.equalsIgnoreCase("B")) {
				service.sendPacket(DataPacket.MESSAGE_TYPE_BROADCAST_MESSAGE, message, to);
			}
		}
	}

}
