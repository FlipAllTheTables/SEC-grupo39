package com.ist.DepChain.nodes;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import javax.xml.crypto.Data;

public class NodeStarter {
    public static void main( String[] args ) throws Exception{
        int base_port = Integer.valueOf(args[0]);
        int my_id = Integer.valueOf(args[1]);

        DatagramSocket socket = new DatagramSocket(base_port + my_id);
        InetAddress addr = InetAddress.getByName("localhost");
        
        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
        //Start the server to listen for incoming messages from clients
        socket.receive(packet);
    }
}
