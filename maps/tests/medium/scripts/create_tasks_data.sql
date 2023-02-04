DELETE FROM social.commit_event WHERE event_id IN (SELECT event_id FROM social.task WHERE resolved_by = 111);
DELETE FROM social.task WHERE resolved_by = 111;

do $$
DECLARE curTime timestamp;
DECLARE creationTime timestamp;
BEGIN
    curTime := now();
    creationTime := curTime - '77 days'::interval;

    INSERT INTO social.task
      (event_id, commit_id, resolved_by, resolved_at,                      resolve_resolution, closed_by, closed_at, close_resolution, locked_by, locked_at, created_at,   primary_object_category_id, type             ) VALUES
      (4,        34919586,  111,         curTime - '10 minutes'::interval, 'accept',           0,         NULL,      NULL,             0,         NULL,      creationTime, 'rd_el',                    'edit'           ),
      (3,        NULL,      111,         curTime                         , 'accept',           0,         NULL,      NULL,             0,         NULL,      creationTime, NULL,                       'closed-feedback'),
      (2,        34963179,  111,         curTime                         , 'revert',           0,         NULL,      NULL,             0,         NULL,      creationTime, 'rd_el',                    'edit'           ),
      (1,        NULL,      111,         curTime + '10 minutes'::interval, 'accept',           0,         NULL,      NULL,             0,         NULL,      creationTime, NULL,                       'closed-feedback');

    INSERT INTO social.commit_event
      (event_id, created_by, created_at,   branch_id, commit_id, action,           bounds, primary_object_id, primary_object_category_id, primary_object_label,   primary_object_notes, bounds_geom, type  ) VALUES
      (4,        128833740,  creationTime, 0,         34919586,  'object-created', NULL,   1824703773,        'rd_el',                    '{{categories:rd_el}}', 'created',            NULL,        'edit')
    , (2,        128833740,  creationTime, 0,         34963179,  'object-created', NULL,   1824954961,        'rd_el',                    '{{categories:rd_el}}', 'created',            NULL,        'edit');

    INSERT INTO social.feedback_event
      (event_id, created_by, created_at,   feedback_task_id, action,   type             ) VALUES
      (3,        128833740,  creationTime, 1,                'closed', 'closed-feedback'),
      (1,        128833740,  creationTime, 2,                'closed', 'closed-feedback');

    INSERT INTO social.feedback_task_pending
      (id, created_at,   position,                            type,   description, source,       workflow) VALUES
      (1 , creationTime, st_geomfromtext('POINT(0 0)', 3395), 'road', '"descr"',   'wow-source', 'task'  ),
      (2 , creationTime, st_geomfromtext('POINT(0 0)', 3395), 'road', '"descr"',   'wow-source', 'task'  );

END $$;
