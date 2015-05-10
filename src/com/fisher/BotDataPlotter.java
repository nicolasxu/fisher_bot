package com.fisher;

import com.ib.controller.Bar;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.ohlc.OHLCSeries;
import org.jfree.data.xy.DefaultHighLowDataset;
import org.jfree.data.xy.OHLCDataset;


import java.util.ArrayList;
import java.util.Date;

/**
 * Created by nick on 5/10/15.
 */
public class BotDataPlotter {
    /* this class get data from
          1. received bar from DataHandler
          2. Fisher and Trigger data from FisherBot
       and then plot data on ChartPanel which is attached to a JFrame.
       Data conversion is done in side of this class
    */

    // outside data reference
    public ArrayList<Bar> m_bars;
    public ArrayList<Double> m_fishers;
    public ArrayList<Double> m_triggers;



    // converted data set for plotting
    public OHLCDataset m_ohlcSeries;
    public TimeSeries  m_fisherSeries;
    public TimeSeries  m_triggerSeries;


    public BotDataPlotter() {

    }

    public void updatePlotData () {
        // 1. convert data from reference to drawing data
        Date[]   dateArr   = new Date[m_bars.size()];
        double[] openArr   = new double[m_bars.size()];
        double[] highArr   = new double[m_bars.size()];
        double[] lowArr    = new double[m_bars.size()];
        double[] closeArr  = new double[m_bars.size()];
        double[] volumeArr = new double[m_bars.size()];

        for(int i = 0 ; i < m_bars.size(); i++) {
            Bar currentBar = m_bars.get(i);
            dateArr[i]   = new Date(currentBar.m_time * 1000);
            openArr[i]   = currentBar.m_open;
            highArr[i]   = currentBar.m_high;
            lowArr[i]    = currentBar.m_low;
            closeArr[i]  = currentBar.m_close;
            volumeArr[i] =  0;
        }

        // public DefaultHighLowDataset(Comparable seriesKey,
        //          Date[] date,
        //          double[] high,
        //          double[] low,
        //          double[] open,
        //          double[] close,
        //          double[] volume);

        this.m_ohlcSeries = new DefaultHighLowDataset("Bar data",
                dateArr,
                highArr,
                lowArr,
                openArr,
                closeArr,
                volumeArr);

        // 2. send message to re-draw the plot
    }

    public JFreeChart createChart() {

        // 1. create OHLC plot

        // 2. create fisher and trigger plot

        // 3. create the combined plot

        // 4. create the chart

        // 5. return the chart
    }

    public void purgeDrawingData () {
        // clear all the series that've been converted for plotting.
        // Have to call updatePlotData to generate drawing data from reference again.
        
    }

    public void setBarSource(ArrayList<Bar> bars) {
        this.m_bars = bars;
    }
    public void setFisherSource (ArrayList<Double> fishers) {
        this.m_fishers = fishers;
    }
    public void setTriggerSource (ArrayList<Double> trigger) {
        this.m_triggers = trigger;

    }




}
