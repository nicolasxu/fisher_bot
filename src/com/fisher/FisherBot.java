package com.fisher;

import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.controller.Bar;


import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nick on 5/2/15.
 */
public class FisherBot implements IBot {


    public DataHandler m_dataHandler;

    public ArrayList<Double> m_smoothed;

    public SuperSmootherFilter m_ssFilter;
    public FisherFilter m_fisherFilter;
    public StdFilter m_stdFilter;

    // array list of result data
    public ArrayList<Double> m_fisher;  // fisher transform result
    public ArrayList<Double> m_trigger; // fisher transform trigger
    public ArrayList<Double> m_standDeviations;

    public int m_initialLotSize;
    public int m_currentLotSize;
    public double m_profitSize;
    public double m_initialProfitSize;

    public boolean m_thisBarBought;
    public boolean m_thisBarSold;
    public int m_lastBarCount;
    public double m_lowest;
    public double m_highest;
    public HashMap<Integer, Boolean> m_stopLossOrderTriggered;

    public ArrayList<Bar> m_inputBars;
    public ArrayList<Double> m_opens;
    public ArrayList<Double> m_closes;
    public ArrayList<Double> m_medians;
    public int m_maxSize;
    public int m_lossInPoint;
    public String m_filePath;
    public String m_lossFileName;

    public ILogger logger;

    public FisherBot(DataHandler handler, ArrayList<Bar> bars, ILogger logger) {
        this.m_dataHandler = handler;

        this.logger = logger;
        this.m_inputBars = bars;
        this.m_maxSize = 450000; // 45 wan
        this.m_lossInPoint = 0;

        // init filters
        this.m_ssFilter = new SuperSmootherFilter(10);
        this.m_fisherFilter = new FisherFilter(15);
        this.m_stdFilter = new StdFilter(8);

        // init value
        this.m_smoothed = new ArrayList<Double>();
        this.m_fisher = new ArrayList<Double>();
        this.m_trigger = new ArrayList<Double>();
        this.m_opens = new ArrayList<Double>();
        this.m_closes = new ArrayList<Double>();
        this.m_medians = new ArrayList<Double>();
        this.m_standDeviations = new ArrayList<Double>();

        this.m_stopLossOrderTriggered = new HashMap<Integer, Boolean>();


        this.logger = handler.m_logger;

        this.m_initialLotSize = 100000; // 100k, 1 lot
        this.m_currentLotSize = this.m_initialLotSize;
        this.m_initialProfitSize = 50 * 0.00001;
        this.m_profitSize = m_initialProfitSize;
        this.m_lowest = 4;
        this.m_highest = 0;

        this.m_thisBarBought = false;
        this.m_thisBarSold   = false;
        this.m_filePath = "/Users/nick/documents/fisher/";
        this.m_lossFileName = "lossPoint.log";

        this.loadLossPoints();

    }

    int calculateNextOrderQuantity() {

        int possibleQuantity = (int)(this.m_lossInPoint / this.m_profitSize);
        if(possibleQuantity + this.m_initialLotSize <= this.m_maxSize) {
            // good
            this.m_lossInPoint = 0;
            this.saveLossPoints();
            return possibleQuantity + this.m_initialLotSize;

        } else {
            // use max size, then adjust m_lossInPoint
            this.m_lossInPoint = this.m_lossInPoint - (int)(this.m_maxSize*this.m_initialProfitSize);
            this.saveLossPoints();
            return this.m_maxSize;
        }

    }

    public void loadLossPoints() {


        FileReader fReader;
        BufferedReader bReader;
        String line = null;

        try {
            fReader = new FileReader(m_filePath + m_lossFileName);
            bReader = new BufferedReader(fReader);
            line = bReader.readLine();
            bReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int theLoss = Integer.parseInt(line);
        if (theLoss > 0) {
            this.m_lossInPoint = theLoss;

        } else {
            this.m_lossInPoint = 0;
        }
        System.out.println("loaded lossInPoints: " + this.m_lossInPoint);

    }
    public void saveLossPoints() {
        // after update m_lossInPoint, call this method to save it to disk
        //this.m_lossInPoint;

        FileWriter fWriter;
        BufferedWriter bWriter;
        try {
            fWriter = new FileWriter(m_filePath + m_lossFileName, false);
            bWriter = new BufferedWriter(fWriter);
            bWriter.write(String.format("%d", this.m_lossInPoint));
            bWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void updateClose() {

        for (int i = Math.max(0, m_closes.size() - 1 ); i < m_inputBars.size(); i ++) {

            if(i >= m_closes.size()) {
                this.m_closes.add(m_inputBars.get(i).m_close);
            } else {
                this.m_closes.set(i, m_inputBars.get(i).m_close);
            }
        }
    }

    public void updateOpen() {
        ArrayList<Double> opens = new ArrayList<Double>();
    }

    public void updateMedian() {

    }


    public double roundTo5 (double input) {
        // 1.14042 => 1.14040
        double point = 0.00001;

        double number = Math.round(input / point / 5) * 5 * point;
        return number;
    }
    public void buy() {
        // buy at current ask price with 50 points take profit
        int quantity;
        double profitSize;


        if( this.m_lossInPoint > 0) {
            // there is a loss

            quantity = this.calculateNextOrderQuantity();
            profitSize = this.m_initialProfitSize;


        } else {
            // no loss, use initial size
            quantity = this.m_initialLotSize;
            profitSize = this.m_initialProfitSize;
        }
        logger.log("buying amount: " + quantity);

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
        parentOrder.m_totalQuantity = quantity;
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
        takeProfitOrder.m_lmtPrice  = parentOrder.m_lmtPrice + profitSize;
        System.out.println("takeProfitOrder.m_lmtPrice: " + takeProfitOrder.m_lmtPrice);
        takeProfitOrder.m_totalQuantity = parentOrder.m_totalQuantity;
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
        stopLossOrder.m_totalQuantity = parentOrder.m_totalQuantity;
        m_dataHandler.m_request.placeOrder(m_dataHandler.m_reqId++, m_dataHandler.m_contract, stopLossOrder);

        // TODO: reset the order quantity if it is more than the initial quantity

    }

    public void sell() {

        int quantity;
        double profitSize;


        if( this.m_lossInPoint > 0) {
            // there is a loss

            quantity = this.calculateNextOrderQuantity();
            profitSize = this.m_initialProfitSize;


        } else {
            // no loss, use initial size
            quantity = this.m_initialLotSize;
            profitSize = this.m_initialProfitSize;
        }
        logger.log("selling amount: " + quantity);

        Order parentOrder = new Order();
        parentOrder.m_clientId = this.m_dataHandler.m_clientId;
        parentOrder.m_action      = "SELL";
        parentOrder.m_orderType   = "LMT";
        parentOrder.m_transmit    = false;
        parentOrder.m_tif         = "GTD";
        parentOrder.m_goodTillDate = this.m_dataHandler.toTimeString(this.m_dataHandler.m_currentServerTime + 5 * 60);
        parentOrder.m_lmtPrice    = this.roundTo5(this.m_dataHandler.m_currentBidPrice);
        logger.log("current bid price: " + this.m_dataHandler.m_currentBidPrice);
        parentOrder.m_totalQuantity = quantity;
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
        takeProfitOrder.m_lmtPrice  = parentOrder.m_lmtPrice - profitSize;
        takeProfitOrder.m_totalQuantity = parentOrder.m_totalQuantity;
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
        stopLossOrder.m_totalQuantity = parentOrder.m_totalQuantity;
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
    public boolean isStopLossOrder(int orderId) {
        Order theOrder = this.m_dataHandler.m_orders.get(orderId);
        // if order is executed before the program started, then the get result is empty
        if (theOrder == null) {
            return false;
        }
        if(theOrder.m_auxPrice > 0 && theOrder.m_orderType.compareTo("STP") == 0 && theOrder.m_parentId > 0) {
            return true;
        }
        return false;
    }
    public void findStopLossOrders() {
        // go through m_dataHandler.m_orders and put stopLoss order ID to m_stopLossOrderTriggered
        for(Map.Entry<Integer, Order> entry : this.m_dataHandler.m_orders.entrySet()) {
            int orderId = entry.getKey();
            if(this.isStopLossOrder(orderId)) {
                if(!m_stopLossOrderTriggered.containsKey(orderId)) {
                    m_stopLossOrderTriggered.put(orderId, false);
                }
            }

        }
    }
    double findLossInPoint(int stopLossOrderId) {
        //return 0.00001 * point * lotSize
        Order slOrder = this.m_dataHandler.m_orders.get(stopLossOrderId);
        if (slOrder == null) {
            logger.log("stoploss order " + stopLossOrderId + "is null");
        }
        double auxPrice = slOrder.m_auxPrice;
        int parentId = slOrder.m_parentId;
        Order parentOrder = this.m_dataHandler.m_orders.get(parentId);
        if(parentOrder == null) {
            logger.log("parentOrder "+parentId+ " is null");
            return 100.0;
        }
        int quantity = parentOrder.m_totalQuantity; // e.g.: 100000
        if(parentOrder.m_action.compareTo("BUY") == 0) {
            // buy order
            logger.log("buy order loss: " +  String.format("%.5g%n", (parentOrder.m_lmtPrice - auxPrice))  + " quantity: " + quantity);
            return (parentOrder.m_lmtPrice - auxPrice) * quantity;
        } else {
            // sell order
            logger.log("sell order loss: " + String.format("%.5g%n", (auxPrice - parentOrder.m_lmtPrice)) + " quantity: " + quantity);
            return (auxPrice - parentOrder.m_lmtPrice) * quantity;
        }
    }
    public void processTriggeredStopLoss(int orderId) {
        // based on triggered stop loss order, set
        // 1. m_currentLotSize
        // 2. m_profitSize
        logger.log("running findLossInPoint(orderId)");
        double loss = findLossInPoint(orderId); // 0.00043 * 100000
        this.m_lossInPoint = this.m_lossInPoint + (int)loss;
        this.saveLossPoints();
        logger.log("loss in points: " + loss);
    }


    public void processStopLossOrder() {
        // this function should be executed after findStopLossOrders()
        // go through this.m_m_stopLossOrderTriggered
        // if new sp order triggered, then
        // 1. process it
        // 2. set m_stopLossOrderTriggered to true

        for(Map.Entry<Integer, Boolean> entry: this.m_stopLossOrderTriggered.entrySet()) {
            int orderId = entry.getKey();
            boolean triggered = entry.getValue();
            if(triggered == false) {
                // not previously triggered
                OrderState state = this.m_dataHandler.m_orderStates.get(orderId);
                if(state == null) {
                    return;
                }
                if(state.m_status.compareTo("Filled") == 0) {
                    logger.log("stopLoss Order triggered: " + orderId);
                    // but new state is filled
                    this.processTriggeredStopLoss(orderId);
                    this.m_stopLossOrderTriggered.put(orderId, true);

                }
            }
        }
    }

    public void updateOrderExecution() {
        //this.m_dataHandler.m_orderStates
        // 1. check if stoploss get more
        // 2. if increased, then reset the quantity for next order based on the current quantity
        this.findStopLossOrders();
        this.processStopLossOrder();

    }


    public void calculate() {

        // go through all the filters
        this.updateClose();

        this.m_stdFilter.filter(m_closes, m_standDeviations);

        this.m_ssFilter.filter(m_closes, m_smoothed);

        this.m_fisherFilter.filter(m_smoothed, m_fisher, m_trigger);

        if(m_inputBars.size() > m_lastBarCount) {
            // new bar
            m_thisBarBought = false;
            m_thisBarSold = false;
        }

        this.m_lastBarCount = m_inputBars.size();

        this.findPastLowHigh();


    }
    public void decide() {
        double fI; // latest fisher[i], trigger[i]
        double fIm1, fIm2, fIm3; // fisher[i-1], trigger[i-1]
        int fisherCount = this.m_fisher.size();
        fI = this.m_fisher.get(fisherCount - 1 );


        fIm1 = this.m_fisher.get(fisherCount - 2);
        fIm2 = this.m_fisher.get(fisherCount - 3);
        fIm3 = this.m_fisher.get(fisherCount - 4);


        if (Math.abs(fI ) > 1 || Math.abs(fIm1) > 1 || Math.abs(fIm2) > 1 || Math.abs(fIm3) > 1) {
            // if any slight clue, fisher value abs is more than 1, then using this logic

        }


        if(fI > fIm1 && fIm2 > fIm1) {

            if(m_thisBarBought == false ) {
                logger.log("executing buying...");
                logger.log("fIm2: " +fIm2+ " FIm1: "+fIm1 +"FI: "+ fI);
                this.buy();
                m_thisBarBought = true;
            }
        }

        if(fI < fIm1 && fIm2 < fIm1) {

            if(m_thisBarSold == false) {
                logger.log("executing selling...");
                logger.log("fIm2: " +fIm2+ " FIm1: "+fIm1 +"FI: "+ fI);
                this.sell();
                m_thisBarSold = true;
            }
        }
    }
}
