/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatclient;

import com.nxkundu.client.service.ClientService;
import com.nxkundu.server.bo.Client;
import com.nxkundu.server.bo.DataPacket;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.DefaultListModel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author nxkundu
 * 
 * @email nxk161830@utdallas.edu
 * @name Nirmallya Kundu
 * 
 * CHAT SCREEN UI 
 * 
 * This class contains the main chat screen 
 * After the user logged in
 * This page is showed to the user
 * with ONLINE OFFLINE clients
 * and the chat history of the user
 * Also the user can logout from this screen
 */
public class ChatScreen extends javax.swing.JFrame implements Runnable{

    /**
     * Creates new form ChatScreent
     */
    private Client client;
    private ClientService clientService;
    
    private Thread threadClientChatScreen;
    private Thread threadUpdateFriendList;
    private Thread threadSendReceiveDataPacket;
    private Thread threadSendReceiveAllDataPacket;
    
    private Map<String, DefaultListModel<DisplayData>> mapModelChatHistory;
    DefaultListModel<String> modelOnlineFriends = null;
    DefaultListModel<String> modelOfflineFriends = null;
    
    /****************************** Constructors ******************************/
    
    /**
     * Constructor
     * When the chat Screen is called
     * it shows the
     * 
     * 1> List of ONLINE Clients
     * 2> List of OFFLINE Clients
     * 3> The Chat History of all the client
     * 
     * @param client
     * @param clientService 
     */
    public ChatScreen(Client client, ClientService clientService) {
        initComponents();
        this.client = client;
        this.clientService = clientService;
        this.txtMessage.grabFocus();
        this.LabelUser.setText(client.getUserName());
        
        modelOnlineFriends = new DefaultListModel<>();
        modelOnlineFriends.addElement(DataPacket.MESSAGE_TYPE_BROADCAST_MESSAGE);
        modelOfflineFriends = new DefaultListModel<>();
        
        listOnlineFriends.setModel(modelOnlineFriends);
        listOfflineFriends.setModel(modelOfflineFriends);
        
        
        listOnlineFriends.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    
                    listOfflineFriends.setSelectedIndex(-1);
                    
                    String chatFriend = listOnlineFriends.getSelectedValue();
                    labelChatFriend.setText(chatFriend);
                    labelChatFriendStatus.setText("Online");
                    
                    listChatHistory.setCellRenderer(new JListRendered());
                    listChatHistory.setModel(mapModelChatHistory.get(chatFriend));
                }
            }
        });
        
        listOfflineFriends.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    
                    listOnlineFriends.setSelectedIndex(-1);
                    
                    String chatFriend = listOfflineFriends.getSelectedValue();
                    labelChatFriend.setText(chatFriend);
                    
                    labelChatFriendStatus.setText((clientService.getMapAllClients().get(chatFriend)).lastSeen());
                    
                    listChatHistory.setCellRenderer(new JListRendered());
                    listChatHistory.setModel(mapModelChatHistory.get(chatFriend));
                }
            }
        });
        
        
        mapModelChatHistory = new HashMap<>();
        DefaultListModel<DisplayData> modelBroadCastMessage = new DefaultListModel<>();
        
        listChatHistory.setCellRenderer(new JListRendered());
        listChatHistory.setModel(modelBroadCastMessage);
        
        mapModelChatHistory.put(DataPacket.MESSAGE_TYPE_BROADCAST_MESSAGE, modelBroadCastMessage);
        
        threadClientChatScreen = new Thread(this, "ClientChatScreenStart");
        threadClientChatScreen.start();
        
    }
    
    @Override
    public void run() {
        
        /**
        * updateFriendListThread() - this method starts the thread threadUpdateFriendList
        * which updates the Clients as ONLINE/OFFLINE
        * based on the received From the server
        */
        updateFriendListThread();
        
        /**
        * receiveChatMessage() - this method starts the thread threadSendReceiveDataPacket
        * which receives the messages from the server of a particular
        * client whose chat is current open
        * and displays it on the respective User Client Chat Screen
        */
        receiveChatMessage();
        
        /**
        * receiveAllChatMessage() - this method starts the thread threadSendReceiveDataPacket
        * which receives all the messages from the server of a all the clients
        * EXCEPT the client whose chat is current open
        * and buffers it to display it on the client as soon as 
        * the client open that user client chat screen
        */
        receiveAllChatMessage();
    }
    
    /**
     * This method is used when the
     * Client send a Personal/Broadcast
     * Message to the Other Clients
     */
    private void sendMessage() {
        
        String message = txtMessage.getText();
        String toClient = labelChatFriend.getText();
        
        if(message.length() == 0) {
            return;
        }
        
        mapModelChatHistory.get(toClient).addElement(new DisplayData("Me: " + message));
        txtMessage.setText("");
       
        if(toClient.equals(DataPacket.MESSAGE_TYPE_BROADCAST_MESSAGE)) {
            
            message = client.getUserName() +" [BroadcastMessage]: " + message;
            clientService.sendPacket(DataPacket.MESSAGE_TYPE_BROADCAST_MESSAGE, message, "");
        }
        else {
            
            message = client.getUserName() +": " + message;
            clientService.sendPacket(DataPacket.MESSAGE_TYPE_MESSAGE, message, toClient);
        }
        
        listChatHistory.ensureIndexIsVisible(mapModelChatHistory.get(toClient).getSize());
    }
    
    /**
     * This method is used when the
     * Client send a Personal/Broadcast
     * Multimedia Message (Image File) to the Other Clients
     */
    private void sendImage() {
        
        String message = txtMessage.getText();
        String toClient = labelChatFriend.getText();
        JFileChooser fileChooser = new JFileChooser();
        
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "png", "tif"));

        if (fileChooser.showOpenDialog(rootPane) == JFileChooser.APPROVE_OPTION) {
            
            java.io.File file = fileChooser.getSelectedFile();
        
            if(toClient.equals(DataPacket.MESSAGE_TYPE_BROADCAST_MESSAGE)) {

//                clientService.sendPacket(DataPacket.MESSAGE_TYPE_BROADCAST_IMAGE, file.getPath(), toClient);
//                
//                mapModelChatHistory.get(toClient).addElement(new DisplayData("Me: ", new ImageIcon(file.getPath())));
            }
            else {
                
                clientService.sendPacket(DataPacket.MESSAGE_TYPE_IMAGE_MESSAGE, file.getPath(), toClient);
                
                mapModelChatHistory.get(toClient).addElement(new DisplayData("Me: ", new ImageIcon(file.getPath())));
            
            }
            listChatHistory.ensureIndexIsVisible(mapModelChatHistory.get(toClient).getSize());
            
        }
    }
    
    /**
     * updateFriendListThread() - this method starts the thread threadUpdateFriendList
     * which updates the Clients as ONLINE/OFFLINE
     * based on the received From the server
     */
    private void updateFriendListThread() {
        
        threadUpdateFriendList = new Thread("UpdateFriendList"){
            @Override
            public void run() {

		while(ClientService.isLoggedIn) {

                    updateFriendList();

                    try {

                            Thread.sleep(6000);
                    }
                    catch(Exception e) {

                            e.printStackTrace();
                    }
                    
		}
            }
	};

	threadUpdateFriendList.start();
    }
    
    /**
     * updateFriendList() - this method updates the Clients as ONLINE/OFFLINE
     * based on the received From the server
     */
    private void updateFriendList() {
        
        ConcurrentMap<String, Client> mapAllClients = clientService.getMapAllClients();
       
        if(mapAllClients != null && !mapAllClients.isEmpty()) {
            
            mapAllClients.remove(client.getUserName());
            
            for(String userName : mapAllClients.keySet()) {
                    
                if(mapAllClients.get(userName).isOnline()) {

                    modelOfflineFriends.removeElement(userName);
                    
                    if(!modelOnlineFriends.contains(userName)) {
                        
                        modelOnlineFriends.addElement(userName);
                    }
                }
                
                if(!mapAllClients.get(userName).isOnline()) {

                    modelOnlineFriends.removeElement(userName);
                    
                    if(!modelOfflineFriends.contains(userName)) {
                        
                        modelOfflineFriends.addElement(userName);
                    }
                }
                
                if(!mapModelChatHistory.containsKey(userName)) {
                    DefaultListModel<DisplayData> model = new DefaultListModel<>();
                    mapModelChatHistory.put(userName, model);
                }
            }
            
            listOnlineFriends.setModel(modelOnlineFriends);
            listOfflineFriends.setModel(modelOfflineFriends);
        }
        
        if(modelOnlineFriends.contains(labelChatFriend.getText())){
            listOnlineFriends.setSelectedValue(labelChatFriend.getText(), true);
        }
        else {
            listOnlineFriends.setSelectedIndex(-1);
        }
        
        if(modelOfflineFriends.contains(labelChatFriend.getText())) {
            listOfflineFriends.setSelectedValue(labelChatFriend.getText(), true);
        }
        else {
            listOfflineFriends.setSelectedIndex(-1);
        }
    }
    
    /**
     * receiveChatMessage() - this method starts the thread threadSendReceiveDataPacket
     * which receives the messages from the server of a particular
     * client whose chat is current open
     * and displays it on the respective User Client Chat Screen
     */
    private void receiveChatMessage() {
        
        threadSendReceiveDataPacket = new Thread("SendReceiveDataPacket"){
            @Override
            public void run() {
                
                while(ClientService.isLoggedIn) {
  
                    String fromClient = labelChatFriend.getText();
                    
                    if(!fromClient.equals(DataPacket.MESSAGE_TYPE_BROADCAST_MESSAGE)) {
                        
                        Map<String, ConcurrentLinkedQueue<DataPacket>> mapClientReceivedDataPacket = null;
                        mapClientReceivedDataPacket = clientService.getMapClientReceivedDataPacket();

                        ConcurrentLinkedQueue<DataPacket> qDataPacket = mapClientReceivedDataPacket.get(fromClient);

                        if(qDataPacket != null && !qDataPacket.isEmpty()) {

                            DataPacket dataPacket = qDataPacket.poll();

                            DefaultListModel<DisplayData> modelChatHistory = new DefaultListModel<>();
                            if(mapModelChatHistory.containsKey(fromClient)) {
                                modelChatHistory = mapModelChatHistory.get(fromClient);
                            }
                            
                            switch(dataPacket.getMessageType()) {
                                
                                case DataPacket.MESSAGE_TYPE_MESSAGE:
                                    modelChatHistory.addElement(new DisplayData(dataPacket.getMessage()));
                                    break;
                                    
                                case DataPacket.MESSAGE_TYPE_BROADCAST_MESSAGE:
                                    modelChatHistory.addElement(new DisplayData(dataPacket.getMessage()));
                                    break;
                                    
                                case DataPacket.MESSAGE_TYPE_IMAGE_MESSAGE:
                                    
                                    BufferedImage bufferedImage = null;
                                    try {
                                        
                                        bufferedImage = ImageIO.read(new ByteArrayInputStream(dataPacket.getByteImage()));
                                    } 
                                    catch (IOException e) {
                                        
                                        e.printStackTrace();
                                    }
                                    modelChatHistory.addElement(new DisplayData(dataPacket.getFromClient().getUserName(), (dataPacket.getByteImage())));
                                    break;
                            }
                            
                            listChatHistory.ensureIndexIsVisible(modelChatHistory.getSize());
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
        
        threadSendReceiveDataPacket.start();
        
    }
    
    /**
     * receiveAllChatMessage() - this method starts the thread threadSendReceiveDataPacket
     * which receives all the messages from the server of a all the clients
     * EXCEPT the client whose chat is current open
     * and buffers it to display it on the client as soon as 
     * the client open that user client chat screen
     */
    private void receiveAllChatMessage() {
        
        threadSendReceiveAllDataPacket = new Thread("SendReceiveAllDataPacket"){
            @Override
            public void run() {
                
                while(ClientService.isLoggedIn) {
                    
                    Map<String, ConcurrentLinkedQueue<DataPacket>> mapClientReceivedDataPacket = null;
                    mapClientReceivedDataPacket = clientService.getMapClientReceivedDataPacket();
                    
                    String exceptClient = labelChatFriend.getText();
                    
                    for(String fromClient : mapClientReceivedDataPacket.keySet()) { 
                        
                        if(fromClient.equals(exceptClient)) {
                            continue;
                        }
  
                        ConcurrentLinkedQueue<DataPacket> qDataPacket = mapClientReceivedDataPacket.get(fromClient);

                        if(qDataPacket != null && !qDataPacket.isEmpty()) {

                            DataPacket dataPacket = qDataPacket.poll();

                            DefaultListModel<DisplayData> modelChatHistory = new DefaultListModel<>();
                            if(mapModelChatHistory.containsKey(fromClient)) {
                                modelChatHistory = mapModelChatHistory.get(fromClient);
                            }
                            modelChatHistory.addElement(new DisplayData(dataPacket.getMessage()));
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
        
        threadSendReceiveAllDataPacket.start();
        
    }


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        buttonLogout = new javax.swing.JButton();
        LabelUser = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        listOnlineFriends = new javax.swing.JList<>();
        jPanel5 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        listOfflineFriends = new javax.swing.JList<>();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        listChatHistory = new javax.swing.JList<>();
        txtMessage = new javax.swing.JTextField();
        btnSend = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        labelChatFriend = new javax.swing.JLabel();
        labelChatFriendStatus = new javax.swing.JLabel();
        ButtonImage = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jLabel1.setFont(new java.awt.Font("Lucida Grande", 1, 18)); // NOI18N
        jLabel1.setText("Welcome, ");

        buttonLogout.setText("Logout");
        buttonLogout.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                buttonLogoutMouseClicked(evt);
            }
        });

        LabelUser.setFont(new java.awt.Font("Lucida Grande", 1, 18)); // NOI18N
        LabelUser.setText("User");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(LabelUser)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonLogout)
                .addGap(6, 6, 6))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(buttonLogout, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
                    .addComponent(LabelUser))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        buttonLogout.getAccessibleContext().setAccessibleName("LogoutButton");
        LabelUser.getAccessibleContext().setAccessibleName("UserLabel");

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jLabel3.setFont(new java.awt.Font("Lucida Grande", 1, 14)); // NOI18N
        jLabel3.setText("Online Friends");

        listOnlineFriends.setFont(new java.awt.Font("Lucida Grande", 1, 14)); // NOI18N
        jScrollPane2.setViewportView(listOnlineFriends);
        listOnlineFriends.getAccessibleContext().setAccessibleName("onlineList");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane2))
                .addGap(6, 6, 6))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jLabel3)
                .addGap(6, 6, 6)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 172, Short.MAX_VALUE)
                .addGap(6, 6, 6))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jLabel4.setFont(new java.awt.Font("Lucida Grande", 1, 14)); // NOI18N
        jLabel4.setText("Offline Friends");

        listOfflineFriends.setFont(new java.awt.Font("Lucida Grande", 1, 14)); // NOI18N
        jScrollPane1.setViewportView(listOfflineFriends);
        listOfflineFriends.getAccessibleContext().setAccessibleName("OfflineList");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(6, 6, 6))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jLabel4)
                .addGap(6, 6, 6)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 169, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(6, 6, 6))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jScrollPane3.setViewportView(listChatHistory);
        listChatHistory.getAccessibleContext().setAccessibleName("ChatList");

        txtMessage.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtMessageKeyPressed(evt);
            }
        });

        btnSend.setText("Send");
        btnSend.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnSendMouseClicked(evt);
            }
        });

        jLabel5.setFont(new java.awt.Font("Lucida Grande", 1, 14)); // NOI18N
        jLabel5.setText("Chat With, ");

        labelChatFriend.setFont(new java.awt.Font("Lucida Grande", 1, 14)); // NOI18N
        labelChatFriend.setText("BROADCAST_MESSAGE");

        labelChatFriendStatus.setText("Online");

        ButtonImage.setText("Image");
        ButtonImage.setMaximumSize(new java.awt.Dimension(70, 29));
        ButtonImage.setMinimumSize(new java.awt.Dimension(70, 29));
        ButtonImage.setPreferredSize(new java.awt.Dimension(75, 29));
        ButtonImage.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ButtonImageMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelChatFriend, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelChatFriendStatus))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(txtMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 269, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnSend, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(ButtonImage, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(labelChatFriend)
                    .addComponent(labelChatFriendStatus))
                .addGap(6, 6, 6)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(txtMessage)
                    .addComponent(ButtonImage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnSend, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        txtMessage.getAccessibleContext().setAccessibleName("ChatText");
        btnSend.getAccessibleContext().setAccessibleName("SendButton");
        labelChatFriend.getAccessibleContext().setAccessibleName("FriendLabel");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(10, 10, 10))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(10, 10, 10))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    /**
     * buttonLogoutMouseClicked()
     * This method handles the Logout of the User
     * @param evt 
     */
    private void buttonLogoutMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_buttonLogoutMouseClicked
        // TODO add your handling code here:
        
        clientService.logout();
        Login login = Login.getInstance();
        login.setVisible(true);
        this.setVisible(false);
    }//GEN-LAST:event_buttonLogoutMouseClicked

    private void btnSendMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSendMouseClicked
        // TODO add your handling code here:

        sendMessage();
    }//GEN-LAST:event_btnSendMouseClicked

    private void txtMessageKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtMessageKeyPressed
        // TODO add your handling code here:
        if(evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
            sendMessage();
            listChatHistory.ensureIndexIsVisible(listChatHistory.getMaxSelectionIndex());
        }
    }//GEN-LAST:event_txtMessageKeyPressed

    private void ButtonImageMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ButtonImageMouseClicked
        // TODO add your handling code here:
        
        sendImage();
    }//GEN-LAST:event_ButtonImageMouseClicked


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton ButtonImage;
    private javax.swing.JLabel LabelUser;
    private javax.swing.JButton btnSend;
    private javax.swing.JButton buttonLogout;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel labelChatFriend;
    private javax.swing.JLabel labelChatFriendStatus;
    private javax.swing.JList<DisplayData> listChatHistory;
    private javax.swing.JList<String> listOfflineFriends;
    private javax.swing.JList<String> listOnlineFriends;
    private javax.swing.JTextField txtMessage;
    // End of variables declaration//GEN-END:variables

    
}

