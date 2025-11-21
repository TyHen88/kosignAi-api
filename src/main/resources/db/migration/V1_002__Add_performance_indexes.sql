-- Performance optimization indexes
-- This migration adds indexes to improve query performance

-- Indexes for tb_ppc_bank table
CREATE INDEX IF NOT EXISTS idx_ppc_bank_status ON tb_ppc_bank(status);
CREATE INDEX IF NOT EXISTS idx_ppc_bank_status_updated ON tb_ppc_bank(status, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_ppc_bank_title ON tb_ppc_bank(title);
CREATE INDEX IF NOT EXISTS idx_ppc_bank_url ON tb_ppc_bank(url);

-- GIN index for JSONB content_json searches (if not exists)
CREATE INDEX IF NOT EXISTS idx_ppc_bank_content_json_gin ON tb_ppc_bank USING GIN(content_json);

-- Indexes for tb_workflow table
CREATE INDEX IF NOT EXISTS idx_workflow_status ON tb_workflow(sts);
CREATE INDEX IF NOT EXISTS idx_workflow_status_created ON tb_workflow(sts, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_workflow_title ON tb_workflow(title);

-- GIN index for JSONB metadata searches
CREATE INDEX IF NOT EXISTS idx_workflow_metadata_gin ON tb_workflow USING GIN(metadata);

-- Indexes for tb_category table
CREATE INDEX IF NOT EXISTS idx_category_status ON tb_category(sts);
CREATE INDEX IF NOT EXISTS idx_category_workflow_id ON tb_category(workflow_id);
CREATE INDEX IF NOT EXISTS idx_category_name ON tb_category(name);

-- Composite index for category-workflow joins
CREATE INDEX IF NOT EXISTS idx_category_status_workflow ON tb_category(sts, workflow_id);

-- Indexes for tb_user table
CREATE INDEX IF NOT EXISTS idx_user_status ON tb_user(sts);
CREATE INDEX IF NOT EXISTS idx_user_role ON tb_user(role);
CREATE INDEX IF NOT EXISTS idx_user_email ON tb_user(email);
CREATE INDEX IF NOT EXISTS idx_user_username ON tb_user(username);

-- Full-text search index for title and content (if not exists)
-- This uses the existing fts_vector column
CREATE INDEX IF NOT EXISTS idx_ppc_bank_fts_title_content ON tb_ppc_bank USING GIN(
    to_tsvector('english', coalesce(title, '') || ' ' || coalesce(content, ''))
);

-- Analyze tables to update statistics
ANALYZE tb_ppc_bank;
ANALYZE tb_workflow;
ANALYZE tb_category;
ANALYZE tb_user;

