package com.fisher;

import com.ib.controller.Bar;

import java.util.ArrayList;

/**
 * Created by nick on 5/3/15.
 */
public interface IFilter {
    public void filter(ArrayList<Bar> input, ArrayList<Double> output);
    // if you want to recalculate the whole output, you have to set output to size 0 by output.clear()
    // otherwise, the filter will always recalcualte the last one, or those haven't been
    // calculated yet.

}
