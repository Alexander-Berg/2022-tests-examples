--
-- PostgreSQL database dump
--



--
-- Name: approve_order_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.approve_order_seq', 5, true);


--
-- Data for Name: attributes; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.attributes (id, contents) VALUES (1, '"cat:rd_jc"=>"1"');
INSERT INTO revision.attributes (id, contents) VALUES (2, '"rd_el:fc"=>"7", "cat:rd_el"=>"1", "rd_el:fow"=>"3", "rd_el:ferry"=>"0", "rd_el:paved"=>"1", "rd_el:f_zlev"=>"0", "rd_el:oneway"=>"B", "rd_el:t_zlev"=>"0", "rd_el:access_id"=>"31", "rd_el:speed_cat"=>"73", "rd_el:struct_type"=>"0"');
INSERT INTO revision.attributes (id, contents) VALUES (3, '"rel:role"=>"start", "rel:slave"=>"rd_jc", "rel:master"=>"rd_el"');
INSERT INTO revision.attributes (id, contents) VALUES (4, '"rel:role"=>"end", "rel:slave"=>"rd_jc", "rel:master"=>"rd_el"');
INSERT INTO revision.attributes (id, contents) VALUES (5, '"cat:rd"=>"1", "rd:rd_type"=>"1"');
INSERT INTO revision.attributes (id, contents) VALUES (6, '"cat:rd_nm"=>"1", "rd_nm:lang"=>"ru", "rd_nm:name"=>"Дорога к налоговой", "rd_nm:is_local"=>"1"');
INSERT INTO revision.attributes (id, contents) VALUES (7, '"rel:role"=>"official", "rel:slave"=>"rd_nm", "rel:master"=>"rd"');
INSERT INTO revision.attributes (id, contents) VALUES (8, '"rel:role"=>"part", "rel:slave"=>"rd_el", "rel:master"=>"rd"');
INSERT INTO revision.attributes (id, contents) VALUES (9, '"cat:addr"=>"1", "addr:disp_class"=>"5"');
INSERT INTO revision.attributes (id, contents) VALUES (10, '"cat:addr_nm"=>"1", "addr_nm:lang"=>"ru", "addr_nm:name"=>"Налоговая", "addr_nm:is_local"=>"1"');
INSERT INTO revision.attributes (id, contents) VALUES (11, '"rel:role"=>"official", "rel:slave"=>"addr_nm", "rel:master"=>"addr"');
INSERT INTO revision.attributes (id, contents) VALUES (12, '"rel:role"=>"associated_with", "rel:slave"=>"addr", "rel:master"=>"rd"');


--
-- Name: attributes_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.attributes_id_seq', 12, true);


--
-- Data for Name: branch; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.branch (id, type, state, created, created_by, finished, finished_by, attributes) VALUES (0, 'trunk', 'normal', '2015-05-12 18:15:14.498864+03', 0, NULL, 0, NULL);
INSERT INTO revision.branch (id, type, state, created, created_by, finished, finished_by, attributes) VALUES (2, 'archive', 'normal', '2014-11-13 15:29:06.121743+03', 127525902, '2014-11-13 15:29:58.76401+03', 127525902, NULL);
INSERT INTO revision.branch (id, type, state, created, created_by, finished, finished_by, attributes) VALUES (3, 'stable', 'normal', '2014-11-13 15:31:21.488369+03', 127525902, NULL, 0, NULL);
INSERT INTO revision.branch (id, type, state, created, created_by, finished, finished_by, attributes) VALUES (1, 'approved', 'normal', '2014-11-13 15:30:54.405108+03', 127525902, NULL, 0, NULL);


--
-- Name: branch_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.branch_id_seq', 3, true);


--
-- Data for Name: commit; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (1, 'approved', true, '2014-11-13 15:27:32.909658+03', 127525902, '"action"=>"object-created", "primary-objects"=>"3"', 2, 1);
INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (2, 'approved', true, '2014-11-13 15:27:46.563181+03', 127525902, '"action"=>"object-created", "primary-objects"=>"11"', 2, 2);
INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (3, 'approved', true, '2014-11-13 15:28:25.333137+03', 127525902, '"action"=>"object-created", "primary-objects"=>"21"', 2, 3);
INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (4, 'approved', false, '2014-11-13 15:29:46.014334+03', 127525902, '"action"=>"object-modified", "primary-objects"=>"21"', 2, 0);
INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (5, 'approved', true, '2014-11-13 15:30:45.576893+03', 127525902, '"action"=>"object-modified", "primary-objects"=>"21"', 3, 5);
INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (6, 'approved', false, '2014-11-13 15:31:55.377325+03', 127525902, '"action"=>"object-modified", "primary-objects"=>"21"', 3, 0);


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

INSERT INTO revision.geometry (id, contents) VALUES (1, '0101000020430D000019D0F681627652418934881A4BE05C41');
INSERT INTO revision.geometry (id, contents) VALUES (2, '0101000020430D0000F578D0BE9976524138E6F82A5DE05C41');
INSERT INTO revision.geometry (id, contents) VALUES (3, '0102000020430D00000200000019D0F681627652418934881A4BE05C41F578D0BE9976524138E6F82A5DE05C41');
INSERT INTO revision.geometry (id, contents) VALUES (4, '0101000020430D0000785032698176524197838ED876E05C41');
INSERT INTO revision.geometry (id, contents) VALUES (5, '0101000020430D0000F739211984765241EE0F439E67E05C41');
INSERT INTO revision.geometry (id, contents) VALUES (6, '0101000020430D0000D47FF1457E7652418C1C1AF865E05C41');


--
-- Name: geometry_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.geometry_id_seq', 6, true);


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
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (23, 3, 0, 0, false, 0, 11, 0, 21, 22);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (31, 6, 0, 0, false, 0, 12, 0, 11, 21);


--
-- Data for Name: object_revision_with_geometry; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (1, 1, 0, 0, false, 1, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (2, 1, 0, 0, false, 2, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (3, 1, 0, 0, false, 3, 2, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (21, 3, 0, 5, false, 4, 9, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (21, 4, 3, 0, false, 5, 9, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (21, 5, 3, 0, false, 6, 9, 0, 0, 0);


--
-- Data for Name: object_revision_without_geometry; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (11, 2, 0, 0, false, 0, 5, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (12, 2, 0, 0, false, 0, 6, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (22, 3, 0, 0, false, 0, 10, 0, 0, 0);


--
-- PostgreSQL database dump complete
--

