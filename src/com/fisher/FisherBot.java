package com.fisher;

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
        this.m_profitSize = 15 * 0.00001;
    }

    public void buy() {
        // buy at current ask price with 50 points take profit

        Order parentOrder = new Order();
        parentOrder.m_clientId    = this.m_dataHandler.m_clientId;
        //parentOrder.m_orderId is not used
        parentOrder.m_action      = "BUY";
        parentOrder.m_orderType   = "LMT";
        parentOrder.m_transmit    = false;
        parentOrder.m_tif         = "DAY";
        parentOrder.m_lmtPrice    = this.m_dataHandler.m_currentAskPrice;
        logger.log("current ask price: " + this.m_dataHandler.m_currentAskPrice);
        parentOrder.m_totalQuantity = m_currentLotSize;
        int parentOrderId = this.m_dataHandler.m_reqId++;
        // placing order
        m_dataHandler.m_request.placeOrder(parentOrderId, this.m_dataHandler.m_contract, parentOrder);
        logger.log("parent orderId is: " + parentOrderId);

        Order takeProfitOrder = new Order();
        takeProfitOrder.m_parentId  = parentOrderId;
        takeProfitOrder.m_clientId  = this.m_dataHandler.m_clientId;
        // orderId is not used in order.m_orderId
        takeProfitOrder.m_action    = "SELL";
        takeProfitOrder.m_orderType = "LMT";
        takeProfitOrder.m_transmit  = true;
        takeProfitOrder.m_tif         = "DAY";
        takeProfitOrder.m_auxPrice  = 0;
        takeProfitOrder.m_lmtPrice  = this.m_dataHandler.m_currentAskPrice + this.m_profitSize;

        takeProfitOrder.m_totalQuantity = m_currentLotSize;

        m_dataHandler.m_request.placeOrder(m_dataHandler.m_reqId++, m_dataHandler.m_contract, takeProfitOrder);

    }

    public void sell() {
        Order parentOrder = new Order();
        parentOrder.m_clientId = this.m_dataHandler.m_clientId;
        parentOrder.m_action      = "SELL";
        parentOrder.m_orderType   = "LMT";
        parentOrder.m_transmit    = false;
        parentOrder.m_tif         = "DAY";
        parentOrder.m_lmtPrice    = this.m_dataHandler.m_currentBidPrice;
        logger.log("current bid price: " + this.m_dataHandler.m_currentBidPrice);
        parentOrder.m_totalQuantity = m_currentLotSize;
        int parentOrderId = this.m_dataHandler.m_reqId++;
        // place order
        m_dataHandler.m_request.placeOrder(parentOrderId, this.m_dataHandler.m_contract, parentOrder);
        logger.log("parent orderId is: " + parentOrderId);

        Order takeProfitOrder = new Order();
        takeProfitOrder.m_parentId  = parentOrderId;
        takeProfitOrder.m_clientId  = this.m_dataHandler.m_clientId;
        takeProfitOrder.m_action    = "BUY";
        takeProfitOrder.m_orderType = "LMT";
        takeProfitOrder.m_transmit  = true;
        takeProfitOrder.m_tif         = "DAY";
        takeProfitOrder.m_auxPrice  = 0;
        takeProfitOrder.m_lmtPrice  = this.m_dataHandler.m_currentBidPrice - this.m_profitSize;
        takeProfitOrder.m_totalQuantity = m_currentLotSize;
        m_dataHandler.m_request.placeOrder(m_dataHandler.m_reqId++, m_dataHandler.m_contract, takeProfitOrder);


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
        double fIm1, fIm2, tIm1; // fisher[i-1], trigger[i-1]

        fI = this.m_fisher.get(this.m_fisher.size() - 1 );
        tI = this.m_trigger.get(this.m_trigger.size() - 1);

        fIm1 = this.m_fisher.get(this.m_fisher.size() - 2);
        fIm2 = this.m_fisher.get(this.m_fisher.size() - 3);
        tIm1 = this.m_trigger.get(this.m_trigger.size() - 2);



        logger.log("fIm2: "+fIm2+ " fIm1: " +fIm1+" fI: "+ fI);

        // decision logic goes here...

        if(fI > fIm1 && fIm2 > fIm1) {
            logger.log("buying...");
            this.buy();
        }

        if(fI < fIm1 && fIm2 < fIm1) {
            logger.log("selling...");
            this.sell();
        }



    }

}
