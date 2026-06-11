package io.github.akshatkumargupta07.consistenthashing;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHashRing {

    private volatile TreeMap<Long, Node> ring = new TreeMap<>();
    private final Hasher hasher;
    private final int virtualNodes;

    public ConsistentHashRing(List<Node> nodes , int virtualNodes, Hasher hasher){
        this.hasher=hasher;
        this.virtualNodes=virtualNodes;
        hashRing(nodes, ring);
    }

    private void hashRing(List<Node> nodes, TreeMap<Long, Node> targetRing) {
        for (Node node : nodes) {
            for (int i = 0; i < (int) (virtualNodes * Math.ceil(node.multiplier())); i++) {
                long position = hash(node.id() + "-vnode-" + i);
                targetRing.put(position, node);
            }
        }
    }

    public Node route(String key){
        if(ring.isEmpty()){
            throw new IllegalStateException("No nodes in the ring");
        }
        long hash = hash(key);
        SortedMap<Long , Node> tail = ring.tailMap(hash);
        Long position = tail.isEmpty() ? ring.firstKey() : tail.firstKey();
        return ring.get(position);
    }

    private long hash(String input){
        return hasher.hash(input);
    }

    public synchronized void updateNodes(List<Node> nodes){
        TreeMap<Long, Node> newRing = new TreeMap<>();
        hashRing(nodes, newRing);
        ring = newRing;
    }
}
