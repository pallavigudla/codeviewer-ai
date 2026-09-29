-- Code Review Agent - Sample Developer & Review Seed Data
INSERT INTO users (id, email, username, password_hash, full_name, role, is_email_verified, created_at, updated_at)
VALUES 
('ffa5eb0b-eaa0-4898-8b61-043718ce1dd8', 'pallavi@codereviewagent.com', 'pallavi_siri', '$2a$10$e8w.b0nL5iG0Q9c4gV2LleQ0.H/B0kYv.kG.Y1f.6Z/w.', 'Pallavi Siri', 'DEVELOPER', TRUE, NOW(), NOW()),
('8d0cc17a-6d97-412b-bfc3-dd250bfa61b0', 'rahul@codereviewagent.com', 'rahul_s', '$2a$10$e8w.b0nL5iG0Q9c4gV2LleQ0.H/B0kYv.kG.Y1f.6Z/w.', 'Rahul Sharma', 'DEVELOPER', TRUE, NOW(), NOW()),
('54ff9be2-8d32-47ca-92b8-cbd5c1119dae', 'ananya@codereviewagent.com', 'ananya_r', '$2a$10$e8w.b0nL5iG0Q9c4gV2LleQ0.H/B0kYv.kG.Y1f.6Z/w.', 'Ananya Reddy', 'DEVELOPER', TRUE, NOW(), NOW());
