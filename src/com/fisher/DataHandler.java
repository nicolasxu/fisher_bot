package com.fisher;

import com.ib.client.*;
import com.ib.controller.Bar;
import com.ib.controller.ConcurrentHashSet;

import java.text.DateFormat;
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
    public HashMap<Long, Double> m_temp5minTicks;
    public Bar m_unfinishedBar;


    public DataHandler() {
        this.m_reqId = 1; // increase one after each request
        this.m_clientId = 1234;
        this.m_period = "5 mins";
        this.m_nextValidOrderId = -1;
        this.m_newBidAskPrice = false;
        this.m_currentAskPrice = 0;
        this.m_currentBidPrice = 0;


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
        this.m_systemStartTimeString = "";


        m_request.eConnect(null, 7496, this.m_clientId);

        m_request.reqCurrentTime(); // request server time first
        m_request.TwsConnectionTime(); // request tws connection time


    }
    public void fetchContractData () {
        System.out.println("fetching data...");
        List<TagValue> XYZ = new ArrayList<TagValue>();


        this.m_request.reqHistoricalData(this.m_reqId++,
                this.m_contract, this.m_systemStartTimeString + " CST", "2 D",
                "5 mins", "BID_ASK", 0, 2, XYZ );

    }

    public void requestLiveData () {
        System.out.println("Request live data...");
        List<TagValue> XYZ = new ArrayList<TagValue>();
        this.m_request.reqMktData(this.m_reqId++, this.m_contract, "233", false, XYZ);
    }


    @Override
    public void tickPrice(int tickerId, int field, double price, int canAutoExecute) {
        this.m_request.reqCurrentTime();
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

        //System.out.println("tickPrice() - Req: " + tickerId + " field:" + fieldDesc + " price: " + price);
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
        this.fetchContractData();
        this.requestLiveData();

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

        if(open != -1) {
            // Bar( long time, double high, double low, double open, double close, double wap, long volume, int count)
            Bar bar = new Bar(Long.parseLong(date.trim()), high, low, open, close, -1, -1, -1);
            this.m_bars.add(bar);
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

        if(this.m_newBidAskPrice) {
            // when receiving new bid or ask price
            double current5minCount =  Math.floor(time / (60*5));
            double prev5minCount = Math.floor(this.m_currentServerTime / (60 * 5));
            if(current5minCount > prev5minCount) {
                // new 5 min bar
                double mid = (m_currentAskPrice + m_currentBidPrice) / 2;

                // fix unfinished bar time: need to set time, e.g: previous bar time + 300
                long lastBarTime = this.m_bars.get(this.m_bars.size() - 1).time();
                this.m_unfinishedBar.m_time = lastBarTime + 60 * 5;

                // append to the end of the ArrayList
                this.m_bars.add(this.m_unfinishedBar);
                Bar lastBar = m_bars.get(m_bars.size() -1);
                System.out.println("lastBar.time: " + lastBar.time() + " lastBar.open: " + lastBar.open() +
                        " lastBar.Close: " + lastBar.close() + " lastBar.low: "+ lastBar.low() +
                        " lastBar.high: "+ lastBar.high());

                // create new unfinished bar
                this.m_unfinishedBar = new Bar(time, mid, mid, mid, mid, -1, -1, -1 );
                System.out.println(time + " - new 5 min bar");
                m_temp5minTicks.clear(); // clear everything in temp tick hasp map
                System.out.println("m_temp5minTicks clearing: " + m_temp5minTicks.size());

            } else {
                // still current 5 min bar
                m_temp5minTicks.put(time, (m_currentAskPrice + m_currentAskPrice) / 2 );
                //System.out.println("tick number is: " + m_temp5minTicks.size());


                // update bar
                double high = 0;
                double low = 10;
                for(HashMap.Entry<Long, Double> entry : m_temp5minTicks.entrySet()) {

                    Double tickPrice = entry.getValue();
                    if (tickPrice > high) {
                        high = tickPrice;
                    }
                    if (tickPrice < low) {
                        low = tickPrice;
                    }
                }

                if(this.m_unfinishedBar == null) {
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
                    this.m_unfinishedBar = new Bar(time, mid, mid, mid, mid, -1, -1, -1);
                }

                this.m_unfinishedBar.m_close = (m_currentAskPrice + m_currentAskPrice) / 2;
                this.m_unfinishedBar.m_high = high;
                this.m_unfinishedBar.m_low = low;
            }
        }

        this.m_currentServerTime = time;

        // if 1st time, then set the string
        if(this.m_systemStartTimeString.length() == 0) {
            Date dd = new Date(time * 1000); // multiply by 1000 to convert seconds to millisecond
            DateFormat format = new SimpleDateFormat("yyyyMMdd hh:mm:ss");  // yyyymmdd hh:mm:ss tmz
            this.m_systemStartTimeString = format.format(dd);
            System.out.println("System time is: " + this.m_systemStartTimeString + " - " + this.m_currentServerTime);
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
