# Setup & Execution Guide

## Prerequisites
* **Java 21 LTS**
* **Maven 3.8+**
* **PostgreSQL 14+**

## Quick Start

1. **Configure Environment Variables**:
   Update `backend/.env` with your PostgreSQL database password, Groq AI API key, and EmailJS keys.

2. **Start Backend Application**:
   ```bash
   cd backend
   mvn spring-boot:run
   ```

3. **Open Application**:
   Navigate to [http://localhost:8080/](http://localhost:8080/) in your web browser.
