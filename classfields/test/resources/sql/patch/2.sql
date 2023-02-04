ALTER TABLE `task_history`
ADD COLUMN `new_view` BLOB,
MODIFY `new_state` INT;