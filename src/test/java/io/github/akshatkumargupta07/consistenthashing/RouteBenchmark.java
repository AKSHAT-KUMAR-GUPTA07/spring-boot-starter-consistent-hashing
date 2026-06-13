package io.github.akshatkumargupta07.consistenthashing;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)        // measure average time PER CALL
@OutputTimeUnit(TimeUnit.NANOSECONDS)   // report that time in nanoseconds
@State(Scope.Benchmark)                 // one shared instance holds the ring
@Warmup(iterations = 5, time = 1)       // 5 throwaway rounds to warm the JIT
@Measurement(iterations = 10, time = 1) // 10 real rounds that actually count
@Fork(3)                                // run in 3 independent JVMs (variance)
@Threads(1)
public class RouteBenchmark {

    @Param({"10", "100", "1000"})       // physical nodes
    private int nodeCount;

    @Param({"50", "150", "500"})        // virtual nodes per physical node
    private int virtualNodes;

    private ConsistentHashRing ring;
    private String[] keys;
    private int index;

    @Setup(Level.Trial)                 // runs ONCE before timing — not measured
    public void setup() {
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            nodes.add(new Node("node-" + i, "host-" + i, 7000 + i, 1.0));
        }
        ring = new ConsistentHashRing(nodes, virtualNodes, new Md5Hasher());

        keys = new String[1024];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = "key-" + i;
        }
    }

    @Benchmark
    public Node routeBench() {
        return ring.route(keys[index++ & 1023]);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(RouteBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }

}
