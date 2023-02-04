CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."XXX_OPT_2015_INVOICES" ("CONTRACT_EID", "CONTRACT_ID", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "INVOICE_EID", "INVOICE_ID", "INVOICE_DT", "INVOICE_TYPE", "PAYMENT_CONTROL_TYPE", "TOTAL_SUM", "CURRENCY", "NDS", "NDS_PCT", "LOYAL_CLIENTS", "DISCOUNT_TYPE", "DISCOUNT_TYPE_SRC", "PAYMENT_TYPE", "IS_AGENCY_CREDIT_LINE", "COMMISSION_TYPE") AS 

     with
counting_date as (select date'2019-07-03' as dt from dual),


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
s_attrs_src as (
    select value_num, value_str,
           d.contract_id                                         as contract_id,
           d.code,
           d.key_num                                              as client_id,
           c.from_dt,
           c.till_dt
      from xxxx_contract_signed_attr d
      join s_cltrl                          c on c.collateral_id = d.collateral_id
                                             and c.contract_id = d.contract_id
                                             and c.code = d.code
)
--select * from s_attrs_src;

, 

s_attrs as (
     select *
       from s_attrs_src
         -- BALANCE-25145: смотрим значение атрибута на конец пред.месяца
      where trunc((select dt from counting_date), 'MM') - 1 between from_dt and till_dt
)

   
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

           case
           when c.invoice_dt >= date '2019-03-01' then
               nvl(
         -- Показываем КОС на конец прошлого периода,
         -- Чтобы в одном периоде по одному договору было одно и то же
         -- значения атрибута во всех счетах
           (select distinct a.value_num
            from s_attrs a
             where a.code = 'AR_PAYMENT_CONTROL_TYPE'
             and a.contract_id = c.contract_id), 0)
             else 0
           end                                      as payment_control_type,
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
                                                -- BALANCE-17175
                                                (
                                                     -- только базовые/профы
                                                    c.contract_commission_type in (1, 2, 8, 21, 23) and
                                                    -- то учитываем еще и код 22
                                                    nvl(c.commission_type,
                                                        c.discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 17, 19, 22, 25, 28, 29, 36, 37, 40, 42)
                                                )
                                                   or
                                                (
                                                    -- Беларусия
                                                    c.contract_commission_type in (20) and
                                                    nvl(c.commission_type, c.discount_type) in (7, 37, 1, 2, 3, 4, 24, 36, 12)
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
;

CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."XXX_COMM_2013_BASE_SRC" (
    "CONTRACT_EID",
    "CONTRACT_ID",
    "CONTRACT_FROM_DT",
    "CONTRACT_TILL_DT",
    "INVOICE_EID",
    "INVOICE_ID",
    "INVOICE_DT",
    "INVOICE_TYPE",
    "CURRENCY",
    "NDS",
    "NDS_PCT",
    "TOTAL_SUM",
    "LOYAL_CLIENTS",
    "IS_OPT",
    "IS_THERE_OLD_ACTS",
    "DISCOUNT_TYPE",
    "DISCOUNT_TYPE_SRC",
    "PAYMENT_TYPE",
    "IS_AGENCY_CREDIT_LINE",
    "PAYMENT_CONTROL_TYPE",
    "COMMISSION_TYPE"
) AS
    SELECT DISTINCT
        xxxx.contract_eid       AS contract_eid,
        xxxx.contract_id        AS contract_id,
        xxxx.contract_from_dt   AS contract_from_dt,
        xxxx.contract_till_dt   AS contract_till_dt,
        xxxx.invoice_eid        AS invoice_eid,
        xxxx.invoice_id         AS invoice_id,
        xxxx.invoice_dt         AS invoice_dt,
        xxxx.invoice_type       AS invoice_type,
        xxxx.currency           AS currency,
        xxxx.nds                AS nds,
        xxxx.nds_pct            AS nds_pct,
        xxxx.total_sum          AS total_sum,
        xxxx.loyal_clients      AS loyal_clients,
        CASE
            WHEN xxxx.paysys_id IN (
                1025,
                1026,
                1027
            ) THEN 0
            WHEN ( DECODE(xxxx.endbuyer_id, 0, NULL, xxxx.endbuyer_id) ) IS NULL
                 OR xxxx.endbuyer_inn = xxxx.agency_inn THEN 1
            ELSE 0
        END AS is_opt,
         CASE
            WHEN (
                SELECT
                    COUNT(1)
                FROM
                    xxxx_new_comm_contract_basic b
                WHERE
                    xxxx.invoice_id = b.invoice_id
                    AND b.hidden < 4
                    AND b.act_dt < DATE '2015-01-01'
                    AND xxxx.invoice_dt < DATE '2015-01-01'
            ) > 0 THEN 1
            ELSE 0
        END AS is_there_old_acts,
        xxxx.discount_type      AS discount_type,
        xxxx.discount_type      AS discount_type_src,
        xxxx.payment_type       AS payment_type,
        0 AS is_agency_credit_line,
        0 as PAYMENT_CONTROL_TYPE,
        xxxx.commission_type    AS commission_type
       
    FROM
        (
            SELECT DISTINCT
                x.contract_eid               AS contract_eid,
                x.contract_id                AS contract_id,
                x.contract_from_dt           AS contract_from_dt,
                x.contract_till_dt           AS contract_till_dt,
                x.invoice_eid                AS invoice_eid,
                x.invoice_id                 AS invoice_id,
                x.invoice_dt                 AS invoice_dt,
--           x.act_dt                                   as act_dt,  
                x.currency                   AS currency,
                x.nds                        AS nds,
                x.nds_pct                    AS nds_pct,
                nvl(x.commission_type, x.discount_type) AS discount_type,
                x.payment_type               AS payment_type,
                x.contract_commission_type   AS commission_type,
                x.loyal_client               AS loyal_clients,
                x.endbuyer_id                AS endbuyer_id,
                x.endbuyer_inn               AS endbuyer_inn,
                x.agency_inn                 AS agency_inn,
                x.paysys_id                  AS paysys_id,
                i.invoice_type               AS invoice_type,
                i.total_sum                  AS total_sum
            FROM
                xxxx_new_comm_contract_basic x
                LEFT JOIN xxxx_invoice i ON x.invoice_id = i.inv_id
        ) xxxx
    WHERE
        xxxx.discount_type IN (
            1,
            2,
            3,
            7,
            11,
            12,
            13,
            14,
            19,
            27,
            36,
            37,
            42
        );
 
CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."XXX_AR_EXCLUDED_CLIENTS" ("CONTRACT_ID", "CLIENT_ID", "FROM_DT") AS 
  with

counting_date as (select date'2019-07-03' as dt from dual)
  --select * from counting_date;
  ,
  
s_excluded_clients_pre as (
    select contract_id,
           key_num                                             as client_id,
           start_dt                                            as from_dt
      from xxxx_Contract_Signed_Attr
     where code = 'EXCLUDE_CLIENTS'
        -- дата начала действия ДС — не позднее последнего дня прошного месяца
        -- (можно было бы брать только прошлый квартал, но нам надо учесть
        -- ситуацию, когда допник просто подолжает све дествие в след.квартале)
       and start_dt < trunc((select dt from counting_date), 'MM')
        -- только включенные аг-ва (если тут 0/null, то не должны исключать)
       and value_num = 1
)


select d."CONTRACT_ID",d."CLIENT_ID",d."FROM_DT"
  from s_excluded_clients_pre      d
  join (
        select contract_id, max(from_dt) as from_dt
          from s_excluded_clients_pre
         group by contract_id
       )                            l on l.contract_id = d.contract_id
                                     and l.from_dt = d.from_dt;     

CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."XXX_AR_CONSOLIDATIONS_Q" ("CONTRACT_ID", "CONTRACT_EID", "AGENCY_ID", "COMMISSION_TYPE", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "START_DT", "FINISH_DT", "LINKED_CONTRACT_ID", "LINKED_AGENCY_ID", "CONS_TYPE") AS 

with

counting_date as (select date'2019-07-03' as dt from dual)
  --select * from counting_date;
,
  
s_quarters AS (
    select d.dt from_dt, add_months(d.dt, 3) - 1/24/60/60 as till_dt
      from (
         select add_months(date'2018-03-01', 3*(level-1)) as dt
           from dual
        connect by level <= 20
           ) d
),

s_consolidate_cls_src as (
        select 
             /*+ materialize */
              cp.contract_id,
              cp.collateral_id,
              cp.start_dt           as start_dt,
              fd.value_dt   as finish_dt
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
           join s_quarters                           q on q.till_dt between cp.start_dt and trunc(to_date(fd.value_dt)) + 1 - 1/24/60/60
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
      from xxxx_Contract_Signed_Attr
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

select 
  cp.contract_id,
       -- сразу получаем для главного договора:
       -- аг-во, тип премии, чтобы удобнее делать подмену
       c.contract_eid    as contract_eid,
       c.client_id      as agency_id,
       c.commission_type     as commission_type,
       c.contract_from_dt    as contract_from_dt,
       c.contract_till_dt    as contract_till_dt,
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
  join Xxxx_Contract2                     cl on cl.contract_id = lc.key_num; 

CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."XXX_AR_ACTS_Q" ("CONTRACT_EID", "CONTRACT_ID", "CONTRACT_EID_ORIG", "CONTRACT_ID_ORIG", "AGENCY_ID", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "COMMISSION_TYPE", "DISCOUNT_TYPE", "DISCOUNT_TYPE_ORIG", "FROM_DT", "TILL_DT", "BRAND_ID", "ACT_DT", "MONTH", "AMT_W_NDS_RUB", "AMT_RUB", "FAILED_BOK", "FAILED_MEDIA") AS 
  with
     
  counting_date as (select date'2019-07-03' as dt from dual)
  --select * from counting_date;
  ,
  
s_quarters AS (
    select d.dt from_dt, add_months(d.dt, 3) - 1/24/60/60 as till_dt
      from (
         select add_months(date'2016-03-01', 3*(level-1)) as dt
           from dual
        connect by level <= 20
           ) d
        -- прошлый квартал
     where trunc((select dt from counting_date), 'MM') - 1 between d.dt and add_months(d.dt, 3)
        -- такой же квартал год назад
        or add_months(trunc((select dt from counting_date), 'MM') - 1, -12) between d.dt
                                                         and add_months(d.dt, 3)
)
, s_excluded_clients as (
    select /*+ materialize */ *
      from xxx_ar_excluded_clients  e
)
-- BALANCE-28313:
-- Делаем подмену основных атрибутов договора:
--      - id/eid
--      - agency_id
--      - contract_from_dt/contract_till_dt
--      - commission_type
-- + исключаем клиентов
-- BALANCE-28319: пока не фильтруем по шкале, чтобы учесть подмену
, s_acts as (
    select /*+ materialize */
 -- Консолидируем только договоры с типом консолидации "на главный"
           -- те, которые "пропорционально" показываем отдельно, чтобы знать
           -- оборот, к котором потом применим % в завис-ти от прироста по аг-ву
           -- (аг-во консолидируем всегда)
           decode(c.cons_type, 2, c.contract_eid, d.contract_eid)           as contract_eid,
           decode(c.cons_type, 2, c.contract_id,  d.contract_id)            as contract_id,
           d.contract_eid                                                   as contract_eid_orig,
           d.contract_id                                                    as contract_id_orig,
           d.contract_from_dt as contract_from_dt,
           d.contract_till_dt as contract_till_dt,
               -- аг-во консолидируем всегда, чтобы потом найти прирост
           -- по главному агенству
           decode(c.cons_type, null, d.agency_id, c.agency_id)              as agency_id,
           decode(c.cons_type, null, d.commission_type, c.commission_type)  as commission_type,
           decode(d.discount_type,
                -- Справочник считаем как Директ, т.к. шкала по общему обороту
--                12, 7,  -- BALANCE-30127
                -- Вся медийка под номером 1 будет
                2, 1,
                3, 1,
--                36, 1,
                37, 1,
                d.discount_type)                            as discount_type,
           d.discount_type                                  as discount_type_orig,
           q.from_dt, q.till_dt,
           d.brand_id,
           d.act_dt,
           d.from_dt                                        as month,
           d.amt_w_nds_rub,
           d.amt_rub
      from xxx_opt_2015_acts     d
      join s_quarters               q on d.from_dt between q.from_dt and q.till_dt
                                     and trunc((select dt from counting_date), 'MM') - 1 between q.from_dt and q.till_dt
      left outer
      join xxx_ar_consolidations_q c  on c.linked_contract_id = d.contract_id
     where not exists (
                select 1
                  from s_excluded_clients  e
                 where e.contract_id = d.contract_id
                   and e.client_id = d.client_id
           )
        -- BALANCE-23037
        --and d.contract_till_dt > add_months(trunc((select dt from counting_date), 'YYYY'), 2)
        -- PAYSUP-456911
       and d.contract_till_dt > date'2019-03-01'
       and d.is_loyal = 0
       and d.discount_type in (1, 2, 3, 7, 12, 36, 37, 40, 42)
)
-- БОК + макс.оборот по медийке
, s_control as (
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
              from s_acts                         d
             where trunc((select dt from counting_date), 'MM') - 1 between from_dt and till_dt
             group by d.from_dt,
                      d.contract_id_orig,
                      d.brand_id
               ) d
           ) d
     group by d.contract_id_orig, d.from_dt
)
select a."CONTRACT_EID",a."CONTRACT_ID",a."CONTRACT_EID_ORIG",a."CONTRACT_ID_ORIG",a."AGENCY_ID",a."CONTRACT_FROM_DT",a."CONTRACT_TILL_DT",a."COMMISSION_TYPE",a."DISCOUNT_TYPE",a."DISCOUNT_TYPE_ORIG",a."FROM_DT",a."TILL_DT",a."BRAND_ID",a."ACT_DT",a."MONTH",a."AMT_W_NDS_RUB",a."AMT_RUB",
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
  from s_acts                   a
  left outer
  join s_control                c on c.contract_id_orig = a.contract_id_orig
                                 and c.from_dt = a.from_dt;                               
                                 
CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."XXX_AR_AGENCIES_STATS" ("AGENCY_ID", "SERVICE_ID", "SERVICE_ORDER_ID", "CLIENT_ID", "MONTH", "AMT") AS 
  with
  counting_date as (select date'2019-07-03' as dt from dual)
  --select * from counting_date;
,

s_dates as (
    select add_months(trunc((select dt from counting_date), 'MM'), -1)     as from_dt,
           trunc((select dt from counting_date), 'MM') - 1/24/60/60        as till_dt
      from dual
)
select  /*+ parallel(8) */
       a.client_id                                              as agency_id,
       o.service_id,
       o.service_order_id,
       o.client_id,
       trunc(a.dt, 'MM')                                        as month,
       sum((at.amount - at.amount_nds - at.amount_nsp)*cr.rate) as amt

      from xxxx_acts a 
      join xxxx_invoice  i on i.inv_id = a.invoice_id
      join xxxx_act_trans                   at on at.act_id = a.act_id
      join xxxx_order                       o  on o.order_id = at.parent_order_id                                       

      join xxxx_currency_rate              cr on cr.cc = i.currency
                                              and cr.rate_dt = trunc(a.dt)
     join s_dates          d on a.dt between d.from_dt and d.till_dt
  where a.hidden < 4
   and nvl(at.commission_type,
      nvl(i.commission_type, i.discount_type)) in (7)
   and a.client_id in (select agency_id from xxx_opt_2015_acts
                        where from_dt = d.from_dt)
 group by a.client_id, trunc(a.dt, 'MM'),
          o.service_id, o.service_order_id,
          o.client_id;

CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."XXX_OPT_2015_ACTS" ("CONTRACT_EID", "CONTRACT_ID", "INVOICE_EID", "INVOICE_ID", "INVOICE_DT", "INVOICE_TYPE", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "CURRENCY", "NDS", "PAYMENT_TYPE", "COMMISSION_TYPE", "PAYMENT_CONTROL_TYPE", "DISCOUNT_TYPE", "DISCOUNT_TYPE_SRC", "IS_AGENCY_CREDIT_LINE", "IS_LOYAL", "CLIENT_ID", "ORDER_ID", "SERVICE_ID", "SERVICE_ORDER_ID", "AGENCY_ID", "ACT_ID", "ACT_EID", "ACT_DT", "FROM_DT", "TILL_DT", "AMT_W_NDS", "AMT", "AMT_W_NDS_RUB", "AMT_RUB", "BRAND_ID", "NDS_COUNT", "CURRENCY_COUNT") AS 
  with
counting_date as (select date'2019-07-03' as dt from dual),

s_brands as (
        select /*+ materialize */
               atr.key_num          as client_id,
               atr.start_dt         as collateral_dt,
               c.start_dt           as dt,
               trunc(nvl(c.finish_dt, date'2019-07-03'))
                -- BALANCE-28403: добавляем границу дня с секундами
                + 1 - 1/24/60/60            as finish_dt,
               min(c.MAIN_CLIENT_ID)     as main_client_id
          from xxxx_contract_signed_attr    atr
          -- BALANCE-27379: клиента в тех.связке начинаем учитывать, когда
            -- начинает действовать допник, в кот.он был добавлен
          join xxxx_ui_contract_apex        c on c.contract_id = atr.contract_id
                                          

         where atr.code = 'BRAND_CLIENTS'
         group by atr.key_num, atr.start_dt, c.start_dt, nvl(c.finish_dt, date'2019-07-03')
)

--select  *from s_brands;
--select * from s_payments;
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
           b.payment_control_type,
		   -- BALANCE-26651,  BALANCE-17175
           nvl(         
             case

            when at.commission_type in (22, 29) then 1

            when b.commission_type in (10, 16) then 97
            else at.commission_type
           end,
           case
                when b.commission_type in (10, 16) then 97
                else b.discount_type
           end)                                         as discount_type,

       nvl(at.commission_type, b.discount_type_src)     as discount_type_src,


           b.is_agency_credit_line,
           case
           when nvl(a.is_loyal, 0) = 1 and b.discount_type = 7
            then 1
            else 0
           end                                               as is_loyal,
           o.client_id                                      as client_id,
           o.order_id, 
           o.service_id, 
           o.service_order_id,
           a.client_id                                     as agency_id,
         --  nvl(brand.main_client_id, xxx.client_id)             as brand_id,
           a.act_id                                             as act_id,
           a.act_eid                                            as act_eid,
           a.dt                                         as act_dt,
           trunc(a.dt, 'MM')                            as from_dt,
           add_months(trunc(a.dt, 'MM'), 1) - 1/84600   as till_dt,
		   at.amount                                         as amt_w_nds,
           at.amount-at.amount_nds-at.amount_nsp           as amt,
           at.amount*cr.rate                                 as amt_w_nds_rub,
           (at.amount-at.amount_nds-at.amount_nsp)*cr.rate as amt_rub
      from XXX_OPT_2015_INVOICES        b
      join xxxx_acts     a on b.invoice_id = a.invoice_id
                                              and a.hidden <4
                                              and a.dt >= date'2015-03-01'
                                                and ( a.is_loyal = 0 and  a.dt >= date'2017-03-01'
                                                or a.dt < date'2017-03-01')
    
      join xxxx_act_trans                   at on at.act_id = a.act_id
      join xxxx_order                       o  on o.order_id = at.parent_order_id                    


      join xxxx_currency_rate              cr on cr.cc = b.currency
                                              and cr.rate_dt = trunc(a.dt)
      left outer
      join s_brands                               brand on brand.client_id = a.client_id
     where b.commission_type in (1, 2, 3, 4, 5, 6, 8, 10, 11, 12, 13, 16, 17, 19, 20, 21, 23)
   and (
          -- base, prof
          (
            b.commission_type in (1, 2, 8, 21, 23)
        and (

                a.dt >= date'2019-03-01' and b.contract_till_dt > date'2019-03-01' and b.discount_type in (1, 2, 3, 7, 12, 36, 37, 38, 40, 42)
                -- BALANCE-24516: новые условия применимы только актов нового
                -- фин.года при условиия, что договор продлен
                 or a.dt >= date'2017-03-01' and b.contract_till_dt > date'2017-03-01' and b.discount_type in (1, 2, 3, 7, 12, 36, 37, 38)
                -- BALANCE-22085
                -- В актах по новым и продленным договорам 2016:
                --  * отбрасываем маркет (11)
                --  * включаем Авто (25)
                --2016 год
-- BALANCE-24734: добавляем 36 тип, чтобы старые счета учесть
            or  a.dt >= date'2017-03-01' and b.contract_till_dt  <= date'2017-03-01' and b.discount_type in (1, 2, 3, 7, 12, 25, 36)
             or  a.dt >= date'2016-03-01' and a.dt < date'2017-03-01'   and b.discount_type in (1, 2, 3, 7, 12, 25)
             -- 2015 год
             or  a.dt >= date'2016-03-01' and b.contract_till_dt  <= date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
             or  a.dt  < date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
            )
          )
          -- ua
       or (
             b.commission_type = 6 
  and (
                   -- BALANCE-25339
                   a.dt >= date'2017-04-01'
               and b.discount_type in (0, 1, 7, 8, 12, 36)
                   -- BALANCE-22914
                or   a.dt >= date'2016-04-01' and a.dt < date'2017-04-01'
               and b.discount_type in (0, 1, 8, 7, 15)
                or  a.dt <  date'2016-04-01'
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
          -- belarus, BALANCE-29835
       or (b.commission_type = 20 and b.discount_type in (1, 2, 3, 4, 24, 36, 12, 7, 37))
       )

      union all 
   select b.contract_eid,
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
           0                                        as  payment_control_type,
           b.discount_type,
           b.discount_type                           as discount_type_src,
           0       as    is_agency_credit_line,
           case
           when nvl(a.is_loyal, 0) = 1 and b.discount_type = 7
            then 1
            else 0
           end                                               as is_loyal,
           o.client_id                                      as client_id,
           o.order_id, 
           o.service_id, 
           o.service_order_id,
           a.client_id                                     as agency_id,
           a.act_id                                             as act_id,
           a.act_eid                                            as act_eid,
           a.dt                                                 as act_dt,
           trunc(a.dt, 'MM')                            as from_dt,
           add_months(trunc(a.dt, 'MM'), 1) - 1/84600   as till_dt,
		   at.amount                                         as amt_w_nds,
           at.amount-at.amount_nds-at.amount_nsp           as amt,
           at.amount*cr.rate                                 as amt_w_nds_rub,
           (at.amount-at.amount_nds-at.amount_nsp)*cr.rate as amt_rub
       from XXX_COMM_2013_BASE_SRC                       b
       join xxxx_acts     a on b.invoice_id = a.invoice_id
                                              and a.hidden <4
                                              and a.dt >= date'2019-04-01'
      join xxxx_act_trans                   at on at.act_id = a.act_id
      join xxxx_order                       o  on o.order_id = at.parent_order_id                    
      join xxxx_currency_rate              cr on cr.cc = b.currency
                                              and cr.rate_dt = trunc(a.dt)

      where b.commission_type = 60
       and b.discount_type in (7, 12, 13, 27, 37, 42)
)
--select * from s_base;
--select  *from s_ar_acts_src;
,
s_ar_acts as (
     select  s.*,
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
--select  *from s_acts_temp;
,

s_acts_src as ( select * from s_acts_temp
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
            -- BALANCE-27062: belarus added
        and currency in ('RUR', 'BYN', 'BYR', 'KZT')

)
--select *from s_acts_src;

select
"CONTRACT_EID", "CONTRACT_ID", "INVOICE_EID", "INVOICE_ID", "INVOICE_DT", "INVOICE_TYPE", "CONTRACT_FROM_DT", 
"CONTRACT_TILL_DT", "CURRENCY", "NDS", "PAYMENT_TYPE", "COMMISSION_TYPE","PAYMENT_CONTROL_TYPE", "DISCOUNT_TYPE", 
"DISCOUNT_TYPE_SRC", "IS_AGENCY_CREDIT_LINE", "IS_LOYAL", "CLIENT_ID",
"ORDER_ID", 
"SERVICE_ID",
"SERVICE_ORDER_ID", 
"AGENCY_ID", 
"ACT_ID", "ACT_EID", "ACT_DT", "FROM_DT", "TILL_DT", "AMT_W_NDS", "AMT", "AMT_W_NDS_RUB", "AMT_RUB", "BRAND_ID",
"NDS_COUNT", "CURRENCY_COUNT"
from s_acts_src;

CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."XXX_OPT_2015_PAYMENTS" ("CONTRACT_EID", "CONTRACT_ID", "INVOICE_EID", "INVOICE_ID", "INVOICE_DT", "INVOICE_TYPE", "PAYMENT_CONTROL_TYPE", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "CURRENCY", "NDS", "PAYMENT_TYPE", "COMMISSION_TYPE", "DISCOUNT_TYPE", "DISCOUNT_TYPE_SRC", "IS_AGENCY_CREDIT_LINE", "INVOICE_TOTAL_SUM", "INVOICE_TOTAL_SUM_W_NDS", "DOC_DATE", "OEBS_DT", "PAYMENT_NUMBER", "FROM_DT", "TILL_DT", "AMT", "AMT_W_NDS", "AMT_BY_INVOICE", "PAYMENTS_CURR_BY_INVOICE_W_NDS", "IS_FULLY_PAID_PRE", "IS_EARLY_PAYMENT_PRE", "IS_2018", "FULLY_PAID_POS", "EARLY_PAYMENT_POS", "AMT_TTL_W_NDS", "AMT_TTL", "PAYMENT_COUNT_BY_CONTRACT", "PAYMENT_COUNT_BY_INVOICE", "IS_FULLY_PAID", "IS_EARLY_PAYMENT", "IS_EARLY_PAYMENT_TRUE") AS 
  with
counting_date as (select date'2019-07-03' as dt from dual),


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

-- История действия подписанных атрибутов
s_attrs_src as (
    select value_num, value_str,
           d.contract_id                                         as contract_id,
           d.code,
           d.key_num                                              as client_id,
           c.from_dt,
           c.till_dt
      from xxxx_contract_signed_attr d
      join s_cltrl                          c on c.collateral_id = d.collateral_id
                                             and c.contract_id = d.contract_id
                                             and c.code = d.code
)

,
s_attrs as (
     select *
       from s_attrs_src
         -- BALANCE-25145: смотрим значение атрибута на конец пред.месяца
      where trunc((select dt from counting_date), 'MM') - 1 between from_dt and till_dt
)
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

--select  *from s_changes_payment_type;
,
s_payments_temp as (
    select b.contract_eid,
           b.contract_id,
           b.invoice_eid,
           b.invoice_id,
           b.invoice_dt,
           b.invoice_type,
           b.payment_control_type,
           b.contract_from_dt,
           b.contract_till_dt,
           b.currency,
           b.nds,
           b.payment_type,
           b.commission_type,
           b.discount_type,
           b.discount_type_src,
           b.is_agency_credit_line,
           b.total_sum*100/(100 + b.nds*b.nds_pct)      as invoice_total_sum,
           b.total_sum                                  as invoice_total_sum_w_nds,
           oebs.comiss_date,
           oebs.dt                                      as oebs_dt,
           oebs.payment_number,                          
           trunc(oebs.comiss_date, 'MM')                 as from_dt,
           add_months(
            trunc(oebs.comiss_date, 'MM'), 1) -1/84600    as till_dt,
           oebs.oebs_payment*100/(100 + b.nds*b.nds_pct) as amt,
           oebs.oebs_payment                             as amt_w_nds,
       -- Сумма платежей нарастающим итогом по счету
       -- в хронологическом порядке
-- (используется для расчета премии в других представлениях)
           sum(oebs.oebs_payment*100/(100 + b.nds*b.nds_pct))
            over(partition by b.invoice_id
                     order by oebs.comiss_date,oebs.dt,oebs.payment_number)        as amt_by_invoice,
   -- (используется для определения ранней оплаты, только внутри данного
         -- представления)            
            sum(oebs.oebs_payment)
            over(partition by b.invoice_id
                     order by oebs.comiss_date, oebs.dt,oebs.payment_number)       as  payments_curr_by_invoice_w_nds
      from XXX_OPT_2015_INVOICES        b
      join xxxx_oebs_cash_payment_test     oebs on oebs.invoice_id = b.invoice_id
                                                and oebs.comiss_date >= date'2015-03-01'
                                                and oebs.comiss_date is not null
   where 
         -- base, prof
          (
            b.commission_type in (1, 2, 8, 21, 23)
        and (
              -- BALANCE-30851: Добавить Дзен во вьюшки
                b.invoice_dt >= date'2019-03-01'        and
                b.discount_type in (1, 2, 3, 7, 12, 36, 37, 40) and
                b.currency = 'RUR'
             or
               -- BALANCE-24516
                b.invoice_dt >= date'2017-03-01'        and
                b.discount_type in (1, 2, 3, 7, 12, 36, 37)     and
                b.currency = 'RUR'
             or
              -- BALANCE-22085
                b.invoice_dt >= date'2016-03-01'        and
                b.invoice_dt  < date'2017-03-01'        and
                -- BALANCE-24734: добавляем 36 тип, чтобы старые счета учесть
                b.discount_type in (1, 2, 3, 7, 12, 25, 36) and
                -- BALANCE-22203
                -- BALANCE-22331: по новым условиям считаем только рубли
                b.currency = 'RUR'
             or b.invoice_dt  < date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12, 36)
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
       or (b.commission_type in (10, 16) and 1=1)
          -- audio
       or (b.commission_type = 11 and b.discount_type_src = 29)
          -- media verticals
       or (b.commission_type = 17 and b.discount_type = 25)
          -- market, BALANCE-27251, BALANCE-26854
       or (b.commission_type in (12, 13) and b.discount_type_src = 11)
          -- belarus, BALANCE-29835
       or (b.commission_type = 20 and b.discount_type in (1, 2, 3, 4, 24, 36, 12, 7, 37))

       union all

       select b.contract_eid,
           b.contract_id,
           b.invoice_eid,
           b.invoice_id,
           b.invoice_dt,
           b.invoice_type,
           0    as payment_control_type,
           b.contract_from_dt,
           b.contract_till_dt,
           b.currency,
           b.nds,
           b.payment_type,
           b.commission_type,
           b.discount_type,
           b.discount_type_src,
           b.is_agency_credit_line,
           b.total_sum*100/(100 + b.nds*b.nds_pct)      as invoice_total_sum,
           b.total_sum                                  as invoice_total_sum_w_nds,
           oebs.comiss_date,
           oebs.dt                                      as oebs_dt,
           oebs.payment_number,                          
           trunc(oebs.comiss_date, 'MM')                 as from_dt,
           add_months(
            trunc(oebs.comiss_date, 'MM'), 1) -1/84600    as till_dt,
           oebs.oebs_payment*100/(100 + b.nds*b.nds_pct) as amt,
           oebs.oebs_payment                             as amt_w_nds,
       -- Сумма платежей нарастающим итогом по счету
       -- в хронологическом порядке
-- (используется для расчета премии в других представлениях)
           sum(oebs.oebs_payment*100/(100 + b.nds*b.nds_pct))
            over(partition by b.invoice_id
                     order by oebs.comiss_date,oebs.dt,oebs.payment_number)        as amt_by_invoice,
   -- (используется для определения ранней оплаты, только внутри данного
         -- представления)            
            sum(oebs.oebs_payment)
            over(partition by b.invoice_id
                     order by oebs.comiss_date, oebs.dt,oebs.payment_number)       as  payments_curr_by_invoice_w_nds
      from XXX_COMM_2013_BASE_SRC        b
      join xxxx_oebs_cash_payment_test     oebs on oebs.invoice_id = b.invoice_id
                                                and oebs.comiss_date >= date'2019-04-01'
                                                and oebs.comiss_date is not null
       where b.commission_type = 60
       and b.discount_type in (7, 12, 13, 27, 37, 42)

)
--select * from s_payments_temp;

,

 s_early_payment_pre as 
(
select d.*,
-- полностью оплаченный счет
       case
        when payments_curr_by_invoice_w_nds >= invoice_total_sum_w_nds
        then 1
        else 0
       end               as is_fully_paid_pre,
       (
        -- Счет оплачен досрочно (не менее чем на 1 день ранее срока оплаты)
        -- Счет оплачен полностью. За частичную оплату счета (независимо от срока
        --    частичной оплаты) вознаграждение за досрочную оплату не начисляется
        --    и не перечисляется
            case
            when d.is_agency_credit_line = 1
            then (
                select case
                       when a.act_count = 1
                         and d.comiss_date <= a.payment_term_dt - 1
                          -- BALANCE-24851
                         and a.act_amount = d.payments_curr_by_invoice_w_nds
                        then 1
                        else 0
                       end
                  from (
                    -- Т.к. ЛС, то по счету на оплату, должен быть ровно 1 акт
                    -- Сумма счета и акта должна совпадать
                    select min(a.PAYMENT_TERM_DT)   as payment_term_dt,
                           count(distinct a.act_id)     as act_count,
                           sum(a.amount)            as act_amount
                      from xxxx_acts        a
                     where a.invoice_id = d.invoice_id
                       and a.hidden < 4
                       ) a
                 )
            else 0
            end
       )       as is_early_payment_pre,
       (
            select case
                    when a.act_count = 1
                     and d.comiss_date <= a.payment_term_dt - 1
                      -- BALANCE-24851
                     and a.act_amount = d.payments_curr_by_invoice_w_nds
                    then 1
                    else 0
                   end
              from (
                -- Т.к. ЛС, то по счету на оплату, должен быть ровно 1 акт
                -- Сумма счета и акта должна совпадать
                select min(a.PAYMENT_TERM_DT)   as payment_term_dt,
                       count(distinct a.id)     as act_count,
                       sum(a.amount)            as act_amount
                  from xxxx_acts        a
                 where a.invoice_id = d.invoice_id
                   and a.hidden < 4
                       ) a

       )                                            as is_early_payment_true_pre

  from s_payments_temp          d
  where not exists (
            select 1
              from xxxx_acts a
             where a.is_loyal = 1
               and a.invoice_id = d.invoice_id
               and a.hidden < 4
               and a.dt >= date'2017-03-01'
               and d.invoice_dt >= date'2017-03-01' 
)
)
-- select * from s_early_payment_pre; 
,
 s_early_payment_counted as (
select d.*,
-- BALANCE-27396: условия по платежам 2018 года можно применять только для

       -- счетов, созданных после 2018-03
       case
        when from_dt >= date'2018-03-01'
         and invoice_dt >= date'2018-03-01'
        then 1
        else 0
       end                                          as is_2018,
       count(decode(is_fully_paid_pre, 1, 1, null)) over(partition by invoice_id
                             order by comiss_date, oebs_dt, payment_number) as
                        fully_paid_pos,
       count(decode(is_early_payment_pre, 1, 1, null)) over(partition by invoice_id
                             order by comiss_date, oebs_dt, payment_number) as
                         early_payment_pos,
       count(decode(is_early_payment_true_pre, 1, 1, null)) over(partition by invoice_id
                             order by comiss_date, oebs_dt, payment_number) as
                        early_payment_true_pos
  from s_early_payment_pre          d
)

--select *from s_early_payment_counted;

select d."CONTRACT_EID",d."CONTRACT_ID",d."INVOICE_EID",d."INVOICE_ID",d."INVOICE_DT",d."INVOICE_TYPE", d."PAYMENT_CONTROL_TYPE", d."CONTRACT_FROM_DT",d."CONTRACT_TILL_DT",d."CURRENCY",d."NDS",d."PAYMENT_TYPE",d."COMMISSION_TYPE",d."DISCOUNT_TYPE",d."DISCOUNT_TYPE_SRC",d."IS_AGENCY_CREDIT_LINE",d."INVOICE_TOTAL_SUM",d."INVOICE_TOTAL_SUM_W_NDS",d."COMISS_DATE",d."OEBS_DT",d."PAYMENT_NUMBER",d."FROM_DT",d."TILL_DT",d."AMT",d."AMT_W_NDS",d."AMT_BY_INVOICE",d."PAYMENTS_CURR_BY_INVOICE_W_NDS",d."IS_FULLY_PAID_PRE",d."IS_EARLY_PAYMENT_PRE",d."IS_2018",d."FULLY_PAID_POS",d."EARLY_PAYMENT_POS",
 -- BALANCE-27395/27396/27397: начиная с 2018-03 платим только по
       -- полностью оплаченным счетам, а не по каждой оплате
       case
        when is_2018 = 1
        then invoice_total_sum_w_nds
        else amt_w_nds
       end                                                   as amt_ttl_w_nds,
       case
        when is_2018 = 1
        then invoice_total_sum
        else amt
       end                                                   as amt_ttl,
       -- BALANCE-28622: кол-во платежей с таким номером в текущем периоде
       count(1)
        over(partition by contract_id, from_dt, payment_number)                    as payment_count_by_contract,
       count(1)
        over(partition by contract_id, from_dt,
                         invoice_id, payment_number)        as payment_count_by_invoice,
       -- BALANCE-26692: отображаем только первое появление признака
       decode(fully_paid_pos, 1, is_fully_paid_pre, 0)       as is_fully_paid,
       decode(early_payment_pos, 1, is_early_payment_pre, 0) as is_early_payment,
       decode(early_payment_true_pos, 1, is_early_payment_true_pre, 0) as is_early_payment_true
  from s_early_payment_counted d;

CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."XXX_AR_ACTS_Q_EXT" ("CONTRACT_EID", "CONTRACT_ID", "AGENCY_ID", "DISCOUNT_TYPE", "COMMISSION_TYPE", "FROM_DT", "TILL_DT","AMT", "AMT_W_NDS", "AMT_Q", "AMT_PREV_Q", "FAILED") AS 
 with

counting_date as (select date'2019-07-03' as dt from dual)
  --select * from counting_date;
,

s_quarters AS (
    select d.dt from_dt, add_months(d.dt, 3) - 1/24/60/60 as till_dt
      from (
         select add_months(date'2016-03-01', 3*(level-1)) as dt
           from dual
        connect by level <= 20
           ) d
        -- прошлый квартал
     where trunc((select dt from counting_date), 'MM') - 1 between d.dt and add_months(d.dt, 3)
        -- такой же квартал год назад
        or add_months(trunc((select dt from counting_date), 'MM') - 1, -12) between d.dt
                                                         and add_months(d.dt, 3)
)
, s_excluded_clients as (
    select /*+ materialize */ *
      from xxx_ar_excluded_clients  e
)
-- обороты по-квартально по каждому договору
-- (учитывая новую шкалу)
, s_curr_contracts as (
    select d.contract_eid,        d.contract_id,        d.agency_id,
           d.contract_from_dt,    d.contract_till_dt,
           d.discount_type,
           d.from_dt, d.till_dt,
           d.commission_type,
           sum(d.amt_w_nds_rub)                             as amt_w_nds_rub,
           sum(d.amt_rub)                                   as amt
      from xxx_ar_acts_q      d
     where d.commission_type in (1, 2,  8, 21, 23)
       and trunc((select dt from counting_date), 'MM') - 1 between d.from_dt and d.till_dt
     group by d.contract_eid,       d.contract_id,      d.agency_id,
           d.contract_from_dt,      d.contract_till_dt,
           d.discount_type,
           d.from_dt, d.till_dt,
           d.commission_type
)

--select  * from s_curr_contracts;
-- статистика по агентству (не договору)
-- не используем v_opt_2015_acts, тк надо учесть оферты
, s_agency_stats_src as (
    select /*+ materialize */
           -- аг-во консолидируем всегда, т.к. независимо от типа консолидации
           -- договора, нам надо знать прирост по объединенному аг-ву
           decode(c.cons_type, null, a.client_id, c.agency_id)      as agency_id,
           decode(nvl(at.commission_type, nvl(i.commission_type, i.discount_type)),
                -- Справочник считаем как Директ, т.к. шкала по общему обороту
--                12, 7,
                -- Вся медийка под номером 1 будет
                2, 1,
                3, 1,
--              36, 1,
                37, 1,
                nvl(at.commission_type,
                    nvl(i.commission_type, i.discount_type)))       as discount_type,
           q.from_dt,
           trunc(a.dt, 'MM')                                        as month,
           sum((at.amount - at.amount_nds - at.amount_nsp)*cr.rate) as amt
      from xxxx_acts         a
      join xxxx_invoice     i on i.inv_id = a.invoice_id
                          
      join xxxx_act_trans   at on at.act_id = a.act_id
      join xxxx_order       o  on o.order_id = at.parent_order_id
                              and not exists (
                                       select 1
                                         from s_excluded_clients  e
                                        where e.contract_id = i.contract_id
                                          and e.client_id = o.client_id
                                  )
      join xxxx_currency_rate    cr on cr.cc = i.currency
                                      and cr.rate_dt = trunc(a.dt)
      join s_quarters       q on a.dt between q.from_dt and q.till_dt
      left outer
      join xxx_ar_consolidations_q c on c.linked_agency_id = a.client_id
     where a.hidden < 4
        -- в s_curr_contracts уже объединенные договоры. Следовательно,
        -- agency_id там тоже от главных договоров. Поэтому, и слева
        -- и справа должно быть агентство после объединения
        and decode(c.cons_type, null, a.client_id, c.agency_id) in
            (select agency_id from s_curr_contracts)
       and a.is_loyal = 0
          and nvl(at.commission_type, nvl(i.commission_type, i.discount_type) ) in
                                    (1, 2, 3, 7, 12, 36, 37, 40, 42)
     group by q.from_dt, trunc(a.dt, 'MM'),
      decode(c.cons_type, null, a.client_id, c.agency_id),
           decode(nvl(at.commission_type, nvl(i.commission_type, i.discount_type)),
                -- Справочник считаем как Директ, т.к. шкала по общему обороту
--                12, 7,
                -- Вся медийка под номером 1 будет
                2, 1,
                3, 1,
--                36, 1,
                37, 1,
                nvl(at.commission_type, nvl(i.commission_type, i.discount_type)))
)

--select  *from s_agency_stats_src;

-- Обороты по агентству в текущем периоде
, s_agency_stats_curr as (
    select agency_id,
           discount_type,
           from_dt,
           sum(amt)                                               as amt
      from s_agency_stats_src d
      group by agency_id, from_dt, discount_type
)
--select * from s_agency_stats_curr;
-- Прошлогодние обороты по аг-вам
, s_agency_stats_prev as (
    select agency_id,
           discount_type,
           add_months(from_dt, 12)                                as from_dt,
           count(from_dt)                                         as month_cnt,
           sum(amt)                                               as amt
      from s_agency_stats_src
     group by agency_id, add_months(from_dt, 12), discount_type
)
select /*+ parallel(16) */
       d.contract_eid,
       d.contract_id,
       d.agency_id,
       d.discount_type,
       d.commission_type,
       d.from_dt, d.till_dt,
       d.amt,
       -- За квартал, разбитый по типам рекламы
       d.amt_w_nds_rub      amt_w_nds,
       cs.amt                   as amt_q,
       nvl(ps.amt, 0)           as amt_prev_q,
       case
        when ps.month_cnt != 3 then 1
        else 0
     end                      as failed

  from s_curr_contracts      d
  join s_agency_stats_curr   cs on cs.agency_id = d.agency_id
                               and cs.from_dt = d.from_dt
                               and cs.discount_type = d.discount_type
  left outer
  join s_agency_stats_prev   ps on ps.agency_id = d.agency_id
                               and ps.from_dt = d.from_dt
                               and ps.discount_type = d.discount_type;

CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."XXX_AR_REWARDS" ("CONTRACT_ID", "CONTRACT_EID", "FROM_DT", "TILL_DT", "NDS", "CURRENCY", "DISCOUNT_TYPE", "REWARD_TYPE", "TURNOVER_TO_CHARGE", "REWARD_TO_CHARGE", "DELKREDERE_TO_CHARGE", "DKV_TO_CHARGE", "TURNOVER_TO_PAY", "TURNOVER_TO_PAY_W_NDS", "REWARD_TO_PAY", "REWARD_TO_PAY_SRC", "DELKREDERE_TO_PAY", "DKV_TO_PAY", "INSERT_DT", "TP") AS 
  with
xxx_ar_rewards_history as (
    select 'base'   as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay,         reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_base_src   
    where (contract_id like '1000%' or contract_id like '%0000%' or contract_id like '21310%')
    union all
    select 'prof'   as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay,         reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_prof_src   
     where (contract_id like '1000%' or contract_id like '%0000%' or contract_id like '21310%')
    union all
--    select 'sprav'  as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay, null as reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_sprav_src  union all
--    select 'auto'   as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay, null as reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_auto_src   union all
--    select 'spec'   as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay, null as reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_spec_src   union all
--    select 'autoru' as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay, null as reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_autoru_src union all
--    select 'estate' as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay,         reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_estate_src union all
--    select 'nonrez' as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay, null as reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_nonrez_src union all
--    select 'imho'   as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay, null as reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_imho_src   union all
    select 'kazakh' as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay,         reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_kazakh_src 
     where (contract_id like '1000%' or contract_id like '%0000%' or contract_id like '21310%')
    union all
    select 'belarus'as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay, null as reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_belarus_src   
     where (contract_id like '1000%' or contract_id like '%0000%' or contract_id like '21310%')
--    union all
--    select 'audio'  as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay, null as reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_audio_src
)
, s_last_insert_dt as (
    select /*+ materialize */
           tp, from_dt, till_dt, max(insert_dt) as insert_dt
      from xxx_ar_rewards_history
     group by tp, from_dt, till_dt
)
, s_reward_src as (
    select d.contract_id,     d.contract_eid,
           d.from_dt,         d.till_dt,
           d.nds,             d.currency,
           d.discount_type,   d.reward_type,
           d.turnover_to_charge,
           d.reward_to_charge,
           d.delkredere_to_charge,
           d.dkv_to_charge,
           d.turnover_to_pay,
           d.turnover_to_pay_w_nds,
           d.reward_to_pay,
           d.reward_to_pay_src,
           d.delkredere_to_pay,
           d.dkv_to_pay,
           d.insert_dt,
           d.tp
      from xxx_ar_rewards_history                d
        -- BALANCE-17502: последняя вставка по каждому интервалу
      join s_last_insert_dt     l on l.from_dt = d.from_dt
                                 and l.till_dt = d.till_dt
                                 and l.insert_dt = d.insert_dt
                                 and l.tp = d.tp
--     union all
--        -- BALANCE-20145
--    select contract_id,     contract_eid,
--           from_dt,         till_dt,
--           nds,             currency,
--           -- BALANCE-26215
--           discount_type,
--           reward_type,
--           null                 as turnover_to_charge,
--           -- BALANCE-25723
--           case when from_dt >= date'2017-03-01' then
--                reward_to_charge
--            else null
--           end                  as reward_to_charge,
--           null                 as delkredere_to_charge,
--           null                 as dkv_to_charge,
--           null                 as turnover_to_pay,
--           null                 as turnover_to_pay_w_nds,
--           reward_to_pay,
--           -- BALANCE-25532
--           reward_to_pay_src,
--           delkredere_to_pay,
--           null                 as dkv_to_pay,
--           -- BALANCE-26473: чтобы корректировки были видны в периодах,
--           -- когда нет фактической премии
--           till_dt              as insert_dt,
--           substr(type, 1, 
--            instr(type, '_')-1) as tp
--      from bo.t_commission_correction
--     where type like '%_inline'
)
, s_reward as (
    select contract_id,     contract_eid,
           from_dt,         till_dt,
           nds,             currency,
           discount_type,   reward_type,
           sum(turnover_to_charge)      as turnover_to_charge,
           sum(reward_to_charge)        as reward_to_charge,
           sum(delkredere_to_charge)    as delkredere_to_charge,
           sum(dkv_to_charge)           as dkv_to_charge,
           sum(turnover_to_pay)         as turnover_to_pay,
           sum(turnover_to_pay_w_nds)   as turnover_to_pay_w_nds,
           sum(reward_to_pay)           as reward_to_pay,
           sum(reward_to_pay_src)       as reward_to_pay_src,
           sum(delkredere_to_pay)       as delkredere_to_pay,
           sum(dkv_to_pay)              as dkv_to_pay,
           max(insert_dt)               as insert_dt,
           max(tp)                      as tp
      from s_reward_src
     group by contract_id,     contract_eid,
              from_dt,         till_dt,
              nds,             currency,
              discount_type,   reward_type
)
select d.contract_id,     d.contract_eid,
       d.from_dt,         d.till_dt,
       d.nds,             d.currency,
       d.discount_type,   d.reward_type,
       d.turnover_to_charge,
       d.reward_to_charge,
       d.delkredere_to_charge,
       d.dkv_to_charge,
       d.turnover_to_pay,
       d.turnover_to_pay_w_nds,
       d.reward_to_pay,
       d.reward_to_pay_src,
       d.delkredere_to_pay,
       d.dkv_to_pay,
       d.insert_dt,
       d.tp
  from s_reward d
-- union all
--select contract_id, contract_eid,
--       from_dt, till_dt,
--       nds,
--       currency,
--       -- BALANCE-26215
--       nvl(discount_type, 100) as discount_type,
--       0    as reward_type,
--       null as turnover_to_charge,
--       reward_to_charge,
--       delkredere_to_charge,
--       dkv_to_charge,
--       turnover_to_pay,
--       null as turnover_to_pay_w_nds,
--       reward_to_pay,
--       null as reward_to_pay_src,
--       delkredere_to_pay,
--       dkv_to_pay,
--       null as insert_dt,
--       type as tp
--  from bo.t_commission_correction
-- where type not like '%_inline'
;
