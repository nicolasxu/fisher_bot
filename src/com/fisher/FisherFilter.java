package com.fisher;

import java.util.ArrayList;

/**
 * Created by nick on 5/3/15.
 */
public class FisherFilter extends IFilter{

    private int period; // fisher period to go back and find the max and min number
    private ArrayList<Double> midValues; // mid value buffer needed for calculation
    public void filter(ArrayList<Double> input, ArrayList<Double> outputFisher,
                       ArrayList<Double> outputTrigger) {


        for(int i = Math.max(0, outputFisher.size() - 1); i < input.size(); i ++) {

            if ( i < this.period) {
                // less than required bar count,  e.g: period == 10, i will be 0 ~ 9
                // just copy input to output

                double valueZero = 0;

                // writer fisher
                if(outputFisher.size() >= i + 1) {
                    outputFisher.set(i, valueZero);
                } else {
                    outputFisher.add(valueZero);
                }

                // write trigger
                if(outputTrigger.size() >= i +1) {
                    outputTrigger.set(i, valueZero);
                } else {
                    outputTrigger.add(valueZero);
                }

                // write mid value
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

                // calculation mid value calculation
                double valueI = 0.5 * 2.0 * ((price - minL)/(maxH - minL) - 0.5) + 0.5 * midValues.get(i - 1);

                if(valueI > 0.9999) valueI = 0.9999;
                if(valueI < -0.9999) valueI = -0.9999;

                // write calculation mid value buffer
                if(midValues.size() >= i + 1) {
                    midValues.set(i, valueI);
                } else {
                    midValues.add(valueI);
                }

                // current fisher value i calculation
                double fisherI = 0.25 * Math.log((1 + midValues.get(i)) / (1 - midValues.get(i))) + 0.5 * outputFisher.get(i - 1);

                // write fisher
                if(outputFisher.size() >= i +1) {
                    outputFisher.set(i, fisherI );
                } else {
                    outputFisher.add(fisherI);
                }
                //System.out.println("Fisher result: " + fisherI);

                // write trigger
                double triggerI = outputFisher.get(i - 1);
                if(outputTrigger.size() >= i +1) {
                    outputTrigger.set(i, triggerI);
                } else {
                    outputTrigger.add(triggerI);
                }
            }
        }
        double latestInput = input.get(input.size() - 1);
        //System.out.println("latest fisher input value: " + latestInput);
        //double latestFisher  = outputFisher.get(outputFisher.size() - 1 );
        //double latestTrigger = outputTrigger.get(outputTrigger.size()  - 1);
        //System.out.println("Fisher latest: " + latestFisher + " Trigger latest: " + latestTrigger);


    }
    public FisherFilter() {
        this.period = 15;
        this.midValues = new ArrayList<Double>();
    }

    public void setPeriod (int p) {
        this.period = p;
    }
    public int getPeriod () {
        return this.period;
    }
}
