package io.github.akshatkumargupta07.consistenthashing;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

public class ConsistentHashRingTest {

    List<Node> nodes = List.of(
            new Node("cache1", "redis1", 7000, 1.0),
            new Node("cache2", "redis2", 7001, 2.0),
            new Node("cache3", "redis3", 7002, 3.0)
    );

    ConsistentHashRing consistentHashRing = new ConsistentHashRing(nodes, 300, new Md5Hasher());

    @Test
    void distributionTest() {
        HashMap<String, Integer> counts = new HashMap<>();

        for (int i = 0; i < 10000; i++) {
            Node node = consistentHashRing.route("key-" + i);
            counts.merge(node.id(), 1, Integer::sum);
        }

        counts.forEach((id, count) ->
                System.out.println(id + " -> " + count + " keys"));
    }

}
