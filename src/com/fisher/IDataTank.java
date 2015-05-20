package com.fisher;

import com.ib.controller.Bar;

import java.util.ArrayList;

/**
 * Created by nick on 5/16/15.
 */
public interface IDataTank {
    ArrayList<Double> getClose();
    ArrayList<Double> getOpen();
    ArrayList<Double> getHigh();
    ArrayList<Double> getLow();
    ArrayList<Double> getMedian();
    void pushBar(Bar newBar);
    void removeBar(int index);

    int getReqId();
    void setReqId(int id);

    void setServerTime(long time);
    long getServerTime();

    String timeToString();



}
