
  CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."V_OPT_2015_MARKET_SKV" ("CONTRACT_EID", "CONTRACT_ID", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "DISCOUNT_TYPE", "COMMISSION_TYPE", "CLIENT_ID", "NDS", "NDS_COUNT", "CURRENCY_COUNT", "FROM_DT", "TILL_DT", "TILL_DT_FC", "AMT_W_NDS_RUB", "AMT_RUB_NOT_LC", "AMT_RUB", "AMT_FOR_FORECAST", "MAX_RATIO", "EXCLUDED", "FAILED") AS 
  with
s_dates as (
    select date'2016-03-01'   as fin_year_dt
      from dual
),
-- полугодия
s_half_years as (
    select d.dt from_dt, add_months(d.dt, 6)-1/24/60/60 as till_dt
      from (
         select add_months(date'2016-03-01', 6*(level-1)) as dt
           from dual
        connect by level <= 10
           ) d
),
s_acts as (
    select b.*
      from bo.v_opt_2015_acts b
     where b.commission_type in (12, 13)
),
s_skv_not_lc as (
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
select d."CONTRACT_EID",d."CONTRACT_ID",d."CONTRACT_FROM_DT",d."CONTRACT_TILL_DT",d."DISCOUNT_TYPE",d."COMMISSION_TYPE",d."CLIENT_ID",d."NDS",d."NDS_COUNT",d."CURRENCY_COUNT",d."FROM_DT",d."TILL_DT",d."TILL_DT_FC",d."AMT_W_NDS_RUB",d."AMT_RUB_NOT_LC",
       d.amt_rub_not_lc                as amt_rub,
       d.amt_rub_not_lc                as amt_for_forecast,
       -- ТЗ: Полугодовая премия начисляется Агентству только при условии, если
       -- стоимость фактически оказанных Яндексом в течение соответствующего
       -- Отчетного полугодия Услуг по сервису Директ, связанных с размещением
       -- Материалов одного Клиента Агентства < 70% (округляется до целых
       -- процентов)
       -- BALANCE-24640
       0                        as max_ratio,
       0                        as excluded,
       0                        as failed
  from (
        select d.contract_eid, d.contract_id,
               d.contract_from_dt,
               d.contract_till_dt,
               d.discount_type, d.commission_type,
               d.client_id, d.nds,
               d.nds_count, d.currency_count,
               d.from_dt, d.till_dt, d.till_dt_fc,
               d.amt_w_nds_rub,
               d.amt_rub            as amt_rub_not_lc
          from s_skv_not_lc d
       ) d
       -- смотрим максимальную долю по клиенту по директу за полугодие
  -- join bo.v_opt_2015_market_skv_less_70   l on l.contract_id = d.contract_id
  --                                          and l.from_dt = d.from_dt
;
