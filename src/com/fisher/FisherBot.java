package com.fisher;

import com.ib.client.EWrapper;
import com.ib.client.Order;

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
    public double m_profitSize;

    public boolean m_thisBarBought;
    public boolean m_thisBarSold;
    public int m_lastBarCount;
    public double m_lowest;
    public double m_highest;

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
        this.m_profitSize = 50 * 0.00001;
        this.m_lowest = 4;
        this.m_highest = 0;

        this.m_thisBarBought = false;
        this.m_thisBarSold   = false;
    }

    public double roundTo5 (double input) {
        // 1.14042 => 1.14040
        double point = 0.00001;

        double number = Math.round(input / point / 5) * 5 * point;
        return number;
    }
    public void buy() {
        // buy at current ask price with 50 points take profit

        Order parentOrder = new Order();
        parentOrder.m_clientId    = this.m_dataHandler.m_clientId;
        //parentOrder.m_orderId is not used
        parentOrder.m_action      = "BUY";
        parentOrder.m_orderType   = "LMT";
        parentOrder.m_transmit    = false;
        parentOrder.m_tif         = "GTD";
        parentOrder.m_goodTillDate = this.m_dataHandler.toTimeString(this.m_dataHandler.m_currentServerTime + 5 * 60);
        parentOrder.m_lmtPrice    = this.roundTo5(this.m_dataHandler.m_currentAskPrice);
        logger.log("current ask price: " + this.m_dataHandler.m_currentAskPrice);
        parentOrder.m_totalQuantity = m_currentLotSize;
        int parentOrderId = this.m_dataHandler.m_reqId++;
        // placing order
        m_dataHandler.m_request.placeOrder(parentOrderId, this.m_dataHandler.m_contract, parentOrder);
        logger.log("parent orderId is: " + parentOrderId);

        // take profit order
        Order takeProfitOrder = new Order();
        takeProfitOrder.m_parentId  = parentOrderId;
        takeProfitOrder.m_clientId  = this.m_dataHandler.m_clientId;
        // orderId is not used in order.m_orderId
        takeProfitOrder.m_action    = "SELL";
        takeProfitOrder.m_orderType = "LMT";
        takeProfitOrder.m_transmit  = false;
        takeProfitOrder.m_tif         = "GTC";

        takeProfitOrder.m_auxPrice  = 0;
        takeProfitOrder.m_lmtPrice  = parentOrder.m_lmtPrice + this.m_profitSize;
        System.out.println("takeProfitOrder.m_lmtPrice: " + takeProfitOrder.m_lmtPrice);
        takeProfitOrder.m_totalQuantity = m_currentLotSize;
        m_dataHandler.m_request.placeOrder(m_dataHandler.m_reqId++, m_dataHandler.m_contract, takeProfitOrder);

        // stop loss order
        Order stopLossOrder = new Order();
        stopLossOrder.m_parentId = parentOrderId;
        stopLossOrder.m_clientId = this.m_dataHandler.m_clientId;
        stopLossOrder.m_action = "SELL";
        stopLossOrder.m_orderType = "STP";
        stopLossOrder.m_auxPrice = this.roundTo5(this.m_lowest);
        System.out.println("stopLossOrder.m_auxPrice: " + stopLossOrder.m_auxPrice);
        stopLossOrder.m_transmit = true;
        stopLossOrder.m_tif = "GTC";
        stopLossOrder.m_totalQuantity = m_currentLotSize;
        m_dataHandler.m_request.placeOrder(m_dataHandler.m_reqId++, m_dataHandler.m_contract, stopLossOrder);

        // TODO: reset the order quantity if it is more than the initial quantity

    }

    public void sell() {
        Order parentOrder = new Order();
        parentOrder.m_clientId = this.m_dataHandler.m_clientId;
        parentOrder.m_action      = "SELL";
        parentOrder.m_orderType   = "LMT";
        parentOrder.m_transmit    = false;
        parentOrder.m_tif         = "GTD";
        parentOrder.m_goodTillDate = this.m_dataHandler.toTimeString(this.m_dataHandler.m_currentServerTime + 5 * 60);
        parentOrder.m_lmtPrice    = this.roundTo5(this.m_dataHandler.m_currentBidPrice);
        logger.log("current bid price: " + this.m_dataHandler.m_currentBidPrice);
        parentOrder.m_totalQuantity = m_currentLotSize;
        int parentOrderId = this.m_dataHandler.m_reqId++;
        // place order
        m_dataHandler.m_request.placeOrder(parentOrderId, this.m_dataHandler.m_contract, parentOrder);
        logger.log("parent orderId is: " + parentOrderId);

        // take profit order
        Order takeProfitOrder = new Order();
        takeProfitOrder.m_parentId  = parentOrderId;
        takeProfitOrder.m_clientId  = this.m_dataHandler.m_clientId;
        takeProfitOrder.m_action    = "BUY";
        takeProfitOrder.m_orderType = "LMT";
        takeProfitOrder.m_transmit  = false;
        takeProfitOrder.m_tif         = "GTC";
        takeProfitOrder.m_auxPrice  = 0;
        takeProfitOrder.m_lmtPrice  = parentOrder.m_lmtPrice - this.m_profitSize;
        takeProfitOrder.m_totalQuantity = m_currentLotSize;
        m_dataHandler.m_request.placeOrder(m_dataHandler.m_reqId++, m_dataHandler.m_contract, takeProfitOrder);

        // stop loss order

        Order stopLossOrder = new Order();
        stopLossOrder.m_parentId = parentOrderId;
        stopLossOrder.m_clientId = this.m_dataHandler.m_clientId;
        stopLossOrder.m_action = "BUY";
        stopLossOrder.m_orderType = "STP";
        stopLossOrder.m_auxPrice = this.roundTo5(this.m_highest);
        stopLossOrder.m_transmit = true;
        stopLossOrder.m_tif = "GTC";
        stopLossOrder.m_totalQuantity = m_currentLotSize;
        m_dataHandler.m_request.placeOrder(m_dataHandler.m_reqId++, m_dataHandler.m_contract, stopLossOrder);

        // TODO: reset the order quantity if it is more than the initial quantity

    }

    public void closeAllPosition() {

    }

    public void findPastLowHigh () {
        int period = 10;

        m_highest = this.m_dataHandler.m_bars.get(this.m_dataHandler.m_bars.size() - 1).m_close;
        m_lowest = m_highest;

        int totalBarNumber = this.m_dataHandler.m_bars.size();
        for(int i = Math.max(0, totalBarNumber - period); i < totalBarNumber; i++ ) {
            double tempHigh = this.m_dataHandler.m_bars.get(i).m_high;
            double tempLow = this.m_dataHandler.m_bars.get(i).m_low;

            if(tempHigh > m_highest) {
                m_highest = tempHigh;
            }

            if(tempLow < m_lowest) {
                m_lowest = tempLow;
            }
        }
    }

    public void updateStopLossExecutionCount() {
        //this.m_dataHandler.m_orderStates
        // 1. check if stoploss get more
        // 2. if increased, then reset the quantity for next order based on the current quantity
    }


    public void calculate() {

        // go through all the filters

        this.m_ssFilter.filter(m_inputData, m_smoothed);

        this.m_fisherFilter.filter(m_smoothed, m_fisher, m_trigger);

        if(m_inputData.size() > m_lastBarCount) {
            // new bar
            m_thisBarBought = false;
            m_thisBarSold = false;
        }

        this.m_lastBarCount = m_inputData.size();

        this.findPastLowHigh();


    }
    public void decide() {
        double fI, tI; // latest fisher[i], trigger[i]
        double fIm1, fIm2, tIm1; // fisher[i-1], trigger[i-1]

        fI = this.m_fisher.get(this.m_fisher.size() - 1 );
        tI = this.m_trigger.get(this.m_trigger.size() - 1);

        fIm1 = this.m_fisher.get(this.m_fisher.size() - 2);
        fIm2 = this.m_fisher.get(this.m_fisher.size() - 3);
        tIm1 = this.m_trigger.get(this.m_trigger.size() - 2);



        logger.log("fIm2: "+fIm2+ " fIm1: " +fIm1+" fI: "+ fI);

        // decision logic goes here...

        if(fI > fIm1 && fIm2 > fIm1) {

            if(m_thisBarBought == false ) {
                logger.log("buying...");
                this.buy();
                m_thisBarBought = true;
            }

        }

        if(fI < fIm1 && fIm2 < fIm1) {

            if(m_thisBarSold == false) {
                logger.log("selling...");
                this.sell();
                m_thisBarSold = true;
            }

        }



    }

}
