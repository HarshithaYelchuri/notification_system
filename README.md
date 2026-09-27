# Production-Quality Scalable Notification Service Platform

A distributed, enterprise-grade **Notification Service Platform** built from scratch using **Java 21**, **Spring Boot 3.3.4**, **Apache Kafka (KRaft mode)**, **MongoDB**, **Redis**, **Docker Compose**, **Micrometer/Prometheus**, and **Grafana**.

---

## 1. High-Level Architecture

```
                               ┌──────────────────┐
                               │   Client Layer   │
                               └────────┬─────────┘
                                        │ HTTP REST + X-Tenant-Id + X-Correlation-Id
                                        ▼
                          ┌───────────────────────────┐
                          │     Notification API      │
                          │   /api/notifications      │
                          │     /api/templates        │
                          └─────────────┬─────────────┘
                                        │ Publish Ingestion Event
                                        ▼
                            Kafka: notification.ingest
                                        │
                                        ▼
                          ┌───────────────────────────┐
                          │   Notification Processor  │
                          │   Enrichment & Rendering  │
                          └─────────────┬─────────────┘
                                        │ Publish Enriched Event
                                        ▼
                           Kafka: notification.enriched
                                        │
                      ┌─────────────────┼─────────────────┐
                      ▼                 ▼                 ▼
               ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
               │    Email    │   │     SMS     │   │   Webhook   │
               │   Channel   │   │   Channel   │   │   Channel   │
               └──────┬──────┘   └──────┬──────┘   └──────┬──────┘
                      │                 │                 │
                      └─────────────────┼─────────────────┘
                                        │ Status / Retry / DLQ
                                        ▼
             ┌─────────────────────────────────────────────────────┐
             │ Kafka: notification.audit | retry | dlq            │
             └──────────────────────────┬──────────────────────────┘
                                        │
                                        ▼
                             ┌─────────────────────┐
                             │ MongoDB persistent  │
                             │ Audit & Status Logs │
                             └─────────────────────┘
```

---

## 2. Infrastructure Components

- **MongoDB (v7.0)**: Persistent store for templates, notifications, and immutable audit logs.
- **Redis (v7.2)**: High-speed cache for notification templates with tenant-isolated keys and automatic eviction/TTL.
- **Apache Kafka (v3.7 / KRaft)**: Event-driven messaging with partitioned topics (`ingest`, `enriched`, `audit`, `retry`, `dlq`).
- **Prometheus & Grafana**: Operational monitoring with real-time dashboards tracking processing latency, throughput, retries, and DLQ counts.

---

## 3. Key Design Decisions & Modernizations

1. **Java 21 LTS & Spring Boot 3.3.4**: Modern code standards using Records, Sealed Interfaces, Virtual Threads, and Jakarta Validation.
2. **Kafka KRaft Mode**: Removed obsolete ZooKeeper dependencies for streamlined local docker deployment.
3. **Multi-Tenancy Context**: Mandatory `X-Tenant-Id` header propagated across HTTP -> MongoDB -> Redis -> Kafka Headers -> Processor -> Channel -> Audit.
4. **Resilient Retry & Backoff**: Transient errors trigger exponential backoff retries via `notification.retry` topic. Unrecoverable validation errors bypass retries directly to `notification.dlq`.
5. **Request Tracing (MDC)**: Automated injection and cleanup of `X-Correlation-Id` across synchronous HTTP filters and asynchronous Kafka consumers.

---

## 4. Quickstart Guide

### Prerequisites
- Java 21 JDK
- Docker & Docker Compose

### Step 1: Start Supporting Infrastructure
```bash
docker compose up -d
```

### Step 2: Build & Test the Service
```bash
# Windows
.\gradlew.bat test build

# Linux/macOS
./gradlew test build
```

### Step 3: Run the Application
```bash
.\gradlew.bat bootRun
```

---

## 5. API Reference & cURL Examples

### A. Template Management

#### 1. Create Template
`POST /api/templates`
```bash
curl -X POST http://localhost:8080/api/templates \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-123" \
  -d '{
    "name": "ORDER_CONFIRMATION",
    "messageTemplate": "Hello {{name}}, your order {{orderId}} has been shipped!",
    "variables": ["name", "orderId"]
  }'
```

#### 2. Get Template by ID
`GET /api/templates/{id}`
```bash
curl -X GET http://localhost:8080/api/templates/66f4e1a8b92d4f1234567890 \
  -H "X-Tenant-Id: tenant-123"
```

#### 3. Update Template
`PATCH /api/templates/{id}`
```bash
curl -X PATCH http://localhost:8080/api/templates/66f4e1a8b92d4f1234567890 \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-123" \
  -d '{
    "messageTemplate": "Hi {{name}}, order {{orderId}} is out for delivery!",
    "variables": ["name", "orderId"]
  }'
```

#### 4. Search Templates with Pagination
`GET /api/templates?name=ORDER&page=0&size=10`
```bash
curl -X GET "http://localhost:8080/api/templates?name=ORDER&page=0&size=10" \
  -H "X-Tenant-Id: tenant-123"
```

---

### B. Send Notification API

#### 1. Dispatch Email Notification
`POST /api/notifications`
```bash
curl -X POST http://localhost:8080/api/notifications \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-123" \
  -H "X-Correlation-Id: corr-abc-999" \
  -d '{
    "templateId": "66f4e1a8b92d4f1234567890",
    "notificationType": "EMAIL",
    "recipient": "customer@example.com",
    "dynamicVariables": {
      "name": "Harshi",
      "orderId": "ORD-88219"
    }
  }'
```

#### 2. Query Notification Status
`GET /api/notifications/{id}`
```bash
curl -X GET http://localhost:8080/api/notifications/notif-66f4e1a8b92d4f \
  -H "X-Tenant-Id: tenant-123"
```

---

## 6. Observability Dashboards & Metrics

- **Actuator Health Endpoint**: `http://localhost:8080/actuator/health`
- **Prometheus Metrics Stream**: `http://localhost:8080/actuator/prometheus`
- **Prometheus UI**: `http://localhost:9090`
- **Grafana Dashboard**: `http://localhost:3000` (User: `admin`, Password: `admin`)
