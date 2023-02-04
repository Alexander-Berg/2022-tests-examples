DELETE FROM social.commit_event WHERE event_id IN (SELECT event_id FROM social.task WHERE resolved_by = 111);
DELETE FROM social.task WHERE resolved_by = 111;

do $$
DECLARE curTime timestamp;
DECLARE creationTime timestamp;
BEGIN
    curTime := now();
    creationTime := curTime - '77 days'::interval;

    INSERT INTO social.task
      (event_id, commit_id, resolved_by, resolved_at, resolve_resolution, closed_by, closed_at, close_resolution, locked_by, locked_at, created_at, primary_object_category_id, type) VALUES
      (32643153, 34919586, 111, curTime - '10 minutes'::interval, 'accept', 0, NULL, NULL, 0, NULL, creationTime, 'rd_el', 'edit')
    , (32686769, 34963179, 111, curTime, 'revert', 111, curTime, 'revert', 0, NULL, creationTime, 'rd_el', 'edit')
    , (32686823, 34963232, 111, curTime, 'revert', 777, curTime, 'revert', 0, NULL, creationTime, 'rd_el', 'edit')
    , (32686854, 34963265, 111, curTime, 'revert', 111, curTime - '5 hours'::interval, 'revert', 0, NULL, creationTime, 'rd_el', 'edit')
    , (32686898, 34963306, 111, curTime, 'revert', 111, curTime + '5 hours'::interval, 'revert', 0, NULL, creationTime, 'rd_el', 'edit')
    , (35892476, 38175684, 111, curTime, 'accept', 0, NULL, NULL, 0, NULL, curTime, 'rd_el', 'edit');

    INSERT INTO social.commit_event
      (event_id, created_by, created_at, branch_id, commit_id, action, bounds, primary_object_id, primary_object_category_id, primary_object_label, primary_object_notes, bounds_geom, type) VALUES
      (32643153, 128833740, creationTime, 0, 34919586, 'object-created', NULL, 1824703773, 'rd_el', '{{categories:rd_el}}', 'created', NULL, 'edit')
    , (32686854, 431392314, creationTime, 0, 34963265, 'object-created', NULL, 1824955733, 'rd_el', '{{categories:rd_el}}', 'created', NULL, 'edit')
    , (32686769, 431392314, creationTime, 0, 34963179, 'object-created', NULL, 1824954961, 'rd_el', '{{categories:rd_el}}', 'created', NULL, 'edit')
    , (32686823, 431392314, creationTime, 0, 34963232, 'object-created', NULL, 1824955423, 'rd_el', '{{categories:rd_el}}', 'created', NULL, 'edit')
    , (35892476, 111, curTime, 0, 38175684, 'commit-reverted', NULL, 1824954961, 'rd_el', '{{categories:rd_el}}', 'reverted', NULL, 'edit')
    , (32686898, 431392314, creationTime, 0, 34963306, 'object-created', NULL, 1824955883, 'rd_el', '{{categories:rd_el}}', 'created', NULL, 'edit');
END $$;
