package com.ist.DepChain.nodes;

import java.security.spec.ECFieldF2m;
import java.util.Scanner;

import com.ist.DepChain.links.AuthenticatedPerfectLink;

public class CommandListener implements Runnable {

    private NodeState nodestate;
    private AuthenticatedPerfectLink apLink;
    private static final int BASE_PORT = 5000;

    public CommandListener(NodeState nodeState, AuthenticatedPerfectLink apLink) {
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
        String[] parts = command.split("\\s+", 3);
        String cmd = parts[0];

        switch (cmd.toUpperCase()) {
            case "TEST":
                if (parts.length == 3) {
                    String arg = parts[1];
                    String destId = parts[2];
                    if (Integer.valueOf(destId) != nodestate.myId) {
                        System.out.println("Sending test message " + arg + " to node " + destId);
                        String message = "TEST|" + nodestate.myId + "|" + nodestate.seqNum++ + "|" + arg;
                        apLink.send(message, BASE_PORT + Integer.valueOf(destId));
                    } else {
                        System.out.println("Can't send message to own ID");
                    }
                } else {
                    System.out.println("Incorrect number of arguments for TEST command");
                }
                break;

            case "APPEND":
                if(nodestate.myId != 0){
                    if (parts.length == 2) {
                        String arg = parts[1];
                        String innit = "INNIT|" + nodestate.myId + "|" + nodestate.seqNum++ + "|" + arg;
                        apLink.send(innit, BASE_PORT);
                    } else {
                        System.out.println("Incorrect number of arguments for APPEND command");
                    }
                } else {
                    System.out.println("Leader can't send APPEND message to himself");
                }
                break;
                
            default:
                System.out.println("Unknown command: " + cmd);
                break;
        }
    }
}
