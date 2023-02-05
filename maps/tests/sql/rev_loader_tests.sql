--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

SET search_path = revision, pg_catalog;

--
-- Name: approve_order_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('approve_order_seq', 2, true);


--
-- Data for Name: attributes; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO attributes VALUES (5, '"cat:rd_jc"=>"1"');
INSERT INTO attributes VALUES (6, '"rd_el:fc"=>"7", "cat:rd_el"=>"1", "rd_el:fow"=>"0", "rd_el:paved"=>"1", "rd_el:f_zlev"=>"0", "rd_el:oneway"=>"B", "rd_el:t_zlev"=>"0", "rd_el:access_id"=>"31", "rd_el:speed_cat"=>"73", "rd_el:struct_type"=>"0"');
INSERT INTO attributes VALUES (7, '"rel:role"=>"start", "rel:slave"=>"rd_jc", "rel:master"=>"rd_el"');
INSERT INTO attributes VALUES (8, '"rel:role"=>"end", "rel:slave"=>"rd_jc", "rel:master"=>"rd_el"');
INSERT INTO attributes VALUES (9, '"cat:rd"=>"1", "rd:rd_type"=>"1"');
INSERT INTO attributes VALUES (10, '"cat:rd_nm"=>"1", "rd_nm:lang"=>"ru", "rd_nm:name"=>"дорога1", "rd_nm:is_local"=>"1"');
INSERT INTO attributes VALUES (11, '"rel:role"=>"official", "rel:slave"=>"rd_nm", "rel:master"=>"rd"');
INSERT INTO attributes VALUES (12, '"rel:role"=>"part", "rel:slave"=>"rd_el", "rel:master"=>"rd"');
INSERT INTO attributes VALUES (13, '"cat:cond"=>"1", "cond:access_id"=>"14", "cond:cond_type"=>"1"');
INSERT INTO attributes VALUES (14, '"cat:cond_dt"=>"1", "cond_dt:day"=>"127", "cond_dt:date_end"=>"3112", "cond_dt:time_end"=>"1200", "cond_dt:date_start"=>"0101", "cond_dt:time_start"=>"0000"');
INSERT INTO attributes VALUES (15, '"rel:role"=>"applied_to", "rel:slave"=>"cond_dt", "rel:master"=>"cond"');
INSERT INTO attributes VALUES (16, '"rel:role"=>"from", "rel:slave"=>"rd_el", "rel:master"=>"cond"');
INSERT INTO attributes VALUES (17, '"rel:role"=>"to", "rel:slave"=>"rd_el", "rel:master"=>"cond", "rel:seq_num"=>"0"');
INSERT INTO attributes VALUES (18, '"rel:role"=>"via", "rel:slave"=>"rd_jc", "rel:master"=>"cond"');
INSERT INTO attributes VALUES (19, '"cat:addr"=>"1", "addr:disp_class"=>"5"');
INSERT INTO attributes VALUES (20, '"cat:addr_nm"=>"1", "addr_nm:lang"=>"ru", "addr_nm:name"=>"12", "addr_nm:is_local"=>"1"');
INSERT INTO attributes VALUES (21, '"rel:role"=>"official", "rel:slave"=>"addr_nm", "rel:master"=>"addr"');
INSERT INTO attributes VALUES (22, '"rel:role"=>"associated_with", "rel:slave"=>"addr", "rel:master"=>"rd"');
INSERT INTO attributes VALUES (23, '"cat:ad_jc"=>"1"');
INSERT INTO attributes VALUES (24, '"cat:ad_el"=>"1"');
INSERT INTO attributes VALUES (25, '"rel:role"=>"start", "rel:slave"=>"ad_jc", "rel:master"=>"ad_el"');
INSERT INTO attributes VALUES (26, '"rel:role"=>"end", "rel:slave"=>"ad_jc", "rel:master"=>"ad_el"');
INSERT INTO attributes VALUES (27, '"cat:ad"=>"1", "ad:disp_class"=>"5", "ad:level_kind"=>"4"');
INSERT INTO attributes VALUES (28, '"cat:ad_nm"=>"1", "ad_nm:lang"=>"ru", "ad_nm:name"=>"атд1", "ad_nm:is_local"=>"1"');
INSERT INTO attributes VALUES (29, '"rel:role"=>"official", "rel:slave"=>"ad_nm", "rel:master"=>"ad"');
INSERT INTO attributes VALUES (30, '"cat:ad_fc"=>"1"');
INSERT INTO attributes VALUES (31, '"rel:role"=>"part", "rel:slave"=>"ad_fc", "rel:master"=>"ad"');
INSERT INTO attributes VALUES (32, '"rel:role"=>"part", "rel:slave"=>"ad_el", "rel:master"=>"ad_fc"');
INSERT INTO attributes VALUES (33, '"cat:addr_nm"=>"1", "addr_nm:lang"=>"ru", "addr_nm:name"=>"1", "addr_nm:is_local"=>"1"');
INSERT INTO attributes VALUES (34, '"cat:addr_nm"=>"1", "addr_nm:lang"=>"ru", "addr_nm:name"=>"2", "addr_nm:is_local"=>"1"');
INSERT INTO attributes VALUES (35, '"cat:cond"=>"1", "cond:access_id"=>"31", "cond:cond_type"=>"1"');
INSERT INTO attributes VALUES (36, '"rel:role"=>"to", "rel:slave"=>"rd_el", "rel:master"=>"cond", "rel:seq_num"=>"1"');


--
-- Name: attributes_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('attributes_id_seq', 36, true);


--
-- Data for Name: branch; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO branch VALUES (1, '2016-01-25 15:53:04.312935+03', 208826301, NULL, 0, NULL, 'normal', 'approved');
INSERT INTO branch VALUES (0, '2016-01-25 14:24:52.830581+03', 0, NULL, 0, NULL, 'normal', 'trunk');
INSERT INTO branch VALUES (2, '2016-01-25 16:02:34.432608+03', 208826301, '2016-01-25 16:20:32.250065+03', 208826301, NULL, 'normal', 'archive');
INSERT INTO branch VALUES (3, '2016-01-25 16:20:46.516909+03', 208826301, '2016-01-25 17:08:48.506746+03', 208826301, NULL, 'normal', 'archive');
INSERT INTO branch VALUES (4, '2016-01-25 17:08:52.949331+03', 208826301, NULL, 0, NULL, 'normal', 'stable');


--
-- Name: branch_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('branch_id_seq', 4, true);


--
-- Data for Name: commit; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO commit VALUES (2, 'approved', true, '2016-01-25 15:10:33.660905+03', 208826301, '"action"=>"object-created", "edit_notes:11"=>"created,modified-relations-masters-added-created-rd_el", "edit_notes:12"=>"created,modified-relations-masters-added-created-rd_el", "edit_notes:13"=>"created", "primary_object:13"=>"1"', 2, 1);
INSERT INTO commit VALUES (3, 'approved', true, '2016-01-25 15:39:49.308139+03', 208826301, '"action"=>"object-created", "edit_notes:12"=>"modified-relations-masters-added-created-rd_el", "edit_notes:21"=>"created,modified-relations-masters-added-created-rd_el", "edit_notes:22"=>"created", "primary_object:22"=>"1"', 2, 1);
INSERT INTO commit VALUES (4, 'approved', true, '2016-01-25 15:43:20.665994+03', 208826301, '"action"=>"object-created", "edit_notes:13"=>"modified-relations-masters-added-created-rd", "edit_notes:22"=>"modified-relations-masters-added-created-rd", "edit_notes:31"=>"created", "primary_object:31"=>"1"', 2, 1);
INSERT INTO commit VALUES (5, 'approved', true, '2016-01-25 15:44:10.277327+03', 208826301, '"action"=>"object-created", "edit_notes:12"=>"modified-relations-masters-added-created-cond", "edit_notes:13"=>"modified-relations-masters-added-created-cond", "edit_notes:22"=>"modified-relations-masters-added-created-cond", "edit_notes:41"=>"created", "primary_object:41"=>"1"', 2, 1);
INSERT INTO commit VALUES (6, 'approved', true, '2016-01-25 15:45:53.965486+03', 208826301, '"action"=>"object-created", "edit_notes:51"=>"created,modified-relations-masters-added-associated_with-rd", "primary_object:51"=>"1"', 2, 1);
INSERT INTO commit VALUES (7, 'approved', true, '2016-01-25 15:46:25.524017+03', 208826301, '"action"=>"object-created", "edit_notes:61"=>"created,modified-relations-masters-added-created-ad_el", "edit_notes:62"=>"created", "primary_object:62"=>"1"', 2, 1);
INSERT INTO commit VALUES (8, 'approved', true, '2016-01-25 15:48:10.831521+03', 208826301, '"action"=>"object-created", "edit_notes:71"=>"created", "primary_object:71"=>"1"', 2, 1);
INSERT INTO commit VALUES (9, 'approved', true, '2016-01-25 15:48:15.381885+03', 208826301, '"action"=>"object-created", "edit_notes:62"=>"modified-relations-masters-added-created-ad_fc", "edit_notes:71"=>"modified-geometry-contours-added", "edit_notes:81"=>"created,modified-relations-masters-added-part-ad", "primary_object:81"=>"1"', 2, 1);
INSERT INTO commit VALUES (10, 'approved', false, '2016-01-25 16:18:00.431774+03', 208826301, '"action"=>"object-modified", "edit_notes:13"=>"modified-relations-masters-added-to-cond", "edit_notes:22"=>"modified-relations-masters-removed-to-cond", "edit_notes:41"=>"modified-geometry-elements-added,modified-geometry-elements-removed", "primary_object:41"=>"1"', 2, 0);
INSERT INTO commit VALUES (11, 'approved', false, '2016-01-25 16:18:11.685777+03', 208826301, '"action"=>"object-deleted", "edit_notes:12"=>"modified-relations-masters-removed-start-rd_el", "edit_notes:21"=>"deleted,modified-relations-masters-removed-end-rd_el", "edit_notes:22"=>"deleted,modified-relations-masters-removed-part-rd", "edit_notes:31"=>"modified-geometry-elements-removed", "primary_object:22"=>"1"', 2, 0);
INSERT INTO commit VALUES (12, 'approved', true, '2016-01-25 16:18:41.450521+03', 208826301, '"action"=>"object-modified", "edit_notes:13"=>"modified-relations-masters-added-to-cond", "edit_notes:22"=>"modified-relations-masters-removed-to-cond", "edit_notes:41"=>"modified-geometry-elements-added,modified-geometry-elements-removed", "primary_object:41"=>"1"', 3, 2);
INSERT INTO commit VALUES (13, 'approved', false, '2016-01-25 16:21:07.8296+03', 208826301, '"action"=>"object-deleted", "edit_notes:12"=>"modified-relations-masters-removed-start-rd_el", "edit_notes:21"=>"deleted,modified-relations-masters-removed-end-rd_el", "edit_notes:22"=>"deleted,modified-relations-masters-removed-part-rd", "edit_notes:31"=>"modified-geometry-elements-removed", "primary_object:22"=>"1"', 3, 0);
INSERT INTO commit VALUES (14, 'approved', false, '2016-01-25 16:23:59.769185+03', 208826301, '"action"=>"object-created", "edit_notes:11"=>"modified-relations-masters-added-created-rd_el", "edit_notes:111"=>"created,modified-relations-masters-added-created-rd_el", "edit_notes:112"=>"created", "primary_object:112"=>"1"', 3, 0);
INSERT INTO commit VALUES (15, 'approved', false, '2016-01-25 16:24:31.794501+03', 208826301, '"action"=>"object-modified", "edit_notes:31"=>"modified-geometry-elements-added", "edit_notes:112"=>"modified-relations-masters-added-part-rd", "primary_object:31"=>"1"', 3, 0);
INSERT INTO commit VALUES (16, 'approved', false, '2016-01-25 17:05:07.725527+03', 208826301, '"action"=>"object-modified", "edit_notes:41"=>"modified-attributes-other", "primary_object:41"=>"1"', 3, 0);
INSERT INTO commit VALUES (17, 'approved', false, '2016-01-25 17:05:20.35045+03', 208826301, '"action"=>"object-modified", "edit_notes:51"=>"modified-attributes-names-official", "primary_object:51"=>"1"', 3, 0);
INSERT INTO commit VALUES (18, 'approved', false, '2016-01-25 17:05:57.123753+03', 208826301, '"action"=>"object-modified", "edit_notes:51"=>"modified-attributes-names-official", "primary_object:51"=>"1"', 2, 0);
INSERT INTO commit VALUES (19, 'approved', false, '2016-01-25 17:09:34.931572+03', 208826301, '"action"=>"object-modified", "edit_notes:62"=>"modified-geometry", "edit_notes:71"=>"modified-geometry-contours", "edit_notes:81"=>"modified-geometry-elements", "primary_object:62"=>"1"', 4, 0);
INSERT INTO commit VALUES (20, 'approved', false, '2016-01-25 18:25:30.277499+03', 208826301, '"action"=>"object-modified", "edit_notes:51"=>"modified-geometry", "primary_object:51"=>"1"', 3, 0);
INSERT INTO commit VALUES (21, 'approved', false, '2016-01-26 14:53:54.178487+03', 208826301, '"action"=>"object-modified", "edit_notes:41"=>"modified-attributes-other", "primary_object:41"=>"1"', 3, 0);
INSERT INTO commit VALUES (22, 'approved', false, '2016-01-28 12:38:19.387587+03', 208826301, '"action"=>"object-modified", "edit_notes:51"=>"modified-relations-masters-removed-associated_with-rd", "primary_object:51"=>"1"', 2, 0);
INSERT INTO commit VALUES (23, 'approved', false, '2016-01-28 12:38:30.519551+03', 208826301, '"action"=>"object-modified", "edit_notes:51"=>"modified-relations-masters-removed-associated_with-rd", "primary_object:51"=>"1"', 3, 0);
INSERT INTO commit VALUES (24, 'approved', false, '2016-01-28 12:38:36.105668+03', 208826301, '"action"=>"object-modified", "edit_notes:51"=>"modified-relations-masters-added-associated_with-rd", "primary_object:51"=>"1"', 3, 0);
INSERT INTO commit VALUES (25, 'approved', false, '2016-01-28 12:38:40.53041+03', 208826301, '"action"=>"object-modified", "edit_notes:51"=>"modified-relations-masters-removed-associated_with-rd", "primary_object:51"=>"1"', 3, 0);
INSERT INTO commit VALUES (26, 'approved', false, '2016-02-22 11:58:27.502578+03', 208826301, '"action"=>"object-modified", "edit_notes:41"=>"modified-geometry-elements-added", "edit_notes:112"=>"modified-relations-masters-added-to-cond", "primary_object:41"=>"1"', 3, 0);


--
-- Name: commit_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('commit_id_seq', 26, true);


--
-- Data for Name: description; Type: TABLE DATA; Schema: revision; Owner: mapspro
--



--
-- Name: description_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('description_id_seq', 1, false);


--
-- Data for Name: geometry; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO geometry VALUES (4, '0101000020430D0000BA96065BFBF14F4164B4D702F8825C41');
INSERT INTO geometry VALUES (5, '0101000020430D00002EBCAC7E52F54F414280DDADE9825C41');
INSERT INTO geometry VALUES (6, '0102000020430D000002000000BA96065BFBF14F4164B4D702F8825C412EBCAC7E52F54F414280DDADE9825C41');
INSERT INTO geometry VALUES (7, '0101000020430D000050B8695EC8F74F4116B315E1A8825C41');
INSERT INTO geometry VALUES (8, '0102000020430D0000020000002EBCAC7E52F54F414280DDADE9825C4150B8695EC8F74F4116B315E1A8825C41');
INSERT INTO geometry VALUES (9, '0101000020430D0000FFB3B8592AF54F412010F6DB58825C41');
INSERT INTO geometry VALUES (10, '0101000020430D0000B745BF6298F44F41712404BD64825C41');
INSERT INTO geometry VALUES (11, '0102000020430D000004000000B745BF6298F44F41712404BD64825C4181E2F44F50F54F4171680E3CFC815C41A3D38BD4B5F54F419671D48D8B825C41B745BF6298F44F41712404BD64825C41');
INSERT INTO geometry VALUES (12, '0101000020430D0000C04A344411F04F4115518286F8825C41');
INSERT INTO geometry VALUES (13, '0102000020430D000002000000BA96065BFBF14F4164B4D702F8825C41C04A344411F04F4115518286F8825C41');
INSERT INTO geometry VALUES (14, '0102000020430D000004000000B745BF6298F44F41712404BD64825C41DCE24CE0DCF34F41E8F080491B825C410772CB43DFF34F415A81EE109E825C41B745BF6298F44F41712404BD64825C41');
INSERT INTO geometry VALUES (15, '0101000020430D0000D0C4EE0A41F54F41ACE3144358825C41');


--
-- Name: geometry_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('geometry_id_seq', 15, true);


--
-- Name: object_id_seq; Type: SEQUENCE SET; Schema: revision; Owner: mapspro
--

SELECT pg_catalog.setval('object_id_seq', 170, true);


--
-- Data for Name: object_revision; Type: TABLE DATA; Schema: revision; Owner: mapspro
--



--
-- Data for Name: object_revision_relation; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO object_revision_relation VALUES (14, 2, 0, 0, false, 0, 7, 0, 13, 11);
INSERT INTO object_revision_relation VALUES (15, 2, 0, 0, false, 0, 8, 0, 13, 12);
INSERT INTO object_revision_relation VALUES (33, 4, 0, 0, false, 0, 11, 0, 31, 32);
INSERT INTO object_revision_relation VALUES (34, 4, 0, 0, false, 0, 12, 0, 31, 13);
INSERT INTO object_revision_relation VALUES (44, 5, 0, 0, false, 0, 16, 0, 41, 13);
INSERT INTO object_revision_relation VALUES (46, 5, 0, 0, false, 0, 18, 0, 41, 12);
INSERT INTO object_revision_relation VALUES (54, 6, 0, 0, false, 0, 22, 0, 31, 51);
INSERT INTO object_revision_relation VALUES (63, 7, 0, 0, false, 0, 25, 0, 62, 61);
INSERT INTO object_revision_relation VALUES (64, 7, 0, 0, false, 0, 26, 0, 62, 61);
INSERT INTO object_revision_relation VALUES (73, 8, 0, 0, false, 0, 29, 0, 71, 72);
INSERT INTO object_revision_relation VALUES (82, 9, 0, 0, false, 0, 31, 0, 71, 81);
INSERT INTO object_revision_relation VALUES (83, 9, 0, 0, false, 0, 32, 0, 81, 62);
INSERT INTO object_revision_relation VALUES (45, 10, 5, 0, true, 0, 17, 0, 41, 22);
INSERT INTO object_revision_relation VALUES (91, 10, 0, 0, false, 0, 17, 0, 41, 13);
INSERT INTO object_revision_relation VALUES (23, 11, 3, 0, true, 0, 7, 0, 22, 12);
INSERT INTO object_revision_relation VALUES (24, 11, 3, 0, true, 0, 8, 0, 22, 21);
INSERT INTO object_revision_relation VALUES (35, 11, 4, 0, true, 0, 12, 0, 31, 22);
INSERT INTO object_revision_relation VALUES (45, 12, 5, 0, true, 0, 17, 0, 41, 22);
INSERT INTO object_revision_relation VALUES (101, 12, 0, 0, false, 0, 17, 0, 41, 13);
INSERT INTO object_revision_relation VALUES (45, 5, 0, 12, false, 0, 17, 0, 41, 22);
INSERT INTO object_revision_relation VALUES (23, 13, 3, 0, true, 0, 7, 0, 22, 12);
INSERT INTO object_revision_relation VALUES (24, 13, 3, 0, true, 0, 8, 0, 22, 21);
INSERT INTO object_revision_relation VALUES (35, 13, 4, 0, true, 0, 12, 0, 31, 22);
INSERT INTO object_revision_relation VALUES (113, 14, 0, 0, false, 0, 7, 0, 112, 11);
INSERT INTO object_revision_relation VALUES (114, 14, 0, 0, false, 0, 8, 0, 112, 111);
INSERT INTO object_revision_relation VALUES (121, 15, 0, 0, false, 0, 12, 0, 31, 112);
INSERT INTO object_revision_relation VALUES (43, 16, 5, 0, true, 0, 15, 0, 41, 42);
INSERT INTO object_revision_relation VALUES (53, 17, 6, 0, true, 0, 21, 0, 51, 52);
INSERT INTO object_revision_relation VALUES (132, 17, 0, 0, false, 0, 21, 0, 51, 131);
INSERT INTO object_revision_relation VALUES (53, 18, 6, 0, true, 0, 21, 0, 51, 52);
INSERT INTO object_revision_relation VALUES (142, 18, 0, 0, false, 0, 21, 0, 51, 141);
INSERT INTO object_revision_relation VALUES (53, 6, 0, 0, false, 0, 21, 0, 51, 52);
INSERT INTO object_revision_relation VALUES (43, 5, 0, 0, false, 0, 15, 0, 41, 42);
INSERT INTO object_revision_relation VALUES (23, 3, 0, 0, false, 0, 7, 0, 22, 12);
INSERT INTO object_revision_relation VALUES (24, 3, 0, 0, false, 0, 8, 0, 22, 21);
INSERT INTO object_revision_relation VALUES (35, 4, 0, 0, false, 0, 12, 0, 31, 22);
INSERT INTO object_revision_relation VALUES (54, 22, 6, 0, true, 0, 22, 0, 31, 51);
INSERT INTO object_revision_relation VALUES (54, 23, 6, 0, true, 0, 22, 0, 31, 51);
INSERT INTO object_revision_relation VALUES (151, 24, 0, 0, false, 0, 22, 0, 31, 51);
INSERT INTO object_revision_relation VALUES (151, 25, 24, 0, true, 0, 22, 0, 31, 51);
INSERT INTO object_revision_relation VALUES (161, 26, 0, 0, false, 0, 36, 0, 41, 112);


--
-- Data for Name: object_revision_with_geometry; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO object_revision_with_geometry VALUES (11, 2, 0, 0, false, 4, 5, 0, 0, 0);
INSERT INTO object_revision_with_geometry VALUES (12, 2, 0, 0, false, 5, 5, 0, 0, 0);
INSERT INTO object_revision_with_geometry VALUES (13, 2, 0, 0, false, 6, 6, 0, 0, 0);
INSERT INTO object_revision_with_geometry VALUES (51, 6, 0, 0, false, 9, 19, 0, 0, 0);
INSERT INTO object_revision_with_geometry VALUES (61, 7, 0, 0, false, 10, 23, 0, 0, 0);
INSERT INTO object_revision_with_geometry VALUES (21, 11, 3, 0, true, 7, 5, 0, 0, 0);
INSERT INTO object_revision_with_geometry VALUES (22, 11, 3, 0, true, 8, 6, 0, 0, 0);
INSERT INTO object_revision_with_geometry VALUES (21, 13, 3, 0, true, 7, 5, 0, 0, 0);
INSERT INTO object_revision_with_geometry VALUES (22, 13, 3, 0, true, 8, 6, 0, 0, 0);
INSERT INTO object_revision_with_geometry VALUES (111, 14, 0, 0, false, 12, 5, 0, 0, 0);
INSERT INTO object_revision_with_geometry VALUES (112, 14, 0, 0, false, 13, 6, 0, 0, 0);
INSERT INTO object_revision_with_geometry VALUES (21, 3, 0, 0, false, 7, 5, 0, 0, 0);
INSERT INTO object_revision_with_geometry VALUES (22, 3, 0, 0, false, 8, 6, 0, 0, 0);
INSERT INTO object_revision_with_geometry VALUES (62, 7, 0, 0, false, 11, 24, 0, 0, 0);
INSERT INTO object_revision_with_geometry VALUES (62, 19, 7, 0, false, 14, 24, 0, 0, 0);
INSERT INTO object_revision_with_geometry VALUES (51, 20, 6, 0, false, 15, 19, 0, 0, 0);


--
-- Data for Name: object_revision_without_geometry; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO object_revision_without_geometry VALUES (31, 4, 0, 0, false, 0, 9, 0, 0, 0);
INSERT INTO object_revision_without_geometry VALUES (32, 4, 0, 0, false, 0, 10, 0, 0, 0);
INSERT INTO object_revision_without_geometry VALUES (41, 5, 0, 0, false, 0, 13, 0, 0, 0);
INSERT INTO object_revision_without_geometry VALUES (71, 8, 0, 0, false, 0, 27, 0, 0, 0);
INSERT INTO object_revision_without_geometry VALUES (72, 8, 0, 0, false, 0, 28, 0, 0, 0);
INSERT INTO object_revision_without_geometry VALUES (81, 9, 0, 0, false, 0, 30, 0, 0, 0);
INSERT INTO object_revision_without_geometry VALUES (42, 16, 5, 0, true, 0, 14, 0, 0, 0);
INSERT INTO object_revision_without_geometry VALUES (52, 17, 6, 0, true, 0, 20, 0, 0, 0);
INSERT INTO object_revision_without_geometry VALUES (131, 17, 0, 0, false, 0, 33, 0, 0, 0);
INSERT INTO object_revision_without_geometry VALUES (52, 18, 6, 0, true, 0, 20, 0, 0, 0);
INSERT INTO object_revision_without_geometry VALUES (141, 18, 0, 0, false, 0, 34, 0, 0, 0);
INSERT INTO object_revision_without_geometry VALUES (52, 6, 0, 0, false, 0, 20, 0, 0, 0);
INSERT INTO object_revision_without_geometry VALUES (42, 5, 0, 0, false, 0, 14, 0, 0, 0);
INSERT INTO object_revision_without_geometry VALUES (41, 21, 5, 0, false, 0, 35, 0, 0, 0);

-- FIX FIX --
INSERT INTO attributes_relations SELECT * FROM attributes WHERE id IN (SELECT attributes_id FROM object_revision_relation);

--
-- PostgreSQL database dump complete
--

