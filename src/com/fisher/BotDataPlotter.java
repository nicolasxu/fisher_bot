package com.fisher;

import com.ib.controller.Bar;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.ohlc.OHLCItem;
import org.jfree.data.time.ohlc.OHLCSeries;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;
import org.jfree.data.xy.DefaultHighLowDataset;
import org.jfree.data.xy.DefaultOHLCDataset;
import org.jfree.data.xy.OHLCDataset;


import java.awt.*;
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
    public OHLCSeries m_ohlcSeries;
    public OHLCSeriesCollection m_ohlcDataset;
    public TimeSeries  m_fisherSeries;
    public TimeSeries  m_triggerSeries;
    public TimeSeriesCollection m_tsCollection;


    public BotDataPlotter() {

        // Init reference data to empty, so draw nothing
        this.m_bars = new ArrayList<Bar>();
        this.m_fishers = new ArrayList<Double>();
        this.m_triggers = new ArrayList<Double>();

        // Init series
        this.m_ohlcSeries = new OHLCSeries("bars");
        this.m_fisherSeries = new TimeSeries("fisher");
        this.m_triggerSeries = new TimeSeries("trigger");

        // mount series to dataset
        this.m_ohlcDataset = new OHLCSeriesCollection();
        this.m_ohlcDataset.addSeries(this.m_ohlcSeries);
        this.m_tsCollection = new TimeSeriesCollection();
        this.m_tsCollection.addSeries(this.m_fisherSeries);
        this.m_tsCollection.addSeries(this.m_triggerSeries);

        // update plot data based on empoty reference
        this.updatePlotData();

    }

    public void updatePlotData () {
        // always re-draw the last one
        // 1. convert data from reference to drawing data

        // remove the last m_ohlcSeries data
        if(m_ohlcSeries.getItemCount() > 0) {
            m_ohlcSeries.remove(m_ohlcSeries.getItemCount() - 1);

        }

        // remove the last m_fisherSeries data
        if(m_fisherSeries.getItemCount() > 0) {
            int index = m_fisherSeries.getItemCount() - 1;
            m_fisherSeries.delete(index, index); // last one
        }

        // even no need to redraw trigger
        if(m_triggerSeries.getItemCount() > 0) {
            int index = m_triggerSeries.getItemCount() - 1;
            m_triggerSeries.delete(index, index); // last one
        }

        for(int i = Math.max(m_ohlcSeries.getItemCount() - 1, 0)  ; i < m_bars.size(); i++) {

            Bar currentBar = m_bars.get(i);
            Minute theMinute = new Minute(new Date(currentBar.m_time*1000));

            // add
            OHLCItem item = new OHLCItem(theMinute,
                    currentBar.m_open,
                    currentBar.m_high,
                    currentBar.m_low,
                    currentBar.m_close );

            this.m_ohlcSeries.add(item);
            m_fisherSeries.add(theMinute, m_fishers.get(i));
            m_triggerSeries.add(theMinute, m_triggers.get(i));

        }

        // 2. send message to re-draw the plot
        //    m_ohlcSeries.addChangeListener();
    }

    public JFreeChart createChart() {

        // 0. create common domain axis for combined chart
        String timeAxisLabel = "5M";
        DateAxis timeAxis = new DateAxis(timeAxisLabel);

        // 1. create OHLC plot
        String valueAxisLabel = "Price"; // range axis title only for OHLC
        NumberAxis valueAxis = new NumberAxis(valueAxisLabel);
        XYPlot candlePlot = new XYPlot(this.m_ohlcDataset, null, valueAxis, (XYItemRenderer)null);
        candlePlot.setRenderer(new CandlestickRenderer());

        // 2. create fisher and trigger plot
        String fisherRangeLabel = "";
        NumberAxis fisherValueAxis = new NumberAxis(fisherRangeLabel);
        valueAxis.setAutoRangeIncludesZero(false);
        XYPlot fisherPlot = new XYPlot(this.m_tsCollection, null, fisherValueAxis, (XYItemRenderer)null);
        StandardXYToolTipGenerator toolTipGenerator = null;
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        fisherPlot.setRenderer(renderer);

        // 3. create the combined plot
        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(timeAxis); // passing in the common Axis
        combinedPlot.add(candlePlot, 1);
        combinedPlot.add(fisherPlot, 1);
        combinedPlot.setGap(8.0D);
        combinedPlot.setDomainGridlinePaint(Color.white);
        combinedPlot.setDomainGridlinesVisible(true);
        combinedPlot.setDomainPannable(true);

        // 4. create the chart
        JFreeChart chart = new JFreeChart("United States Public Debt", JFreeChart.DEFAULT_TITLE_FONT, combinedPlot, false);
        ChartUtilities.applyCurrentTheme(chart);

        // 5. return the chart
        return chart;
    }

    public void purgeDrawingData () {
        // clear all the series that've been converted for plotting.
        // Have to call updatePlotData to generate drawing data from reference again.
        this.m_ohlcSeries.clear();
        this.m_fisherSeries.clear();
        this.m_triggerSeries.clear();

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
