/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatclient;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 *
 * @author nxkundu
 */
public class JListRendered extends DefaultListCellRenderer implements ListCellRenderer<Object> {
    
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

        //ASSIGN TO VALUE THAT IS PASSED
        DisplayData displayData = (DisplayData) value;

        setText(displayData.getName());
        setIcon(displayData.getImage());

        if(isSelected) {
            
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        }
        else {

            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        setEnabled(true);
        setFont(list.getFont());

        return this;
    }
}
