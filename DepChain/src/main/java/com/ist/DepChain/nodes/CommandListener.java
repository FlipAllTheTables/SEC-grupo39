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
        String[] parts = command.split("\\s+", 2);
        String cmd = parts[0];
        String arg = (parts.length > 1) ? parts[1] : "";

        switch (cmd.toUpperCase()) {
            case "TEST":
                System.out.println("Sending test message: " + arg);
                String message = "TEST|" + nodestate.myId + "|" + nodestate.seqNum + "|" + arg;
                apLink.send(message, BASE_PORT + 1);
                break;

            case "APPEND":
                if(nodestate.myId != 0){
                    String innit = "INNIT|" + nodestate.myId + "|" + nodestate.seqNum + "|" + arg;
                    apLink.send(innit, BASE_PORT);
                }
            case "STATUS":
                System.out.println("Node State: " + nodestate);
                break;

            default:
                System.out.println("Unknown command: " + cmd);
                break;
        }
    }
}
