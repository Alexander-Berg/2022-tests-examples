with
-- опорные даты
s_dates as (
        -- начало фин. года
    select date'2015-03-01'   as fin_year_dt
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
                                                       commission_payback_pct                   as commission_payback_pct,
           contract_commission_type                 as commission_type
  from xxxx_new_comm_contract_basic
  where (
                                                -- BALANCE-17175
                              
                  (
                                                  
                                                    -- только базовые/профы
                                                    contract_commission_type in (1, 2, 8) and
                                                    -- то учитываем еще и код 22
                                                    nvl(commission_type,
                                                        discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 17, 19, 22, 28)
                                                )
                                                or
                                                (
                                                    -- иначе, как ранее
                                                    nvl(commission_type,
                                                        discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 17, 19, 28)
                                                )
                                              )
),
-- ----------------------------------------------------------------------------
-- основная выборка по актам
-- ----------------------------------------------------------------------------
-- все акты по счетам
s_acts_all as (
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
           trunc(xxx.act_dt, 'MM')                            as from_dt,
           add_months(trunc(xxx.act_dt, 'MM'), 1) - 1/84600   as till_dt,
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
     where b.commission_type = 2
        -- Для сервисов: Директ, Медийка, Маркет, Справочник
       and b.discount_type in (1, 2, 3, 7, 11, 12)
)

--select * from s_acts_all order by contract_id ;,
,
-- акты без лояльных клиентов
s_acts_wo_lc as (
    select d.*
      from s_acts_all d
     where not (d.loyal_clients = 1 and d.discount_type = 7)
),
-- актны по лояльным клиентам
s_acts_lc as (
    select d.*
      from s_acts_all d
     where (d.loyal_clients = 1 and d.discount_type = 7)
),
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
           trunc(oebs.comiss_date, 'MM')                 as from_dt,
           add_months(
            trunc(oebs.comiss_date, 'MM'), 1)-1/84600    as till_dt,
           oebs.oebs_payment*100/(100 + b.nds*b.nds_pct) as amt,
           oebs.oebs_payment                             as amt_w_nds
      from s_base        b
      join xxxx_oebs_cash_payment_test     oebs on oebs.invoice_id = b.invoice_id
                                                and oebs.comiss_date >= (select fin_year_dt from s_dates)
                                                and oebs.comiss_date is not null
     where b.commission_type = 2
        -- Для сервисов: Директ, Медийка, Маркет, Справочник
       and b.discount_type in (1, 2, 3, 7, 11, 12)
),
-- ----------------------------------------------------------------------------
-- Базовое КВ (месячное)
-- ----------------------------------------------------------------------------
-- Акты по ЛК. Отбрасываем клиентов, у которых за месяц < 50к
s_kv_acts_lc as (
    select contract_eid, contract_id,
           currency, discount_type,
           nds, from_dt, till_dt, payment_type,
           client_id,
           sum(amt_w_nds)       as amt_w_nds,
           sum(amt)             as amt
      from s_acts_lc
     group by contract_eid, contract_id,
              currency, discount_type,
              nds, from_dt, till_dt, payment_type,
              client_id
        -- BALANCE-19689
        -- Для ЛК базовая премия на услуги по Директу предоставляется только,
        -- если в данном месяце услуг по Директу было оказано (по актам)
        -- на 50 000 т.р. (без НДС) и более
     having sum(amt_rub) >= 50000
),
s_kv_lc as (
    select contract_eid, contract_id,
           currency, discount_type,
           nds, from_dt, till_dt, payment_type,
           sum(amt_w_nds)       as amt_w_nds,
           sum(amt)             as amt,
           sum(amt)*.1          as reward
      from s_kv_acts_lc
     group by contract_eid, contract_id,
              currency, discount_type,
              nds, from_dt, till_dt, payment_type
),
-- Акты без ЛК)
s_kv_acts as (
    select contract_eid, contract_id, currency, nds, discount_type,
           from_dt, till_dt, payment_type,
           amt, amt_w_nds
      from s_acts_wo_lc
),
-- Складываем акты и оплаты
s_kv_src as (
    select contract_eid, contract_id,
           currency, discount_type,
           nds, from_dt, till_dt, payment_type,
           sum(amt_acts)            as amt_to_charge,
           sum(amt_oebs_w_nds)      as amt_to_pay_w_nds,
           sum(amt_oebs)            as amt_to_pay
      from (
            select contract_eid, contract_id, currency, nds, discount_type,
                   from_dt, till_dt, payment_type,
                   amt  as amt_acts, amt_w_nds  as amt_acts_w_nds,
                   0    as amt_oebs, 0          as amt_oebs_w_nds
              from s_kv_acts
             union all
            select contract_eid, contract_id, currency, nds, discount_type,
                   from_dt, till_dt, payment_type,
                   0    as amt_acts, 0          as amt_acts_w_nds,
                   amt  as amt_oebs, amt_w_nds  as amt_oebs_w_nds
              from s_payments
           )
     group by contract_eid, contract_id, currency, payment_type,
              nds, discount_type, from_dt, till_dt
),
-- Подготовка к контролю (заранее считаем бюджеообразующих клиентов)
s_kv_control_src as (
    select d.*,
           -- Соотношение оборота по Директу по клиенту к
           -- обороту по договору (Агентства)
           nvl(ratio_to_report(amt_rub_direct)
              over (partition by contract_id, from_dt), 0) as ratio
      from (
        select contract_id, from_dt, till_dt, client_id,
               nds_count, currency_count,
               sum(amt_rub)                                  as amt_rub,
               sum(decode(discount_type, 7, amt_rub, null))  as amt_rub_direct
          from (
                select d.*,
                       -- Проверяем, что по договору всегда была
                       -- 1 валюта и 1 НДС
                       count(distinct nds)
                        over (partition by contract_id) as nds_count,
                       count(distinct currency)
                        over (partition by contract_id) as currency_count
                  from s_acts_all d
               )
         group by contract_id, from_dt, till_dt, client_id,
                  nds_count, currency_count
           ) d
),
-- Штрафы
--  - оборот по договору >= 100к
--  - клиентов >= 5
--  - нет клиентов с оборотом > 70% (округляется до целых процентов) по Директу
--  - нет нерезидентов
--  - только 1 валюта в счетах
s_kv_control as (
    select d.*,
           case
           when amt_rub >= 100000
            and client_count >= 5
            and is_there_boc = 0
            and nds_count = 1
            and currency_count = 1
           then 0
           else 1
            end as failed
      from (
        select d.*,
               case when max_client_ratio_by_direct >= 0.7 then 1 else 0
                end as is_there_boc
          from (
            select contract_id, from_dt, till_dt,
                   nds_count, currency_count,
                   sum(amt_rub)             as amt_rub,
                   count(client_id)         as client_count,
                   round(max(ratio), 2)     as max_client_ratio_by_direct
              from s_kv_control_src
             group by contract_id, from_dt, till_dt,
                      nds_count, currency_count
               ) d
           ) d
),
-- Сумма КВ без контроля акты <= оплат
s_kv_pre as (
    select d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
		   d.payment_type,
           d.nds, d.currency,
           d.discount_type,
           sum(d.amt_to_charge)         as turnover_to_charge,
           sum(decode(f.failed,
                0, d.amt_to_charge*0.1,
                0))                     as reward_to_charge,
           sum(d.amt_to_pay)            as turnover_to_pay,
           sum(d.amt_to_pay_w_nds)      as turnover_to_pay_w_nds,
           -- BALANCE-19851
           -- Штрафовать по оплатам не должны
           sum(d.amt_to_pay*0.1)        as reward_to_pay
      from s_kv_src         d
      -- BALANCE-19851
      left outer
      join s_kv_control     f on f.contract_id = d.contract_id
                             and f.from_dt = d.from_dt
                             and f.till_dt = d.till_dt
     group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt, payment_type,
              d.discount_type, d.currency, d.nds
),
-- КВ с разбивкой по типам рекламы, без контроля, что оплат не более, чем актов
s_kv01 as (
    select contract_eid, contract_id, from_dt, till_dt,
           discount_type, currency, nds,
           -- к перечислению
           turnover_to_charge,
           reward_to_charge,
           -- к начислению (см. s_kv10)
           0                as turnover_to_pay,
           0                as turnover_to_pay_w_nds,
           decode(payment_type,
                -- BALANCE-19979: для предоплаты платим от актов
                2, reward_to_charge,
                -- постоплата
                3, 0)       as reward_to_pay
      from s_kv_pre
),
-- КВ с контролем, что оплат не более, чем актов
-- без разбивки по типам рекламы
s_kv10 as (
    select contract_eid, contract_id, from_dt, till_dt,
           currency, nds,
           -- к перечислению (см. s_kv01)
           0                                    as turnover_to_charge,
           0                                    as reward_to_charge,
           -- к начислению
           turnover_to_pay,
           turnover_to_pay_w_nds,
           (least(reward_to_charge_sum, reward_to_pay_sum) -
                least(reward_to_charge_sum_prev, reward_to_pay_sum_prev)
           )                                    as reward_to_pay
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
              from (
                    -- Убираем детализацию по типам рекламы
                select d.contract_id, contract_eid,
                       d.from_dt, d.till_dt, payment_type,
                       d.nds, d.currency,
                       sum(turnover_to_charge)      as turnover_to_charge,
                       sum(reward_to_charge)        as reward_to_charge,
                       sum(turnover_to_pay)         as turnover_to_pay,
                       sum(turnover_to_pay_w_nds)   as turnover_to_pay_w_nds,
                       sum(reward_to_pay)           as reward_to_pay
                  from s_kv_pre         d
                    -- оплаты показываем только для постоплаты
                 where payment_type = 3
                 group by d.contract_eid, d.contract_id,
                          d.from_dt, d.till_dt, payment_type,
                          d.currency, d.nds
                   ) d
           ) s
),
---------------V_OPT_2015_PROF_SKV
s_half_years as (
    select d.dt from_dt, add_months(d.dt, 6)-1/24/60/60 as till_dt
      from (
         select add_months(date'2015-03-01', 6*(level-1)) as dt
           from dual
        connect by level <= 10
           ) d
),

s_opt_2015_acts as (
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
       xxx.act_id                                             as act_id,
       xxx.act_dt                                             as act_dt,
       trunc(xxx.act_dt, 'MM')                                as from_dt,
       add_months(trunc(xxx.act_dt, 'MM'), 1) - 1/84600       as till_dt,
       xxx.amount                                        as amt_w_nds,
       xxx.amount-xxx.amount_nds-xxx.amount_nsp            as amt,
       xxx.amount*cr.rate                                as amt_w_nds_rub,
       (xxx.amount-xxx.amount_nds-xxx.amount_nsp)*cr.rate  as amt_rub
  from s_base        b
  join xxxx_new_comm_contract_basic      xxx  on xxx.invoice_id = b.invoice_id
                                          and xxx.hidden < 4
  join xxxx_currency_rate            cr on cr.cc = b.currency
                                          and cr.rate_dt = trunc(xxx.act_dt)
 where b.commission_type in (1, 2, 3, 4, 5, 6, 7, 8)
   and (
          -- base, prof, ua
          (
            b.commission_type in (1, 2, 6, 8)
        and b.discount_type in (1, 2, 3, 7, 11, 12)
          )
          -- spec
       or (b.commission_type = 3 and b.discount_type in (17))
          -- auto
       or (b.commission_type = 4 and b.discount_type in (19))
          -- sprav
       or (b.commission_type = 5 and b.discount_type = 12)
       )
)

--select * from s_opt_2015_acts;
,
s_opt_2015_prof_skv_less_70 as (
-- договора за период, по которым нет
-- клиентов с оборотом по директу > 70%
-- BALANCE-15995
select d.contract_id, d.from_dt, max(ratio) as max_ratio
  from (
    select d.*,
           -- BALANCE-15635
           nvl(ratio_to_report(amt_rub)
              over (partition by d.contract_id, d.from_dt), 0) as ratio
      from (
        select d.contract_id,
               d.client_id,
               h.from_dt,
               -- BALANCE-15635
               sum(decode(d.discount_type, 7, d.amt_rub, null))  as amt_rub
          from s_opt_2015_acts             d

          join s_half_years                   h  on d.act_dt between h.from_dt
                                                                 and h.till_dt
               where d.commission_type = 2
         group by h.from_dt,
                  d.contract_id,
                  d.client_id
           ) d
       ) d
 group by d.contract_id, d.from_dt
)
--select * from s_opt_2015_prof_skv_less_70;
,
-- Акты ЛК, у которых закончились программы в полугодии
-- Акты при этом надо брать не только за тек. год, т.к. бывают
-- программы, которые начинаются в прошлом фин. году. И по таким
-- ЛК надо учитывать весь оборот по программе, а не только тот,
-- которьй получился в текущем фин.году
-- BALANCE-18412
s_acts_lc_view as (
    select 
    b.contract_eid,
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
           b.is_loyal,
           b.client_id,
           b.amt_w_nds,
           b.amt,
           b.amt_w_nds_rub,
           b.amt_rub,
           b.act_dt,
           h.from_dt,
           trunc(h.till_dt)                         as till_dt,
           lc.lc_turnover  ,
           lc.collateral_first_dt                         as lc_start_dt,
           lc.collateral_end_dt                                as lc_end_dt
      from s_opt_2015_acts               b
      join XXXX_LOYAL_CLIENTS_CONTR_ATTR   lc on lc.contract_id = b.contract_id
                                              and lc.client_id = b.client_id
                                              -- только закончившиеся ДС
                                              -- относительно полугодия
                                              -- TODO: вспомнить, зачем это?
                                              and case
                                                    when date'2015-09-10' >= date'2016-03-01' then date'2016-03-01'-1/24/60/60
                                                    when date'2015-09-10' >= date'2015-09-01' then date'2015-09-01'-1/24/60/60
                                                    when date'2015-09-10' >= date'2015-03-01' then date'2015-03-01'-1/24/60/60
                                                    else trunc(date'2015-09-10', 'MM')
                                                end > lc.collateral_end_dt
                                                
                                                
--                                                      and 
--                                                     date'2015-09-01'-1/24/60/60 > lc.collateral_end_dt
                                                
      join s_half_years                     h  on lc.collateral_end_dt between h.from_dt
                                                                and h.till_dt
     where b.is_loyal = 1
       and b.act_dt between lc.collateral_first_dt and lc.collateral_end_dt
       and b.commission_type = 2
        -- только договора текущего фин.года
       and b.contract_till_dt > (select fin_year_dt from s_dates)
)

--select * from s_acts_lc_view;
,
s_skv_lc as (
    select d.*
      from (
        select d.contract_eid, d.contract_id,
               d.contract_from_dt,
               d.contract_till_dt,
               d.currency, d.nds, d.discount_type,
               d.client_id, d.is_loyal,
               d.lc_turnover,
               d.from_dt,
               d.till_dt,
               sum(d.amt_w_nds_rub)                     as amt_w_nds_rub,
               sum(d.amt_rub)                           as amt_rub
          from s_acts_lc_view                 d
         group by d.from_dt, d.till_dt,
                  d.contract_from_dt,
                  d.contract_till_dt,
                  d.contract_eid, d.contract_id,
                  d.currency, d.nds, d.discount_type,
                  d.client_id, d.is_loyal,
                  d.lc_turnover
           ) d
     where d.amt_rub >= lc_turnover*6*1.1
)
--select * from s_skv_lc;
,
s_skv_not_lc as (
    select d.contract_eid, d.contract_id,
           d.contract_from_dt,
           d.contract_till_dt,
           d.currency, d.nds, d.discount_type,
           d.client_id, d.is_loyal,
           -- для прогноза (для ЛК не нужно, т.к. ЛК не учитываем в прогнозе)
           -- чтобы знать, за какой период брать оборот для проноза
           d.till_dt                                    as till_dt_fc,
           -- периоды полугодия
           h.from_dt                                    as from_dt,
           trunc(h.till_dt)                             as till_dt,
           sum(d.amt_w_nds_rub)                         as amt_w_nds_rub,
           sum(d.amt_rub)                               as amt_rub
      from s_opt_2015_acts             d
      join s_half_years                   h  on d.act_dt between h.from_dt
                                                             and h.till_dt
        -- Без ЛК
       and d.is_loyal = 0
       and d.commission_type = 2
        -- Директ, Медийка (вся), Справочник
     where d.discount_type in (7, 1, 2, 3, 12)
     group by h.from_dt, trunc(h.till_dt), d.till_dt,
              d.contract_from_dt,
              d.contract_till_dt,
              d.contract_eid, d.contract_id,
              d.currency, d.nds, d.discount_type,
              d.client_id, d.is_loyal
),
s_opt_2015_prof_skv as(
select d."CONTRACT_EID",d."CONTRACT_ID",d."CONTRACT_FROM_DT",d."CONTRACT_TILL_DT",d."DISCOUNT_TYPE",d."CLIENT_ID",d."FROM_DT",d."TILL_DT",d."TILL_DT_FC",d."AMT_W_NDS_RUB",d."AMT_RUB_NOT_LC",d."AMT_RUB_LC",
       d.amt_rub_not_lc + d.amt_rub_lc as amt_rub,
       d.amt_rub_not_lc                as amt_for_forecast,
       -- ТЗ: Полугодовая премия начисляется Агентству только при условии, если
       -- стоимость фактически оказанных Яндексом в течение соответствующего
       -- Отчетного полугодия Услуг по сервису Директ, связанных с размещением
       -- Материалов одного Клиента Агентства < 70% (округляется до целых
       -- процентов)
       l.max_ratio,
       case
        when round(l.max_ratio, 2) < 0.7 then 0
        else 1
       end          as failed
  from (
        select d.contract_eid, d.contract_id,
               d.contract_from_dt,
               d.contract_till_dt,
               d.discount_type,
               d.client_id,
               d.from_dt, d.till_dt, d.till_dt_fc,
               d.amt_w_nds_rub,
               d.amt_rub            as amt_rub_not_lc,
               0                    as amt_rub_lc
          from s_skv_not_lc d
         union all
        select d.contract_eid, d.contract_id,
               d.contract_from_dt,
               d.contract_till_dt,
               d.discount_type,
               d.client_id,
               d.from_dt, d.till_dt, d.till_dt as till_dt_fc,
               d.amt_w_nds_rub,
               0                    as amt_rub_not_lc,
               d.amt_rub            as amt_rub_lc
          from s_skv_lc d
       ) d
       -- смотрим максимальную долю по клиенту по директу за полугодие
  join s_opt_2015_prof_skv_less_70   l on l.contract_id = d.contract_id
                                         and l.from_dt = d.from_dt
),
-- СКВ (полугодие)
s_skv as (
    select d.*,
           xx_calc_prof_skv(d.amt_rub, d.from_dt) as reward
      from (
        select d.contract_eid, d.contract_id, d.from_dt, d.till_dt,
               -- BALANCE-15641
               sum(amt_w_nds_rub)               as amt_w_nds_rub,
               sum(amt_rub)                     as amt_rub
          from s_opt_2015_prof_skv d
         where failed = 0
         group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt
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
       reward_to_pay                                -- к перечислению
  from (
        SELECT contract_eid, contract_id,
               from_dt, till_dt,
               discount_type,
               currency, nds,
               turnover_to_charge,
               reward_to_charge,
               turnover_to_pay,
               turnover_to_pay_w_nds,
               reward_to_pay,
               1            as reward_type
          from s_kv01
         union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               71           as discount_type,
               currency, nds,
               amt          as turnover_to_charge,
               reward       as reward_to_charge,
               null         as turnover_to_pay,
               null         as turnover_to_pay_w_nds,
               reward       as reward_to_pay,
               1            as reward_type
          from s_kv_lc
         union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               null         as discount_type,
               currency, nds,
               turnover_to_charge,
               reward_to_charge,
               turnover_to_pay,
               turnover_to_pay_w_nds,
               reward_to_pay,
               10           as reward_type
          from s_kv10
       union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               null                             as discount_type,
               -- BALANCE-15641
               'RUR'                            as currency,
               1                                as nds,
               amt_rub                          as turnover_to_charge,
               reward                           as reward_to_charge,
               amt_rub                          as turnover_to_pay,
               amt_w_nds_rub                    as turnover_to_pay_w_nds,
               reward                           as reward_to_pay,
               2                                as reward_type
          from s_skv
       )
       where reward_type = 2
--       where reward_to_charge!=0 
--       union all
--       select contract_id,
--       contract_eid,
--       from_dt,
--       till_dt,
--       nds,
--       currency,
--       discount_type,
--       reward_type,
--       turnover_to_charge,                          -- оборот к начислению
--       reward_to_charge,                            -- к начислению
--       turnover_to_pay_w_nds,
--       turnover_to_pay,                             -- оборот к перечислению
--       reward_to_pay                                -- к перечислению
--  from (
--        select contract_eid, contract_id,
--               from_dt, till_dt,
--               discount_type,
--               currency, nds,
--               turnover_to_charge,
--               reward_to_charge,
--               turnover_to_pay,
--               turnover_to_pay_w_nds,
--               reward_to_pay,
--               1            as reward_type
--          from s_kv01
--         union all  
--        select contract_eid, contract_id,
--               from_dt, till_dt,
--               null         as discount_type,
--               currency, nds,
--               turnover_to_charge,
--               reward_to_charge,
--               turnover_to_pay,
--               turnover_to_pay_w_nds,
--               reward_to_pay,
--               10           as reward_type
--          from s_kv10
--       )
--       where reward_to_pay!=0
  order by contract_id, from_dt, discount_type, currency, nds;
