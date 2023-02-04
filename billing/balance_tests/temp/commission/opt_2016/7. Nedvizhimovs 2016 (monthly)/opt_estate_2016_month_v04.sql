with
-- опорные даты
s_dates as (
        -- начало фин. года
    select date'2015-03-01'   as fin_year_dt
      from dual
),
s_base as (
select distinct
           c.contract_eid                             as contract_eid,
               case
        when c.contract_commission_type in (4,10,12,13)
         and c.firm_id  = 1
        then
        nvl(
        (select distinct l.value_num
                from xxxx_new_comm_contract_basic l
               where l.contract_id  = c.contract_id
                 and l.code = 'LINK_CONTRACT_ID'
--                 )
                 and l.value_num is not null),
           c.contract_id)
        else c.contract_id
       end                               as contract_id,
           c.invoice_eid                              as invoice_eid,
           c.invoice_id                               as invoice_id,
           c.invoice_dt                               as invoice_dt,
           c.contract_from_dt                         as contract_from_dt,
           c.contract_till_dt                         as contract_till_dt,
           c.currency                                 as currency,
           c.nds                                      as nds, 
           c.nds_pct                                  as nds_pct,
           c.loyal_client                             as loyal_clients,
       -- BALANCE-17175
           decode(
            nvl(c.commission_type, c.discount_type),
            22, 1,
		      	29, 1,
            nvl(c.commission_type, c.discount_type)
           )                                        as discount_type,
		   nvl(c.commission_type, c.discount_type)  as discount_type_src,
           c.payment_type                             as payment_type, 
                                              -- ?  as commission_payback_type
           c.commission_payback_pct                   as commission_payback_pct,
           c.contract_commission_type                 as commission_type
  from xxxx_new_comm_contract_basic c
  where (
                                                -- BALANCE-17175
                                                (
                                                    -- только базовые/профы
                                                    c.contract_commission_type in (1, 2, 8) and
                                                    -- то учитываем еще и код 22
                                                    nvl(c.commission_type,
                                                        c.discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 17, 19, 22, 25, 28, 29)
                                                )
                                                or
                                                (
                                                    -- Маркет
                                                    c.contract_commission_type in (12, 13) and
                                                    nvl(c.commission_type, c.discount_type) in
                                                    (11)
                                                )
                                                or
                                                (
                                                    -- По Недвижимости смотрим всё
                                                    c.contract_commission_type in (10) and 1=1
                                                )
                                               or
                                                (
                                                    -- иначе, как ранее
                                                    nvl(c.commission_type,
                                                        c.discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 17, 19, 28, 29)
                                                )
                                              )
)
--select * from s_base;
,
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
    b.commission_type,
    b.discount_type,
    case
    when nvl(xxx.is_loyal, 0) = 1 and b.discount_type = 7
      then 1
      else 0
    end                                              as is_loyal,
    xxx.client_id                                      as client_id,
    xxx.agency_id                                      as agency_id,
    xxx.act_dt                                         as act_dt,
    trunc(xxx.act_dt, 'MM')                            as from_dt,
    add_months(trunc(xxx.act_dt, 'MM'), 1) - 1/84600   as till_dt,
    -- Проверяем, что по договору всегда была -- 1 валюта и 1 НДС
	  count(distinct b.nds)      over (partition by b.contract_id) as nds_count,
    count(distinct b.currency) over (partition by b.contract_id) as currency_count,
    xxx.amount                                         as amt_w_nds,
    xxx.amount-xxx.amount_nds-xxx.amount_nsp           as amt,
    xxx.amount*cr.rate                                 as amt_w_nds_rub,
    (xxx.amount-xxx.amount_nds-xxx.amount_nsp)*cr.rate as amt_rub
    from s_base        b
    join xxxx_new_comm_contract_basic     xxx on b.invoice_id = xxx.invoice_id
                                       and xxx.hidden <4
    join xxxx_currency_rate              cr on cr.cc = b.currency
                                       and cr.rate_dt = trunc(xxx.act_dt)
    where b.commission_type in (1, 2, 3, 4, 5, 6, 8, 10, 11, 12, 13)
   and (
          -- base, prof
          (
            b.commission_type in (1, 2, 8)
        and (
                -- BALANCE-22085
                -- В новых актах по новым и продленным договорам
                --  * отбрасываем маркет (11)
                --  * включаем Авто (25)k
            xxx.act_dt >= date'2016-03-01' and 
            b.contract_till_dt >= date'2016-03-01' and 
            b.discount_type in (1, 2, 3, 7, 12, 25) and
            -- BALANCE-22203
            -- BALANCE-22331: в новых условиях оставляем только рубли
             b.currency = 'RUR'
            -- В новых актах по непродленным договорам и старых актах
            -- оставляем старые условия
            -- BALANCE-22319
             or xxx.act_dt >= date'2016-03-01' and b.contract_till_dt < date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
             or xxx.act_dt  < date'2016-03-01' and b.discount_type in  (1, 2, 3, 7, 11, 12)
            )
          )
          -- ua
       or (b.commission_type = 6 and b.discount_type in (1, 2, 3, 7, 12))
          -- spec
       or (b.commission_type = 3 and b.discount_type in (17))
          -- auto
       or (b.commission_type = 4 and b.discount_type in (19))
          -- sprav
       or (b.commission_type = 5 and b.discount_type = 12)
           -- estate
       or (b.commission_type = 10 and 1=1)
          -- audio
       or (b.commission_type = 11 and b.discount_type_src = 29)
          -- market
       or (b.commission_type in (12, 13) and b.discount_type = 11  
                                        -- BALANCE-22578
                                         and b.currency = 'RUR')
       )
)
--select * from s_acts;
,

s_acts_all as (
    select b.*
      from s_acts   b
     where b.commission_type = 10
)

--select * from s_acts_all;
,
-- ----------------------------------------------------------------------------
-- основная выборка по оплатам
-- ----------------------------------------------------------------------------
s_payments as (
    select b.contract_eid,
           b.contract_id,
           b.invoice_eid,
           b.invoice_id,
           b.invoice_dt,
           b.currency,
           b.nds,
           b.payment_type,
           b.discount_type,
           b.loyal_clients,
           oebs.comiss_date,
           trunc(oebs.comiss_date, 'MM')                as from_dt,
           add_months(
           trunc(oebs.comiss_date, 'MM'), 1)-1/84600   as till_dt,
           oebs.oebs_payment*100/(100 + b.nds*b.nds_pct) as amt,
           oebs.oebs_payment                             as amt_w_nds
      from s_base       b
      join xxxx_oebs_cash_payment_test     oebs on oebs.invoice_id = b.invoice_id
                                                and oebs.comiss_date >= (select fin_year_dt from s_dates)
                                                and oebs.comiss_date is not null
     where b.commission_type = 10
)
--select * from s_payments;
,
-- ----------------------------------------------------------------------------
-- Базовое КВ (месячное)
-- ----------------------------------------------------------------------------

s_kv_src as (
    select contract_eid, contract_id,
           currency, nds, payment_type,
           from_dt, till_dt,
           sum(amt_acts)            as amt_to_charge,
           sum(amt_oebs_w_nds)      as amt_to_pay_w_nds,
           sum(amt_oebs)            as amt_to_pay
      from (
            select contract_eid, contract_id, currency, nds, discount_type,
                   from_dt, till_dt, payment_type,
                   amt  as amt_acts, amt_w_nds  as amt_acts_w_nds,
                   0    as amt_oebs, 0          as amt_oebs_w_nds
              from s_acts_all
             union all
            select contract_eid, contract_id, currency, nds, discount_type,
                   from_dt, till_dt, payment_type,
                   0    as amt_acts, 0          as amt_acts_w_nds,
                   amt  as amt_oebs, amt_w_nds  as amt_oebs_w_nds
              from s_payments
           )
     group by contract_eid, contract_id, currency,
              nds, from_dt, till_dt, payment_type
),

--select * from s_kv_src;


-- Последний подписанный план для квартала
-- Попадание в квартал — дата начала действия ДС
s_plans as (
    select contract_id                                         as contract_id,
           plan_dt                                             as from_dt,
           plan_sum  as plan
      from xxxx_new_comm_contract_basic
     where plan_dt is not null
     and plan_dt < trunc(sysdate, 'MM')

)
--select * from s_plans;
,
s_kv_control as (
    select /*+ materialize */d.*,
           case
           when amt_rub >= plan
            and nds_count = 1
            and currency_count = 1
            and client_count >= 2
           then 0
           else 1
            end as failed2,
           case
           when amt_rub >= 30000
            and nds_count = 1
            and currency_count = 1
            and client_count >= 2
           then 0
           else 1
            end as failed
      from (
        select d.contract_id, d.from_dt, d.till_dt,
               nds_count, currency_count,
               p.plan  as plan,
               count(distinct client_id)                    as client_count,
               sum(amt_rub)                                 as amt_rub
          from s_acts_all   d
          left outer
          join s_plans      p on p.contract_id = d.contract_id
      
         group by d.contract_id, d.from_dt, d.till_dt, p.plan,
                  nds_count, currency_count
           ) d
)
--select * from s_plans;
--select * from s_kv_control;
--select * from s_kv_src;

--select d.* from s_kv_src   d
--      --left outer
--      join s_kv_control     f on f.contract_id = d.contract_id
--                             and f.from_dt = d.from_dt
--                             and f.till_dt = d.till_dt;
,
-- Сумма КВ без контроля акты <= оплат
s_kv_pre as (
    select d.contract_id, contract_eid,
           d.from_dt, d.till_dt, d.payment_type,
           d.nds, d.currency,
           sum(d.amt_to_charge)         as turnover_to_charge,
           sum(decode(f.failed, 0, d.amt_to_charge*0.3, 0) +
                decode(f.failed2, 0, d.amt_to_charge*0.1, 0)
               )                     as reward_to_charge,
           sum(d.amt_to_pay)            as turnover_to_pay,
           sum(d.amt_to_pay_w_nds)      as turnover_to_pay_w_nds,
           -- BALANCE-19851
           -- Штрафовать по оплатам не должны
           sum(d.amt_to_pay*0.4)        as reward_to_pay
      from s_kv_src         d
      left outer
      join s_kv_control     f on f.contract_id = d.contract_id
                             and f.from_dt = d.from_dt
                             and f.till_dt = d.till_dt
     group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt,
              d.currency, d.nds, d.payment_type
)
--select * from s_kv_pre;
,
-- КВ с контролем, что оплат не более, чем актов
s_kv as (
    select contract_eid, contract_id, from_dt, till_dt,
           currency, nds,
           -- к перечислению
           turnover_to_charge,
           reward_to_charge,
           -- к начислению
           decode(payment_type,
                -- предоплата
                2, turnover_to_charge,
                -- постоплата
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
--select * from s_kv;
,

-- результирующий запрос
s_opt_2015_estate as (select contract_id,
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
               null         as discount_type,
               currency, nds,
               turnover_to_charge,
               reward_to_charge,
               turnover_to_pay,
               turnover_to_pay_w_nds,
               reward_to_pay,
               1            as reward_type
          from s_kv
       )
)


select distinct
       s.contract_id,
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
      from s_opt_2015_estate
     )       s

order by contract_id, from_dt, discount_type, currency, nds;
