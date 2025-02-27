package com.ist.DepChain.links;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class FairLossLink {
    
    private DatagramSocket socket;
    private byte[] buf = new byte[256];

    public FairLossLink() {}

    public void send(String m, int port) throws Exception {
        buf = m.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName("localhost"), port);
        socket.send(packet);
    }

    public DatagramPacket deliver() throws Exception {
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        return packet;
    }
}
