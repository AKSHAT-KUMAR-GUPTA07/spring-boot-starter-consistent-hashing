package io.github.akshatkumargupta07.consistenthashing;

import java.util.*;

public class ConsistentHashRing {

    /*
     * volatile is safe here despite SonarQube java:S3077 (volatile on a mutable
     * object only publishes the reference, not its contents). This ring is
     * copy-on-write: the live TreeMap is never mutated — updateNodes() builds a
     * new map and swaps this reference, which is the only thing that changes, and
     * volatile publishes that swap to lock-free route() readers.
     * Invariant: never put()/remove() on the live map; always build-new-and-swap.
     */
    @SuppressWarnings("java:S3077")
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
        TreeMap<Long, Node> currentRing = ring;
        if(currentRing.isEmpty()){
            throw new IllegalStateException("No nodes in the ring");
        }
        long hash = hash(key);
        var entry = currentRing.ceilingEntry(hash);
        if(entry == null){
            entry = currentRing.firstEntry();
        }
        return entry.getValue();
    }

    private long hash(String input){
        return hasher.hash(input);
    }

    public synchronized void updateNodes(List<Node> nodes){
        TreeMap<Long, Node> newRing = new TreeMap<>();
        hashRing(nodes, newRing);
        ring = newRing;
    }

    public Set<Node> getNodes(){
        return new HashSet<>(ring.values());
    }
}
