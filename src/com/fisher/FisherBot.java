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
    public ArrayList<Double> m_inputData;
    public ArrayList<Double> m_smoothed;

    public SuperSmootherFilter m_ssFilter;
    public FisherFilter m_fisherFilter;

    // array list of result data
    public ArrayList<Double> m_fisher;  // fisher transform result
    public ArrayList<Double> m_trigger; // fisher transform trigger

    public FisherBot(DataHandler handler, ArrayList<Double> inputData) {
        this.m_dataHandler = handler;
        this.m_inputData = inputData;

        this.m_ssFilter = new SuperSmootherFilter();
        this.m_fisherFilter = new FisherFilter();

        this.m_smoothed = new ArrayList<Double>();
        this.m_fisher = new ArrayList<Double>();
        this.m_trigger = new ArrayList<Double>();

    }


    public void calculate() {

        // go through all the filters

        this.m_ssFilter.filter(m_inputData, m_smoothed);

        this.m_fisherFilter.filter(m_smoothed, m_fisher, m_trigger);

    }
    public void decide() {
        double fI, tI; // latest fisher[i], trigger[i]
        double fIm1, tIm1; // fisher[i-1], trigger[i-1]

        fI = this.m_fisher.get(this.m_fisher.size() - 1 );
        tI = this.m_trigger.get(this.m_trigger.size() - 1);

        fIm1 = this.m_fisher.get(this.m_fisher.size() - 2);
        tIm1 = this.m_trigger.get(this.m_trigger.size() - 2);

        if(Math.abs(fI) > 1) {
            // check vol > 200

            // review 2015 May 6, 5 min data

            System.out.println("fI > 1, fI: " + fI);

            if(fI > 0) {
                if (fI < tI && fIm1 > tIm1) {
                    System.out.println("sell triggered");
                }
            } else {
                if (fI > tI && fIm1 < tIm1) {
                    System.out.println("buy triggered");
                }
            }


        } else {
            // check the volatility (stand dev),
            // if volatile, we can still enter market
            System.out.println("fI is: " + fI + " - do nothing");
        }



    }

}
