package com.fisher;

import com.ib.client.Order;

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
    double m_commissionRate;
    double m_lastOrderPrice;
    boolean m_takingProfitWorking;

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
        this.m_commissionRate = 2 * 0.00001; // 0.2 base point * tradeValue
        this.m_lastOrderPrice = 0; // init to 0, and will be assigned everything new order is created,
                                   // but not for take profit order
        this.m_takingProfitWorking = false;
    }

    @Override
    public void calculate() {
        System.out.println("KalmanBot calculating...");
        this.m_midTicks = handler.constantTicksToArrayList();
        this.m_kFilter.filter(this.m_midTicks, this.m_output);
    }

    public void cancelPendingOrders() {
        // TODO: make sure it works all the times
        this.handler.m_request.reqGlobalCancel();
    }

    public int calculateNewOrderAmount() {
        System.out.println("calculateNewOrderAmount()...");
        // assuming next order will always reverse the direction of current position
        int    pos  = this.handler.m_position;
        double cost = this.handler.m_avgPositionCost;

        double currentPrice = (this.handler.m_currentBidPrice + this.handler.m_currentAskPrice) / 2;

        // calculate profit/loss

        if(pos == 0) {

            double commissionAmount = this.m_initialOrderSize * this.m_commissionRate * 2 / this.m_takeProfitSize;
            System.out.println("calculateNewOrderAmount() - since position is 0, next amount is: " + this.m_initialOrderSize + (int)commissionAmount );
            return this.m_initialOrderSize + (int)commissionAmount;
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
                    double adjOrderAmount = (this.m_takeProfitSize - diff + this.m_takeProfitSize) / this.m_takeProfitSize * this.m_initialOrderSize;
                    double commissionAmount = ((adjOrderAmount + pos) * this.m_commissionRate) * 2 / this.m_takeProfitSize;
                    return ((int)adjOrderAmount + pos + (int)commissionAmount);

                } else {
                    // making enough money
                    double commissionAmount = (this.m_initialOrderSize + pos )* this.m_commissionRate * 2 / this.m_takeProfitSize;
                    return this.m_initialOrderSize + pos + (int)commissionAmount;

                }
            } else {
                // losing money
                double amount = (-diff + this.m_takeProfitSize) / this.m_takeProfitSize * this.m_initialOrderSize;
                double commissionAmount = (amount + pos) * this.m_commissionRate * 2 / this.m_takeProfitSize;
                return (int)(amount + pos + commissionAmount);

            }

            // at current open new position


        } else {
            // next order will be a buy order, pos is negative
            double currentAsk = this.handler.m_currentAskPrice;
            double diff = cost - currentAsk;
            if(diff > 0) {
                // making money
                if(diff - this.m_takeProfitSize < 0) {
                    // but not making enough
                    double adjOrderAmount = (this.m_takeProfitSize - diff + this.m_takeProfitSize) / this.m_takeProfitSize * this.m_initialOrderSize;
                    double commissionAmount = (adjOrderAmount + -pos) * this.m_commissionRate * 2 / this.m_takeProfitSize;
                    return (int)(adjOrderAmount + -pos + commissionAmount );
                } else {
                    // making enough
                    double commissionAmount = (this.m_initialOrderSize + -pos ) * this.m_commissionRate * 2 / this.m_takeProfitSize;
                    return this.m_initialOrderSize + (int)commissionAmount + -pos;
                }
            } else {
                // losing money
                double amount = (-diff + this.m_takeProfitSize) / this.m_takeProfitSize * this.m_initialOrderSize;
                double commissionAmount = (amount + -pos) * this.m_commissionRate * 2 / this.m_takeProfitSize;
                return (int)(amount + -pos + commissionAmount);
            }
        }
    }

    public void tryTakeProfit() {
        if(this.handler.m_position == 0) {
            this.m_takingProfitWorking = false;
            return;
        }
        if(m_takingProfitWorking == false) {

            if(this.handler.m_position > 0) {

                // sell at bid price
                double bidPrice = this.roundTo5(this.handler.m_currentBidPrice);
                if(bidPrice - this.m_lastOrderPrice >= this.m_takeProfitSize) {
                    // good to close
                    System.out.println("tryTakeProfit() - selling " + this.handler.m_position);
                    this.m_takingProfitWorking = true;
                    sell(this.handler.m_position);
                }
            } else {
                // buy at ask price
                double askPrice = this.roundTo5(this.handler.m_currentAskPrice);
                if(askPrice - this.m_lastOrderPrice >= this.m_takeProfitSize) {
                    // good to close
                    System.out.println("tryTakeProfit() - buying " + this.handler.m_position);
                    this.m_takingProfitWorking = true;
                    buy(-this.handler.m_position);
                }
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

        /*
        if(signal0 == signalM1) {
            // in the same Kalman trend as the previous point
            // check for take profit
            this.tryTakeProfit();
        }
        */

        if(signalM1 == 1 && signal0 == 0) {
            // sell
            int amount = this.calculateNewOrderAmount();
            System.out.println("selling signal triggered, amount: " + amount);
            this.sell(amount);
            this.m_lastOrderPrice = this.roundTo5(this.handler.m_currentBidPrice);
        }

        if(signalM1 == 0 && signal0 == 1) {
            // buy
            int amount = this.calculateNewOrderAmount();
            System.out.println("buying signal triggered, amount: " + amount);
            this.buy(amount);
            this.m_lastOrderPrice = this.roundTo5(this.handler.m_currentAskPrice);

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
        sellOrder.m_action      = "SELL";
        sellOrder.m_orderType   = "LMT";
        sellOrder.m_transmit    = true;
        sellOrder.m_tif         = "GTD";
        sellOrder.m_goodTillDate = this.handler.toTimeString(this.handler.m_serverTimeAlwaysValid + 5 * 60);
        sellOrder.m_lmtPrice    = this.roundTo5(this.handler.m_currentAskPrice);

        sellOrder.m_totalQuantity = amount;
        int sellOrderId = this.handler.m_reqId++;
        // placing order
        this.handler.m_request.placeOrder(sellOrderId, this.handler.m_contract, sellOrder);
        System.out.println("Buy order placed, id: " + sellOrderId);

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
