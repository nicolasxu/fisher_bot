package com.fisher;

import com.ib.client.Order;

import java.lang.reflect.Array;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created by nick on 7/8/15.
 */
public class KalmanBot implements IBot {

    ArrayList<Double> m_midTicks;
    KalmanFilter m_kFilter;
    ArrayList<Double> m_output;
    DataHandler handler;
    int m_initialOrderSize;
    double m_takeProfitSize;
    double m_comissionRate;

    public KalmanBot (DataHandler handler) {
        this.handler = handler;
        this.m_midTicks = handler.constantTicksToArrayList();
        if(this.m_midTicks == null) {
            System.out.println("tick list is empty when initializing KalmanBot, aborted...");
            return;
        }

        this.m_kFilter = new KalmanFilter(5);
        this.m_output = new ArrayList<Double>();
        this.m_initialOrderSize = 100000; // 100,000 Euros
        this.m_takeProfitSize = 50 * 0.00001; // 50 points
        this.m_comissionRate = 2 * 0.00001; // 0.2 base point * tradeValue
    }

    @Override
    public void calculate() {
        this.m_midTicks = handler.constantTicksToArrayList();
        this.m_kFilter.filter(this.m_midTicks, this.m_output);
    }

    public void cancelPendingOrders() {
        // TODO: make sure it works all the times
        this.handler.m_request.reqGlobalCancel();
    }

    public int calculateNextAmount() {
        // assuming next order will always reverse the direction of current position
        int    pos  = this.handler.m_position;
        double cost = this.handler.m_avgPositionCost;

        double currentPrice = (this.handler.m_currentBidPrice + this.handler.m_currentAskPrice) / 2;

        // calculate profit/loss

        if(pos == 0) {
            double comissionAmount = this.m_initialOrderSize * this.m_comissionRate * 2 / this.m_takeProfitSize;
            return this.m_initialOrderSize + (int)comissionAmount;
        }

        if(pos > 0) {
            // next order will be a sell order

            // at current price close position
            // -- sell at current bid price
            double currentBid = this.handler.m_currentBidPrice;
            double diff = currentBid - cost;
            if((diff) > 0) {
                // making money
                if(diff < this.m_takeProfitSize) {
                    // but not making enough
                    double adjOrderAmount = (this.m_takeProfitSize - diff) / this.m_takeProfitSize * this.m_initialOrderSize;
                    double comissionAmount = ((adjOrderAmount + this.m_initialOrderSize) * this.m_comissionRate) * 2 / this.m_takeProfitSize;
                    return ((int)adjOrderAmount + this.m_initialOrderSize + (int)comissionAmount);

                } else {
                    // making enough money
                    double comissionAmount = this.m_initialOrderSize * this.m_comissionRate * 2 / this.m_takeProfitSize;
                    return this.m_initialOrderSize + (int)comissionAmount;

                }
            } else {
                // losing money
                double amount = (-diff + this.m_takeProfitSize) / this.m_takeProfitSize * this.m_initialOrderSize;
                double comissionAmount = amount * this.m_comissionRate * 2 / this.m_takeProfitSize;
                return (int)(amount + comissionAmount);

            }

            // at current open new position


        } else {
            // next order will be a buy order
            double currentAsk = this.handler.m_currentAskPrice;
            double diff = cost - currentAsk;
            if(diff > 0) {
                // making money
                if(diff - this.m_takeProfitSize < 0) {
                    // but not making enough
                    double adjOrderAmount = (this.m_takeProfitSize - diff) / this.m_takeProfitSize * this.m_initialOrderSize;
                    double comissionAmount = (adjOrderAmount + this.m_initialOrderSize) * this.m_comissionRate * 2 / this.m_takeProfitSize;
                    return (int)(adjOrderAmount + comissionAmount);
                } else {
                    // making enough
                    double comissionAmount = this.m_initialOrderSize * this.m_comissionRate * 2 / this.m_takeProfitSize;
                    return this.m_initialOrderSize + (int)comissionAmount;
                }
            } else {
                // losing money
                double amount = (-diff + this.m_takeProfitSize) / this.m_takeProfitSize * this.m_initialOrderSize;
                double comissionAmount = amount * this.m_comissionRate * 2 / this.m_takeProfitSize;
                return (int)(amount + comissionAmount);
            }
        }
    }

    @Override
    public void decide() {
        int index = this.m_kFilter.buySellSignal.size() - 1;
        if(index < 1) {
            // only element in the result
            return;
        }

        int signal0  = this.m_kFilter.buySellSignal.get(index);
        int signalM1 = this.m_kFilter.buySellSignal.get(index - 1);

        if(signalM1 == 1 && signal0 == 0) {
            // sell
            int amount = this.calculateNextAmount();
            this.sell(amount);
        }

        if(signalM1 == 0 && signal0 == 1) {
            // buy
            int amount = this.calculateNextAmount();
            this.buy(amount);

        }

    }
    public double roundTo5(double input) {

        // 1.14042 => 1.14040
        double point = 0.00001;

        double number = Math.round(input / point / 5) * 5 * point;


        DecimalFormat df = new DecimalFormat("#.######");
        df.setRoundingMode(RoundingMode.HALF_UP);
        number = Double.parseDouble(df.format(number));
        System.out.println("roundTo5() result: " + number);
        return number;

    }
    public void buy(int amount) {
        System.out.println("buy triggered, amount: " + amount);

        Order buyOrder = new Order();
        buyOrder.m_clientId    = this.handler.m_clientId;
        //buyOrder.m_orderId is not used
        buyOrder.m_action      = "BUY";
        buyOrder.m_orderType   = "LMT";
        buyOrder.m_transmit    = true;
        buyOrder.m_tif         = "GTD";
        buyOrder.m_goodTillDate = this.handler.toTimeString(this.handler.m_serverTimeAlwaysValid + 5 * 60);
        buyOrder.m_lmtPrice    = this.roundTo5(this.handler.m_currentAskPrice);

        buyOrder.m_totalQuantity = amount;
        int buyOrderId = this.handler.m_reqId++;
        // placing order
        this.handler.m_request.placeOrder(buyOrderId, this.handler.m_contract, buyOrder);
        System.out.println("Buy order placed, id: " + buyOrderId);


    }

    public void sell(int amount) {
        System.out.println("sell triggered, amount: " + amount);

        Order sellOrder = new Order();
        sellOrder.m_clientId    = this.handler.m_clientId;
        //sellOrder.m_orderId is not used
        sellOrder.m_action      = "BUY";
        sellOrder.m_orderType   = "LMT";
        sellOrder.m_transmit    = true;
        sellOrder.m_tif         = "GTD";
        sellOrder.m_goodTillDate = this.handler.toTimeString(this.handler.m_serverTimeAlwaysValid + 5 * 60);
        sellOrder.m_lmtPrice    = this.roundTo5(this.handler.m_currentAskPrice);

        sellOrder.m_totalQuantity = amount;
        int buyOrderId = this.handler.m_reqId++;
        // placing order
        this.handler.m_request.placeOrder(buyOrderId, this.handler.m_contract, sellOrder);
        System.out.println("Buy order placed, id: " + buyOrderId);
        
    }

    public void closePosition() {


        if(this.handler.m_position == 0) {
            System.out.println("closePosition() - position already zero");
            return;
        }

        System.out.println("closePosition() - closing opposite position: " + this.handler.m_position );

        this.handler.m_request.reqGlobalCancel();

        Order closeOrder = new Order();
        closeOrder.m_clientId = this.handler.m_clientId;
        closeOrder.m_orderType = "MKT";
        closeOrder.m_transmit = true;
        closeOrder.m_totalQuantity = Math.abs(this.handler.m_position);

        if(this.handler.m_position > 0) {
            closeOrder.m_action = "SELL";
        }

        if(this.handler.m_position < 0) {
            closeOrder.m_action = "BUY";
        }
        int orderId = this.handler.m_reqId++;
        System.out.println("closePosition() - Since right now the position is: " + handler.m_position + " , placing "
                + closeOrder.m_action+ " order with quantity: " + closeOrder.m_totalQuantity + " to close position");
        this.handler.m_request.placeOrder(orderId, this.handler.m_contract, closeOrder);

    }
}
