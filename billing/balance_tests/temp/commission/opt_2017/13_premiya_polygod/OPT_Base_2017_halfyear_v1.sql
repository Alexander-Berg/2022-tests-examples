 
with
-- опорные даты
s_dates as (
        -- начало фин. года
    select date'2015-04-01'   as fin_year_dt
      from dual
),

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
)
--select * from s_half_years;
,



counting_date as (select date'2017-05-03' as dt from dual)
--select * from counting_date;
,

s_base as (   
select distinct
           c.contract_eid      as contract_eid,
 		-- BALANCE-21757, BALANCE-21758
       -- BALANCE-22838
       -- недвижимость, авто, маркет: для договоров в фирме 1
       -- показываем связанные договора из БЮ.
       -- часть оборотов "висят" на старых договорах (фирма 1),
       -- часть оборотов — на новых договорах (фирмы бизнес юнитов).
       -- эти договоры имеют одинаковый external_id, но разные id.
       -- чтобы не ломался контроль оборотов, который работает на основе id,
       -- подменяем id для старых договоров, показывая id новых договоров.
       case
        when c.contract_commission_type in (4, 10, 12, 13)
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
       end                                            as contract_id,
           c.invoice_eid                              as invoice_eid,
           c.invoice_id                               as invoice_id,
           c.invoice_dt                               as invoice_dt,
           i.invoice_type                             as invoice_type,
           c.contract_from_dt                         as contract_from_dt,
           c.contract_till_dt                         as contract_till_dt,
           c.currency                                 as currency,
           c.nds                                      as nds, 
           c.nds_pct                                  as nds_pct,
           c.loyal_client                             as loyal_clients,
             -- BALANCE-17175
           case nvl(c.commission_type, c.discount_type)
            when 22 then 1
            when 29 then 1  -- Аудиореклама должа учитываться как мейдика
            else nvl(c.commission_type, c.discount_type)
           end                                      as discount_type,
		       nvl(c.commission_type, c.discount_type)  as discount_type_src,
           c.payment_type                             as payment_type, 
           decode(nvl(c.commission_type, c.discount_type),
             -- Только для Директа
              7,
          nvl((
            -- оплаты приходят на счет "на оплату", по нему выходим на ЛС
            -- по ЛС смотрим в доп.атрибуты на наличие subclient_id
            -- subclient_id is     null: агентская кредитная линия
            -- subclient_id is not null: клиентская кредитная линия
            select decode(p.value_num, null, 1, 0)
              from xxxx_invoice_repayment   ir
              join xxxx_extprops            p on p.object_id = ir.invoice_id
                                             and p.classname = 'PersonalAccount'
                                             and p.attrname = 'subclient_id'
             where ir.repayment_invoice_id = c.invoice_id 
               union
                select 1
                   from xxxx_invoice_repayment   ir              
                      where ir.repayment_invoice_id = c.invoice_id 
                     and not exists (
                     select 1 from xxxx_extprops p 
                      where p.object_id = ir.invoice_id
                     and p.classname = 'PersonalAccount'
                     and p.attrname = 'subclient_id')
        -- Если ничего не найдено, то не ЛС вообще, поэтому не агентская кр.л.
        ), 0),
        0)                                      as is_agency_credit_line,
           c.contract_commission_type                 as commission_type
  from xxxx_new_comm_contract_basic c
  left join XXXX_INVOICE i on c.invoice_id = i.inv_id
  where (
                                                -- BALANCE-17175
                                                (
                                                     -- только базовые/профы
                                                    c.contract_commission_type in (1, 2, 8) and
                                                    -- то учитываем еще и код 22
                                                    nvl(c.commission_type,
                                                        c.discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 17, 19, 22, 25, 28, 29, 36)
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
                                                    -- Маркет, спецпроекты
                                                    c.contract_commission_type in (14) and
                                                    nvl(c.commission_type, c.discount_type) in
                                                    (33)
                                                )
                                                or
                                                
                                                (
                                                    -- По Недвижимости смотрим всё
                                                    c.contract_commission_type in (10, 16) and 1=1
                                                )
                                                or
                                                (
                                                    -- По Украине пропускаем все,
                                                    -- чтобы было в 15/16 году.
                                                    -- Отфильтруем более точно
                                                    -- на уровне v_opt_2015_acts
                                                    -- BALANCE-23716
                                                    c.contract_commission_type in (6) and
                                                    nvl(c.commission_type, c.discount_type) in
                                                    (0, 1, 2, 3, 7, 8, 12, 15, 36)
                                                )
                                                  or
                                                (
                                                    -- Медийка Вертикали — Медийка-Авто.ру
                                                    c.contract_commission_type in (17)
                                                and nvl(c.commission_type, c.discount_type) in (25)
                                                )
                                               or
                                                (
                                                    -- иначе, как ранее
                                                    nvl(c.commission_type, c.discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 17, 19, 28, 29)
                                                )
                                              )
)
--select * from s_base order by  in voice_id ;
,

-- История действия подписанных атрибутов
s_attrs_src as (
    select key_num   as value_num,
           contract_id                                         as contract_id,
           code,
           start_dt                                                as from_dt,
           nvl(
                lead(start_dt) over(partition by code, contract_id
                                     order by stamp),
                add_months(trunc((select dt from counting_date), 'MM'), 11)
           ) - 1/24/60/60                                       as till_dt
      from xxxx_contract_signed_attr
)
--select  *from s_attrs_src;
,
s_changes_payment_type as (
    select *
      from (
            select s.contract_id, s.from_dt, s.till_dt, s.value_num
              from s_attrs_src s
             where s.code in ('PAYMENT_TYPE')
               and exists (select 1 from xxxx_new_comm_contract_basic d
                            where d.contract_id = s.contract_id)
          )
)

--select * from s_changes_payment_type;
,
s_brands as (
        select /*+ materialize */
               atr.key_num          as client_id,
               min(c.MAIN_CLIENT_ID)     as main_client_id
          from xxxx_contract_signed_attr    atr
          join xxxx_ui_contract_apex        c on c.contract_id = atr.contract_id
                                               -- только живые тех.связки
                                              and (c.finish_dt is null
                                                or c.finish_dt >= trunc((select dt from counting_date), 'MM') - 1)
         where atr.code = 'BRAND_CLIENTS'
         group by atr.key_num
)

--select  *from s_brands;
,

s_ar_acts as (
    select 
    b.contract_eid,
           b.contract_id,
           b.invoice_eid,
           b.invoice_id,
           b.invoice_dt,
           b.invoice_type,
           b.contract_from_dt,
           b.contract_till_dt,
           b.currency,
           b.nds,
           b.payment_type,
           b.commission_type,
           b.discount_type,
           b.discount_type_src,
           b.is_agency_credit_line,
           case
           when nvl(xxx.is_loyal, 0) = 1 and b.discount_type = 7
            then 1
            else 0
           end                                               as is_loyal,
           xxx.client_id                                      as client_id,
           nvl(brand.main_client_id, xxx.client_id)             as brand_id,
           xxx.act_id                                             as act_id,
           xxx.act_eid                                            as act_eid,
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
                                              -- BALANCE-24627: более ранние акты не нужны
                                              and xxx.act_dt >= date'2015-03-01'
                                           -- BALANCE-24798: исключаем ЛК
                                              and ( xxx.is_loyal = 0 and xxx.act_dt >= date'2017-03-01'
                                                or xxx.act_dt < date'2017-03-01')

                                             
                                             
      join xxxx_currency_rate              cr on cr.cc = b.currency
                                              and cr.rate_dt = trunc(xxx.act_dt)
      left outer
      join s_brands                               brand on brand.client_id = xxx.client_id
     where b.commission_type in (1, 2, 3, 4, 5, 6, 8, 10, 11, 12, 13, 16, 17, 19)
   and (
          -- base, prof
          (
            b.commission_type in (1, 2, 8)
        and (
              
                -- BALANCE-24516: новые условия применимы только актов нового
                -- фин.года при условиия, что договор продлен
                xxx.act_dt >= date'2017-03-01' and b.contract_till_dt > date'2017-03-01' and b.discount_type in (1, 2, 3, 7, 12,36)
                -- BALANCE-22085
                -- В актах по новым и продленным договорам 2016:
                --  * отбрасываем маркет (11)
                --  * включаем Авто (25)
                --2016 год
             or  xxx.act_dt >= date'2017-03-01' and b.contract_till_dt  <= date'2017-03-01' and b.discount_type in (1, 2, 3, 7, 12, 25, 36)
             -- BALANCE-24734: добавляем 36 тип, чтобы старые счета учесть
             or  xxx.act_dt >= date'2016-03-01' and xxx.act_dt < date'2017-03-01'   and b.discount_type in (1, 2, 3, 7, 12, 25)
             -- 2015 год
             or  xxx.act_dt >= date'2016-03-01' and b.contract_till_dt  <= date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
             or  xxx.act_dt  < date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
            )
          )
          -- ua
       or (b.commission_type = 6 
   and(
                   -- BALANCE-25339
                   xxx.act_dt >= date'2017-04-01'
               and b.discount_type in (0, 1, 7, 8, 12, 36)
                   -- BALANCE-22914
                or   xxx.act_dt >= date'2016-04-01' and xxx.act_dt < date'2017-04-01'
               and b.discount_type in (0, 1, 8, 7, 15)
                or  xxx.act_dt <  date'2016-04-01'
               and b.discount_type in (1, 2, 3, 7, 12)
                ))
          -- spec
       or (b.commission_type = 3 and b.discount_type in (17))
          -- auto
       or (b.commission_type = 4 and b.discount_type in (19))
          -- sprav
       or (b.commission_type = 5 and b.discount_type = 12)
          -- estate
       or (b.commission_type in (10, 16) and 1=1)
          -- audio
       or (b.commission_type = 11 and b.discount_type_src = 29)
          -- market
       or (b.commission_type in (12, 13) and b.discount_type_src = 11)
          -- media verticals
       or (b.commission_type = 17 and b.discount_type = 25)
          -- verticals ico
       or (b.commission_type = 19 and b.discount_type = 7)
       )
)
--select  *from s_ar_acts  order by contract_id ;
,
s_acts_temp as (
    select b.*,
           -- Проверяем, что по договору всегда была -- 1 валюта и 1 НДС
           count(distinct b.nds)      over (partition by b.contract_id) as nds_count,
           count(distinct b.currency) over (partition by b.contract_id) as currency_count
      from s_ar_acts  b
        -- BALANCE-24627: только акты за тек.год
     where b.act_dt >= date'2017-03-01'
)

--select * from s_acts_temp;
,

s_opt_2015_acts as ( select * from s_acts_temp
 where 
            -- BALANCE-22203
            -- BALANCE-22331
            -- BALANCE-23542
            -- BALANCE-22578
            -- в новых условиях оставляем только одна валюта
            -- BALANCE-24627: оптимизация (from_dt -> act_dt)
                act_dt >= date'2016-03-01'
            and currency_count = 1
            and nds_count = 1
            and currency = 'RUR'
           
)
--select  *from s_opt_2015_acts;
,

s_acts as (
    select b.*
      from s_opt_2015_acts b
     where b.commission_type in (1, 8)
)
--select * from s_acts;
,


s_opt_2015_base_skv_less_70 as (
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
               d.brand_id                                        as client_id,
               h.from_dt,
               -- BALANCE-15635
               sum(decode(d.discount_type, 7, d.amt_rub, null))  as amt_rub
          from s_opt_2015_acts             d

          join s_half_years                   h  on d.act_dt between h.from_dt
                                                                 and h.till_dt
         where d.commission_type in (1, 8)
         group by h.from_dt,
                  d.contract_id,
                  d.brand_id
           ) d
       ) d
 group by d.contract_id, d.from_dt
)
--select * from s_opt_2015_base_skv_less_70;
,
-- Акты ЛК, у которых закончились программы в полугодии
-- Акты при этом надо брать не только за тек. год, т.к. бывают
-- программы, которые начинаются в прошлом фин. году. И по таким
-- ЛК надо учитывать весь оборот по программе, а не только тот,
-- которьй получился в текущем фин.году
-- BALANCE-18412
s_acts_lc_view as (
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
           lc.lc_turnover,
           -- BALANCE-20689
           round(abs(months_between(
              lc.collateral_end_dt, lc.collateral_first_dt)))      as lc_months_count,
           lc.collateral_first_dt                                as lc_start_dt,
           lc.collateral_end_dt                                as lc_end_dt
      from s_acts               b
      join XXXX_LOYAL_CLIENTS_CONTR_ATTR   lc on lc.contract_id = b.contract_id
                                              and lc.client_id = b.client_id
                                              -- только закончившиеся ДС
                                              -- относительно полугодия
                                              -- TODO: вспомнить, зачем это?
                                              and case
                                                    when date'2016-03-01' >= date'2016-03-01' then date'2016-03-01'-1/24/60/60
                                                    when date'2015-03-01' >= date'2015-09-01' then date'2015-09-01'-1/24/60/60
                                                    when date'2016-03-01' >= date'2015-03-01' then date'2015-03-01'-1/24/60/60
                                                    else trunc(date'2016-1-10', 'MM')
                                                end > lc.collateral_end_dt
                                                
                                                
--                                                      and 
--                                                     date'2016-03-01'-1/24/60/60 > lc.collateral_end_dt
                                                
      join s_half_years                     h  on lc.collateral_end_dt between h.from_dt
                                                                and h.till_dt
     where b.is_loyal = 1
       and b.act_dt between lc.collateral_first_dt and lc.collateral_end_dt
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
               d.currency, d.nds, d.discount_type, d.commission_type,
               d.client_id, d.is_loyal,
               d.lc_turnover,
               d.from_dt,
               d.till_dt,
               d.lc_months_count,
			   d.nds_count, d.currency_count,
               sum(d.amt_w_nds_rub)                     as amt_w_nds_rub,
               sum(d.amt_rub)                           as amt_rub
          from s_acts_lc_view                 d
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
--select * from s_skv_lc;
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
      from s_acts             d
      join s_half_years                   h  on d.act_dt between h.from_dt
                                                             and h.till_dt
        -- Без ЛК
       and d.is_loyal = 0
        -- Директ, Медийка (вся), Справочник
		-- BALANCE-22408
     where d.discount_type in (7, 1, 2, 3, 12, 36)
     group by h.from_dt, trunc(h.till_dt), d.till_dt,
              d.contract_from_dt,
              d.contract_till_dt,
              d.contract_eid, d.contract_id,
              d.currency, d.nds, d.discount_type, d.commission_type,
              d.nds_count, d.currency_count,
              d.client_id, d.is_loyal
)
--select * from s_skv_not_lc;

,

s_opt_2015_base_skv as ( select d.contract_eid, d.contract_id,
       d.contract_from_dt,
       d.contract_till_dt,
       decode(d.discount_type,
            -- Справочник считаем как Директ, т.к. шкала по общему обороту
            12, 7,
            -- Вся медийка под номером 1 будет
            2, 1, 3, 1,
            d.discount_type) as discount_type,
       d.nds,
       d.client_id,
       d.from_dt, d.till_dt, d.till_dt_fc,
       d.nds_count, d.currency_count,
       d.amt_w_nds_rub,
       d.amt_rub_not_lc,
       d.amt_rub_lc,
       d.amt_rub_not_lc + d.amt_rub_lc as amt_rub,
       d.amt_rub_not_lc                as amt_for_forecast,
       -- ТЗ: Полугодовая премия начисляется Агентству только при условии, если
       -- стоимость фактически оказанных Яндексом в течение соответствующего
       -- Отчетного полугодия Услуг по сервису Директ, связанных с размещением
       -- Материалов одного Клиента Агентства < 70% (округляется до целых
       -- процентов)
       l.max_ratio,
       0                        as excluded,
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
               d.client_id, d.nds,
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
               d.client_id, d.nds,
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

--select * from s_opt_2015_base_skv;
,
s_skv_result as (
    select contract_eid, contract_id, from_dt, till_dt, nds,
           sum(amt_rub)         as amt_rub,
           sum(amt_w_nds_rub)   as amt_w_nds_rub,
           sum(reward)          as reward
      from (
    select d.*,
           case
            when nds_count = 1 and currency_count = 1
            then bo.pk_comm.calc_base_skv(d.amt_rub, d.from_dt, d.discount_type)
            else 0
           end                                  as reward
      from (
        select d.contract_eid, d.contract_id, d.from_dt, d.till_dt, d.nds,
               d.nds_count, d.currency_count, d.discount_type,
               -- BALANCE-15641
               sum(amt_w_nds_rub)               as amt_w_nds_rub,
               sum(amt_rub)                     as amt_rub
          from s_opt_2015_base_skv d
         where failed = 0
           and excluded = 0
         group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt, d.nds,
                  d.nds_count, d.currency_count, d.discount_type
           ) d
           )
     group by contract_eid, contract_id, from_dt, till_dt, nds
)

--select * from s_skv_result;

select distinct s."CONTRACT_ID",s."CONTRACT_EID",s."FROM_DT",s."TILL_DT",s."NDS",s."CURRENCY",s."DISCOUNT_TYPE",s."REWARD_TYPE",s."TURNOVER_TO_CHARGE",s."REWARD_TO_CHARGE",s."TURNOVER_TO_PAY",s."TURNOVER_TO_PAY_W_NDS",s."REWARD_TO_PAY",s."REWARD_TO_PAY_SRC"
  from (
    select contract_id,     contract_eid,
           from_dt,         till_dt,
           nds,             currency,
           discount_type,
           300 + reward_type            as reward_type,
           -- к начислению
           sum(turnover_to_charge)      as turnover_to_charge,
           sum(reward_to_charge)        as reward_to_charge,
--           0                            as delkredere_to_charge,
--           0                            as dkv_to_charge,
           -- к перечислению
           sum(turnover_to_pay)         as turnover_to_pay,
           sum(turnover_to_pay_w_nds)   as turnover_to_pay_w_nds,
           sum(reward_to_pay)           as reward_to_pay,
           sum(reward_to_pay_src)       as reward_to_pay_src
--           0                            as delkredere_to_pay,
--           0                            as dkv_to_pay
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
                   reward_to_pay_src,
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
                             reward_to_pay_src,                           -- к перечислению
                             reward_to_pay                                -- к перечислению
                        from (
                        select contract_eid, contract_id,
                                     from_dt, till_dt,
                                     null                             as discount_type,
                                     -- BALANCE-15641
                                     'RUR'                            as currency,
                                     nds,
                                     amt_rub                          as turnover_to_charge,
                                     reward                           as reward_to_charge,
                                     amt_rub                          as turnover_to_pay,
                                     amt_w_nds_rub                    as turnover_to_pay_w_nds,
                                     reward                           as reward_to_pay,
                                     reward                           as reward_to_pay_src,
                                     2                                as reward_type
                                from s_skv_result
                                  -- BALANCE-24627: полугод считаем только 2 раза в год
                               where to_char(date'2017-09-02', 'MM') in ('03', '09')
                                 )
         )
         )
     group by contract_id,     contract_eid,
              from_dt,         till_dt,
              nds,             currency,
              discount_type,   reward_type
)            s

 order by contract_id, from_dt, discount_type, currency, nds;
