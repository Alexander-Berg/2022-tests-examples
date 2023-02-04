
  CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."V_OPT_2017_ESTATE_MSK" ("CONTRACT_ID", "CONTRACT_EID", "FROM_DT", "TILL_DT", "NDS", "CURRENCY", "DISCOUNT_TYPE", "REWARD_TYPE", "TURNOVER_TO_CHARGE", "REWARD_TO_CHARGE", "TURNOVER_TO_PAY_W_NDS", "TURNOVER_TO_PAY", "REWARD_TO_PAY_SRC", "REWARD_TO_PAY") AS 
  with
-- История действия подписанных атрибутов
s_attrs_src as (
    select value_num,
           contract2_id                                         as contract_id,
           code,
           cl_dt                                                as from_dt,
           nvl(
                lead(cl_dt) over(partition by code, contract2_id
                                     order by stamp),
                add_months(trunc(sysdate, 'MM'), 11)
           ) - 1/24/60/60                                       as till_dt
      from bo.mv_contract_signed_attr_hist
),
s_changes_payment_type as (
    select *
      from (
            select s.contract_id, s.from_dt, s.till_dt, s.value_num
              from s_attrs_src s
             where s.code in ('PAYMENT_TYPE')
               and exists (select 1 from s_attrs_src d
                            where d.code in ('WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE')
                              and d.contract_id = s.contract_id)
          )
),
s_acts as (
    select b.*
      from bo.v_opt_2015_acts   b
     where b.commission_type = 16
        -- BALANCE-29611: новые акты тут не смотрим (смотрим в python)
       and b.act_dt < date'2019-01-01'
),
-- ----------------------------------------------------------------------------
-- основная выборка по оплатам
-- ----------------------------------------------------------------------------
s_payments as (
    select b.*
      from bo.v_opt_2015_payments b
     where b.commission_type = 16
        -- BALANCE-29611: начиная с 2019-01-01 считаем по новому и в питоне.
        --                в старом расчете оплаты только по старым счетам смотрим
       and b.invoice_dt < date'2019-01-01'
),
-- ----------------------------------------------------------------------------
-- Базовое КВ (месячное)
-- ----------------------------------------------------------------------------
-- Акты и оплаты за прошлый месяц
s_kv_src as (
    select contract_eid, contract_id,
           currency, nds, payment_type,
           invoice_type, invoice_dt,
           contract_from_dt, contract_till_dt,
           from_dt, till_dt,
           sum(amt_acts)            as amt_to_charge,
           sum(amt_oebs_w_nds)      as amt_to_pay_w_nds,
           sum(amt_oebs)            as amt_to_pay
      from (
            select contract_eid, contract_id, currency, nds, discount_type,
                   from_dt, till_dt, payment_type,
                   contract_from_dt, contract_till_dt,
                   invoice_type, invoice_dt,
                   amt  as amt_acts, amt_w_nds  as amt_acts_w_nds,
                   0    as amt_oebs, 0          as amt_oebs_w_nds
              from s_acts
             where act_dt between add_months(trunc(sysdate, 'MM'), -1)
                              and trunc(sysdate, 'MM') - 1/24/60/60
             union all
            select contract_eid, contract_id, currency, nds, discount_type,
                   from_dt, till_dt, payment_type,
                   contract_from_dt, contract_till_dt,
                   invoice_type, invoice_dt,
                   0    as amt_acts, 0          as amt_acts_w_nds,
                   amt  as amt_oebs, amt_w_nds  as amt_oebs_w_nds
              from s_payments
             where doc_date between add_months(trunc(sysdate, 'MM'), -1)
                                and trunc(sysdate, 'MM') - 1/24/60/60
           )
     group by contract_eid, contract_id, currency,
              invoice_type, invoice_dt,
              contract_from_dt, contract_till_dt,
              nds, from_dt, till_dt, payment_type
),
s_kv_control as (
    select d.*,
           case
           when amt_rub >= 50000
            and nds_count = 1
            and currency_count = 1
            and client_count >= 3
           then 0
           else 1
            end as failed
      from (
        select d.contract_id, d.from_dt, d.till_dt,
               nds_count, currency_count,
               count(distinct client_id)                    as client_count,
               sum(amt_rub)                                 as amt_rub
          from s_acts   d
         group by d.contract_id, d.from_dt, d.till_dt,
                  d.nds_count, d.currency_count
           ) d
),
s_pcts as (
    select date'2000-01-01' as from_dt, date'2018-01-01'-1/86400 as till_dt, 0.25 as pct
      from dual
     union all
    select date'2018-01-01' as from_dt, date'2020-01-01'-1/86400 as till_dt, 0.18 as pct
      from dual
),
s_kv_pre as (
    select d.contract_id, contract_eid,
           d.from_dt, d.till_dt, d.payment_type,
           d.nds, d.currency,
           sum(d.amt_to_charge)         as turnover_to_charge,
           sum(decode(f.failed,
                0, d.amt_to_charge*pa.pct,
                0))                     as reward_to_charge,
           sum(d.amt_to_pay)            as turnover_to_pay,
           sum(d.amt_to_pay_w_nds)      as turnover_to_pay_w_nds,
           -- BALANCE-19851
           -- Штрафовать по оплатам не должны
           sum(
            case
              -- BALANCE-26669:
              -- предоплата, новый счет, договор продлен - платим от актов по новым условиям
            when d.invoice_type = 'prepayment'
             and d.invoice_dt >= date'2018-01-01'
             and d.contract_till_dt > date'2018-01-01'
             and d.from_dt >= date'2018-01-01'
            then d.amt_to_charge*pa.pct
              -- Постоплата, новый счет, договор продлен - платим от денег по новым условиям
            when d.invoice_type <> 'prepayment'
             and d.invoice_dt >= date'2018-01-01'
             and d.contract_till_dt > date'2018-01-01'
             and d.from_dt >= date'2018-01-01'
            then d.amt_to_pay*pa.pct
              -- все остальные случаи - платим от денег по старым условиям
              --    - договор не проден, либо
              --    - старый счет, либо
              --    - отчетный период: < 2018-01-01
              -- Важно: по старым предоплатным счетам не должны учитывать акты
              -- вместо оплат, т.к. эти оплаты уже есть в истории в предыдущих
              -- периодах.
            else d.amt_to_pay*pp.pct
            end)                        as reward_to_pay
      from s_kv_src         d
        -- BALANCE-26669
        -- проценты для актов
      join s_pcts           pa on d.from_dt between pa.from_dt and pa.till_dt
       -- проценты для оплат (смотрим на дату счета)
      join s_pcts           pp on d.invoice_dt between pp.from_dt and pp.till_dt
      left outer
      join s_kv_control     f on f.contract_id = d.contract_id
                             and f.from_dt = d.from_dt
                             and f.till_dt = d.till_dt
     group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt,
              d.currency, d.nds, d.payment_type
),
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
                reward_to_pay)                  as reward_to_pay_src,
           decode(payment_type,
                2, reward_to_charge,
                (least(reward_to_charge_sum, reward_to_pay_sum) -
                    least(reward_to_charge_sum_prev, reward_to_pay_sum_prev)
                ))                              as reward_to_pay
      from (
            select d.*,
                   (select l.payment_type from s_kv_pre l
                     where l.contract_id = d.contract_id
                   )                                    as payment_type,
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
              from (
                select d.contract_id, contract_eid,
                       d.from_dt, d.till_dt,
                       d.nds, d.currency,
                       d.turnover_to_charge,
                       d.reward_to_charge,
                       d.turnover_to_pay,
                       d.turnover_to_pay_w_nds,
                       d.reward_to_pay
                  from s_kv_pre         d
                 union all
                    -- BALANCE-24627
                    -- История, чтобы раздать "долги", если таковые остались
                    -- с прошлых месяцев
                    -- BALANCE-25301: учитываем оплаты с предоплатных периодов
                select d.contract_id, contract_eid,
                       d.from_dt, d.till_dt,
                       d.nds, d.currency,
                       sum(d.turnover_to_charge)    as turnover_to_charge,
                       sum(d.reward_to_charge)      as reward_to_charge,
                       sum(d.turnover_to_pay)       as turnover_to_pay,
                       sum(d.turnover_to_pay_w_nds) as turnover_to_pay_w_nds,
                       sum(case
                              -- если в это время был предоплатником, то платим от актов
                         when nvl(chpt.value_num, 3) = 2
                           -- Пример: contract_id = 239691, 2016-10. Станет постоплатой
                           -- только в след. месяце, но уже в 2016-10 есть 310 строка,
                           -- из которой надо достать реальную оплату, а не акты
                          and d.reward_type = 301
                              -- заполняется у предоплаты
                         then d.reward_to_pay
                         else d.reward_to_pay_src
                          end)                      as  reward_to_pay
                  from bo.v_ar_rewards        d
                  left outer
                  join s_changes_payment_type chpt on chpt.contract_id = d.contract_id
                                                  and d.till_dt between chpt.from_dt
                                                                    and chpt.till_dt
                 where d.contract_id in (
                            select contract_id from s_kv_pre
                       )
                    -- BALANCE-24877: исключаем расчеты за тек.период, если это
                    --                не первый расчет за расчет.период
                   and d.from_dt < add_months(trunc(sysdate, 'MM'), -1)
                   and d.reward_type in (310, 410, 510, 301, 401, 501)
              group by d.contract_id, contract_eid,
                       d.from_dt, d.till_dt,
                       d.nds, d.currency
                   ) d
           ) s
)
-- результирующий запрос
select contract_id,
       contract_eid,
       from_dt,
       till_dt,
       nds,
       currency,
       97                   as discount_type,
       1                    as reward_type,
       turnover_to_charge,                          -- оборот к начислению
       reward_to_charge,                            -- к начислению
       turnover_to_pay_w_nds,
       turnover_to_pay,                             -- оборот к перечислению
       reward_to_pay_src,                           -- к перечислению
       reward_to_pay                                -- к перечислению
  from s_kv;
