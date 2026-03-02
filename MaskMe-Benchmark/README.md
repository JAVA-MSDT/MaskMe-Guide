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

- **Hardware**: amd64, 4096MB RAM
- **JVM**: 21.0.1
- **Warmup**: 1000 iterations
- **Measure**: 10000 iterations
- **Scale**: 11 fields (UserDto) + 4 fields (AddressDto)

### Results Summary

| Metric                  | Hardcoded | MaskMe | Impact |
| ----------------------- | --------- | ------ | ------ |
| **Total Response Time** | 173ms     | 207ms  | +20%   |
| **Masking Overhead**    | 0.017ms   | 0.021ms| +24%   |
| **Memory per Cycle**    | 35.5KB    | 82KB   | +131%  |
| **CPU Usage**           | 45%       | 52%    | +16%   |
| **GC Collections**      | 12        | 15     | +25%   |
| **95th Percentile**     | 0.02ms    | 0.03ms | +50%   |
```
