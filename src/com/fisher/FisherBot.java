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

    public int m_initialLotSize;
    public int m_currentLotSize;
    public double m_profitPoint;

    public ILogger logger;

    public FisherBot(DataHandler handler, ArrayList<Double> inputData, ILogger logger) {
        this.m_dataHandler = handler;
        this.m_inputData = inputData;
        this.logger = logger;

        this.m_ssFilter = new SuperSmootherFilter();
        this.m_fisherFilter = new FisherFilter();

        this.m_smoothed = new ArrayList<Double>();
        this.m_fisher = new ArrayList<Double>();
        this.m_trigger = new ArrayList<Double>();

        this.logger = handler.m_logger;

        this.m_initialLotSize = 100000; // 100k, 1 lot
        this.m_currentLotSize = this.m_initialLotSize;
        this.m_profitPoint = 50 * 0.00001;
    }

    public void buy() {

        Order parentOrder = new Order();
        parentOrder.m_orderId    = this.m_dataHandler.m_nextValidOrderId++;
        parentOrder.m_action     = "BUY";
        parentOrder.m_orderType  = "LMT";
        parentOrder.m_transmit   = true;
        parentOrder.m_lmtPrice   = this.m_dataHandler.m_currentAskPrice;
        logger.log("current ask price: " + this.m_dataHandler.m_currentAskPrice);
        logger.log("parentOrder.m_orderId: " + parentOrder.m_orderId);
        parentOrder.m_totalQuantity = m_currentLotSize;


        Order takeProfitOrder = new Order();
        takeProfitOrder.m_orderId = this.m_dataHandler.m_nextValidOrderId++;
        takeProfitOrder.m_action  = "SELL";
        takeProfitOrder.m_orderType = "LMT";
        takeProfitOrder.m_transmit  = true;
        takeProfitOrder.m_lmtPrice  = this.m_dataHandler.m_currentAskPrice + this.m_profitPoint;
        takeProfitOrder.m_parentId  = parentOrder.m_orderId;
        takeProfitOrder.m_totalQuantity = m_currentLotSize;

        m_dataHandler.m_request.placeOrder(this.m_dataHandler.m_reqId++, this.m_dataHandler.m_contract, parentOrder);
        m_dataHandler.m_request.placeOrder(this.m_dataHandler.m_reqId, this.m_dataHandler.m_contract, takeProfitOrder);


    }

    public void sell() {

    }

    public void closeAllPosition() {

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


            if(fI < 0) {
                //
                if(fI > fIm1) {
                    // buy
                    System.out.println("buy triggered...");
                }
            } else {
                if(fI < fIm1) {
                    // sell
                    System.out.println("sell triggered...");
                }
            }


        } else {
            // check the volatility (stand dev),
            // if volatile, we can still enter market
            System.out.println("fI < 1: " + fI + " - do nothing");
        }



    }

}
