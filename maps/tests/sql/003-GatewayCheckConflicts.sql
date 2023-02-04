--
-- PostgreSQL database dump
--



--
-- Name: approve_order_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.approve_order_seq', 2, true);


--
-- Data for Name: attributes; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.attributes (id, contents) VALUES (1, '"role"=>"start_jc"');
INSERT INTO revision.attributes (id, contents) VALUES (2, '"role"=>"end_jc"');
INSERT INTO revision.attributes (id, contents) VALUES (3, '"role"=>"start_road"');
INSERT INTO revision.attributes (id, contents) VALUES (4, '"role"=>"via"');
INSERT INTO revision.attributes (id, contents) VALUES (5, '"role"=>"next_road"');


--
-- Name: attributes_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.attributes_id_seq', 5, true);


--
-- Data for Name: branch; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.branch (id, type, state, created, created_by, finished, finished_by, attributes) VALUES (0, 'trunk', 'normal', '2015-05-12 18:15:12.981379+03', 0, NULL, 0, NULL);
INSERT INTO revision.branch (id, type, state, created, created_by, finished, finished_by, attributes) VALUES (1, 'approved', 'unavailable', '2014-12-08 21:07:48.865459+03', 131, NULL, 0, NULL);
INSERT INTO revision.branch (id, type, state, created, created_by, finished, finished_by, attributes) VALUES (3, 'archive', 'unavailable', '2014-12-08 21:07:48.989697+03', 131, '2014-12-08 21:07:49.020551+03', 131, NULL);
INSERT INTO revision.branch (id, type, state, created, created_by, finished, finished_by, attributes) VALUES (5, 'stable', 'unavailable', '2014-12-08 21:07:49.025529+03', 131, NULL, 0, NULL);


--
-- Name: branch_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.branch_id_seq', 5, true);


--
-- Data for Name: commit; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (1, 'approved', true, '2014-12-08 21:07:48.687808+03', 131, '"description"=>"test commit 1"', 3, 1);
INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (2, 'approved', true, '2014-12-08 21:07:48.73908+03', 131, '"description"=>"test second commit"', 3, 2);


--
-- Name: commit_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.commit_id_seq', 5, true);


--
-- Data for Name: description; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.description (id, contents) VALUES (1, 'the great maneuver');


--
-- Name: description_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.description_id_seq', 1, true);


--
-- Data for Name: geometry; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.geometry (id, contents) VALUES (1, '0102000020430D000002000000000000000000F03F000000000000F03F0000000000001440000000000000F03F');


--
-- Name: geometry_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.geometry_id_seq', 1, true);


--
-- Name: object_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.object_id_seq', 57, true);


--
-- Data for Name: object_revision; Type: TABLE DATA; Schema: revision; Owner: mapspro
--



--
-- Data for Name: object_revision_relation; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (9, 1, 0, 0, false, 0, 1, 0, 1, 4);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (10, 1, 0, 0, false, 0, 2, 0, 1, 5);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (11, 1, 0, 0, false, 0, 1, 0, 2, 5);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (12, 1, 0, 0, false, 0, 2, 0, 2, 6);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (13, 1, 0, 0, false, 0, 1, 0, 3, 5);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (14, 1, 0, 0, false, 0, 2, 0, 3, 7);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (15, 1, 0, 0, false, 0, 3, 0, 8, 3);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (16, 1, 0, 0, false, 0, 4, 0, 8, 5);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (17, 1, 0, 0, false, 0, 5, 0, 8, 2);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (23, 2, 0, 0, false, 0, 1, 0, 20, 22);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (24, 2, 0, 0, false, 0, 2, 0, 20, 21);


--
-- Data for Name: object_revision_with_geometry; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (20, 2, 0, 0, false, 1, 0, 0, 0, 0);


--
-- Data for Name: object_revision_without_geometry; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (1, 1, 0, 0, false, 0, 0, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (2, 1, 0, 0, false, 0, 0, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (3, 1, 0, 0, false, 0, 0, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (4, 1, 0, 0, false, 0, 0, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (5, 1, 0, 0, false, 0, 0, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (6, 1, 0, 0, false, 0, 0, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (7, 1, 0, 0, false, 0, 0, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (8, 1, 0, 0, false, 0, 0, 1, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (21, 2, 0, 0, false, 0, 0, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (22, 2, 0, 0, false, 0, 0, 0, 0, 0);


--
-- PostgreSQL database dump complete
--

