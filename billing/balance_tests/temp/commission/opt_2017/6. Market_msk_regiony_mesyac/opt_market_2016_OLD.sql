with
-- опорные даты
s_dates as (
        -- начало фин. года
    select date'2016-03-01'   as fin_year_dt
      from dual
),

s_base as (
select distinct
           c.contract_eid                             as contract_eid,
               case
        when c.contract_commission_type in (4,10)
         and c.firm_id  = 1
        then
        nvl(
        (select distinct l.value_num
                from xxxx_new_comm_contract_basic l
               where l.contract_id = c.contract_id
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
		       29  as discount_type_src,
           c.payment_type                             as payment_type, 
                                              -- ?  as commission_payback_type
           c.commission_payback_pct                   as commission_payback_pct,
           c.contract_commission_type                 as commission_type
  from xxxx_new_comm_contract_basic c
  where (
                                                -- BALANCE-17175
                                                (
                                                    -- только счета, созданные в 2015
--                                                    invoice_dt >= date'2015-03-01' and
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
--select * from s_base order by  invoice_id ;
,

-- ----------------------------------------------------------------------------
-- основная выборка по актам
-- ----------------------------------------------------------------------------
-- все акты по счетам
s_acts_src as (
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
           end                               as is_loyal,
           xxx.client_id                                      as client_id,
           xxx.act_dt                                         as act_dt,
           trunc(xxx.act_dt, 'MM')                            as from_dt,
           add_months(trunc(xxx.act_dt, 'MM'), 1) - 1/84600   as till_dt,
            count(distinct b.nds)      over (partition by b.contract_id) as nds_count,
           count(distinct b.currency) over (partition by b.contract_id) as currency_count,
           xxx.amount                                         as amt_w_nds,
           xxx.amount-xxx.amount_nds-xxx.amount_nsp           as amt,
           xxx.amount*cr.rate                                 as amt_w_nds_rub,
           (xxx.amount-xxx.amount_nds-xxx.amount_nsp)*cr.rate as amt_rub
      from s_base        b
      join xxxx_new_comm_contract_basic     xxx on b.invoice_id = xxx.invoice_id
                                              and xxx.hidden <4
                                              and xxx.act_dt >= (select fin_year_dt from s_dates)
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
               --  * включаем Авто (25)
            xxx.act_dt >= date'2016-03-01' and b.contract_till_dt >= date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 12, 25)
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
       or (b.commission_type in (12, 13) and b.discount_type = 11)
       )
)
--select * from s_acts_src order by invoice_id;
,
-- акты без лояльных клиентов
-- все акты по счетам
s_acts as (
    select b.*
      from s_acts_src   b
     where b.commission_type in (12, 13)
     -- BALANCE-22203
     and b.currency = 'RUR'
)
--select * from s_acts ;
,
-- ----------------------------------------------------------------------------
-- Базовое КВ (месячное)
-- ----------------------------------------------------------------------------
-- Складываем акты и оплаты
s_kv_src as (
    select contract_eid, contract_id,
           currency, discount_type,
           commission_type,
           nds, from_dt, till_dt, payment_type,
           sum(amt_acts)            as amt_to_charge,
           sum(amt_oebs_w_nds)      as amt_to_pay_w_nds,
           sum(amt_oebs)            as amt_to_pay
      from (
            select contract_eid, contract_id, currency, nds, discount_type,
                   commission_type,
                   from_dt, till_dt, payment_type, invoice_dt,
                   amt  as amt_acts, amt_w_nds  as amt_acts_w_nds,
                   amt  as amt_oebs, amt_w_nds  as amt_oebs_w_nds
              from s_acts
           )
     group by contract_eid, contract_id, currency, payment_type,
              commission_type,
              nds, discount_type, from_dt, till_dt
)
--select  * from  s_kv_src;
,
-- Подготовка к контролю (заранее считаем бюджеообразующих клиентов)
s_kv_control_src as (
    select d.*,
           -- Соотношение оборота по Директу по клиенту к
           -- обороту по договору (Агентства)
           nvl(ratio_to_report(amt_rub)
              over (partition by contract_id, from_dt), 0) as ratio
      from (
        select contract_id, from_dt, till_dt, client_id,
               commission_type,
               nds_count, currency_count,
               sum(amt_rub)                                   as amt_rub
          from s_acts
         group by contract_id, from_dt, till_dt, client_id,
                  commission_type,
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
                   commission_type,
                   nds_count, currency_count,
                   sum(amt_rub)                 as amt_rub,
                   count(distinct client_id)    as client_count,
                   round(max(ratio), 2)         as max_client_ratio_by_direct
              from s_kv_control_src
             group by contract_id, from_dt, till_dt,
                      commission_type,
                      nds_count, currency_count
               ) d
           ) d
),
-- Штрафы
s_kv_control as (
    select d.*,
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
           sum(d.amt_to_charge)         as turnover_to_charge,
           sum(d.amt_to_charge*
                decode(f.failed,
                    0, 0.08,
                    0))                 as reward_to_charge,
           sum(d.amt_to_pay)            as turnover_to_pay,
           sum(d.amt_to_pay_w_nds)      as turnover_to_pay_w_nds,
           -- BALANCE-19851
           -- Штрафовать по оплатам не должны
           -- BALANCE-22195: применяем старые и новые условия раздельно
           sum(d.amt_to_pay*
                decode(f.failed,
                    0, 0.08,
                    0))                 as reward_to_pay
      from s_kv_src         d
      -- BALANCE-19851
      left outer
      join s_kv_control     f on f.contract_id = d.contract_id
                             and f.from_dt = d.from_dt
                             and f.till_dt = d.till_dt
     group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt, d.commission_type,
              d.discount_type, d.currency, d.nds
)
--select * from s_kv_pre;
,
-- КВ с разбивкой по типам рекламы, без контроля, что оплат не более, чем актов
s_kv01 as (
    select contract_eid, contract_id, from_dt, till_dt,
           discount_type, currency, nds,
           commission_type,
           -- к перечислению
           turnover_to_charge,
           reward_to_charge,
           turnover_to_pay,
           turnover_to_pay_w_nds,
           reward_to_pay
      from s_kv_pre
)
--select * from s_kv01;

-- результирующий запрос
select distinct s."CONTRACT_ID",s."CONTRACT_EID",s."FROM_DT",s."TILL_DT",s."NDS",s."CURRENCY",s."DISCOUNT_TYPE",s."REWARD_TYPE",s."TURNOVER_TO_CHARGE",s."REWARD_TO_CHARGE",s."DELKREDERE_TO_CHARGE",s."DKV_TO_CHARGE",s."TURNOVER_TO_PAY",s."TURNOVER_TO_PAY_W_NDS",s."REWARD_TO_PAY",s."DELKREDERE_TO_PAY",s."DKV_TO_PAY"
  from (
    select contract_id,     contract_eid,
           from_dt,         till_dt,
           nds,             currency,
           discount_type,
           300 + reward_type            as reward_type,
           -- к начислению
           sum(turnover_to_charge)      as turnover_to_charge,
           sum(reward_to_charge)        as reward_to_charge,
           0                            as delkredere_to_charge,
           0                            as dkv_to_charge,
           -- к перечислению
           sum(turnover_to_pay)         as turnover_to_pay,
           sum(turnover_to_pay_w_nds)   as turnover_to_pay_w_nds,
           sum(reward_to_pay)           as reward_to_pay,
           0                            as delkredere_to_pay,
           0                            as dkv_to_pay


  from (
  select contract_id,     contract_eid,
                   from_dt,         till_dt,
                   nds,             currency,
                   discount_type,
                   reward_type,
                   turnover_to_charge,
                   reward_to_charge,
                   turnover_to_pay_w_nds,
                   turnover_to_pay,
                   reward_to_pay
    from(
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
               decode(commission_type,
                    12, 1,
                    13, 3)  as reward_type
          from s_kv01
       )))
       group by contract_id,     contract_eid,
              from_dt,         till_dt,
              nds,             currency,
              discount_type,   reward_type
)            s
  order by contract_id, from_dt, discount_type, currency, nds;
