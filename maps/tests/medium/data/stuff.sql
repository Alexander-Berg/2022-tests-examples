INSERT INTO ugc.task (task_id, status, geom, duration, distance, indoor_plan_id, created_at, activated_at, done_at, "interval", created_by, cancelled_by, skip_evaluation, is_test)
VALUES('1', 'available', '0101000020E610000063B323D577184C40EBAA402D06054D40', '2524', '1768', '7', '2020-12-22 15:26:58.610489+03', NULL, NULL, '0', '', NULL, false, false);

INSERT INTO ugc.task_name (task_id, locale, value)
VALUES('1', 'ru', 'Аврора');

INSERT INTO ugc.task_path (task_path_id, task_id, geom, indoor_level_universal_id, duration, distance, description)
VALUES('2', '1', '0102000020E610000002000000F052EA9271AC42408789062978D84B402F89B3226AAC42400BCF4BC5C6D84B40', '3B', '300', '250', 'слыш работать');

INSERT INTO ugc.assignment (assignment_id, task_id, status, assigned_to, acquired_at, expires_at, submitted_at, evaluated_at)
VALUES('3', '1', 'abandoned', '8', '2020-10-06 12:35:20.398706+03', '2020-10-16 12:35:20.398706+03', NULL, NULL);

INSERT INTO ugc.assignment_task_path (assignment_task_path_id, assignment_id, task_id, task_path_id, status, reason)
VALUES('4', '3', '1', '2', 'abandoned', NULL);

INSERT INTO ugc.assignment_task_path_track (assignment_task_path_track_id, assignment_task_path_id, s3_id)
VALUES('5', '4', '00000000-00000000-00000000-00000000');

INSERT INTO ugc.barrier (barrier_id, assignment_task_path_track_id, assignment_id, indoor_plan_id, indoor_level_universal_id, geom, created_at, comment)
VALUES('6', '5', '3', '7', '3B', '0102000020E6100000020000005342B0AA5E184C40F3716DA818054D40212235ED62184C40AC915D6919054D40', '2021-02-25 17:23:39.610824+03', 'компилируется');

INSERT INTO ugc.startrek_epics (indoor_plan_id, startrek_ticket)
VALUES('7', 'DARIA-5006');
