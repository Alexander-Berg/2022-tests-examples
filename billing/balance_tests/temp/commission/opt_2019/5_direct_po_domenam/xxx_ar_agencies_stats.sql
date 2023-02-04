
  CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."XXX_AR_AGENCIES_STATS" ("AGENCY_ID", "SERVICE_ID", "SERVICE_ORDER_ID", "CLIENT_ID", "MONTH", "AMT") AS 
  with
  counting_date as (select date'2019-04-03' as dt from dual)
  --select * from counting_date;
,

s_dates as (
    select add_months(trunc((select dt from counting_date), 'MM'), -1)     as from_dt,
           trunc((select dt from counting_date), 'MM') - 1/24/60/60        as till_dt
      from dual
)
select  /*+ parallel(8) */
       a.client_id                                              as agency_id,
       o.service_id,
       o.service_order_id,
       o.client_id,
       trunc(a.dt, 'MM')                                        as month,
       sum((at.amount - at.amount_nds - at.amount_nsp)*cr.rate) as amt
       
      from xxxx_acts a 
      join xxxx_new_comm_contract_basic  xxx on xxx.invoice_id = a.invoice_id
      join xxxx_act_trans                   at on at.act_id = a.act_id
      join xxxx_order                       o  on o.order_id = at.parent_order_id                                       
                                             
      join xxxx_currency_rate              cr on cr.cc = xxx.currency
                                              and cr.rate_dt = trunc(a.dt)
     join s_dates          d on a.dt between d.from_dt and d.till_dt
  where a.hidden < 4
   and nvl(at.commission_type,
      nvl(xxx.commission_type, xxx.discount_type)) in (7)
   and a.client_id in (select agency_id from xxx_opt_2015_acts
                        where from_dt = d.from_dt)
 group by a.client_id, trunc(a.dt, 'MM'),
          o.service_id, o.service_order_id,
          o.client_id
          ;
