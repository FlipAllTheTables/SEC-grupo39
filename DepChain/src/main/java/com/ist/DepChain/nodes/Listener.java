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
                    if (dp == null){
                        System.out.println("Signature didnt match content");
                    }
                    else{
                        new Thread(() -> {
                            try {
                                messageHandler(dp);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void messageHandler(DatagramPacket dp) throws Exception {
            String message = new String(dp.getData(), 0, dp.getLength());
            
            String command = message.split("\\|",6)[0];
            String senderId = message.split("\\|",6)[1];
            String seqNum = message.split("\\|",6)[2];

            switch(command) {
                case "ACK": 
                    //System.out.println("Received ack from " + senderId +  " with sequence number: " + seqNum);
                    if(nodestate.acks.contains(Integer.valueOf(seqNum))){
                        nodestate.acks.remove(Integer.valueOf(seqNum));
                    }
                    break;

                case "TEST":
                    try {
                        //System.out.println("Sending Acknoledge to message: " + seqNum + " from sender: " + senderId);
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                case "INNIT":
                    System.out.println("Received message from " + senderId + ": " + message);
                    try {
                        //System.out.println("Sending Acknoledge to message: " + seqNum + " from sender: " + senderId);
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    String value = message.split("\\|",6)[3];

                    if (nodestate.myId == 0){ //If im the leader
                        synchronized(nodestate.valuesToAppend){
                            if (!nodestate.valuesToAppend.contains(value)){
                                System.out.println("Value: " + value + " added to the list of values to append");
                                nodestate.valuesToAppend.add(value);
                            }
                            else{
                                System.out.println("Value: " + value + " already in the list of values to append");
                                return;
                            }
                            int consensusIndex = ++nodestate.consensusIndex;
                            nodestate.val.add(consensusIndex, value);
                            nodestate.valts.add(consensusIndex, 0);
                            bizantineConsensus.read(consensusIndex);
                        }
                    }
                    else{
                        String init = "INNIT|" + nodestate.myId + "|" + nodestate.seqNum++ + "|" + value;
                        new Thread(() -> {
                            try {
                                apLink.send(init, BASE_PORT);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }                    
                    break;

                case "READ":
                    System.out.println("Received message from " + senderId + ": " + message);
                    if (Integer.valueOf(senderId) != 0) {
                        System.out.println("READ command comes from non-leader node, discarded");
                        return;
                    }
                    try {
                        //System.out.println("Sending Acknoledge to message: " + seqNum + " from sender: " + senderId);
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    bizantineConsensus.state(message);
                    break;

                case "STATE":
                    System.out.println("Received message from " + senderId + ": " + message);
                    int consensusRun = Integer.valueOf(message.split("\\|",6)[3]);
                    try {
                        //System.out.println("Sending Acknoledge to message: " + seqNum + " from sender: " + senderId);
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    bizantineConsensus.analyseState(message, consensusRun);
                    break;

                case "COLLECTED":
                    System.out.println("Received message from " + senderId + ": " + message);
                    try {
                        //System.out.println("Sending Acknoledge to message: " + seqNum + " from sender: " + senderId);
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    int consensusIndex = Integer.valueOf(message.split("\\|",6)[3]);
                    bizantineConsensus.readCollected(message, consensusIndex); 
                    break;

                case "WRITE":
                    System.out.println("Received message from " + senderId + ": " + message);
                    try {
                        //System.out.println("Sending Acknoledge to message: " + seqNum + " from sender: " + senderId);
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    bizantineConsensus.countWrites(message);
                    break;

                case "ACCEPT":
                    System.out.println("Received message from " + senderId + ": " + message);
                    try {
                        //System.out.println("Sending Acknoledge to message: " + seqNum + " from sender: " + senderId);
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    bizantineConsensus.countAccepts(message);
                    break;
                
                case "READALL":
                    //System.out.println("Received message from " + senderId + ": " + message);
                    try {
                        //System.out.println("Sending Acknoledge to message: " + seqNum + " from sender: " + senderId);
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String response = "INFO|" + nodestate.myId + "|" + nodestate.seqNum++ + "|" + nodestate.blockChain.toString();
                    new Thread(() -> {
                        try {
                            apLink.send(response, BASE_PORT+Integer.parseInt(senderId));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                    break;

                case "INFO":
                    try {
                        //System.out.println("Sending Acknoledge to message: " + seqNum + " from sender: " + senderId);
                        apLink.sendAck(Integer.valueOf(seqNum), Integer.valueOf(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String content = message.split("\\|",6)[3];
                    System.out.println("Received message from " + senderId + ": " + content);
                    break;

                default:
                    System.out.println("No command found");
                    break;
            }
        }

    }
