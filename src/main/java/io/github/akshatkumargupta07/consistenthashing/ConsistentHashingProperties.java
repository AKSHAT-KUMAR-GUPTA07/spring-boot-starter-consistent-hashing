package io.github.akshatkumargupta07.consistenthashing;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "consistent-hashing")
public class ConsistentHashingProperties {

    private int virtualNodes = 100;
    private List<Node> nodes = List.of();

    public int getVirtualNodes(){
        return virtualNodes;
    }

    public void setVirtualNodes(int virtualNodes){
        this.virtualNodes=virtualNodes;
    }

    public List<Node> getNodes(){
        return nodes;
    }

    public void setNodes(List<Node> nodes){
        this.nodes=nodes;
    }
}
