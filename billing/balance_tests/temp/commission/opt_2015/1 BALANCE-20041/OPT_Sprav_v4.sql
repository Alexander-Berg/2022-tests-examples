with
-- опорные даты
s_dates as (
    select date'2015-03-01'   as start_dt
      from dual
),
s_base as (
select distinct
           contract_eid                             as contract_eid,
           contract_id                              as contract_id,
           invoice_eid                              as invoice_eid,
           invoice_id                               as invoice_id,
           invoice_dt                               as invoice_dt,
           contract_from_dt                         as contract_from_dt,
           contract_till_dt                         as contract_till_dt,
           currency                                 as currency,
           nds                                      as nds, 
           nds_pct                                  as nds_pct,
           loyal_client                             as loyal_clients,
       -- BALANCE-17175
           decode(
            nvl(commission_type, discount_type),
            22, 1,
            nvl(commission_type, discount_type)
           )                                        as discount_type,
           payment_type                             as payment_type, 
                                              -- ?  as commission_payback_type
           commission_payback_pct                   as commission_payback_pct,
           contract_commission_type                 as commission_type
  from xxxx_new_comm_contract_basic
  where (
                                                -- BALANCE-17175
                                                (
                                                    -- только счета, созданные в 2015
--                                                    invoice_dt >= date'2015-03-01' and
                                                    -- только базовые/профы
                                                    contract_commission_type in (1, 2, 8) and
                                                    -- то учитываем еще и код 22
                                                    nvl(commission_type,
                                                        discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 19, 22)
                                                )
                                                or
                                                (
                                                    -- иначе, как ранее
                                                    nvl(commission_type,
                                                        discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 19)
                                                )
                                              )
),
s_acts as (
    select b.contract_eid,
           b.contract_id,
           b.invoice_eid,
           b.invoice_id,
           b.currency,
           b.nds,
           b.payment_type,
           b.discount_type,
           xxx.client_id                                      as client_id,
           trunc(xxx.act_dt, 'MM')                            as from_dt,
           add_months(trunc(xxx.act_dt, 'MM'), 1) - 1/84600   as till_dt,
           xxx.amount                                         as amt_w_nds,
           xxx.amount-xxx.amount_nds-xxx.amount_nsp           as amt,
           xxx.amount*cr.rate                                 as amt_w_nds_rub,
           (xxx.amount-xxx.amount_nds-xxx.amount_nsp)*cr.rate as amt_rub
      from s_base        b
      join xxxx_new_comm_contract_basic     xxx on b.invoice_id = xxx.invoice_id
                                              and xxx.hidden <4
                                              and xxx.act_dt >= (select start_dt from s_dates)
      join xxxx_currency_rate              cr on cr.cc = b.currency
                                              and cr.rate_dt = trunc(xxx.act_dt)
     where b.commission_type = 5
       and b.discount_type = 12
),
--
-- основная выборка по оплатам
--
s_payments as (
    select b.contract_eid,
           b.contract_id,
           b.invoice_eid,
           b.invoice_id,
           b.currency,
           b.nds,
           b.payment_type,
           b.discount_type,
           trunc(oebs.comiss_date, 'MM')                 as from_dt,
           add_months(
            trunc(oebs.comiss_date, 'MM'), 1)-1/84600    as till_dt,
           oebs.oebs_payment*100/(100 + b.nds*b.nds_pct) as amt,
           oebs.oebs_payment                             as amt_w_nds
      from s_base        b
      join xxxx_oebs_cash_payment_test     oebs on oebs.invoice_id = b.invoice_id
                                                and oebs.comiss_date >= (
                                                        select start_dt from s_dates)
                                                and oebs.comiss_date is not null
     where b.commission_type = 5
       and b.discount_type = 12
),
-- Штрафы
--  - оборот по договору >= 5к
--  - клиентов >= 5
s_kv_control as (
    select d.*,
           case
           when amt_rub >= 5000 and
                client_count >= 5
            and nds_count = 1
            and currency_count = 1
           then 0
           else 1
            end as failed
      from (
        select contract_id, from_dt, till_dt,
               nds_count, currency_count,
               count(client_id)                             as client_count,
               sum(amt_rub)                                 as amt_rub
          from (
                select d.*,
                       -- Проверяем, что по договору всегда была
                       -- 1 валюта и 1 НДС
                       count(distinct nds)
                        over (partition by contract_id) as nds_count,
                       count(distinct currency)
                        over (partition by contract_id) as currency_count
                  from s_acts d
               )
         group by contract_id, from_dt, till_dt,
                  nds_count, currency_count
           ) d
),
-- КВ
s_kv_pre as (
    select d.contract_id, d.contract_eid,
           d.from_dt, d.till_dt, d.payment_type,
           d.nds, d.currency,
           d.discount_type,
           -- начисление
           sum(d.amt_to_charge)               as turnover_to_charge,
           sum(d.amt_to_charge*0.25)          as reward_to_charge,
           -- оплата
           sum(d.amt_to_pay)                  as turnover_to_pay,
           sum(d.amt_to_pay_w_nds)            as turnover_to_pay_w_nds,
           sum(d.amt_to_pay*0.25)             as reward_to_pay
      from (
            select contract_id, contract_eid,
                   from_dt, till_dt, payment_type,
                   discount_type, currency, nds,
                   amt          as amt_to_charge,
                   amt_w_nds    as amt_to_charge_w_nds,
                   0            as amt_to_pay,
                   0            as amt_to_pay_w_nds
              from s_acts
             union all
            select contract_id, contract_eid,
                   from_dt, till_dt, payment_type,
                   discount_type, currency, nds,
                   0            as amt_to_charge,
                   0            as amt_to_charge_w_nds,
                   amt          as amt_to_pay,
                   amt_w_nds    as amt_to_pay_w_nds
              from s_payments
           )                d
      join s_kv_control     f on f.contract_id = d.contract_id
                             and f.from_dt = d.from_dt
                             and f.till_dt = d.till_dt
                             and f.failed = 0
     group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt, payment_type,
              d.discount_type, d.currency, d.nds
),
-- КВ с контролем, что оплат не более, чем актов
s_kv as (
    select contract_eid, contract_id, from_dt, till_dt,
           currency, nds, discount_type,
           -- к перечислению
           turnover_to_charge,
           reward_to_charge,
           -- к начислению
           decode(payment_type,
                2, turnover_to_charge,
                3, turnover_to_pay)             as turnover_to_pay,
           decode(payment_type,
                2, 0,
                3, turnover_to_pay_w_nds)       as turnover_to_pay_w_nds,
           decode(payment_type,
                2, reward_to_charge,
                (least(reward_to_charge_sum, reward_to_pay_sum) -
                    least(reward_to_charge_sum_prev, reward_to_pay_sum_prev)
                ))                              as reward_to_pay
      from (
            select d.*,
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
                                    reward_to_pay       as reward_to_pay_sum_prev
              from s_kv_pre d
           ) s
)
--
-- результирующий запрос
--
select contract_id,
       contract_eid,
       from_dt,
       till_dt,
       nds,
       currency,
       discount_type,
       reward_type,
       turnover_to_charge,                          -- оборот к начислению
       reward_to_charge,                            -- к начислению
       turnover_to_pay_w_nds,
       turnover_to_pay,                             -- оборот к перечислению
       reward_to_pay                                -- к перечислению
  from (
        select contract_eid, contract_id,
               from_dt, till_dt,
               discount_type,
               currency, nds,
               turnover_to_charge,
               reward_to_charge,
               turnover_to_pay,
               turnover_to_pay_w_nds,
               reward_to_pay,
               1            as reward_type
          from s_kv
       );
