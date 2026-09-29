# System Architecture — Code Review Agent

## Overview
Code Review Agent is an autonomous AI Code Review Platform built with **Java 21**, **Spring Boot 3.3.4**, **PostgreSQL**, **Groq AI LLMs**, and an autonomous **Hindsight Memory Engine**.

```
+-----------------------------------------------------------------------------------+
|                                  FRONTEND / UI                                    |
|   Vanilla JS Engineering Dashboard | SVG Visualizations | Theme Controller         |
+-----------------------------------------------------------------------------------+
                                         │  HTTP / REST API
                                         ▼
+-----------------------------------------------------------------------------------+
|                               SPRING BOOT BACKEND                                 |
| AuthController | AiReviewController | TeamController | MemoryController | Analytics  |
+-------------------+--------------------+-------------------+----------------------+
        │                   │                    │                   │
        ▼                   ▼                    ▼                   ▼
+---------------+   +---------------+   +---------------+   +-----------------------+
|  PostgreSQL   |   |   Groq AI     |   | Hindsight     |   |  EmailJS Service      |
|  Database     |   |   LLM API     |   | Memory Store  |   |  6-Digit OTP Delivery |
| (codereview)  |   | (gpt-oss-120b)|   | (JSON Engine) |   |  (5-Min Expiry)       |
+---------------+   +---------------+   +---------------+   +-----------------------+
```

## Key Architectural Layers

1. **REST Controller Layer** (`com.codereviewagent.controller`):
   - Auth & OTP endpoints (`/api/auth/*`)
   - AI Code Review endpoints (`/api/ai/*`)
   - Team & Workspace Management (`/api/team/*`)
   - Hindsight Memory Store (`/api/memory/*`)
   - Notifications (`/api/notifications/*`)

2. **Service & Engine Layer** (`com.codereviewagent.service`):
   - `GroqAiServiceImpl`: Prompts Groq API with language detection & Hindsight memory context.
   - `HindsightMemoryStore`: Manages persistent developer anti-patterns in JSON.
   - `AuthServiceImpl`: Enforces deferred user registration until OTP verification.
   - `TeamServiceImpl`: Manages team creation, 6-character Join Codes, and membership limits.

3. **Persistence Layer** (`com.codereviewagent.repository` & `entity`):
   - Spring Data JPA with PostgreSQL.
