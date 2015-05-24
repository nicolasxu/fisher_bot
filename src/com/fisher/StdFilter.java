package com.fisher;

import java.util.ArrayList;

/**
 * Created by nick on 5/24/15.
 */
public class StdFilter extends IFilter {

    public SuperSmootherFilter m_smoother;
    public ArrayList<Double> m_smoothedValues;
    public int m_period;


    public StdFilter(int period) {
        m_period = period;
        m_smoother = new SuperSmootherFilter(period);
        m_smoothedValues = new ArrayList<Double>();
    }

    public double calculateStd(ArrayList<Double> input, int index) {

        double result = 0.0;
        double totalDiff = 0.0;

        for(int i = 0; i < this.m_period; i++) {
            totalDiff = totalDiff + Math.pow( input.get(index - i) - this.m_smoothedValues.get(index) , 2);
        }
        result = Math.sqrt(totalDiff/this.m_period);

        return result;
    }

    public void filter(ArrayList<Double> input, ArrayList<Double> output) {

        m_smoother.filter(input, this.m_smoothedValues);

        // calculate stand deviation

        for(int i = Math.max(0, output.size() - 1); i < input.size(); i++) {

            if( i < m_period) {

                output.add(0.0);
            } else {

                // calcualte STD
                double result = calculateStd(input, i);

                if(i>=output.size()) {
                    output.add(result);
                } else {
                    output.set(i, result);
                }
            }
        }
    }
}
