-- матвьюхи/вьюхи, кода будут смотреть синонимы
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

