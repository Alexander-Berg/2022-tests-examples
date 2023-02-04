with
-- опорные даты
s_dates as (
        -- начало фин. года
    select date'2015-04-01'   as fin_year_dt
      from dual
)
--select * from s_dates;
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

 /*
  *  Расчет комиссионного вознаграждения на 15/16 год
  *  для базовых договоров.
  *
  *  СКВ, без разбивки по типам рекламы
  *
  *  Сделано отдельным представлением, чтобы можно было использовать
  *  в прогнозах и различных выгрузках
  *
  */
 
 -- полугодия
 s_half_years as (
     -- первое полугодие — из 5 месяцев
     select d.dt from_dt, add_months(d.dt, 5)-1/24/60/60 as till_dt
       from (
          select date'2015-04-01' as dt
            from dual
            ) d
      union all
     -- остальные — по 6 месяцев
     select d.dt from_dt, add_months(d.dt, 6)-1/24/60/60 as till_dt
       from (
          select add_months(date'2015-09-01', 6*(level-1)) as dt
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
--       xxx.external_id                                    as act_eid,
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
            b.commission_type in (1, 2, 8)
        and b.discount_type in (1, 2, 3, 7, 11, 12)
          )       
          -- ua
       or (b.commission_type = 6 and b.discount_type in (1, 2, 3, 7, 12))
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
 s_acts as (
      select b.*,
      -- Проверяем, что по договору всегда была
      -- 1 валюта и 1 НДС
    count(distinct nds)
    over (partition by b.contract_id)       as nds_count,
    count(distinct currency)
    over (partition by b.contract_id)       as currency_count
       from s_opt_2015_acts b
      where b.commission_type in (1, 8)
 ),
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
            b.commission_type,
            b.is_loyal,
            b.client_id,
            b.amt_w_nds,
            b.amt,
            b.amt_w_nds_rub,
            b.amt_rub,
            b.act_dt,
            h.from_dt,
            trunc(h.till_dt)                         as till_dt,
            b.nds_count, b.currency_count,
            lc.lc_turnover                            as lc_turnover,
            -- BALANCE-20689
            round(abs(months_between(
                 lc.collateral_end_dt, lc.collateral_first_dt)))      as lc_months_count,
            lc.collateral_first_dt                               as lc_start_dt,
            lc.collateral_end_dt                                as lc_end_dt
       from s_acts                        b
       join XXXX_LOYAL_CLIENTS_CONTR_ATTR   lc on lc.contract_id = b.contract_id
                                               and lc.client_id = b.client_id
                                               -- только закончившиеся ДС
                                               -- относительно полугодия
                                               -- TODO: вспомнить, зачем это?
                                               and case
                                                     when date'2015-09-10' >= date'2016-03-01' then date'2016-03-01'-1/24/60/60
                                                     when date'2015-09-10' >= date'2015-09-01' then date'2015-09-01'-1/24/60/60
                                                     when date'2015-09-10' >= date'2015-04-01' then date'2015-04-01'-1/24/60/60
                                                     else trunc(date'2015-09-10', 'MM')
                                                 end > lc.collateral_end_dt
--                                                        and 
--                                                     date'2015-09-01'-1/24/60/60 > lc.collateral_end_dt
       join s_half_years                     h  on lc.collateral_end_dt between h.from_dt
                                                                 and h.till_dt
      where b.is_loyal = 1
        and b.act_dt between lc.collateral_first_dt and lc.collateral_end_dt
         -- только договора текущего фин.года
        and b.contract_till_dt > (select fin_year_dt from s_dates)
 )
 
--  select * from s_acts_lc;
 ,
 s_skv_lc as (
     select d.*
       from (
         select d.contract_eid, d.contract_id,
                d.contract_from_dt,
                d.contract_till_dt,
                d.currency, d.nds, d.discount_type, d.commission_type,
                d.client_id, d.is_loyal,
                d.lc_turnover,
                d.from_dt,
                d.till_dt,
                d.lc_months_count,
                d.nds_count, d.currency_count,
                sum(d.amt_w_nds_rub)                     as amt_w_nds_rub,
                sum(d.amt_rub)                           as amt_rub
           from s_acts_lc_view                d
          group by d.from_dt, d.till_dt,
                   d.contract_from_dt,
                   d.contract_till_dt,
                   d.contract_eid, d.contract_id,
                   d.currency, d.nds, d.discount_type, d.commission_type,
                   d.client_id, d.is_loyal, d.lc_months_count,
                   d.nds_count, d.currency_count,
                   d.lc_turnover
            ) d
      where d.amt_rub >= lc_turnover*lc_months_count*1.1
 )
 

 ,
 s_skv_not_lc as (
     select d.contract_eid, d.contract_id,
            d.contract_from_dt,
            d.contract_till_dt,
            d.currency, d.nds, d.discount_type, d.commission_type,
            d.client_id, d.is_loyal,
            d.nds_count, d.currency_count,
            -- для прогноза (для ЛК не нужно, т.к. ЛК не учитываем в прогнозе)
            -- чтобы знать, за какой период брать оборот для проноза
            d.till_dt                                    as till_dt_fc,
            -- периоды полугодия
            h.from_dt                                    as from_dt,
            trunc(h.till_dt)                             as till_dt,
            sum(d.amt_w_nds_rub)                         as amt_w_nds_rub,
            sum(d.amt_rub)                               as amt_rub
       from s_acts                    d
       join s_half_years                 h  on d.act_dt between h.from_dt
                                                            and h.till_dt
         -- Без ЛК
        and d.is_loyal = 0
         -- Директ, Медийка (вся), Справочник
      where d.discount_type in (7, 1, 2, 3, 12)
      group by h.from_dt, trunc(h.till_dt), d.till_dt,
               d.contract_from_dt,
               d.contract_till_dt,
               d.contract_eid, d.contract_id,
               d.currency, d.nds, d.discount_type, d.commission_type,
               d.nds_count, d.currency_count,
               d.client_id, d.is_loyal
 )
-- select * from s_skv_not_lc;
 ,
 
 /*
  *  Расчет комиссионного вознаграждения на 15/16 год
  *  для базовых договоров.
  *
  *  Для СКВ. Те, у кого менее 70% по директу.
  *  Вынесено отдельно, чтобы можно было быстро проверять,
  *  прошел ли клиент эту проверку или нет.
  */
 
s_opt_2015_base_skv_less_70 as(
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
          where d.commission_type in (1, 8)
          group by h.from_dt,
                   d.contract_id,
                   d.client_id
            ) d
        ) d
  group by d.contract_id, d.from_dt
  
  )
  ,
 
 s_opt_2015_base_skv as(
 select d.*,
        d.amt_rub_not_lc + d.amt_rub_lc as amt_rub,
        -- ТЗ: Полугодовая премия начисляется Агентству только при условии, если
        -- стоимость фактически оказанных Яндексом в течение соответствующего
        -- Отчетного полугодия Услуг по сервису Директ, связанных с размещением
        -- Материалов одного Клиента Агентства < 70% (округляется до целых
        -- процентов)
        l.max_ratio,
        case
         when round(l.max_ratio, 2) < 0.7
     -- BALANCE-20688
            and nds_count = 1
            and currency_count = 1
            then 0
         else 1
        end          as failed
   from (
         select d.contract_eid, d.contract_id,
                d.contract_from_dt,
                d.contract_till_dt,
                d.discount_type, d.commission_type,
                d.client_id,
                d.nds_count, d.currency_count,
                d.from_dt, d.till_dt, d.till_dt_fc,
                d.amt_w_nds_rub,
                d.amt_rub            as amt_rub_not_lc,
                0                    as amt_rub_lc
           from s_skv_not_lc d
          union all
         select d.contract_eid, d.contract_id,
                d.contract_from_dt,
                d.contract_till_dt,
                d.discount_type, d.commission_type,
                d.client_id,
                d.nds_count, d.currency_count,
                d.from_dt, d.till_dt, d.till_dt as till_dt_fc,
                d.amt_w_nds_rub,
                0                    as amt_rub_not_lc,
                d.amt_rub            as amt_rub_lc
           from s_skv_lc d
        ) d
        -- смотрим максимальную долю по клиенту по директу за полугодие
   join s_opt_2015_base_skv_less_70   l on l.contract_id = d.contract_id
                                          and l.from_dt = d.from_dt
)
,


-- скв (полугодие)
 -- BALANCE-19948, BALANCE-19941
 s_skv as (
     select d.*,
            xx_calc_base_skv(d.amt_rub, d.from_dt) as reward
       from (
         select d.contract_eid, d.contract_id, d.from_dt, d.till_dt,
                -- BALANCE-15641
                sum(amt_w_nds_rub)               as amt_w_nds_rub,
                sum(amt_rub)                     as amt_rub
           from s_opt_2015_base_skv d
          where failed = 0
          group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt
            ) d
            )




-- результирующий запрос
select * from  (select contract_id,
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
               null                             as discount_type,
               -- BALANCE-15641
               'RUR'                            as currency,
               1                                as nds,
               amt_rub                          as turnover_to_charge,
               reward                           as reward_to_charge,
               amt_rub                          as turnover_to_pay,
               amt_w_nds_rub                   as turnover_to_pay_w_nds,
               reward                            as reward_to_pay,
               2                                as reward_type
          from s_skv
        
       )
       
       where reward_type = 2
)
order by contract_id, from_dt, discount_type, currency, nds;

