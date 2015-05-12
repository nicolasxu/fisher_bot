package com.fisher;

import javax.swing.*;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by nick on 5/12/15.
 */
public class TextAreaLogger implements ILogger{

    JTextArea textArea;

    public TextAreaLogger(JTextArea ta) {
        this.textArea = ta;
    }
    @Override
    public void log(String text) {
        Calendar c = new GregorianCalendar();

        this.textArea.append(String.format("%tT", c.getInstance()) + " - " + text + "\n");

        // scroll to the end of logs
        this.textArea.setCaretPosition(this.textArea.getDocument().getLength());

    }
    public void logNoTime(String text) {
        this.textArea.append(text + "\n");
    }
}
