package com.fisher;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created by nick on 7/8/15.
 */
public class KalmanFilter extends IFilter {

    enum SignalMode {
        TREND,
        KALMAN
    };

    int smoothRatio;
    double sqrtK100;
    double k100;
    int min_rates_total;

    double velocity;
    double previousVelocity;
    double distance;
    double error;

    public ArrayList<Integer> buySellSignal;

    SignalMode signal;


    public KalmanFilter(int ratio) {
        this.smoothRatio = ratio;
        this.signal = SignalMode.KALMAN;
        this.min_rates_total = 2;
        this.sqrtK100 = Math.sqrt(this.smoothRatio/100.0);
        this.k100 = this.smoothRatio/100.0;
        this.buySellSignal = new ArrayList<Integer>();



    }
    public void setSignalModeKalman() {
        this.signal = SignalMode.KALMAN;
    }
    public void setSignalModeTrend() {
        this.signal = SignalMode.TREND;
    }


    public void filter(ArrayList<Double> input, ArrayList<Double> output) {

        if(input.size() < this.min_rates_total) {
            return;
        }

        DecimalFormat df = new DecimalFormat("#.########");
        df.setRoundingMode(RoundingMode.HALF_UP);

        for(int i = Math.max(0, output.size() -1 ); i < input.size(); i++) {

            if(i < 1) {
                Double inputNumber = input.get(i);
                output.add(inputNumber);
                buySellSignal.add(-1);
                this.previousVelocity = 0.0;
                this.velocity = this.previousVelocity;

            } else {
                this.velocity = this.previousVelocity;

                // store the velocity before running at current bar
                if(i == input.size() - 1) {

                    this.velocity = this.previousVelocity;
                }
                this.distance = input.get(i) - output.get(i - 1);
                this.distance = Double.parseDouble(df.format(this.distance));

                this.error    = output.get(i-1) + this.distance * this.sqrtK100;

                this.velocity = this.velocity + this.distance*this.k100;
                this.previousVelocity = this.velocity;

                double currentBarValue = this.error + this.velocity;
                //currentBarValue = Double.parseDouble(df.format(currentBarValue));


                if(i >= output.size()) {
                    output.add(currentBarValue);
                } else {
                    output.set(i, currentBarValue);
                }

                // calculating trend
                if(this.signal == SignalMode.KALMAN) {
                    // Kalman signal
                    if(this.velocity > 0) {
                        System.out.println("buySellSignal["+i+"]: " + 1);
                        if(i >= buySellSignal.size()) {
                            // buy
                            buySellSignal.add(1);
                        } else {
                            buySellSignal.set(i, 1);
                        }

                    } else {

                        System.out.println("buySellSignal["+i+"]: " + 0);
                        if(i >= buySellSignal.size()) {
                            // sell
                            buySellSignal.add(0);
                        } else {
                            buySellSignal.set(i, 0);
                        }
                    }

                } else {
                    // Trend signal
                    if(output.get(i -1 ) > output.get(i)) {
                        if(i >= buySellSignal.size()) {
                            buySellSignal.add(1);
                        } else {
                            buySellSignal.set(i, 1);
                        }

                    } else {
                        if(i >= buySellSignal.size()) {
                            buySellSignal.add(0);
                        } else {
                            buySellSignal.set(i, 0);
                        }
                    }
                }
            }
        }
    }
}

