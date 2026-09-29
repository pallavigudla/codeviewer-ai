# Code Review Agent — Enterprise AI Code Review & Hindsight Memory Platform

An autonomous, production-ready AI Code Review Platform powered by **Java 21**, **Spring Boot 3.3.4**, **PostgreSQL**, **Groq AI (`llama-3.3-70b-versatile`)**, **Hindsight Memory Engine**, and **EmailJS OTP Authentication**.

---

## 🏆 Hackathon Overview & Highlights

Code Review Agent solves the core problem of static code analyzers: **lack of developer context & recurring team mistakes**. By pairing Groq LLMs with an autonomous **Hindsight Memory Engine**, the platform remembers developer-specific anti-patterns and team coding standards, tailoring every code review to the developer's historical growth.

```
+-----------------------------------------------------------------------------------+
|                                  USER INTERFACE                                   |
|      Vanilla JS Engineering Dashboard | Dark & Light Modes | Notification Bell    |
+-----------------------------------------------------------------------------------+
                                         │  HTTP / REST APIs
                                         ▼
+-----------------------------------------------------------------------------------+
|                               SPRING BOOT BACKEND                                 |
|  AuthController | ReviewController | TeamController | MemoryController | Analytics    |
+-------------------+--------------------+-------------------+----------------------+
        │                   │                    │                   │
        ▼                   ▼                    ▼                   ▼
+---------------+   +---------------+   +---------------+   +-----------------------+
|  PostgreSQL   |   |   Groq AI     |   | Hindsight     |   |  EmailJS Service      |
|  Database     |   |   LLM API     |   | Memory Store  |   |  6-Digit OTP Delivery |
| (codereview)  |   | (llama-3.3)   |   | (JSON Engine) |   |  (5-Min Expiry)       |
+---------------+   +---------------+   +---------------+   +-----------------------+
```

---

## 🚀 Key Features

* **Multi-Language AI Code Review**: Auto-detects and reviews **Java**, **JavaScript**, **HTML**, **CSS**, **Python**, **C++**, and **SQL**.
* **File Upload Validation**: Supports strictly `.java`, `.js`, `.html`, `.css`, `.py`, `.cpp`, `.sql` up to **2MB** with upload progress indicators.
* **8 Structured Review Categories**:
  1. **Bugs** (Logic bugs, unhandled exceptions)
  2. **Security** (SQL injection, hardcoded secrets, cryptographic flaws)
  3. **Performance** (Query efficiency, loop allocations)
  4. **Readability** (Documentation, clarity)
  5. **Naming** (Naming conventions, variable intent)
  6. **Best Practices** (Idiomatic patterns, clean code)
  7. **Complexity** (Method length, cyclomatic complexity)
  8. **Overall Score** (0-10 rating & Verdict badges: `APPROVED`, `CHANGES_REQUESTED`, `COMMENTED`)
* **Hindsight Memory Engine**: Stores historical anti-patterns and team standards in a dedicated, non-PostgreSQL persistent JSON engine (`hindsight_memory_store.json`).
* **Vertical Color-Coded Memory Timeline**:
  * 🟢 **Green**: Improvement
  * 🔴 **Red**: Repeated Mistake
  * 🟣 **Purple**: Team Standard
  * 🔵 **Blue**: New Memory
* **Collaborative Team Workspace**: Team Leads create workspaces, generate **6-character Join Codes** (e.g. `CRA472`), set member limits, and manage team members.
* **Improvement Engine & Vanilla JS SVG Charts**: Renders chart-ready JSON for Weekly Reviews, Monthly Improvement, Most Common Mistakes, Language Usage, Memory Growth, and Team Activity.
* **Authentication & EmailJS OTP**: BCrypt password hashing, 6-digit OTP verification with 5-minute expiry, and Remember Me session support.

---

## 🏗️ Folder Structure

```
codeviewer-ai/
├── README.md
└── backend/
    ├── .env.example
    ├── pom.xml
    ├── hindsight_memory_store.json
    ├── src/main/java/com/codereviewagent/
    │   ├── ai/            # Language detection & Groq AI client
    │   ├── config/        # Database, Security, CORS & DataInitializer seeder
    │   ├── controller/    # Auth, Dashboard, Review, Team, Memory & Analytics APIs
    │   ├── dto/           # Request & Response Data Transfer Objects
    │   ├── entity/        # PostgreSQL JPA Entities & Enums
    │   ├── exception/     # Global REST exception advice & custom handlers
    │   ├── memory/        # Hindsight Memory persistent file store
    │   ├── repository/    # Spring Data JPA Repository interfaces
    │   └── service/       # Business logic & AI integration services
    ├── src/main/resources/
    │   ├── application.properties
    │   └── static/        # Frontend HTML, CSS, and Vanilla JS assets
    └── src/test/java/com/codereviewagent/  # Unit & Integration Tests
```

---

## 📋 Prerequisites

* **Java 21 LTS**
* **Maven 3.8+**
* **PostgreSQL 14+** (Database name: `codereview_agent`)

---

## ⚙️ Environment Configuration

Copy `backend/.env.example` to `backend/.env` or export environment variables:

```env
# Server Configuration
SERVER_PORT=8080

# PostgreSQL Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=codereview_agent
DB_USERNAME=postgres
DB_PASSWORD=postgres
DB_URL=jdbc:postgresql://localhost:5432/codereview_agent

# EmailJS OTP Configuration
EMAILJS_SERVICE_ID=your_emailjs_service_id
EMAILJS_TEMPLATE_ID=your_emailjs_template_id
EMAILJS_PUBLIC_KEY=your_emailjs_public_key

# Groq AI LLM Configuration
GROQ_API_KEY=your_groq_api_key
GROQ_MODEL=llama-3.3-70b-versatile
GROQ_TIMEOUT_MS=10000
```

> **Fallback Mode**: If `GROQ_API_KEY` or EmailJS keys are omitted, the application uses built-in static analysis fallbacks and logging so that all APIs run without errors.

---

## 🚀 Running the Application

### 1. Create PostgreSQL Database
```sql
CREATE DATABASE codereview_agent;
```

### 2. Launch Backend
```bash
cd backend
mvn clean spring-boot:run
```

### 3. Access Web Interface
Open your browser to:
```
http://localhost:8080/
```

---

## 👥 Hackathon Pre-Configured Demo Accounts

On initial launch, `DataInitializer` automatically seeds realistic demo records:

| Developer | Email | Password | Role |
| :--- | :--- | :--- | :--- |
| **Pallavi Siri** | `pallavi@codereviewagent.com` | `password123` | TEAM_LEAD |
| **Rahul Sharma** | `rahul@codereviewagent.com` | `password123` | DEVELOPER |
| **Ananya Reddy** | `ananya@codereviewagent.com` | `password123` | DEVELOPER |
| **Karthik Varma** | `karthik@codereviewagent.com` | `password123` | DEVELOPER |
| **Sneha Patel** | `sneha@codereviewagent.com` | `password123` | DEVELOPER |
| **Arjun Kumar** | `arjun@codereviewagent.com` | `password123` | DEVELOPER |

* **Demo Join Code**: `CRA472`
* **Demo Team Code**: `TEAM-4KD9P1`
* **Demo Projects**: `SkillBridge AI`, `Inventory API`, `Payment Gateway`, `College Search Portal`

---

## 🧪 Running Tests

Execute the unit and integration test suite:

```bash
cd backend
mvn test
```

Includes test coverage for:
* `AuthControllerTest`: User registration & login validation
* `TeamControllerTest`: Workspace creation & join logic
* `ReviewControllerTest`: Code review generation
* `MemoryServiceTest`: Hindsight memory persistence
* `UserRepositoryTest`: Data JPA mapping

---

## 📄 License

MIT License. Developed for automated AI code review and engineering excellence.
