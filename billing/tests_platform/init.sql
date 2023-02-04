create user bo IDENTIFIED by bo_pwd;
GRANT CONNECT, RESOURCE, DBA TO bo;

create sequence bo.s_ar_run;

create table  bo.t_ar_run (
    id              number  not null
  , run_dt          date    not null
  , insert_dt       date    not null
  , start_dt        date
  , finish_dt       date
  , constraint t_ar_run_pk primary key (id)
);


create sequence bo.s_ar_paid_periods
  start with 1
  increment by 1
  nocache
  nocycle
;


create table bo.t_ar_paid_periods (
      id                number          not null
    , contract_id       number          not null
    , discount_type     number          not null
    , from_dt           date            not null
    , paid_dt           date            not null
    , constraint pk_ar_paid_periods primary key (id)
);



CREATE TABLE bo.t_comm_estate_src (
	contract_id NUMBER NOT NULL,
	contract_eid VARCHAR2(64 CHAR) NOT NULL,
	from_dt DATE NOT NULL,
	till_dt DATE NOT NULL,
	nds NUMBER NOT NULL,
	currency VARCHAR2(16 CHAR) NOT NULL,
	discount_type NUMBER NOT NULL,
	reward_type NUMBER NOT NULL,
	turnover_to_charge NUMBER,
	reward_to_charge NUMBER,
	turnover_to_pay NUMBER,
	turnover_to_pay_w_nds NUMBER,
	reward_to_pay NUMBER,
	reward_to_pay_src NUMBER,
	insert_dt DATE
);


CREATE TABLE bo.t_comm_base_src (
	contract_id NUMBER NOT NULL,
	contract_eid VARCHAR2(64 CHAR) NOT NULL,
	from_dt DATE NOT NULL,
	till_dt DATE NOT NULL,
	nds NUMBER NOT NULL,
	currency VARCHAR2(16 CHAR) NOT NULL,
	discount_type NUMBER NOT NULL,
	reward_type NUMBER NOT NULL,
	turnover_to_charge NUMBER,
	reward_to_charge NUMBER,
	turnover_to_pay NUMBER,
	turnover_to_pay_w_nds NUMBER,
	reward_to_pay NUMBER,
	reward_to_pay_src NUMBER,
	insert_dt DATE
);
