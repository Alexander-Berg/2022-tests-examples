create sequence s_xxxx_new_comm_contract_basic order;

create table xxxx_new_comm_contract_basic
(
  ID NUMBER,
  TEST_CASE_ID NUMBER,
  CONTRACT_ID NUMBER,
  CONTRACT_EID VARCHAR2(16 BYTE),
  CONTRACT_FROM_DT DATE,
  CONTRACT_TILL_DT DATE,
  CONTRACT_COMMISSION_TYPE NUMBER,
  INVOICE_ID NUMBER,
  INVOICE_EID VARCHAR2(16 BYTE),
  INVOICE_DT DATE,
  PAYMENT_TYPE NUMBER,
  COMMISSION_PAYBACK_TYPE NUMBER,
  COMMISSION_TYPE NUMBER,
  DISCOUNT_TYPE NUMBER,
  CURRENCY VARCHAR2(16 BYTE),
  NDS NUMBER,
  NDS_PCT NUMBER,
  INVOICE_FIRST_PAYMENT DATE,
  INVOICE_FIRST_ACT DATE,
  ACT_ID NUMBER,
  HIDDEN NUMBER,
  ACT_DT DATE,
  AMOUNT NUMBER,
  AMOUNT_NDS NUMBER,
  AMOUNT_NSP NUMBER,
  CLIENT_ID NUMBER,
  LOYAL_CLIENT NUMBER,
  cfo varchar2(16 byte),
  REGIONAL_PROGRAM NUMBER,
  COMMISSION_PAYBACK_PCT NUMBER,
  IS_LOYAL NUMBER,
  ENDBUYER_ID NUMBER, 
	ENDBUYER_INN NUMBER, 
	AGENCY_INN NUMBER, 
  PAYSYS_ID NUMBER,
  PRODUCT_ID NUMBER,
  constraint xxxx_new_comm_contract_basic primary key ( id ) enable
);

create sequence s_xxxx_oebs_cash_payment_test order;

create table xxxx_oebs_cash_payment_test
(
  ID NUMBER,
  INVOICE_ID NUMBER,
  OEBS_PAYMENT NUMBER,
  DOC_DATE DATE,
  COMISS_DATE DATE,
  constraint xxxx_oebs_cash_payment_test primary key ( id ) enable
);

create sequence s_xxxx_client_discount_m order;

create table xxxx_client_discount_m
(
  ID NUMBER,
  CLIENT_ID NUMBER,
  CLIENT_AVG_DISCOUNT NUMBER,
  DT DATE,
  constraint xxxx_client_discount_m primary key ( id ) enable
);

create sequence s_xxxx_contract_signed_attr order;

create table xxxx_contract_signed_attr
(
  ID NUMBER,
  contract_id number,
  CODE VARCHAR2 (64 char),
  KEY_NUM NUMBER,
  constraint xxxx_contract_signed_attr primary key ( id ) enable
);

create sequence s_xxxx_loyal_clients_contr_atr order;

create table xxxx_loyal_clients_contr_attr
(
  ID NUMBER,
  contract_id number,
  CLIENT_ID NUMBER,
  LC_TURNOVER NUMBER,
  COLLATERAL_FIRST_DT DATE,
  COLLATERAL_END_DT DATE,
  constraint xxxx_loyal_clients_contr_attr primary key ( id ) enable
);

create sequence s_xxxx_currency_rate order;

create table xxxx_currency_rate
(
  ID NUMBER,
  CC VARCHAR2 (64 char),
  RATE NUMBER,
  RATE_DT DATE,
  constraint xxxx_currency_rate primary key ( id ) enable
);


create table xxxx_commission_part
(
  ID NUMBER,
  CONTRACT_ID number,
  FROM_DT DATE,
  TILL_DT DATE,
  NDS NUMBER,
  CURRENCY VARCHAR2(16 BYTE),
  REWARD_TYPE NUMBER,
  DISCOUNT_TYPE NUMBER ,
  COMMISSION_TYPE NUMBER ,
  TURNOVER_TO_CHARGE NUMBER,
  REWARD_TO_CHARGE NUMBER ,
  DELKREDERE_TO_CHARGE NUMBER ,
  DKV_TO_CHARGE  NUMBER ,
  TURNOVER_TO_PAY_W_NDS NUMBER,
  TURNOVER_TO_PAY  NUMBER,
  REWARD_TO_PAY NUMBER ,
  DELKREDERE_TO_PAY NUMBER,
  DKV_TO_PAY NUMBER ,
  
  constraint xxxx_commission_part primary key ( id ) enable
);

create sequence s_xxxx_commission_part order; 