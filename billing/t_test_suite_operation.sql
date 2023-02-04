--liquibase formatted sql

--changeset fellow:TRUST-9925
CREATE SEQUENCE meta.s_test_suite_operation_id;

CREATE TABLE meta.t_test_suite_operation (
    id NUMBER(*,0) DEFAULT meta.s_test_suite_operation_id.nextval NOT NULL,
    start_dt DATE DEFAULT sysdate,
	finish_dt DATE DEFAULT sysdate,
	passport_uid NUMBER(*,0) NOT NULL,
	operator_uid NUMBER(*,0),
	terminal_id NUMBER(*,0) NOT NULL,
	op_data CLOB CHECK (op_data IS JSON),
	status VARCHAR(512),

	PRIMARY KEY (id),
	FOREIGN KEY(terminal_id) REFERENCES meta.t_terminal (id),
	FOREIGN KEY(passport_uid) REFERENCES meta.t_terminal_test_uid (passport_id)
);


GRANT SELECT,INSERT,UPDATE ON meta.t_test_suite_operation TO django_fraud;
GRANT SELECT, ALTER ON meta.s_test_suite_operation_id TO django_fraud;
