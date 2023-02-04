with
  
   
  counting_date as (select date'2018-06-03' as dt from dual)
  --select * from counting_date;
  ,
  
  
  s_cltrl as (
      select d.*, 
             start_dt                                                 as from_dt,
             nvl(
                lead(start_dt) over(partition by code, contract_id
                                     order by collateral_id),
                add_months(trunc((select dt from counting_date), 'MM'), 3)
           ) - 1/24/60/60                                       as till_dt
      from (
        select distinct contract_id, collateral_id, start_dt , code, stamp
          from xxxx_contract_signed_attr
           ) d
)
--select  *from s_cltrl;  

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
--select * from s_base order by  invoice_id ;
,



s_brands as (
        select /*+ materialize */
               atr.key_num          as client_id,
               atr.start_dt         as collateral_dt,
               c.start_dt           as dt,
--               nvl(c.finish_dt, date'2018-06-03')    as finish_dt,
               trunc(nvl(c.finish_dt, date'2018-06-03')) + 1 - 1/24/60/60            as finish_dt,
               min(c.MAIN_CLIENT_ID)     as main_client_id
          from xxxx_contract_signed_attr    atr
          -- BALANCE-27379: клиента в тех.связке начинаем учитывать, когда
            -- начинает действовать допник, в кот.он был добавлен
          join xxxx_ui_contract_apex        c on c.contract_id = atr.contract_id
                                              where atr.code = 'BRAND_CLIENTS'
         group by atr.key_num, atr.start_dt, c.start_dt, nvl(c.finish_dt, date'2018-06-03')
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
           xxx.agency_id                                      as agency_id,
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
       left outer 
       join s_brands     b on b.client_id = s.client_id
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

--select * from s_acts_src;

,
s_acts as (
 select b.*
      from s_acts_src   b
           -- 1 - Базовая 2015
           -- 8 - Базовая, регионы 2015
           -- 21 - Базовая СПб
     where b.commission_type in (1, 8, 21)
)

--select  *from s_acts_src;
,

--select * from s_acts_src;

s_quarters AS (
    select d.dt from_dt, add_months(d.dt, 3) - 1/24/60/60 as till_dt
      from (
         select add_months(date'2016-03-01', 3*(level-1)) as dt
           from dual
        connect by level <= 20
              ) d
     where trunc((select dt from counting_date), 'MM') - 1 between d.dt and add_months(d.dt, 3)
        or add_months(trunc((select dt from counting_date), 'MM') - 1, -12) between d.dt
        and add_months(d.dt, 3)

)

--select * from s_quarters;
-- BALANCE-25122: исключаемые из расчета аг-ва, все подписанные допники
,
s_excluded_clients_pre as (
    select contract_id,
           key_num                                             as client_id,
           start_dt                                            as from_dt
      from Xxxx_Contract_Signed_Attr
     where code = 'EXCLUDE_CLIENTS'
        -- дата начала действия ДС — не позднее последнего дня прошного месяца
        -- (можно было бы брать только прошлый квартал, но нам надо учесть
        -- ситуацию, когда допник просто подолжает све дествие в след.квартале)
       and start_dt < trunc((select dt from counting_date), 'MM')
        -- только включенные аг-ва (если тут 0/null, то не должны исключать)
       and value_num = 1
)

--select * from s_excluded_clients_pre;
,
-- BALANCE-26611: выбираем активный допник на конец квартала
s_ar_excluded_clients as (
select d."CONTRACT_ID",d."CLIENT_ID",d."FROM_DT"
  from s_excluded_clients_pre      d
  join (
        select contract_id, max(from_dt) as from_dt
          from s_excluded_clients_pre
         group by contract_id
       )                            l on l.contract_id = d.contract_id
                                     and l.from_dt = d.from_dt
)

--select * from s_ar_excluded_clients;

,
s_consolidate_cls_src as (
        select 
             /*+ materialize */
              cp.contract_id,
              cp.collateral_id,
              cp.start_dt           as start_dt,
              fd.value_str   as finish_dt
--               nvl(s.is_signed, s.is_faxed)             as sign_dt

          from xxxx_contract_signed_attr    cp
          -- BALANCE-27379: клиента в тех.связке начинаем учитывать, когда
            -- начинает действовать допник, в кот.он был добавлен
          join xxxx_contract_signed_attr       fd on 
                               fd.code = 'CONSOLIDATION_FINISH_DT'
                              and fd.collateral_id = cp.collateral_id
                              
           -- BALANCE-28305: Дата окончания допника на объединение (CONSOLIDATION_FINISH_DT)
           -- хранится без секунд (в транкнутом виду). Добавляем секунды явно, чтобы
           -- проверка на вхождение в интервал проходила
           join s_quarters                           q on q.till_dt between cp.start_dt and trunc(to_date(fd.value_str)) + 1 - 1/24/60/60
                                                  -- смотрим только прошлый квартал
                                                    and trunc((select dt from counting_date), 'MM')-1 between q.from_dt and q.till_dt
          where cp.code = 'REWARD_CONSOLIDATION_PERIOD'
           -- период консолидации - квартал
           and cp.value_num = 2
)

--select  * from s_consolidate_cls_src;


-- История действия подписанных атрибутов
, 

s_attrs_src as (
    select value_num,
--           update_dt,
           contract_id                                         as contract_id,
           code,
           start_dt                                            as from_dt,
           nvl(
            lead(start_dt) over(partition by code, contract_id
                                 order by stamp),
            add_months(trunc((select dt from counting_date), 'MM'), 3)
           ) -1/24/60/60                                        as till_dt
      from Xxxx_Contract_Signed_Attr
)
-- Подписанные атрибуты на конец пред. месяца
--select * from s_attrs_src;
, 

s_attrs as (
    select *
      from s_attrs_src
        -- BALANCE-25145: смотрим значение атрибута на конец пред.месяца
     where trunc((select dt from counting_date), 'MM') - 1 between from_dt and till_dt
)
--select  * from s_attrs;
,

s_ar_consolidations_q as (
  select 
  cp.contract_id,
       -- сразу получаем для главного договора:
       -- аг-во, тип премии, чтобы удобнее делать подмену
       c.contract_eid    as contract_eid,
       c.client_id      as agency_id,
       c.commission_type     as commission_type,
       c.contract_from_dt           as contract_from_dt,
       c.contract_till_dt     as contract_till_dt,
       cp.start_dt,
       cp.finish_dt,
--       cp.sign_dt,
       lc.key_num       as linked_contract_id,
       cl.client_id     as linked_agency_id,
       -- 1 - пропорционально
       -- 2 - на главный
       ct.value_num     as cons_type

  from s_consolidate_cls_src                    cp
  join Xxxx_Contract_Signed_Attr     lc on lc.code = 'LINKED_REWARD_CONTRACTS'
                                             and lc.collateral_id = cp.collateral_id
                                             and lc.value_num = 1
  join Xxxx_Contract_Signed_Attr    ct on ct.code = 'REWARD_CONSOLIDATION_TYPE'
                                             and ct.collateral_id = cp.collateral_id
  join Xxxx_Contract2                      c  on c.contract_id = cp.contract_id
  join Xxxx_Contract2                     cl on cl.contract_id = lc.key_num
  
   )    

--select * from s_ar_consolidations_q;
,

s_acts_all as (
    select /*+ materialize */
           decode(c.cons_type, null, d.contract_eid, c.contract_eid)    as contract_eid,
           decode(c.cons_type, null, d.contract_id,  c.contract_id)     as contract_id,
           d.contract_eid                                               as contract_eid_orig,
           d.contract_id                                                as contract_id_orig,
           -- 1 - Пропорционально обороту
           -- 2 - На главный договор
            nvl(c.cons_type,
                -- BALANCE-28989: проверяем тип консолидации для главного
                --                договора. записей может быть >1, так что
                --                берем максимальный (все равно, все одинаковые)
                nvl((select max(j.cons_type)
                       from s_ar_consolidations_q   j
                      where j.contract_id = d.contract_id), 0))         as cons_type,
           decode(c.cons_type, null, d.agency_id, c.agency_id)          as agency_id,
           decode(c.cons_type, null, d.contract_from_dt, c.contract_from_dt) as contract_from_dt,
           decode(c.cons_type, null, d.contract_till_dt, c.contract_till_dt) as contract_till_dt,
           decode(c.cons_type, null, d.commission_type, c.commission_type)   as commission_type,
           decode(d.discount_type,
                -- Справочник считаем как Директ, т.к. шкала по общему обороту
                12, 7,
                -- Вся медийка под номером 1 будет
                2, 1,
                3, 1,
                36, 1,
                37, 1,
                d.discount_type)                            as discount_type,
           d.discount_type                                  as discount_type_orig,
           q.from_dt, q.till_dt,
           d.brand_id,
           d.act_dt,
           d.from_dt   as month,
           d.amt_w_nds_rub,
           d.amt_rub
      from s_acts_src     d
      join s_quarters               q on d.from_dt between q.from_dt and q.till_dt
              and trunc((select dt from counting_date), 'MM') - 1 between q.from_dt and q.till_dt
      left outer
      join s_ar_consolidations_q c  on c.linked_contract_id = d.contract_id
     where not exists (
                select 1
                  from s_ar_excluded_clients  e
                 where e.contract_id = d.contract_id
                   and e.client_id = d.client_id
           )
        -- BALANCE-23037
       and d.contract_till_dt > add_months(trunc((select dt from counting_date), 'YYYY'), 2)
       and d.is_loyal = 0
       and d.discount_type in (1, 2, 3, 7, 12, 36, 37)
)
--select * from s_acts_all;


--select * from s_ar_consolidations_q;

-- обороты по-месячно по каждому договору
 -- (учитывая новую шкалу)

-- БОК + макс.оборот по медийке
, 

s_control as (
    select d.contract_id_orig, d.from_dt,
           max(ratio)               as max_ratio,
           sum(amt_rub_media)       as amt_rub_media
      from (
        select d.*,
               nvl(ratio_to_report(amt_rub)
                  over (partition by d.contract_id_orig, d.from_dt), 0) as ratio
          from (
            select d.contract_id_orig,
                   d.brand_id                                        as client_id,
                   d.from_dt,
                sum(case when d.discount_type_orig in (1, 2, 3, 36, 37)
                         then d.amt_rub
                         else null end)                              as amt_rub_media,
                   sum(decode(d.discount_type_orig, 7, d.amt_rub, null))  as amt_rub
              from s_acts_all                         d
             where trunc((select dt from counting_date), 'MM') - 1 between from_dt and till_dt
             group by d.from_dt,
                      d.contract_id_orig,
                      d.brand_id
               ) d
           ) d
     group by d.contract_id_orig, d.from_dt
)

,

s_ar_acts_q as (
select a.*,
       -- пишем в отдельную колонку, т.к. failed используется в
       -- прогнозах. а БОК в прогнозах не должен учитываться
       case
        when a.discount_type = 7
        and round(c.max_ratio, 2) >= 0.7 then 1
        else 0
       end                      as failed_bok,
       case
          when a.discount_type = 1
          and c.amt_rub_media >= 300000000 then 1
        else 0
       end                      as failed_media
  from s_acts_all                   a
  left outer
  join s_control                c on c.contract_id_orig = a.contract_id_orig
                                 and c.from_dt = a.from_dt
)

--select * from s_ar_acts_q;

, 

s_curr_contracts as (
    select d.contract_eid,        d.contract_id,        d.agency_id,
           d.contract_id_orig,
           d.contract_from_dt,    d.contract_till_dt,
           d.discount_type,
           d.discount_type_orig,
           d.from_dt, d.till_dt,
           d.failed_bok, d.failed_media,
           d.month,
           sum(d.amt_w_nds_rub)                             as amt_w_nds_rub,
           sum(d.amt_rub)                                   as amt
      from s_ar_acts_q                         d
      join s_quarters               q on d.from_dt between q.from_dt and q.till_dt
     where d.commission_type in (1, 8, 21)
     and trunc((select dt from counting_date), 'MM') - 1 between d.from_dt and d.till_dt
       group by d.contract_eid,       d.contract_id,      d.agency_id,
           d.contract_id_orig,
           d.contract_from_dt,      d.contract_till_dt,
           d.discount_type, d.discount_type_orig,
           d.from_dt, d.till_dt,
           d.failed_bok, d.failed_media,
           d.month
)
--select * from s_curr_contracts;
,
s_failed_contracts as (
    select *
      from s_curr_contracts
     where failed_bok = 1
        or failed_media = 1
)

--select * from s_failed_contracts;
,

-- статистика по агентству (не договору)
-- не используем v_opt_2015_acts, тк надо учесть оферты

 s_agency_stats_src as (
     select /*+ materialize */

           decode(c.cons_type, null, a.agency_id, c.agency_id)      as agency_id,
           decode(nvl(a.commission_type, a.discount_type),
                -- Справочник считаем как Директ, т.к. шкала по общему обороту
                12, 7,
                -- Вся медийка под номером 1 будет
                2, 1,
                3, 1,
                36, 1,
                37, 1,
                nvl(a.commission_type, a.discount_type))                                    as discount_type,
           q.from_dt,
           trunc(a.act_dt, 'MM')                                        as month,
           sum((a.amount - a.amount_nds - a.amount_nsp)*cr.rate)              as amt

      from xxxx_new_comm_contract_basic         a
        
      join s_quarters       q on a.act_dt between q.from_dt and q.till_dt
      left outer
      join s_ar_consolidations_q c  on c.linked_agency_id = a.agency_id
      join xxxx_currency_rate              cr on cr.cc = a.currency
                                              and cr.rate_dt = trunc(a.act_dt)               
      where 
       a.hidden < 4
       and nvl(a.commission_type, a.discount_type) in    (1, 2, 3, 7, 12, 36, 37)
          and (a.contract_id is null 
          or
               a.contract_id not in (
               select s.contract_id
                                     from s_failed_contracts s
                                     where s.contract_id = a.contract_id
                                       and s.discount_type_orig = nvl(
                                     a.commission_type, a.discount_type)
                                  )
                                  )
       and decode(c.cons_type, null, a.agency_id, c.agency_id)  in (select agency_id from s_curr_contracts)
       and a.is_loyal = 0
       and not exists (
                select 1
                  from s_ar_excluded_clients    e
                  
                 where e.contract_id = a.contract_id
                   and e.client_id = a.client_id
           )
      group by q.from_dt, trunc(a.act_dt, 'MM'),  decode(c.cons_type, null, a.agency_id, c.agency_id),
    decode(nvl(a.commission_type, a.discount_type),
                -- Справочник считаем как Директ, т.к. шкала по общему обороту
                12, 7,
                -- Вся медийка под номером 1 будет
                2, 1,
                3, 1,
                36, 1,
                37, 1,
                nvl(a.commission_type, a.discount_type))
     
)

--select  * from s_agency_stats_src  ;
, 

s_agency_stats_curr as (
    select d.*,
           -- нарастающий итог по аг-ву с начала квартала
           sum(amt) over (partition by agency_id, from_dt, discount_type
                               order by month)                      as amt_m,
           -- итог за квартал
           sum(amt) over (partition by agency_id, from_dt,
                                       discount_type)               as amt_q
      from s_agency_stats_src d
)

--select * from s_agency_stats_curr;
--select  * from s_agency_stats_src;
-- Прошлогодние обороты по аг-вам, по которым есть обороты в 2016 году
, 

s_agency_stats_prev as (
    select
    agency_id,
           discount_type,
           add_months(from_dt, 12)                                as from_dt,
           count(from_dt)                                         as month_cnt,
           sum(decode(from_dt,
                month, amt,
                0))                                               as amt_fm,
           sum(amt)                                               as amt
      from s_agency_stats_src s
     group by agency_id, add_months(from_dt, 12), discount_type
)
--select * from s_agency_stats_prev;
--select * from s_curr_contracts;
,

s_opt_2015_base_q as (  
   select 
   -- если косолидация есть для договора, то надо весь оборот
       -- перенести на главный договор
       d.contract_eid,
       d.contract_id,
       d.contract_id_orig,
       d.agency_id,
       d.contract_from_dt,    d.contract_till_dt,d.discount_type,
       d.from_dt, d.till_dt,
       d.month                  as till_dt_fc,
-- За месяц, разбитый по типам рекламы
       d.amt_w_nds_rub          as amt_w_nds_rub,
       d.amt                    as amt,
       d.amt                    as amt_for_forecast,
       -- За месяц (нарастающий итог)
       cs.amt_m                 as amt_ag,
-- За квартал
       cs.amt_q                 as amt_ag_q,
       nvl(ps.amt,0)                   as amt_ag_prev,
       nvl(ps.amt_fm,0)                as amt_ag_prev_fm,
       case
       -- BALANCE-28048: для спецпремий не надо фейлить
        when nvl(ps.amt, 0) = 0 and d.discount_type = 1 then 0
        when nvl(ps.amt_fm, 0) = 0 then 1
        when ps.month_cnt != 3 then 1
        else 0
       end                      as failed,
       d.failed_bok,
       d.failed_media,
       -- пишем в отдельную колонку, т.к. failed используется в
       -- прогнозах. а БОК в прогнозах не должен учитываться
     
       0                        as excluded

  from s_curr_contracts      d
  join s_agency_stats_curr   cs on cs.agency_id = d.agency_id
                               and cs.from_dt = d.from_dt
                               and cs.month = d.month
                               and cs.discount_type = d.discount_type
  left outer
  join s_agency_stats_prev   ps on ps.agency_id = d.agency_id
                               and ps.from_dt = d.from_dt
                               and ps.discount_type = d.discount_type

)
--select * from s_opt_2015_base_q;

, 

s_q_by_month as (
    -- Т.к. суммы в исходных строках с разным уровнем гранулированности,
    -- то приводим сначала их все к уровню месяца.
    -- Исходные данные:
    --      тек. оборот по договору — помесячный по сервису
    --      тек. оборот по аг-ву    — поквартальный
    --      прошлый оборот по аг-ву — поквартальный
    select 
    /*+ parallel(16)
               opt_param('optimizer_index_caching' 0)
               opt_param('optimizer_index_cost_adj' 1000)
           */
    contract_eid, contract_id, from_dt, till_dt, agency_id,
           till_dt_fc, amt_ag_prev, amt_ag_prev_fm, discount_type,
           amt_ag_q                     as amt_ag,
           -- тек. оборот по договору — поднимаем до месяца
           sum(amt)                     as amt_rub,
           sum(amt_w_nds_rub)           as amt_w_nds_rub
      from s_opt_2015_base_q
     where failed = 0
       and excluded = 0
       and failed_bok = 0
       and failed_media = 0
       and trunc((select dt from counting_date), 'MM') - 1 between from_dt and till_dt
     group by contract_eid, contract_id, from_dt, till_dt, agency_id, 
              till_dt_fc, amt_ag_q, amt_ag_prev, amt_ag_prev_fm, discount_type
)
--select * from s_q_by_month;

, 

s_q_growth as (
    select /*+ OPTIMIZER_FEATURES_ENABLE('11.2.0.1') */
            d.*,
               bo.pk_comm.calc_base_q(
                    amt_rub,
--                    29700.4,
                    amt_ag,
                    amt_ag_prev,
--                    49200,
                    amt_ag_prev_fm,
                    discount_type
             )                             as reward
      from (
            -- Теперь обороты поднимаем до уровня квартала
        select 
        contract_eid, contract_id, from_dt, till_dt, agency_id,
               discount_type, 
               -- прошлый оборот по аг-ву — уже за квартал, ничего не делаем
               amt_ag_prev,
               amt_ag_prev_fm,
               -- тек. оборот по договору — поднимаем до квартала
               sum(amt_rub)                 as amt_rub,
               sum(amt_w_nds_rub)           as amt_w_nds_rub,
               amt_ag
          from s_q_by_month
         group by contract_eid, contract_id, from_dt, till_dt, agency_id,
                  discount_type, 
                  amt_ag_prev, amt_ag_prev_fm, amt_ag
          ) d
)

--select    * from s_q_growth;

, 

s_q_growth_not_consdted as (
    select d.contract_id, d.contract_eid,
           d.main_contract_id,
           d.discount_type,
           d.from_dt,
           sum(d.amt)                       as amt_rub,
           sum(d.amt_w_nds_rub)             as amt_w_nds_rub
      from (
        select d.contract_id_orig                       as contract_id,
               d.contract_eid_orig                      as contract_eid,
               d.contract_id                            as main_contract_id,
               d.from_dt,
               d.discount_type,
               d.act_dt,
               d.amt_rub                                as amt,
               d.amt_w_nds_rub  
     -- BALANCE-28455: смотрим все обороты, т.к. шкалы могут быть разными
          from s_ar_acts_q         d
            -- Шкала с учетом консолидации
         where d.commission_type in (1, 8, 21)
            -- Пропорционально обороту
           and d.cons_type = 1
            -- только те, кто не сфейлился
           and d.failed_bok = 0
           and d.failed_media = 0
            ) d
     group by d.contract_id, d.contract_eid, d.main_contract_id, d.discount_type, d.from_dt
)

--select * from s_q_growth_not_consdted;
, 

s_q_growth_proply_consolidated as (
    select 
    d.contract_eid, d.contract_id,
           r.from_dt, r.till_dt, d.discount_type,
           d.amt_rub, d.amt_w_nds_rub,
           -- пропорционально раскладываем премию
           r.reward*d.amt_rub/r.amt_rub     as reward
      from s_q_growth_not_consdted              d
      join s_q_growth                           r on r.contract_id = d.main_contract_id
                                                 and r.from_dt = d.from_dt
                                                 and r.discount_type = d.discount_type
)
--select  *from s_q_growth_proply_consolidated;
,


s_q as (
        select 
        contract_eid,
        contract_id, from_dt, till_dt,
               discount_type,
               sum(amt_rub)         as amt_rub,
               sum(amt_w_nds_rub)   as amt_w_nds_rub,
               sum(reward)          as reward
          from (
                -- Тут только:
                --  - неконсолидированные договоры
                --  - консолидированные договоры с консолидацией на главный договор
            select 
            contract_eid, 
            contract_id, from_dt, till_dt,
                   discount_type,
                   amt_rub, amt_w_nds_rub, reward
              from s_q_growth
             where contract_id not in (
                    select contract_id
                      from s_ar_consolidations_q
                     where cons_type = 1)
             union all
                -- пропорционально консолидированные
            select 
            contract_eid, 
            contract_id, from_dt, till_dt,
                   discount_type,
                   amt_rub, amt_w_nds_rub, reward
              from s_q_growth_proply_consolidated
--             union all
--            select 
--            contract_eid,
--            contract_id, from_dt, till_dt,
--                   1       as discount_type,
--                   amt_rub, amt_w_nds_rub, reward
--              from s_q_audio
               )
        group by 
        contract_eid, 
        contract_id, from_dt, till_dt, discount_type
)

--select * from s_q;
,

--select  *from s_q;
s_opt_2015_base as (
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
               'RUR'                            as currency,
               1                                as nds,
               amt_rub                          as turnover_to_charge,
               reward                           as reward_to_charge,
               amt_rub                          as turnover_to_pay,
               amt_w_nds_rub                    as turnover_to_pay_w_nds,
               reward                           as reward_to_pay,
               reward                           as reward_to_pay_src,
               20                               as reward_type
          from s_q
            -- BALANCE-24627: квартальные премии считаем не всегда
--         where to_char(sysdate, 'MM') in ('03', '06', '09', '12')
 )        
       )

--select * from s_opt_2015_base;       
       
  , 
  
 s_dates as (
    select /*+ materialize */
           level                                        as lvl,
           add_months(trunc((select  dt from counting_date), 'mm'), -level)     as dt
      from dual
   connect by level <= bo.pk_comm.get_months_for_calc
)      

       
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
              from s_opt_2015_base
            
           )
     group by contract_id,     contract_eid,
              from_dt,         till_dt,
              nds,             currency,
              discount_type,   reward_type
)            s
     -- BALANCE-17502: показываем только те интервалы,
     --                которые включают прошлый месяц
     -- BALANCE-19491: или N прошлых периодов, если задано
join s_dates d on d.dt between s.from_dt and s.till_dt       
order by contract_id, from_dt, discount_type, currency, nds;
       
