ALTER TABLE `tasks`
ADD `salary_stat_ready` BOOL
GENERATED ALWAYS AS (`is_parent` = FALSE AND (state = 2 OR derived_task IS NOT NULL)) -- i.e. state = COMPLETED
VIRTUAL NOT NULL;

ALTER TABLE `tasks`
ADD INDEX salary_stat_index (`lock_user`, `queue`, `salary_stat_ready`, `finish_time`, `cost`);