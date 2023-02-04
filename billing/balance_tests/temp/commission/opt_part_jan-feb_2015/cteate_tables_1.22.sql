create sequence s_xxxx_new_comm_contract_basic order;

--drop table xxxx_new_comm_contract_basic;

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
  INVOICE_SUM NUMBER,
  ACT_ID NUMBER,
  HIDDEN NUMBER,
  ACT_DT DATE,
  AMOUNT NUMBER,
  AMOUNT_NDS NUMBER,
  AMOUNT_NSP NUMBER,
  CLIENT_ID NUMBER,
  AGENCY_ID NUMBER,
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
  PERSON_ID  NUMBER,
  FIRM_ID NUMBER,
  ORDER_ID NUMBER,
  SERVICE_ID NUMBER,
  SERVICE_ORDER_ID	NUMBER,
  CODE  varchar2(32 byte),
  VALUE_NUM NUMBER,
  PLAN_SUM NUMBER,
  PLAN_DT DATE,  
  ACT_EID VARCHAR2(16 BYTE),
  KKM_ID NUMBER,
  KKM_EID VARCHAR2(16 BYTE),
  SUBCLIENT_ID NUMBER,
  BRAND_FINISH_DT DATE,
  ACT_PAYMENT_TERM_DT DATE,
  LS_INVOICE_ID NUMBER,
  MAIN_CLIENT_ID NUMBER,
  constraint xxxx_new_comm_contract_basic primary key ( id ) enable
);


create sequence s_xxxx_act_trans order;

create table xxxx_act_trans
(
  ID NUMBER,
  ACT_ID NUMBER,
  COMMISSION_TYPE NUMBER,
  AMOUNT NUMBER,
  AMOUNT_NDS NUMBER,
  AMOUNT_NSP NUMBER,
  PARENT_ORDER_ID NUMBER,
constraint xxxx_act_trans primary key ( id ) enable
);


create sequence s_xxxx_order order;

create table xxxx_order
(
  ID NUMBER,
  ORDER_ID NUMBER,
  CLIENT_ID NUMBER,
  SERVICE_ID NUMBER,
  SERVICE_ORDER_ID	NUMBER,
constraint xxxx_order primary key ( id ) enable
);

create sequence s_xxxx_invoice order;

create table xxxx_invoice
(
  ID NUMBER,
  INV_ID NUMBER,
  INVOICE_TYPE varchar2(32 byte),
  TOTAL_SUM NUMBER,
  constraint xxxx_invoice primary key ( id ) enable
);

create sequence s_xxxx_ui_contract_apex order;

create table xxxx_ui_contract_apex
(
  ID NUMBER,
  CONTRACT_ID NUMBER,
  START_DT DATE,
  FINISH_DT DATE,
  MAIN_CLIENT_ID NUMBER,
  constraint xxxx_ui_contract_apex primary key ( id ) enable
);

create sequence s_xxxx_commission_reward_2013 order;

create table xxxx_commission_reward_2013
(
ID NUMBER,
REWARD_TYPE	NUMBER,
REWARD_TO_PAY	NUMBER,
TURNOVER_TO_PAY_W_NDS	NUMBER,
TURNOVER_TO_PAY	NUMBER,
REWARD_TO_CHARGE	NUMBER,
TURNOVER_TO_CHARGE	NUMBER,
CURRENCY	VARCHAR2(2000),
NDS	NUMBER,
TILL_DT	DATE,
FROM_DT	DATE,
CONTRACT_EID	VARCHAR2(2000),
CONTRACT_ID	NUMBER,
INSERT_DT DATE,
DISCOUNT_TYPE NUMBER, 
REWARD_TO_PAY_SRC NUMBER,
  constraint xxxx_commission_reward_2013 primary key ( id ) enable
);

create sequence s_xxxx_extprops order;

create table xxxx_extprops
(
  ID NUMBER,
  CLASSNAME VARCHAR2(16 BYTE),
  ATTRNAME VARCHAR2(16 BYTE),
  OBJECT_ID NUMBER,
  VALUE_NUM NUMBER,
  constraint xxxx_extprops primary key ( id ) enable
);

create sequence s_xxxx_invoice_repayment order;

create table xxxx_invoice_repayment
(
  ID NUMBER,
  INVOICE_ID NUMBER,
  REPAYMENT_INVOICE_ID NUMBER,
  constraint xxxx_invoice_repayment primary key ( id ) enable
);

create sequence s_xxxx_oebs_cash_payment_test order;

create table xxxx_oebs_cash_payment_test
(
  ID NUMBER,
  INVOICE_ID NUMBER,
  OEBS_PAYMENT NUMBER,
  DOC_DATE DATE,
  COMISS_DATE DATE,
  DT DATE,
  PAYMENT_NUMBER VARCHAR2(16 BYTE),
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
  collateral_id number,
  CODE VARCHAR2 (64 char),
  KEY_NUM NUMBER,
  value_num NUMBER,
  value_str VARCHAR2(512 CHAR),
  START_DT DATE,
  STAMP number,
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

create sequence s_xxxx_commission_part order;
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

 

create sequence s_xxxx_deal order; 

 CREATE TABLE xxxx_deal 
(	ID NUMBER, 
	EXTERNAL_ID NUMBER, 
	NAME VARCHAR2(512 BYTE), 
	AGENCY_REV_RATIO NUMBER, 
  
  constraint xxxx_deal primary key ( id ) enable
);

create sequence s_xxxx_deal_notification order; 

 CREATE TABLE xxxx_deal_notification 
(	ID NUMBER, 
	EXTERNAL_ID NUMBER, 
	DOC_DATE DATE, 
	DOC_NUMBER VARCHAR2(64 BYTE), 
  
  constraint xxxx_deal_notification primary key ( id ) enable
);

create sequence s_xxxx_ar_deal_stats order;
CREATE TABLE xxxx_ar_deal_stats
(	ID NUMBER, 
  FROM_DT DATE, 
	TILL_DT DATE, 
	SERVICE_ID NUMBER, 
	SERVICE_ORDER_ID NUMBER, 
	DEAL_EXTERNAL_ID NUMBER, 
	CLIENT_ID NUMBER, 
	SHOWS NUMBER, 
	CLICKS NUMBER, 
	COST NUMBER, 
	AMT_RUR_WO_NDS NUMBER, 
	COSTCUR NUMBER, 
	INSERT_DT DATE,
  constraint xxxx_ar_deal_stats primary key ( id ) enable
   );

create sequence s_xxxx_contract2 order;

create table xxxx_contract2
(
  ID NUMBER,
  CONTRACT_ID NUMBER,
  CONTRACT_EID VARCHAR2(16 BYTE),
  CLIENT_ID NUMBER,
  COMMISSION_TYPE NUMBER,
  CONTRACT_FROM_DT DATE,
  CONTRACT_TILL_DT DATE,
  SIGN_DT DATE,
  constraint xxxx_contract2 primary key ( id ) enable
);



create sequence s_xxxx_acts order;

create table xxxx_acts
(
  ID NUMBER,
  ACT_ID NUMBER,
  CONTRACT_ID NUMBER,
  INVOICE_ID NUMBER,
  DT DATE,
  HIDDEN NUMBER,
  constraint xxxx_acts primary key ( id ) enable
);