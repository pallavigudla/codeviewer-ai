# PostgreSQL Database Documentation

## Database Information
* **Database Name**: `codereview_agent`
* **Port**: `5432`
* **Dialect**: `PostgreSQL 14+`

## Setup & Initialization

1. Create PostgreSQL database:
```sql
CREATE DATABASE codereview_agent;
```

2. Run schema DDL script:
```bash
psql -U postgres -d codereview_agent -f database/schema.sql
```

3. (Optional) Populate seed data:
```bash
psql -U postgres -d codereview_agent -f database/sample_data.sql
psql -U postgres -d codereview_agent -f database/team_seed.sql
```
