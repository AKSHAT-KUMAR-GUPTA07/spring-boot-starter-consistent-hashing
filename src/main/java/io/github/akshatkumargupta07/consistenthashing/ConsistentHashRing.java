package io.github.akshatkumargupta07.consistenthashing;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHashRing {

    private final TreeMap<Long, Node> ring = new TreeMap<>();
    private final Hasher hasher;

    public ConsistentHashRing(List<Node> nodes , int virtualNodes, Hasher hasher){
        this.hasher=hasher;
        for (Node node: nodes){
            for(int i=0 ; i<  (int) (virtualNodes *  Math.ceil(node.multiplier()) ); i++){
                long position = hash(node.id() + "-vnode-" + i);
                ring.put(position , node);
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
}
