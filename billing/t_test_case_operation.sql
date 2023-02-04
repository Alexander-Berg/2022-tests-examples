--liquibase formatted sql

--changeset fellow:TRUST-9925
CREATE SEQUENCE meta.s_test_case_operation_id;

CREATE TABLE meta.t_test_case_operation (
	id NUMBER(*,0) DEFAULT meta.s_test_case_operation_id.nextval NOT NULL,
	start_dt DATE DEFAULT sysdate,
	finish_dt DATE DEFAULT sysdate,
	test_case_id NUMBER(*,0) NOT NULL,
	test_suite_operation_id NUMBER(*,0) NOT NULL,
	purchase_token VARCHAR(40),
	return_path VARCHAR(200),
	error VARCHAR(1024),
    info CLOB CHECK (info IS JSON),
    status VARCHAR(512),

	PRIMARY KEY (id),
	FOREIGN KEY(test_case_id) REFERENCES meta.t_test_case (id),
	FOREIGN KEY(test_suite_operation_id) REFERENCES meta.t_test_suite_operation (id)
);


GRANT SELECT,INSERT,UPDATE ON meta.t_test_case_operation TO django_fraud;
GRANT SELECT,ALTER ON meta.s_test_case_operation_id TO django_fraud;
