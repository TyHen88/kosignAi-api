
-- Create function unaccent
CREATE EXTENSION IF NOT EXISTS unaccent;

   -- Add specific table content indexing
CREATE INDEX idx_table_content_gin ON tb_ppc_bank 
USING GIN ((tables->'rows'->0->'cells'->0->>'text'));


-- Ensure the unaccent extension is available (run once per database)
CREATE EXTENSION IF NOT EXISTS unaccent;

-- 1. Add a new column to store the searchable text vector
ALTER TABLE tb_ppc_bank ADD COLUMN fts_vector tsvector;

-- 2. Populate the new column for all existing data
-- This combines title, content, and tables into a single searchable vector.
UPDATE tb_ppc_bank
SET fts_vector =
    to_tsvector('english', coalesce(title, '')) ||
    to_tsvector('english', coalesce(content, '')) ||
    to_tsvector('english', coalesce(CAST(tables AS text), ''))
WHERE fts_vector IS NULL;

-- 3. Create a GIN index for high-performance searching on the new column
CREATE INDEX fts_vector_idx ON tb_ppc_bank USING gin(fts_vector);

-- 4. Create a trigger function to automatically update the fts_vector on any change
CREATE OR REPLACE FUNCTION update_fts_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.fts_vector :=
        to_tsvector('english', coalesce(NEW.title, '')) ||
        to_tsvector('english', coalesce(NEW.content, '')) ||
        to_tsvector('english', coalesce(CAST(NEW.content_json AS text), ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 5. Assign the trigger to the table
CREATE TRIGGER fts_vector_update_trigger
BEFORE INSERT OR UPDATE ON tb_ppc_bank
FOR EACH ROW EXECUTE FUNCTION update_fts_vector();
