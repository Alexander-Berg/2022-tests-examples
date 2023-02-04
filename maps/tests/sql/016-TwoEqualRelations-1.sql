--
-- PostgreSQL database dump
--

SET client_encoding = 'UTF8';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

SET search_path = revision, public, pg_catalog;

--
-- Name: approve_order_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('approve_order_seq', 1, false);


-- Data for Name: attributes; Type: TABLE DATA; Schema: revision; Owner: mapspro

INSERT INTO attributes (id, contents) VALUES (1, '"cat:rd_jc"=>"1"');
INSERT INTO attributes (id, contents) VALUES (2, '"rd_el:fc"=>"7", "cat:rd_el"=>"1", "rd_el:fow"=>"0", "rd_el:paved"=>"1", "rd_el:f_zlev"=>"0", "rd_el:oneway"=>"B", "rd_el:t_zlev"=>"0", "rd_el:access_id"=>"31", "rd_el:speed_cat"=>"73", "rd_el:struct_type"=>"0"');
INSERT INTO attributes (id, contents) VALUES (3, '"rel:role"=>"start", "rel:slave"=>"rd_jc", "rel:master"=>"rd_el"');
INSERT INTO attributes (id, contents) VALUES (4, '"rel:role"=>"end", "rel:slave"=>"rd_jc", "rel:master"=>"rd_el"');
INSERT INTO attributes (id, contents) VALUES (5, '"cat:rd"=>"1", "rd:rd_type"=>"1"');
INSERT INTO attributes (id, contents) VALUES (6, '"cat:rd_nm"=>"1", "rd_nm:lang"=>"ru", "rd_nm:name"=>"Road", "rd_nm:is_local"=>"1"');
INSERT INTO attributes (id, contents) VALUES (7, '"rel:role"=>"official", "rel:slave"=>"rd_nm", "rel:master"=>"rd"');
INSERT INTO attributes (id, contents) VALUES (8, '"rel:role"=>"part", "rel:slave"=>"rd_el", "rel:master"=>"rd"');


--
-- Name: attributes_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('attributes_id_seq', 8, true);


--
-- Data for Name: branch; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO branch (id, created, created_by, finished, finished_by, attributes, state, type) VALUES (0, '2014-08-19 16:07:22.700024+04', 0, NULL, 0, NULL, 'normal', 'trunk');


--
-- Name: branch_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('branch_id_seq', 1, false);


--
-- Data for Name: commit; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (1, 'draft', true, '2015-06-16 20:12:07.936755+03', 127525902, '"action"=>"object-created", "edit_notes:1"=>"created,modified-relations-masters-added-created-rd_el", "edit_notes:2"=>"created,modified-relations-masters-added-created-rd_el", "edit_notes:3"=>"created", "primary_object:3"=>"1"', NULL, 0);
INSERT INTO commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (2, 'draft', true, '2015-06-16 20:12:16.745937+03', 127525902, '"action"=>"object-created", "edit_notes:2"=>"modified-relations-masters-added-created-rd_el", "edit_notes:11"=>"created,modified-relations-masters-added-created-rd_el", "edit_notes:12"=>"created", "primary_object:12"=>"1"', NULL, 0);
INSERT INTO commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (3, 'draft', true, '2015-06-16 20:13:20.259219+03', 127525902, '"action"=>"object-created", "edit_notes:3"=>"modified-relations-masters-added-created-rd", "edit_notes:12"=>"modified-relations-masters-added-created-rd", "edit_notes:21"=>"created", "primary_object:21"=>"1"', NULL, 0);
INSERT INTO commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (4, 'draft', true, '2015-06-16 20:16:08.685442+03', 127525902, '"action"=>"object-modified", "edit_notes:12"=>"modified-relations-masters-removed-part-rd", "edit_notes:21"=>"modified-geometry-elements-removed", "primary_object:21"=>"1"', NULL, 0);
INSERT INTO commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (5, 'draft', true, '2015-06-16 20:16:41.381775+03', 127525902, '"action"=>"object-modified", "edit_notes:12"=>"modified-relations-masters-added-part-rd", "edit_notes:21"=>"modified-geometry-elements-added", "primary_object:21"=>"1"', NULL, 0);
INSERT INTO commit (id, state, trunk, created, created_by, attributes, stable_branch_id, approve_order) VALUES (6, 'draft', true, '2015-06-16 20:16:50.584711+03', 127525902, '"action"=>"object-deleted", "edit_notes:2"=>"modified-relations-masters-removed-start-rd_el", "edit_notes:11"=>"deleted,modified-relations-masters-removed-end-rd_el", "edit_notes:12"=>"deleted,modified-relations-masters-removed-part-rd", "edit_notes:21"=>"modified-geometry-elements-removed", "primary_object:12"=>"1"', NULL, 0);


--
-- Name: commit_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('commit_id_seq', 6, true);


--
-- Name: description_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('description_id_seq', 1, false);


--
-- Data for Name: geometry; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO geometry (id, contents) VALUES (1, '0101000020430D00000333490A5575524178AE7CAD00E15C41');
INSERT INTO geometry (id, contents) VALUES (2, '0101000020430D000035B7F0CB7D755241E9F6516507E15C41');
INSERT INTO geometry (id, contents) VALUES (3, '0102000020430D0000020000000333490A5575524178AE7CAD00E15C4135B7F0CB7D755241E9F6516507E15C41');
INSERT INTO geometry (id, contents) VALUES (4, '0101000020430D00003C3F0468B07552419EE276020FE15C41');
INSERT INTO geometry (id, contents) VALUES (5, '0102000020430D00000200000035B7F0CB7D755241E9F6516507E15C413C3F0468B07552419EE276020FE15C41');


--
-- Name: geometry_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('geometry_id_seq', 5, true);


--
-- Name: object_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('object_id_seq', 40, true);


--
-- Data for Name: object_revision; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (1, 1, 0, 0, false, 1, 1, 0, 0, 0);
INSERT INTO object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (2, 1, 0, 0, false, 2, 1, 0, 0, 0);
INSERT INTO object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (3, 1, 0, 0, false, 3, 2, 0, 0, 0);
INSERT INTO object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (4, 1, 0, 0, false, 0, 3, 0, 3, 1);
INSERT INTO object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (5, 1, 0, 0, false, 0, 4, 0, 3, 2);

INSERT INTO object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (11, 2, 0, 6, false, 4, 1, 0, 0, 0);
INSERT INTO object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (12, 2, 0, 6, false, 5, 2, 0, 0, 0);
INSERT INTO object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (13, 2, 0, 6, false, 0, 3, 0, 12, 2);
INSERT INTO object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (14, 2, 0, 6, false, 0, 4, 0, 12, 11);

INSERT INTO object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (23, 3, 0, 0, false, 0, 7, 0, 21, 22);
INSERT INTO object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (24, 3, 0, 0, false, 0, 8, 0, 21, 3);
INSERT INTO object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (25, 3, 0, 4, false, 0, 8, 0, 21, 12);
INSERT INTO object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (21, 3, 0, 0, false, 0, 5, 0, 0, 0);
INSERT INTO object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (22, 3, 0, 0, false, 0, 6, 0, 0, 0);

INSERT INTO object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (25, 4, 3, 0, true, 0, 8, 0, 21, 12);

INSERT INTO object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (31, 5, 0, 6, false, 0, 8, 0, 21, 12);

INSERT INTO object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (11, 6, 2, 0, true, 4, 1, 0, 0, 0);
INSERT INTO object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (12, 6, 2, 0, true, 5, 2, 0, 0, 0);
INSERT INTO object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (13, 6, 2, 0, true, 0, 3, 0, 12, 2);
INSERT INTO object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (14, 6, 2, 0, true, 0, 4, 0, 12, 11);
INSERT INTO object_revision (object_id, commit_id, prev_commit_id, next_commit_id, deleted, geometry_id, attributes_id, description_id, master_object_id, slave_object_id) VALUES (31, 6, 5, 0, true, 0, 8, 0, 21, 12);



--
-- PostgreSQL database dump complete
--

