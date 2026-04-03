# MaskMe Benchmark Project

Isolated benchmark comparing MaskMe library vs hardcoded masking.

## Run Benchmark

```bash
cd MaskMe-Benchmark
mvn clean install
mvn exec:java
```

## Output

```
### Test Environment

- **Hardware**: aarch64, 6144MB RAM
- **JVM**: 21.0.9
- **Warmup**: 10,000 iterations
- **Measure**: 100,000 iterations
- **Scale**: 11 fields (UserDto) + 4 fields (AddressDto)

### Single Condition Results

| Metric              | Hardcoded | MaskMe   | Impact    |
| ------------------- | --------- | -------- | --------- |
| **Avg Time**        | 0.0008ms  | 0.0806ms | +9,767%   |
| **95th Percentile** | 0.0016ms  | 0.0917ms | +5,544%   |
| **Memory per Op**   | 0.73 KB   | 0.30 KB  | -59%      |
| **CPU Usage**       | 99.1%     | 98.9%    | ~0%       |
| **GC Collections**  | 4         | 70       | +1,650%   |

### Multiple Conditions Results

| Metric              | Hardcoded | MaskMe   | Impact    |
| ------------------- | --------- | -------- | --------- |
| **Avg Time**        | 0.0004ms  | 0.0812ms | +18,891%  |
| **95th Percentile** | 0.0008ms  | 0.0903ms | +11,944%  |
| **Memory per Op**   | 0.51 KB   | 0.26 KB  | -49%      |
| **CPU Usage**       | 95.0%     | 98.9%    | +4%       |
| **GC Collections**  | 4         | 71       | +1,675%   |

### Real-World Context

MaskMe per-field cost: ~0.007ms/field — competitive with Jackson (~0.01-0.05ms/field).
In a typical 100ms web request, MaskMe adds 0.08ms (< 0.1% of total time).
```
