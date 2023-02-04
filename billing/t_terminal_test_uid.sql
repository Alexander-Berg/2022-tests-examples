--liquibase formatted sql

--changeset fellow:TRUST-9925
CREATE TABLE meta.t_terminal_test_uid (
	passport_id NUMBER(*,0) NOT NULL,
	oauth_secret VARCHAR(50),
	card_secret VARCHAR(50),

	PRIMARY KEY (passport_id)
);


GRANT SELECT,INSERT,UPDATE ON meta.t_terminal_test_uid TO django_fraud;

