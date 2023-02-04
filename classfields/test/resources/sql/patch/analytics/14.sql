ALTER TABLE `all_tasks`
ADD COLUMN `similarity_hash` CHAR(32) DEFAULT NULL,
ADD COLUMN `similarity_hash_source` JSON DEFAULT NULL;
