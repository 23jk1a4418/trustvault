-- PostgreSQL reference schema. Spring JPA currently creates/updates these tables for local demo use.
CREATE TABLE app_users (id UUID PRIMARY KEY, username VARCHAR(255) NOT NULL UNIQUE, password_hash VARCHAR(255) NOT NULL, role VARCHAR(32) NOT NULL);
CREATE TABLE documents (id UUID PRIMARY KEY, name VARCHAR(255) NOT NULL, category VARCHAR(255) NOT NULL, owner VARCHAR(255) NOT NULL, storage_key VARCHAR(255) NOT NULL, content_type VARCHAR(255) NOT NULL, size BIGINT NOT NULL, created_at TIMESTAMPTZ NOT NULL);
CREATE TABLE document_grants (id UUID PRIMARY KEY, document_id UUID NOT NULL REFERENCES documents(id), username VARCHAR(255) NOT NULL, can_read BOOLEAN NOT NULL, can_edit BOOLEAN NOT NULL, UNIQUE(document_id, username));
CREATE TABLE audit_events (id UUID PRIMARY KEY, occurred_at TIMESTAMPTZ NOT NULL, actor VARCHAR(255) NOT NULL, action VARCHAR(255) NOT NULL, document_id UUID, details VARCHAR(1000), previous_hash CHAR(64) NOT NULL, event_hash CHAR(64) NOT NULL);
CREATE INDEX idx_grants_user ON document_grants(username);
CREATE INDEX idx_audit_time ON audit_events(occurred_at DESC);
