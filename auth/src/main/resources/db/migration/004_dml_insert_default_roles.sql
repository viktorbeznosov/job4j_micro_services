-- V4: Insert default roles
INSERT INTO roles (name) VALUES ('ROLE_USER'), ('ROLE_ADMIN'), ('ROLE_MANAGER')
ON CONFLICT (name) DO NOTHING;