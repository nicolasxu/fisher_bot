package com.fisher;

import com.ib.controller.Bar;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.ohlc.OHLCItem;
import org.jfree.data.time.ohlc.OHLCSeries;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.text.SimpleDateFormat;
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
       DataTank conversion is done in side of this class
    */

    // outside data reference
    public ArrayList<Bar> m_bars;
    public ArrayList<Double> m_fishers;
    public ArrayList<Double> m_triggers;
    public ArrayList<Double> m_stds;
    public ArrayList<Double> m_kalmanInputTicks;
    public ArrayList<Double> m_kalmanOutput;
    public ArrayList<Integer> m_kalmanBuySellSignal;



    // converted data set for plotting
    public OHLCSeries m_ohlcSeries;
    public OHLCSeriesCollection m_ohlcDataset;
    public TimeSeries  m_fisherSeries;
    public TimeSeries  m_triggerSeries;
    public TimeSeries  m_stdSeries;
    public TimeSeriesCollection m_tsCollection;
    public TimeSeriesCollection m_stdCollection;

    XYSeries m_kalmanInputSeries = new XYSeries("Tick");
    XYSeries m_kalmanOutputSeries = new XYSeries("Kalman");

    public JFreeChart m_chart;
    boolean m_chartCustomized;

    public BotDataPlotter() {

        // Init reference data to empty, so draw nothing
        this.m_bars = new ArrayList<Bar>();
        this.m_fishers = new ArrayList<Double>();
        this.m_triggers = new ArrayList<Double>();

        // Init series
        this.m_ohlcSeries = new OHLCSeries("bars");
        this.m_fisherSeries = new TimeSeries("fisher");
        this.m_triggerSeries = new TimeSeries("trigger");
        this.m_stdSeries = new TimeSeries("STD");

        // mount series to dataset
        this.m_ohlcDataset = new OHLCSeriesCollection();
        this.m_ohlcDataset.addSeries(this.m_ohlcSeries);
        this.m_tsCollection = new TimeSeriesCollection();
        this.m_tsCollection.addSeries(this.m_fisherSeries);
        this.m_tsCollection.addSeries(this.m_triggerSeries);
        this.m_stdCollection = new TimeSeriesCollection();
        this.m_stdCollection.addSeries(this.m_stdSeries);

        this.m_kalmanInputTicks = null;
        this.m_kalmanOutput = null;
        this.m_kalmanBuySellSignal = null;
        this.m_chartCustomized = false;

        // update plot data based on empoty reference
        //this.updatePlotData();
        this.updateKalmanPlot();

    }

    public void updatePlotData () {
        //System.out.println("updatePlotData() called...");
        // always re-draw the last one
        // 1. convert data from reference to drawing data
        this.purgeDrawingData();



        for(int i = Math.max(m_ohlcSeries.getItemCount() , 0)  ; i < m_bars.size(); i++) {

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
            m_stdSeries.add(theMinute, m_stds.get(i));

        }
    }

    public void updateKalmanPlot() {
        // update the last data point
        if(this.m_kalmanInputTicks == null) {
            return;
        }
        // clear previous kalman data
        this.m_kalmanInputSeries.clear();
        this.m_kalmanOutputSeries.clear();

        for(int i = 0; i < this.m_kalmanInputTicks.size(); i++) {
            this.m_kalmanOutputSeries.add(i, this.m_kalmanOutput.get(i));
            this.m_kalmanInputSeries.add(i, this.m_kalmanInputTicks.get(i));
        }


    }

    private XYDataset createKalmanData() {

        if(this.m_kalmanInputTicks == null) {
            return null;
        }

        for(int i = 0; i < this.m_kalmanInputTicks.size(); i++) {
            m_kalmanInputSeries.add(i, this.m_kalmanInputTicks.get(i));
            m_kalmanOutputSeries.add(i, this.m_kalmanOutput.get(i));
        }

        XYSeriesCollection dataCollection = new XYSeriesCollection();
        dataCollection.addSeries(m_kalmanInputSeries);
        dataCollection.addSeries(m_kalmanOutputSeries);
        System.out.println("kalman data is not empty: this.m_kalmanOutput.size(): " + this.m_kalmanOutput.size());
        return dataCollection;
    }

    public void customizeChart() {
        // customize chart
        JFreeChart chart = this.m_chart;

        XYPlot plot = (XYPlot) chart.getPlot();

        // find out the max and min value for price series
        XYSeriesCollection collection = (XYSeriesCollection)plot.getDataset();
        XYSeries priceSeries = collection.getSeries(0);
        double maxY = priceSeries.getMaxY();
        double minY = priceSeries.getMinY();

        // set max and min for range axis (y axis)
        NumberAxis rangeAxis = (NumberAxis)plot.getRangeAxis();
        rangeAxis.setRange(minY, maxY);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer() {

            public Color getItemColor(int series, int item) {
                // modify code here to change color for different part of the line in one serie line
                if(series == 1) {

                    int isBuy = 0;
                    if(item < m_kalmanBuySellSignal.size()) {
                        isBuy = m_kalmanBuySellSignal.get(item);
                    }
                    System.out.println("item: " + item + " buySell: " + isBuy);
                    if(isBuy == 1) {

                        return Color.red;
                    }
                    if(isBuy == 0){

                        return Color.green;
                    }
                    if(isBuy == -1) {

                        return Color.yellow;
                    } else {
                        return Color.yellow;
                    }
                } else {
                    return Color.yellow;
                }



            }

            @Override
            protected void drawFirstPassShape(Graphics2D g2, int pass, int series, int item, Shape shape) {
                super.drawFirstPassShape(g2, pass, series, item, shape);

                //g2.setStroke(getItemStroke(series, item));
                Color c1 = getItemColor(series, item - 1);
                Color c2 = getItemColor(series, item);
                // color of the line is determined by the 1st point, c1
                GradientPaint linePaint = new GradientPaint(0, 0, c1, 0, 0, c2);
                g2.setPaint(linePaint);
                g2.draw(shape);
            }
        };

        // Customize point shape
        Rectangle rect = new Rectangle();
        rect.setRect(-1,0,2,2); // cordiantes and dimension
        renderer.setSeriesShape(1, rect);
        Ellipse2D.Double ellipse = new Ellipse2D.Double(-1,0,2,2);

        renderer.setSeriesShape(0, ellipse);

        // Finally, attach the renderer
        plot.setRenderer(renderer);

        this.m_chartCustomized = true;
    }

    public JFreeChart createKalmanChart() {
        // create the chart...
        XYDataset dataCollection = createKalmanData();
        Date today = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
        JFreeChart chart = ChartFactory.createXYLineChart(
                df.format(today), // chart title
                "Count", // x axis label
                "Price", // y axis label
                dataCollection, // data
                PlotOrientation.VERTICAL,
                true, // include legend
                true, // tooltips
                false // urls
        );

        if(this.m_chart == chart ) {
            System.out.println("two charts are equal " + chart);
        } else {
            System.out.println("two charts are different: " + this.m_chart + " vs " + chart);
        }

        this.m_chart = chart;





        return chart;
    }

    public JFreeChart createFisherCombineChart() {

        // 0. create common domain axis for combined chart
        String timeAxisLabel = "5M";
        DateAxis timeAxis = new DateAxis(timeAxisLabel);

        // 1. create OHLC plot
        String valueAxisLabel = "Price"; // range axis title only for OHLC
        NumberAxis valueAxis = new NumberAxis(valueAxisLabel);
        valueAxis.setAutoRangeIncludesZero(false);
        XYPlot candlePlot = new XYPlot(this.m_ohlcDataset, null, valueAxis, (XYItemRenderer)null);
        candlePlot.setRenderer(new CandlestickRenderer());

        // 2. create fisher and trigger plot
        String fisherRangeLabel = "Fisher Value";
        NumberAxis fisherValueAxis = new NumberAxis(fisherRangeLabel);
        XYPlot fisherPlot = new XYPlot(this.m_tsCollection, null, fisherValueAxis, (XYItemRenderer)null);
        StandardXYToolTipGenerator toolTipGenerator = null;
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        fisherPlot.setRenderer(renderer);

        // 3. create std plot
        String stdRangeLabel = "STD Value";
        NumberAxis stdValueAxis = new NumberAxis(stdRangeLabel);
        XYPlot stdPlot = new XYPlot(this.m_stdCollection, null, stdValueAxis, (XYItemRenderer)null);
        XYLineAndShapeRenderer stdRenderer = new XYLineAndShapeRenderer(true, false);
        stdPlot.setRenderer(stdRenderer);



        // 4. create the combined plot
        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(timeAxis); // passing in the common Axis
        combinedPlot.add(candlePlot, 1);
        combinedPlot.add(fisherPlot, 1);
        combinedPlot.add(stdPlot, 1);
        combinedPlot.setGap(8.0D);
        combinedPlot.setDomainGridlinePaint(Color.white);
        combinedPlot.setDomainGridlinesVisible(true);
        combinedPlot.setDomainPannable(true);

        // 5. create the chart
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
        this.m_stdSeries.clear();

    }

    // set source for plotting Fisher bot
    public void setBarSource(ArrayList<Bar> bars) {
        this.m_bars = bars;
    }
    public void setFisherSource (ArrayList<Double> fishers) {
        this.m_fishers = fishers;
    }
    public void setTriggerSource (ArrayList<Double> trigger) {
        this.m_triggers = trigger;
    }
    public void setStdSource(ArrayList<Double> stdSource) {
        this.m_stds = stdSource;
    }

    // set source for plotting kalman bot
    public void setKalmanTickSource(ArrayList<Double> ticks) {this.m_kalmanInputTicks = ticks; }
    public void setKalmanOutputSource(ArrayList<Double> output) {this.m_kalmanOutput = output; }
    public void setKalmanSignalSource(ArrayList<Integer> signal) {this.m_kalmanBuySellSignal = signal; }

}
