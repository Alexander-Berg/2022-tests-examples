
  CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."V_OPT_2017_VERTICALS_SPEC_SKV" ("CONTRACT_EID", "CONTRACT_ID", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "DISCOUNT_TYPE", "COMMISSION_TYPE", "CLIENT_ID", "FROM_DT", "TILL_DT", "TILL_DT_FC", "EXCLUDED", "FAILED", "AMT_W_NDS", "AMT_FOR_FORECAST", "AMT") AS 
  with
s_years as (
    -- Показываем предыдущий, текущий и будущий годы
    select d.dt from_dt, add_months(d.dt, 12) - 1/24/60/60 as till_dt
      from (
         select add_months(trunc(sysdate, 'YYYY'), 12*(level-2)) as dt
           from dual
        connect by level <= 3
           ) d
),
s_acts as (
    select b.*
      from bo.v_opt_2015_acts_f     b
      join bo.t_invoice             i on i.id = b.invoice_id
                                     and i.firm_id = 12
     where b.commission_type = 3
),
s_skv as (
    select d.contract_eid, d.contract_id,
           d.contract_from_dt,
           d.contract_till_dt,
           d.currency, d.nds, d.discount_type, d.commission_type,
           d.client_id, d.is_loyal,
           -- для прогноза (для ЛК не нужно, т.к. ЛК не учитываем в прогнозе)
           -- чтобы знать, за какой период брать оборот для проноза
           d.till_dt                                    as till_dt_fc,
           -- период
           h.from_dt                                    as from_dt,
           trunc(h.till_dt)                             as till_dt,
           sum(d.amt_w_nds_rub)                         as amt_w_nds,
           sum(d.amt_rub)                               as amt
      from s_acts                   d
      join s_years                  h  on d.act_dt between h.from_dt
                                                       and h.till_dt
     group by h.from_dt, trunc(h.till_dt), d.till_dt,
              d.contract_from_dt,
              d.contract_till_dt,
              d.contract_eid, d.contract_id,
              d.currency, d.nds, d.discount_type, d.commission_type,
              d.client_id, d.is_loyal
)
select d.contract_eid, d.contract_id,
       d.contract_from_dt,
       d.contract_till_dt,
       d.discount_type, d.commission_type,
       d.client_id,
       d.from_dt, d.till_dt, d.till_dt_fc,
       0                        as excluded,
       0                        as failed,
       d.amt_w_nds,
       d.amt                    as amt_for_forecast,
       d.amt
  from s_skv d
;
