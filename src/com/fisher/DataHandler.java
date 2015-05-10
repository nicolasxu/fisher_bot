package com.fisher;

import com.ib.client.*;
import com.ib.controller.Bar;

import javax.swing.*;
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
    public JFrame m_appFrame;



    public DataHandler() {
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
        this.m_fiveMinPrices = new ArrayList<Double>();;
        this.m_systemStartTimeString = "";

        this.m_fisherBot = new FisherBot(this, this.m_medians);

        m_request.eConnect(null, 7496, this.m_clientId);

        m_request.reqCurrentTime(); // request server time first

        this.m_newBarFlag = false;


    }

    public void setOutsideFrame (JFrame theFrame) {
        this.m_appFrame = theFrame;
    }


    public void fetchContractHistoricalData() {
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

        if(this.m_newBidPrice || this.m_newAskPrice) {


            double current5minCount =  Math.floor(time / (60 * 5));
            double prev5minCount = Math.floor(this.m_currentServerTime / (60 * 5));

            if(current5minCount > prev5minCount) {

                // new bar, handle it when receive both bid and ask price
                this.m_newBarFlag = true;

                if(this.m_newAskPrice && this.m_newBidPrice) {

                    // new five min bar
                    double midPrice = this.findMidPrice(); // average between latest bid and ask price
                    long lastBarTime = this.m_bars.get(this.m_bars.size() - 1).time();
                    Bar newBar = new Bar(lastBarTime + 5 * 60, midPrice, midPrice, midPrice, midPrice, -1, -1, -1);

                    // Add new Bar
                    this.m_bars.add(newBar);
                    this.m_fiveMinPrices.clear();

                    // create and add new mediens
                    this.m_medians.add(midPrice);

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

        if(high > lastBar.m_high) {
            lastBar.m_high = high;
        }

        if(low < lastBar.m_low) {
            lastBar.m_low = low;
        }

        lastBar.m_close = mid;

        // 5. update last m_medians
        this.m_medians.set(lastIndex, (lastBar.high() + lastBar.low()) / 2 );


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
                break;
            case 2:
                // ask
                this.m_currentAskPrice = price;
                this.m_newAskPrice = true;
                break;
            default:

                break;
        }

        //this.handleTickPrice();

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
        this.fetchContractHistoricalData();


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

                double inputNumber = b.high() * 0.5 + b.low() * 0.5;

                // enable below line for live data:
                m_medians.add(inputNumber);

                // generate data for MT5
                //System.out.println("ssTestPrice["+m_bars.indexOf(b)+"]= "+ f.format(inputNumber)   +";");
            }


            /*
            /// data (remove them for live usage) ///

            smoothInput.add(1.1219);
            smoothInput.add(1.12225);
            smoothInput.add(1.12234);
            smoothInput.add(1.1221);
            smoothInput.add(1.12181);
            smoothInput.add(1.12156);
            smoothInput.add(1.12147);
            smoothInput.add(1.12134);
            smoothInput.add(1.12138);
            smoothInput.add(1.12166);
            smoothInput.add(1.12142);
            smoothInput.add(1.12132);
            smoothInput.add(1.1211);
            smoothInput.add(1.12113);
            smoothInput.add(1.12154);
            smoothInput.add(1.12177);
            smoothInput.add(1.12164);
            smoothInput.add(1.12126);
            smoothInput.add(1.12105);
            smoothInput.add(1.12116);
            smoothInput.add(1.1213);
            smoothInput.add(1.1212);
            smoothInput.add(1.12138);
            smoothInput.add(1.12146);
            smoothInput.add(1.12145);
            smoothInput.add(1.12145);
            smoothInput.add(1.12145);
            smoothInput.add(1.12135);
            smoothInput.add(1.12129);
            smoothInput.add(1.1215);
            smoothInput.add(1.12147);
            smoothInput.add(1.12147);
            smoothInput.add(1.12131);
            smoothInput.add(1.12099);
            smoothInput.add(1.12138);
            smoothInput.add(1.12166);
            smoothInput.add(1.12135);
            smoothInput.add(1.12135);
            smoothInput.add(1.12135);
            smoothInput.add(1.12109);
            smoothInput.add(1.12099);
            smoothInput.add(1.12086);
            smoothInput.add(1.12111);
            smoothInput.add(1.1213);
            smoothInput.add(1.12101);
            smoothInput.add(1.12084);
            smoothInput.add(1.12102);
            smoothInput.add(1.12106);
            smoothInput.add(1.12114);
            smoothInput.add(1.12116);
            smoothInput.add(1.12115);
            smoothInput.add(1.12132);
            smoothInput.add(1.1214);
            smoothInput.add(1.12126);
            smoothInput.add(1.1214);
            smoothInput.add(1.1214);
            smoothInput.add(1.12105);
            smoothInput.add(1.12098);
            smoothInput.add(1.12121);
            smoothInput.add(1.121);
            smoothInput.add(1.1206);
            smoothInput.add(1.12054);
            smoothInput.add(1.1206);
            smoothInput.add(1.12074);
            smoothInput.add(1.12055);
            smoothInput.add(1.1206);
            smoothInput.add(1.12082);
            smoothInput.add(1.12078);
            smoothInput.add(1.12072);
            smoothInput.add(1.12078);
            smoothInput.add(1.12072);
            smoothInput.add(1.12087);
            smoothInput.add(1.12102);
            smoothInput.add(1.12092);
            smoothInput.add(1.12089);
            smoothInput.add(1.12092);
            smoothInput.add(1.12104);
            smoothInput.add(1.12101);
            smoothInput.add(1.12092);
            smoothInput.add(1.12099);
            smoothInput.add(1.1209);
            smoothInput.add(1.12089);
            smoothInput.add(1.12102);
            smoothInput.add(1.12102);
            smoothInput.add(1.12095);
            smoothInput.add(1.12084);
            smoothInput.add(1.12072);
            smoothInput.add(1.12062);
            smoothInput.add(1.12062);
            smoothInput.add(1.12072);
            smoothInput.add(1.12092);
            smoothInput.add(1.12092);
            smoothInput.add(1.1208);
            smoothInput.add(1.12078);
            smoothInput.add(1.12098);
            smoothInput.add(1.12095);
            smoothInput.add(1.12095);
            smoothInput.add(1.12085);
            smoothInput.add(1.12082);
            smoothInput.add(1.1211);
            smoothInput.add(1.12119);
            smoothInput.add(1.12112);
            smoothInput.add(1.12112);
            smoothInput.add(1.12136);
            smoothInput.add(1.12173);
            smoothInput.add(1.12217);
            smoothInput.add(1.12138);
            smoothInput.add(1.12052);
            smoothInput.add(1.12122);
            smoothInput.add(1.12169);
            smoothInput.add(1.12219);
            smoothInput.add(1.12175);
            smoothInput.add(1.12211);
            smoothInput.add(1.1221);
            smoothInput.add(1.12278);
            smoothInput.add(1.12406);
            smoothInput.add(1.1246);
            smoothInput.add(1.1241);
            smoothInput.add(1.12403);
            smoothInput.add(1.12381);
            smoothInput.add(1.12501);
            smoothInput.add(1.12474);
            smoothInput.add(1.12456);
            smoothInput.add(1.12396);
            smoothInput.add(1.12443);
            smoothInput.add(1.12519);
            smoothInput.add(1.12464);
            smoothInput.add(1.12555);
            smoothInput.add(1.126);
            smoothInput.add(1.12635);
            smoothInput.add(1.12567);
            smoothInput.add(1.12575);
            smoothInput.add(1.12525);
            smoothInput.add(1.12501);
            smoothInput.add(1.12455);
            smoothInput.add(1.12472);
            smoothInput.add(1.12562);
            smoothInput.add(1.12544);
            smoothInput.add(1.12504);
            smoothInput.add(1.12528);
            smoothInput.add(1.12595);
            smoothInput.add(1.1255);
            smoothInput.add(1.12567);
            smoothInput.add(1.12623);
            smoothInput.add(1.12766);
            smoothInput.add(1.1271);
            smoothInput.add(1.12635);
            smoothInput.add(1.12492);
            smoothInput.add(1.12385);
            smoothInput.add(1.1238);
            smoothInput.add(1.12396);
            smoothInput.add(1.12419);
            smoothInput.add(1.12412);
            smoothInput.add(1.12385);
            smoothInput.add(1.12426);
            smoothInput.add(1.12384);
            smoothInput.add(1.12367);
            smoothInput.add(1.12385);
            smoothInput.add(1.12452);
            smoothInput.add(1.1251);
            smoothInput.add(1.12596);
            smoothInput.add(1.12645);
            smoothInput.add(1.12665);
            smoothInput.add(1.1268);
            smoothInput.add(1.12669);
            smoothInput.add(1.12668);
            smoothInput.add(1.127);
            smoothInput.add(1.12731);
            smoothInput.add(1.1275);
            smoothInput.add(1.12748);
            smoothInput.add(1.12692);
            smoothInput.add(1.12666);
            smoothInput.add(1.12677);
            smoothInput.add(1.12729);
            smoothInput.add(1.12715);
            smoothInput.add(1.12679);
            smoothInput.add(1.1265);
            smoothInput.add(1.12581);
            smoothInput.add(1.1257);
            smoothInput.add(1.12538);
            smoothInput.add(1.12628);
            smoothInput.add(1.12686);
            smoothInput.add(1.1269);
            smoothInput.add(1.12628);
            smoothInput.add(1.12615);
            smoothInput.add(1.12575);
            smoothInput.add(1.12502);
            smoothInput.add(1.12555);
            smoothInput.add(1.12573);
            smoothInput.add(1.12582);
            smoothInput.add(1.12608);
            smoothInput.add(1.12621);
            smoothInput.add(1.12584);
            smoothInput.add(1.1261);
            smoothInput.add(1.12553);
            smoothInput.add(1.1258);
            smoothInput.add(1.1251);
            smoothInput.add(1.12505);
            smoothInput.add(1.1252);
            smoothInput.add(1.1256);
            smoothInput.add(1.1269);
            smoothInput.add(1.12606);
            smoothInput.add(1.12338);
            smoothInput.add(1.12344);
            smoothInput.add(1.12207);
            smoothInput.add(1.1218);
            smoothInput.add(1.1219);
            smoothInput.add(1.1218);
            smoothInput.add(1.12176);
            smoothInput.add(1.1227);
            smoothInput.add(1.12245);
            smoothInput.add(1.12193);
            smoothInput.add(1.12233);
            smoothInput.add(1.12195);
            smoothInput.add(1.12113);
            smoothInput.add(1.1207);
            smoothInput.add(1.12011);
            smoothInput.add(1.11908);
            smoothInput.add(1.11986);
            smoothInput.add(1.1202);
            smoothInput.add(1.12);
            smoothInput.add(1.11996);
            smoothInput.add(1.11974);
            smoothInput.add(1.11969);
            smoothInput.add(1.11972);
            smoothInput.add(1.1192);
            smoothInput.add(1.1187);
            smoothInput.add(1.11889);
            smoothInput.add(1.11879);
            smoothInput.add(1.11894);
            smoothInput.add(1.11961);
            smoothInput.add(1.12049);
            smoothInput.add(1.11999);
            smoothInput.add(1.11976);
            smoothInput.add(1.11904);
            smoothInput.add(1.11837);
            smoothInput.add(1.1181);
            smoothInput.add(1.11819);
            smoothInput.add(1.1188);
            smoothInput.add(1.11859);
            smoothInput.add(1.11872);
            smoothInput.add(1.11895);
            smoothInput.add(1.11929);
            smoothInput.add(1.11966);
            smoothInput.add(1.11945);
            smoothInput.add(1.11919);
            smoothInput.add(1.11914);
            smoothInput.add(1.11929);
            smoothInput.add(1.1196);
            smoothInput.add(1.11975);
            smoothInput.add(1.11998);
            smoothInput.add(1.11993);
            smoothInput.add(1.11957);
            smoothInput.add(1.11938);
            smoothInput.add(1.11987);
            smoothInput.add(1.11976);
            smoothInput.add(1.11954);
            smoothInput.add(1.11909);
            smoothInput.add(1.11871);
            smoothInput.add(1.11869);
            smoothInput.add(1.11882);
            smoothInput.add(1.11947);
            smoothInput.add(1.11978);
            smoothInput.add(1.11965);
            smoothInput.add(1.11985);
            smoothInput.add(1.1198);
            smoothInput.add(1.11994);
            smoothInput.add(1.11999);
            smoothInput.add(1.12074);
            smoothInput.add(1.12159);
            smoothInput.add(1.12115);
            smoothInput.add(1.12106);
            smoothInput.add(1.12105);
            smoothInput.add(1.12116);
            smoothInput.add(1.12156);
            smoothInput.add(1.12154);
            smoothInput.add(1.1216);
            smoothInput.add(1.12135);
            smoothInput.add(1.12095);
            smoothInput.add(1.12072);
            smoothInput.add(1.12062);
            smoothInput.add(1.12039);
            smoothInput.add(1.1203);
            smoothInput.add(1.11976);
            smoothInput.add(1.11972);



            /// data ///

            IFilter ssfilter = new SuperSmootherFilter();
            ssfilter.filter(smoothInput, smoothed);
            System.out.println("SuperSmootherFilter size is: " + smoothed.size() + ", below is smoothed data ");
            for(Double result: smoothed) {
                System.out.println(f.format(result));
            }
            ArrayList<Double> outputFisher = new ArrayList<Double>();
            ArrayList<Double> outputTrigger = new ArrayList<Double>();

            FisherFilter fisherFilter = new FisherFilter();
            fisherFilter.filter(smoothed, outputFisher, outputTrigger);

            DecimalFormat f2 = new DecimalFormat("0.000000");
            System.out.println("below is fisher output");
            for(Double result: outputFisher) {

                System.out.println( "outputFisher[" + outputFisher.indexOf(result) + "]:" + f2.format(result));
            }

            */
            this.m_fisherBot.calculate();
            this.m_fisherBot.decide();
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

            Date dd = new Date(time * 1000); // multiply by 1000 to convert seconds to millisecond
            DateFormat format = new SimpleDateFormat("yyyyMMdd HH:mm:ss");  // yyyymmdd hh:mm:ss tmz
            this.m_systemStartTimeString = format.format(dd);
            System.out.println("m_systemStartTimeString (Local) is: " + this.m_systemStartTimeString + " - " + this.m_currentServerTime);

            // TODO, bug here, first bar will have high low price bug, need to copy high low price to m_tempFiveMinPrice

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
