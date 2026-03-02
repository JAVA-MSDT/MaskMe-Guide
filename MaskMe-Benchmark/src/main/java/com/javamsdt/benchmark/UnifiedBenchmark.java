package com.javamsdt.benchmark;

import com.javamsdt.masking.SpringMaskingApplication;
import com.javamsdt.masking.dto.UserDto;
import com.javamsdt.masking.mapper.UserMapper;
import com.javamsdt.masking.service.UserService;
import com.javamsdt.masking.domain.User;
import com.javamsdt.masking.maskme.condition.PhoneMaskingCondition;
import io.github.javamsdt.maskme.MaskMeInitializer;
import io.github.javamsdt.maskme.implementation.condition.MaskMeOnInput;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.management.*;
import java.text.DecimalFormat;
import java.util.*;

public class UnifiedBenchmark {

    private static final int WARMUP = 10000;
    private static final int ITERATIONS = 100000;
    private static final int BATCH_SIZE = 1000;
    private static final int CONCURRENT_THREADS = 10;
    private static final int CONCURRENT_OPS = 1000;
    private static final double NS_TO_MS = 1_000_000.0;
    private static final DecimalFormat DF = new DecimalFormat("#.####");

    public static void main(String[] args) {
        System.out.println("=".repeat(100));
        System.out.println("MASKME vs HARDCODED BENCHMARK");
        System.out.println("=".repeat(100));
        System.out.println("\n### Benchmark Strategy\n");
        System.out.println("This benchmark uses JMH-style methodology:");
        System.out.println("  1. Warmup Phase: " + WARMUP + " iterations - JIT compilation & optimization");
        System.out.println("  2. GC Pause: 100ms - Stabilize memory state");
        System.out.println("  3. Measure Phase: " + ITERATIONS + " iterations - Collect performance data");
        System.out.println("  4. Statistical Analysis: Calculate percentiles, averages, resource usage\n");
        
        System.out.println("### Metrics Explained\n");
        System.out.println("  • Avg Time: Mean execution time per single masking operation");
        System.out.println("  • 95th Percentile: 95% of operations complete within this time (excludes outliers)");
        System.out.println("  • Memory per Op: Average memory allocated per operation (bytes)");
        System.out.println("  • CPU Usage: Percentage of CPU time spent on masking vs total time");
        System.out.println("  • GC Collections: Number of garbage collection cycles during measurement");
        System.out.println("  • Throughput: Operations per second");
        System.out.println("  • Impact: Percentage difference (MaskMe vs Hardcoded)\n");

        Results hardcoded = runHardcodedBenchmark();
        Results maskme = runMaskMeBenchmark();

        printResults(hardcoded, maskme);
    }

    private static Results runHardcodedBenchmark() {
        System.out.println("[1/2] Running Hardcoded Benchmark...");
        
        com.javamsdt.hardcoded.dto.AddressDto address = new com.javamsdt.hardcoded.dto.AddressDto(
            1L, "first Street", "City One", "Zip One");
        com.javamsdt.hardcoded.dto.UserDto user = new com.javamsdt.hardcoded.dto.UserDto(
            1L, "Ahmed Samy", "one@mail.com", "123456", "01000000000",
            address, java.time.LocalDate.of(1985, 1, 25), "M", "Male",
            new java.math.BigDecimal("20.0"), java.time.Instant.now());

        Results results = new Results();
        
        System.out.println("  🔹 Single Condition");
        results.single = benchmarkSingle(() -> user.mask("maskMe", null));
        
        System.out.println("  🔹 Multiple Conditions");
        results.multiple = benchmarkSingle(() -> user.mask("maskMe", "01000000000"));
        
        System.out.println("  🔹 Batch Processing");
        results.batch = benchmarkBatch(() -> user.mask("maskMe", "01000000000"));
        
        System.out.println("  🔹 Concurrent");
        results.concurrent = benchmarkConcurrent(() -> user.mask("maskMe", "01000000000"));
        
        System.out.println("✓ Hardcoded completed\n");
        return results;
    }

    private static Results runMaskMeBenchmark() {
        System.out.println("[2/2] Running MaskMe Benchmark...");
        
        ConfigurableApplicationContext context = SpringApplication.run(SpringMaskingApplication.class, "--spring.main.banner-mode=off");
        
        try {
            UserService userService = context.getBean(UserService.class);
            UserMapper userMapper = context.getBean(UserMapper.class);
            User user = userService.findUserById(1L);
            UserDto userDto = userMapper.toDto(user);

            Results results = new Results();
            
            System.out.println("  🔹 Single Condition");
            results.single = benchmarkSingle(() -> MaskMeInitializer.mask(userDto, MaskMeOnInput.class, "maskMe"));
            
            System.out.println("  🔹 Multiple Conditions");
            results.multiple = benchmarkSingle(() -> MaskMeInitializer.mask(userDto, MaskMeOnInput.class, "maskMe", PhoneMaskingCondition.class, "01000000000"));
            
            System.out.println("  🔹 Batch Processing");
            results.batch = benchmarkBatch(() -> MaskMeInitializer.mask(userDto, MaskMeOnInput.class, "maskMe", PhoneMaskingCondition.class, "01000000000"));
            
            System.out.println("  🔹 Concurrent");
            results.concurrent = benchmarkConcurrent(() -> MaskMeInitializer.mask(userDto, MaskMeOnInput.class, "maskMe", PhoneMaskingCondition.class, "01000000000"));
            
            System.out.println("✓ MaskMe completed\n");
            return results;
        } finally {
            context.close();
        }
    }

    private static Metric benchmarkSingle(Runnable task) {
        for (int i = 0; i < WARMUP; i++) task.run();
        System.gc();
        try { Thread.sleep(100); } catch (Exception e) {}
        
        System.gc();
        Runtime runtime = Runtime.getRuntime();
        long memBefore = runtime.totalMemory() - runtime.freeMemory();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long cpuBefore = threadMXBean.getCurrentThreadCpuTime();
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        long gcBefore = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
        
        List<Long> samples = new ArrayList<>(ITERATIONS);
        long start = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            long s = System.nanoTime();
            task.run();
            samples.add(System.nanoTime() - s);
        }
        long end = System.nanoTime();
        
        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        long cpuAfter = threadMXBean.getCurrentThreadCpuTime();
        long gcAfter = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
        
        Collections.sort(samples);
        Metric m = new Metric();
        m.avgTime = (end - start) / NS_TO_MS / ITERATIONS;
        m.p95 = samples.get((int)(samples.size() * 0.95)) / NS_TO_MS;
        m.memory = Math.max(0, (memAfter - memBefore) / ITERATIONS);
        m.cpu = ((cpuAfter - cpuBefore) / NS_TO_MS) / ((end - start) / NS_TO_MS) * 100;
        m.gc = gcAfter - gcBefore;
        return m;
    }

    private static Metric benchmarkBatch(Runnable task) {
        for (int i = 0; i < 100; i++) task.run();
        
        System.gc();
        long start = System.nanoTime();
        for (int i = 0; i < BATCH_SIZE; i++) task.run();
        long end = System.nanoTime();
        
        Metric m = new Metric();
        m.avgTime = (end - start) / NS_TO_MS / BATCH_SIZE;
        m.throughput = BATCH_SIZE * 1000.0 / ((end - start) / NS_TO_MS);
        return m;
    }

    private static Metric benchmarkConcurrent(Runnable task) {
        for (int i = 0; i < 100; i++) task.run();
        
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            threads.add(new Thread(() -> {
                for (int j = 0; j < CONCURRENT_OPS; j++) task.run();
            }));
        }
        
        long start = System.currentTimeMillis();
        threads.forEach(Thread::start);
        threads.forEach(t -> { try { t.join(); } catch (Exception e) {} });
        long end = System.currentTimeMillis();
        
        Metric m = new Metric();
        m.totalTime = end - start;
        m.throughput = (CONCURRENT_THREADS * CONCURRENT_OPS * 1000.0) / m.totalTime;
        return m;
    }

    private static void printResults(Results hardcoded, Results maskme) {
        System.out.println("=".repeat(100));
        System.out.println("BENCHMARK RESULTS");
        System.out.println("=".repeat(100));

        System.out.println("\n### Test Environment\n");
        System.out.println("- **Hardware**: " + System.getProperty("os.arch") + ", " + 
            (Runtime.getRuntime().maxMemory() / 1024 / 1024) + "MB RAM");
        System.out.println("- **JVM**: " + System.getProperty("java.version"));
        System.out.println("- **Scale**: 11 fields (UserDto) + 4 fields (AddressDto)");

        System.out.println("\n### Single Condition\n");
        System.out.println("| Metric              | Hardcoded    | MaskMe       | Impact     |");
        System.out.println("| ------------------- | ------------ | ------------ | ---------- |");
        printRow("Avg Time", hardcoded.single.avgTime, maskme.single.avgTime, "ms");
        printRow("95th Percentile", hardcoded.single.p95, maskme.single.p95, "ms");
        printRow("Memory per Op", hardcoded.single.memory / 1024.0, maskme.single.memory / 1024.0, "KB");
        printRow("CPU Usage", hardcoded.single.cpu, maskme.single.cpu, "%");
        printRow("GC Collections", hardcoded.single.gc, maskme.single.gc, "");

        System.out.println("\n### Multiple Conditions\n");
        System.out.println("| Metric              | Hardcoded    | MaskMe       | Impact     |");
        System.out.println("| ------------------- | ------------ | ------------ | ---------- |");
        printRow("Avg Time", hardcoded.multiple.avgTime, maskme.multiple.avgTime, "ms");
        printRow("95th Percentile", hardcoded.multiple.p95, maskme.multiple.p95, "ms");
        printRow("Memory per Op", hardcoded.multiple.memory / 1024.0, maskme.multiple.memory / 1024.0, "KB");
        printRow("CPU Usage", hardcoded.multiple.cpu, maskme.multiple.cpu, "%");
        printRow("GC Collections", hardcoded.multiple.gc, maskme.multiple.gc, "");

        System.out.println("\n### Batch Processing (" + BATCH_SIZE + " users)\n");
        System.out.println("| Metric              | Hardcoded    | MaskMe       | Impact     |");
        System.out.println("| ------------------- | ------------ | ------------ | ---------- |");
        printRow("Avg Time", hardcoded.batch.avgTime, maskme.batch.avgTime, "ms");
        printRow("Throughput", hardcoded.batch.throughput, maskme.batch.throughput, "ops/s");

        System.out.println("\n### Concurrent (" + CONCURRENT_THREADS + " threads, " + CONCURRENT_OPS + " ops each)\n");
        System.out.println("| Metric              | Hardcoded    | MaskMe       | Impact     |");
        System.out.println("| ------------------- | ------------ | ------------ | ---------- |");
        printRow("Total Time", hardcoded.concurrent.totalTime, maskme.concurrent.totalTime, "ms");
        printRow("Throughput", hardcoded.concurrent.throughput, maskme.concurrent.throughput, "ops/s");

        System.out.println("=".repeat(100));
    }

    private static void printRow(String metric, double hardcoded, double maskme, String unit) {
        String hStr = DF.format(hardcoded) + unit;
        String mStr = DF.format(maskme) + unit;
        
        if (hardcoded < 0.0001 || maskme < 0.0001) {
            System.out.printf("| %-19s | %12s | %12s | %10s |\n", metric, hStr, mStr, "N/A");
        } else {
            double impact = ((maskme - hardcoded) / hardcoded) * 100;
            String impactStr = (impact > 0 ? "~" : "") + DF.format(impact) + "%";
            System.out.printf("| %-19s | %12s | %12s | %10s |\n", metric, hStr, mStr, impactStr);
        }
    }

    static class Results {
        Metric single, multiple, batch, concurrent;
    }

    static class Metric {
        double avgTime, p95, cpu, throughput, totalTime;
        long memory, gc;
    }
}
