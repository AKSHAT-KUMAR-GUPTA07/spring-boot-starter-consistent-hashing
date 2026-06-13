package io.github.akshatkumargupta07.consistenthashing;

import java.util.List;

public class PropertiesNodeProvider implements NodeProvider{

    private final ConsistentHashingProperties consistentHashingProperties;

    public PropertiesNodeProvider(ConsistentHashingProperties consistentHashingProperties){
        this.consistentHashingProperties=consistentHashingProperties;
    }

    @Override
    public List<Node> getNodes() {
        return consistentHashingProperties.getNodes();
    }
}
