/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatclient;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 *
 * @author nxkundu
 * 
 * @email nxk161830@utdallas.edu
 * @name Nirmallya Kundu
 * 
 * This class is used to display the 
 * SENT/RECEIVED Messages of the client(s)
 * to the UI
 */
public class DisplayData {
    
    private String name;
    private Icon image;
    
    public DisplayData (String name) {
        
        this.name = name;
        this.image = null;
    }
    
    public DisplayData (String name, byte[] byteImage) {
        
        this.name = name;
        this.image = new ImageIcon(byteImage);
    }
    
    public DisplayData (String name, Icon image) {
        
        this.name = name;
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Icon getImage() {
        return image;
    }

    public void setImage(Icon image) {
        this.image = image;
    }
}
