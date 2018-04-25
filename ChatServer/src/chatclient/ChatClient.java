/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatclient;

/**
 *
 * @author nxkundu
 * 
 * @email nxk161830@utdallas.edu
 * @name Nirmallya Kundu
 * 
 * This class contains the main method
 * for the UI Chat Application
 */
public class ChatClient {

    /**
     * @param args the command line arguments
     * 
     * This is the main method which is called 
     * when the UI Chat Application is called
     */
    public static void main(String[] args) {
        // TODO code application logic here
        
        System.out.println("Client Started ...");
        
        /*
         * This calls the Login Class
         * which has the user login and signup
         * forms.
         */
        Login login = Login.getInstance();
        login.setVisible(true);
    }
    
}
