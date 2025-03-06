package com.ist.DepChain.nodes;

import java.net.DatagramPacket;
import com.ist.DepChain.links.AuthenticatedPerfectLink;

/**
     * Listener class that implements runnable to continuously wait for incoming messages
     */
    public class Listener implements Runnable {

        private AuthenticatedPerfectLink apLink;
        public NodeState nodestate;

        public Listener(AuthenticatedPerfectLink apLink, NodeState nodeState) {
            this.apLink = apLink;
            this.nodestate = nodeState;
        }

        public void run() {
            System.out.println("Started listening for messages");
            while (true) {
                try {
                    DatagramPacket dp = apLink.deliver();
                    messageHandler(dp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void messageHandler(DatagramPacket dp) {
            String message = new String(dp.getData(), 0, dp.getLength());
            
            String command = message.split("|",2)[0];
            String senderId = message.split("|",4)[1];
            switch(command) {
                case "ACK": 
                    System.out.println("Received ack from " + senderId);
                    String seqNum = message.split("|",2)[2];
                    if(nodestate.acks.contains(Integer.valueOf(seqNum))){
                        nodestate.acks.remove(Integer.valueOf(seqNum));
                    }
                    break;
                default:
                    System.out.println("Received message from " + senderId + ": " + message);
                    break;
            }
        }

    }
