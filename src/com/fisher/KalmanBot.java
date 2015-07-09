package com.fisher;

import com.ib.client.Order;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by nick on 7/8/15.
 */
public class KalmanBot implements IBot {

    ArrayList<Double> m_midTicks;
    KalmanFilter m_kFilter;
    ArrayList<Double> m_output;
    DataHandler handler;

    public KalmanBot (DataHandler handler) {
        this.handler = handler;
        this.m_midTicks = handler.constantTicksToArrayList();
        if(this.m_midTicks == null) {
            System.out.println("tick list is empty when initializing KalmanBot, aborted...");
            return;
        }

        this.m_kFilter = new KalmanFilter(5);
        this.m_output = new ArrayList<Double>();
    }

    @Override
    public void calculate() {
        this.m_midTicks = handler.constantTicksToArrayList();
        this.m_kFilter.filter(this.m_midTicks, this.m_output);
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
            this.buy(true);
        }

        if(signalM1 == 0 && signal0 == 1) {
            // buy
            this.sell(true);
        }

    }
    public void buy(boolean closeOppositePosition) {
        System.out.println("buy triggered, closePosition: " + closeOppositePosition);
        if(this.handler.m_position < 0 ) {
            this.closePosition();

        }

    }

    public void sell(boolean closeOppositePosition) {
        System.out.println("sell triggered, closePosition: " + closeOppositePosition);
        if(this.handler.m_position > 0) {
            this.closePosition();

        }

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
