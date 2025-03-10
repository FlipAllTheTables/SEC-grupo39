package com.ist.DepChain.nodes;

import java.net.DatagramPacket;
import com.ist.DepChain.links.AuthenticatedPerfectLink;

/**
     * Listener class that implements runnable to continuously wait for incoming messages
     */
    public class Listener implements Runnable {

        private AuthenticatedPerfectLink apLink;
        public NodeState nodestate;
        private static final int BASE_PORT = 5000;
        private BizantineConsensus bizantineConsensus;

        public Listener(AuthenticatedPerfectLink apLink, NodeState nodeState, BizantineConsensus bizantineConsensus) {
            this.apLink = apLink;
            this.nodestate = nodeState;
            this.bizantineConsensus = bizantineConsensus;
        }

        public void run() {
            System.out.println("Started listening for messages");
            while (true) {
                try {
                    DatagramPacket dp = apLink.deliver();
                    messageHandler(dp);
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void messageHandler(DatagramPacket dp) {
            String message = new String(dp.getData(), 0, dp.getLength());
            
            String command = message.split("\\|",6)[0];
            String senderId = message.split("\\|",6)[1];
            String seqNum = message.split("\\|",6)[2];
            String content = message.split("\\|",6)[3];
            String signature = message.split("\\|",6)[4];

            switch(command) {
                case "ACK": 
                    System.out.println("Received ack from " + senderId);
                    if(nodestate.acks.contains(Integer.valueOf(seqNum))){
                        nodestate.acks.remove(Integer.valueOf(seqNum));
                    }
                    break;

                case "TEST":
                    try {
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                case "INNIT":
                    if(nodestate.myId == 0){
                        System.out.println("Received message from " + senderId + ": " + message);
                        try {
                            apLink.sendAck(Integer.valueOf(seqNum), BASE_PORT + Integer.valueOf(senderId));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        bizantineConsensus.read();
                    }
                    break;

                case "READ":
                    System.out.println("Received message from " + senderId + ": " + message);
                    try {
                        apLink.sendAck(Integer.valueOf(seqNum), BASE_PORT + Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    bizantineConsensus.state(Integer.valueOf(senderId));
                    break;

                case "STATE":
                    System.out.println("Received message from " + senderId + ": " + message);
                    int consensusRun = Integer.valueOf(message.split("\\|",6)[5]);
                    try {
                        apLink.sendAck(Integer.valueOf(seqNum), BASE_PORT + Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    bizantineConsensus.analyseState(message, consensusRun);
                    break;

                case "COLLECTED":
                    System.out.println("Received message from " + senderId + ": " + message);
                    try {
                        apLink.sendAck(Integer.valueOf(seqNum), BASE_PORT + Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                default:
                    System.out.println("Received message from " + senderId + ": " + message);
                    break;
            }
        }

    }
