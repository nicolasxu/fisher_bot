package com.fisher;

import com.ib.controller.Bar;

import java.util.ArrayList;

/**
 * Created by nick on 5/16/15.
 */
public class DataTank implements IDataTank {

    int m_reqId;


    @Override
    public ArrayList<Double> getClose() {
        return null;
    }

    @Override
    public ArrayList<Double> getOpen() {
        return null;
    }

    @Override
    public ArrayList<Double> getHigh() {
        return null;
    }

    @Override
    public ArrayList<Double> getLow() {
        return null;
    }

    @Override
    public ArrayList<Double> getMedian() {
        return null;
    }

    @Override
    public void pushBar(Bar newBar) {

    }

    @Override
    public void removeBar(int index) {

    }

    @Override
    public int getReqId() {
        return 0;
    }

    @Override
    public void setReqId(int id) {

    }

    @Override
    public void setServerTime(long time) {

    }

    @Override
    public long getServerTime() {
        return 0;
    }

    @Override
    public String timeToString() {
        return null;
    }
}
