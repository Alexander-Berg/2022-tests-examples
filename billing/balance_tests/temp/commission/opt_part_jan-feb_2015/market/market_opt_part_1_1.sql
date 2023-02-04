with
s_invoices as (
    select x.contract_eid                             as contract_eid,
           x.contract_id                              as contract_id,
           x.contract_from_dt                         as contract_from_dt,
           x.contract_till_dt                         as contract_till_dt,
           x.invoice_eid                              as invoice_eid,
           x.invoice_id                               as invoice_id,
           x.invoice_dt                               as invoice_dt,
           x.currency                                 as currency,
           x.nds                                      as nds,
           x.nds_pct                                  as nds_pct,
           nvl(x.commission_type, x.discount_type)      as discount_type,
           x.contract_commission_type                 as commission_type
		     
    from xxxx_new_comm_contract_basic x
    -- ¬ ”краине нет оптовой схемы
      where contract_commission_type in (52)
      and nvl(commission_type, discount_type) in (11, 111)
    group by contract_eid, contract_id, contract_from_dt, contract_till_dt,
    invoice_eid, invoice_id, invoice_dt, currency, nds, nds_pct,
    nvl(commission_type, discount_type), contract_commission_type, endbuyer_id
)

--select * from s_invoices;
--select * from xxxx_new_comm_contract_basic;
,
s_acts as (
    select d.contract_eid, d.contract_id,
           d.currency, d.nds,
           d.commission_type,
           d.from_dt, d.till_dt,
           sum(decode(d.discount_type, 11,  d.amt, 0))              as amt,
           sum(decode(d.discount_type, 11,  d.amt_with_nds, 0))     as amt_with_nds,
           sum(decode(d.discount_type, 11,  d.amt, 0))              as amt_cpc,
           sum(decode(d.discount_type, 111, d.amt, 0))              as amt_cpa
      from (
        select i.contract_eid,
               i.contract_id,
               i.currency,
               i.nds,
               i.commission_type,
               i.discount_type,
               trunc(xxx.act_dt, 'MM')                                as from_dt,
               add_months(trunc(xxx.act_dt, 'MM'), 1) - 1/84600       as till_dt,
               xxx.amount                                        as amt_with_nds,
               xxx.amount-xxx.amount_nds-xxx.amount_nsp            as amt
          from s_invoices           i
          join xxxx_new_comm_contract_basic           xxx  on i.invoice_id = xxx.invoice_id
                                     		 and xxx.hidden < 4
          ) d
     group by d.contract_eid, d.contract_id,
              d.currency, d.nds,
              d.commission_type,
              d.from_dt, d.till_dt
)
--select * from s_acts;
,
s_payments as (
    select d.contract_eid, d.contract_id,
           d.currency, d.nds,
           d.commission_type,
           d.from_dt, d.till_dt,
           sum(decode(d.discount_type, 11,  d.amt, 0))              as amt,
           sum(decode(d.discount_type, 11,  d.amt_with_nds, 0))     as amt_with_nds,
           sum(decode(d.discount_type, 11,  d.amt, 0))              as amt_cpc,
           sum(decode(d.discount_type, 111, d.amt, 0))              as amt_cpa
      from (
        select i.contract_eid,
               i.contract_id,
               i.currency,
               i.nds,
               i.commission_type,
               i.discount_type,
               trunc(oebs.comiss_date, 'MM')                            as from_dt,
               add_months(trunc(oebs.comiss_date, 'MM'), 1) - 1/84600   as till_dt,
               oebs.oebs_payment                                                 as amt_with_nds,
               oebs.oebs_payment*100/(100 + i.nds*i.nds_pct)                     as amt
          from s_invoices               i
          -- платежи
          join xxxx_oebs_cash_payment_test      oebs on oebs.invoice_id = i.invoice_id
											 and oebs.comiss_date >= date'2013-03-01'
                                            and oebs.comiss_date is not null
           ) d
     group by d.contract_eid, d.contract_id,
              d.currency, d.nds,
              d.commission_type,
              d.from_dt, d.till_dt
)

--select * from s_payments;
,
s_kv_src as (
    select d.*,
           -- ƒелькредере считаем вместе с  ¬
           -- потом разделим, когда проверим, что
           -- оплаты <= актов
           amt_to_charge*(0.08+0.02)            as reward_to_charge,
           amt_to_pay*(0.08+0.02)               as reward_to_pay,
           0.1*case
            when amt_cpa_to_charge > amt_cpc_to_charge
                then amt_cpc_to_charge
                else amt_cpa_to_charge
           end                                  as dkv_to_charge,
           0.1*case
            when amt_cpa_to_pay > amt_cpc_to_pay
                then amt_cpc_to_pay
                else amt_cpa_to_pay
           end                                  as dkv_to_pay
      from (
            select contract_eid, contract_id, currency, nds,
                   commission_type, from_dt, till_dt,
                   sum(amt_to_charge)                       as amt_to_charge,
                   sum(amt_to_charge_with_nds)              as amt_to_charge_with_nds,
                   sum(amt_cpa_to_charge)                   as amt_cpa_to_charge,
                   sum(amt_cpc_to_charge)                   as amt_cpc_to_charge,
                   sum(amt_to_pay)                          as amt_to_pay,
                   sum(amt_to_pay_with_nds)                 as amt_to_pay_with_nds,
                   sum(amt_cpa_to_pay)                      as amt_cpa_to_pay,
                   sum(amt_cpc_to_pay)                      as amt_cpc_to_pay
              from (
                    select contract_eid, contract_id, currency, nds,
                           commission_type, from_dt, till_dt,
                           amt                  as amt_to_charge,
                           amt_with_nds         as amt_to_charge_with_nds,
                           amt_cpc              as amt_cpc_to_charge,
                           amt_cpa              as amt_cpa_to_charge,
                           0                    as amt_to_pay,
                           0                    as amt_to_pay_with_nds,
                           0                    as amt_cpc_to_pay,
                           0                    as amt_cpa_to_pay
                      from s_acts
                     union all
                    select contract_eid, contract_id, currency, nds,
                           commission_type, from_dt, till_dt,
                           0                    as amt_to_charge,
                           0                    as amt_to_charge_with_nds,
                           0                    as amt_cpc_to_charge,
                           0                    as amt_cpa_to_charge,
                           amt                  as amt_to_pay,
                           amt_with_nds         as amt_to_pay_with_nds,
                           amt_cpc              as amt_cpc_to_pay,
                           amt_cpa              as amt_cpa_to_pay
                      from s_payments
                   )
             group by contract_eid, contract_id, currency, nds,
                      commission_type, from_dt, till_dt
           ) d
)
--select * from s_kv_src;
,
s_kv as (
    select contract_eid, contract_id, from_dt, till_dt,
           commission_type, currency, nds,
           -- к перечислению
           amt_to_charge                        as turnover_to_charge,
           reward_to_charge,
           dkv_to_charge,
           -- к начислению
           amt_to_pay                           as turnover_to_pay,
           amt_to_pay_with_nds                  as turnover_to_pay_w_nds,
           (least(reward_to_charge_sum, reward_to_pay_sum) -
                least(reward_to_charge_sum_prev, reward_to_pay_sum_prev)
           )                                    as reward_to_pay,
           (least(dkv_to_charge_sum, dkv_to_pay_sum) -
                least(dkv_to_charge_sum_prev, dkv_to_pay_sum_prev)
           )                                    as dkv_to_pay
      from (
            select d.*,
                    -- дл€ контрол€ оплат
                   sum(reward_to_charge)
                    over(partition by contract_id
                             order by from_dt)          as reward_to_charge_sum,
                   sum(reward_to_charge)
                    over(partition by contract_id
                             order by from_dt) -
                                    reward_to_charge    as reward_to_charge_sum_prev,
                   sum(reward_to_pay)
                    over(partition by contract_id
                             order by from_dt)          as reward_to_pay_sum,
                   sum(reward_to_pay)
                    over(partition by contract_id
                             order by from_dt) -
                                    reward_to_pay       as reward_to_pay_sum_prev,
                    -- дл€ контрол€ оплат дкв
                   sum(dkv_to_charge)
                    over(partition by contract_id
                             order by from_dt)          as dkv_to_charge_sum,
                   sum(dkv_to_charge)
                    over(partition by contract_id
                             order by from_dt) -
                                    dkv_to_charge       as dkv_to_charge_sum_prev,
                   sum(dkv_to_pay)
                    over(partition by contract_id
                             order by from_dt)          as dkv_to_pay_sum,
                   sum(dkv_to_pay)
                    over(partition by contract_id
                             order by from_dt) -
                                    dkv_to_pay          as dkv_to_pay_sum_prev
              from s_kv_src d
           ) s
),

s_src as (
select contract_id,
       contract_eid,
       from_dt,
       till_dt,
       nds,
       currency,
       commission_type,
       11                   as discount_type,
       1                    as reward_type,
       -- к начислению
       turnover_to_charge,                              -- оборот к начислению
       reward_to_charge*.8  as reward_to_charge,        -- комисси€ к начислению
       reward_to_charge*.2  as delkredere_to_charge,    -- делькредере к начислению
       dkv_to_charge,
       -- к перечислению
       turnover_to_pay,                                 -- оборот к перечислению
       turnover_to_pay_w_nds,                           -- оборот к перечислению (с Ќƒ—)
       reward_to_pay*.8     as reward_to_pay,           -- комисси€ к перечислению
       reward_to_pay*.2     as delkredere_to_pay,       -- делькредере к перечислению
       dkv_to_pay
  from s_kv)
  
,
-- ѕодготовка к результату: вычитаем посчитанную ком.часть
s_pre as (
    select 
    old.contract_id,
           old.contract_eid,
           old.from_dt,
           old.till_dt,
           old.nds,
           old.currency,
           old.commission_type,
           -- к начислению
           old.turnover_to_charge   - nvl(com.turnover_to_charge, 0)       as turnover_to_charge,              -- оборот к начислению
           old.reward_to_charge - nvl(com.reward_to_charge, 0)       as reward_to_charge,        -- комисси€ к начислению
           old.delkredere_to_charge - nvl(com.delkredere_to_charge, 0)   as delkredere_to_charge,    -- делькредере к начислению
           old.dkv_to_charge       - nvl(com.dkv_to_charge, 0)          as dkv_to_charge,
           -- к перечислению
           old.turnover_to_pay_w_nds - nvl(com.turnover_to_pay_w_nds, 0)  as turnover_to_pay_w_nds,
           old.turnover_to_pay       - nvl(com.turnover_to_pay, 0)        as turnover_to_pay,
           old.reward_to_pay    - nvl(com.reward_to_pay, 0)          as reward_to_pay,           -- комисси€ к перечислению
           old.delkredere_to_pay    - nvl(com.delkredere_to_pay, 0)      as delkredere_to_pay,       -- делькредере к перечислению
           old.dkv_to_pay          - nvl(com.dkv_to_pay, 0)             as dkv_to_pay
      from s_src         old
      left outer
        -- смотрим, что было посчитано в ком.части
        -- чтобы вычесть
      join xxxx_commission_part  com on com.contract_id = old.contract_id
                                                   and com.from_dt = old.from_dt
                                                   and com.till_dt = old.till_dt
                                                   and com.nds = old.nds
                                                   and com.currency = old.currency
                                                   and com.reward_type = 1
                                                   and com.discount_type = 11
       where date'2015-01-01' between old.from_dt and old.till_dt
       or date'2015-02-01' between old.from_dt and old.till_dt
)
--select *from s_pre;
--
-- результирующий запрос
--





select contract_id,
       contract_eid,
       min(from_dt)                     as from_dt,
       max(till_dt)                     as till_dt,
       nds,
       currency,
       commission_type,
       11                               as discount_type,
       201                              as reward_type,
       -- к начислению
       sum(turnover_to_charge)          as turnover_to_charge,
       sum(reward_to_charge)            as reward_to_charge,
       sum(delkredere_to_charge)        as delkredere_to_charge,
       sum(dkv_to_charge)               as dkv_to_charge,
       -- к перечислению
       sum(turnover_to_pay)             as turnover_to_pay,
       sum(turnover_to_pay_w_nds)       as turnover_to_pay_w_nds,
       sum(reward_to_pay)               as reward_to_pay,
       sum(delkredere_to_pay)           as delkredere_to_pay,
       sum(dkv_to_pay)                  as dkv_to_pay
  from s_pre  
   group by contract_id, contract_eid,
          nds, currency, commission_type
  order by contract_id,
       contract_eid,
       from_dt,
       till_dt,
       nds,
       currency,
       commission_type,discount_type,reward_type,turnover_to_charge,reward_to_charge,delkredere_to_charge;