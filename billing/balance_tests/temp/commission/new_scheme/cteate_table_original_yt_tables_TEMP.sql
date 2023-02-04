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