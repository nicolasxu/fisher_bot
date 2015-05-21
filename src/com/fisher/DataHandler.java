package com.fisher;

import com.ib.client.*;
import com.ib.controller.Bar;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.lang.Math;

/**
 * Created by nick on 4/27/15.
 */
public class DataHandler implements EWrapper{

    EClientSocket m_request;      // instance of child of EClientSocket Connection
                            // for connect to TWS, request data, and place order
    public int m_clientId;  // the unique number that identify this program

    public Contract m_contract;
    public String m_period;
    public int m_reqId;

    public String m_systemStartTimeString;
    public long m_currentServerTime; // update with ever tick
    public int m_nextValidOrderId;
    public ArrayList<Bar> m_bars;
    public ArrayList<Double> m_medians; // medians value for each bar, ( (high + low) / 2)
    public double m_currentBidPrice;
    public double m_currentAskPrice;
    public boolean m_newBidPrice;
    public boolean m_newAskPrice;

    public int m_fiveMinBarRequestId;
    public int m_marketDataRequestId;
    public HashMap<Long, Double> m_temp5minTicks;
    public ArrayList<Double> m_fiveMinPrices;
    public FisherBot m_fisherBot;
    public boolean m_newBarFlag;
    public BotDataPlotter m_appPlotter;
    public ILogger m_logger;

    public boolean testBuyOrderSent = false;
    public boolean testSellOrderSent = false;

    public HashMap<Integer, Order> m_orders;
    public HashMap<Integer, OrderState> m_orderStates;
    public ArrayList<Double> m_stopLossOrderIds;




    public DataHandler(ILogger logger) {

        logger.log("DataHandler() called...");
        this.m_logger = logger;

        this.m_reqId = 1; // increase one after each request
        this.m_clientId = 1234;
        this.m_period = "5 mins";
        this.m_nextValidOrderId = -1;
        this.m_newBidPrice = false;
        this.m_newAskPrice = false;
        this.m_currentAskPrice = 0;
        this.m_currentBidPrice = 0;
        this.m_fiveMinBarRequestId = -1;
        this.m_marketDataRequestId = -1;

        // init member variable
        m_request = new Request(this);

        this.m_temp5minTicks = new HashMap<Long, Double>();

        // contract initialization
        this.m_contract = new Contract();
        this.m_contract.m_symbol = "EUR";
        this.m_contract.m_secType = "CASH";
        this.m_contract.m_currency = "USD";
        this.m_contract.m_exchange = "IDEALPRO";

        this.m_bars = new ArrayList<Bar>();
        this.m_medians = new ArrayList<Double>();
        this.m_fiveMinPrices = new ArrayList<Double>();
        this.m_orders = new HashMap<Integer, Order>();
        this.m_orderStates = new HashMap<Integer, OrderState>();
        this.m_stopLossOrderIds = new ArrayList<Double>();

        this.m_systemStartTimeString = "";

        this.m_fisherBot = new FisherBot(this, this.m_medians, logger);

        m_request.eConnect(null, 7496, this.m_clientId);

        m_request.reqCurrentTime(); // request server time first

        this.m_newBarFlag = false;

    }

    public void setPlotter(BotDataPlotter plotter) {
        this.m_appPlotter = plotter;
    }

    public String toTimeString (long time) {

        Date dd = new Date(time * 1000); // multiply by 1000 to convert seconds to millisecond
        DateFormat format = new SimpleDateFormat("yyyyMMdd HH:mm:ss");  // yyyymmdd hh:mm:ss tmz
        return format.format(dd);

    }


    public void fetchContractHistoricalData() {
        this.m_logger.log("fetching data...");
        List<TagValue> XYZ = new ArrayList<TagValue>();
        this.m_fiveMinBarRequestId = this.m_reqId;
        this.m_request.reqHistoricalData(this.m_reqId++,
                this.m_contract, this.m_systemStartTimeString, "2 D",
                "5 mins", "MIDPOINT", 0, 2, XYZ);

        // no need to create one, if current time is unfinished 5 min bar, then the last bar is the current unfinished one

    }

    public void requestLiveData () {
        this.m_logger.log("Request live data...");
        this.m_marketDataRequestId = this.m_reqId;
        List<TagValue> XYZ = new ArrayList<TagValue>();
        this.m_request.reqMarketDataType(2);
        this.m_request.reqMktData(this.m_reqId++, this.m_contract, "233", false, XYZ);
        this.m_request.reqPositions();


    }

    public void handleNew5minBar(long time) {
        // if new bar, create one and append it to m_bars

        if(this.m_newBidPrice || this.m_newAskPrice) {


            double current5minCount =  Math.floor(time / (60 * 5));
            double prev5minCount = Math.floor(this.m_currentServerTime / (60 * 5));

            if(current5minCount > prev5minCount) {

                // new bar, handle it when receive both bid and ask price
                this.m_newBarFlag = true;

                if(this.m_newAskPrice || this.m_newBidPrice) {

                    // new five min bar

                    long lastBarTime = this.m_bars.get(this.m_bars.size() - 1).time();
                    double lastClosePrice = this.m_bars.get(this.m_bars.size() - 1).close(); // new open equals last close
                    Bar newBar = new Bar(lastBarTime + 5 * 60, lastClosePrice, lastClosePrice, lastClosePrice, lastClosePrice, -1, -1, -1);

                    // Add new Bar
                    this.m_bars.add(newBar);
                    this.m_fiveMinPrices.clear();

                    // create and add new mediens
                    this.m_medians.add(lastClosePrice);

                    ////////// debug log msg ///////

                    System.out.println("new bar!");
                    Bar last2Bar = this.m_bars.get(m_bars.size() -2);

                    String logMsg = last2Bar.m_time + " last bar - open: " + last2Bar.open();
                    logMsg = logMsg + " close: " + last2Bar.close();
                    logMsg = logMsg + " low: " + last2Bar.low();
                    logMsg = logMsg + " high: " + last2Bar.high();
                    System.out.println(logMsg);

                    System.out.println("last bar median price: " + m_medians.get(m_medians.size() - 2));

                    Bar currentBar = this.m_bars.get(m_bars.size() -1);
                    String logMsg2 = currentBar.m_time + " current bar - open: " + currentBar.open();
                    logMsg2 = logMsg2 + " close: " + currentBar.close();
                    logMsg2 = logMsg2 + " low: " + currentBar.low();
                    logMsg2 = logMsg2 + " high: " + currentBar.high();

                    System.out.println(logMsg2);

                    System.out.println("current median price: " + m_medians.get(m_medians.size() -1));

                    //////// end of debug log msg //////

                    // calculate
                    this.m_fisherBot.calculate();
                    // decide
                    this.m_fisherBot.decide();

                    // reset new price flag
                    this.m_newBidPrice = false;
                    this.m_newAskPrice = false;

                    // update time server time, so that new bar branch won't be triggered next time
                    this.m_currentServerTime = time;

                    // update plot data for new bar
                    this.m_appPlotter.updatePlotData();

                } else {
                    if(this.m_newBidPrice == false) {
                        this.m_currentBidPrice = 0;
                    }

                    if (this.m_newAskPrice == false) {
                        this.m_currentAskPrice = 0;
                    }

                }


            } else {

                // update last bar, last median, no new bar or median creation
                this.handleTickPrice();

                // just calculate
                this.m_fisherBot.calculate();

                // then decide also
                this.m_fisherBot.decide();

                // reset the flag
                if(this.m_newAskPrice == true) {
                    this.m_newAskPrice = false;
                }
                if(this.m_newBidPrice == true) {
                    this.m_newBidPrice = false;
                }

                // update time server time
                this.m_currentServerTime = time;


            }
        }
    }

    public double findMidPrice() {
        // find mid price based on latest bid and ask
        double p1, p2, mid;
        if (m_currentAskPrice > 0) {
            p1 = m_currentAskPrice;
        } else {
            p1 = m_currentBidPrice;
        }
        if(m_currentBidPrice > 0) {
            p2 = m_currentBidPrice;
        } else {
            p2 = m_currentAskPrice;
        }
        mid = (p1+ p2) / 2;

        return mid;
    }

    public void handleTickPrice() {
        // always put data to last m_bars

        // 1. find the mid price
        double mid = this.findMidPrice(); // from the current bid and ask price, not from bar high and low price

        // 2. add to temp price list
        this.m_fiveMinPrices.add(mid);



        // 3. find high and low
        double high = 0;
        double low = 10;

        for (Double price: m_fiveMinPrices) {
            if(price > high) {
                high = price;
            }
            if(price < low) {
                low = price;
            }
        }

        // 4. update last bar
        int lastIndex = m_bars.size() - 1;
        Bar lastBar = m_bars.get(lastIndex);

        if(high > lastBar.m_high) {
            lastBar.m_high = high;
        }

        if(low < lastBar.m_low) {
            lastBar.m_low = low;
        }

        lastBar.m_close = mid;

        // 5. update last m_medians
        this.m_medians.set(lastIndex, lastBar.close() );


    }


    @Override
    public void tickPrice(int tickerId, int field, double price, int canAutoExecute) {

        String fieldDesc = TickType.getField(field);
        //System.out.println(fieldDesc + ": " + price);

        switch (field) {
            case 1:
                // bid
                this.m_currentBidPrice = price;
                this.m_newBidPrice = true;

                /*
                if(testSellOrderSent == false) {
                    this.m_fisherBot.sell();
                    this.testSellOrderSent = true;
                }
                */



                break;
            case 2:
                // ask
                this.m_currentAskPrice = price;
                this.m_newAskPrice = true;


//                if(testBuyOrderSent == false) {
//                    this.m_fisherBot.buy();
//                    this.testBuyOrderSent = true;
//                }



                break;
            default:

                break;
        }



        // price is handled in currentTime() handler
        this.m_request.reqCurrentTime();


    }

    @Override
    public void tickSize(int tickerId, int field, int size) {

        //System.out.println("tickSize() - size: " + size);
    }

    @Override
    public void tickOptionComputation(int tickerId, int field, double impliedVol, double delta, double optPrice, double pvDividend, double gamma, double vega, double theta, double undPrice) {

    }

    @Override
    public void tickGeneric(int tickerId, int tickType, double value) {
        this.m_logger.log("tickGeneric() -" + " type: " + tickType + " value: " + value);
    }

    @Override
    public void tickString(int tickerId, int tickType, String value) {
        TickType.getField(tickType);
        this.m_logger.log("tickString() - " + "type: " + tickType + " value: " + value);
    }

    @Override
    public void tickEFP(int tickerId, int tickType, double basisPoints, String formattedBasisPoints, double impliedFuture, int holdDays, String futureExpiry, double dividendImpact, double dividendsToExpiry) {

    }

    @Override
    public void orderStatus(int orderId, String status, int filled, int remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
        //this.m_logger.log("OrderId: "+ orderId + " Status: " + status + " ParentId: " + parentId);
    }

    @Override
    public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
        this.m_logger.log("openOrder(), orderId: " + orderId + " orderStatus.m_status: " + orderState.m_status + " order.m_orderId: " + order.m_orderId);


        this.m_orderStates.put(orderId, orderState);
        this.m_orders.put(orderId, order);
        // update execution count
        this.m_fisherBot.updateStopLossExecutionCount();

        System.out.println("m_orderStates.size(): " + m_orderStates.size());
        System.out.println("m_orders.size(): " + m_orders.size());
    }

    @Override
    public void openOrderEnd() {

    }

    @Override
    public void updateAccountValue(String key, String value, String currency, String accountName) {

    }

    @Override
    public void updatePortfolio(Contract contract, int position, double marketPrice, double marketValue, double averageCost, double unrealizedPNL, double realizedPNL, String accountName) {

    }

    @Override
    public void updateAccountTime(String timeStamp) {

    }

    @Override
    public void accountDownloadEnd(String accountName) {

    }

    @Override
    public void nextValidId(int orderId) {

        this.m_logger.log("next valid orderId: " + orderId);
        // The next available order Id received from TWS upon connection.
        // Increment all successive orders by one based on this Id.
        this.m_nextValidOrderId = orderId + 1000;
        this.m_reqId = orderId; // let request id not colide with order id
        this.fetchContractHistoricalData();
        this.m_request.reqOpenOrders();


    }

    @Override
    public void contractDetails(int reqId, ContractDetails contractDetails) {

    }

    @Override
    public void bondContractDetails(int reqId, ContractDetails contractDetails) {

    }

    @Override
    public void contractDetailsEnd(int reqId) {

    }

    @Override
    public void execDetails(int reqId, Contract contract, Execution execution) {

    }

    @Override
    public void execDetailsEnd(int reqId) {

    }

    @Override
    public void updateMktDepth(int tickerId, int position, int operation, int side, double price, int size) {

    }

    @Override
    public void updateMktDepthL2(int tickerId, int position, String marketMaker, int operation, int side, double price, int size) {

    }

    @Override
    public void updateNewsBulletin(int msgId, int msgType, String message, String origExchange) {

    }

    @Override
    public void managedAccounts(String accountsList) {

    }

    @Override
    public void receiveFA(int faDataType, String xml) {

    }

    @Override
    public void historicalData(int reqId, String date, double open, double high, double low, double close, int volume, int count, double WAP, boolean hasGaps) {
        /* for Forex
           count always -1
           volume always -1
           the last data is:
           reqId: 1 date: finished-20150427  14:19:31-20150429  14:19:31 open:-1.0

        */


        if(open != -1 && reqId == this.m_fiveMinBarRequestId) {
            // Bar( long time, double high, double low, double open, double close, double wap, long volume, int count)
            long localTime = Long.parseLong(date.trim());
            Bar bar = new Bar(localTime, high, low, open, close, -1, -1, -1);

            this.m_bars.add(bar);
        }

        if(open == -1 && reqId == this.m_fiveMinBarRequestId) {
            // request history data completed
            DecimalFormat f = new DecimalFormat("0.00000");
            DateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss");  // yyyymmdd hh:mm:ss tmz

            for(Bar b: m_bars) {

                // enable below line for live data:
                m_medians.add(b.close());

                // generate data for MT5
                //System.out.println("ssTestPrice["+m_bars.indexOf(b)+"]= "+ f.format(inputNumber)   +";");
            }


            this.m_fisherBot.calculate();
            this.m_fisherBot.decide();

            // bind data to plotter
            this.m_appPlotter.setBarSource(this.m_bars);
            this.m_appPlotter.setFisherSource(m_fisherBot.m_fisher);
            this.m_appPlotter.setTriggerSource(m_fisherBot.m_trigger);
            this.m_appPlotter.updatePlotData();

            this.requestLiveData();
        }

    }

    @Override
    public void scannerParameters(String xml) {

    }

    @Override
    public void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance, String benchmark, String projection, String legsStr) {

    }

    @Override
    public void scannerDataEnd(int reqId) {

    }

    @Override
    public void realtimeBar(int reqId, long time, double open, double high, double low, double close, long volume, double wap, int count) {

    }

    @Override
    public void currentTime(long time) {

        this.handleNew5minBar(time);


        // if 1st time, then set the string
        if(this.m_systemStartTimeString.length() == 0) {
            this.m_currentServerTime = time;
            this.m_systemStartTimeString = this.toTimeString(time);
            this.m_logger.log("m_systemStartTimeString (Local) is: " + this.m_systemStartTimeString + " - " + this.m_currentServerTime);

        }
    }

    @Override
    public void fundamentalData(int reqId, String data) {

    }

    @Override
    public void deltaNeutralValidation(int reqId, UnderComp underComp) {

    }

    @Override
    public void tickSnapshotEnd(int reqId) {

    }

    @Override
    public void marketDataType(int reqId, int marketDataType) {

    }

    @Override
    public void commissionReport(CommissionReport commissionReport) {

    }

    @Override
    public void position(String account, Contract contract, int pos, double avgCost) {
        if(contract.m_symbol.compareTo("EUR") == 0) {
            this.m_logger.log("position is: " + pos + " for contract: " + contract.m_symbol);
        }


    }

    @Override
    public void positionEnd() {

    }

    @Override
    public void accountSummary(int reqId, String account, String tag, String value, String currency) {

    }

    @Override
    public void accountSummaryEnd(int reqId) {

    }

    @Override
    public void verifyMessageAPI(String apiData) {

    }

    @Override
    public void verifyCompleted(boolean isSuccessful, String errorText) {

    }

    @Override
    public void displayGroupList(int reqId, String groups) {

    }

    @Override
    public void displayGroupUpdated(int reqId, String contractInfo) {

    }

    @Override
    public void error(Exception e) {
        System.out.println("error(String str): " +e.getMessage());
    }

    @Override
    public void error(String str) {
        System.out.println("error(String str): "+str);
    }

    @Override
    public void error(int id, int errorCode, String errorMsg) {
        this.m_logger.log("error(int id, int errorCode, String errorMsg): " + errorMsg);
    }

    @Override
    public void connectionClosed() {

    }
}
