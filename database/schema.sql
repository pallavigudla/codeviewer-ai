-- Code Review Agent - PostgreSQL Database Schema (Individual Developer Model)

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(20) NOT NULL DEFAULT 'DEVELOPER',
    is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS email_verifications (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(20),
    otp_code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS email_otps (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS projects (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    repository_url VARCHAR(500),
    default_branch VARCHAR(100) DEFAULT 'main',
    description TEXT,
    owner_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS code_submissions (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id),
    author_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    commit_hash VARCHAR(100),
    branch_name VARCHAR(100),
    pr_url VARCHAR(500),
    language VARCHAR(50) DEFAULT 'Java',
    diff_content TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS reviews (
    id UUID PRIMARY KEY,
    submission_id UUID NOT NULL REFERENCES code_submissions(id),
    reviewer_id UUID NOT NULL REFERENCES users(id),
    is_ai_generated BOOLEAN NOT NULL DEFAULT TRUE,
    summary TEXT,
    feedback_comments TEXT,
    verdict VARCHAR(30) NOT NULL DEFAULT 'APPROVED',
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS review_metrics (
    id UUID PRIMARY KEY,
    review_id UUID NOT NULL REFERENCES reviews(id),
    code_quality_score DOUBLE PRECISION NOT NULL,
    security_score DOUBLE PRECISION NOT NULL,
    performance_score DOUBLE PRECISION NOT NULL,
    total_issues_found INT NOT NULL DEFAULT 0,
    critical_issues INT NOT NULL DEFAULT 0,
    warning_issues INT NOT NULL DEFAULT 0,
    info_issues INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);
