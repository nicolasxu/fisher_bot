package com.fisher;

import sun.security.util.Length;

import java.util.ArrayList;

/**
 * Created by nick on 6/14/15.
 */
public class EmaFilter extends IFilter {
    public int m_period;
    public double m_alpha;
    public double m_pr;

    //m_Pr=2.0/(Length+1.0);

    public EmaFilter(int period) {
        this.m_period = period;
        this.m_alpha = 2.0;
        this.m_pr = m_alpha/(m_period + 1.0);

    }
    public void filter(ArrayList<Double> input, ArrayList<Double> output) {
        int inputSize = input.size();

        if (inputSize < 0) {
            // do nothing if input is empty
            return;
        }

        for(int index = Math.max(0, output.size() - 1) ; index < inputSize; index++) {

            if(index == 0) {
                if(index >= output.size()) {
                    output.add(input.get(index));
                } else {
                    output.set(index, input.get(index));
                }
            }

            if(index > 0) {
                double result = output.get(index)*m_pr + (1 - m_pr)*output.get(index -1);

                if(index >= output.size()) {
                    output.add(result);
                } else {
                    output.set(index, result);
                }
            }
        }
    }
}
