INSERT INTO `suspect_items`
(`id`, `project_id`, `object_id`, `status`, `mtime`, `plugins`) VALUES
(1, 1, 100, 'new', NOW(), ''),
(2, 1, 101, 'new', NOW(), ''),
(3, 1, 102, 'in_progress', NOW(), ''),
(4, 1, 103, 'done', NOW(), ''),
(5, 1, 104, 'done', NOW(), ''),
(6, 1, 105, 'in_progress', NOW(), '');

INSERT INTO `suspect_items_actions`
(`id`, `action`, `suspect_item_id`, `moderator_id`, `ctime`, `plugins`) VALUES
(1, 'approve', 1, 5, '2017-03-09', ''),
(2, 'delete', 1, 5, '2017-03-10', ''),
(3, 'import', 2, 5, '2017-03-11', ''),
(4, 'delete', 2, 5, '2017-03-12', ''),
(5, 'signal', 3, 5, '2017-03-13', ''),
(6, 'delete', 3, 5, '2017-03-14', ''),
(7, 'create_object', 4, 5, '2017-03-15', ''),
(8, 'delete', 4, 5, '2017-03-16', ''),
(9, 'import', 5, 5, '2017-03-17', ''),
(10, 'autoblock', 5, 5, '2017-03-18', ''),
(11, 'create_object', 6, 5, '2017-03-19', ''),
(12, 'noquota', 6, 5, '2017-03-20', '');

INSERT INTO `suspect_items_actions_reasons`
(`action_id`, `reason_id`) VALUES
(2, 1),
(2, 2),
(2, 3),
(4, 1),
(4, 2),
(6, 1),
(7, 1),
(7, 2),
(7, 3),
(12, 4),
(12, 5);
