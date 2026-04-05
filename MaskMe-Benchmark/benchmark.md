- I have the below benchmark have been made for a running project that has a hardcoded mnasking and maskme library.
- I have also made a project that uses maskme library and compares the results.
- In the Spring project there is a benchmark class that runs the tests and prints the results.
- Can you check that the class i have really doing a right benchmark?
- I would like that the benchmark to have a similar benchmark structure as the below.
- if you would like to make your own benchmark implementation, please do not hesitate to do it as following
    - Create a new project with row data masking using a hardcoded approach, then run there the benchmark and then
      compare it with the spring one here
    - avoid changing the spring project.
    - you can only refactor the benchmark class.

## Test results between Hardcoded and MaskMe in external project and MaskMe-Benchmark project

## External Project.

### Test Environment

- **Hardware**: MacBook Pro M1, 16GB RAM
- **JVM**: OpenJDK 17, -Xmx4g
- **Database**: PostgreSQL 14 (local)
- **Load**: 100 concurrent requests, 1000 total requests
- **Scale**: 6+ fields across 1 endpoint

### Results Summary

| Metric             | Hardcoded | MaskMe | Difference |
|--------------------|-----------|--------|------------|
| Avg Response Time  | 18ms      | 24ms   | +33%       |
| 95th Percentile    | 35ms      | 48ms   | +37%       |
| Memory per Request | 3KB       | 7KB    | +133%      |
| CPU Usage          | 12%       | 16%    | +33%       |
| GC Pressure        | Low       | Medium | +50%       |

### B.1 Load Test Configuration

- **Environment**: Production-like (16GB RAM, 8 cores)
- **Load Pattern**: 500 concurrent users, 5000 requests total
- **Test Duration**: 10 minutes
- **Endpoints**: All 6 masking endpoints
- **Scale**: 68+ fields across 6 endpoint

### B.2 Projected Results Summary

| Metric                  | Current (Hardcoded) | Projected (MaskMe) | Impact |
|-------------------------|---------------------|--------------------|--------|
| **Total Response Time** | 173ms               | 207ms              | +20%   |
| **Masking Overhead**    | 24.5ms              | 58ms               | +137%  |
| **Memory per Cycle**    | 35.5KB              | 82KB               | +131%  |
| **CPU Usage**           | 45%                 | 52%                | +16%   |
| **GC Collections/min**  | 12                  | 15                 | +25%   |
| **Error Rate**          | 0.1%                | 0.15%              | +50%   |

## MaskMe-Benchmark Project.

### Test Environment

- **Hardware**: amd64, 4014MB RAM
- **JVM**: 21.0.10
- **Scale**: 11 fields (UserDto) + 4 fields (AddressDto)

### Single Condition

| Metric          | Hardcoded | MaskMe | Impact    |
|-----------------|-----------|--------|-----------|
| Avg Time        | 0ms       | 0.13ms | ~3835.38% |
| 95th Percentile | 0.01ms    | 0.19ms | ~2723.53% |
| Memory per Op   | 1.39KB    | 1.54KB | ~11.33%   |
| CPU Usage       | 86.73%    | 98.19% | ~13.22%   |
| GC Collections  | 4         | 40     | ~900%     |

### Multiple Conditions

| Metric          | Hardcoded | MaskMe | Impact    |
|-----------------|-----------|--------|-----------|
| Avg Time        | 0ms       | 0.13ms | ~5859.03% |
| 95th Percentile | 0ms       | 0.19ms | ~4954.05% |
| Memory per Op   | 0.96KB    | 0.38KB | -60.45%   |
| CPU Usage       | 72.86%    | 97.57% | ~33.91%   |
| GC Collections  | 4         | 40     | ~900%     |

### Batch Processing (100 users)

| Metric     | Hardcoded      | MaskMe       | Impact    |
|------------|----------------|--------------|-----------|
| Avg Time   | 0ms            | 0.13ms       | ~2999.25% |
| Throughput | 235294.12ops/s | 7591.98ops/s | -96.77%   |

### Concurrent (10 threads)

| Metric     | Hardcoded      | MaskMe     | Impact   |
|------------|----------------|------------|----------|
| Total Time | 7ms            | 40ms       | ~471.43% |
| Throughput | 142857.14ops/s | 25000ops/s | -82.5%   |

====================================================================================================




