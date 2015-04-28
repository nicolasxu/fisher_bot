package com.fisher;

import com.ib.client.AnyWrapper;
import com.ib.client.EClientSocket;

/**
 * Created by nick on 4/27/15.
 */
public class Request extends EClientSocket {

    public Request(AnyWrapper anyWrapper) {

        // parent class requres a AnyWrapper interface as param for constructor
        super(anyWrapper);
    }


    // may add some other method to send groups orders with stop loss and take profit order



}
