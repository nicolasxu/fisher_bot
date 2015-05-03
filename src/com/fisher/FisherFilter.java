package com.fisher;

import java.util.ArrayList;

/**
 * Created by nick on 5/3/15.
 */
public class FisherFilter extends IFilter{

    private int period;
    private ArrayList<Double> midValues;
    public void filter(ArrayList<Double> input, ArrayList<Double> outputFisher,
                       ArrayList<Double> outputTrigger) {

        for(int i = 0; i < input.size(); i ++) {

            if ( i < this.period) {
                // period == 10, i will be 0 ~ 9
                // just copy input to output

                double valueZero = 0;
                if(outputFisher.size() >= i + 1) {
                    outputFisher.set(i, valueZero);
                } else {
                    outputFisher.add(valueZero);
                }

                if(outputTrigger.size() >= i +1) {
                    outputTrigger.set(i, valueZero);
                } else {
                    outputTrigger.add(valueZero);
                }

                if(midValues.size() >= i + 1) {
                    midValues.set(i, valueZero);
                } else {
                    midValues.add(valueZero);
                }


            } else {
                // more bar then the required this.period
                // do calculation
                // starts from [period]
                double price = input.get(i);
                double maxH = price;
                double minL = price;

                // find the max and min in past period - 1 bars
                for(int j = 0; j< this.period; j++) {
                    double nPrice = input.get(i - j);
                    if(nPrice > maxH) maxH = nPrice;
                    if(nPrice < minL) minL = nPrice;
                }
                //Value1[i]=      0.5 * 2.0 * ((price - MinL)/(MaxH - MinL) - 0.5) + 0.5 * Value1[i+1];
                double valueI = 0.5 * 0.2 * ((price - minL)/(maxH - minL) - 0.5) + 0.5 * midValues.get(i - 1);

                if(valueI > 0.9999) valueI = 0.9999;
                if(valueI < -0.9999) valueI = -0.9999;

                if(midValues.size() >= i + 1) {
                    midValues.set(i, valueI);
                } else {
                    midValues.add(valueI);
                }
                //Fisher[i]=0.25 * MathLog((1+Value1[i])/(1-Value1[i]))+0.5*Fisher[i+1];

                double fisherI = 0.25 * Math.log((1 + midValues.get(i)) / (1 - midValues.get(i))) + 0.5 * outputFisher.get(i - 1);

                if(outputFisher.size() >= i +1) {
                    outputFisher.set(i, fisherI );
                } else {
                    outputFisher.add(fisherI);
                }

                double triggerI = outputFisher.get(i - 1);
                if(outputTrigger.size() >= i +1) {
                    outputTrigger.set(i, triggerI);
                } else {
                    outputTrigger.add(triggerI);
                }


            }


        }
    }
    public FisherFilter() {
        this.period = 10;
        this.midValues = new ArrayList<Double>();
    }

    public void setPeriod (int p) {
        this.period = p;
    }
    public int getPeriod () {
        return this.period;
    }
}
