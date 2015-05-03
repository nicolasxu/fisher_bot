package com.fisher;

import com.ib.client.EClientSocket;
import com.ib.client.EWrapper;
import com.ib.client.Order;
import com.ib.controller.Bar;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by nick on 5/2/15.
 */
public class FisherBot implements IBot {


    public DataHandler m_dataHandler;

    public FisherBot(DataHandler handler) {
        this.m_dataHandler = handler;
    }


    public void calculate() {
        ArrayList<Bar> bars = m_dataHandler.m_bars;


    }
    public void decide() {

    }

}
