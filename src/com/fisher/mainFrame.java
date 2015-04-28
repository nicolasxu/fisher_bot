/*
 * Created by JFormDesigner on Mon Apr 27 11:40:55 CST 2015
 */

package com.fisher;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.GroupLayout;
import javax.swing.border.*;

/**
 * @author shenxin xu
 */
public class mainFrame extends JFrame {

    public DataHandler handler;
    public mainFrame() {
        initComponents();
        this.stopButton.setEnabled(false);
    }

    private void startButtonActionPerformed(ActionEvent e) {
        // TODO add your code here
        System.out.println("starting the application");
        this.handler = new DataHandler(); // kick start the connection in the DataHandler constructor
        this.startButton.setEnabled(false);
        this.stopButton.setEnabled(true);
    }

    private void stopButtonActionPerformed(ActionEvent e) {
        // TODO add your code here
        this.handler.m_request.eDisconnect();
        System.out.println("Disconnected...");
        this.handler = null;
        this.stopButton.setEnabled(false);
        this.startButton.setEnabled(true);
    }



    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - nick xu
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        textField1 = new JTextField();
        buttonBar = new JPanel();
        startButton = new JButton();
        stopButton = new JButton();

        //======== this ========
        setTitle("Fish Transform Trading");
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));

            // JFormDesigner evaluation mark
            dialogPane.setBorder(new javax.swing.border.CompoundBorder(
                new javax.swing.border.TitledBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 0),
                    "JFormDesigner Evaluation", javax.swing.border.TitledBorder.CENTER,
                    javax.swing.border.TitledBorder.BOTTOM, new java.awt.Font("Dialog", java.awt.Font.BOLD, 12),
                    java.awt.Color.red), dialogPane.getBorder())); dialogPane.addPropertyChangeListener(new java.beans.PropertyChangeListener(){public void propertyChange(java.beans.PropertyChangeEvent e){if("border".equals(e.getPropertyName()))throw new RuntimeException();}});

            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {

                //---- textField1 ----
                textField1.setEditable(false);

                GroupLayout contentPanelLayout = new GroupLayout(contentPanel);
                contentPanel.setLayout(contentPanelLayout);
                contentPanelLayout.setHorizontalGroup(
                    contentPanelLayout.createParallelGroup()
                        .addComponent(textField1, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 609, Short.MAX_VALUE)
                );
                contentPanelLayout.setVerticalGroup(
                    contentPanelLayout.createParallelGroup()
                        .addComponent(textField1, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 296, Short.MAX_VALUE)
                );
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 85, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};

                //---- startButton ----
                startButton.setText("Start");
                startButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        startButtonActionPerformed(e);
                    }
                });
                buttonBar.add(startButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- stopButton ----
                stopButton.setText("Stop");
                stopButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        stopButtonActionPerformed(e);
                    }
                });
                buttonBar.add(stopButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - nick xu
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JTextField textField1;
    private JPanel buttonBar;
    private JButton startButton;
    private JButton stopButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
