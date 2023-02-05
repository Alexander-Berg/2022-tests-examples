INSERT INTO revision.commit (created_by, created, trunk, attributes)
VALUES (1, '2015-01-01 09:00:00+00', TRUE, 'action=>object-created,edit_notes:10=>created,primary_object:10=>1'::hstore);

INSERT INTO revision.commit (created_by, created, trunk, attributes)
VALUES (1, '2015-01-02 09:00:00+00', TRUE, 'edit_notes:20=>modified-geometry,primary_object:20=>1'::hstore);

INSERT INTO revision.commit (created_by, created, trunk, attributes)
VALUES (1, '2015-01-03 09:00:00+00', TRUE, 'action=>object-modified,edit_notes:30=>modified-relations-masters-added-bld_assigned-poi_auto,primary_object:30=>1'::hstore);

INSERT INTO revision.commit (created_by, created, trunk, attributes)
VALUES (1, '2015-01-04 09:00:00+00', TRUE, 'action=>object-created,edit_notes:40=>created,edit_notes:41=>"created,created-split,modified-relations-masters-added-ln_part-hydro_ln",edit_notes:42=>"created,modified-relations-masters-added-created-hydro_ln_el",edit_notes:43=>"created,modified-relations-masters-added-created-hydro_ln_el,modified-relations-masters-added-start-hydro_ln_el",edit_notes:44=>"modified-geometry,modified-split",edit_notes:45=>"modified-geometry-elements,modified-geometry-elements-added",edit_notes:46=>"modified-relations-masters-added-created-hydro_ln_el,modified-relations-masters-removed-start-hydro_ln_el",primary_object:41=>1'::hstore);

INSERT INTO revision.commit (created_by, created, trunk, attributes)
VALUES (1, '2015-01-05 09:00:00+00', TRUE, 'action=>object-modified,edit_notes:50=>modified-geometry-elements-added,edit_notes:51=>modified-relations-masters-added-part-transport_railway,edit_notes:52=>modified-relations-masters-added-part-transport_railway,edit_notes:53=>modified-relations-masters-added-part-transport_railway,edit_notes:54=>modified-relations-masters-added-part-transport_railway,
edit_notes:55=>modified-relations-masters-added-part-transport_railway,primary_object:50=>1'::hstore);

INSERT INTO revision.commit (created_by, created, trunk, attributes)
VALUES (1, '2015-01-06 09:00:00+00', TRUE, 'action=>group-moved,edit_notes:61=>modified-geometry,edit_notes:62=>modified-geometry'::hstore);

INSERT INTO revision.commit (created_by, created, trunk, attributes)
VALUES (1, '2015-01-07 09:00:00+00', TRUE, 'action=>group-modified-attributes,edit_notes:71=>modified-attributes-other,edit_notes:72=>modified-attributes-other,edit_notes:73=>modified-attributes-other'::hstore);
