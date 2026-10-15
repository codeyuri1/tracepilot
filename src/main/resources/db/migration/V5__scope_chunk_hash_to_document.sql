-- Identical text may occur in several documents, ACL scopes, or repeated chunks.
-- The existing (document_id, chunk_index) key identifies chunks; hashes are content metadata.
ALTER TABLE knowledge_chunks DROP CONSTRAINT knowledge_chunks_content_hash_key;
