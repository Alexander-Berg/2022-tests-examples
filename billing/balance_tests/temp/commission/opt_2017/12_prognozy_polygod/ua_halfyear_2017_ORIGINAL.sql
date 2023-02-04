CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."V_OPT_2015_UA_SKV" ("CONTRACT_EID", "CONTRACT_ID", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "CURRENCY", "NDS", "DISCOUNT_TYPE", "COMMISSION_TYPE", "NDS_COUNT", "CURRENCY_COUNT", "CLIENT_ID", "FROM_DT", "TILL_DT", "TILL_DT_FC", "AMT_W_NDS", "AMT_FOR_FORECAST", "AMT", "MAX_RATIO", "EXCLUDED", "FAILED") AS 
  with
s_dates as (
    select date'2015-04-01'   as fin_year_dt
      from dual
),
-- полугодия
s_half_years as (
    select d.dt from_dt, add_months(d.dt, 6)-1/24/60/60 as till_dt
      from (
         select add_months(date'2015-04-01', 6*(level-1)) as dt
           from dual
        connect by level <= 10
           ) d
),
s_acts as (
    select b.*
      from bo.v_opt_2015_acts b
     where b.commission_type in (6)
),
s_skv as (
    select d.contract_eid, d.contract_id,
           d.contract_from_dt,
           d.contract_till_dt,
           d.currency, d.nds, d.discount_type, d.commission_type,
           d.nds_count, d.currency_count,
           d.client_id, d.is_loyal,
           -- для прогноза (для ЛК не нужно, т.к. ЛК не учитываем в прогнозе)
           -- чтобы знать, за какой период брать оборот для проноза
           d.till_dt                                    as till_dt_fc,
           -- периоды полугодия
           h.from_dt                                    as from_dt,
           trunc(h.till_dt)                             as till_dt,
           sum(d.amt_w_nds)                             as amt_w_nds,
           sum(d.amt)                                   as amt
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
select d."CONTRACT_EID",d."CONTRACT_ID",d."CONTRACT_FROM_DT",d."CONTRACT_TILL_DT",d."CURRENCY",d."NDS",d."DISCOUNT_TYPE",d."COMMISSION_TYPE",d."NDS_COUNT",d."CURRENCY_COUNT",d."CLIENT_ID",d."FROM_DT",d."TILL_DT",d."TILL_DT_FC",d."AMT_W_NDS",d."AMT_FOR_FORECAST",d."AMT",
       -- ТЗ: Полугодовая премия начисляется Агентству только при условии, если
       -- стоимость фактически оказанных Яндексом в течение соответствующего
       -- Отчетного полугодия Услуг по сервису Директ, связанных с размещением
       -- Материалов одного Клиента Агентства < 70% (округляется до целых
       -- процентов)
       l.max_ratio,
       case
           when contract_eid in (select contract_eid
                                   from bo.v_opt_2015_exclusions)
           then 1
           else 0
       end                      as excluded,
       case
        when round(l.max_ratio, 2) < 0.7
          -- BALANCE-20688
         and nds_count = 1
         and currency_count = 1
        then 0
        else 1
       end          as failed
  from (
        select d.contract_eid, d.contract_id,
               d.contract_from_dt,
               d.contract_till_dt,
               d.currency, d.nds,
               d.discount_type, d.commission_type,
               d.nds_count, d.currency_count,
               d.client_id,
               d.from_dt, d.till_dt, d.till_dt_fc,
               d.amt_w_nds,
               d.amt                    as amt_for_forecast,
               d.amt
          from s_skv d
       ) d
       -- смотрим максимальную долю по клиенту по директу за полугодие
  join bo.v_opt_2015_ua_skv_less_70   l on l.contract_id = d.contract_id
                                         and l.from_dt = d.from_dt;
