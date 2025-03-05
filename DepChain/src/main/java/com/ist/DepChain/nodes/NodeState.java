package com.ist.DepChain.nodes;

import java.util.ArrayList;
import java.util.List;

public class NodeState {
    List<Integer> acks;

    public NodeState() {
        acks = new ArrayList<>();
    }
}
