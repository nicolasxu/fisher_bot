package com.fisher;

import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.controller.Bar;


import java.io.*;
import java.util.*;

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
    public int m_nextOrderQuantity;
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
    public ArrayList<Integer> m_closePositionOrders; // contains order of all close position orders,
                                                     // added in closePosition()

    public ILogger logger;
    public boolean m_buyFlag;
    public boolean m_sellFlag;
    public int toCancelOrderId;
    public boolean m_closingPosition;
    public boolean m_sleeping;
    public double m_lastDealPrice;

    public FisherBot(DataHandler handler, ArrayList<Bar> bars, ILogger logger) {
        this.m_dataHandler = handler;

        this.logger = logger;
        this.m_inputBars = bars;
        this.m_maxSize = 450000; // 45 wan
        this.m_lossInPoint = 0;

        // init filters
        this.m_ssFilter = new SuperSmootherFilter(8);
        this.m_fisherFilter = new FisherFilter(5);
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
        this.m_nextOrderQuantity = this.m_initialLotSize;
        this.m_currentLotSize = this.m_initialLotSize;
        this.m_initialProfitSize = 50 * 0.00001;
        this.m_profitSize = m_initialProfitSize;
        this.m_lowest = 4;
        this.m_highest = 0;
        this.m_closePositionOrders = new ArrayList<Integer>();

        this.m_thisBarBought = false;
        this.m_thisBarSold   = false;
        this.m_filePath = "/Users/nick/documents/fisher/";
        this.m_lossFileName = "lossPoint.log";

        this.m_buyFlag  = false;
        this.m_sellFlag = false;
        this.toCancelOrderId = 0;
        this.m_closingPosition = false;
        this.m_sleeping = false;
        this.m_lastDealPrice = 0.0;

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
    public double roundTo5 (double input) {
        // 1.14042 => 1.14040
        double point = 0.00001;

        double number = Math.round(input / point / 5) * 5 * point;
        return number;
    }
    public void closePosition() {
        System.out.println("closePosition() - triggered");
        if(this.m_closingPosition == true) {
            System.out.println("closing position... no order sent in this call");
            // if position is being closed right now, do nothing.
            return;
        }


        if(this.m_dataHandler.m_position == 0 ) {
            //this.m_dataHandler.m_request.reqGlobalCancel();
            this.m_dataHandler.m_request.cancelOrder(this.toCancelOrderId);
            System.out.println("cancel order with when position 0, id: " + this.toCancelOrderId);
            return;
        }
        this.m_closingPosition = true; // set the target, since reading the position takes time,
        // we don't want to trigger another close position order meanwhile.

        //this.m_dataHandler.m_request.reqGlobalCancel();
        this.m_dataHandler.m_request.cancelOrder(this.toCancelOrderId);
        System.out.println("cancel order with position("+ m_dataHandler.m_position+ "), id: " + this.toCancelOrderId);


        Order closeOrder = new Order();
        closeOrder.m_clientId = this.m_dataHandler.m_clientId;
        closeOrder.m_orderType = "MKT";
        closeOrder.m_transmit = true;
        closeOrder.m_totalQuantity = Math.abs(this.m_dataHandler.m_position);

        if(this.m_dataHandler.m_position > 0) {
            closeOrder.m_action = "SELL";


        }

        if(this.m_dataHandler.m_position < 0) {
            closeOrder.m_action = "BUY";
        }

        int orderId = this.m_dataHandler.m_reqId++;
        System.out.println("closePosition() - Since right now the position is: " + m_dataHandler.m_position + " , placing "
                + closeOrder.m_action+ " order with quantity: " + closeOrder.m_totalQuantity + " to close position");

        this.m_dataHandler.m_request.placeOrder(orderId, this.m_dataHandler.m_contract, closeOrder);

        // add order id to this collection
        this.m_closePositionOrders.add(orderId);

    }

    public void sleep() {
        this.m_sleeping = true;

        class WakeUpTask extends TimerTask {

            /**
             * The action to be performed by this timer task.
             */
            @Override
            public void run() {
                System.out.println("waking up ...");
                m_sleeping = false;

            }
        }
        TimerTask theTask = new WakeUpTask();
        Timer theTimer = new Timer();
        theTimer.schedule(theTask, 100*1000); // delay 15 seconds

    }

    public void buyOrSell() {
        if (this.m_buyFlag == true) {
            // buy
            this.buy();
            System.out.println("reset buy flag to false");
            this.m_buyFlag = false;
            this.sleep();
        }

        if(this.m_sellFlag == true) {
            // sell
            this.sell();
            this.m_sellFlag = false;
            System.out.println("reset sell flag to false");
            this.sleep();

        }
    }
    public void buy() {
        // buy at current ask price with 50 points take profit

        logger.log("buy() triggered - executing buying amount: " + m_nextOrderQuantity);
        System.out.println("buying amount: " + m_nextOrderQuantity);
        Order parentOrder = new Order();
        parentOrder.m_clientId    = this.m_dataHandler.m_clientId;
        //parentOrder.m_orderId is not used
        parentOrder.m_action      = "BUY";
        parentOrder.m_orderType   = "LMT";
        parentOrder.m_transmit    = false;
        parentOrder.m_tif         = "GTD";
        parentOrder.m_goodTillDate = this.m_dataHandler.toTimeString(this.m_dataHandler.m_currentServerTime + 5 * 60);
        parentOrder.m_lmtPrice    = this.roundTo5(this.m_dataHandler.m_currentAskPrice);
        this.m_lastDealPrice = parentOrder.m_lmtPrice;
        logger.log("current ask price: " + this.m_dataHandler.m_currentAskPrice);
        parentOrder.m_totalQuantity = m_nextOrderQuantity;
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
        takeProfitOrder.m_lmtPrice  = parentOrder.m_lmtPrice + m_initialProfitSize;
        System.out.println("takeProfitOrder.m_lmtPrice: " + takeProfitOrder.m_lmtPrice);
        takeProfitOrder.m_totalQuantity = parentOrder.m_totalQuantity;
        int tpOrderId = m_dataHandler.m_reqId++;
        this.toCancelOrderId = tpOrderId;
        m_dataHandler.m_request.placeOrder(tpOrderId, m_dataHandler.m_contract, takeProfitOrder);

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

        this.m_thisBarBought = true;
        this.m_thisBarSold   = false;
        // TODO: reset the order quantity if it is more than the initial quantity

    }
    public void sell() {


        logger.log("sell() triggered - execute selling amount: " + m_nextOrderQuantity);

        Order parentOrder = new Order();
        parentOrder.m_clientId = this.m_dataHandler.m_clientId;
        parentOrder.m_action      = "SELL";
        parentOrder.m_orderType   = "LMT";
        parentOrder.m_transmit    = false;
        parentOrder.m_tif         = "GTD";
        parentOrder.m_goodTillDate = this.m_dataHandler.toTimeString(this.m_dataHandler.m_currentServerTime + 5 * 60);
        parentOrder.m_lmtPrice    = this.roundTo5(this.m_dataHandler.m_currentBidPrice);
        this.m_lastDealPrice = parentOrder.m_lmtPrice;
        logger.log("current bid price: " + this.m_dataHandler.m_currentBidPrice);
        parentOrder.m_totalQuantity = m_nextOrderQuantity;
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
        takeProfitOrder.m_lmtPrice  = parentOrder.m_lmtPrice - m_initialProfitSize;
        takeProfitOrder.m_totalQuantity = parentOrder.m_totalQuantity;
        int tpOrderId = m_dataHandler.m_reqId++;
        this.toCancelOrderId = tpOrderId;
        m_dataHandler.m_request.placeOrder(tpOrderId, m_dataHandler.m_contract, takeProfitOrder);

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

        this.m_thisBarSold   = true;
        this.m_thisBarBought = false;
        // TODO: reset the order quantity if it is more than the initial quantity

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

    public void findOutLoss() {
        // new
        // triggered in data handler execDetails()
        // calculate loss, if any, in EURUSD points
        int index = this.m_dataHandler.m_executions.size() - 1;
        if (index < 0) {
            // m_exections is empty
            return;
        }
        Execution lastExec = this.m_dataHandler.m_executions.get(index);
        boolean isCloseOrder = false;
        for(int i = 0; i < this.m_closePositionOrders.size(); i++) {
            if(this.m_closePositionOrders.get(i) == lastExec.m_orderId) {
                isCloseOrder = true;
            }
        }

        if(isCloseOrder) {
            // till this point, the close position order is successfully executed

        }
    }

    public void calculateNextPosition(double diff) {
        if(diff >= 0) {
            // making money
            this.m_nextOrderQuantity = this.m_initialLotSize;

        } else {
            // losing money
            this.m_nextOrderQuantity = this.m_initialLotSize + (int)Math.round(-diff / (m_initialProfitSize) );
        }
        System.out.println("m_nextOrderQuantity: " + m_nextOrderQuantity);
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

        // place possible buy order for reverse trend
        if(fI > fIm1 && fIm2 > fIm1 && !this.m_sleeping) {
            System.out.println("down trend reverse to up trend");
            if(m_thisBarBought == false ) {
                //System.out.println("executing buying");
                logger.log("executing buying...");
                logger.log("fIm2: " +fIm2+ " FIm1: "+fIm1 +"FI: "+ fI);
                if(this.m_dataHandler.m_position != 0) {
                    this.closePosition();
                    this.m_buyFlag = true;
                    System.out.println("set m_buyFlag to true");
                    logger.log("m_buyFlag = true");
                } else {
                    this.m_buyFlag = true;
                    this.buyOrSell();
                }


                //m_thisBarBought = true;
            }
        }

        // place possible sell order for reverse trend
        if(fI < fIm1 && fIm2 < fIm1 &!this.m_sleeping) {
            System.out.println("up trend reverse to down trend");
            if(m_thisBarSold == false) {
                //System.out.println("executing selling");
                logger.log("executing selling...");
                logger.log("fIm2: " + fIm2 + " FIm1: " + fIm1 + "FI: " + fI);
                if(this.m_dataHandler.m_position != 0.0) {
                    this.closePosition();
                    this.m_sellFlag = true;
                    System.out.println("set sell flag to true");
                } else {
                    this.m_sellFlag = true;
                    System.out.println("set sell flag to true");
                    logger.log("m_sellFlag = true");
                    this.buyOrSell();
                }

                //m_thisBarSold = true;
            }
        }

        // check previous order for reverse situation
        if(fI < fIm1 && fIm1 < fIm2 &&!this.m_sleeping) {
            // still in down trend
            // close all buy position
            if(this.m_dataHandler.m_position > 0) {
                System.out.println("still in decline position and has positive position ("+this.m_dataHandler.m_position +")");
                this.closePosition();
                this.m_sellFlag = true;
                if(this.m_thisBarBought) {
                    System.out.println("set sell flag to true");
                    this.m_sellFlag = true;

                }
            }

        }

        if(fI > fIm1 && fIm1 > fIm2 && !this.m_sleeping) {
            // still in upwards trend
            // close all sell position
            if(this.m_dataHandler.m_position < 0) {
                System.out.println("still in upwards position and has negative position ("+this.m_dataHandler.m_position +")");
                this.closePosition();
                this.m_buyFlag = true;
                if(this.m_thisBarSold) {
                    System.out.println("set buy flag to be true");
                    this.m_buyFlag = true;


                }
            }
        }

    }
}
