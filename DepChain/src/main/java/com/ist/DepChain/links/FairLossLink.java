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
        byte[] buf = new byte[1024];
        buf = m.getBytes();
        System.out.println("FLL: Sending message" + m + " to port " + port);
        DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName("localhost"), port);
        socket.send(packet);
    }

    public DatagramPacket deliver() throws Exception {
        System.out.println("FLL: Delivering message");
        byte[] buf = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        System.out.println("FLL: Received message" + new String(packet.getData(), 0, packet.getLength()));
        return packet;
    }
}
