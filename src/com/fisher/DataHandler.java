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

    public double m_currentBidPrice;
    public double m_currentAskPrice;
    public boolean m_newBidPrice;
    public boolean m_newAskPrice;

    public int m_fiveMinBarRequestId;
    public int m_marketDataRequestId;
    public HashMap<Long, Double> m_temp5minTicks;
    public ArrayList<Double> m_fiveMinPrices;

    public boolean m_newBarFlag;
    public BotDataPlotter m_appPlotter;
    public ILogger m_logger;

    public boolean testBuyOrderSent = false;
    public boolean testSellOrderSent = false;

    public HashMap<Integer, Order> m_orders;
    public HashMap<Integer, OrderState> m_orderStates;
    public ArrayList<Double> m_stopLossOrderIds;
    public ArrayList<Execution> m_executions;

    public int m_position;
    public double m_avgPositionCost;
    public double m_lastEmptyPosAccountValue;

    public int m_lastConstantTickCount;




    public ArrayList<Tick> m_constantDistanceTicks;
    public double m_tickFilterDistance;
    public long m_serverTimeAlwaysValid;

    public KalmanBot m_kalmanBot;



    public DataHandler(ILogger logger) {

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

        this.m_fiveMinPrices = new ArrayList<Double>();
        this.m_orders = new HashMap<Integer, Order>();
        this.m_orderStates = new HashMap<Integer, OrderState>();
        this.m_stopLossOrderIds = new ArrayList<Double>();

        this.m_systemStartTimeString = "";

        m_request.eConnect(null, 7496, this.m_clientId);

        //this.connectForever(60);

        m_request.reqCurrentTime(); // request server time first

        this.m_newBarFlag = false;

        this.m_executions = new ArrayList<Execution>();

        this.m_position = 0;
        this.m_avgPositionCost = 0;
        this.m_lastEmptyPosAccountValue = 0.0;
        this.m_tickFilterDistance = 50 * 0.00001; // 50 points
        this.m_constantDistanceTicks = new ArrayList<Tick>();

        this.m_serverTimeAlwaysValid = 0;

        this.m_kalmanBot = new KalmanBot(this);
        this.m_lastConstantTickCount = 0;

    }
    public void loadConstantTicks() {
        // load constant today ticks
        // only today
        // TODO
    }

    public ArrayList<Double> constantTicksToArrayList() {
        ArrayList<Double> result = new ArrayList<Double>();
        for(Tick tick: this.m_constantDistanceTicks) {
            result.add((tick.bid + tick.ask) / 2);
        }
        return result;
    }
    public void buildConstantDistanceTicks() {

        double currentMidPrice = 0;

        if(this.m_constantDistanceTicks.size() == 0) {
            // empty, first tick
            currentMidPrice = this.findBidAskPriceBothValid();

            if(currentMidPrice == 0) {
                // if current price not valid, then just skip this tick
                System.out.println("currentMidPrice is 0, no tick added");
                return;
            }

            // put into array list
            this.m_constantDistanceTicks.add(new Tick(this.m_serverTimeAlwaysValid, this.m_currentBidPrice, this.m_currentAskPrice));

        } else {
            // not empty
            // 1. get last stored tick
            int total = this.m_constantDistanceTicks.size();
            Tick lastTick = this.m_constantDistanceTicks.get(total - 1);
            double lastMidPrice = (lastTick.bid + lastTick.ask) / 2;
            currentMidPrice = (this.m_currentBidPrice + this.m_currentAskPrice) / 2;

            if(Math.abs(lastMidPrice - currentMidPrice) >= this.m_tickFilterDistance ) {
                this.m_constantDistanceTicks.add(new Tick(this.m_serverTimeAlwaysValid, this.m_currentBidPrice, this.m_currentAskPrice));
                System.out.println("new m_constantDistanceTicks.size(): " + this.m_constantDistanceTicks.size());
            } else {
                System.out.println("buildConstantDistanceTicks() - skip current current tick: " + currentMidPrice);
            }
        }
    }

    public void connectForever(int seconds) {
        class AlwaysConnect extends TimerTask {

            /**
             * The action to be performed by this timer task.
             */
            @Override
            public void run() {
                System.out.println("checking connection...");
                if (m_request.isConnected() == false) {
                    m_request.eConnect(null, 7496, m_clientId);
                }
            }
        }
        TimerTask theTask = new AlwaysConnect();
        Timer theTimer = new Timer();
        theTimer.schedule(theTask, 0, seconds * 1000); // every one min

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

    public void build5minBar(long time) {
        // if new bar, create one and append it to m_bars,
        // if not new bar, update current bar based on the latest tick price
        // Only build 5 min bars, no bot logic in this function

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
                // not a new bar
                // update last bar, no new bar
                this.updateCurrentBarHighLowPrice();

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

    public double findBidAskMidPrice() {
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

    public double findBidAskPriceBothValid() {

        if(this.m_currentBidPrice > 0 && this.m_currentAskPrice > 0) {
            // both bid and ask has value
            return (this.m_currentBidPrice + this.m_currentAskPrice) / 2;
        }

        // if either bid or ask is empty, then return 0;
        return 0.0;
    }

    public void updateCurrentBarHighLowPrice() {
        // always put data to last m_bars

        // 1. find the mid price
        double bidAskMid = this.findBidAskMidPrice(); // from the current bid and ask price, not from bar high and low price

        // 2. add to temp price list
        this.m_fiveMinPrices.add(bidAskMid);
        System.out.println("m_fiveMinPrices.size(): " + this.m_fiveMinPrices.size());

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
        int latestBarIndex = m_bars.size() - 1;
        Bar lastBar = m_bars.get(latestBarIndex);

        if(high > lastBar.m_high) {
            lastBar.m_high = high;
        }

        if(low < lastBar.m_low) {
            lastBar.m_low = low;
        }

        lastBar.m_close = bidAskMid;

    }

    @Override
    public void tickPrice(int tickerId, int field, double price, int canAutoExecute) {
        // tickPrice() -> currentTime() -> build5minBar() -> updateCurrentBarHighLowPrice()

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



        // potential action for new price is handled in currentTime() handler
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

    }

    @Override
    public void tickString(int tickerId, int tickType, String value) {
        TickType.getField(tickType);

    }

    @Override
    public void tickEFP(int tickerId, int tickType, double basisPoints, String formattedBasisPoints, double impliedFuture, int holdDays, String futureExpiry, double dividendImpact, double dividendsToExpiry) {

    }

    @Override
    public void orderStatus(int orderId, String status, int filled, int remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {

    }

    @Override
    public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
        this.m_logger.log("openOrder(), orderId: " + orderId + " orderStatus.m_status: " + orderState.m_status + " order.m_orderId: " + order.m_orderId);

        // print out taking profit message trigger msg
        if(orderState.m_status.compareTo("Filled") == 0) {
            if(order.m_parentId > 0 && order.m_orderType.compareTo("LMT") == 0) {
                // taking profit triggered
                System.out.println("taking profit triggered at: " + order.m_lmtPrice);
                m_logger.log("taking profit triggered at:"  + order.m_lmtPrice);
                m_logger.log("profit is: " + 50 * 0.00001 * order.m_totalQuantity);
                System.out.println("profit is: " + 50 * 0.00001 * order.m_totalQuantity);
            }
        }

        this.m_orderStates.put(orderId, orderState);
        this.m_orders.put(orderId, order);



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
        // This method is called when the reqExecutions() method is invoked, or when an order is filled.

        if(contract.m_symbol.compareTo(this.m_contract.m_symbol) != 0) {
            // if not "EUR", don't do nothing.
            return;
        }

        this.m_executions.add(execution);
        System.out.println("ExecDetails() - " + "reqId: "+reqId + " side: " + execution.m_side + " " + execution.m_avgPrice);

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

            // **** call bot calculate() and decide() if needed when historical data is downloaded

           // TODO: here

            // ****

            // bind data to plotter
            //this.m_appPlotter.setBarSource(this.m_bars);
            //this.m_appPlotter.setFisherSource(m_fisherBot.m_fisher);
            //this.m_appPlotter.setTriggerSource(m_fisherBot.m_trigger);
            //this.m_appPlotter.setStdSource(m_fisherBot.m_standDeviations);
            //this.m_appPlotter.purgeDrawingData();
            //this.m_appPlotter.updatePlotData();

            this.requestLiveData();
            // only need to call reqAccountSummary() once to subscribe the data
            this.m_request.reqAccountSummary(this.m_reqId++, "All", "TotalCashValue");



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

        this.m_serverTimeAlwaysValid = time;

        //this.build5minBar(time);

        this.buildConstantDistanceTicks();

        if(this.m_constantDistanceTicks.size() > this.m_lastConstantTickCount) {
            // new qualified tick generated,
            this.m_kalmanBot.calculate();
            this.m_kalmanBot.decide();

            // update tick count
            this.m_lastConstantTickCount = this.m_constantDistanceTicks.size();

        } else {
            // could also do something when
        }



        // if 1st time, then set the time string
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
            System.out.println("position() - found \"EUR\": " + pos);
            this.m_logger.log("position is: " + pos + " for contract: " + contract.m_symbol);
            this.m_position = pos;
            this.m_avgPositionCost = avgCost;

            if(pos == 0) {
                // all position is liquidated, request summary for "SettledCash"
                //System.out.println("requesting account summary... in position()...");

            }
        }
    }

    @Override
    public void positionEnd() {

    }

    @Override
    public void accountSummary(int reqId, String account, String tag, String value, String currency) {
        System.out.println("accountSummary() triggered with tag: " + tag +" position: " + this.m_position);
        if(tag.compareTo("TotalCashValue") == 0 && this.m_position == 0) {
            System.out.println("TotalCashValue tag with position == 0");
            // only run this when all positions are closed
            System.out.println("TotalCashValue: " + value);
            double currentValue = Double.parseDouble(value);
            if(this.m_lastEmptyPosAccountValue !=0) {
                // last value is valid
                System.out.println("lastSettledCash: " + this.m_lastEmptyPosAccountValue + " current Settled Cash: " + currentValue);
                double diff = currentValue - this.m_lastEmptyPosAccountValue;
                if(diff > 0) {
                    System.out.println("making money: " + diff);

                    this.m_lastEmptyPosAccountValue = currentValue;
                } else {
                    System.out.println("lossing money: " + diff);
                    // calculate next order size e.g.:


                }


            } else {
                // value is not valid, probably start up
                this.m_lastEmptyPosAccountValue = currentValue;
                System.out.println("set m_lastEmptyPosAccountValue to " + currentValue);
            }

        }
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
        System.out.println("connectionClosed() - connection closed ");
    }
}
