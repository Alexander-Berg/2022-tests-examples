CREATE SEQUENCE s_xxxx_new_comm_contract_basic ORDER;

--drop table xxxx_new_comm_contract_basic;

CREATE TABLE xxxx_new_comm_contract_basic (
    id                         NUMBER,
    test_case_id               NUMBER,
    contract_id                NUMBER,
    contract_eid               VARCHAR2(16 BYTE),
    contract_from_dt           DATE,
    contract_till_dt           DATE,
    contract_commission_type   NUMBER,
    invoice_id                 NUMBER,
    invoice_eid                VARCHAR2(16 BYTE),
    invoice_dt                 DATE,
    payment_type               NUMBER,
    commission_payback_type    NUMBER,
    commission_type            NUMBER,
    discount_type              NUMBER,
    currency                   VARCHAR2(16 BYTE),
    nds                        NUMBER,
    nds_pct                    NUMBER,
    invoice_first_payment      DATE,
    invoice_first_act          DATE,
    invoice_sum                NUMBER,
    act_id                     NUMBER,
    hidden                     NUMBER,
    act_dt                     DATE,
    amount                     NUMBER,
    amount_nds                 NUMBER,
    amount_nsp                 NUMBER,
    client_id                  NUMBER,
    agency_id                  NUMBER,
    loyal_client               NUMBER,
    cfo                        VARCHAR2(16 BYTE),
    regional_program           NUMBER,
    commission_payback_pct     NUMBER,
    is_loyal                   NUMBER,
    endbuyer_id                NUMBER,
    endbuyer_inn               NUMBER,
    agency_inn                 NUMBER,
    paysys_id                  NUMBER,
    product_id                 NUMBER,
    person_id                  NUMBER,
    firm_id                    NUMBER,
    order_id                   NUMBER,
    service_id                 NUMBER,
    service_order_id           NUMBER,
    code                       VARCHAR2(32 BYTE),
    value_num                  NUMBER,
    plan_sum                   NUMBER,
    plan_dt                    DATE,
    act_eid                    VARCHAR2(16 BYTE),
    kkm_id                     NUMBER,
    kkm_eid                    VARCHAR2(16 BYTE),
    subclient_id               NUMBER,
    brand_finish_dt            DATE,
    act_payment_term_dt        DATE,
    ls_invoice_id              NUMBER,
    main_client_id             NUMBER,
    CONSTRAINT xxxx_new_comm_contract_basic PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_xxxx_act_trans ORDER;

CREATE TABLE xxxx_act_trans (
    id                NUMBER,
    act_id            NUMBER,
    commission_type   NUMBER,
    amount            NUMBER,
    amount_nds        NUMBER,
    amount_nsp        NUMBER,
    parent_order_id   NUMBER,
    CONSTRAINT xxxx_act_trans PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_xxxx_order ORDER;

CREATE TABLE xxxx_order (
    id                 NUMBER,
    order_id           NUMBER,
    client_id          NUMBER,
    service_id         NUMBER,
    service_order_id   NUMBER,
    CONSTRAINT xxxx_order PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_xxxx_invoice ORDER;

CREATE TABLE xxxx_invoice (
    id                NUMBER,
    inv_id            NUMBER,
    invoice_type      VARCHAR2(32 BYTE),
    total_sum         NUMBER,
    currency          VARCHAR2(16 BYTE),
    commission_type   NUMBER,
    discount_type     NUMBER,
    contract_id       NUMBER,
    CONSTRAINT xxxx_invoice PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_xxxx_ui_contract_apex ORDER;

CREATE TABLE xxxx_ui_contract_apex (
    id               NUMBER,
    contract_id      NUMBER,
    start_dt         DATE,
    finish_dt        DATE,
    main_client_id   NUMBER,
    CONSTRAINT xxxx_ui_contract_apex PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_xxxx_commission_reward_2013 ORDER;

CREATE TABLE xxxx_commission_reward_2013 (
    id                      NUMBER,
    reward_type             NUMBER,
    reward_to_pay           NUMBER,
    turnover_to_pay_w_nds   NUMBER,
    turnover_to_pay         NUMBER,
    reward_to_charge        NUMBER,
    turnover_to_charge      NUMBER,
    currency                VARCHAR2(2000),
    nds                     NUMBER,
    till_dt                 DATE,
    from_dt                 DATE,
    contract_eid            VARCHAR2(2000),
    contract_id             NUMBER,
    insert_dt               DATE,
    discount_type           NUMBER,
    reward_to_pay_src       NUMBER,
    CONSTRAINT xxxx_commission_reward_2013 PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_xxxx_extprops ORDER;

CREATE TABLE xxxx_extprops (
    id          NUMBER,
    classname   VARCHAR2(16 BYTE),
    attrname    VARCHAR2(16 BYTE),
    object_id   NUMBER,
    value_num   NUMBER,
    CONSTRAINT xxxx_extprops PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_xxxx_invoice_repayment ORDER;

CREATE TABLE xxxx_invoice_repayment (
    id                     NUMBER,
    invoice_id             NUMBER,
    repayment_invoice_id   NUMBER,
    CONSTRAINT xxxx_invoice_repayment PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_xxxx_oebs_cash_payment_test ORDER;

CREATE TABLE xxxx_oebs_cash_payment_test (
    id               NUMBER,
    invoice_id       NUMBER,
    oebs_payment     NUMBER,
    doc_date         DATE,
    comiss_date      DATE,
    dt               DATE,
    payment_number   VARCHAR2(16 BYTE),
    CONSTRAINT xxxx_oebs_cash_payment_test PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_xxxx_client_discount_m ORDER;

CREATE TABLE xxxx_client_discount_m (
    id                    NUMBER,
    client_id             NUMBER,
    client_avg_discount   NUMBER,
    dt                    DATE,
    CONSTRAINT xxxx_client_discount_m PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_xxxx_contract_signed_attr ORDER;

CREATE TABLE xxxx_contract_signed_attr (
    id              NUMBER,
    contract_id     NUMBER,
    collateral_id   NUMBER,
    code            VARCHAR2(64 CHAR),
    key_num         NUMBER,
    value_num       NUMBER,
    value_str       VARCHAR2(512 CHAR),
    value_dt        DATE,
    start_dt        DATE,
    stamp           NUMBER,
    CONSTRAINT xxxx_contract_signed_attr PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_xxxx_loyal_clients_contr_atr ORDER;

CREATE TABLE xxxx_loyal_clients_contr_attr (
    id                    NUMBER,
    contract_id           NUMBER,
    client_id             NUMBER,
    lc_turnover           NUMBER,
    collateral_first_dt   DATE,
    collateral_end_dt     DATE,
    CONSTRAINT xxxx_loyal_clients_contr_attr PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_xxxx_currency_rate ORDER;

CREATE TABLE xxxx_currency_rate (
    id        NUMBER,
    cc        VARCHAR2(64 CHAR),
    rate      NUMBER,
    rate_dt   DATE,
    CONSTRAINT xxxx_currency_rate PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_xxxx_commission_part ORDER;

CREATE TABLE xxxx_commission_part (
    id                      NUMBER,
    contract_id             NUMBER,
    from_dt                 DATE,
    till_dt                 DATE,
    nds                     NUMBER,
    currency                VARCHAR2(16 BYTE),
    reward_type             NUMBER,
    discount_type           NUMBER,
    commission_type         NUMBER,
    turnover_to_charge      NUMBER,
    reward_to_charge        NUMBER,
    delkredere_to_charge    NUMBER,
    dkv_to_charge           NUMBER,
    turnover_to_pay_w_nds   NUMBER,
    turnover_to_pay         NUMBER,
    reward_to_pay           NUMBER,
    delkredere_to_pay       NUMBER,
    dkv_to_pay              NUMBER,
    CONSTRAINT xxxx_commission_part PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_xxxx_deal ORDER;

CREATE TABLE xxxx_deal (
    id                 NUMBER,
    external_id        NUMBER,
    name               VARCHAR2(512 BYTE),
    agency_rev_ratio   NUMBER,
    CONSTRAINT xxxx_deal PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_xxxx_deal_notification ORDER;

CREATE TABLE xxxx_deal_notification (
    id            NUMBER,
    external_id   NUMBER,
    doc_date      DATE,
    doc_number    VARCHAR2(64 BYTE),
    CONSTRAINT xxxx_deal_notification PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_xxxx_ar_deal_stats ORDER;

CREATE TABLE xxxx_ar_deal_stats (
    id                 NUMBER,
    from_dt            DATE,
    till_dt            DATE,
    service_id         NUMBER,
    service_order_id   NUMBER,
    deal_external_id   NUMBER,
    client_id          NUMBER,
    shows              NUMBER,
    clicks             NUMBER,
    cost               NUMBER,
    amt_rur_wo_nds     NUMBER,
    costcur            NUMBER,
    insert_dt          DATE,
    CONSTRAINT xxxx_ar_deal_stats PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_xxxx_contract2 ORDER;

CREATE TABLE xxxx_contract2 (
    id                 NUMBER,
    contract_id        NUMBER,
    contract_eid       VARCHAR2(16 BYTE),
    client_id          NUMBER,
    commission_type    NUMBER,
    contract_from_dt   DATE,
    contract_till_dt   DATE,
    sign_dt            DATE,
    CONSTRAINT xxxx_contract2 PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_xxxx_acts ORDER;

CREATE TABLE xxxx_acts (
    id                NUMBER,
    act_id            NUMBER,
    act_eid           VARCHAR2(16 BYTE),
    amount            NUMBER,
    contract_id       NUMBER,
    invoice_id        NUMBER,
    dt                DATE,
    hidden            NUMBER,
    client_id         NUMBER,
    is_loyal          NUMBER,
    payment_term_dt   DATE,
    CONSTRAINT xxxx_acts PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_xxxx_ar_market_stats ORDER;

CREATE TABLE xxxx_ar_market_stats (
    id                    NUMBER,
    agency_id             NUMBER,
    client_id             NUMBER,
    client_pass_checks    NUMBER,
    client_total_checks   NUMBER,
    client_active_days    NUMBER,
    client_total_days     NUMBER,
    from_dt               DATE,
    till_dt               DATE,
    insert_dt             DATE,
    CONSTRAINT xxxx_ar_market_stats PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_xxxx_ar_direct_domain_stats ORDER;

CREATE TABLE xxxx_ar_direct_domain_stats (
    id                  NUMBER,
    is_gray             NUMBER,
    domain              VARCHAR2(32 BYTE),
    service_order_id    NUMBER,
    is_blacklist        NUMBER,
    service_id          NUMBER,
    cost                NUMBER,
    billing_export_id   NUMBER,
    from_dt             DATE,
    till_dt             DATE,
    CONSTRAINT xxxx_ar_direct_domain_stats PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_xxxx_act_by_page ORDER;

CREATE TABLE xxxx_act_by_page (
    id         NUMBER,
    exportid   NUMBER,
    engineid   NUMBER,
    pageid     NUMBER,
    amt_rur    NUMBER,
    act_dt     DATE,
    CONSTRAINT xxxx_act_by_page PRIMARY KEY ( id ) ENABLE
);
   
--берем данные по ЛС   

CREATE SEQUENCE s_xxxx_group_order_act_div ORDER;

CREATE TABLE xxxx_group_order_act_div (
    id                       NUMBER,
    act_id                   NUMBER,
    service_id               NUMBER,
    group_service_order_id   NUMBER,
    service_order_id         NUMBER,
    inv_amount               NUMBER,
    order_amount             NUMBER,
    order_id                 NUMBER,
    dt                       DATE,
    CONSTRAINT xxxx_group_order_act_div PRIMARY KEY ( id ) ENABLE
);  
   
--для получения оборота по оферте для кварталок   

CREATE SEQUENCE s_xxxx_sales_daily ORDER;

CREATE TABLE xxxx_sales_daily (
    id                 NUMBER,
    agency_id          NUMBER,
    act_id             NUMBER,
    service_id         NUMBER,
    ar_commission_type NUMBER,
    service_order_id   NUMBER,
    amt_rur            NUMBER,
    dt_month           NUMBER,
    act_dt             DATE,
    CONSTRAINT xxxx_sales_daily PRIMARY KEY ( id ) ENABLE
);

--данные из крутилки   

CREATE SEQUENCE s_xxxx_bk_page ORDER;

CREATE TABLE xxxx_bk_page (
    id           NUMBER,
    pageid       NUMBER,
    targettype   NUMBER,
    CONSTRAINT xxxx_bk_page PRIMARY KEY ( id ) ENABLE
);

CREATE SEQUENCE s_xxxx_fin_docs ORDER;

CREATE TABLE xxxx_fin_docs (
    id             NUMBER,
    contract_eid   VARCHAR2(64 BYTE),
    agency_id      NUMBER,
    from_dt        DATE,
    receive_dt     DATE,
    CONSTRAINT xxxx_fin_docs PRIMARY KEY ( id ) ENABLE
);