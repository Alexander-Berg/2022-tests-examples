--
-- PostgreSQL database dump
--



--
-- Name: approve_order_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.approve_order_seq', 1, false);


--
-- Data for Name: attributes; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.attributes (id, contents) VALUES (1, '"cat:rd_jc"=>"1"');
INSERT INTO revision.attributes (id, contents) VALUES (2, '"rd_el:fc"=>"7", "cat:rd_el"=>"1", "rd_el:fow"=>"3", "rd_el:ferry"=>"0", "rd_el:paved"=>"1", "rd_el:f_zlev"=>"0", "rd_el:oneway"=>"B", "rd_el:t_zlev"=>"0", "rd_el:access_id"=>"31", "rd_el:speed_cat"=>"73", "rd_el:struct_type"=>"0"');
INSERT INTO revision.attributes (id, contents) VALUES (3, '"rel:role"=>"start", "rel:slave"=>"rd_jc", "rel:master"=>"rd_el"');
INSERT INTO revision.attributes (id, contents) VALUES (4, '"rel:role"=>"end", "rel:slave"=>"rd_jc", "rel:master"=>"rd_el"');


--
-- Name: attributes_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.attributes_id_seq', 4, true);


--
-- Data for Name: branch; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.branch (id, type, state, created, created_by, finished, finished_by, attributes) VALUES (0, 'trunk', 'normal', '2015-05-12 18:15:17.734866+03', 0, NULL, 0, NULL);


--
-- Name: branch_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.branch_id_seq', 1, false);


--
-- Data for Name: commit; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (1, 'draft', true, '2014-11-09 12:42:36.315568+03', 127525902, '"action"=>"object-created", "primary-objects"=>"3"', NULL, 0);
INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (2, 'draft', true, '2014-11-09 12:42:46.103787+03', 127525902, '"action"=>"object-modified", "primary-objects"=>"1"', NULL, 0);
INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (3, 'draft', true, '2014-11-09 12:43:11.596117+03', 127525902, '"action"=>"object-created", "primary-objects"=>"12"', NULL, 0);
INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (4, 'draft', true, '2014-11-09 12:43:20.416438+03', 127525902, '"action"=>"object-modified", "primary-objects"=>"11"', NULL, 0);


--
-- Name: commit_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.commit_id_seq', 4, true);


--
-- Data for Name: description; Type: TABLE DATA; Schema: revision; Owner: mapspro
--



--
-- Name: description_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.description_id_seq', 1, false);


--
-- Data for Name: geometry; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.geometry (id, contents) VALUES (1, '0101000020430D00002CE6E83520E44F414852B3E88A7B5C41');
INSERT INTO revision.geometry (id, contents) VALUES (2, '0101000020430D0000E9B6016A0AE44F418ED94FB67F7B5C41');
INSERT INTO revision.geometry (id, contents) VALUES (3, '0102000020430D0000020000002CE6E83520E44F414852B3E88A7B5C41E9B6016A0AE44F418ED94FB67F7B5C41');
INSERT INTO revision.geometry (id, contents) VALUES (4, '0101000020430D00002105072B29E44F4147A477898F7B5C41');
INSERT INTO revision.geometry (id, contents) VALUES (5, '0102000020430D0000020000002105072B29E44F4147A477898F7B5C41E9B6016A0AE44F418ED94FB67F7B5C41');
INSERT INTO revision.geometry (id, contents) VALUES (6, '0101000020430D0000DB6A3B775EE44F4196967986997B5C41');
INSERT INTO revision.geometry (id, contents) VALUES (7, '0102000020430D0000020000002105072B29E44F4147A477898F7B5C41DB6A3B775EE44F4196967986997B5C41');
INSERT INTO revision.geometry (id, contents) VALUES (8, '0101000020430D000012A930D252E44F41FF477DBCA17B5C41');
INSERT INTO revision.geometry (id, contents) VALUES (9, '0102000020430D0000020000002105072B29E44F4147A477898F7B5C4112A930D252E44F41FF477DBCA17B5C41');


--
-- Name: geometry_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.geometry_id_seq', 9, true);


--
-- Name: object_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.object_id_seq', 20, true);


--
-- Data for Name: object_revision; Type: TABLE DATA; Schema: revision; Owner: mapspro
--



--
-- Data for Name: object_revision_relation; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (4, 1, 0, 0, false, 0, 3, 0, 3, 1);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (5, 1, 0, 0, false, 0, 4, 0, 3, 2);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (13, 3, 0, 4, false, 0, 3, 0, 12, 1);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (14, 3, 0, 4, false, 0, 4, 0, 12, 11);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (13, 4, 3, 0, true, 0, 3, 0, 12, 1);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (14, 4, 3, 0, true, 0, 4, 0, 12, 11);


--
-- Data for Name: object_revision_with_geometry; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (2, 1, 0, 0, false, 2, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (1, 1, 0, 2, false, 1, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (3, 1, 0, 2, false, 3, 2, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (1, 2, 1, 0, false, 4, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (3, 2, 1, 0, false, 5, 2, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (11, 3, 0, 4, false, 6, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (12, 3, 0, 4, false, 7, 2, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (11, 4, 3, 0, true, 6, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (12, 4, 3, 0, true, 7, 2, 0, 0, 0);


--
-- Data for Name: object_revision_without_geometry; Type: TABLE DATA; Schema: revision; Owner: mapspro
--



--
-- PostgreSQL database dump complete
--

