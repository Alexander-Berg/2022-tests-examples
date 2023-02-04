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
INSERT INTO revision.attributes (id, contents) VALUES (2, '"rd_el:fc"=>"7", "cat:rd_el"=>"1", "rd_el:fow"=>"3", "rd_el:paved"=>"1", "rd_el:f_zlev"=>"0", "rd_el:oneway"=>"B", "rd_el:t_zlev"=>"0", "rd_el:access_id"=>"31", "rd_el:speed_cat"=>"73", "rd_el:struct_type"=>"0"');
INSERT INTO revision.attributes (id, contents) VALUES (3, '"rel:role"=>"start", "rel:slave"=>"rd_jc", "rel:master"=>"rd_el"');
INSERT INTO revision.attributes (id, contents) VALUES (4, '"rel:role"=>"end", "rel:slave"=>"rd_jc", "rel:master"=>"rd_el"');


--
-- Name: attributes_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.attributes_id_seq', 4, true);


--
-- Data for Name: branch; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.branch (id, type, state, created, created_by, finished, finished_by, attributes) VALUES (0, 'trunk', 'normal', '2015-05-12 18:15:16.129584+03', 0, NULL, 0, NULL);


--
-- Name: branch_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.branch_id_seq', 1, false);


--
-- Data for Name: commit; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (1, 'draft', true, '2015-01-30 19:22:30.543698+03', 127525902, '"action"=>"object-created", "primary-objects"=>"3"', NULL, 0);
INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (2, 'draft', true, '2015-01-30 19:22:39.726277+03', 127525902, '"action"=>"object-created", "primary-objects"=>"13"', NULL, 0);
INSERT INTO revision.commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (3, 'draft', true, '2015-01-30 19:22:53.427999+03', 127525902, '"action"=>"object-created", "primary-objects"=>"36"', NULL, 0);


--
-- Name: commit_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.commit_id_seq', 3, true);


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

INSERT INTO revision.geometry (id, contents) VALUES (1, '0101000020430D0000DC9FAED774745241F37476D11EE25C41');
INSERT INTO revision.geometry (id, contents) VALUES (2, '0101000020430D0000044EE28B31755241040403C4FFE15C41');
INSERT INTO revision.geometry (id, contents) VALUES (3, '0102000020430D000002000000DC9FAED774745241F37476D11EE25C41044EE28B31755241040403C4FFE15C41');
INSERT INTO revision.geometry (id, contents) VALUES (4, '0101000020430D000007EE25578A74524138B3DD26ABE25C41');
INSERT INTO revision.geometry (id, contents) VALUES (5, '0101000020430D0000545D92F558755241C26E8CB986E25C41');
INSERT INTO revision.geometry (id, contents) VALUES (6, '0102000020430D00000200000007EE25578A74524138B3DD26ABE25C41545D92F558755241C26E8CB986E25C41');
INSERT INTO revision.geometry (id, contents) VALUES (7, '0102000020430D000002000000DC9FAED774745241F37476D11EE25C4169218C50E07452418B7907220DE25C41');
INSERT INTO revision.geometry (id, contents) VALUES (8, '0102000020430D00000200000007EE25578A74524138B3DD26ABE25C415F0C4671F97452412FBB809097E25C41');
INSERT INTO revision.geometry (id, contents) VALUES (9, '0101000020430D00005F0C4671F97452412FBB809097E25C41');
INSERT INTO revision.geometry (id, contents) VALUES (10, '0102000020430D0000020000005F0C4671F97452412FBB809097E25C41545D92F558755241C26E8CB986E25C41');
INSERT INTO revision.geometry (id, contents) VALUES (11, '0101000020430D000069218C50E07452418B7907220DE25C41');
INSERT INTO revision.geometry (id, contents) VALUES (12, '0102000020430D00000200000069218C50E07452418B7907220DE25C41044EE28B31755241040403C4FFE15C41');
INSERT INTO revision.geometry (id, contents) VALUES (13, '0101000020430D0000DA5BA458DD7452412939A4C7FCE15C41');
INSERT INTO revision.geometry (id, contents) VALUES (14, '0101000020430D00004613B730FE7452411C0B7BB8B1E25C41');
INSERT INTO revision.geometry (id, contents) VALUES (15, '0102000020430D000002000000DA5BA458DD7452412939A4C7FCE15C4169218C50E07452418B7907220DE25C41');
INSERT INTO revision.geometry (id, contents) VALUES (16, '0102000020430D00000200000069218C50E07452418B7907220DE25C415F0C4671F97452412FBB809097E25C41');
INSERT INTO revision.geometry (id, contents) VALUES (17, '0102000020430D0000020000005F0C4671F97452412FBB809097E25C414613B730FE7452411C0B7BB8B1E25C41');


--
-- Name: geometry_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.geometry_id_seq', 17, true);


--
-- Name: object_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('revision.object_id_seq', 50, true);


--
-- Data for Name: object_revision; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (1, 1, 0, 0, false, 1, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (2, 1, 0, 0, false, 2, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (3, 1, 0, 3, false, 3, 2, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (4, 1, 0, 0, false, 0, 3, 0, 3, 1);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (5, 1, 0, 3, false, 0, 4, 0, 3, 2);

INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (11, 2, 0, 0, false, 4, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (12, 2, 0, 0, false, 5, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (13, 2, 0, 3, false, 6, 2, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (14, 2, 0, 0, false, 0, 3, 0, 13, 11);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (15, 2, 0, 3, false, 0, 4, 0, 13, 12);

INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (3, 3, 1, 0, false, 7, 2, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (5, 3, 1, 0, true, 0, 4, 0, 3, 2);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (13, 3, 2, 0, false, 8, 2, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (15, 3, 2, 0, true, 0, 4, 0, 13, 12);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (21, 3, 0, 0, false, 9, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (22, 3, 0, 0, false, 0, 4, 0, 13, 21);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (23, 3, 0, 0, false, 10, 2, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (24, 3, 0, 0, false, 0, 3, 0, 23, 21);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (25, 3, 0, 0, false, 0, 4, 0, 23, 12);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (26, 3, 0, 0, false, 11, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (27, 3, 0, 0, false, 0, 4, 0, 3, 26);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (28, 3, 0, 0, false, 12, 2, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (29, 3, 0, 0, false, 0, 3, 0, 28, 26);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (30, 3, 0, 0, false, 0, 4, 0, 28, 2);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (31, 3, 0, 0, false, 13, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (32, 3, 0, 0, false, 14, 1, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (33, 3, 0, 0, false, 15, 2, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (34, 3, 0, 0, false, 0, 3, 0, 33, 31);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (35, 3, 0, 0, false, 0, 4, 0, 33, 26);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (36, 3, 0, 0, false, 16, 2, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (37, 3, 0, 0, false, 0, 3, 0, 36, 26);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (38, 3, 0, 0, false, 0, 4, 0, 36, 21);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (39, 3, 0, 0, false, 17, 2, 0, 0, 0);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (40, 3, 0, 0, false, 0, 3, 0, 39, 21);
INSERT INTO revision.object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (41, 3, 0, 0, false, 0, 4, 0, 39, 32);

--
-- PostgreSQL database dump complete
--

