-- Create 3 objects
-- 1 - only move
-- 2 - change attributes and then move
-- 3 - move and then change attribute
-- Only 1st object should be filtered out by diffalerts


-- Then create 3 more objects
-- 4 - rd_el
-- 5 - rd
-- 6 - relation between rd_el and rd


-- userid 100001 = cartographer
-- userid 100002 = outsourcer
-- userid 100003 = common user

SET search_path = revision, public;

INSERT INTO branch VALUES (1, '2016-01-25 15:53:04.312935+03', 208826301, NULL, 0, NULL, 'normal', 'approved');
INSERT INTO branch VALUES (2, '2016-01-25 17:08:52.949331+03', 208826301, NULL, 0, NULL, 'normal', 'stable');

SELECT pg_catalog.setval('branch_id_seq', 2, true);

INSERT INTO commit VALUES (1, 'approved', true, '2016-01-25 15:10:33.660905+03', 100001, '', 2, 1);
INSERT INTO commit VALUES (2, 'approved', true, '2016-01-25 15:39:49.308139+03', 100003, '"action"=>"group-moved", "source"=>"long-task"', 2, 1);
INSERT INTO commit VALUES (3, 'approved', true, '2016-01-25 15:43:20.665994+03', 100002, '', 2, 1);
INSERT INTO commit VALUES (4, 'approved', true, '2016-01-25 15:44:10.277327+03', 100002, '"action"=>"group-moved", "source"=>"long-task"', 2, 1);
INSERT INTO commit VALUES (5, 'approved', true, '2016-01-25 15:45:53.965486+03', 100001, '"action"=>"group-moved", "source"=>"long-task"', 2, 1);
INSERT INTO commit VALUES (6, 'approved', true, '2016-01-25 15:46:25.524017+03', 100001, '', 2, 1);

INSERT INTO commit VALUES (7, 'approved', true, '2016-01-25 15:46:25.524017+03', 100001, '"action"=>"group-modified-attributes", "edit_notes:1"=>"modified-attributes-other"', 2, 1);
INSERT INTO commit VALUES (8, 'approved', true, '2016-01-25 15:46:25.524017+03', 100001, '"action"=>"group-modified-attributes", "source"=>"long-task"', 2, 1);

INSERT INTO commit VALUES (9, 'approved', true, '2016-01-25 15:10:33.660905+03', 100001, '', 2, 1);
INSERT INTO commit VALUES (10, 'approved', true, '2016-01-25 15:10:33.660905+03', 100001, '', 2, 1);
INSERT INTO commit VALUES (11, 'approved', true, '2016-01-25 15:10:33.660905+03', 100003, '', 2, 1);

SELECT pg_catalog.setval('commit_id_seq', 11, true);

INSERT INTO attributes VALUES (1, '"cat:poi_medicine"=>"1", "poi_medicine:ft_type_id"=>"172"');
INSERT INTO attributes VALUES (2, '"cat:poi_medicine"=>"1", "poi_medicine:ft_type_id"=>"181"');
INSERT INTO attributes VALUES (3, '"cat:rd_el"=>"1"');
INSERT INTO attributes VALUES (4, '"cat:rd"=>"1"');
INSERT INTO attributes VALUES (5, '"rel:role"=>"part", "rel:slave"=>"rd_el", "rel:master"=>"rd"');
INSERT INTO attributes VALUES (6, '"cat:rd"=>"1", "rd:rd_type"=>"1"');

SELECT pg_catalog.setval('attributes_id_seq', 6, true);

INSERT INTO geometry VALUES (1, ST_GeomFromEWKT('SRID=3395;POINT(0 0)'));
INSERT INTO geometry VALUES (2, ST_GeomFromEWKT('SRID=3395;POINT(10 10)'));
INSERT INTO geometry VALUES (3, ST_GeomFromEWKT('SRID=3395;LINESTRING(10 10, 20 20)'));
INSERT INTO geometry VALUES (4, ST_GeomFromEWKT('SRID=3395;LINESTRING(10 10, 30 30)'));

SELECT pg_catalog.setval('geometry_id_seq', 4, true);

INSERT INTO object_revision_with_geometry VALUES (1, 1, 0, 2, false, 1, 1, 0, 0, 0);
INSERT INTO object_revision_with_geometry VALUES (2, 1, 0, 3, false, 1, 1, 0, 0, 0);
INSERT INTO object_revision_with_geometry VALUES (3, 1, 0, 5, false, 1, 1, 0, 0, 0);

INSERT INTO object_revision_with_geometry VALUES (1, 2, 1, 0, false, 2, 1, 0, 0, 0); -- Move object 1

INSERT INTO object_revision_with_geometry VALUES (2, 3, 1, 4, false, 1, 2, 0, 0, 0); -- Change attribute of object 2
INSERT INTO object_revision_with_geometry VALUES (2, 4, 3, 0, false, 2, 2, 0, 0, 0); -- Move object 2

INSERT INTO object_revision_with_geometry VALUES (3, 5, 1, 6, false, 2, 1, 0, 0, 0); -- Move object 3
INSERT INTO object_revision_with_geometry VALUES (3, 6, 5, 0, false, 2, 2, 0, 0, 0); -- Change attribute of object 3

INSERT INTO object_revision_with_geometry VALUES (1, 7, 2, 0, false, 2, 2, 0, 0, 0); -- Group edit object 1 from the editor
INSERT INTO object_revision_with_geometry VALUES (2, 8, 4, 0, false, 2, 1, 0, 0, 0); -- Group edit object 2 from the long task

INSERT INTO object_revision_with_geometry    VALUES (4, 9, 0, 10, false, 3, 3, 0, 0, 0); -- Create rd_el
INSERT INTO object_revision_without_geometry VALUES (5, 9, 0, 11, false, 0, 4, 0, 0, 0); -- Create rd
INSERT INTO object_revision_relation         VALUES (6, 9, 0, 0, false, 0, 5, 0, 5, 4); -- Create relation

INSERT INTO object_revision_with_geometry    VALUES (4, 10, 9, 0, false, 4, 3, 0, 0, 0); -- Change geometry of rd_el
INSERT INTO object_revision_without_geometry VALUES (5, 11, 9, 0, false, 0, 6, 0, 0, 0); -- Change attributes of rd

SELECT pg_catalog.setval('object_id_seq', 6, true);

-- Ponomarev's fix
INSERT INTO attributes_relations SELECT * FROM attributes WHERE id IN (SELECT attributes_id FROM object_revision_relation);

SET search_path = public;
