with

 counting_date as (select date'2018-05-03' as dt from dual)
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
		   c.contract_from_dt                         as contract_from_dt,
           c.contract_till_dt                         as contract_till_dt,
           c.invoice_eid                              as invoice_eid,
           c.invoice_id                               as invoice_id,
           c.invoice_dt                               as invoice_dt,
           i.invoice_type                             as invoice_type,
		   i.total_sum                                as total_sum,           
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
            case
           -- Только для Директа/Медийки в Директе
           when nvl(c.commission_type, c.discount_type) in (7, 37) then
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
        ), 0)
        else 0
        end                                   as is_agency_credit_line,
           c.contract_commission_type                 as commission_type
  from xxxx_new_comm_contract_basic c
  left join XXXX_INVOICE i on c.invoice_id = i.inv_id
  where (
                                               (
                                                     -- только базовые/профы
                                                    c.contract_commission_type in (1, 2, 8, 21) and
                                                    -- то учитываем еще и код 22
                                                    nvl(c.commission_type,
                                                        c.discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 17, 19, 22, 25, 28, 29, 36, 37)
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
                                                    c.contract_commission_type in (17) and
                                                nvl(c.commission_type, c.discount_type) in (25)
                                                )
                                                or
                                                (
                                                    -- иначе, как ранее
                                                    nvl(c.commission_type, c.discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 17, 19, 28, 29)
                                                )
                                              )
)

,
  

s_brands as (
        select /*+ materialize */
               atr.key_num          as client_id,
               atr.start_dt         as collateral_dt,
               c.start_dt           as dt,
               nvl(c.finish_dt, date'2018-05-03')    as finish_dt,
               min(c.MAIN_CLIENT_ID)     as main_client_id
          from xxxx_contract_signed_attr    atr
          -- BALANCE-27379: клиента в тех.связке начинаем учитывать, когда
            -- начинает действовать допник, в кот.он был добавлен
          join xxxx_ui_contract_apex        c on c.contract_id = atr.contract_id
                                                
         where atr.code = 'BRAND_CLIENTS'
         group by atr.key_num, atr.start_dt, c.start_dt, nvl(c.finish_dt, date'2018-05-03')
)

--select  *from s_brands;
,
s_ar_acts_src as (
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
		 -- BALANCE-26651,  BALANCE-17175
       nvl(
           case xxx.commission_type
            when 22 then 1
            when 29 then 1  -- Аудиореклама == мейдика
            else xxx.commission_type
           end,
           b.discount_type)                             as discount_type,
       -- BALANCE-26651
           nvl(xxx.commission_type, b.discount_type_src)     as discount_type_src,
           b.is_agency_credit_line,
           case
           when nvl(xxx.is_loyal, 0) = 1 and b.discount_type = 7
            then 1
            else 0
           end                                               as is_loyal,
           xxx.client_id                                     as client_id,
           xxx.order_id, 
           xxx.service_id, 
           xxx.service_order_id,
--           nvl(brand.main_client_id, xxx.client_id)          as brand_id,
           xxx.act_id                                        as act_id,
           xxx.act_eid                                       as act_eid,
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
--      left outer
--      join s_brands                               brand on brand.client_id = xxx.client_id
     where b.commission_type in (1, 2, 3, 4, 5, 6, 8, 10, 11, 12, 13, 16, 17, 19, 21)
   and (
          -- base, prof
          (
            b.commission_type in (1, 2, 8, 21)
        and (
              
                -- BALANCE-24516: новые условия применимы только актов нового
                -- фин.года при условиия, что договор продлен
                xxx.act_dt >= date'2017-03-01' and b.contract_till_dt > date'2017-03-01' and b.discount_type in (1, 2, 3, 7, 12, 36, 37)
                -- BALANCE-22085
                -- В актах по новым и продленным договорам 2016:
                --  * отбрасываем маркет (11)
                --  * включаем Авто (25)
                --2016 год
	         -- BALANCE-24734: добавляем 36 тип, чтобы старые счета учесть
             or  xxx.act_dt >= date'2017-03-01' and b.contract_till_dt  <= date'2017-03-01' and b.discount_type in (1, 2, 3, 7, 12, 25, 36)
             or  xxx.act_dt >= date'2016-03-01' and xxx.act_dt < date'2017-03-01'   and b.discount_type in (1, 2, 3, 7, 12, 25)
             -- 2015 год
             or  xxx.act_dt >= date'2016-03-01' and b.contract_till_dt  <= date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
             or  xxx.act_dt  < date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
            )
          )
          -- ua
       or (
             b.commission_type = 6 
  and (
                   -- BALANCE-25339
                   xxx.act_dt >= date'2017-04-01'
               and b.discount_type in (0, 1, 7, 8, 12, 36)
                   -- BALANCE-22914
                or   xxx.act_dt >= date'2016-04-01' and xxx.act_dt < date'2017-04-01'
               and b.discount_type in (0, 1, 8, 7, 15)
                or  xxx.act_dt <  date'2016-04-01'
               and b.discount_type in (1, 2, 3, 7, 12)
                )
     )
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
--select  *from s_ar_acts_src;
,

s_ar_acts as (
     select  s.*,
       -- BALANCE-27709: тех связку берем на конец месяца, в котором сформирован
       --   акт, а не на "trunc(sysdate, 'MM') - 1", таким образом
       --   принцип формирование тех связки будет независим от
       --   даты запуска расчета
       nvl(b.main_client_id, s.client_id)               as brand_id   
       from s_ar_acts_src s
       left join s_brands     b on b.client_id = s.client_id
            -- BALANCE-27379: клиента в тех.связке начинаем учитывать, когда
            -- начинает действовать допник, в кот.он был добавлен
            and b.collateral_dt <= s.till_dt
        -- BALANCE-27363: только живые тех.связки на конец пред месяца
          and s.till_dt between b.dt and b.finish_dt
)
--select  *from s_ar_acts;
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


s_acts_src as ( select * from s_acts_temp
 where      -- BALANCE-22203
            -- BALANCE-22331
            -- BALANCE-23542
            -- BALANCE-22578
            -- в новых условиях оставляем только одна валюта
            -- BALANCE-24627: оптимизация (from_dt -> act_dt)
                act_dt >= date'2016-03-01'
            and currency_count = 1
            and nds_count = 1
            -- BALANCE-27062: belarus added
        and currency in ('RUR', 'BYN', 'BYR')
)

--select  *from s_acts_src;
,

s_stat_src as (
    select * from xxxx_ar_deal_stats
)

, s_last_stat_insert_dt as (
    select from_dt,    --Начало периода, truncated дата и время (например, 2018-03-01 00:00:00) ||  from_date, передаваемый в ручку
	till_dt,   	-- Окончание периода дата и время (2018-03-31 23:59:59) ||  to_date, передаваемый в ручку
	max(insert_dt) as insert_dt  --Дата и время получения статистики (не truncated)  ||  в ручку не передается, заполняется sysdate
      from s_stat_src
     group by from_dt, till_dt
)
,

s_ar_deal_stats as (

	select d.*,
		   dl.name                      as deal_name,     --VARCHAR2(512 BYTE) Указанное площадкой название сделки. Ограничение в директе - 255 символов
		   dl.agency_rev_ratio/1000000  as agency_reward_ratio,--NUMBER  Вознаграждение агентства, выбранное площадкой. В долях единицы (0,12 означает 12%)
		   dl.doc_date,   -- DATE Дата уведомления о подтверждении условий сделки
		   dl.doc_number  -- VARCHAR2(64 BYTE)?  Номер уведомления о подтверждении условий сделки
	  from s_stat_src                d
	  join s_last_stat_insert_dt     l on l.from_dt = d.from_dt
								 and l.till_dt = d.till_dt
								 and l.insert_dt = d.insert_dt
	  join xxxx_deal            dl on dl.external_id = d.deal_external_id
)
--select * from s_ar_deal_stats;
,

 	
s_pcts_dates as (
    select contract_id as contract2_id, 
           max(start_dt) as cl_last_dt
           from xxxx_contract_signed_attr
           where code = 'PERSONAL_DEAL_BASE_PCT'
           		and start_dt < trunc((select dt from counting_date), 'MM')
           group by contract_id
    
)
--select * from s_pcts_dates;
,

 s_pcts as (
    select h.contract_id as contract2_id , h.value_num/100 as pct
      from xxxx_contract_signed_attr      h
      join s_pcts_dates                         d on d.contract2_id = h.contract_id
                                                 and d.cl_last_dt = h.start_dt
)
--select * from s_pcts;

 ,

s_acts as (
    select contract_id, contract_eid, from_dt, till_dt, invoice_id, invoice_eid,
           order_id, service_id, service_order_id, act_id, act_eid,
           -- Коэффициент К (комиссия Яндекса и комиссия Adfox)
           p.pct                as ya_n_fox_pct,
           sum(amt_w_nds_rub)   as amt_w_nds_rub,
           sum(amt_rub)         as amt_rub
      from s_acts_src   a
      join s_pcts               p on p.contract2_id = a.contract_id
     where trunc((select dt from counting_date), 'MM') - 1/24/60/60 between from_dt and till_dt
     group by contract_id, contract_eid, from_dt, till_dt, invoice_id, invoice_eid,
              order_id, service_id, service_order_id, act_id, act_eid, p.pct
)
--select  *from s_acts;

--select  *from s_ar_deal_stats;
,

s_stats as (
    select service_id, service_order_id,
           deal_name, deal_external_id,
           doc_number, doc_date,
           -- % вознаграждения по сделке
           agency_reward_ratio                                  as pct,
           -- доля сделки в заказе
           cost/(sum(cost) over(
                partition by service_id, service_order_id))     as ratio
      from s_ar_deal_stats
     where trunc((select dt from counting_date), 'MM') - 1/24/60/60 between from_dt and till_dt
)


,

s_rewards as (
    select a.*, s.deal_name, s.pct, s.ratio,
           s.doc_date, s.doc_number,
           a.amt_rub/(1 + a.ya_n_fox_pct)                   as base_amt,
           (a.amt_rub/(1 + a.ya_n_fox_pct))*s.pct           as reward
      from s_acts       a
      join s_stats      s on s.service_id = a.service_id
                         and s.service_order_id = a.service_order_id
)


select contract_id,
       contract_eid,
       from_dt,
       till_dt,
       501                              as reward_type,
       invoice_id,
       invoice_eid,
       act_id,
       act_eid,
       order_id,
       deal_name,                                           -- название сделки
       doc_date,                                            -- дата уведомления
       doc_number,                                          -- номер уведомления
       amt_rub                          as turnover_ttl,    -- оборот из акта
       base_amt                         as turnover_reward, -- база для расчета премии
       pct,
       reward
  from s_rewards
  order by contract_id, from_dt,  invoice_id, act_id, reward

;