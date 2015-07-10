package com.fisher;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by nick on 7/7/15.
 */
public class Tick {
    public long time; // millli seconds since 1970.01.01
    public double bid;
    public double ask;

    public Tick() {
        this.time = 0;
        this.bid = 0;
        this.ask = 0;
    }
    public Tick(long t, double b, double a) {
        this.time = t;
        this.bid  = b;
        this.ask  = a;
        System.out.println("new tick created: " + this.time + " bid: " +this.bid + " ask: " + this.ask);
        this.saveToFile();
    }
    public void saveToFile() {
        String path = "/Users/nick/Documents/fisher/";
        String fileName = "IB50DiffTick";

        Date currentDate = new Date(this.time * 1000); // since this.time is in second, not millisecond
        DateFormat df = new SimpleDateFormat("yyyyMMdd");
        fileName = fileName + df.format(currentDate) + ".csv";

        FileWriter fw = null;
        try{
            fw = new FileWriter(path + fileName, true);
            fw.write(this.time + ", " + this.bid + ", " + this.ask + "\n");
            fw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
