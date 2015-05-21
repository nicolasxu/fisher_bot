package com.fisher;

import com.ib.client.Order;
import com.ib.client.OrderState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
    public double m_initialProfitSize;

    public boolean m_thisBarBought;
    public boolean m_thisBarSold;
    public int m_lastBarCount;
    public double m_lowest;
    public double m_highest;
    public HashMap<Integer, Boolean> m_stopLossOrderTriggered;
    public ArrayList<OrderParam> m_orderParams;

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

        this.m_stopLossOrderTriggered = new HashMap<Integer, Boolean>();
        this.m_orderParams = new ArrayList<OrderParam>();

        this.logger = handler.m_logger;

        this.m_initialLotSize = 100000; // 100k, 1 lot
        this.m_currentLotSize = this.m_initialLotSize;
        this.m_initialProfitSize = 50 * 0.00001;
        this.m_profitSize = m_initialProfitSize;
        this.m_lowest = 4;
        this.m_highest = 0;

        this.m_thisBarBought = false;
        this.m_thisBarSold   = false;
    }

    public class OrderParam {
        public int orderSize;
        public double profitPoint;
        public OrderParam() {
            this.orderSize = m_initialLotSize;
            this.profitPoint = m_initialProfitSize;
        }
        public OrderParam(int size, double profit) {
            this.orderSize = size;
            this.profitPoint = profit;
        }
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

        if(m_orderParams.size() > 0) {
            OrderParam param = m_orderParams.get(m_orderParams.size() - 1);
            quantity = param.orderSize;
            profitSize = param.profitPoint;
            // remove one
            m_orderParams.remove(m_orderParams.size() - 1);
        } else {
            quantity = this.m_initialLotSize;
            profitSize = this.m_initialProfitSize;
        }

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

        if(m_orderParams.size() > 0) {
            OrderParam param = m_orderParams.get(m_orderParams.size() - 1);
            quantity = param.orderSize;
            profitSize = param.profitPoint;
            // remove one
            m_orderParams.remove(m_orderParams.size() - 1);
        } else {
            quantity = this.m_initialLotSize;
            profitSize = this.m_initialProfitSize;
        }

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
        double auxPrice = slOrder.m_auxPrice;
        int parentId = slOrder.m_parentId;
        Order parentOrder = this.m_dataHandler.m_orders.get(parentId);
        int quantity = parentOrder.m_totalQuantity; // e.g.: 100000
        if(parentOrder.m_action.compareTo("BUY") == 0) {
            // buy order
            System.out.println("buy order loss: " + (parentOrder.m_lmtPrice - auxPrice) + " quantity: " + quantity);
            return (parentOrder.m_lmtPrice - auxPrice) * quantity;
        } else {
            // sell order
            System.out.println("sell order loss: " + (auxPrice - parentOrder.m_lmtPrice) + " quantity: " + quantity);
            return (auxPrice - parentOrder.m_lmtPrice) * quantity;
        }
    }
    public void processTriggeredStopLoss(int orderId) {
        // based on triggered stop loss order, set
        // 1. m_currentLotSize
        // 2. m_profitSize
        double loss = findLossInPoint(orderId); // 0.00043 * 100000
        int nextSize = (int)(loss/ m_initialProfitSize + m_initialLotSize);
        //OrderParam(int size, double porfit)
        OrderParam op = new OrderParam( nextSize, this.m_initialProfitSize);
        this.m_orderParams.add(op);


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
                if(state.m_status.compareTo("Filled") == 0) {
                    // but new state is filled
                    this.processTriggeredStopLoss(orderId);
                    this.m_stopLossOrderTriggered.put(orderId, true);

                }
            }
        }
    }

    public void updateStopLossExecutionCount() {
        //this.m_dataHandler.m_orderStates
        // 1. check if stoploss get more
        // 2. if increased, then reset the quantity for next order based on the current quantity
        this.findStopLossOrders();
        this.processStopLossOrder();

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
