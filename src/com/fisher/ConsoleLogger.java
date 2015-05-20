package com.fisher;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by nick on 5/16/15.
 */
public class ConsoleLogger implements ILogger {

    @Override
    public void log(String text) {

        Calendar c = new GregorianCalendar();

        System.out.println(String.format("%tT", c.getInstance()) + " - " + text);

    }
}
