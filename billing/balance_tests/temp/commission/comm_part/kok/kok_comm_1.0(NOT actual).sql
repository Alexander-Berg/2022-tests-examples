with
--
-- основная выборка для базовой комиссии за 13/14 фин. год
--
s_base_src as (
    select distinct
           contract_eid                             as contract_eid,
           contract_id                              as contract_id,
           invoice_eid                              as invoice_eid,
           invoice_id                               as invoice_id,
           currency                                 as currency,
           nds                                      as nds, 
           nds_pct                                  as nds_pct,
           nvl(commission_type, discount_type)      as discount_type,
           payment_type                             as payment_type, 
           commission_payback_pct                   as commission_payback_type,
           contract_commission_type                 as commission_type,
           endbuyer_id                              as endbuyer_id               
      from xxxx_new_comm_contract_basic                   
      where discount_type in (1, 2, 3, 7, 11, 12, 14)
),
--
-- основная выборка по актам
--
s_base_acts as (
  select * from (
    select b.contract_eid,
           b.contract_id,
           b.currency,
           b.nds,
           b.payment_type,
           b.commission_payback_type                as payback_type,
           trunc(xxx.act_dt, 'MM')                      as from_dt,
           add_months(
               trunc(xxx.act_dt, 'MM'), 1) - 1/84600    as till_dt,
           xxx.amount                                   as amt_w_nds, 
           xxx.amount-xxx.amount_nds-xxx.amount_nsp     as amt, 
           b.commission_type                            as commission_type,    
           b.endbuyer_id                                as endbuyer_id,
           xxx.act_dt                                   as act_dt
      from s_base_src                       b
      join xxxx_new_comm_contract_basic     xxx on b.invoice_id = xxx.invoice_id
      and xxx.hidden <4
  ) b
  where b.commission_type = 49 
        -- BALANCE-19159
        -- начиная с 01.01.2015 исключаем акты, по счетам которых
        -- не указан конечный получатель
       and (
            -- начиная с 2015, исключаем оптовую часть
            -- и оставляем только те счета, в которых есть конечный покупатель
            (
                b.act_dt >= date'2015-01-01'
            and b.endbuyer_id is not null
            )
            or
            -- до 2015 года ничего не делаем
            (
                b.act_dt < date'2015-01-01'
            )
           )
),
--
-- основная выборка по оплатам
--
s_payments as (
    select b.contract_eid,
           b.contract_id,
           b.currency,
           b.nds,
           b.payment_type,
           b.commission_payback_type                as payback_type,
           -- BALANCE-15155
           trunc(oebs.comiss_date, 'MM')            as from_dt,
           add_months(
            trunc(oebs.comiss_date, 'MM'), 1) - 1/84600 as till_dt,
           oebs.oebs_payment                                   as amt_w_nds,
           oebs.oebs_payment*100/(100 + b.nds*b.nds_pct)       as amt           
      from s_base_src                       b
      join xxxx_oebs_cash_payment_test     oebs on oebs.invoice_id = b.invoice_id
                                                 -- BALANCE-14988
                                                and oebs.comiss_date >= date'2013-03-01'
                                                 -- BALANCE-15155
                                                and oebs.comiss_date is not null
      where b.commission_type = 49
        -- BALANCE-19159
        -- начиная с 01.01.2015 исключаем акты, по счетам которых
        -- не указан конечный получатель
       and (
            -- начиная с 2015, исключаем оптовую часть
            -- и оставляем только те счета, в которых есть конечный покупатель
            (
                oebs.comiss_date >= date'2015-01-01'
            and b.endbuyer_id is not null
            )
            or
            -- до 2015 года ничего не делаем
            (
                oebs.comiss_date < date'2015-01-01'
            )
           )
),
--
-- основная выборка по оплатам/актам
-- (чтобы договор выводился даже тогда,
-- когда актов нет, но есть оплаты)
--
s_base as (
    select contract_eid, contract_id, currency,
           nds, from_dt, till_dt,
           sum(amt_acts) as amt_to_charge,
           sum(amt_oebs) as amt_oebs,
           sum(decode(payment_type,
                -- предоплата: платим по начисленному (актам)
                2, amt_acts_w_nds,
                -- постоплата: платим или по деньгам или
                -- начисленному (от условий в договоре)
                3, decode(payback_type,
                        1, amt_acts_w_nds,
                        2, amt_oebs_w_nds,
                        0),
                0))           as amt_to_pay_w_nds,
           sum(decode(payment_type,
                -- предоплата: платим по начисленному (актам)
                2, amt_acts,
                -- постоплата: платим или по деньгам или
                -- начисленному (от условий в договоре)
                3, decode(payback_type,
                        1, amt_acts,
                        2, amt_oebs,
                        0),
                0))           as amt_to_pay
      from (
            select contract_eid, contract_id, currency, nds,
                   payment_type, payback_type, from_dt, till_dt,
                   amt  as amt_acts, amt_w_nds  as amt_acts_w_nds,
                   0    as amt_oebs, 0          as amt_oebs_w_nds
              from s_base_acts
             union all
            select contract_eid, contract_id, currency, nds,
                   payment_type, payback_type, from_dt, till_dt,
                   0    as amt_acts, 0          as amt_acts_w_nds,
                   amt  as amt_oebs, amt_w_nds  as amt_oebs_w_nds
              from s_payments
           )
     group by contract_eid, contract_id, currency,
              nds, from_dt, till_dt
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
       null                 as discount_type,
       1                    as reward_type,
       amt_to_charge*.011   as reward_to_charge,        -- к начислению
       amt_to_pay*.011      as reward_to_pay,           -- к перечислению
       amt_to_charge*.01    as delkredere_to_charge,    -- к начислению (делькредере)
       amt_to_pay*.01       as delkredere_to_pay,       -- к перечислению (делькредере)
       null                 as dkv_to_charge,
       null                 as dkv_to_pay,
       amt_to_charge        as turnover_to_charge,      -- оборот к начислению
       amt_to_pay_w_nds     as turnover_to_pay_w_nds,   -- оборот к перечислению (с НДС)
       amt_to_pay           as turnover_to_pay          -- оборот к перечислению
  from s_base
order by contract_id desc, from_dt;
65