
  CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."V_OPT_2015_MARKET" ("CONTRACT_ID", "CONTRACT_EID", "FROM_DT", "TILL_DT", "NDS", "CURRENCY", "DISCOUNT_TYPE", "REWARD_TYPE", "TURNOVER_TO_CHARGE", "REWARD_TO_CHARGE", "TURNOVER_TO_PAY_W_NDS", "TURNOVER_TO_PAY", "REWARD_TO_PAY_SRC", "REWARD_TO_PAY") AS 
  with
-- ----------------------------------------------------------------------------
-- основная выборка
-- ----------------------------------------------------------------------------
-- все акты по счетам
s_acts as (
    select b.*
      from bo.v_opt_2015_acts   b
     where b.commission_type in (12, 13)
        -- BALANCE-22203
       and b.currency = 'RUR'
        -- BALANCE-29283: новые акты тут не смотрим (смотрим в python)
       and b.act_dt < date'2018-10-01'
),
s_payments as (
    select b.*
      from bo.v_opt_2015_payments b
     where b.commission_type in (12, 13)
        -- BALANCE-22203
       and b.currency = 'RUR'
        -- BALANCE-28692: начиная с 2018-09-01 считаем по новому и в питоне.
        --                оплаты только по старым счетам смотрим
       and b.invoice_dt < date'2018-09-01'
),
-- ----------------------------------------------------------------------------
-- Базовое КВ (месячное)
-- ----------------------------------------------------------------------------
-- Складываем акты и оплаты
s_kv_src as (
    select contract_eid, contract_id,
           currency, discount_type,
           commission_type,
           nds, from_dt, till_dt, payment_type,
           count(distinct case when invoice_dt >= date'2018-09-01'
                               then invoice_id
                               else null
                          end)      as invoice_cnt,
           count(distinct
                case when invoice_type = 'prepayment'
                      and invoice_dt >= date'2018-09-01'
                     then invoice_id
                     else null
                end)                as invoice_prep_cnt,
           sum(amt_acts)            as amt_to_charge,
           sum(amt_oebs_w_nds)      as amt_to_pay_w_nds,
           sum(amt_oebs)            as amt_to_pay
      from (
            select contract_eid, contract_id, currency, nds, discount_type,
                   commission_type, invoice_type, invoice_id,
                   from_dt, till_dt, payment_type, invoice_dt,
                   amt  as amt_acts, amt_w_nds  as amt_acts_w_nds,
                   0    as amt_oebs, 0          as amt_oebs_w_nds
              from s_acts
             where act_dt between add_months(trunc(sysdate, 'MM'), -1)
                              and trunc(sysdate, 'MM') - 1/24/60/60
             union all
            select contract_eid, contract_id, currency, nds, discount_type,
                   commission_type, invoice_type, invoice_id,
                   from_dt, till_dt, payment_type, invoice_dt,
                   0    as amt_acts, 0          as amt_acts_w_nds,
                   amt  as amt_oebs, amt_w_nds  as amt_oebs_w_nds
              from s_payments
             where doc_date between add_months(trunc(sysdate, 'MM'), -1)
                                and trunc(sysdate, 'MM') - 1/24/60/60
           )
     group by contract_eid, contract_id, currency, payment_type,
              commission_type,
              nds, discount_type, from_dt, till_dt
),
-- Подготовка к контролю (заранее считаем бюджеообразующих клиентов)
s_kv_control_src as (
    select d.*,
           -- Соотношение оборота по Директу по клиенту к
           -- обороту по договору (Агентства)
           nvl(ratio_to_report(amt_rub)
              over (partition by contract_id, from_dt), 0) as ratio
      from (
        select contract_id, from_dt, till_dt, client_id,
               commission_type, contract_till_dt,
               nds_count, currency_count,
               sum(amt_rub)                                   as amt_rub
          from s_acts
         group by contract_id, from_dt, till_dt, client_id,
                  commission_type, contract_till_dt,
                  nds_count, currency_count
           ) d
),
s_kv_control_pre as (
    select d.*,
           -- учитываем, что в каком-то месяце может не быть оборота
           case
            when add_months(from_dt, -1) = from_dt_1m_ago
            then case when ratio_1m_ago >= 0.7 then 1 else 0 end
            else 0
           end                                          as is_there_boc_1m_ago
      from (
        select d.*,
               lag(from_dt, 1) over (partition by contract_id
                                         order by from_dt)   as from_dt_1m_ago,
               lag(max_client_ratio_by_direct, 1, 0)
                               over (partition by contract_id
                                         order by from_dt)   as ratio_1m_ago,
               case when max_client_ratio_by_direct >= 0.7 then 1 else 0
                end as is_there_boc
          from (
            select contract_id, from_dt, till_dt,
                   commission_type, contract_till_dt,
                   nds_count, currency_count,
                   sum(amt_rub)                 as amt_rub,
                   count(distinct client_id)    as client_count,
                   round(max(ratio), 2)         as max_client_ratio_by_direct
              from s_kv_control_src
             group by contract_id, from_dt, till_dt,
                      commission_type, contract_till_dt,
                      nds_count, currency_count
               ) d
           ) d
),
-- Штрафы
s_kv_control as (
    select d.*,
           case
            -- BALANCE-24639: не прологированные договор, старые условия
            when contract_till_dt < date'2017-03-01' then
                case
                when amt_rub >= decode(commission_type,
                                 12, 25000,
                                 13, 16000)
                 and client_count >= 3
                 and (
                         -- не наказываем, если в одном из
                         -- двух подряд идущих месяцев нет проблем с БОК
                         is_there_boc = 0 or
                         is_there_boc_1m_ago = 0
                     )
                 and nds_count = 1
                 and currency_count = 1
                then 0
                else 1
                end
            -- новые условия
            else
                case
                when amt_rub >= decode(commission_type,
                				12, 200000,
                				13, 100000)
                 and client_count >= 3
                 and nds_count = 1
                 and currency_count = 1
                then 0
                else 1
                end
            end as failed
      from s_kv_control_pre d
),
-- Сумма КВ без контроля акты <= оплат
s_kv_pre as (
    select d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           d.commission_type,
           d.nds, d.currency,
           d.discount_type,
           d.payment_type,
           sum(d.amt_to_charge)         as turnover_to_charge,
           sum(
           case
                when d.from_dt >= date'2018-09-01' then
                    decode(f.failed,
                        0, bo.pk_comm.calc_market_m(d.amt_to_charge, d.commission_type),
                        0)
                else
                d.amt_to_charge*
                decode(f.failed,
                    0, 0.08,
                    0)
               end)                     as reward_to_charge,
           sum(d.amt_to_pay)            as turnover_to_pay,
           sum(d.amt_to_pay_w_nds)      as turnover_to_pay_w_nds,
           -- BALANCE-19851
           -- Штрафовать по оплатам не должны
           -- BALANCE-22195: применяем старые и новые условия раздельно
           sum(
            case
                when d.from_dt >= date'2018-09-01'
                 and d.invoice_cnt = d.invoice_prep_cnt
                then
                    decode(f.failed,
                        0, bo.pk_comm.calc_market_m(d.amt_to_charge, d.commission_type),
                        0)
                else 0
                end
             +  d.amt_to_pay*0.08
            )                    as reward_to_pay
      from s_kv_src         d
      -- BALANCE-19851
      left outer
      join s_kv_control     f on f.contract_id = d.contract_id
                             and f.from_dt = d.from_dt
                             and f.till_dt = d.till_dt
     group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt, d.commission_type,
              d.payment_type,
              d.discount_type, d.currency, d.nds
),
-- КВ с разбивкой по типам рекламы, без контроля, что оплат не более, чем актов
s_kv01 as (
    select contract_eid, contract_id, from_dt, till_dt,
           discount_type, currency, nds,
           -- к перечислению
           turnover_to_charge,
           reward_to_charge_orig                as reward_to_charge,
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
                select d.contract_id, contract_eid, d.discount_type,
                       d.from_dt, d.till_dt,
                       d.nds, d.currency,
                       d.turnover_to_charge,
                       d.reward_to_charge               as reward_to_charge_orig,
                       --BALANCE-29450: не даем покрывать новые акты, которые
                       -- будут учтены в новом контроле оборотов, старыми оплатами
                       case
                        when payment_type = 3 and from_dt >= date'2018-09-01'
                        then 0
                        else d.reward_to_charge
                       end                              as reward_to_charge,
                       d.turnover_to_pay,
                       d.turnover_to_pay_w_nds,
                       d.reward_to_pay
                  from s_kv_pre         d
                 union all
                    -- BALANCE-24627
                    -- История, чтобы раздать "долги", если таковые остались
                    -- с прошлых месяцев
                    -- BALANCE-25301: учитываем оплаты с предоплатных периодов
                select d.contract_id, contract_eid, d.discount_type,
                       d.from_dt, d.till_dt,
                       d.nds, d.currency,
                       sum(d.turnover_to_charge)    as turnover_to_charge_orig,
                       sum(d.turnover_to_charge)    as turnover_to_charge,
                       sum(d.reward_to_charge)      as reward_to_charge,
                       sum(d.turnover_to_pay)       as turnover_to_pay,
                       sum(d.turnover_to_pay_w_nds) as turnover_to_pay_w_nds,
                       sum(d.reward_to_pay_src)     as reward_to_pay
                  from bo.mv_ar_rewards_f       d
                 where d.contract_id in (
                            select contract_id from s_kv_pre
                       )
                    -- BALANCE-24877: исключаем расчеты за тек.период, если это
                    --                не первый расчет за расчет.период
                   and d.from_dt < add_months(trunc(sysdate, 'MM'), -1)
                   and d.reward_type in (301)
              group by d.contract_id, contract_eid,
                       d.from_dt, d.till_dt,
                       d.discount_type,
                       d.nds, d.currency
                   ) d
           ) s
     where from_dt between add_months(trunc(sysdate, 'MM'), -1)
                       and trunc(sysdate, 'MM') - 1/24/60/60
)
-- СКВ, Полугодовой
-- BALANCE-22704, BALANCE-22705
, s_skv as (
    select d.*,
           case
            when nds_count = 1 and currency_count = 1
            then bo.pk_comm.calc_market_skv(d.amt_rub, d.from_dt)
            else 0
           end                                  as reward
      from (
        select d.contract_eid, d.contract_id, d.from_dt, d.till_dt, d.nds,
               d.nds_count, d.currency_count,
               -- BALANCE-15641
               sum(amt_w_nds_rub)               as amt_w_nds_rub,
               sum(amt_rub)                     as amt_rub
          from bo.v_opt_2015_market_skv d
         where failed = 0
           and excluded = 0
         group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt, d.nds,
                  d.nds_count, d.currency_count
           ) d
)
-- BALANCE-22696
-- BALANCE-22699
, s_q as (
    select d.*,
           bo.pk_comm.calc_market_q(
                amt_rub,
                amt_ag,
                amt_ag_prev,
                amt_ag_prev_fm
           )                                as reward
      from (
        select d.contract_eid, d.contract_id, d.from_dt, d.till_dt, d.agency_id,
               sum(amt)                     as amt_rub,
               sum(d.amt_w_nds_rub)         as amt_w_nds_rub,
               amt_ag_q                     as amt_ag,
               amt_ag_prev,
               amt_ag_prev_fm
          from bo.v_opt_2015_market_q d
         where failed = 0
           and excluded = 0
         group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt, d.agency_id,
                  amt_ag_prev, amt_ag_prev_fm, amt_ag_q
           ) d
)
-- результирующий запрос
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
       reward_to_pay_src,                           -- к перечислению
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
               reward_to_pay_src,
               -- BALANCE-22359: для мск+питер и регион всегда показываем 1
               -- (как в базовых)
               1                    as reward_type
          from s_kv01
         union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               null                             as discount_type,
               'RUR'                            as currency,
               1                                as nds,
               amt_rub                          as turnover_to_charge,
               reward                           as reward_to_charge,
               amt_rub                          as turnover_to_pay,
               amt_w_nds_rub                    as turnover_to_pay_w_nds,
               reward                           as reward_to_pay,
               reward                           as reward_to_pay_src,
               20                               as type
          from s_q
         union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               null                             as discount_type,
               'RUR'                            as currency,
               nds,
               amt_rub                          as turnover_to_charge,
               reward                           as reward_to_charge,
               amt_rub                          as turnover_to_pay,
               amt_w_nds_rub                    as turnover_to_pay_w_nds,
               reward                           as reward_to_pay,
               reward                           as reward_to_pay_src,
               2                                as type
          from s_skv
       );
