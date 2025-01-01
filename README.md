# Kafka Streams 限流器

## 概述
本项目展示了一个基于 **Apache Kafka Streams**、**Redis** 和 **Spring Boot** 的限流系统。该系统确保用户在每分钟内不能超过预定义的 API 请求限制。系统利用 Kafka Streams 进行实时流量分析，并使用 Redis 存储限流数据。

---

## 功能
- **实时流量分析**：使用 Kafka Streams 实时处理和统计用户请求。
- **限流功能**：按用户和 API 密钥强制执行 API 调用限制。
- **高性能**：利用 Redis 快速访问数据，使用 Kafka 进行分布式处理。
- **Spring Boot 集成**：易于运行的带限流逻辑的 RESTful API。
- **模拟测试**：使用 Spring Boot 测试模拟高并发请求。

---

## 使用技术

1. **Java 21**
2. **Spring Boot 3.x**
3. **Apache Kafka**
4. **Kafka Streams**
5. **Redis**
6. **Maven**
7. **JUnit 5**

---

## 前置条件

### 环境准备
1. **Kafka**:
   - 下载并安装 Apache Kafka。
   - 启动 Kafka 和 Zookeeper 服务。
   - 创建所需的主题：
     ```bash
     kafka-topics.sh --create --topic api-requests --bootstrap-server localhost:9092
     kafka-topics.sh --create --topic rate-limiter-output --bootstrap-server localhost:9092
     ```

2. **Redis**:
   - 安装并启动 Redis 服务。

3. **Java**:
   - 确保已安装 Java 11 或更高版本。

4. **Maven**:
   - 确保已安装 Maven 并在 PATH 中可用。

---

## 快速开始

### 克隆仓库
```bash
git clone https://github.com/your-repo/kafka-streams-rate-limiter.git
cd kafka-streams-rate-limiter
```

### 构建项目
```bash
mvn clean install
```

### 启动应用
```bash
mvn spring-boot:run
```

---


### 限流规则
- 默认值：每用户每 API 每分钟 10,000 次请求。
- 如需修改规则，可在 `ApiController` 的逻辑中进行调整。

---

## 工作原理

1. **API 请求**：
   - 传入的 API 请求发布到 Kafka 的 `api-requests` 主题。

2. **Kafka Streams 处理**：
   - `RateLimiterStreamProcessor` 按用户和 API 密钥以 1 分钟的窗口聚合请求计数。

3. **Redis 存储**：
   - 聚合后的请求计数存储在 Redis 中，设置 1 分钟的过期时间。

4. **限流执行**：
   - `ApiController` 在处理每个 API 调用之前，检查 Redis 中的请求计数。

---

## 测试

### 运行测试
```bash
mvn test
```

### 测试描述
- 模拟高并发请求。
- 验证限流逻辑：
  - 请求在阈值以下时成功处理。
  - 超出请求限制时返回友好的限流消息。

---

## 示例 API 接口

### `GET /api/api1`
- **请求**：
  ```bash
  curl -X GET "http://localhost:8080/api/api1?userId=user1"
  ```
- **响应**（未超出限制）：
  ```json
  { "message": "Access granted for user user1 on api1" }
  ```
- **响应**（超出限制）：
  ```json
  { "message": "Rate limit exceeded for user user1 on api1" }
  ```

### `POST /api/api2`
- 与 `GET /api/api1` 结构相同。

### `PUT /api/api3`
- 与 `GET /api/api1` 结构相同。

---

## 监控

- **Kafka Streams 状态**：
  - 使用 Kafka Streams 的状态存储检查实时聚合。
- **Redis 指标**：
  - 使用 Redis CLI 查看键值对和过期时间。
  ```bash
  redis-cli
  keys rate_limiter:*
  ```


---

## 联系方式
如有问题或疑问，请联系 [cguanjie1123@gmail.com]。

