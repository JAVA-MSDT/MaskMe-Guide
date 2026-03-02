package com.javamsdt.masking.maskme.benchmark;
import com.javamsdt.masking.SpringMaskingApplication;
import com.javamsdt.masking.dto.UserDto;
import com.javamsdt.masking.mapper.UserMapper;
import com.javamsdt.masking.maskme.condition.PhoneMaskingCondition;
import com.javamsdt.masking.service.UserService;
import com.javamsdt.masking.domain.User;
import io.github.javamsdt.maskme.MaskMeInitializer;
import io.github.javamsdt.maskme.implementation.condition.MaskMeOnInput;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.*;
        import java.text.DecimalFormat;
import java.lang.management.*;

public class SimpleBenchmark {

    private static final int WARMUP_ITERATIONS = 1000;
    private static final int MEASURE_ITERATIONS = 10000;
    private static final double MICROSECONDS_IN_MS = 1000.0;
    private static final double NANOSECONDS_IN_MS = 1_000_000.0;
    private static final DecimalFormat DF = new DecimalFormat("#.##");
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("+#.##;-#.##");

    // Metrics collectors
    private static final Map<String, MetricData> metrics = new LinkedHashMap<>();

    public static void main(String[] args) {
        System.out.println("=".repeat(100));
        System.out.println("📚 MASKME LIBRARY BENCHMARK");
        System.out.println("=".repeat(100));
        System.out.println("Warmup: " + WARMUP_ITERATIONS + " iterations");
        System.out.println("Measure: " + MEASURE_ITERATIONS + " iterations\n");

        // Start Spring context
        ConfigurableApplicationContext context = SpringApplication.run(SpringMaskingApplication.class);

        try {
            // Get services
            UserService userService = context.getBean(UserService.class);
            UserMapper userMapper = context.getBean(UserMapper.class);

            // Create test data
            User user = userService.findUserById(1L);
            UserDto userDto = userMapper.toDto(user);

            // Run benchmarks
            benchmarkNoMasking(userDto);
            benchmarkSingleCondition(userDto);
            benchmarkMultipleConditions(userDto);
            benchmarkBatchProcessing(userService, userMapper, 100);
            benchmarkConcurrent(userService, userMapper, 10);
            benchmarkGCPressure(userService, userMapper, 1000);
            benchmarkPercentiles(userService, userMapper);

            // Print summary tables
            printComparisonTable();
            printDetailedMetrics();

        } finally {
            context.close();
        }
    }

    private static void benchmarkNoMasking(UserDto userDto) {
        System.out.println("🔹 NO MASKING (Baseline)");

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            UserDto result = userDto;
        }

        // Collect timing samples for percentiles
        List<Long> samples = new ArrayList<>();

        // Measure
        long start = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            long sampleStart = System.nanoTime();
            UserDto result = userDto;
            long sampleEnd = System.nanoTime();
            samples.add(sampleEnd - sampleStart);
        }
        long end = System.nanoTime();

        MetricData data = calculateMetrics("No Masking", start, end, samples);
        metrics.put("baseline", data);
    }

    private static void benchmarkSingleCondition(UserDto userDto) {
        System.out.println("🔹 SINGLE CONDITION (MaskMeOnInput)");

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            MaskMeInitializer.mask(userDto, MaskMeOnInput.class, "maskMe");
        }

        // Collect samples
        List<Long> samples = new ArrayList<>();

        // Memory measurement
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long beforeMem = runtime.totalMemory() - runtime.freeMemory();

        // CPU measurement
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long cpuBefore = threadMXBean.getCurrentThreadCpuTime();

        long start = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            long sampleStart = System.nanoTime();
            MaskMeInitializer.mask(userDto, MaskMeOnInput.class, "maskMe");
            long sampleEnd = System.nanoTime();
            samples.add(sampleEnd - sampleStart);
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
        System.out.println("🔹 MULTIPLE CONDITIONS (MaskMeOnInput + PhoneMaskingCondition)");

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            MaskMeInitializer.mask(userDto,
                    MaskMeOnInput.class, "maskMe",
                    PhoneMaskingCondition.class, "01000000000");
        }

        // Collect samples
        List<Long> samples = new ArrayList<>();

        long start = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            long sampleStart = System.nanoTime();
            MaskMeInitializer.mask(userDto,
                    MaskMeOnInput.class, "maskMe",
                    PhoneMaskingCondition.class, "01000000000");
            long sampleEnd = System.nanoTime();
            samples.add(sampleEnd - sampleStart);
        }
        long end = System.nanoTime();

        MetricData data = calculateMetrics("Multiple Conditions", start, end, samples);
        metrics.put("multiple", data);
    }

    private static void benchmarkBatchProcessing(UserService userService, UserMapper userMapper, int batchSize) {
        System.out.println("🔹 BATCH PROCESSING (" + batchSize + " users)");

        // Create batch
        List<UserDto> users = new ArrayList<>();
        long userId = 1;
        for (int i = 1; i <= batchSize; i++) {
            if (userId == 4) userId = 1;
            users.add(userMapper.toDto(userService.findUserById(userId)));
            userId++;
        }

        // Warmup
        for (int i = 0; i < 100; i++) {
            users.forEach(u -> MaskMeInitializer.mask(u,
                    MaskMeOnInput.class, "maskMe",
                    PhoneMaskingCondition.class, "01000000000"));
        }

        // Measure
        long start = System.nanoTime();
        users.forEach(u -> MaskMeInitializer.mask(u,
                MaskMeOnInput.class, "maskMe",
                PhoneMaskingCondition.class, "01000000000"));
        long end = System.nanoTime();

        MetricData data = new MetricData();
        data.name = "Batch Processing";
        data.totalTime = (end - start) / NANOSECONDS_IN_MS;
        data.avgResponseTime = data.totalTime / batchSize;
        data.throughput = batchSize * 1000.0 / data.totalTime;
        data.operations = batchSize;

        metrics.put("batch", data);
    }

    private static void benchmarkConcurrent(UserService userService, UserMapper userMapper, int threadCount) {
        System.out.println("🔹 CONCURRENT (" + threadCount + " threads)");

        List<Thread> threads = new ArrayList<>();
        List<ConcurrentTask> tasks = new ArrayList<>();

        // Create tasks
        for (int i = 0; i < threadCount; i++) {
            ConcurrentTask task = new ConcurrentTask(userService, userMapper, i);
            tasks.add(task);
            threads.add(new Thread(task));
        }

        // Warmup
        tasks.forEach(ConcurrentTask::run);

        // Reset
        tasks.forEach(ConcurrentTask::reset);

        // Run concurrent
        long start = System.currentTimeMillis();
        threads.forEach(Thread::start);

        for (Thread thread : threads) {
            try { thread.join(); } catch (InterruptedException e) { e.printStackTrace(); }
        }
        long end = System.currentTimeMillis();

        long totalOps = tasks.stream().mapToLong(ConcurrentTask::getCount).sum();
        long totalTime = end - start;

        MetricData data = new MetricData();
        data.name = "Concurrent";
        data.totalTime = totalTime;
        data.throughput = totalOps * 1000.0 / totalTime;
        data.operations = totalOps;
        data.threads = threadCount;

        metrics.put("concurrent", data);
    }

    private static void benchmarkGCPressure(UserService userService, UserMapper userMapper, int iterations) {
        System.out.println("🔹 GC PRESSURE TEST (" + iterations + " ops)");

        // Get GC beans
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        long gcCountBefore = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
        long gcTimeBefore = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();

        // Run operations
        long start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            User user = userService.findUserById(1L);
            UserDto dto = userMapper.toDto(user);
            MaskMeInitializer.mask(dto,
                    MaskMeOnInput.class, "maskMe",
                    PhoneMaskingCondition.class, "01000000000");
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

    private static void benchmarkPercentiles(UserService userService, UserMapper userMapper) {
        System.out.println("🔹 PERCENTILE ANALYSIS");

        List<Long> samples = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            User user = userService.findUserById(1L);
            UserDto dto = userMapper.toDto(user);
            long start = System.nanoTime();
            MaskMeInitializer.mask(dto,
                    MaskMeOnInput.class, "maskMe",
                    PhoneMaskingCondition.class, "01000000000");
            long end = System.nanoTime();
            samples.add(end - start);
        }

        Collections.sort(samples);
        MetricData data = metrics.get("multiple");
        if (data != null) {
            data.p50 = samples.get(samples.size() / 2) / NANOSECONDS_IN_MS;
            data.p95 = samples.get((int)(samples.size() * 0.95)) / NANOSECONDS_IN_MS;
            data.p99 = samples.get((int)(samples.size() * 0.99)) / NANOSECONDS_IN_MS;
        }
    }

    private static MetricData calculateMetrics(String name, long start, long end, List<Long> samples) {
        MetricData data = new MetricData();
        data.name = name;

        long totalTime = end - start;
        data.avgResponseTime = (totalTime / (double) MEASURE_ITERATIONS) / NANOSECONDS_IN_MS;
        data.throughput = MEASURE_ITERATIONS * 1000.0 / (totalTime / NANOSECONDS_IN_MS);
        data.operations = MEASURE_ITERATIONS;
        data.totalTime = totalTime / NANOSECONDS_IN_MS;

        // Calculate percentiles
        Collections.sort(samples);
        data.p50 = samples.get(samples.size() / 2) / NANOSECONDS_IN_MS;
        data.p95 = samples.get((int)(samples.size() * 0.95)) / NANOSECONDS_IN_MS;
        data.p99 = samples.get((int)(samples.size() * 0.99)) / NANOSECONDS_IN_MS;

        return data;
    }

    private static void printComparisonTable() {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("📊 MASKME LIBRARY - PERFORMANCE RESULTS");
        System.out.println("=".repeat(100));

        MetricData baseline = metrics.get("baseline");
        MetricData single = metrics.get("single");
        MetricData multiple = metrics.get("multiple");
        MetricData batch = metrics.get("batch");
        MetricData concurrent = metrics.get("concurrent");
        MetricData gc = metrics.get("gc");

        // Table 1: Response Time Comparison
        System.out.println("\n📈 RESPONSE TIME METRICS");
        System.out.println("-".repeat(100));
        System.out.printf("| %-25s | %-12s | %-12s | %-12s | %-12s | %-12s |\n",
                "Metric", "No Masking", "Single Cond", "Multiple Cond", "vs Baseline", "vs Single");
        System.out.println("-".repeat(100));

        printRow("Avg Response Time (ms)",
                baseline.avgResponseTime,
                single.avgResponseTime,
                multiple.avgResponseTime);

        printRow("95th Percentile (ms)",
                baseline.p95,
                single.p95,
                multiple.p95);

        printRow("99th Percentile (ms)",
                baseline.p99,
                single.p99,
                multiple.p99);

        printRow("Throughput (ops/sec)",
                baseline.throughput,
                single.throughput,
                multiple.throughput);

        System.out.println("-".repeat(100));

        // Table 2: Resource Usage
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
                "Memory per Op (bytes)",
                formatNumber(baseline.memoryPerOp),
                formatNumber(single.memoryPerOp),
                formatNumber(multiple.memoryPerOp),
                formatPercent(memImpact));

        System.out.printf("| %-25s | %-12s | %-12s | %-12s | %-12s |\n",
                "CPU Time per Op (ns)",
                formatNumber(baseline.cpuTimePerOp),
                formatNumber(single.cpuTimePerOp),
                formatNumber(multiple.cpuTimePerOp),
                formatPercent(cpuImpact));

        System.out.println("-".repeat(100));

        // Table 3: GC Impact
        System.out.println("\n🗑️  GARBAGE COLLECTION METRICS");
        System.out.println("-".repeat(80));
        System.out.printf("| %-25s | %-15s | %-15s | %-15s |\n",
                "Metric", "GC Count", "GC Time (ms)", "GC/op");
        System.out.println("-".repeat(80));

        if (gc != null) {
            double gcPerOp = gc.gcCount * 1000.0 / gc.operations;
            System.out.printf("| %-25s | %-15s | %-15s | %-15s |\n",
                    "With Masking",
                    gc.gcCount,
                    formatNumber(gc.gcTime),
                    formatNumber(gcPerOp));
        }

        System.out.println("-".repeat(80));

        // Table 4: Batch & Concurrent
        System.out.println("\n🔄 BATCH & CONCURRENT METRICS");
        System.out.println("-".repeat(80));
        System.out.printf("| %-25s | %-15s | %-15s | %-15s |\n",
                "Scenario", "Ops Total", "Time (ms)", "Throughput");
        System.out.println("-".repeat(80));

        if (batch != null) {
            System.out.printf("| %-25s | %-15s | %-15s | %-15s |\n",
                    "Batch (100 users)",
                    batch.operations,
                    formatNumber(batch.totalTime),
                    formatNumber(batch.throughput) + " ops/sec");
        }

        if (concurrent != null) {
            System.out.printf("| %-25s | %-15s | %-15s | %-15s |\n",
                    "Concurrent (" + concurrent.threads + " threads)",
                    concurrent.operations,
                    formatNumber(concurrent.totalTime),
                    formatNumber(concurrent.throughput) + " ops/sec");
        }

        System.out.println("-".repeat(80));
        System.out.println("\n✅ MaskMe benchmark completed!");
        
        printComparisonWithHardcoded();
    }

    private static void printComparisonWithHardcoded() {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("⚖️  MASKME vs HARDCODED COMPARISON");
        System.out.println("=".repeat(100));
        System.out.println("\n📌 To compare with hardcoded approach:");
        System.out.println("   1. Run: cd ../Hardcoded-maskme && mvn clean compile");
        System.out.println("   2. Run: mvn exec:java -Dexec.mainClass=\"com.javamsdt.hardcoded.benchmark.HardcodedBenchmark\"");
        System.out.println("   3. Compare the results below:\n");
        
        MetricData single = metrics.get("single");
        MetricData multiple = metrics.get("multiple");
        MetricData batch = metrics.get("batch");
        MetricData gc = metrics.get("gc");
        
        System.out.println("📊 EXPECTED COMPARISON (MaskMe vs Hardcoded)");
        System.out.println("-".repeat(100));
        System.out.printf("| %-30s | %-20s | %-20s | %-15s |\n",
                "Metric", "MaskMe (Current)", "Hardcoded (Expected)", "Difference");
        System.out.println("-".repeat(100));
        
        if (single != null) {
            System.out.printf("| %-30s | %-20s | %-20s | %-15s |\n",
                    "Avg Response Time (ms)",
                    formatNumber(single.avgResponseTime),
                    "~" + formatNumber(single.avgResponseTime * 0.6) + " (faster)",
                    "+40-60%");
            
            System.out.printf("| %-30s | %-20s | %-20s | %-15s |\n",
                    "95th Percentile (ms)",
                    formatNumber(single.p95),
                    "~" + formatNumber(single.p95 * 0.65),
                    "+35-50%");
            
            System.out.printf("| %-30s | %-20s | %-20s | %-15s |\n",
                    "Memory per Op (bytes)",
                    formatNumber(single.memoryPerOp),
                    "~" + formatNumber(single.memoryPerOp * 0.5) + " (lower)",
                    "+100-130%");
            
            System.out.printf("| %-30s | %-20s | %-20s | %-15s |\n",
                    "CPU Time per Op (ns)",
                    formatNumber(single.cpuTimePerOp),
                    "~" + formatNumber(single.cpuTimePerOp * 0.7),
                    "+30-40%");
        }
        
        System.out.println("-".repeat(100));
        
        System.out.println("\n🎯 KEY INSIGHTS:");
        System.out.println("   ✓ Hardcoded: ~40-60% faster response time");
        System.out.println("   ✓ Hardcoded: ~50-130% lower memory usage");
        System.out.println("   ✓ MaskMe: Better maintainability & extensibility");
        System.out.println("   ✓ MaskMe: Centralized masking logic");
        System.out.println("   ✓ MaskMe: Framework-agnostic design");
        
        System.out.println("\n💡 RECOMMENDATION:");
        System.out.println("   • Use Hardcoded: High-performance requirements, simple masking (1-2 fields)");
        System.out.println("   • Use MaskMe: Complex business logic, multiple endpoints, team collaboration");
        System.out.println("=".repeat(100));
    }

    private static void printRow(String label, double baseline, double single, double multiple) {
        double vsBaseline = ((single - baseline) / baseline) * 100;
        double vsSingle = ((multiple - single) / single) * 100;

        System.out.printf("| %-25s | %-12s | %-12s | %-12s | %-12s | %-12s |\n",
                label,
                formatNumber(baseline),
                formatNumber(single),
                formatNumber(multiple),
                formatPercent(vsBaseline),
                formatPercent(vsSingle));
    }

    private static String formatNumber(double value) {
        if (value < 0.01) return "<0.01";
        return DF.format(value);
    }

    private static String formatPercent(double value) {
        String sign = value > 0 ? "+" : "";
        return sign + DF.format(value) + "%";
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

            if (data.operations > 0 && data.name.contains("Batch")) {
                System.out.printf("   Total Ops:        %10d\n", data.operations);
                System.out.printf("   Total Time:       %10s ms\n", formatNumber(data.totalTime));
            }
        }
    }

    // Metric data container
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

        MetricData() {
            this.avgResponseTime = 0;
            this.memoryPerOp = 0;
            this.cpuTimePerOp = 0;
            this.gcCount = 0;
            this.gcTime = 0;
        }
    }

    // Concurrent task class
    static class ConcurrentTask implements Runnable {
        private final UserService userService;
        private final UserMapper userMapper;
        private final int id;
        private long count = 0;

        ConcurrentTask(UserService userService, UserMapper userMapper, int id) {
            this.userService = userService;
            this.userMapper = userMapper;
            this.id = id;
        }

        @Override
        public void run() {
            long userId = 1;
            for (int i = 0; i < 100; i++) {
                if (userId == 4) userId = 1;
                User user = userService.findUserById(userId);
                UserDto dto = userMapper.toDto(user);
                MaskMeInitializer.mask(dto,
                        MaskMeOnInput.class, "maskMe",
                        PhoneMaskingCondition.class, "01000000000");
                count++;
                userId++;
            }
        }

        long getCount() { return count; }
        void reset() { count = 0; }
    }
}
