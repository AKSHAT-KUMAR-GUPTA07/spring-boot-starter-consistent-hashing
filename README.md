# spring-boot-starter-consistent-hashing

Client-side consistent-hash sharding and routing for Spring Boot 3 — drop-in starter, zero boilerplate.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.akshat-kumar-gupta07/consistent-hashing-spring-boot-starter?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.akshat-kumar-gupta07/consistent-hashing-spring-boot-starter)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)
![License](https://img.shields.io/badge/license-MIT-green)

---

## Why

Distributing load across a pool of nodes (cache servers, shards, workers) with a plain modulo hash breaks every time the pool changes — add or remove one node and *almost every key* remaps to a different node, cold-flushing your entire cache. Consistent hashing limits remapping to only the `~1/N` of keys that actually have to move.

This starter wires a production-ready consistent-hash ring into your Spring Boot application context. Declare your nodes in `application.properties`, inject the ring (or use the `@ConsistentHashRouted` annotation), and you're routing. Membership can change at runtime without a restart, and the live ring is observable over an actuator endpoint.

---

## Quick Start

### 1. Add the dependency

Available on **[Maven Central](https://central.sonatype.com/artifact/io.github.akshat-kumar-gupta07/consistent-hashing-spring-boot-starter)** — no extra repository configuration needed.

**Maven:**

```xml
<dependency>
    <groupId>io.github.akshat-kumar-gupta07</groupId>
    <artifactId>consistent-hashing-spring-boot-starter</artifactId>
    <version>0.2.0</version>
</dependency>
```

**Gradle:**

```groovy
implementation 'io.github.akshat-kumar-gupta07:consistent-hashing-spring-boot-starter:0.2.0'
```

### 2. Declare your nodes

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

### 3. Route a key

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
| **Virtual nodes** | Each physical node occupies many positions on the ring — configurable via `consistent-hashing.virtual-nodes` (default `100`). More virtual nodes → more uniform distribution. |
| **Weighted nodes** | Set `multiplier` per node to skew distribution. A node with `multiplier=2.0` holds twice as many ring positions as one with `multiplier=1.0`. |
| **Dynamic membership** | Change the node pool at runtime with `updateNodes(...)` — lock-free reads, no restart, no downtime. |
| **Pluggable node source** | Implement the `NodeProvider` SPI to seed the ring from service discovery (Kubernetes, Consul, a database). Survives restarts. |
| **Pluggable hasher** | Default is MD5 (zero extra dependencies, JDK-native). Override with any `Hasher` implementation via `@Bean`. |
| **AOP annotation** | `@ConsistentHashRouted(key = "...")` + `@RoutedNode` — routing with no manual ring calls in business code. |
| **Observability** | `GET /actuator/hashring` exposes live ring membership (opt-in, only when actuator is present). |
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

## Dynamic Membership

Real clusters change shape — nodes crash, autoscalers add capacity, deployments roll. The ring handles topology changes at runtime without a restart and without blocking lookups.

### Update the pool at runtime

Inject the ring and hand it the **full** current membership whenever it changes:

```java
@Service
public class TopologyListener {

    private final ConsistentHashRing ring;

    public TopologyListener(ConsistentHashRing ring) {
        this.ring = ring;
    }

    // call this from your discovery watch / health-check loop
    public void onMembershipChange(List<Node> currentNodes) {
        ring.updateNodes(currentNodes);
    }
}
```

`updateNodes(...)` rebuilds a fresh ring and swaps it in atomically (copy-on-write). Concurrent `route(...)` calls never block and never see a half-built ring — they observe either the old ring or the new one, never anything in between. You pass the complete node list (not a delta), so the same call handles additions and removals and is naturally idempotent.

### Seed the ring from service discovery — `NodeProvider`

At startup the ring is seeded by a `NodeProvider`:

```java
public interface NodeProvider {
    List<Node> getNodes();
}
```

By default a `PropertiesNodeProvider` reads the nodes from your configuration. To back the ring with a live source — so that runtime `updateNodes(...)` changes **survive a restart** — provide your own bean:

```java
@Component
public class DiscoveryNodeProvider implements NodeProvider {

    @Override
    public List<Node> getNodes() {
        // pull current membership from Kubernetes / Consul / a DB at boot
        return ...;
    }
}
```

Your bean automatically replaces the default (`@ConditionalOnMissingBean`). The starter stays backend-agnostic — no discovery client is pulled into your app unless you add one.

---

## Observability

When [Spring Boot Actuator](https://docs.spring.io/spring-boot/reference/actuator/index.html) is on the classpath, the starter exposes a read-only endpoint showing the ring's current membership.

**1. Add actuator** (the starter declares it `optional`, so you opt in explicitly):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**2. Expose the endpoint:**

```properties
management.endpoints.web.exposure.include=health,hashring
```

**3. Query it:**

```bash
curl localhost:8080/actuator/hashring
```

```json
{
  "nodeCount": 3,
  "nodes": [
    { "id": "cache1", "host": "redis1", "port": 6379, "multiplier": 1.0 },
    { "id": "cache2", "host": "redis2", "port": 6379, "multiplier": 1.0 },
    { "id": "cache3", "host": "redis3", "port": 6379, "multiplier": 1.0 }
  ]
}
```

If actuator is **not** on the classpath, the endpoint simply does not exist — no crash, no forced dependency. It is purely opt-in.

---

## Performance

`route()` is the only call on your hot path, so it's the number that matters. It runs in **a few hundred nanoseconds** and scales **logarithmically** — a thousand-fold bigger ring costs only ~2× the latency.

Measured with [JMH](https://github.com/openjdk/jmh) (`RouteBenchmark`, average time, 3 forks × 10 iterations, single thread):

| Physical nodes | Virtual nodes | Ring entries | `route()` latency |
|---:|---:|---:|---:|
| 10 | 50 | 500 | 238 ns |
| 10 | 150 | 1,500 | 266 ns |
| 100 | 50 | 5,000 | 292 ns |
| 100 | 150 | 15,000 | 319 ns |
| 100 | 500 | 50,000 | 364 ns |
| 1000 | 150 | 150,000 | 423 ns |
| 1000 | 500 | 500,000 | 476 ns |

The ring grows **1000×** (500 → 500,000 entries) while latency grows only **2.0×** (238 → 476 ns) — an empirical match to the expected `O(log V)` curve. Latency depends only on the *total* ring size, not on how nodes and virtual nodes are split. Even against a half-million-entry ring, that's **~2.1 million routes/sec on a single thread**; reads are lock-free, so it scales across cores.

> Environment: Intel Core i5-8250U @ 1.60GHz (laptop), OpenJDK 21.0.9, JMH 1.37, Ubuntu 24.04. Reproduce with `RouteBenchmark` under `src/test/java` (run its `main`). Server-class CPUs will be meaningfully faster.

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
// pom.xml / build.gradle: add net.openhft:zero-allocation-hashing
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

Consistent hashing maps both nodes and keys onto a fixed-size circular hash space (the "ring"). Each node occupies `virtualNodes × multiplier` positions, distributed evenly. To route a key, its hash is computed and the ring is walked clockwise to the nearest node position — internally a single `O(log V)` lookup over a `TreeMap`.

Virtual nodes are the key insight: without them, a 3-node cluster can produce badly skewed load (one node gets 50% of keys, another 10%). With 150+ virtual nodes per physical node, the spread of key distribution tightens to within a few percent.

Adding or removing a physical node only remaps the keys that fell between that node's ring positions and its predecessors — typically `~1/N` of all keys for an N-node cluster, versus *almost all* keys with a plain modulo hash.

The ring is held in a `volatile` reference and mutated copy-on-write: `updateNodes(...)` builds a new ring and swaps it in, so `route(...)` reads are lock-free and always consistent.

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

- **v0.2** ✅ — dynamic node add/remove at runtime (`updateNodes`), `NodeProvider` SPI for discovery-backed seeding, membership actuator endpoint (`GET /actuator/hashring`), and a JMH `route()` benchmark.
- **v0.3** — load-distribution endpoint (`GET /actuator/hashring/distribution`) showing the share of the ring each node owns; MD5-vs-XXH3 and `updateNodes` rebuild benchmarks; hybrid-ring zero-downtime migration coordinator (pull-based background key migration during topology changes).

---

## License

MIT — see [LICENSE](LICENSE).

---

*Built by [Akshat Kumar Gupta](https://github.com/AKSHAT-KUMAR-GUPTA07)*
