package com.javamsdt.masking.controller;

import com.javamsdt.masking.domain.User;
import com.javamsdt.masking.dto.UserDto;
import com.javamsdt.masking.mapper.UserMapper;
import com.javamsdt.masking.maskme.condition.PhoneMaskingCondition;
import com.javamsdt.masking.service.UserService;
import io.github.javamsdt.maskme.MaskMeInitializer;
import io.github.javamsdt.maskme.implementation.condition.MaskMeOnInput;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/benchmark")
@RequiredArgsConstructor
public class BenchmarkController {

    private final UserService userService;
    private final UserMapper userMapper;

    private static final double NS_TO_MS = 1_000_000.0;

    @GetMapping
    public Map<String, Object> runBenchmark(
            @RequestParam(defaultValue = "5000") int warmup,
            @RequestParam(defaultValue = "50000") int iterations) {

        // Cap iterations to prevent abuse
        warmup = Math.min(warmup, 20000);
        iterations = Math.min(iterations, 200000);

        User user = userService.findUserById(1L);
        UserDto userDto = userMapper.toDto(user);

        // Hardcoded DTO
        com.javamsdt.hardcoded.dto.AddressDto hAddr = new com.javamsdt.hardcoded.dto.AddressDto(
                1L, "first Street", "City One", "Zip One");
        com.javamsdt.hardcoded.dto.UserDto hUser = new com.javamsdt.hardcoded.dto.UserDto(
                1L, "Ahmed Samy", "one@mail.com", "123456", "01000000000",
                hAddr, LocalDate.of(1985, 1, 25), "M", "Male",
                new BigDecimal("20.0"), Instant.now());

        // Run benchmarks
        Map<String, Object> hardcodedSingle = benchmarkSingle(() -> hUser.mask("maskMe", null), warmup, iterations);
        Map<String, Object> hardcodedMultiple = benchmarkSingle(() -> hUser.mask("maskMe", "01000000000"), warmup, iterations);

        Map<String, Object> maskmeSingle = benchmarkSingle(
                () -> MaskMeInitializer.mask(userDto, MaskMeOnInput.class, "maskMe"), warmup, iterations);
        Map<String, Object> maskmeMultiple = benchmarkSingle(
                () -> MaskMeInitializer.mask(userDto, MaskMeOnInput.class, "maskMe",
                        PhoneMaskingCondition.class, "01000000000"), warmup, iterations);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("environment", buildEnvironment(warmup, iterations));
        result.put("hardcoded", Map.of("single", hardcodedSingle, "multiple", hardcodedMultiple));
        result.put("maskme", Map.of("single", maskmeSingle, "multiple", maskmeMultiple));
        return result;
    }

    private Map<String, Object> benchmarkSingle(Runnable task, int warmup, int iterations) {
        for (int i = 0; i < warmup; i++) task.run();
        System.gc();
        try { Thread.sleep(50); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }

        System.gc();
        Runtime runtime = Runtime.getRuntime();
        long memBefore = runtime.totalMemory() - runtime.freeMemory();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long cpuBefore = threadMXBean.getCurrentThreadCpuTime();
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        long gcBefore = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();

        List<Long> samples = new ArrayList<>(iterations);
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            long s = System.nanoTime();
            task.run();
            samples.add(System.nanoTime() - s);
        }
        long end = System.nanoTime();

        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        long cpuAfter = threadMXBean.getCurrentThreadCpuTime();
        long gcAfter = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();

        Collections.sort(samples);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("avgTimeMs", round((end - start) / NS_TO_MS / iterations));
        m.put("p95Ms", round(samples.get((int) (samples.size() * 0.95)) / NS_TO_MS));
        m.put("p99Ms", round(samples.get((int) (samples.size() * 0.99)) / NS_TO_MS));
        m.put("memoryPerOpBytes", Math.max(0, (memAfter - memBefore) / iterations));
        m.put("cpuPercent", round(((cpuAfter - cpuBefore) / NS_TO_MS) / ((end - start) / NS_TO_MS) * 100));
        m.put("gcCollections", gcAfter - gcBefore);
        m.put("throughput", round(iterations * 1000.0 / ((end - start) / NS_TO_MS)));
        return m;
    }

    private Map<String, Object> buildEnvironment(int warmup, int iterations) {
        Map<String, Object> env = new LinkedHashMap<>();
        env.put("arch", System.getProperty("os.arch"));
        env.put("jvm", System.getProperty("java.version"));
        env.put("maxMemoryMB", Runtime.getRuntime().maxMemory() / 1024 / 1024);
        env.put("warmup", warmup);
        env.put("iterations", iterations);
        env.put("scale", "11 fields (UserDto) + 4 fields (AddressDto)");
        return env;
    }

    private static double round(double val) {
        return Math.round(val * 10000.0) / 10000.0;
    }
}
