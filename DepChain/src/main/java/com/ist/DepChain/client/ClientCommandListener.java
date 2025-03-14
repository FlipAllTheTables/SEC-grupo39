package com.ist.DepChain.client;

import java.util.Scanner;

import com.ist.DepChain.links.AuthenticatedPerfectLink;
import com.ist.DepChain.nodes.NodeState;

public class ClientCommandListener implements Runnable {

    private NodeState nodestate;
    private AuthenticatedPerfectLink apLink;
    private static final int BASE_PORT = 5000;

    public ClientCommandListener (NodeState nodeState, AuthenticatedPerfectLink apLink) {
        this.nodestate = nodeState;
        this.apLink = apLink;
    }

    @Override
    public void run() {
        System.out.println("Started listening for commands (type 'exit' to quit)");

        Scanner scanner = new Scanner(System.in);

        while (true) {
            try {
                System.out.print("> "); // Command prompt
                String command = scanner.nextLine().trim();

                if (command.equalsIgnoreCase("exit")) {
                    System.out.println("Shutting down command listener...");
                    break;
                }

                commandHandler(command);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        scanner.close();
    }

    private void commandHandler(String command) throws Exception{
        String[] parts = command.split("\\s+", 2);
        String cmd = parts[0];
        String arg = (parts.length > 1) ? parts[1] : "";
        int i = 0;

        switch (cmd.toUpperCase()) {
            case "APPEND":
                for (i = 1; i < nodestate.numNodes-nodestate.bizantineProcesses; i++) {
                    String innit = "INNIT|" + nodestate.myId + "|" + nodestate.seqNum++ + "|" + arg;
                    int sendPort = BASE_PORT + i;
                    new Thread(() -> {
                        try {
                            apLink.send(innit, sendPort);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                } 
                break;
            
            case "READALL":
                for (i = 1; i < nodestate.numNodes-nodestate.bizantineProcesses + 1; i++) {
                    String read = "READALL|" + nodestate.myId + "|" + nodestate.seqNum++ + "|";
                    int sendPort = BASE_PORT + i;
                    new Thread(() -> {
                        try {
                            apLink.send(read, sendPort);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
                break;
            default:
                System.out.println("Unknown command: " + cmd);
                break;
        }
    }
}
