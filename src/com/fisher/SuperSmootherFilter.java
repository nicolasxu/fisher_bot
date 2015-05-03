package com.fisher;

import com.ib.controller.Bar;

import java.util.ArrayList;

/**
 * Created by nick on 5/3/15.
 */
public class SuperSmootherFilter implements IFilter{

    public int ssPeriod;
    public int minBarCount = 2;
    public void filter(ArrayList<Bar> input, ArrayList<Double> output) {

        double coeA = Math.pow(Math.E, -1.414*3.14159/this.ssPeriod);
        double coeB = 2 * coeA * Math.cos(1.414 * 180 / this.ssPeriod);
        double coeC2 = coeB;
        double coeC3 = - coeA * coeA;
        double coeC1 = 1 - coeC2 - coeC3;

        for(int i = Math.max(0, output.size() - 1); i < input.size(); i ++) {

            if(i < minBarCount) {
                Bar thisBar = input.get(i);
                output.add((thisBar.m_high + thisBar.m_low) / 2);
            }

            if(i >= minBarCount) {
                double inputI    = (input.get(i).m_low + input.get(i).m_high) / 2;
                double inputIm1  = (input.get(i-1).m_low + input.get(i-1).m_high) / 2;
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
