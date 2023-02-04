--
-- PostgreSQL database dump
--



--
-- Name: approve_order_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.approve_order_seq', 4, true);


--
-- Data for Name: attributes; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.attributes (id, contents) VALUES (1, '"cat:rd_jc"=>"1"');
INSERT INTO revision.attributes (id, contents) VALUES (2, '"rd_el:fc"=>"7", "cat:rd_el"=>"1", "rd_el:fow"=>"3", "rd_el:ferry"=>"0", "rd_el:paved"=>"1", "rd_el:f_zlev"=>"0", "rd_el:oneway"=>"B", "rd_el:t_zlev"=>"0", "rd_el:access_id"=>"15", "rd_el:speed_cat"=>"73", "rd_el:struct_type"=>"0"');
INSERT INTO revision.attributes (id, contents) VALUES (3, '"rel:role"=>"start", "rel:slave"=>"rd_jc", "rel:master"=>"rd_el"');
INSERT INTO revision.attributes (id, contents) VALUES (4, '"rel:role"=>"end", "rel:slave"=>"rd_jc", "rel:master"=>"rd_el"');


--
-- Name: attributes_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.attributes_id_seq', 4, true);


--
-- Data for Name: branch; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.branch (id, type, state, created, created_by, finished, finished_by, attributes) VALUES (0, 'trunk', 'normal', '2015-05-12 18:14:34.57212+03', 0, NULL, 0, NULL);
INSERT INTO revision.branch (id, type, state, created, created_by, finished, finished_by, attributes) VALUES (2, 'archive', 'normal', '2014-08-22 13:31:27.973641+04', 127525902, '2014-08-22 13:36:30.040592+04', 127525902, NULL);
INSERT INTO revision.branch (id, type, state, created, created_by, finished, finished_by, attributes) VALUES (3, 'stable', 'normal', '2014-08-22 13:38:39.387003+04', 127525902, NULL, 0, NULL);
INSERT INTO revision.branch (id, type, state, created, created_by, finished, finished_by, attributes) VALUES (1, 'approved', 'normal', '2014-08-22 13:37:40.232093+04', 127525902, NULL, 0, NULL);


--
-- Name: branch_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.branch_id_seq', 3, true);


--
-- Data for Name: commit; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (1, 'approved', true, '2014-08-22 13:28:41.420149+04', 127525902, '"action"=>"object-created", "primary-objects"=>"3"', 2, 1);
INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (3, 'approved', false, '2014-08-22 13:33:42.259221+04', 127525902, '"action"=>"object-created", "primary-objects"=>"22"', 2, 0);
INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (4, 'approved', true, '2014-08-22 13:37:28.684023+04', 127525902, '"action"=>"object-created", "primary-objects"=>"32"', 3, 4);
INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (2, 'approved', true, '2014-08-22 13:33:25.064668+04', 127525902, '"action"=>"object-created", "primary-objects"=>"12"', 3, 2);
INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (5, 'approved', false, '2014-08-22 13:39:12.770538+04', 127525902, '"action"=>"object-created", "primary-objects"=>"42"', 3, 0);
INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (6, 'draft', true, '2014-08-22 13:39:34.687615+04', 127525902, '"action"=>"object-created", "primary-objects"=>"52"', NULL, 0);


--
-- Name: commit_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.commit_id_seq', 6, true);


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

INSERT INTO revision.geometry (id, contents) VALUES (1, '0101000020430D0000E69AFE1E5B7552417DEBB372CCE05C41');
INSERT INTO revision.geometry (id, contents) VALUES (2, '0101000020430D000033E46215787552417E2568FFC1E05C41');
INSERT INTO revision.geometry (id, contents) VALUES (3, '0102000020430D000003000000E69AFE1E5B7552417DEBB372CCE05C418DBF309A6975524180080E39C7E05C4133E46215787552417E2568FFC1E05C41');
INSERT INTO revision.geometry (id, contents) VALUES (4, '0101000020430D00005E6C8E21837552414FD2C586DBE05C41');
INSERT INTO revision.geometry (id, contents) VALUES (5, '0102000020430D00000200000033E46215787552417E2568FFC1E05C415E6C8E21837552414FD2C586DBE05C41');
INSERT INTO revision.geometry (id, contents) VALUES (6, '0101000020430D000099F1DC919A755241B9752DDCB4E05C41');
INSERT INTO revision.geometry (id, contents) VALUES (7, '0102000020430D00000200000033E46215787552417E2568FFC1E05C4199F1DC919A755241B9752DDCB4E05C41');
INSERT INTO revision.geometry (id, contents) VALUES (8, '0101000020430D0000C6B3BC2A9B7552412F85F5B5B4E05C41');
INSERT INTO revision.geometry (id, contents) VALUES (9, '0102000020430D00000200000033E46215787552417E2568FFC1E05C41C6B3BC2A9B7552412F85F5B5B4E05C41');
INSERT INTO revision.geometry (id, contents) VALUES (10, '0101000020430D00002E1D70F37E7552416EE96515EFE05C41');
INSERT INTO revision.geometry (id, contents) VALUES (11, '0102000020430D0000020000005E6C8E21837552414FD2C586DBE05C412E1D70F37E7552416EE96515EFE05C41');
INSERT INTO revision.geometry (id, contents) VALUES (12, '0101000020430D0000AFB5F217A0755241CF51722FE4E05C41');
INSERT INTO revision.geometry (id, contents) VALUES (13, '0102000020430D0000020000005E6C8E21837552414FD2C586DBE05C41AFB5F217A0755241CF51722FE4E05C41');


--
-- Name: geometry_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.geometry_id_seq', 13, true);


--
-- Name: object_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.object_id_seq', 60, true);


--
-- Data for Name: object_revision; Type: TABLE DATA; Schema: revision; Owner: mapspro
--



--
-- Data for Name: object_revision_relation; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (4, 1, 0, 0, false, 0, 3, 0, 3, 1);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (5, 1, 0, 0, false, 0, 4, 0, 3, 2);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (13, 2, 0, 0, false, 0, 3, 0, 12, 2);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (14, 2, 0, 0, false, 0, 4, 0, 12, 11);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (23, 3, 0, 0, false, 0, 3, 0, 22, 2);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (24, 3, 0, 0, false, 0, 4, 0, 22, 21);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (33, 4, 0, 0, false, 0, 3, 0, 32, 2);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (34, 4, 0, 0, false, 0, 4, 0, 32, 31);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (43, 5, 0, 0, false, 0, 3, 0, 42, 11);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (44, 5, 0, 0, false, 0, 4, 0, 42, 41);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (53, 6, 0, 0, false, 0, 3, 0, 52, 11);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (54, 6, 0, 0, false, 0, 4, 0, 52, 51);


--
-- Data for Name: object_revision_with_geometry; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (1, 1, 0, 0, false, 1, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (2, 1, 0, 0, false, 2, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (3, 1, 0, 0, false, 3, 2, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (11, 2, 0, 0, false, 4, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (12, 2, 0, 0, false, 5, 2, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (21, 3, 0, 0, false, 6, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (22, 3, 0, 0, false, 7, 2, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (31, 4, 0, 0, false, 8, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (32, 4, 0, 0, false, 9, 2, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (41, 5, 0, 0, false, 10, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (42, 5, 0, 0, false, 11, 2, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (51, 6, 0, 0, false, 12, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (52, 6, 0, 0, false, 13, 2, 0, 0, 0);


--
-- Data for Name: object_revision_without_geometry; Type: TABLE DATA; Schema: revision; Owner: mapspro
--



--
-- PostgreSQL database dump complete
--

