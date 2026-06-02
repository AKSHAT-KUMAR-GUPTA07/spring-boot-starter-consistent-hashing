package io.github.akshatkumargupta07.consistenthashing;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHashRing {

    private final TreeMap<Long, Node> ring = new TreeMap<>();

    public ConsistentHashRing(List<Node> nodes , int virtualNodes){
        for (Node node: nodes){
            for(int i=0 ; i<virtualNodes; i++){
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
        try{
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            long h=0;
            for(int i=0 ; i<8; i++){
                h = (h << 8) | (digest[i] & 0xFF );
            }
            return h;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }
}
