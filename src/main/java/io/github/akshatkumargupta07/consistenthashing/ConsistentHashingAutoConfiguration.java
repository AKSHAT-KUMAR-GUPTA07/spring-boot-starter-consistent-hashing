package io.github.akshatkumargupta07.consistenthashing;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(ConsistentHashingProperties.class)
public class ConsistentHashingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ConsistentHashRing createHashRing(NodeProvider nodeProvider, ConsistentHashingProperties consistentHashingProperties, Hasher hasher){
        return new ConsistentHashRing(nodeProvider.getNodes() , consistentHashingProperties.getVirtualNodes(), hasher);
    }

    @Bean
    @ConditionalOnMissingBean
    public Hasher defaultHasher(){
        return new Md5Hasher();
    }

    @Bean
    public ConsistentHashRoutingAspect consistentHashRoutingAspect(ConsistentHashRing consistentHashRing){
        return new ConsistentHashRoutingAspect(consistentHashRing);
    }

    @Bean
    @ConditionalOnMissingBean
    public NodeProvider propertiesNodeProvider(ConsistentHashingProperties consistentHashingProperties){
        return new PropertiesNodeProvider(consistentHashingProperties);
    }
}
