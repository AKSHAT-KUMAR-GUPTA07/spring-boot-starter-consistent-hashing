package io.github.akshatkumargupta07.consistenthashing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConsistentHashingAutoConfigurationTest {

    // (1) The harness. One reusable runner, preloaded with the auto-config under test.
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConsistentHashingAutoConfiguration.class))
            .withPropertyValues(
                    "consistent-hashing.nodes[0].id=cache1",
                    "consistent-hashing.nodes[0].host=redis1",
                    "consistent-hashing.nodes[0].port=7000",
                    "consistent-hashing.nodes[0].multiplier=1.0"
            );

    // (2) CASE A — no custom bean. The default must win.
    @Test
    void usesPropertiesNodeProviderByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(NodeProvider.class);
            assertThat(context.getBean(NodeProvider.class))
                    .isInstanceOf(PropertiesNodeProvider.class);
        });
    }

    // (3) CASE B — app supplies its own NodeProvider. The default must back off.
    @Test
    void backsOffWhenCustomNodeProviderPresent() {
        runner.withUserConfiguration(CustomNodeProviderConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(NodeProvider.class);
                    assertThat(context.getBean(NodeProvider.class))
                            .isInstanceOf(CustomNodeProvider.class)
                            .isNotInstanceOf(PropertiesNodeProvider.class);
                    // prove the ring actually SEEDED from the custom provider:
                    assertThat(context.getBean(ConsistentHashRing.class).route("any-key").id())
                            .isEqualTo("custom-node");
                });
    }

    // (4) A custom provider + the @Configuration that registers it — the "app's" bean.
    static class CustomNodeProvider implements NodeProvider {
        @Override
        public List<Node> getNodes() {
            return List.of(new Node("custom-node", "custom-host", 9999, 1.0));
        }
    }

    @Configuration
    static class CustomNodeProviderConfig {
        @Bean
        NodeProvider customNodeProvider() {
            return new CustomNodeProvider();
        }
    }
}
