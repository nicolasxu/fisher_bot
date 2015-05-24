package com.fisher;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by nick on 5/24/15.
 */
public class FileLogger implements ILogger {

    public String m_firstOpenDate;
    FileWriter m_fw;
    BufferedWriter m_bw;
    String m_path;


    public FileLogger() {

        Date date = new Date();
        DateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        m_firstOpenDate = sdf.format(date);

        m_path = "/Users/nick/documents/fisher/";
        String outputFileName = m_path + sdf.format(date) + ".log";
        try {
            m_fw = new FileWriter(outputFileName, true);
            m_bw = new BufferedWriter(m_fw);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    @Override
    public void log(String text) {
        Date date = new Date();
        DateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        DateFormat sdf1 = new SimpleDateFormat("HH:mm:ss");
        String logDateString = sdf.format(date);
        System.out.println("logDateString: " + logDateString);
        BufferedWriter bwToUse = null;
        if(logDateString.compareTo(m_firstOpenDate) == 0) {
            // use old buffer writer
            bwToUse = this.m_bw;
        } else {
            // generate new buffer writer
            // update m_firstOpenDate
            m_firstOpenDate = logDateString;


            try {
                // close previous day log
                this.m_bw.close();
                this.m_fw = new FileWriter(m_path + logDateString + ".log", true);
                this.m_bw = new BufferedWriter(this.m_fw);
                bwToUse = this.m_bw;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            bwToUse.write(sdf1.format(date) + ": " + text + "\n");
            bwToUse.flush();


        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
