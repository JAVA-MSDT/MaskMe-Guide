package com.javamsdt.hardcoded.benchmark;

import com.javamsdt.hardcoded.dto.AddressDto;
import com.javamsdt.hardcoded.dto.UserDto;

import java.lang.management.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.text.DecimalFormat;

public class HardcodedBenchmark {

    private static final int WARMUP_ITERATIONS = 1000;
    private static final int MEASURE_ITERATIONS = 10000;
    private static final double NANOSECONDS_IN_MS = 1_000_000.0;
    private static final DecimalFormat DF = new DecimalFormat("#.##");

    private static final Map<String, MetricData> metrics = new LinkedHashMap<>();

    public static void main(String[] args) {
        System.out.println("=".repeat(100));
        System.out.println("🔧 HARDCODED MASKING BENCHMARK");
        System.out.println("=".repeat(100));
        System.out.println("Warmup: " + WARMUP_ITERATIONS + " iterations");
        System.out.println("Measure: " + MEASURE_ITERATIONS + " iterations\n");

        UserDto testUser = createTestUser();

        benchmarkNoMasking(testUser);
        benchmarkSingleCondition(testUser);
        benchmarkMultipleConditions(testUser);
        benchmarkBatchProcessing(100);
        benchmarkConcurrent(10);
        benchmarkGCPressure(1000);

        printComparisonTable();
        printDetailedMetrics();
    }

    private static UserDto createTestUser() {
        AddressDto address = new AddressDto(1L, "first Street", "City One", "Zip One");
        return new UserDto(
            1L, "Ahmed Samy", "one@mail.com", "123456", "01000000000",
            address, LocalDate.of(1985, 1, 25), "M", "Male",
            new BigDecimal("20.0"), Instant.now()
        );
    }

    private static void benchmarkNoMasking(UserDto userDto) {
        System.out.println("🔹 NO MASKING (Baseline)");
        
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            UserDto result = userDto;
        }

        List<Long> samples = new ArrayList<>();
        long start = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            long sampleStart = System.nanoTime();
            UserDto result = userDto;
            samples.add(System.nanoTime() - sampleStart);
        }
        long end = System.nanoTime();

        metrics.put("baseline", calculateMetrics("No Masking", start, end, samples));
    }

    private static void benchmarkSingleCondition(UserDto userDto) {
        System.out.println("🔹 SINGLE CONDITION (Hardcoded maskMe check)");
        
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            userDto.mask("maskMe", null);
        }

        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long beforeMem = runtime.totalMemory() - runtime.freeMemory();
        
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long cpuBefore = threadMXBean.getCurrentThreadCpuTime();

        List<Long> samples = new ArrayList<>();
        long start = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            long sampleStart = System.nanoTime();
            userDto.mask("maskMe", null);
            samples.add(System.nanoTime() - sampleStart);
        }
        long end = System.nanoTime();

        long cpuAfter = threadMXBean.getCurrentThreadCpuTime();
        long afterMem = runtime.totalMemory() - runtime.freeMemory();

        MetricData data = calculateMetrics("Single Condition", start, end, samples);
        data.memoryPerOp = (afterMem - beforeMem) / MEASURE_ITERATIONS;
        data.cpuTimePerOp = (cpuAfter - cpuBefore) / MEASURE_ITERATIONS;
        metrics.put("single", data);
    }

    private static void benchmarkMultipleConditions(UserDto userDto) {
        System.out.println("🔹 MULTIPLE CONDITIONS (maskMe + phone check)");
        
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            userDto.mask("maskMe", "01000000000");
        }

        List<Long> samples = new ArrayList<>();
        long start = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            long sampleStart = System.nanoTime();
            userDto.mask("maskMe", "01000000000");
            samples.add(System.nanoTime() - sampleStart);
        }
        long end = System.nanoTime();

        metrics.put("multiple", calculateMetrics("Multiple Conditions", start, end, samples));
    }

    private static void benchmarkBatchProcessing(int batchSize) {
        System.out.println("🔹 BATCH PROCESSING (" + batchSize + " users)");
        
        List<UserDto> users = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            users.add(createTestUser());
        }

        for (int i = 0; i < 100; i++) {
            users.forEach(u -> u.mask("maskMe", "01000000000"));
        }

        long start = System.nanoTime();
        users.forEach(u -> u.mask("maskMe", "01000000000"));
        long end = System.nanoTime();

        MetricData data = new MetricData();
        data.name = "Batch Processing";
        data.totalTime = (end - start) / NANOSECONDS_IN_MS;
        data.avgResponseTime = data.totalTime / batchSize;
        data.throughput = batchSize * 1000.0 / data.totalTime;
        data.operations = batchSize;
        metrics.put("batch", data);
    }

    private static void benchmarkConcurrent(int threadCount) {
        System.out.println("🔹 CONCURRENT (" + threadCount + " threads)");
        
        List<Thread> threads = new ArrayList<>();
        List<ConcurrentTask> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            ConcurrentTask task = new ConcurrentTask();
            tasks.add(task);
            threads.add(new Thread(task));
        }

        tasks.forEach(ConcurrentTask::run);
        tasks.forEach(ConcurrentTask::reset);

        long start = System.currentTimeMillis();
        threads.forEach(Thread::start);
        for (Thread thread : threads) {
            try { thread.join(); } catch (InterruptedException e) { e.printStackTrace(); }
        }
        long end = System.currentTimeMillis();

        long totalOps = tasks.stream().mapToLong(ConcurrentTask::getCount).sum();
        MetricData data = new MetricData();
        data.name = "Concurrent";
        data.totalTime = end - start;
        data.throughput = totalOps * 1000.0 / data.totalTime;
        data.operations = totalOps;
        data.threads = threadCount;
        metrics.put("concurrent", data);
    }

    private static void benchmarkGCPressure(int iterations) {
        System.out.println("🔹 GC PRESSURE TEST (" + iterations + " ops)");
        
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        long gcCountBefore = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
        long gcTimeBefore = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();

        long start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            UserDto user = createTestUser();
            user.mask("maskMe", "01000000000");
        }
        long end = System.currentTimeMillis();

        long gcCountAfter = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
        long gcTimeAfter = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();

        MetricData data = new MetricData();
        data.name = "GC Pressure";
        data.gcCount = gcCountAfter - gcCountBefore;
        data.gcTime = gcTimeAfter - gcTimeBefore;
        data.totalTime = end - start;
        data.operations = iterations;
        metrics.put("gc", data);
    }

    private static MetricData calculateMetrics(String name, long start, long end, List<Long> samples) {
        MetricData data = new MetricData();
        data.name = name;
        long totalTime = end - start;
        data.avgResponseTime = (totalTime / (double) MEASURE_ITERATIONS) / NANOSECONDS_IN_MS;
        data.throughput = MEASURE_ITERATIONS * 1000.0 / (totalTime / NANOSECONDS_IN_MS);
        data.operations = MEASURE_ITERATIONS;
        data.totalTime = totalTime / NANOSECONDS_IN_MS;

        Collections.sort(samples);
        data.p50 = samples.get(samples.size() / 2) / NANOSECONDS_IN_MS;
        data.p95 = samples.get((int)(samples.size() * 0.95)) / NANOSECONDS_IN_MS;
        data.p99 = samples.get((int)(samples.size() * 0.99)) / NANOSECONDS_IN_MS;

        return data;
    }

    private static void printComparisonTable() {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("📊 HARDCODED MASKING - PERFORMANCE RESULTS");
        System.out.println("=".repeat(100));

        MetricData baseline = metrics.get("baseline");
        MetricData single = metrics.get("single");
        MetricData multiple = metrics.get("multiple");
        MetricData batch = metrics.get("batch");
        MetricData concurrent = metrics.get("concurrent");
        MetricData gc = metrics.get("gc");

        System.out.println("\n📈 RESPONSE TIME METRICS");
        System.out.println("-".repeat(100));
        System.out.printf("| %-25s | %-12s | %-12s | %-12s | %-12s | %-12s |\n",
                "Metric", "No Masking", "Single Cond", "Multiple Cond", "vs Baseline", "vs Single");
        System.out.println("-".repeat(100));

        printRow("Avg Response Time (ms)", baseline.avgResponseTime, single.avgResponseTime, multiple.avgResponseTime);
        printRow("95th Percentile (ms)", baseline.p95, single.p95, multiple.p95);
        printRow("99th Percentile (ms)", baseline.p99, single.p99, multiple.p99);
        printRow("Throughput (ops/sec)", baseline.throughput, single.throughput, multiple.throughput);
        System.out.println("-".repeat(100));

        System.out.println("\n💾 RESOURCE USAGE METRICS");
        System.out.println("-".repeat(100));
        System.out.printf("| %-25s | %-12s | %-12s | %-12s | %-12s |\n",
                "Metric", "No Masking", "Single Cond", "Multiple Cond", "Impact");
        System.out.println("-".repeat(100));

        double memImpact = baseline.memoryPerOp == 0 ?
                (single.memoryPerOp > 0 ? 100.0 : 0.0) :
                ((single.memoryPerOp - baseline.memoryPerOp) / baseline.memoryPerOp) * 100;

        double cpuImpact = baseline.cpuTimePerOp == 0 ?
                (single.cpuTimePerOp > 0 ? 100.0 : 0.0) :
                ((single.cpuTimePerOp - baseline.cpuTimePerOp) / baseline.cpuTimePerOp) * 100;

        System.out.printf("| %-25s | %-12s | %-12s | %-12s | %-12s |\n",
                "Memory per Op (bytes)", formatNumber(baseline.memoryPerOp),
                formatNumber(single.memoryPerOp), formatNumber(multiple.memoryPerOp), formatPercent(memImpact));

        System.out.printf("| %-25s | %-12s | %-12s | %-12s | %-12s |\n",
                "CPU Time per Op (ns)", formatNumber(baseline.cpuTimePerOp),
                formatNumber(single.cpuTimePerOp), formatNumber(multiple.cpuTimePerOp), formatPercent(cpuImpact));
        System.out.println("-".repeat(100));

        System.out.println("\n🗑️  GARBAGE COLLECTION METRICS");
        System.out.println("-".repeat(80));
        System.out.printf("| %-25s | %-15s | %-15s | %-15s |\n", "Metric", "GC Count", "GC Time (ms)", "GC/op");
        System.out.println("-".repeat(80));
        if (gc != null) {
            double gcPerOp = gc.gcCount * 1000.0 / gc.operations;
            System.out.printf("| %-25s | %-15s | %-15s | %-15s |\n",
                    "With Masking", gc.gcCount, formatNumber(gc.gcTime), formatNumber(gcPerOp));
        }
        System.out.println("-".repeat(80));

        System.out.println("\n🔄 BATCH & CONCURRENT METRICS");
        System.out.println("-".repeat(80));
        System.out.printf("| %-25s | %-15s | %-15s | %-15s |\n", "Scenario", "Ops Total", "Time (ms)", "Throughput");
        System.out.println("-".repeat(80));
        if (batch != null) {
            System.out.printf("| %-25s | %-15s | %-15s | %-15s |\n",
                    "Batch (100 users)", batch.operations, formatNumber(batch.totalTime),
                    formatNumber(batch.throughput) + " ops/sec");
        }
        if (concurrent != null) {
            System.out.printf("| %-25s | %-15s | %-15s | %-15s |\n",
                    "Concurrent (" + concurrent.threads + " threads)", concurrent.operations,
                    formatNumber(concurrent.totalTime), formatNumber(concurrent.throughput) + " ops/sec");
        }
        System.out.println("-".repeat(80));
        System.out.println("\n✅ Hardcoded benchmark completed!");
    }

    private static void printRow(String label, double baseline, double single, double multiple) {
        double vsBaseline = ((single - baseline) / baseline) * 100;
        double vsSingle = ((multiple - single) / single) * 100;
        System.out.printf("| %-25s | %-12s | %-12s | %-12s | %-12s | %-12s |\n",
                label, formatNumber(baseline), formatNumber(single), formatNumber(multiple),
                formatPercent(vsBaseline), formatPercent(vsSingle));
    }

    private static String formatNumber(double value) {
        if (value < 0.01) return "<0.01";
        return DF.format(value);
    }

    private static String formatPercent(double value) {
        return (value > 0 ? "+" : "") + DF.format(value) + "%";
    }

    private static void printDetailedMetrics() {
        System.out.println("\n📋 DETAILED METRICS BY SCENARIO");
        System.out.println("=".repeat(80));

        for (MetricData data : metrics.values()) {
            if (data == null) continue;
            System.out.println("\n🔸 " + data.name);
            System.out.println("   " + "-".repeat(40));

            if (data.avgResponseTime > 0) {
                System.out.printf("   Avg Response Time: %10s ms\n", formatNumber(data.avgResponseTime));
                System.out.printf("   95th Percentile:  %10s ms\n", formatNumber(data.p95));
                System.out.printf("   99th Percentile:  %10s ms\n", formatNumber(data.p99));
                System.out.printf("   Throughput:       %10s ops/sec\n", formatNumber(data.throughput));
            }
            if (data.memoryPerOp > 0) {
                System.out.printf("   Memory per Op:    %10s bytes\n", formatNumber(data.memoryPerOp));
            }
            if (data.cpuTimePerOp > 0) {
                System.out.printf("   CPU Time per Op:  %10s ns\n", formatNumber(data.cpuTimePerOp));
            }
            if (data.gcCount > 0) {
                System.out.printf("   GC Count:         %10d\n", data.gcCount);
                System.out.printf("   GC Time:          %10s ms\n", formatNumber(data.gcTime));
            }
        }
    }

    static class MetricData {
        String name;
        double avgResponseTime;
        double p50, p95, p99;
        double throughput;
        long memoryPerOp;
        long cpuTimePerOp;
        long gcCount;
        long gcTime;
        long operations;
        double totalTime;
        int threads;
    }

    static class ConcurrentTask implements Runnable {
        private long count = 0;

        @Override
        public void run() {
            for (int i = 0; i < 100; i++) {
                UserDto user = createTestUser();
                user.mask("maskMe", "01000000000");
                count++;
            }
        }

        long getCount() { return count; }
        void reset() { count = 0; }
    }
}
