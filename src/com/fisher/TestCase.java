package com.fisher;

import com.ib.client.*;
import com.ib.controller.Bar;

import java.util.ArrayList;

/**
 * Created by nick on 5/16/15.
 */
public class TestCase {

    IDataTank dt;
    TestHandler handler;
    TestClientSocket request;
    FisherBot bot;
    ILogger logger;




    public TestCase() {
        this.logger = new ConsoleLogger();
        this.dt = new DataTank();
        this.handler = new TestHandler();
        this.request = new TestClientSocket(handler);
        //this.bot = new FisherBot();


    }

    public void fetchData() {

    }

    public void loadData() {

    }

    public void runTest () {

    }


}
