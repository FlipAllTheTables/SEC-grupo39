package com.ist.DepChain.links;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import com.ist.DepChain.nodes.NodeState;

public class FairLossLink {
    
    private DatagramSocket socket;
    private NodeState nodestate;

    public FairLossLink(DatagramSocket socket, NodeState nodeState) throws SocketException {
        this.socket = socket;
        this.nodestate = nodeState;
    }

    public void send(String m, int port) throws Exception {
        byte[] buf = new byte[10000];
        buf = m.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName("localhost"), port);
        System.out.println("Sending message: " + m);
        System.out.println("");
        socket.send(packet);
    }

    public DatagramPacket deliver() throws Exception {
        byte[] buf = new byte[10000];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        return packet;
    }
}
