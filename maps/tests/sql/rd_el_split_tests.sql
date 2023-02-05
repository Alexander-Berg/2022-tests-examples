SET search_path = revision, public;

INSERT INTO branch VALUES (0, '2016-01-25 14:24:52.830581+03', 0, NULL, 0, NULL, 'normal', 'trunk');

SELECT pg_catalog.setval('branch_id_seq', 0, true);

-- create rd_el 1
INSERT INTO commit VALUES (1, 'draft', true, '2016-01-25 15:10:33.660905+03', 208826301, '', NULL, 0);

-- split rt_el into 1 and 2
INSERT INTO commit VALUES (2, 'draft', true, '2016-01-25 15:39:49.308139+03', 208826301, '"edit_notes:1"=>"modified-geometry,modified-split","edit_notes:2"=>"created,created-split"', NULL, 0);

-- split rd_el 2 into 2 and 3
INSERT INTO commit VALUES (3, 'draft', true, '2016-01-25 15:43:20.665994+03', 208826301, '"edit_notes:2"=>"modified-geometry,modified-split","edit_notes:3"=>"created,created-split"', NULL, 0);

-- delete rd_el 2
INSERT INTO commit VALUES (4, 'draft', true, '2016-01-25 15:44:10.277327+03', 208826301, '', NULL, 0);

SELECT pg_catalog.setval('commit_id_seq', 4, true);

INSERT INTO attributes VALUES (1, '"cat:rd_el"=>"1"');

SELECT pg_catalog.setval('attributes_id_seq', 1, true);

INSERT INTO geometry VALUES (1, ST_GeomFromEWKT('SRID=3395;LINESTRING(10.0 10.0,20.0 20.0,30.0 30.0,40.0 40.0)'));
INSERT INTO geometry VALUES (2, ST_GeomFromEWKT('SRID=3395;LINESTRING(10.0 10.0,20.0 20.0)'));
INSERT INTO geometry VALUES (3, ST_GeomFromEWKT('SRID=3395;LINESTRING(20.0 20.0,30.0 30.0,40.0 40.0)'));
INSERT INTO geometry VALUES (4, ST_GeomFromEWKT('SRID=3395;LINESTRING(20.0 20.0,30.0 30.0)'));
INSERT INTO geometry VALUES (5, ST_GeomFromEWKT('SRID=3395;LINESTRING(30.0 30.0,40.0 40.0)'));

SELECT pg_catalog.setval('geometry_id_seq', 5, true);

-- create rd_el 1
INSERT INTO object_revision_with_geometry VALUES (1, 1, 0, 2, false, 1, 1, 0, 0, 0);

-- split rt_el into 1 and 2
INSERT INTO object_revision_with_geometry VALUES (1, 2, 1, 0, false, 2, 1, 0, 0, 0);
INSERT INTO object_revision_with_geometry VALUES (2, 2, 0, 3, false, 3, 1, 0, 0, 0);

-- split rd_el 2 into 2 and 3
INSERT INTO object_revision_with_geometry VALUES (2, 3, 2, 4, false, 5, 1, 0, 0, 0);
INSERT INTO object_revision_with_geometry VALUES (3, 3, 0, 0, false, 4, 1, 0, 0, 0);

-- delete rd_el 2
INSERT INTO object_revision_with_geometry VALUES (2, 4, 3, 0, true, 5, 1, 0, 0, 0);

SELECT pg_catalog.setval('object_id_seq', 3, true);
