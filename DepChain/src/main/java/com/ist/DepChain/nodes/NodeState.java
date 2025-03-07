package com.ist.DepChain.nodes;

import java.util.ArrayList;
import java.util.List;

public class NodeState {
    public List<Integer> acks;
    public int seqNum;
    public int myId;
    public int numNodes;

    public NodeState(int myId, int numNodes) {
        acks = new ArrayList<>();
        seqNum = 0;
        this.myId = myId;
        this.numNodes = numNodes;
    }
}
