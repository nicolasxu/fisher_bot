/*
 * Created by JFormDesigner on Mon Apr 27 11:40:55 CST 2015
 */

package com.fisher;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

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
    public DefaultPieDataset data;
    public JFreeChart chart;
    public ChartPanel chartPanel;

    private JScrollPane plotScrollPane;



    public mainFrame() {
        initComponents();
        this.stopButton.setEnabled(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // data
        this.data = new DefaultPieDataset();
        data.setValue("Category 1", 43.2);
        data.setValue("Category 2", 27.9);
        data.setValue("Category 3", 79.5);

        // chart
        this.chart = ChartFactory.createPieChart("Sample Pie Chart", data, true, true, false);
        this.chartPanel = new ChartPanel(this.chart);
        this.plotScrollPane = new JScrollPane();
        this.plotScrollPane.add(chartPanel);
        this.plotScrollPane.setViewportView(chartPanel);

        Container pane = this.getContentPane();
        pane.add(this.plotScrollPane, BorderLayout.SOUTH);


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
        scrollPaneLogArea = new JScrollPane();
        logArea = new JTextArea();
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

                //======== scrollPaneLogArea ========
                {
                    scrollPaneLogArea.setViewportView(logArea);
                }

                GroupLayout contentPanelLayout = new GroupLayout(contentPanel);
                contentPanel.setLayout(contentPanelLayout);
                contentPanelLayout.setHorizontalGroup(
                    contentPanelLayout.createParallelGroup()
                        .addComponent(scrollPaneLogArea, GroupLayout.DEFAULT_SIZE, 609, Short.MAX_VALUE)
                );
                contentPanelLayout.setVerticalGroup(
                    contentPanelLayout.createParallelGroup()
                        .addGroup(contentPanelLayout.createSequentialGroup()
                            .addComponent(scrollPaneLogArea, GroupLayout.PREFERRED_SIZE, 225, GroupLayout.PREFERRED_SIZE)
                            .addGap(0, 406, Short.MAX_VALUE))
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
    private JScrollPane scrollPaneLogArea;
    private JTextArea logArea;
    private JPanel buttonBar;
    private JButton startButton;
    private JButton stopButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
