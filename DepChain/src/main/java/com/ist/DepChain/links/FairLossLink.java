package com.ist.DepChain.links;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import com.ist.DepChain.nodes.NodeState;

public class FairLossLink {
    
    private DatagramSocket socket;
    private byte[] buf = new byte[1024];
    private NodeState nodestate;

    public FairLossLink(DatagramSocket socket, NodeState nodeState) throws SocketException {
        this.socket = socket;
        this.nodestate = nodeState;
    }

    public void send(String m, int port) throws Exception {
        buf = m.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName("localhost"), port);
        socket.send(packet);
    }

    public DatagramPacket deliver() throws Exception {
        System.out.println("FLL: Delivering message");
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        return packet;
    }
}
