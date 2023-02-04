CREATE SEQUENCE boo.s_boo_t_invoice ORDER;

CREATE TABLE "BOO"."T_INVOICE" (
    "TMP_ID"                NUMBER,
    "ID"                    NUMBER,
    "DT"                    DATE,
    "CLIENT_ID"             NUMBER,
    "EXTERNAL_ID"           VARCHAR2(256 BYTE),
    "CONTRACT_ID"           NUMBER,
    "TOTAL_SUM"             NUMBER,
    "CURRENCY"              VARCHAR2(16 BYTE),
    "DISCOUNT_TYPE"         NUMBER,
    "PAYMENT_TERM_DT"       DATE,
    "LOYAL_CLIENTS"         NUMBER,
    "COMMISSION_TYPE"       NUMBER,
    "TYPE"                  VARCHAR2(64 BYTE),
    "ISO_CURRENCY"          VARCHAR2(16 BYTE),
    CONSTRAINT s_boo_t_invoice PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE boo.s_boo_t_contract2 ORDER;

CREATE TABLE "BOO"."T_CONTRACT2" (
    "TMP_ID"        NUMBER,
    "ID"            NUMBER,
    "EXTERNAL_ID"   VARCHAR2(64 BYTE),
    "CLIENT_ID"     NUMBER,
    CONSTRAINT s_boo_t_contract2 PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE boo.s_boo_t_contract_collateral ORDER;

CREATE TABLE "BOO"."T_CONTRACT_COLLATERAL" (
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
    CONSTRAINT s_boo_t_contract_collateral PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE boo.s_boo_t_extprops ORDER;

CREATE TABLE "BOO"."T_EXTPROPS" (
    "TMP_ID"         NUMBER,
    "ID"             NUMBER,
    "OBJECT_ID"      NUMBER,
    "CLASSNAME"      VARCHAR2(128 CHAR),
    "ATTRNAME"       VARCHAR2(128 CHAR),
    "KEY"            VARCHAR2(128 CHAR),
    "VALUE_STR"      VARCHAR2(256 CHAR),
    "VALUE_NUM"      NUMBER,
    "VALUE_DT"       DATE,
    "VALUE_CLOB"     VARCHAR2(4000 BYTE),
    CONSTRAINT s_boo_t_extprops PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE boo.s_boo_t_invoice_repayment ORDER;

CREATE TABLE "BOO"."T_INVOICE_REPAYMNET" (
    "TMP_ID"                 NUMBER,
    "INVOICE_ID"             NUMBER,
    "REPAYMENT_INVOICE_ID"   NUMBER,
    CONSTRAINT s_boo_t_invoice_repayment PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE boo.s_boo_t_act_internal ORDER;

CREATE TABLE "BOO"."T_ACT_INTERNAL" (
    "TMP_ID"              NUMBER,
    "ID"                  NUMBER,
    "DT"                  DATE,
    "CLIENT_ID"           NUMBER,
    "INVOICE_ID"          NUMBER,
    "HIDDEN"              NUMBER,
    "AMOUNT"              NUMBER,
    "EXTERNAL_ID"         VARCHAR2(32 BYTE),
    "PAID_AMOUNT"         NUMBER,
    "PAYMENT_TERM_DT"     DATE,
    "IS_LOYAL"            NUMBER,
    "TYPE"                VARCHAR2(128 BYTE),
    "STATE_ID"            NUMBER,
    CONSTRAINT s_boo_t_act_internal PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE boo.s_boo_t_act_trans ORDER;

CREATE TABLE "BOO"."T_ACT_TRANS" (
    "TMP_ID"            NUMBER,
    "ACT_ID"            NUMBER,
    "CONSUME_ID"        NUMBER, 
    "AMOUNT"            NUMBER,
    "AMOUNT_NDS"        NUMBER,
    "AMOUNT_NSP"        NUMBER,
    "PAID_AMOUNT"       NUMBER,
    "ID"                NUMBER,
    "CONSUME_SUM"       NUMBER,
    "NETTING"           NUMBER,
    "COMMISSION_TYPE"   NUMBER,
    CONSTRAINT s_boo_t_act_trans PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE boo.s_boo_t_consume ORDER;

CREATE TABLE "BOO"."T_CONSUME" (
    "TMP_ID"                NUMBER,
    "ID"                    NUMBER,
    "INVOICE_ID"            NUMBER,
    "PARENT_ORDER_ID"       NUMBER
    CONSTRAINT s_boo_t_consume PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE boo.s_boo_t_order ORDER;

CREATE TABLE "BOO"."T_ORDER" (
    "TMP_ID"                    NUMBER,
    "ID"                        NUMBER,
    "DT"                        DATE,
    "SERVICE_ID"                NUMBER,
    "SERVICE_ORDER_ID"          NUMBER,
    "CLIENT_ID"                 NUMBER,
    CONSTRAINT s_boo_t_order PRIMARY KEY ( tmp_id ) ENABLE
);

CREATE SEQUENCE boo.s_boo_t_person ORDER;

CREATE TABLE "BOO"."T_PERSON" (
    "TMP_ID"   NUMBER,
    "ID"       NUMBER,
    "INN"      VARCHAR2(64 CHAR),
    CONSTRAINT s_boo_t_person PRIMARY KEY ( tmp_id ) ENABLE
);