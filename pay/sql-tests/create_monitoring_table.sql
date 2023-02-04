--liquibase formatted sql

--changeset spirit-1984:t_monitoring_result_ddl context:local

create table DS.T_MONITOR_QUERY_RESULT
(
    SERVICE  VARCHAR2(500)  not null,
    LAST_RUN TIMESTAMP(6)   not null,
    QUERY_ID NUMBER         not null,
    STATUS   NUMBER         not null,
    DESCR    VARCHAR2(2000) not null,
    METRIC   NUMBER
);
