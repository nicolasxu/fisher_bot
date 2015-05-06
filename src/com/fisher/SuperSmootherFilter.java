package com.fisher;

import com.ib.controller.Bar;

import java.util.ArrayList;

/**
 * Created by nick on 5/3/15.
 */
public class SuperSmootherFilter extends IFilter{

    public int ssPeriod; // smooth period
    public int minBarCount = 2; // if input bar count is less than it, just copy
                                // the input, no calculation.

    public void filter(ArrayList<Double> input, ArrayList<Double> output) {
        // input count should always equal output count
        double coeA = Math.pow(Math.E, -1.414*3.14159/this.ssPeriod);
        double coeB = 2 * coeA * Math.cos(1.414 * 180 / this.ssPeriod);
        double coeC2 = coeB;
        double coeC3 = - coeA * coeA;
        double coeC1 = 1 - coeC2 - coeC3;

        for(int i = Math.max(0, output.size() - 1); i < input.size(); i ++) {

            if(i < minBarCount) {
                Double inputNumber = input.get(i);
                output.add(inputNumber);
            }

            if(i >= minBarCount) {
                double inputI    = input.get(i);
                double inputIm1  = input.get(i-1);
                double outputIm1 = output.get(i-1);
                double outputIm2 = output.get(i-2);
                double result = coeC1 * (inputI + inputIm1) / 2 + coeC2 * outputIm1 + coeC3 * outputIm2;
                if(i >= output.size()) {
                    output.add(result);
                } else {
                    output.set(i, result);
                }
            }
        }
    }
    public SuperSmootherFilter() {
        this.ssPeriod = 1;

    }
    public void setPeriod(int p) {
        this.ssPeriod = p;
    }
}
