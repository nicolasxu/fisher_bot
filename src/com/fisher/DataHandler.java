package com.fisher;

import com.ib.client.*;
import com.ib.controller.Bar;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.lang.Math;

/**
 * Created by nick on 4/27/15.
 */
public class DataHandler implements EWrapper{

    Request m_request;      // instance of child of EClientSocket Connection
                            // for connect to TWS, request data, and place order
    public int m_clientId;  // the unique number that identify this program

    public Contract m_contract;
    public String m_period;
    public int m_reqId;

    public String m_systemStartTimeString;
    public long m_currentServerTime; // update with ever tick
    public int m_nextValidOrderId;
    public ArrayList<Bar> m_bars;
    public double m_currentBidPrice;
    public double m_currentAskPrice;
    public boolean m_newBidAskPrice;
    public int m_fiveMinBarRequestId;
    public int m_marketDataRequestId;
    public HashMap<Long, Double> m_temp5minTicks;
    public ArrayList<Double> m_fiveMinPrices;



    public DataHandler() {
        this.m_reqId = 1; // increase one after each request
        this.m_clientId = 1234;
        this.m_period = "5 mins";
        this.m_nextValidOrderId = -1;
        this.m_newBidAskPrice = false;
        this.m_currentAskPrice = 0;
        this.m_currentBidPrice = 0;
        this.m_fiveMinBarRequestId = -1;
        this.m_marketDataRequestId = -1;

        // init member variable
        m_request = new Request(this);

        this.m_temp5minTicks = new HashMap<Long, Double>();

        // contract initlization
        this.m_contract = new Contract();
        this.m_contract.m_symbol = "EUR";
        this.m_contract.m_secType = "CASH";
        this.m_contract.m_currency = "USD";
        this.m_contract.m_exchange = "IDEALPRO";

        this.m_bars = new ArrayList<Bar>();
        this.m_fiveMinPrices = new ArrayList<Double>();;
        this.m_systemStartTimeString = "";

        m_request.eConnect(null, 7496, this.m_clientId);

        m_request.reqCurrentTime(); // request server time first


    }


    public void fetchContractData () {
        System.out.println("fetching data...");
        List<TagValue> XYZ = new ArrayList<TagValue>();
        this.m_fiveMinBarRequestId = this.m_reqId;
        this.m_request.reqHistoricalData(this.m_reqId++,
                this.m_contract, this.m_systemStartTimeString + " EST", "2 D",
                "5 mins", "MIDPOINT", 0, 2, XYZ);

        // no need to create one, if current time is unfinished 5 min bar, then the last bar is the current unfinished one

    }

    public void requestLiveData () {
        System.out.println("Request live data...");
        this.m_marketDataRequestId = this.m_reqId;
        List<TagValue> XYZ = new ArrayList<TagValue>();
        this.m_request.reqMarketDataType(2);
        this.m_request.reqMktData(this.m_reqId++, this.m_contract, "233", false, XYZ);
    }

    public void handleNew5minBar(long time) {
        // if new bar, create one and append it to m_bars

        if(this.m_newBidAskPrice) {

            double current5minCount =  Math.floor(time / (60 * 5));
            double prev5minCount = Math.floor(this.m_currentServerTime / (60 * 5));

            if(current5minCount > prev5minCount) {
                // new five min bar
                double midPrice = this.findMidPrice();
                long lastBarTime = this.m_bars.get(this.m_bars.size() - 1).time();
                Bar newBar = new Bar(lastBarTime + 5 * 60, midPrice, midPrice, midPrice, midPrice, -1, -1, -1);
                this.m_bars.add(newBar);
                this.m_fiveMinPrices.clear();
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
        double mid = this.findMidPrice();

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
        lastBar.m_high = high;
        lastBar.m_low = low;
        lastBar.m_close = mid;


    }


    @Override
    public void tickPrice(int tickerId, int field, double price, int canAutoExecute) {

        String fieldDesc = TickType.getField(field);

        switch (field) {
            case 1:
                // bid
                this.m_currentBidPrice = price;
                this.m_newBidAskPrice = true;
                break;
            case 2:
                // ask
                this.m_currentAskPrice = price;
                this.m_newBidAskPrice = true;
                break;
        }

        this.handleTickPrice();

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
        System.out.println("tickGeneric() -" +" type: "+tickType + " value: " + value);
    }

    @Override
    public void tickString(int tickerId, int tickType, String value) {
        TickType.getField(tickType);
        System.out.println("tickString() - " + "type: " + tickType + " value: " + value);
    }

    @Override
    public void tickEFP(int tickerId, int tickType, double basisPoints, String formattedBasisPoints, double impliedFuture, int holdDays, String futureExpiry, double dividendImpact, double dividendsToExpiry) {

    }

    @Override
    public void orderStatus(int orderId, String status, int filled, int remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {

    }

    @Override
    public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {

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

        // The next available order Id received from TWS upon connection.
        // Increment all successive orders by one based on this Id.
        this.m_nextValidOrderId = orderId;
        this.m_reqId = orderId + 1000000; // let request id not colide with order id
        this.fetchContractData();


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
            long cstTime = Long.parseLong(date.trim());
            Bar bar = new Bar(cstTime, high, low, open, close, -1, -1, -1);

            this.m_bars.add(bar);
        }

        if(open == -1 && reqId == this.m_fiveMinBarRequestId) {
            // request history data completed
            DecimalFormat f = new DecimalFormat("0.00000");
            DateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss");  // yyyymmdd hh:mm:ss tmz


            for(Bar b: m_bars) {
                Date d = new Date(b.m_time * 1000);

                System.out.println("time: " + b.m_time + " " + df.format(d) + " open: "+ f.format(b.open()) + " close: " + f.format(b.close()));
            }

            ArrayList<Double> output = new ArrayList<Double>();
            IFilter filter = new SuperSmootherFilter();
            filter.filter(this.m_bars, output);
            System.out.println("SuperSmootherFilter size is: " + output.size());
            for(Double result: output) {
                System.out.println(result);
            }
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
        this.m_currentServerTime = time;

        // if 1st time, then set the string
        if(this.m_systemStartTimeString.length() == 0) {
            Date dd = new Date(time * 1000); // multiply by 1000 to convert seconds to millisecond
            DateFormat format = new SimpleDateFormat("yyyyMMdd HH:mm:ss");  // yyyymmdd hh:mm:ss tmz
            this.m_systemStartTimeString = format.format(dd);
            System.out.println("m_systemStartTimeString (Local) is: " + this.m_systemStartTimeString + " - " + this.m_currentServerTime);


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
        System.out.println("error(int id, int errorCode, String errorMsg): "+errorMsg);
    }

    @Override
    public void connectionClosed() {

    }
}
