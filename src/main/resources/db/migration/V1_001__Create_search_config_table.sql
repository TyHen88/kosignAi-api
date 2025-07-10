-- Create search configuration table
CREATE TABLE IF NOT EXISTS tb_search_config (
    id BIGINT PRIMARY KEY DEFAULT 1,
    db_only BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) DEFAULT 'SYSTEM'
);

-- Insert default configuration if not exists
INSERT INTO tb_search_config (id, db_only, updated_by) 
VALUES (1, TRUE, 'SYSTEM_INIT')
ON DUPLICATE KEY UPDATE id = id;

-- Add comment to table
ALTER TABLE tb_search_config COMMENT = 'Search configuration: true=database-only, false=hybrid search'; 