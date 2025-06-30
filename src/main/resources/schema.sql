-- Create pages table
CREATE TABLE IF NOT EXISTS pages (
    id BIGSERIAL PRIMARY KEY,
    url VARCHAR(2048) NOT NULL UNIQUE,
    title TEXT,
    content TEXT,
    content_hash VARCHAR(32),
    status INTEGER,
    depth INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create broken_links table
CREATE TABLE IF NOT EXISTS broken_links (
    id BIGSERIAL PRIMARY KEY,
    url VARCHAR(2048) NOT NULL,
    error TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create page_versions table
CREATE TABLE IF NOT EXISTS page_versions (
    id BIGSERIAL PRIMARY KEY,
    page_id BIGINT REFERENCES pages(id),
    url VARCHAR(2048) NOT NULL,
    title TEXT,
    content TEXT,
    content_hash VARCHAR(32),
    status INTEGER,
    depth INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_pages_url ON pages(url);
CREATE INDEX IF NOT EXISTS idx_pages_content_hash ON pages(content_hash);
CREATE INDEX IF NOT EXISTS idx_broken_links_url ON broken_links(url);
CREATE INDEX IF NOT EXISTS idx_page_versions_page_id ON page_versions(page_id);

-- Create function unaccent
CREATE EXTENSION IF NOT EXISTS unaccent;
