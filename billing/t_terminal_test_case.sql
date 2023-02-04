--liquibase formatted sql

--changeset fellow:TRUST-9925
CREATE TABLE meta.t_terminal_test_case (
	terminal_id NUMBER(*,0) NOT NULL,
	test_case_id NUMBER(*,0) NOT NULL,

	PRIMARY KEY (terminal_id, test_case_id),
	FOREIGN KEY(test_case_id) REFERENCES meta.t_test_case (id),
	FOREIGN KEY(terminal_id) REFERENCES meta.t_terminal (id)
);


GRANT SELECT,INSERT,UPDATE,DELETE ON meta.t_terminal_test_case TO django_fraud;
