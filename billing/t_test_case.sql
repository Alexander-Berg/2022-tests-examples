--liquibase formatted sql

--changeset fellow:TRUST-9925
CREATE SEQUENCE meta.s_test_case_id;


CREATE TABLE meta.t_test_case (
	id NUMBER(*,0) DEFAULT meta.s_test_case_id.nextval NOT NULL,
	dt DATE DEFAULT sysdate,
	description VARCHAR2(512),
	expected_result CLOB CHECK (expected_result IS JSON),
	scenario CLOB CHECK (scenario IS JSON),
	data CLOB CHECK (data IS JSON),

	PRIMARY KEY (id)
);


GRANT SELECT,INSERT,UPDATE ON meta.t_test_case TO django_fraud;
GRANT SELECT,ALTER ON meta.s_test_case_id TO django_fraud;
