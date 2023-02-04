with
-- опорные даты
s_dates as (
        -- начало фин. года
    select date'2015-03-01'   as fin_year_dt
      from dual
),
-- Счета, по заказам которых не была предоставлена агентская скидка.
-- Их надо исключить из оборота
s_excluded_invoices as (
    select /*+ materialize */
           distinct xxx.invoice_id
      from bo.xxxx_new_comm_contract_basic xxx
      where xxx.exclude = 1)
      
--select * from s_excluded_invoices;
,


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
                                                    (1, 2, 3, 7, 11, 12, 14, 19, 22, 28)
                                                )
                                                or
                                                (
                                                    -- иначе, как ранее
                                                    nvl(commission_type,
                                                        discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 19, 28)
                                                )
                                              )
)
--select * from s_base;
,
-- ----------------------------------------------------------------------------
-- основная выборка по актам
-- ----------------------------------------------------------------------------
-- все акты по счетам
s_acts as (
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
           nvl(xxx.is_loyal, 0)                               as loyal_clients,
           xxx.client_id                                      as client_id,
           xxx.act_dt                                         as act_dt,
           trunc(xxx.act_dt, 'Q')                            as from_dt,
           add_months(trunc(xxx.act_dt, 'Q'), 3) - 1/84600   as till_dt,
           xxx.amount                                         as amt_w_nds,
           xxx.amount-xxx.amount_nds-xxx.amount_nsp           as amt
--           xxx.amount*cr.rate                                 as amt_w_nds_rub,
--           (xxx.amount-xxx.amount_nds-xxx.amount_nsp)*cr.rate as amt_rub
      from s_base        b
      join xxxx_new_comm_contract_basic     xxx on b.invoice_id = xxx.invoice_id
                                              and xxx.hidden <4
                                              and xxx.act_dt >= (select fin_year_dt from s_dates)
  --    join xxxx_currency_rate              cr on cr.cc = b.currency
  --                                            and cr.rate_dt = trunc(xxx.act_dt)
     where b.commission_type = 7
 -- Для сервисов: Авто.ру - Листинги
       and b.discount_type in (28)
        -- Исключаем счета, в которых не была предоставлена скидка
       and b.invoice_id not in (select invoice_id from s_excluded_invoices)
)
--select * from s_acts;
,

-- Последний подписанный план для квартала
-- Попадание в квартал — дата начала действия ДС
s_plans as (
    select contract_id                                         as contract_id,
           trunc(cl_dt, 'Q')                                    as from_dt,
           add_months(trunc(cl_dt, 'Q'), 3)-1/84600             as till_dt,
           max(cl_dt)                                           as plan_dt,
           max(q_plan) keep (dense_rank last order by cl_dt) as plan
      from xxxx_contract_signed_attr
     where code = 'AUTORU_Q_PLAN'
     group by contract_id, trunc(cl_dt, 'Q'),
              add_months(trunc(cl_dt, 'Q'), 3)-1/84600
)
--select * from s_plans;
,


s_dkv_src as (
    select contract_eid, contract_id,
           currency, nds, discount_type,
           from_dt, till_dt,
           sum(amt_w_nds)           as amt_w_nds,
           sum(amt)                 as amt
      from s_acts
     group by contract_eid, contract_id, currency,
              nds, discount_type, from_dt, till_dt
)
--select * from s_dkv_src;
,
s_dkv as (
    select d.contract_eid, d.contract_id, d.from_dt, d.till_dt,
           d.currency, d.nds, discount_type,
           d.amt_w_nds,
           d.amt,
           d.amt * case
            when d.amt >= p.plan and d.amt < p.plan*1.3 then 0.02
            when d.amt >= p.plan*1.3                    then 0.05
                                                        else 0
           end                      as reward
      from s_dkv_src            d
      join s_plans              p on p.contract_id = d.contract_id
                                 and p.from_dt = d.from_dt
                                 and p.till_dt = d.till_dt
)
--select * from s_dkv;
,
s_opt_2015_autoru as (select contract_id,
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
               amt          as turnover_to_charge,
               reward       as reward_to_charge,
               amt          as turnover_to_pay,
               amt_w_nds    as turnover_to_pay_w_nds,
               reward       as reward_to_pay,
               20           as reward_type
          from s_dkv
       )
)


-- результирующий запрос
select s.contract_id,
       s.contract_eid,
       s.from_dt,
       s.till_dt,
       s.nds,
       s.currency,
       s.discount_type,
       s.reward_type,
       s.reward_to_charge,
       s.reward_to_pay,
       s.delkredere_to_charge,
       s.delkredere_to_pay,
       s.dkv_to_charge,
       s.dkv_to_pay,
       s.turnover_to_charge,
       s.turnover_to_pay_w_nds,
       s.turnover_to_pay
  from (
    select contract_id,     contract_eid,
           from_dt,         till_dt,
           nds,             currency,
           discount_type,
           300 + reward_type    as reward_type,
           -- к начислению
           turnover_to_charge,
           reward_to_charge,
           0                    as delkredere_to_charge,
           0                    as dkv_to_charge,
           -- к перечислению
           turnover_to_pay_w_nds,
           turnover_to_pay,
           reward_to_pay,
           0                    as delkredere_to_pay,
           0                    as dkv_to_pay
      from s_opt_2015_autoru
     )       s
;
