# spring-boot-starter-consistent-hashing

Client-side consistent-hash sharding and routing for Spring Boot 3 — drop-in starter, zero boilerplate.

---

## Why

Distributing load across a dynamic pool of nodes (cache servers, shards, workers) with a plain modulo hash breaks every time you add or remove a node — all keys remap. Consistent hashing limits remapping to only the keys that *need* to move.

This starter wires a production-ready consistent-hash ring into your Spring Boot application context. Declare your nodes in `application.properties`, inject the ring or use the `@ConsistentHashRouted` annotation, and you're done.

---

## Quick Start

### 1. Install (local, until Maven Central release)

```bash
git clone https://github.com/AKSHAT-KUMAR-GUPTA07/spring-boot-starter-consistent-hashing.git
cd spring-boot-starter-consistent-hashing
mvn install
```

### 2. Add the dependency

```xml
<dependency>
    <groupId>io.github.akshat-kumar-gupta07</groupId>
    <artifactId>consistent-hashing-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

### 3. Declare your nodes

```properties
consistent-hashing.virtual-nodes=150

consistent-hashing.nodes[0].id=cache1
consistent-hashing.nodes[0].host=redis1
consistent-hashing.nodes[0].port=6379
consistent-hashing.nodes[0].multiplier=1.0

consistent-hashing.nodes[1].id=cache2
consistent-hashing.nodes[1].host=redis2
consistent-hashing.nodes[1].port=6379
consistent-hashing.nodes[1].multiplier=1.0

consistent-hashing.nodes[2].id=cache3
consistent-hashing.nodes[2].host=redis3
consistent-hashing.nodes[2].port=6379
consistent-hashing.nodes[2].multiplier=1.0
```

### 4. Route a key

**Option A — inject the ring directly:**

```java
@Service
public class CacheService {

    private final ConsistentHashRing ring;

    public CacheService(ConsistentHashRing ring) {
        this.ring = ring;
    }

    public void store(String userId, Object value) {
        Node node = ring.route(userId);
        // connect to node.host() : node.port() and store
    }
}
```

**Option B — use the `@ConsistentHashRouted` annotation (zero manual routing code):**

```java
@Service
public class CacheRouter {

    @ConsistentHashRouted(key = "#userId")
    public void store(String userId, @RoutedNode Node node, Object value) {
        // node is already resolved — connect and store
        System.out.println("Routing " + userId + " → " + node.id());
    }
}
```

The aspect evaluates the `key` attribute as a [SpEL expression](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#expressions), resolves the target node, and injects it into the `@RoutedNode` parameter before your method runs.

---

## Features

| Feature | Detail |
|---|---|
| **Virtual nodes** | Each physical node occupies multiple positions on the ring — configurable via `consistent-hashing.virtual-nodes` (default `100`). More virtual nodes → more uniform distribution. |
| **Weighted nodes** | Set `multiplier` per node to skew distribution. A node with `multiplier=2.0` holds twice as many ring positions as one with `multiplier=1.0`. |
| **Pluggable hasher** | Default is MD5 (zero extra dependencies, JDK-native). Override with any `Hasher` implementation via `@Bean`. |
| **AOP annotation** | `@ConsistentHashRouted(key = "...")` + `@RoutedNode` — routing with no manual ring calls in business code. |
| **Auto-configuration** | Zero XML. Declare nodes in `application.properties` / `application.yml`, the ring is ready. |

---

## Configuration Reference

| Property | Type | Default | Description |
|---|---|---|---|
| `consistent-hashing.virtual-nodes` | `int` | `100` | Virtual node count per physical node (before `multiplier` scaling). |
| `consistent-hashing.nodes[n].id` | `String` | — | Unique node identifier. Used as the ring key. |
| `consistent-hashing.nodes[n].host` | `String` | — | Host address of the node. |
| `consistent-hashing.nodes[n].port` | `int` | — | Port of the node. |
| `consistent-hashing.nodes[n].multiplier` | `double` | `1.0` | Weight multiplier. Ring positions = `ceil(virtualNodes × multiplier)`. |

---

## Advanced Usage

### Weighted nodes

Give a high-memory node proportionally more ring positions:

```properties
consistent-hashing.nodes[0].id=cache-small
consistent-hashing.nodes[0].host=redis1
consistent-hashing.nodes[0].port=6379
consistent-hashing.nodes[0].multiplier=1.0

consistent-hashing.nodes[1].id=cache-large
consistent-hashing.nodes[1].host=redis2
consistent-hashing.nodes[1].port=6379
consistent-hashing.nodes[1].multiplier=3.0
```

`cache-large` receives approximately 75% of all keys; `cache-small` receives 25%.

### Custom hasher

The default MD5 hasher works well for consistent hashing. If you need a faster non-cryptographic hash (e.g. [XXH3](https://github.com/OpenHFT/Zero-Allocation-Hashing)), implement `Hasher` and declare it as a `@Bean`:

```java
// build.gradle / pom.xml: add net.openhft:zero-allocation-hashing
public class Xxh3Hasher implements Hasher {
    @Override
    public long hash(String input) {
        return LongHashFunction.xx3().hashChars(input);
    }
}

@Configuration
public class HashConfig {
    @Bean
    public Hasher hasher() {
        return new Xxh3Hasher();
    }
}
```

The starter's default `Md5Hasher` is `@ConditionalOnMissingBean` — your bean takes precedence automatically.

### YAML configuration

```yaml
consistent-hashing:
  virtual-nodes: 150
  nodes:
    - id: cache1
      host: redis1
      port: 6379
      multiplier: 1.0
    - id: cache2
      host: redis2
      port: 6379
      multiplier: 2.0
```

---

## How It Works

Consistent hashing maps both nodes and keys onto a fixed-size circular hash space (the "ring"). Each node occupies `virtualNodes × multiplier` positions, distributed evenly. To route a key, its hash is computed and the ring is walked clockwise to the nearest node position.

Virtual nodes are the key insight: without them, a 3-node cluster with plain consistent hashing can produce skewed load (one node gets 50% of keys, another gets 10%). With 150+ virtual nodes per physical node, the standard deviation of key distribution drops to under 5%.

Adding or removing a physical node only remaps the keys that fell between that node's ring positions and its predecessor — typically `1/N` of all keys for an N-node cluster.

---

## Building Locally

Requirements: Java 21, Maven 3.9+.

```bash
# build and install to local .m2
mvn install

# run tests
mvn test
```

---

## Roadmap

- **v0.2** — dynamic node add/remove at runtime + actuator endpoint (`GET /actuator/consistent-hashing/{ring}`) exposing live ring distribution histogram
- **v0.3** — hybrid-ring zero-downtime migration coordinator (pull-based background key migration during topology changes)

---

## License

MIT — see [LICENSE](LICENSE).

---

*Built by [Akshat Kumar Gupta](https://github.com/AKSHAT-KUMAR-GUPTA07)*
