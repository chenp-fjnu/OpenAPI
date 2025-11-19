# 集团级API网关系统

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.0-brightgreen.svg)](https://spring.io/projects/spring-cloud)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## 项目简介

集团级API网关系统是为大型企业集团打造的统一API管理和治理平台，提供安全、可靠、高性能的API网关服务。

## 核心功能

### 🔐 认证授权
- **多协议支持**: OAuth2.0、JWT、API Key、AK/SK等多种认证方式
- **RBAC权限控制**: 基于角色的访问控制，支持细粒度权限管理
- **多租户隔离**: 支持多租户架构，数据安全隔离

### 🚦 流量治理
- **智能限流**: 基于接口、IP、用户等维度的多维度限流
- **熔断降级**: 基于失败率、响应时间的熔断机制
- **负载均衡**: 支持多种负载均衡算法和权重分配

### 📊 监控告警
- **实时监控**: API调用量、响应时间、错误率等核心指标
- **链路追踪**: 集成SkyWalking，支持全链路监控
- **告警通知**: 支持多渠道告警（短信、邮件、钉钉等）

### 🛡️ 安全防护
- **WAF防护**: 防范SQL注入、XSS、CSRF等常见攻击
- **数据加密**: 支持端到端加密和敏感数据脱敏
- **合规审计**: 完整的操作日志和调用记录

## 技术架构

### 后端技术栈
- **框架**: Spring Boot 3.2.0 + Spring Cloud 2023.0.0
- **网关**: Spring Cloud Gateway
- **认证**: Spring Security OAuth2 + JWT
- **数据库**: MySQL 8.0 + MyBatis-Plus
- **缓存**: Redis 6.0
- **消息队列**: RocketMQ / Kafka
- **监控**: Micrometer + Prometheus + Grafana
- **链路追踪**: SkyWalking

### 前端技术栈
- **框架**: Vue 3 + TypeScript
- **构建工具**: Vite
- **UI组件**: Element Plus
- **状态管理**: Pinia
- **图表库**: ECharts

### 运维支持
- **容器化**: Docker + Kubernetes
- **配置中心**: Nacos
- **服务发现**: Nacos
- **日志收集**: ELK Stack
- **CI/CD**: Jenkins + GitLab CI

## 项目结构

```
gateway-api-system/
├── gateway-common/          # 公共模块
├── gateway-core/            # 核心网关模块
├── gateway-auth/            # 认证授权模块
├── gateway-routing/         # 路由管理模块
├── gateway-ratelimit/       # 限流模块
├── gateway-monitor/         # 监控模块
├── gateway-admin/           # 管理控制台后端
├── gateway-web/             # 管理控制台前端
└── docs/                    # 项目文档
```

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+
- Nacos 2.3+

### 编译运行
```bash
# 克隆项目
git clone https://github.com/chenp-fjnu/gateway-api-system.git

# 编译项目
mvn clean install

# 启动服务（示例）
cd gateway-auth
mvn spring-boot:run
```

## 开发进度

### ✅ 已完成
- [x] 项目架构设计
- [x] gateway-auth认证模块架构
- [x] 核心实体类和Repository层
- [x] DTO数据传输对象
- [x] 基础控制器和服务层

### 🔄 开发中
- [ ] 完善服务层业务逻辑实现
- [ ] 数据库配置和实体映射
- [ ] JWT认证流程完善

### 📋 计划中
- [ ] gateway-core核心模块
- [ ] gateway-routing路由模块
- [ ] gateway-ratelimit限流模块
- [ ] gateway-monitor监控模块
- [ ] 前端管理界面

## 文档链接

- [📋 需求说明书](./集团级API网关建设需求说明书.md)
- [🏗️ 系统架构设计](./系统架构设计.md)
- [📊 需求分析报告](./需求分析报告.md)
- [📅 项目总体规划](./项目总体规划.md)
- [📝 开发日志](./operation_log.md)

## 贡献指南

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交修改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 许可证

本项目采用 Apache 2.0 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 联系方式

- 开发者: 贾维斯 (Jarvis)
- 项目地址: [https://github.com/chenp-fjnu/gateway-api-system](https://github.com/chenp-fjnu/gateway-api-system)

---

**让每个API调用都安全可靠 🚀**