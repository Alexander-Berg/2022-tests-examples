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
INSERT INTO revision.attributes (id, contents) VALUES (2, '"rd_el:fc"=>"7", "cat:rd_el"=>"1", "rd_el:fow"=>"3", "rd_el:ferry"=>"0", "rd_el:paved"=>"1", "rd_el:f_zlev"=>"0", "rd_el:oneway"=>"F", "rd_el:t_zlev"=>"0", "rd_el:access_id"=>"31", "rd_el:speed_cat"=>"73", "rd_el:struct_type"=>"0"');
INSERT INTO revision.attributes (id, contents) VALUES (3, '"rel:role"=>"start", "rel:slave"=>"rd_jc", "rel:master"=>"rd_el"');
INSERT INTO revision.attributes (id, contents) VALUES (4, '"rel:role"=>"end", "rel:slave"=>"rd_jc", "rel:master"=>"rd_el"');
INSERT INTO revision.attributes (id, contents) VALUES (5, '"cat:rd"=>"1", "rd:rd_type"=>"1"');
INSERT INTO revision.attributes (id, contents) VALUES (6, '"cat:rd_nm"=>"1", "rd_nm:lang"=>"ru", "rd_nm:name"=>"Дорога домой", "rd_nm:is_local"=>"1"');
INSERT INTO revision.attributes (id, contents) VALUES (7, '"rel:role"=>"official", "rel:slave"=>"rd_nm", "rel:master"=>"rd"');
INSERT INTO revision.attributes (id, contents) VALUES (8, '"rel:role"=>"part", "rel:slave"=>"rd_el", "rel:master"=>"rd"');
INSERT INTO revision.attributes (id, contents) VALUES (9, '"cat:addr"=>"1", "addr:disp_class"=>"5"');
INSERT INTO revision.attributes (id, contents) VALUES (10, '"cat:addr_nm"=>"1", "addr_nm:lang"=>"ru", "addr_nm:name"=>"Милый дом", "addr_nm:is_local"=>"1"');
INSERT INTO revision.attributes (id, contents) VALUES (11, '"rel:role"=>"official", "rel:slave"=>"addr_nm", "rel:master"=>"addr"');
INSERT INTO revision.attributes (id, contents) VALUES (12, '"rel:role"=>"associated_with", "rel:slave"=>"addr", "rel:master"=>"rd"');


--
-- Name: attributes_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.attributes_id_seq', 12, true);


--
-- Data for Name: branch; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.branch (id, type, state, created, created_by, finished, finished_by, attributes) VALUES (0, 'trunk', 'normal', '2015-05-12 18:14:52.557678+03', 0, NULL, 0, NULL);


--
-- Name: branch_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.branch_id_seq', 1, false);


--
-- Data for Name: commit; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (1, 'draft', true, '2014-11-12 15:28:58.136066+03', 127525902, '"action"=>"object-created", "primary-objects"=>"3"', NULL, 0);
INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (2, 'draft', true, '2014-11-12 15:29:38.59469+03', 127525902, '"action"=>"object-created", "primary-objects"=>"11"', NULL, 0);
INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (3, 'draft', true, '2014-11-12 15:30:15.285453+03', 127525902, '"action"=>"object-created", "primary-objects"=>"21"', NULL, 0);
INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (4, 'draft', true, '2014-11-12 15:30:29.689939+03', 127525902, '"action"=>"object-modified", "primary-objects"=>"21"', NULL, 0);
INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (5, 'draft', true, '2014-11-12 15:30:52.057309+03', 127525902, '"action"=>"object-modified", "primary-objects"=>"21"', NULL, 0);
INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (6, 'draft', true, '2014-11-12 15:31:00.759885+03', 127525902, '"action"=>"object-deleted", "primary-objects"=>"21"', NULL, 0);


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

INSERT INTO revision.geometry (id, contents) VALUES (1, '0101000020430D0000584E2CD48C765241E01063EAB7E05C41');
INSERT INTO revision.geometry (id, contents) VALUES (2, '0101000020430D0000669D3292B8765241D735237E68E05C41');
INSERT INTO revision.geometry (id, contents) VALUES (3, '0102000020430D000002000000584E2CD48C765241E01063EAB7E05C41669D3292B8765241D735237E68E05C41');
INSERT INTO revision.geometry (id, contents) VALUES (4, '0101000020430D000025C55919937652414696270399E05C41');


--
-- Name: geometry_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.geometry_id_seq', 4, true);


--
-- Name: object_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.object_id_seq', 40, true);


--
-- Data for Name: object_revision; Type: TABLE DATA; Schema: revision; Owner: mapspro
--



--
-- Data for Name: object_revision_relation; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (4, 1, 0, 0, false, 0, 3, 0, 3, 1);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (5, 1, 0, 0, false, 0, 4, 0, 3, 2);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (13, 2, 0, 0, false, 0, 7, 0, 11, 12);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (14, 2, 0, 0, false, 0, 8, 0, 11, 3);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (23, 3, 0, 6, false, 0, 11, 0, 21, 22);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (31, 4, 0, 5, false, 0, 12, 0, 11, 21);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (31, 5, 4, 0, true, 0, 12, 0, 11, 21);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (23, 6, 3, 0, true, 0, 11, 0, 21, 22);


--
-- Data for Name: object_revision_with_geometry; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (1, 1, 0, 0, false, 1, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (2, 1, 0, 0, false, 2, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (3, 1, 0, 0, false, 3, 2, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (21, 3, 0, 6, false, 4, 9, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (21, 6, 3, 0, true, 4, 9, 0, 0, 0);


--
-- Data for Name: object_revision_without_geometry; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (11, 2, 0, 0, false, 0, 5, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (12, 2, 0, 0, false, 0, 6, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (22, 3, 0, 6, false, 0, 10, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (22, 6, 3, 0, true, 0, 10, 0, 0, 0);


--
-- PostgreSQL database dump complete
--

