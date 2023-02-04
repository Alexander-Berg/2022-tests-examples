DROP SCHEMA IF EXISTS renderer_autotest CASCADE;
CREATE SCHEMA renderer_autotest AUTHORIZATION postgres;
GRANT ALL ON SCHEMA renderer_autotest TO postgres;

DROP TABLE IF EXISTS renderer_autotest.ExtentsLocker;
CREATE TABLE renderer_autotest.ExtentsLocker
(
	locktime timestamp,
	bbox geometry
) WITH OIDS;

DROP TABLE IF EXISTS renderer_autotest.wiki_streets;
DROP SEQUENCE IF EXISTS renderer_autotest.wiki_streets_seq;
DROP INDEX IF EXISTS renderer_autotest.street_index;

CREATE SEQUENCE renderer_autotest.wiki_streets_seq;

CREATE TABLE renderer_autotest.wiki_streets
(
	id integer NOT NULL DEFAULT nextval('renderer_autotest.wiki_streets_seq'::regclass),
	layer_id integer,
	screen_label character varying(256),
	render_label character varying(256),
	"type" character varying(256),
	state integer NOT NULL DEFAULT 0,
	cur_revision_id integer NOT NULL DEFAULT 0,
	prev_revision_id integer NOT NULL DEFAULT 0,
	approve_revision_id integer NOT NULL DEFAULT 0,
	the_geom geometry,
	zmin integer NOT NULL DEFAULT 0,
	zmax integer,
	modified timestamp with time zone,
	length double precision NOT NULL DEFAULT 0,
	start_jc_id bigint,
	end_jc_id bigint,
	line_id bigint,
	start_zorder integer NOT NULL DEFAULT 0,
	end_zorder integer NOT NULL DEFAULT 0,
	direction integer NOT NULL DEFAULT 0,
	CONSTRAINT objects_l_elements10_pkey PRIMARY KEY (id),
	CONSTRAINT enforce_dims_the_geom CHECK (ndims(the_geom) = 2),
	CONSTRAINT enforce_srid_the_geom CHECK (srid(the_geom) = 3395)
);
CREATE INDEX street_index ON renderer_autotest.wiki_streets USING gist(the_geom);

INSERT INTO 
	renderer_autotest.wiki_streets(
		id, layer_id, screen_label, render_label, 
		type, state, cur_revision_id, prev_revision_id, 
		approve_revision_id, the_geom, zmin, zmax, 
		modified, length, start_jc_id, end_jc_id, 
		line_id, start_zorder, end_zorder, direction)
	VALUES(
		3766699,4,'улица Елены Колесовой','улица Елены Колесовой','secondary',1,2846663,2763349,2846663,
		'0102000020430D0000080000006B0274A138CF4F41FF6378D62D745C41F64C6C9AEECE4F41CD4204E846745C41F9CFD773D9CE4F41FB5214714E745C41E2013EF0D1CE4F41F800892651745C41C45B948ACDCE4F419462D3DF52745C4193743842ADCE4F4160571F6B60745C41C1DA54E681CE4F410B334BE372745C41CC9AE13C64CE4F412F63E4707E745C41',13,23,'2009-10-20 15:30:54.989896+04',
		0,3818339,3809672,3850142,0,0,0);

INSERT INTO 
	renderer_autotest.wiki_streets(
		id, layer_id, screen_label, render_label, 
		type, state, cur_revision_id, prev_revision_id, 
		approve_revision_id, the_geom, zmin, zmax, 
		modified, length, start_jc_id, end_jc_id, 
		line_id, start_zorder, end_zorder, direction)
	VALUES(
		3771057,4,'улица Марии Поливановой','улица Марии Поливановой','secondary',1,2846651,2846651,2846651,
		'0102000020430D000006000000DEF49CA8B9CE4F41A55F0781B1745C41725A5D2EBFCE4F411D36059AB0745C413972E896C9CE4F4108B66F44AC745C41E4A0F1EA1FCF4F41A3A8E70A83745C411FE3C8EE27CF4F41C783A64C7F745C416A4C839374CF4F41A7091CA45B745C41',
		13,23,'2009-10-20 15:42:13.219856+04',0,3809673,3820992,3850135,0,0,0);

INSERT INTO 
	renderer_autotest.wiki_streets(
		id, layer_id, screen_label, render_label, 
		type, state, cur_revision_id, prev_revision_id, 
		approve_revision_id, the_geom, zmin, zmax, 
		modified, length, start_jc_id, end_jc_id, 
		line_id, start_zorder, end_zorder, direction)
	VALUES(
		3800139,4,'Б&amp; Очаковская','Б&amp; Очаковская','secondary',1,2846734,2846734,2846734,
		'0102000020430D000002000000687EF8AD3ACE4F41AC1FD87264745C41CC9AE13C64CE4F412F63E4707E745C41',
		13,23,'2009-10-20 16:03:03.454212+04',0,3809670,3809672,3850210,0,0,0);

INSERT INTO 
	renderer_autotest.wiki_streets(
		id, layer_id, screen_label, render_label, 
		type, state, cur_revision_id, prev_revision_id, 
		approve_revision_id, the_geom, zmin, zmax, 
		modified, length, start_jc_id, end_jc_id, 
		line_id, start_zorder, end_zorder, direction)
	VALUES(
		3800142,4,'Б&amp; Очаковская','Б&amp; Очаковская','secondary',1,2846723,2846723,2846723,
		'0102000020430D000004000000CC9AE13C64CE4F412F63E4707E745C417C6BC9F76ACE4F41A3A8E70A83745C4172F1C096ABCE4F41AB22A4F7A8745C41EB5CB9ABB9CE4F418E24FB7CB1745C41',
		13,23,'2009-10-20 16:02:35.809193+04',0,3809672,3809673,3850210,0,0,0);

INSERT INTO 
	renderer_autotest.wiki_streets(
		id, layer_id, screen_label, render_label, 
		type, state, cur_revision_id, prev_revision_id, 
		approve_revision_id, the_geom, zmin, zmax, 
		modified, length, start_jc_id, end_jc_id, 
		line_id, start_zorder, end_zorder, direction)
	VALUES(
		3800140,4,'Б&amp; Очаковская','','',1,2796790,0,2796790,
		'0102000020430D000003000000DEF49CA8B9CE4F41A55F0781B1745C41370549220CCF4F413C9E8E76E1745C41E4D787F150CF4F41FFE9A11A0E755C41',
		13,23,'2009-10-20 16:02:35.809193+04',0,3809673,3809671,3754084,0,0,0);

INSERT INTO 
	renderer_autotest.wiki_streets(
		id, layer_id, screen_label, render_label, 
		type, state, cur_revision_id, prev_revision_id, 
		approve_revision_id, the_geom, zmin, zmax, 
		modified, length, start_jc_id, end_jc_id, 
		line_id, start_zorder, end_zorder, direction)
	VALUES(
		3771056,4,'улица Марии Поливановой','улица Марии Поливановой','secondary',1,2846702,2846701,2846702,
		'0102000020430D000006000000DEF49CA8B9CE4F41A55F0781B1745C41FECB325FAECE4F418E4E0FB7AF745C417DAB5C22A6CE4F410E8F3174B0745C41C73A8E056ACE4F41A49EC9FDCE745C41A7B258F667CE4F414628CC1FD0745C4176EF47B71FCE4F4160939431F3745C41',13,23,'2009-10-20 15:42:55.562405+04',
		0,3809673,3816952,3850135,1,1,0);
		
INSERT INTO 
	renderer_autotest.wiki_streets(
		id, layer_id, screen_label, render_label, 
		type, state, cur_revision_id, prev_revision_id, 
		approve_revision_id, the_geom, zmin, zmax, 
		modified, length, start_jc_id, end_jc_id, 
		line_id, start_zorder, end_zorder, direction)
	VALUES(
		993771056,4,'улица Мультистринговая','улица Мультистринговая','secondary',1,2846702,2846701,2846702,
		ST_GeomFromText('MULTILINESTRING((4168703 7458962, 4169203 7458962),(4168703 7458862, 4169203 7458862))', 3395),
		13,23,'2009-10-20 15:42:55.562405+04',
		0,3809673,3816952,3850135,1,1,0);

DROP TABLE IF EXISTS renderer_autotest.wiki_streets2;
DROP SEQUENCE IF EXISTS renderer_autotest.wiki_streets2_seq;

CREATE SEQUENCE renderer_autotest.wiki_streets2_seq;

CREATE TABLE renderer_autotest.wiki_streets2
(
	id integer NOT NULL DEFAULT nextval('renderer_autotest.wiki_streets2_seq'::regclass),
	layer_id integer,
	screen_label character varying(256),
	render_label character varying(256),
	"type" character varying(256),
	state integer NOT NULL DEFAULT 0,
	cur_revision_id integer NOT NULL DEFAULT 0,
	prev_revision_id integer NOT NULL DEFAULT 0,
	approve_revision_id integer NOT NULL DEFAULT 0,
	the_geom geometry,
	zmin integer NOT NULL DEFAULT 0,
	zmax integer,
	modified timestamp with time zone,
	length double precision NOT NULL DEFAULT 0,
	start_jc_id bigint,
	end_jc_id bigint,
	line_id bigint,
	start_zorder integer NOT NULL DEFAULT 0,
	end_zorder integer NOT NULL DEFAULT 0,
	direction integer NOT NULL DEFAULT 0,
	CONSTRAINT objects_l_elements10_pkey2 PRIMARY KEY (id),
	CONSTRAINT enforce_dims_the_geom CHECK (ndims(the_geom) = 2),
	CONSTRAINT enforce_srid_the_geom CHECK (srid(the_geom) = 3395)
);

INSERT INTO 
	renderer_autotest.wiki_streets2(
		id, layer_id, screen_label, render_label, 
		type, state, cur_revision_id, prev_revision_id, 
		approve_revision_id, the_geom, zmin, zmax, 
		modified, length, start_jc_id, end_jc_id, 
		line_id, start_zorder, end_zorder, direction)
	VALUES(
		993771056,4,'́у́л́.́ ́Л́а́зо́ревый проезд','улица Мультистринговая','secondary',1,2846702,2846701,2846702,
		ST_GeomFromText('MULTILINESTRING((4168703 7458962, 4168903 7458902, 4169203 7458962),(4168703 7458862, 4169203 7458862))', 3395),
		13,23,'2009-10-20 15:42:55.562405+04',
		0,3809673,3816952,3850135,1,1,0);
		
INSERT INTO 
	renderer_autotest.wiki_streets2(
		id, layer_id, screen_label, render_label, 
		type, state, cur_revision_id, prev_revision_id, 
		approve_revision_id, the_geom, zmin, zmax, 
		modified, length, start_jc_id, end_jc_id, 
		line_id, start_zorder, end_zorder, direction)
	VALUES(
		55555,4,'Лазо́ревый проезд','улица Мультистринговая','secondary',1,2846702,2846701,2846702,
		ST_GeomFromText('POLYGON((4168703 7458262, 4169203 7458262, 4169203 7458162, 4168703 7458162, 4168703 7458262, 4168703 7458262))', 3395),
		13,23,'2009-10-20 15:42:55.562405+04',
		0,3809673,3816952,3850135,1,1,0);

DROP TABLE IF EXISTS renderer_autotest.sample_points;
CREATE TABLE renderer_autotest.sample_points
(
	id bigint,
	label character varying(256),
	the_geom geometry
);

INSERT INTO renderer_autotest.sample_points(id, label, the_geom) VALUES(1, 'Точка10', SetSRID(ST_MakePoint(10, 10), 3395));
INSERT INTO renderer_autotest.sample_points(id, label, the_geom) VALUES(2, 'Точка20', SetSRID(ST_MakePoint(20, 20), 3395));

DROP TABLE IF EXISTS renderer_autotest.testptfc;
DROP SEQUENCE IF EXISTS renderer_autotest.testptfc_sequence;

CREATE SEQUENCE renderer_autotest.testptfc_sequence;

CREATE TABLE renderer_autotest.testptfc
(
  id bigint DEFAULT nextval('renderer_autotest.testptfc_sequence'::regclass),
  containerid bigint,
  bbox geometry,
  labeldata bytea,
  objectid bigint,
  labeltext text, 
  CONSTRAINT testptfc_id_key UNIQUE (id)
)
WITH (OIDS=FALSE);

DROP TABLE IF EXISTS renderer_autotest.testpgversions;
CREATE TABLE renderer_autotest.testpgversions
(
  id bigserial NOT NULL,
  containerid bigint,
  bbox geometry,
  labeldata bytea,
  objectid bigint,
  labeltext text,
  created timestamp with time zone DEFAULT now(),
  CONSTRAINT labels_id_pkey PRIMARY KEY (id)
) WITH (OIDS=FALSE);

-- legacy
INSERT INTO 
	renderer_autotest.testpgversions(
        id, containerid,
        bbox,
        labeldata,
        objectid, labeltext, created)
	VALUES(
        89147, 150515, 
        ST_GeomFromText('POLYGON((5461161.79672565 7319414.51762272,5461228.78961887 7319414.51762272,5461228.78961887 7319472.6658349,5461161.79672565 7319472.6658349,5461161.79672565 7319414.51762272))'),
        E'\\351\\244\\033\\000\\000\\000\\000\\000\\0342l\\2035\\303\\321?\\220\\215\\375r*\\325TA\\016\\273 \\241\\335\\353[A\\233\\035\\2112;\\325TA\\374\\011\\235*\\354\\353[A\\001\\000\\000\\000\\000\\000\\000\\0002\\000\\000\\000\\035\\036\\036\\336\\021D\\235?M\\244\\007\\352\\205S\\271\\277M\\244\\007\\352\\205S\\271?\\035\\036\\036\\336\\021D\\235?\\331k\\340K-\\325TA\\367\\301F\\366\\346\\353[A'::bytea,
        0, null, '2010-06-24 13:49:48.025854+00');

-- version 1
INSERT INTO 
	renderer_autotest.testpgversions(
        id, containerid,
        bbox,
        labeldata,
        objectid, labeltext, created)
	VALUES(
        1, 1, 
        ST_GeomFromText('POLYGON((4082891.88003373 7490498.49396786,4083076.35137752 7490498.49396786,4083076.35137752 7490557.72990966,4082891.88003373 7490557.72990966,4082891.88003373 7490498.49396786))'),
        E'\\001\\226\\312\\362\\3015\\377\\357?\\000\\000\\304\\226\\377[\\272?\\000\\000\\000\\2608nW\\277\\000\\000\\000\\2608nW?\\000\\000\\304\\226\\377[\\272?\\027\\304\\004\\372i&OA\\373\\247\\263\\306\\363\\222\\\\A\\000\\010\\360\\003\\255\\000\\000\\200C\\000\\000\\200\\262\\261\\254\\013.\\000`\\216C\\000\\000\\2002\\261\\254\\013\\256\\000`\\216C\\000\\000\\000\\000\\012\\360\\003-\\000`\\216C\\000\\000\\2002\\012\\360\\003\\255\\000\\000\\200C\\000\\000\\000\\263'::bytea,
        1, 'sadasd', '2010-09-16 09:26:54.585749+00');

DROP INDEX IF EXISTS roads_bbox;
DROP TABLE IF EXISTS renderer_autotest.roads_part_1;
DROP TABLE IF EXISTS renderer_autotest.roads_part_2;
DROP TABLE IF EXISTS renderer_autotest.roads;
DROP SEQUENCE IF EXISTS renderer_autotest.roads_sequence;

CREATE SEQUENCE renderer_autotest.roads_sequence;

CREATE TABLE renderer_autotest.roads
(
  id bigint DEFAULT nextval('renderer_autotest.roads_sequence'::regclass),
  bbox geometry,
  labeldata bytea,
  objectid bigint,
  labeltext text,
  CONSTRAINT roads_id_key UNIQUE (id)
)
WITH (OIDS=FALSE);

CREATE TABLE renderer_autotest.roads_part_1
(
--  id bigint DEFAULT nextval('renderer_autotest.roads_sequence'::regclass),
--  bbox geometry,
--  labeldata bytea,
--  objectid bigint,
--  CONSTRAINT roads_part_1_id_key UNIQUE (id)
)
INHERITS(renderer_autotest.roads)
WITH (OIDS=FALSE);

CREATE INDEX roads_bbox_part_1
  ON renderer_autotest.roads_part_1 USING gist (bbox);

CREATE TABLE renderer_autotest.roads_part_2
(
--  id bigint DEFAULT nextval('renderer_autotest.roads_sequence'::regclass),
--  bbox geometry,
--  labeldata bytea,
--  objectid bigint,
--  CONSTRAINT roads_part_1_id_key UNIQUE (id)
)
INHERITS(renderer_autotest.roads)
WITH (OIDS=FALSE);

CREATE INDEX roads_bbox_part_2
  ON renderer_autotest.roads_part_2 USING gist (bbox);

CREATE OR REPLACE FUNCTION renderer_autotest.label_insert_trigger()
  RETURNS trigger AS
$BODY$
BEGIN   
    IF ( MOD(NEW.id,  2) = 0) THEN
        INSERT INTO renderer_autotest.roads_part_1 VALUES (NEW.*);                                    
    ELSIF ( MOD(NEW.id, 2) = 1) THEN
        INSERT INTO renderer_autotest.roads_part_2 VALUES (NEW.*);                                    
    END IF;        
    RETURN NULL;
END;
$BODY$
  LANGUAGE 'plpgsql' VOLATILE
  COST 100;

CREATE TRIGGER insert_objects_l_elements_trigger
  BEFORE INSERT
  ON renderer_autotest.roads
  FOR EACH ROW
  EXECUTE PROCEDURE renderer_autotest.label_insert_trigger();
