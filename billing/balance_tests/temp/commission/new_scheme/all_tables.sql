-----YT tables mock
CREATE SEQUENCE bo.s_x_yt_ar_direct_domain_stats ORDER;

CREATE TABLE "BO"."X_YT_AR_DIRECT_DOMAIN_STATS" (
    "ID"                  NUMBER,
    "IS_GRAY"             NUMBER,
    "DOMAIN"              VARCHAR2(32 BYTE),
    "SERVICE_ORDER_ID"    NUMBER,
    "IS_BLACKLIST"        NUMBER,
    "SERVICE_ID"          NUMBER,
    "COST"                NUMBER,
    "BILLING_EXPORT_ID"   NUMBER,
    "FROM_DT"             DATE,
    "TILL_DT"             DATE,
    CONSTRAINT s_x_yt_ar_direct_domain_stats PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_x_yt_fin_docs ORDER;

CREATE TABLE x_yt_fin_docs (
    "ID"             NUMBER,
    "CONTRACT_EID"   VARCHAR2(64 BYTE),
    "AGENCY_ID"      NUMBER,
    "FROM_DT"        DATE,
    "RECEIVE_DT"     DATE,
    CONSTRAINT s_x_yt_fin_docs PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_x_yt_loyal_domains ORDER;

CREATE TABLE x_yt_loyal_domains (
    "TMP_ID"   NUMBER,
    "DOMAIN"    VARCHAR2(64 BYTE),
    "PERIOD"    DATE,
    CONSTRAINT s_x_yt_loyal_domains PRIMARY KEY ( tmp_id ) ENABLE
);


CREATE SEQUENCE s_x_yt_new_domains ORDER;

CREATE TABLE x_yt_new_domains (
    "TMP_ID"   NUMBER,
    "DOMAIN"    VARCHAR2(64 BYTE),
    "PERIOD"    DATE,
    CONSTRAINT s_x_yt_new_domains PRIMARY KEY ( tmp_id ) ENABLE
);
--------ALL TABLES
create table x_t_ar_reward_type_rules as  select * from t_ar_reward_type_rules;

CREATE SEQUENCE bo.s_x_t_invoice ORDER;

CREATE TABLE "BO"."X_T_INVOICE" (
    "TMP_ID"            NUMBER,
    "ID"                NUMBER,
    "DT"                DATE,
    "CLIENT_ID"         NUMBER,
    "EXTERNAL_ID"       VARCHAR2(256 BYTE),
    "CONTRACT_ID"       NUMBER,
    "TOTAL_SUM"         NUMBER,
    "CURRENCY"          VARCHAR2(16 BYTE),
    "DISCOUNT_TYPE"     NUMBER,
    "PERSON_ID"         NUMBER,
    "PAYSYS_ID"         NUMBER,
    "NDS"               NUMBER,
    "NDS_PCT"           NUMBER,
    "LOYAL_CLIENTS"     NUMBER,
    "COMMISSION_TYPE"   NUMBER,
    "TYPE"              VARCHAR2(64 BYTE),
    "ISO_CURRENCY"      VARCHAR2(16 BYTE),
    CONSTRAINT s_x_t_invoice PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE bo.s_x_t_contract2 ORDER;

CREATE TABLE "BO"."X_T_CONTRACT2" (
    "TMP_ID"        NUMBER,
    "ID"            NUMBER,
    "EXTERNAL_ID"   VARCHAR2(64 BYTE),
    "CLIENT_ID"     NUMBER,
    CONSTRAINT s_x_t_contract2 PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE bo.s_x_t_contract_collateral ORDER;

CREATE TABLE "BO"."X_T_CONTRACT_COLLATERAL" (
    "TMP_ID"               NUMBER,
    "ID"                   NUMBER,
    "DT"                   DATE,
    "CONTRACT2_ID"         NUMBER,
    "UPDATE_DT"            DATE,
    "NUM"                  VARCHAR2(64 BYTE),
    "COLLATERAL_TYPE_ID"   NUMBER,
    "IS_FAXED"             DATE,
    "IS_SIGNED"            DATE,
    "IS_CANCELLED"         DATE,
    "ATTRIBUTE_BATCH_ID"   NUMBER,
    CONSTRAINT s_x_t_contract_collateral PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE bo.s_x_t_extprops ORDER;

CREATE TABLE "BO"."X_T_EXTPROPS" (
    "TMP_ID"       NUMBER,
    "ID"           NUMBER,
    "OBJECT_ID"    NUMBER,
    "CLASSNAME"    VARCHAR2(128 CHAR),
    "ATTRNAME"     VARCHAR2(128 CHAR),
    "KEY"          VARCHAR2(128 CHAR),
    "VALUE_STR"    VARCHAR2(256 CHAR),
    "VALUE_NUM"    NUMBER,
    "VALUE_DT"     DATE,
    "VALUE_CLOB"   VARCHAR2(4000 BYTE),
    CONSTRAINT s_x_t_extprops PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE bo.s_x_t_invoice_repayment ORDER;

CREATE TABLE "BO"."X_T_INVOICE_REPAYMENT" (
    "TMP_ID"                 NUMBER,
    "INVOICE_ID"             NUMBER,
    "REPAYMENT_INVOICE_ID"   NUMBER,
    CONSTRAINT s_x_t_invoice_repayment PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE bo.s_x_t_act_internal ORDER;

CREATE TABLE "BO"."X_T_ACT_INTERNAL" (
    "TMP_ID"              NUMBER,
    "ACT_SUM"             NUMBER,
    "AMOUNT"              NUMBER,
    "AMOUNT_NDS"          NUMBER,
    "AMOUNT_NSP"          NUMBER,
    "CLIENT_ID"           NUMBER,
    "CURRENCY_RATE"       NUMBER,
    "DT"                  DATE,
    "EXTERNAL_ID"         VARCHAR2(32),
    "FACTURA"             VARCHAR2(20),
    "GOOD_DEBT"           NUMBER(1),
    "HIDDEN"              NUMBER,
    "ID"                  NUMBER,
    "INVOICE_ID"          NUMBER,
    "IS_DOCS_DETAILED"    NUMBER,
    "IS_DOCS_SEPARATED"   NUMBER,
    "IS_LOYAL"            NUMBER,
    "IS_TRP"              NUMBER(38),
    "JIRA_ID"             VARCHAR2(256),
    "LABEL"               VARCHAR2(512),
    "MEMO"                CLOB,
    "MONTH_DT"            DATE,
    "OPERATION_ID"        NUMBER,
    "PAID_AMOUNT"         NUMBER,
    "PAYMENT_TERM_DT"     DATE,
    "RUR_SUM_A"           NUMBER,
    "RUR_SUM_B"           NUMBER,
    "RUR_SUM_C"           NUMBER,
    "RUR_SUM_D"           NUMBER,
    "RUR_SUM_E"           NUMBER,
    "STATE_ID"            NUMBER,
    "TYPE"                VARCHAR2(128),
    "UNILATERAL"          NUMBER(1),
    "UPDATE_DT"           DATE,
    "USD_RATE"            NUMBER,
    "USD_SUM_C"           NUMBER,
    CONSTRAINT s_x_t_act_internal PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE bo.s_x_t_act_trans ORDER;

CREATE TABLE "BO"."X_T_ACT_TRANS" (
    "TMP_ID"            NUMBER,
    "ACT_ID"            NUMBER,
    "CONSUME_ID"        NUMBER,
    "AMOUNT"            NUMBER,
    "AMOUNT_NDS"        NUMBER,
    "AMOUNT_NSP"        NUMBER,
    "NETTING"           NUMBER,
    "COMMISSION_TYPE"   NUMBER,
    CONSTRAINT s_x_t_act_trans PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE bo.s_x_t_consume ORDER;

CREATE TABLE "BO"."X_T_CONSUME" (
    "TMP_ID"            NUMBER,
    "ID"                NUMBER,
    "PARENT_ORDER_ID"   NUMBER,
    CONSTRAINT s_x_t_consume PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE bo.s_x_t_order ORDER;

CREATE TABLE "BO"."X_T_ORDER" (
    "TMP_ID"             NUMBER,
    "ID"                 NUMBER,
    "SERVICE_ID"         NUMBER,
    "SERVICE_ORDER_ID"   NUMBER,
    "CLIENT_ID"          NUMBER,
    CONSTRAINT s_x_t_order PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE bo.s_x_t_person ORDER;

CREATE TABLE "BO"."X_T_PERSON" (
    "TMP_ID"   NUMBER,
    "ID"       NUMBER,
    "INN"      VARCHAR2(64 CHAR),
    CONSTRAINT s_x_t_person PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE bo.s_x_v_group_order_act_div ORDER;

CREATE TABLE "BO"."X_V_GROUP_ORDER_ACT_DIV" (
    "TMP_ID"                   NUMBER,
    "ACT_ID"                   NUMBER,
    "SERVICE_ID"               NUMBER,
    "GROUP_SERVICE_ORDER_ID"   NUMBER,
    "SERVICE_ORDER_ID"         NUMBER,
    "INV_AMOUNT"               NUMBER,
    "ORDER_AMOUNT"             NUMBER,
    "DT"                       DATE,
    CONSTRAINT s_x_v_group_order_act_div PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE bo.s_x_t_ar_paid_periods ORDER;

CREATE TABLE "BO"."X_T_AR_PAID_PERIODS" (
    "ID"                NUMBER,
    "CONTRACT_ID"       NUMBER,
    "DISCOUNT_TYPE"     NUMBER,
    "FROM_DT"           DATE,
    "PAID_DT"           DATE,
    "COMMISSION_TYPE"   NUMBER,
    CONSTRAINT "s_x_t_ar_paid_periods" PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE bo.s_x_t_ar_invoice_reward ORDER;

CREATE TABLE "BO"."X_T_AR_INVOICE_REWARD" (
    "INVOICE_ID"        NUMBER,
    "REWARD"            NUMBER,
    "COMMISSION_TYPE"   NUMBER,
    "SCALE"             NUMBER,
    "FROM_DT"           DATE
);

--------MV mock
CREATE SEQUENCE bo.s_x_mv_contract_signed_attr ORDER;

CREATE TABLE "BO"."X_MV_CONTRACT_SIGNED_ATTR" (
    "TMP_ID"          NUMBER,
    "CONTRACT_ID"     NUMBER,
    "COLLATERAL_ID"   NUMBER,
    "DT"              DATE,
    "CODE"            VARCHAR2(64 BYTE),
    "KEY_NUM"         NUMBER,
    "VALUE_STR"       VARCHAR2(512 CHAR),
    "VALUE_NUM"       NUMBER,
    "VALUE_DT"        DATE,
    "COLLATERAL_DT"   DATE,
    CONSTRAINT s_x_mv_contract_signed_attr PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE bo.s_x_mv_contract_signed_attr_hist ORDER;

CREATE TABLE "BO"."X_MV_CONTRACT_SIGNED_ATTR_HIST" (
    "TMP_ID"               NUMBER,
    "ID"                   NUMBER,
    "COLLATERAL_ID"        NUMBER,
    "ATTRIBUTE_BATCH_ID"   NUMBER,
    "DT"                   DATE,
    "CODE"                 VARCHAR2(64 BYTE),
    "KEY_NUM"              NUMBER,
    "VALUE_STR"            VARCHAR2(512 CHAR),
    "VALUE_NUM"            NUMBER,
    "VALUE_DT"             DATE,
    "UPDATE_DT"            DATE,
    "CONTRACT2_ID"         NUMBER,
    "CL_DT"                DATE,
    "CL_ID"                NUMBER,
    "STAMP"                VARCHAR2(29 BYTE),
    CONSTRAINT s_x_mv_contract_signed_attr_hist PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE bo.s_x_mv_ui_contract_apex ORDER;

CREATE TABLE "BO"."X_MV_UI_CONTRACT_APEX" (
    "TMP_ID"         NUMBER,
    "CONTRACT_ID"    NUMBER,
    "CONTRACT_EID"   VARCHAR2(64 BYTE),
    "DT"             DATE,
    "FINISH_DT"      DATE,
    "CLIENT_ID"      NUMBER,
    "AGENCY_ID"      NUMBER,
    CONSTRAINT s_boo_mv_ui_contract_apex PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE bo.s_x_mv_currency_rate ORDER;

CREATE TABLE "BO"."X_MV_CURRENCY_RATE" (
    "TMP_ID"    NUMBER,
    "ID"        NUMBER,
    "CC"        VARCHAR2(64 CHAR),
    "RATE"      NUMBER,
    "RATE_DT"   DATE,
    CONSTRAINT s_x_mv_currency_rate PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE bo.s_x_mv_oebs_receipts_2 ORDER;

CREATE TABLE "BO"."X_MV_OEBS_RECEIPTS_2" (
    "TMP_ID"           NUMBER,
    "INVOICE_EID"      VARCHAR2(270 BYTE),
    "SUM"              NUMBER,
    "DT"               DATE,
    "DOC_DATE"         DATE,
    "PAYMENT_NUMBER"   VARCHAR2(1000 BYTE),
    "SOURCE_TYPE"      VARCHAR2(450 CHAR),
    "COMISS_DATE"      DATE,
    CONSTRAINT s_x_mv_oebs_receipts_2 PRIMARY KEY ( tmp_id ) ENABLE
);

