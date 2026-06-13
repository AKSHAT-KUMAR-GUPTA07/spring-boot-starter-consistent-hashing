package io.github.akshatkumargupta07.consistenthashing;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.Map;
import java.util.Set;

@Endpoint(id = "hashring")
public class ConsistentHashEndpoint {

    private final ConsistentHashRing consistentHashRing;

    public ConsistentHashEndpoint (ConsistentHashRing consistentHashRing){
        this.consistentHashRing=consistentHashRing;
    }

    @ReadOperation
    public Map<String,Object> ringState(){
        Set<Node> nodes = consistentHashRing.getNodes();
        return Map.of(
                "nodeCount" , nodes.size(),
                "nodes" , nodes
        );
    }
}
