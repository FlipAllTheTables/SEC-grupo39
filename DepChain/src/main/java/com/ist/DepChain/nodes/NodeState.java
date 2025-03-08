package com.ist.DepChain.nodes;

import java.util.ArrayList;
import java.util.List;
import com.ist.DepChain.util.Pair;

public class NodeState {
    public List<Integer> acks;
    public int seqNum;
    public int myId;
    public int numNodes;
    public List<Integer> valts;
    public List<String> val;
    public List<Pair> consensusPairs;

    public NodeState(int myId, int numNodes) {
        acks = new ArrayList<>();
        seqNum = 0;
        this.myId = myId;
        this.numNodes = numNodes;
        valts = new ArrayList<>();
        val = new ArrayList<>();
        consensusPairs = new ArrayList<>();
    }

    public void addConsensusPair(int value1, String value2) {
        consensusPairs.add(new Pair(value1, value2));
    }
    
}
