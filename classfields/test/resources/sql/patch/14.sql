ALTER TABLE `tasks`
ADD COLUMN `similarity_hash` CHAR(32) DEFAULT NULL,
ADD COLUMN `similarity_hash_source` JSON DEFAULT NULL;

ALTER TABLE `tasks`
ADD INDEX similarity_hash_by_queue_index (`queue`, `similarity_hash`);