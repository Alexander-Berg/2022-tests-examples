CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."V_OPT_2015_MARKET_Q" ("CONTRACT_EID", "CONTRACT_ID", "AGENCY_ID", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "DISCOUNT_TYPE", "FROM_DT", "TILL_DT", "TILL_DT_FC", "AMT_W_NDS_RUB", "AMT", "AMT_FOR_FORECAST", "AMT_AG", "AMT_AG_Q", "AMT_AG_PREV", "AMT_AG_PREV_FM", "FAILED", "EXCLUDED") AS 
  with
s_quarters AS (
    select d.dt from_dt, add_months(d.dt, 3) - 1/24/60/60 as till_dt
      from (
         select add_months(date'2015-03-01', 3*(level-1)) as dt
           from dual
        connect by level <= 20
           ) d
)
-- обороты по-квартально по каждому договору
-- 2016 года
, s_curr_contracts as (
    select d.contract_eid,        d.contract_id,        d.agency_id,
           -- BALANCE-23005
           min(d.contract_from_dt)                          as contract_from_dt,
           max(d.contract_till_dt)                          as contract_till_dt,
           q.from_dt, q.till_dt,
           d.from_dt                                        as month,
           sum(d.amt_w_nds_rub)                             as amt_w_nds_rub,
           sum(d.amt_rub)                                   as amt
      from bo.v_opt_2015_acts_f     d
      join s_quarters               q on d.from_dt between q.from_dt and q.till_dt
     where d.commission_type in (12, 13)
        -- BALANCE-22819
       and d.nds_count = 1
        -- BALANCE-22850
       and d.currency_count = 1
        -- BALANCE-23037
       and d.contract_till_dt > date'2017-03-01'
     group by d.contract_eid,       d.contract_id,      d.agency_id,
           q.from_dt, q.till_dt,
           d.from_dt
)
-- Тек обороты по аг-вам, по которым есть обороты в 2016 году
, s_agency_stats_curr as (
    select d.*,
           -- нарастающий итог по аг-ву с начала квартала
           sum(amt) over (partition by agency_id, from_dt
                               order by month)                      as amt_m,
           -- итог за квартал
           sum(amt) over (partition by agency_id, from_dt)          as amt_q
      from (
    select a.client_id                                              as agency_id,
           q.from_dt,
           trunc(a.dt, 'MM')                                        as month,
           sum((a.amount - a.amount_nds - a.amount_nsp)*cr.rate)    as amt
      from bo.t_act         a
      join bo.t_invoice     i on i.id = a.invoice_id
                             and nvl(i.commission_type, i.discount_type) in (11)
      join biee.mv_currency_rate    cr on cr.cc = i.currency
                                      and cr.rate_dt = trunc(a.dt)
      join s_quarters       q on a.dt between q.from_dt and q.till_dt
     where a.hidden < 4
       and a.client_id in (select agency_id from s_curr_contracts)
     group by a.client_id, q.from_dt, trunc(a.dt, 'MM')
           ) d
)
-- Прошлогодние обороты по аг-вам, по которым есть обороты в 2016 году
, s_agency_stats_prev as (
    SELECT a.client_id                                            as agency_id,
           add_months(q.from_dt, 12)                              as from_dt,
           sum(decode(q.from_dt,
                trunc(a.dt, 'MM'), (a.amount-a.amount_nds-a.amount_nsp)*cr.rate,
                0))                                               as amt_fm,
           sum((a.amount - a.amount_nds - a.amount_nsp)*cr.rate)  as amt
      from bo.t_act         a
      join bo.t_invoice     i on i.id = a.invoice_id
                             and nvl(i.commission_type, i.discount_type) in (11)
      join biee.mv_currency_rate    cr on cr.cc = i.currency
                                      and cr.rate_dt = trunc(a.dt)
      join s_quarters       q on a.dt between q.from_dt and q.till_dt
     where a.hidden < 4
       and a.client_id in (select agency_id from s_curr_contracts)
     group by a.client_id, add_months(q.from_dt, 12)
)
select d.contract_eid,        d.contract_id,        d.agency_id,
       d.contract_from_dt,    d.contract_till_dt,   11 as discount_type,
       d.from_dt, d.till_dt,
       d.month                  as till_dt_fc,
       d.amt_w_nds_rub          as amt_w_nds_rub,
       d.amt                    as amt,
       d.amt                    as amt_for_forecast,
       -- За месяц (нарастающий итог)
       cs.amt_m                 as amt_ag,
       -- За квартал
       cs.amt_q                 as amt_ag_q,
       ps.amt                   as amt_ag_prev,
       ps.amt_fm                as amt_ag_prev_fm,
       decode(nvl(ps.amt_fm, 0),
        0, 1, 0)                as failed,
       case
           when contract_eid in (select contract_eid
                                   from bo.v_opt_2015_exclusions)
           then 1
           else 0
       end                      as excluded
  from s_curr_contracts      d
  join s_agency_stats_curr   cs on cs.agency_id = d.agency_id
                               and cs.from_dt = d.from_dt
                               and cs.month = d.month
  left outer
  join s_agency_stats_prev   ps on ps.agency_id = d.agency_id
                               and ps.from_dt = d.from_dt
;
