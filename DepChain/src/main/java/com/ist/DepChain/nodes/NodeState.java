package com.ist.DepChain.nodes;

import java.util.ArrayList;
import java.util.List;

public class NodeState {
    public List<Integer> acks;
    public int seqNum;

    public NodeState() {
        acks = new ArrayList<>();
        seqNum = 0;
    }
}
