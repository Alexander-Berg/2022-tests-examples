
  CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."V_OPT_2017_VERTICALS_SKV" ("CONTRACT_EID", "CONTRACT_ID", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "DISCOUNT_TYPE", "COMMISSION_TYPE", "CLIENT_ID", "NDS", "NDS_COUNT", "CURRENCY_COUNT", "FROM_DT", "TILL_DT", "TILL_DT_FC", "AMT_W_NDS_RUB", "AMT_RUB", "AMT_FOR_FORECAST", "MAX_RATIO", "EXCLUDED", "FAILED") AS 
  with
s_half_years as (
    -- остальные — по 6 месяцев
    select d.dt from_dt, add_months(d.dt, 6)-1/24/60/60 as till_dt
      from (
         select add_months(date'2016-03-01', 6*(level-1)) as dt
           from dual
        connect by level <= 10
           ) d
),
s_acts as (
    select b.*
      from bo.v_opt_2015_acts   b
     where b.commission_type in (17)
)
-- КВ с контролем, что оплат не более, чем актов
, s_skv as (
    select d.contract_eid, d.contract_id,
           d.contract_from_dt,
           d.contract_till_dt,
           d.currency, d.nds, d.discount_type, d.commission_type,
           d.client_id, d.is_loyal,
           d.nds_count, d.currency_count,
           -- для прогноза
           -- чтобы знать, за какой период брать оборот для проноза
           d.till_dt                                    as till_dt_fc,
           -- периоды полугодия
           h.from_dt                                    as from_dt,
           trunc(h.till_dt)                             as till_dt,
           sum(d.amt_w_nds_rub)                         as amt_w_nds_rub,
           sum(d.amt_rub)                               as amt_rub
      from s_acts                       d
      join s_half_years                 h  on d.act_dt between h.from_dt
                                                           and h.till_dt
     group by h.from_dt, trunc(h.till_dt), d.till_dt,
              d.contract_from_dt,
              d.contract_till_dt,
              d.contract_eid, d.contract_id,
              d.currency, d.nds, d.discount_type, d.commission_type,
              d.nds_count, d.currency_count,
              d.client_id, d.is_loyal
)
select d."CONTRACT_EID",d."CONTRACT_ID",d."CONTRACT_FROM_DT",d."CONTRACT_TILL_DT",d."DISCOUNT_TYPE",d."COMMISSION_TYPE",d."CLIENT_ID",d."NDS",d."NDS_COUNT",d."CURRENCY_COUNT",d."FROM_DT",d."TILL_DT",d."TILL_DT_FC",d."AMT_W_NDS_RUB",d."AMT_RUB",
       d.amt_rub                       as amt_for_forecast,
       0                               as max_ratio,
       0                               as excluded,
       0                               as failed
  from (
        select d.contract_eid, d.contract_id,
               d.contract_from_dt,
               d.contract_till_dt,
               d.discount_type, d.commission_type,
               d.client_id, d.nds,
               d.nds_count, d.currency_count,
               d.from_dt, d.till_dt, d.till_dt_fc,
               d.amt_w_nds_rub,
               d.amt_rub
          from s_skv d
       ) d
;
