package com.ist.DepChain.links;

import java.net.SocketException;

public class StubbornLink {

    private FairLossLink fairLossLink;

    public StubbornLink(int port) throws SocketException {
        fairLossLink = new FairLossLink(port);
    }
    
}
