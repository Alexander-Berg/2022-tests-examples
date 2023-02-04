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
-- Data for Name: attributes; Type: TABLE DATA; Schema: revision; Owner: mapspro
--

INSERT INTO attributes VALUES (1, '"cat:rd_jc"=>"1"');
INSERT INTO attributes VALUES (2, '"rd_el:fc"=>"7", "cat:rd_el"=>"1", "rd_el:fow"=>"0", "rd_el:paved"=>"1", "rd_el:f_zlev"=>"0", "rd_el:oneway"=>"B", "rd_el:t_zlev"=>"0", "rd_el:access_id"=>"31", "rd_el:speed_cat"=>"73", "rd_el:struct_type"=>"0"');
INSERT INTO attributes VALUES (3, '"rel:role"=>"start", "rel:slave"=>"rd_jc", "rel:master"=>"rd_el"');
INSERT INTO attributes VALUES (4, '"rel:role"=>"end", "rel:slave"=>"rd_jc", "rel:master"=>"rd_el"');


INSERT INTO geometry VALUES (1, '0101000020430D0000C8AF4A50467452414A1ABC8BF0E05C41');
INSERT INTO geometry VALUES (2, '0101000020430D00002E883B394474524152208E33B0E05C41');
INSERT INTO geometry VALUES (3, '0102000020430D000003000000C8AF4A50467452414A1ABC8BF0E05C41E9A50CCE7C745241E9EFE748D3E05C412E883B394474524152208E33B0E05C41');
INSERT INTO geometry VALUES (4, '0102000020430D00000300000005692F8252745241391D52FFE9E05C41E9A50CCE7C745241E9EFE748D3E05C416336AD1A52745241C2C5E0CEB8E05C41');
INSERT INTO geometry VALUES (5, '0101000020430D000005692F8252745241391D52FFE9E05C41');
INSERT INTO geometry VALUES (6, '0101000020430D00006336AD1A52745241C2C5E0CEB8E05C41');
INSERT INTO geometry VALUES (7, '0102000020430D000002000000C8AF4A50467452414A1ABC8BF0E05C4105692F8252745241391D52FFE9E05C41');
INSERT INTO geometry VALUES (8, '0102000020430D0000020000006336AD1A52745241C2C5E0CEB8E05C412E883B394474524152208E33B0E05C41');
INSERT INTO geometry VALUES (9, '0102000020430D0000020000006336AD1A52745241C2C5E0CEB8E05C4105692F8252745241391D52FFE9E05C41');
INSERT INTO geometry VALUES (10, '0102000020430D0000020000006336AD1A52745241C2C5E0CEB8E05C41FA95F65B527452412576309BDDE05C41');
INSERT INTO geometry VALUES (11, '0101000020430D0000FA95F65B527452412576309BDDE05C41');
INSERT INTO geometry VALUES (12, '0102000020430D000004000000C8AF4A50467452414A1ABC8BF0E05C4105692F8252745241391D52FFE9E05C41E9A50CCE7C745241E9EFE748D3E05C416336AD1A52745241C2C5E0CEB8E05C41');


INSERT INTO commit VALUES (1, 'draft', true, '2015-07-20 14:33:06.000161+03', 127525902, '"action"=>"object-created", "edit_notes:1"=>"created,modified-relations-masters-added-created-rd_el", "edit_notes:2"=>"created,modified-relations-masters-added-created-rd_el", "edit_notes:3"=>"created", "primary_object:3"=>"1"', NULL, 0);

INSERT INTO object_revision VALUES (1, 1, 0, 0, false, 1, 1, 0, 0, 0);
INSERT INTO object_revision VALUES (2, 1, 0, 0, false, 2, 1, 0, 0, 0);
INSERT INTO object_revision VALUES (3, 1, 0, 2, false, 3, 2, 0, 0, 0);
INSERT INTO object_revision VALUES (4, 1, 0, 2, false, 0, 3, 0, 3, 1);
INSERT INTO object_revision VALUES (5, 1, 0, 2, false, 0, 4, 0, 3, 2);

INSERT INTO commit VALUES (2, 'draft', true, '2015-07-20 14:33:27.069254+03', 127525902, '"action"=>"object-created", "edit_notes:1"=>"modified-relations-masters-added-created-rd_el,modified-relations-masters-removed-start-rd_el", "edit_notes:2"=>"modified-relations-masters-added-created-rd_el,modified-relations-masters-removed-end-rd_el", "edit_notes:3"=>"modified-geometry,modified-split", "edit_notes:11"=>"created,modified-relations-masters-added-created-rd_el,modified-relations-masters-added-start-rd_el", "edit_notes:12"=>"created,modified-relations-masters-added-created-rd_el,modified-relations-masters-added-end-rd_el", "edit_notes:13"=>"created,created-split", "edit_notes:18"=>"created,created-split", "edit_notes:21"=>"created", "primary_object:21"=>"1"', NULL, 0);

INSERT INTO object_revision VALUES (3, 2, 1, 4, false, 4, 2, 0, 0, 0);
INSERT INTO object_revision VALUES (4, 2, 1, 0, true, 0, 3, 0, 3, 1);
INSERT INTO object_revision VALUES (5, 2, 1, 0, true, 0, 4, 0, 3, 2);
INSERT INTO object_revision VALUES (11, 2, 0, 4, false, 5, 1, 0, 0, 0);
INSERT INTO object_revision VALUES (12, 2, 0, 0, false, 6, 1, 0, 0, 0);
INSERT INTO object_revision VALUES (13, 2, 0, 4, false, 7, 2, 0, 0, 0);
INSERT INTO object_revision VALUES (14, 2, 0, 4, false, 0, 3, 0, 13, 1);
INSERT INTO object_revision VALUES (15, 2, 0, 4, false, 0, 4, 0, 13, 11);
INSERT INTO object_revision VALUES (16, 2, 0, 4, false, 0, 3, 0, 3, 11);
INSERT INTO object_revision VALUES (17, 2, 0, 0, false, 0, 4, 0, 3, 12);
INSERT INTO object_revision VALUES (18, 2, 0, 0, false, 8, 2, 0, 0, 0);
INSERT INTO object_revision VALUES (19, 2, 0, 0, false, 0, 3, 0, 18, 12);
INSERT INTO object_revision VALUES (20, 2, 0, 0, false, 0, 4, 0, 18, 2);
INSERT INTO object_revision VALUES (21, 2, 0, 3, false, 9, 2, 0, 0, 0);
INSERT INTO object_revision VALUES (22, 2, 0, 0, false, 0, 3, 0, 21, 12);
INSERT INTO object_revision VALUES (23, 2, 0, 3, false, 0, 4, 0, 21, 11);

INSERT INTO commit VALUES (3, 'draft', true, '2015-07-20 14:33:38.494333+03', 127525902, '"action"=>"object-modified", "edit_notes:11"=>"modified-relations-masters-removed-end-rd_el", "edit_notes:21"=>"modified-geometry", "edit_notes:31"=>"created,modified-relations-masters-added-end-rd_el", "primary_object:21"=>"1"', NULL, 0);

INSERT INTO object_revision VALUES (21, 3, 2, 0, false, 10, 2, 0, 0, 0);
INSERT INTO object_revision VALUES (23, 3, 2, 0, true, 0, 4, 0, 21, 11);
INSERT INTO object_revision VALUES (31, 3, 0, 0, false, 11, 1, 0, 0, 0);
INSERT INTO object_revision VALUES (32, 3, 0, 0, false, 0, 4, 0, 21, 31);

INSERT INTO commit VALUES (4, 'draft', true, '2015-07-20 14:33:47.388964+03', 127525902, '"action"=>"object-deleted", "edit_notes:1"=>"modified-relations-masters-added-start-rd_el,modified-relations-masters-removed-start-rd_el", "edit_notes:3"=>"modified-geometry", "edit_notes:11"=>"deleted,modified-relations-masters-removed-end-rd_el,modified-relations-masters-removed-start-rd_el", "edit_notes:13"=>"deleted", "primary_object:11"=>"1"', NULL, 0);

INSERT INTO object_revision VALUES (3, 4, 2, 0, false, 12, 2, 0, 0, 0);
INSERT INTO object_revision VALUES (11, 4, 2, 0, true, 5, 1, 0, 0, 0);
INSERT INTO object_revision VALUES (13, 4, 2, 0, true, 7, 2, 0, 0, 0);
INSERT INTO object_revision VALUES (14, 4, 2, 0, true, 0, 3, 0, 13, 1);
INSERT INTO object_revision VALUES (15, 4, 2, 0, true, 0, 4, 0, 13, 11);
INSERT INTO object_revision VALUES (16, 4, 2, 0, true, 0, 3, 0, 3, 11);
INSERT INTO object_revision VALUES (41, 4, 0, 0, false, 0, 3, 0, 3, 1);

SELECT pg_catalog.setval('approve_order_seq', 1, false);
SELECT pg_catalog.setval('attributes_id_seq', 4, true);
SELECT pg_catalog.setval('branch_id_seq', 1, false);
SELECT pg_catalog.setval('commit_id_seq', 4, true);
SELECT pg_catalog.setval('description_id_seq', 1, false);
SELECT pg_catalog.setval('geometry_id_seq', 12, true);
SELECT pg_catalog.setval('object_id_seq', 50, true);


--
-- PostgreSQL database dump complete
--

