
  CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."V_OPT_2015_BASE_SKV" ("CONTRACT_EID", "CONTRACT_ID", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "DISCOUNT_TYPE", "NDS", "CLIENT_ID", "FROM_DT", "TILL_DT", "TILL_DT_FC", "NDS_COUNT", "CURRENCY_COUNT", "AMT_W_NDS_RUB", "AMT_RUB_NOT_LC", "AMT_RUB_LC", "AMT_RUB", "AMT_FOR_FORECAST", "MAX_RATIO", "EXCLUDED", "FAILED") AS 
  with
-- полугодия
s_half_years as (
    -- остальные — по 6 месяцев
    select d.dt from_dt, add_months(d.dt, 6)-1/24/60/60 as till_dt
      from (
         select add_months(date'2017-03-01', 6*(level-1)) as dt
           from dual
        connect by level <= 10
           ) d
),
s_acts as (
    select b.*
      from bo.v_opt_2015_acts b
     where b.commission_type in (1, 8, 21)
        -- только договора текущего фин.года
       and b.contract_till_dt > add_months(trunc(sysdate, 'YYYY'), 2)
),
-- Акты ЛК, у которых закончились программы в полугодии
-- Акты при этом надо брать не только за тек. год, т.к. бывают
-- программы, которые начинаются в прошлом фин. году. И по таким
-- ЛК надо учитывать весь оборот по программе, а не только тот,
-- которьй получился в текущем фин.году
-- BALANCE-18412
s_acts_lc as (
    select b.contract_eid,
           b.contract_id,
           b.invoice_eid,
           b.invoice_id,
           b.invoice_dt,
           b.contract_from_dt,
           b.contract_till_dt,
           b.currency,
           b.nds,
           b.payment_type,
           b.discount_type,
           b.commission_type,
           b.is_loyal,
           b.client_id,
           b.amt_w_nds,
           b.amt,
           b.amt_w_nds_rub,
           b.amt_rub,
           b.act_dt,
           h.from_dt,
           trunc(h.till_dt)                         as till_dt,
           b.nds_count, b.currency_count,
           lc.turnover,
           -- BALANCE-20689
           round(abs(months_between(
                lc.todate, lc.collateral_dt)))      as lc_months_count,
           lc.collateral_dt                         as lc_start_dt,
           lc.todate                                as lc_end_dt
      from s_acts                           b
      join bo.mv_loyal_clients_contr_attr   lc on lc.contract_id = b.contract_id
                                              and lc.client_id = b.client_id
                                              -- только закончившиеся ДС
                                              -- относительно полугодия
                                              -- TODO: вспомнить, зачем это?
                                              and case
                                                    when sysdate >= date'2016-03-01' then date'2016-03-01'-1/24/60/60
                                                    when sysdate >= date'2015-09-01' then date'2015-09-01'-1/24/60/60
                                                    when sysdate >= date'2015-04-01' then date'2015-04-01'-1/24/60/60
                                                    else trunc(sysdate, 'MM')
                                                end > lc.todate
      join s_half_years                     h  on lc.todate between h.from_dt
                                                                and h.till_dt
     where b.is_loyal = 1
       and b.act_dt between lc.collateral_dt and lc.todate
),
s_skv_lc as (
    select d.*
      from (
        select d.contract_eid, d.contract_id,
               d.contract_from_dt,
               d.contract_till_dt,
               d.currency, d.nds, d.discount_type, d.commission_type,
               d.client_id, d.is_loyal,
               d.turnover,
               d.from_dt,
               d.till_dt,
               d.lc_months_count,
               d.nds_count, d.currency_count,
               sum(d.amt_w_nds_rub)                     as amt_w_nds_rub,
               sum(d.amt_rub)                           as amt_rub
          from s_acts_lc                 d
         group by d.from_dt, d.till_dt,
                  d.contract_from_dt,
                  d.contract_till_dt,
                  d.contract_eid, d.contract_id,
                  d.currency, d.nds, d.discount_type, d.commission_type,
                  d.client_id, d.is_loyal, d.lc_months_count,
                  d.nds_count, d.currency_count,
                  d.turnover
           ) d
     where d.amt_rub >= turnover*lc_months_count*1.1
),
s_skv_not_lc as (
    select d.contract_eid, d.contract_id,
           d.contract_from_dt,
           d.contract_till_dt,
           d.currency, d.nds, d.discount_type, d.commission_type,
           d.client_id, d.is_loyal,
           d.nds_count, d.currency_count,
           -- для прогноза (для ЛК не нужно, т.к. ЛК не учитываем в прогнозе)
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
        -- Без ЛК
       and d.is_loyal = 0
     where d.discount_type in (7, 1, 2, 3, 12, 36, 37)
     group by h.from_dt, trunc(h.till_dt), d.till_dt,
              d.contract_from_dt,
              d.contract_till_dt,
              d.contract_eid, d.contract_id,
              d.currency, d.nds, d.discount_type, d.commission_type,
              d.nds_count, d.currency_count,
              d.client_id, d.is_loyal
)
select d.contract_eid, d.contract_id,
       d.contract_from_dt,
       d.contract_till_dt,
       decode(d.discount_type,
            -- Справочник считаем как Директ, т.к. шкала по общему обороту
            12, 7,
            -- Вся медийка под номером 1 будет
            2, 1, 3, 1, 37, 1,
            d.discount_type) as discount_type,
       d.nds,
       d.client_id,
       d.from_dt, d.till_dt, d.till_dt_fc,
       d.nds_count, d.currency_count,
       d.amt_w_nds_rub,
       d.amt_rub_not_lc,
       d.amt_rub_lc,
       d.amt_rub_not_lc + d.amt_rub_lc as amt_rub,
       d.amt_rub_not_lc                as amt_for_forecast,
       -- ТЗ: Полугодовая премия начисляется Агентству только при условии, если
       -- стоимость фактически оказанных Яндексом в течение соответствующего
       -- Отчетного полугодия Услуг по сервису Директ, связанных с размещением
       -- Материалов одного Клиента Агентства < 70% (округляется до целых
       -- процентов)
       l.max_ratio,
       0                        as excluded,
       case
          -- BALANCE-20688
        when nds_count > 1                  then 1
        when currency_count > 1             then 1
          -- BALANCE-25859: БОК только для Директа и Справочника
        when round(l.max_ratio, 2) >= 0.7
         and discount_type in (7, 12)       then 1
        else 0
       end          as failed
  from (
        select d.contract_eid, d.contract_id,
               d.contract_from_dt,
               d.contract_till_dt,
               d.discount_type, d.commission_type,
               d.client_id, d.nds,
               d.nds_count, d.currency_count,
               d.from_dt, d.till_dt, d.till_dt_fc,
               d.amt_w_nds_rub,
               d.amt_rub            as amt_rub_not_lc,
               0                    as amt_rub_lc
          from s_skv_not_lc d
         union all
        select d.contract_eid, d.contract_id,
               d.contract_from_dt,
               d.contract_till_dt,
               d.discount_type, d.commission_type,
               d.client_id, d.nds,
               d.nds_count, d.currency_count,
               d.from_dt, d.till_dt, d.till_dt as till_dt_fc,
               d.amt_w_nds_rub,
               0                    as amt_rub_not_lc,
               d.amt_rub            as amt_rub_lc
          from s_skv_lc d
       ) d
       -- смотрим максимальную долю по клиенту по директу за полугодие
  join bo.v_opt_2015_base_skv_less_70   l on l.contract_id = d.contract_id
                                         and l.from_dt = d.from_dt;
