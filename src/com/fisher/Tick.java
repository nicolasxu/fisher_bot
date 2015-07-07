package com.fisher;

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
    }

}
