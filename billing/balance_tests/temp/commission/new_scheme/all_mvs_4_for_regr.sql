CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."X_V_OPT_2015_INVOICES" ("CONTRACT_EID", "CONTRACT_ID", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "INVOICE_EID", "INVOICE_ID", "INVOICE_DT", "INVOICE_TYPE", "PAYMENT_CONTROL_TYPE", "TOTAL_SUM", "CURRENCY", "NDS", "NDS_PCT", "LOYAL_CLIENTS", "DISCOUNT_TYPE", "DISCOUNT_TYPE_SRC", "PAYMENT_TYPE", "IS_AGENCY_CREDIT_LINE", "COMMISSION_TYPE") AS 

     with
counting_date as (select date'2019-07-03' as dt from dual),


s_attrs_src as (
    select value_num, 
        update_dt,
           contract2_id                                         as contract_id,
           code,
               cl_dt                                                as from_dt,
                nvl(
            lead(cl_dt) over(partition by code, contract2_id
                                 order by stamp),
            add_months(trunc((select dt from counting_date), 'MM'), 3)
           ) -1/24/60/60                                        as till_dt
      from bo.x_mv_contract_signed_attr_hist
    
)
--select * from s_attrs_src;

, 

s_attrs as (
     select *
       from s_attrs_src
         -- BALANCE-25145: смотрим значение атрибута на конец пред.месяца
      where trunc((select dt from counting_date), 'MM') - 1 between from_dt and till_dt
)
, s_pct as (
    select /*+ materialize*/
           a.value_num, a.contract_id
      from s_attrs a
     where a.code = 'AR_PAYMENT_CONTROL_TYPE'
)

select 
        c.external_id      as contract_eid,
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
        when ca_ct.value_num  in (4, 10, 12, 13)
         and f.value_num  = 1
        then nvl((
        select l.value_num
                from bo.x_mv_contract_signed_attr l
               where l.contract_id =  c.id
                 and l.code = 'LINK_CONTRACT_ID'
                 and l.value_num is not null),
           c.id)
        else c.id
       end                                            as contract_id,
           fr.dt                                    as contract_from_dt,
       tl.value_dt                              as contract_till_dt,
       i.external_id                            as invoice_eid,
       i.id                                     as invoice_id,
       i.dt                                     as invoice_dt,
       i.type                                   as invoice_type,
           case
           when i.dt >= date '2019-03-01' then
               nvl(
         -- Показываем КОС на конец прошлого периода,
         -- Чтобы в одном периоде по одному договору было одно и то же
         -- значения атрибута во всех счетах
           (select a.value_num
              from s_pct       a
                where a.contract_id = i.contract_id), 0)
             else 0
           end                                      as payment_control_type,
           i.total_sum                                as total_sum, 
          i.currency                               as currency,
       i.nds                                    as nds,
       i.nds_pct                                as nds_pct,
       i.loyal_clients                          as loyal_clients,
             -- BALANCE-17175
           case nvl(i.commission_type, i.discount_type)
            when 22 then 1
            when 29 then 1  -- Аудиореклама должа учитываться как мейдика
                else nvl(i.commission_type, i.discount_type)
           end                                      as discount_type,
           nvl(i.commission_type, i.discount_type)  as discount_type_src,
       ca_pt.value_num                          as payment_type,
           case
           -- Только для Директа/Медийки в Директе
           when nvl(i.commission_type, i.discount_type) in (7, 37) then
          nvl((
            -- оплаты приходят на счет "на оплату", по нему выходим на ЛС
            -- по ЛС смотрим в доп.атрибуты на наличие subclient_id
            -- subclient_id is     null: агентская кредитная линия
            -- subclient_id is not null: клиентская кредитная линия
            select decode(p.value_num, null, 1, 0)
              from bo.x_t_invoice_repayment   ir
              join bo.x_t_extprops           p on p.object_id = ir.invoice_id
                                             and p.classname = 'PersonalAccount'
                                             and p.attrname = 'subclient_id'
             where ir.repayment_invoice_id = i.id
              union
                -- BALANCE-25160: в последнее время записей в t_extprops нет
                -- это тоже надо учесть
                select 1
                   from bo.x_t_invoice_repayment   ir  
                      where ir.repayment_invoice_id =  i.id
                     and not exists (
                     select 1 from bo.x_t_extprops p 
                      where p.object_id = ir.invoice_id
                     and p.classname = 'PersonalAccount'
                     and p.attrname = 'subclient_id'
)
        -- Если ничего не найдено, то не ЛС вообще, поэтому не агентская кр.л.
        ), 0)
        else 0
        end                                   as is_agency_credit_line,
        ca_ct.value_num                   as commission_type
  from bo.x_t_contract2                   c
  -- тип комиссии
  join s_attrs                          ca_ct  on ca_ct.contract_id = c.id
                                              and ca_ct.code = 'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE'
                                                -- bo.t_enums_tree:
                                                --  1 Базовая 2015
                                                --  2 Премиум 2015
                                                --  3 Спецпроекты
                                                --  4 Яндекс.Авто
                                                --  5 Справочник
                                                --  6 Украина 2015
                                                --  7 Авто.ру
                                                --  8 Базовая, регион
                                                -- 10 Недвижимость 2015
                                                -- 11 Аудиореклама 2015
                                                -- 14 Маркет, спецпроекты
                                                -- 15 ОФД
                                                -- 16 Недвижимость 2017
                                                -- 17 Медийка Вертикали 2017
                                                -- 19 Интерко премия 2017
                                                -- 20 Премии Беларусь
                                                -- 21 Базовая СПб
                                                -- 23 Базовая Лайт
                                              and ca_ct.value_num in (1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14, 16, 17, 19, 20, 21, 23)
  -- тип оплаты:
  --  2                    предоплата
  --  3                    постоплата
  join s_attrs                          ca_pt  on ca_pt.contract_id = c.id
                                              and ca_pt.code = 'PAYMENT_TYPE'
                                              and ca_pt.value_num in (2, 3)
  -- начало договора
  join bo.x_t_contract_collateral         fr on fr.contract2_id = c.id
                                          and fr.num is null
  -- конец договора
  join bo.x_mv_contract_signed_attr       tl on tl.contract_id = c.id
                                          and tl.code = 'FINISH_DT'
  -- фирма договора
  join s_attrs                          f  on f.contract_id = c.id
                                          and f.code = 'FIRM'
  -- счет
  join bo.x_t_invoice                     i  on i.contract_id = c.id
                                          and (
                                                (
                                                     -- только базовые/профы
                                                    ca_ct.value_num in (1, 2, 8, 21, 23) and
                                                    -- то учитываем еще и код 22
                                                    nvl(i.commission_type, i.discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 17, 19, 22, 25, 28, 29, 36, 37, 38, 40, 42)
                                                )
                                                   or
                                                (
                                                    -- Беларусия
                                                    ca_ct.value_num in (20) and
                                                    nvl(i.commission_type, i.discount_type) in (7, 37, 1, 2, 3, 4, 24, 36, 12)
                                                )
                                                 or
                                                (
                                                    -- Маркет
                                                   ca_ct.value_num in (12, 13) and
                                                    nvl(i.commission_type, i.discount_type) in
                                                    (11)
                                                )
                                                or
                                                (
                                                    -- Маркет, спецпроекты
                                                    ca_ct.value_num in (14) and
                                                    nvl(i.commission_type, i.discount_type) in
                                                    (33)
                                                )
                                                or
                                                (
                                                    -- По Недвижимости смотрим всё
                                                    ca_ct.value_num in (10, 16) and 1=1
                                                )
                                                or
                                                (
                                                    -- По Украине пропускаем все,
                                                    -- чтобы было в 15/16 году.
                                                    -- Отфильтруем более точно
                                                    -- на уровне v_opt_2015_acts
                                                    -- BALANCE-23716
                                                    ca_ct.value_num in (6) and
                                                    nvl(i.commission_type, i.discount_type) in
                                                    (0, 1, 2, 3, 7, 8, 12, 15, 36)
                                                )
                                                or
                                                (
                                                    -- Медийка Вертикали — Медийка-Авто.ру
                                                    ca_ct.value_num in (17)
                                                and nvl(i.commission_type,
                                                    i.discount_type) in (25)
                                                )
                                                or
                                                (
                                                    -- иначе, как ранее
                                                    nvl(i.commission_type,
                                                        i.discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 17, 19, 28, 29)
                                                )
                                              );
--------------------------


  CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."X_V_COMM_2013_BASE_SRC" ("CONTRACT_EID", "CONTRACT_ID", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "INVOICE_EID", "INVOICE_ID", "INVOICE_DT", "CURRENCY", "NDS", "NDS_PCT", "INVOICE_TYPE", "TOTAL_SUM", "LOYAL_CLIENTS", "ENDBUYER_ID", "ENDBUYER_INN", "PERSON_INN", "IS_OPT", "IS_THERE_OLD_ACTS", "DISCOUNT_TYPE", "DISCOUNT_TYPE_SRC", "PAYMENT_TYPE", "IS_AGENCY_CREDIT_LINE", "PAYMENT_CONTROL_TYPE", "COMMISSION_PAYBACK_TYPE", "COMMISSION_PAYBACK_PCT", "COMMISSION_TYPE") AS 
  select c.external_id                            as contract_eid,
       c.id                                     as contract_id,
       fr.dt                                    as contract_from_dt,
       tl.value_dt                              as contract_till_dt,
       i.external_id                            as invoice_eid,
       i.id                                     as invoice_id,
       i.dt                                     as invoice_dt,
       i.currency                               as currency,
       i.nds                                    as nds,
       i.nds_pct                                as nds_pct,
       i.type                                   as invoice_type,
       i.total_sum                              as total_sum,
       i.loyal_clients                          as loyal_clients,
       
       
       decode(ex.value_num, 0, null,
                ex.value_num)                   as endbuyer_id,
       eb.inn                                   as endbuyer_inn,
       ag.inn                                   as person_inn,
       
       
       case
          
          
        when i.paysys_id in (1025, 1026, 1027) then 0
        when decode(ex.value_num, 0, null, ex.value_num) is null
          or eb.inn = ag.inn
        then 1
        else 0
       end                                      as is_opt,
       0                                        as is_there_old_acts,
       
       decode(
        nvl(i.commission_type, i.discount_type),
        22, 1,
        29, 1,          
        nvl(i.commission_type, i.discount_type)
       )                                        as discount_type,
       nvl(i.commission_type, i.discount_type)  as discount_type_src,
       ca_pt.value_num                          as payment_type,
       0                                        as is_agency_credit_line,
       0                                        as payment_control_type,
       ca_pbt.value_num                         as commission_payback_type,
       ca_cpp.value_num                         as commission_payback_pct,
       ca_ct.value_num                          as commission_type
  from bo.x_t_contract2                   c
  
  join bo.x_mv_contract_signed_attr       ca_ct  on ca_ct.contract_id = c.id
                                              and ca_ct.code = 'COMMISSION_TYPE'
                                              and ca_ct.value_num in (47, 48, 49, 50, 56, 57, 60)
  
  
  
  join bo.x_mv_contract_signed_attr       ca_pt  on ca_pt.contract_id = c.id
                                              and ca_pt.code = 'PAYMENT_TYPE'
                                              and ca_pt.value_num in (2, 3)
  
  join bo.x_t_contract_collateral         fr on fr.contract2_id = c.id
                                          and fr.num is null
  
  join bo.x_mv_contract_signed_attr       tl on tl.contract_id = c.id
                                          and tl.code = 'FINISH_DT'
  
  left outer
  join bo.x_mv_contract_signed_attr       ca_cpp on ca_cpp.contract_id = c.id
                                              and ca_cpp.code = 'COMMISSION_PAYBACK_PCT'
  
  
  
  left outer
  join bo.x_mv_contract_signed_attr       ca_pbt on ca_pbt.contract_id = c.id
                                              and ca_pbt.code = 'COMMISSION_PAYBACK_TYPE'
                                              and ca_pbt.value_num in (1, 2)
  
  join bo.x_t_invoice                     i  on i.contract_id = c.id
                                          and (
                                                
                                                
                                                (
                                                    
                                                    i.dt >= date'2014-03-01' and
                                                    
                                                    ca_ct.value_num in (47, 48) and
                                                    
                                                    nvl(i.commission_type,
                                                        i.discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 19, 22, 29, 36)
                                                )
                                                or
                                                (
                                                    
                                                    nvl(i.commission_type,
                                                        i.discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 13, 14, 19, 27, 29, 36, 37, 42)
                                                )
                                              )
  
  join bo.x_t_person                      ag on ag.id = i.person_id
  
  left outer
  join bo.x_t_extprops                    ex on ex.object_id = i.id
                                          and ex.classname = 'Invoice'
                                          and ex.attrname = 'endbuyer_id'
  
  left outer
  join bo.x_t_person                      eb on eb.id = ex.value_num;


--------------------------  

CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."X_T_ACT" ("ID", "DT", "CLIENT_ID", "INVOICE_ID", "RUR_SUM_A", "RUR_SUM_B", "RUR_SUM_C", "RUR_SUM_D", "RUR_SUM_E", "USD_RATE", "HIDDEN", "USD_SUM_C", "AMOUNT", "AMOUNT_NDS", "AMOUNT_NSP", "EXTERNAL_ID", "PAID_AMOUNT", "FACTURA", "UPDATE_DT", "UNILATERAL", "JIRA_ID", "GOOD_DEBT", "CURRENCY_RATE", "IS_DOCS_SEPARATED", "IS_DOCS_DETAILED", "ACT_SUM", "PAYMENT_TERM_DT", "IS_LOYAL", "IS_TRP", "MONTH_DT", "OPERATION_ID", "LABEL", "TYPE", "MEMO", "STATE_ID") AS 
select "ID","DT","CLIENT_ID","INVOICE_ID","RUR_SUM_A","RUR_SUM_B","RUR_SUM_C","RUR_SUM_D","RUR_SUM_E","USD_RATE","HIDDEN","USD_SUM_C","AMOUNT","AMOUNT_NDS","AMOUNT_NSP","EXTERNAL_ID","PAID_AMOUNT","FACTURA","UPDATE_DT","UNILATERAL","JIRA_ID","GOOD_DEBT","CURRENCY_RATE","IS_DOCS_SEPARATED","IS_DOCS_DETAILED","ACT_SUM","PAYMENT_TERM_DT","IS_LOYAL","IS_TRP","MONTH_DT","OPERATION_ID","LABEL","TYPE","MEMO","STATE_ID" from bo.x_t_act_internal
where hidden < 4 and type = 'generic';

--------------------------

CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."X_V_AR_ACTS" ("CONTRACT_EID", "CONTRACT_ID", "INVOICE_EID", "INVOICE_ID", "INVOICE_DT", "INVOICE_TYPE", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "CURRENCY", "NDS", "PAYMENT_TYPE", "COMMISSION_TYPE", "PAYMENT_CONTROL_TYPE", "DISCOUNT_TYPE", "DISCOUNT_TYPE_SRC", "IS_AGENCY_CREDIT_LINE", "IS_LOYAL", "CLIENT_ID", "ORDER_ID", "SERVICE_ID", "SERVICE_ORDER_ID", "AGENCY_ID", "ACT_ID", "ACT_EID", "ACT_DT", "FROM_DT", "TILL_DT", "AMT_W_NDS", "AMT", "AMT_W_NDS_RUB", "AMT_RUB", "BRAND_ID") AS 
  with
counting_date as (select date'2019-07-03' as dt from dual),

s_brands as (
        select 
               atr.key_num                  as client_id,
               atr.collateral_dt,
               c.dt,
               trunc(nvl(c.finish_dt, date'2019-07-03'))
                
                + 1 - 1/24/60/60            as finish_dt,
               min(c.client_id)             as main_client_id
          from bo.x_mv_contract_signed_attr    atr
            
            
          join bo.x_mv_ui_contract_apex        c on c.contract_id = atr.contract_id
         where atr.code = 'BRAND_CLIENTS'
            
           and atr.value_num = 1
         group by atr.key_num, atr.collateral_dt, c.dt, nvl(c.finish_dt, date'2019-07-03')
),

s_src as (
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
       end                                              as is_loyal,
       o.client_id                                      as client_id,
       o.id                                             as order_id,
       o.service_id,
       o.service_order_id,
       a.client_id                                      as agency_id,
       a.id                                             as act_id,
       a.external_id                                    as act_eid,
       a.dt                                             as act_dt,
       trunc(a.dt, 'MM')                                as from_dt,
       add_months(trunc(a.dt, 'MM'), 1) - 1/84600       as till_dt,
       at.amount                                        as amt_w_nds,
       at.amount-at.amount_nds-at.amount_nsp            as amt,
       at.amount*cr.rate                                as amt_w_nds_rub,
       (at.amount-at.amount_nds-at.amount_nsp)*cr.rate  as amt_rub
  from bo.x_v_opt_2015_invoices        b
  join bo.x_t_act                         a  on a.invoice_id = b.invoice_id
                                          and a.hidden < 4
                                          and a.dt >= date'2015-03-01'
                                           
                                          and ( a.is_loyal = 0 and a.dt >= date'2017-03-01'
                                             or a.dt <  date'2017-03-01')
  join bo.x_t_act_trans                   at on at.act_id = a.id
                                          and at.netting is null
  join bo.x_t_consume                     q  on q.id = at.consume_id
  join bo.x_t_order                       o  on o.id = q.parent_order_id
  join bo.x_mv_currency_rate            cr on cr.cc = b.currency
                                          and cr.rate_dt = trunc(a.dt)
 where b.commission_type in (1, 2, 3, 4, 5, 6, 8, 10, 11, 12, 13, 16, 17, 19, 20, 21, 23)
   and (
          
          (
            b.commission_type in (1, 2, 8, 21, 23)
        and (
                a.dt >= date'2019-03-01' and b.contract_till_dt > date'2019-03-01' and b.discount_type in (1, 2, 3, 7, 12, 36, 37, 38, 40, 42)
                
                
             or a.dt >= date'2017-03-01' and b.contract_till_dt > date'2017-03-01' and b.discount_type in (1, 2, 3, 7, 12, 36, 37, 38)
                

             or a.dt >= date'2017-03-01' and b.contract_till_dt <= date'2017-03-01' and b.discount_type in (1, 2, 3, 7, 12, 25, 36)
             or a.dt >= date'2016-03-01' and a.dt < date'2017-03-01'                and b.discount_type in (1, 2, 3, 7, 12, 25)
             
             or a.dt >= date'2016-03-01' and b.contract_till_dt <= date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
             or a.dt  < date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
            )
          )
          
       or (
                b.commission_type = 6
            and (
                   
                   a.dt >= date'2017-04-01'
               and b.discount_type in (0, 1, 7, 8, 12, 36)
                   
                or a.dt >= date'2016-04-01' and a.dt < date'2017-04-01'
               and b.discount_type in (0, 1, 8, 7, 15)
                or a.dt <  date'2016-04-01'
               and b.discount_type in (1, 2, 3, 7, 12)
                )
          )
          
       or (b.commission_type = 3 and b.discount_type in (17))
          
       or (b.commission_type = 4 and b.discount_type in (19))
          
       or (b.commission_type = 5 and b.discount_type = 12)
          
       or (b.commission_type in (10, 16) and 1=1)
          
       or (b.commission_type = 11 and b.discount_type_src = 29)
          
       or (b.commission_type in (12, 13) and b.discount_type_src = 11)
          
       or (b.commission_type = 17 and b.discount_type = 25)
          
       or (b.commission_type = 19 and b.discount_type = 7)
          
       or (b.commission_type in (20) and at.commission_type in (7, 37, 1, 2, 3, 4, 24, 36, 12))
       )

   union all

   select  b.contract_eid,
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
           
           nvl(at.commission_type, b.discount_type)         as discount_type,
           
           nvl(at.commission_type, b.discount_type_src)     as discount_type_src,
           b.is_agency_credit_line,
           case
            when nvl(a.is_loyal, 0) = 1 and b.discount_type = 7
             then 1
             else 0
           end                                              as is_loyal,
           o.client_id                                      as client_id,
           o.id                                             as order_id,
           o.service_id,
           o.service_order_id,
           a.client_id                                      as agency_id,
           a.id                                             as act_id,
           a.external_id                                    as act_eid,
           a.dt                                             as act_dt,
           trunc(a.dt, 'MM')                                as from_dt,
           add_months(trunc(a.dt, 'MM'), 1) - 1/84600       as till_dt,
           at.amount                                        as amt_w_nds,
           at.amount-at.amount_nds-at.amount_nsp            as amt,
           at.amount*cr.rate                                as amt_w_nds_rub,
           (at.amount-at.amount_nds-at.amount_nsp)*cr.rate  as amt_rub
               
          from bo.x_v_comm_2013_base_src       b
          join bo.x_t_act                         a  on a.invoice_id = b.invoice_id
                                                  and a.hidden < 4
                                                  and a.dt >= date'2019-04-01'
          join bo.x_t_act_trans                   at on at.act_id = a.id
          join bo.x_t_consume                     q  on q.id = at.consume_id
          join bo.x_t_order                       o  on o.id = q.parent_order_id
          join bo.x_mv_currency_rate            cr on cr.cc = b.currency
                                          and cr.rate_dt = trunc(a.dt)
         where b.commission_type = 60
           and b.discount_type in (7, 12, 13, 27, 37, 42)



)
select s."CONTRACT_EID",s."CONTRACT_ID",s."INVOICE_EID",s."INVOICE_ID",s."INVOICE_DT",s."INVOICE_TYPE",s."CONTRACT_FROM_DT",s."CONTRACT_TILL_DT",s."CURRENCY",s."NDS",s."PAYMENT_TYPE",s."COMMISSION_TYPE",s."PAYMENT_CONTROL_TYPE",s."DISCOUNT_TYPE",s."DISCOUNT_TYPE_SRC",s."IS_AGENCY_CREDIT_LINE",s."IS_LOYAL",s."CLIENT_ID",s."ORDER_ID",s."SERVICE_ID",s."SERVICE_ORDER_ID",s."AGENCY_ID",s."ACT_ID",s."ACT_EID",s."ACT_DT",s."FROM_DT",s."TILL_DT",s."AMT_W_NDS",s."AMT",s."AMT_W_NDS_RUB",s."AMT_RUB",
  
       nvl(b.main_client_id, s.client_id)               as brand_id
  from s_src        s
  left outer
  join s_brands     b on b.client_id = s.client_id
                      
                      
                     and b.collateral_dt <= s.till_dt
                      
                     and s.till_dt between b.dt and b.finish_dt;



--------------------------
  CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."X_V_OPT_2015_ACTS_SRC" ("CONTRACT_EID", "CONTRACT_ID", "INVOICE_EID", "INVOICE_ID", "INVOICE_DT", "INVOICE_TYPE", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "CURRENCY", "NDS", "PAYMENT_TYPE", "COMMISSION_TYPE", "PAYMENT_CONTROL_TYPE", "DISCOUNT_TYPE", "DISCOUNT_TYPE_SRC", "IS_AGENCY_CREDIT_LINE", "IS_LOYAL", "CLIENT_ID", "ORDER_ID", "SERVICE_ID", "SERVICE_ORDER_ID", "AGENCY_ID", "ACT_ID", "ACT_EID", "ACT_DT", "FROM_DT", "TILL_DT", "AMT_W_NDS", "AMT", "AMT_W_NDS_RUB", "AMT_RUB", "BRAND_ID", "NDS_COUNT", "CURRENCY_COUNT") AS 
  with s_src as (
    select b.*,
           
           count(distinct b.nds)      over (partition by b.contract_id) as nds_count,
           count(distinct b.currency) over (partition by b.contract_id) as currency_count
      from bo.x_v_ar_acts  b
        
     where b.act_dt >= date'2017-03-01'
)

select "CONTRACT_EID","CONTRACT_ID","INVOICE_EID","INVOICE_ID","INVOICE_DT","INVOICE_TYPE","CONTRACT_FROM_DT","CONTRACT_TILL_DT","CURRENCY","NDS","PAYMENT_TYPE","COMMISSION_TYPE","PAYMENT_CONTROL_TYPE","DISCOUNT_TYPE","DISCOUNT_TYPE_SRC","IS_AGENCY_CREDIT_LINE","IS_LOYAL","CLIENT_ID","ORDER_ID","SERVICE_ID","SERVICE_ORDER_ID","AGENCY_ID","ACT_ID","ACT_EID","ACT_DT","FROM_DT","TILL_DT","AMT_W_NDS","AMT","AMT_W_NDS_RUB","AMT_RUB","BRAND_ID","NDS_COUNT","CURRENCY_COUNT" from s_src
   where 
        act_dt >= date'2016-03-01'
    and currency_count = 1
        and nds_count = 1
        
        and currency in ('RUR', 'BYN', 'BYR', 'KZT');

--------------------------

CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."X_V_OPT_2015_PAYMENTS" ("CONTRACT_EID", "CONTRACT_ID", "INVOICE_EID", "INVOICE_ID", "INVOICE_DT", "INVOICE_TYPE", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "CURRENCY", "NDS", "PAYMENT_TYPE", "COMMISSION_TYPE", "DISCOUNT_TYPE", "DISCOUNT_TYPE_SRC", "IS_AGENCY_CREDIT_LINE", "INVOICE_TOTAL_SUM", "INVOICE_TOTAL_SUM_W_NDS", "PAYMENT_CONTROL_TYPE", "DOC_DATE", "OEBS_DT", "PAYMENT_NUMBER", "FROM_DT", "TILL_DT", "AMT", "AMT_W_NDS", "AMT_BY_INVOICE", "PAYMENTS_CURR_BY_INVOICE_W_NDS", "IS_FULLY_PAID_PRE", "IS_EARLY_PAYMENT_PRE", "IS_EARLY_PAYMENT_TRUE_PRE", "IS_2018", "FULLY_PAID_POS", "EARLY_PAYMENT_POS", "EARLY_PAYMENT_TRUE_POS", "AMT_TTL_W_NDS", "AMT_TTL", "PAYMENT_COUNT_BY_CONTRACT", "PAYMENT_COUNT_BY_INVOICE", "IS_FULLY_PAID", "IS_EARLY_PAYMENT", "IS_EARLY_PAYMENT_TRUE") AS 
  with
  
s_payments as (
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
       b.discount_type,
       b.discount_type_src,
       b.is_agency_credit_line,
       b.total_sum*100/(100 + b.nds*b.nds_pct)      as invoice_total_sum,
       b.total_sum                                  as invoice_total_sum_w_nds,
       b.payment_control_type,
       oebs.doc_date,
       oebs.dt                                      as oebs_dt,
       oebs.payment_number,
       trunc(oebs.doc_date, 'MM')                   as from_dt,
       add_months(
        trunc(oebs.doc_date, 'MM'), 1)-1/84600      as till_dt,
       oebs.sum*100/(100 + b.nds*b.nds_pct)         as amt,
       oebs.sum                                     as amt_w_nds,
       
       
       
       sum(oebs.sum*100/(100 + b.nds*b.nds_pct))
            over(partition by b.invoice_id
                                             
                     order by oebs.doc_date, oebs.dt, oebs.payment_number, oebs.source_type)   as amt_by_invoice,
       
        
       sum(oebs.sum)
            over(partition by b.invoice_id
                     order by oebs.doc_date, oebs.dt, oebs.payment_number, oebs.source_type)   as payments_curr_by_invoice_w_nds
  from bo.x_v_opt_2015_invoices       b
  join bo.x_mv_oebs_receipts_2            oebs on oebs.invoice_eid = b.invoice_eid
                                            and oebs.doc_date >= date'2015-03-01'
                                            and oebs.doc_date is not null
 where 
          
          (
            b.commission_type in (1, 2, 8, 21, 23)
        and (
             
                b.invoice_dt >= date'2019-03-01'        and
                b.discount_type in (1, 2, 3, 7, 12, 36, 37, 38, 40, 42) and
                b.currency = 'RUR'
             or
                
                b.invoice_dt >= date'2017-03-01'        and
                b.discount_type in (1, 2, 3, 7, 12, 36, 37) and
                b.currency = 'RUR'
             or
                
                b.invoice_dt >= date'2016-03-01'        and 
                b.invoice_dt  < date'2017-03-01'        and
                
                b.discount_type in (1, 2, 3, 7, 12, 25, 36) and
                
                
                b.currency = 'RUR'
             or b.invoice_dt  < date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12, 36)
            )
          )
          
       or (b.commission_type = 6 and b.discount_type in (1, 2, 3, 7, 12))
          
       or (b.commission_type = 3 and b.discount_type in (17))
          
       or (b.commission_type = 4 and b.discount_type in (19))
          
       or (b.commission_type = 5 and b.discount_type = 12)
          
       or (b.commission_type in (10, 16) and 1=1)
          
       or (b.commission_type = 11 and b.discount_type_src = 29)
          
       or (b.commission_type = 17 and b.discount_type = 25)
          
       or (b.commission_type in (12, 13) and b.discount_type_src = 11)
       
       or (b.commission_type = 20 and b.discount_type in (1, 2, 3, 4, 24, 36, 12, 7, 37))

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
       b.discount_type,
       b.discount_type_src,
       b.is_agency_credit_line,
       b.total_sum*100/(100 + b.nds*b.nds_pct)      as invoice_total_sum,
       b.total_sum                                  as invoice_total_sum_w_nds,
       b.payment_control_type,
       oebs.doc_date,
       oebs.dt                                      as oebs_dt,
       oebs.payment_number,
       trunc(oebs.doc_date, 'MM')                   as from_dt,
       add_months(
        trunc(oebs.doc_date, 'MM'), 1)-1/84600      as till_dt,
       oebs.sum*100/(100 + b.nds*b.nds_pct)         as amt,
       oebs.sum                                     as amt_w_nds,
       
       
       
       sum(oebs.sum*100/(100 + b.nds*b.nds_pct))
            over(partition by b.invoice_id
                                             
                     order by oebs.doc_date, oebs.dt, oebs.payment_number, oebs.source_type)   as amt_by_invoice,
       
        
       sum(oebs.sum)
            over(partition by b.invoice_id
                     order by oebs.doc_date, oebs.dt, oebs.payment_number, oebs.source_type)   as payments_curr_by_invoice_w_nds
      from bo.x_v_comm_2013_base_src       b
      join bo.x_mv_oebs_receipts_2            oebs on oebs.invoice_eid = b.invoice_eid
                                                and oebs.doc_date is not null
                                                and oebs.doc_date >= date'2019-04-01'
     where b.commission_type = 60
       and b.discount_type in (7, 12, 13, 27, 37, 42)

)
--select * from s_payments;
, s_early_payment_pre as (
select d.*,
       
       case
        when payments_curr_by_invoice_w_nds >= invoice_total_sum_w_nds
        then 1
        else 0
       end                                          as is_fully_paid_pre,
       (
           case
            when d.is_agency_credit_line = 1
            then (
                select case
                        when a.act_count = 1
                         and d.doc_date <= a.payment_term_dt - 1
                          
                         and a.act_amount = d.payments_curr_by_invoice_w_nds
                        then 1
                        else 0
                       end
                  from (
                    
                    
                    select min(a.payment_term_dt)   as payment_term_dt,
                           count(distinct a.id)     as act_count,
                           sum(a.amount)            as act_amount
                      from bo.x_t_act         a
                     where a.invoice_id = d.invoice_id
                       and a.hidden < 4
                       ) a
                 )
            else 0
            end
       )                                            as is_early_payment_pre,
       (
            select case
                    when a.act_count = 1
                     and d.doc_date <= a.payment_term_dt - 1
                      
                     and a.act_amount = d.payments_curr_by_invoice_w_nds
                    then 1
                    else 0
                   end
              from (
                
                
                select min(a.payment_term_dt)   as payment_term_dt,
                       count(distinct a.id)     as act_count,
                       sum(a.amount)            as act_amount
                  from bo.x_t_act         a
                 where a.invoice_id = d.invoice_id
                   and a.hidden < 4
                       ) a

       )                                            as is_early_payment_true_pre
  from s_payments           d
    
 where not exists (
            select 1
              from bo.x_t_act a
             where a.is_loyal = 1
               and a.invoice_id = d.invoice_id
               and a.hidden < 4
               and a.dt >= date'2017-03-01'
               and d.invoice_dt >= date'2017-03-01'
       )
)
, s_early_payment_counted as (
select d.*,
       
       
       case
        when from_dt >= date'2018-03-01'
         and invoice_dt >= date'2018-03-01'
        then 1
        else 0
       end                                          as is_2018,
       count(decode(is_fully_paid_pre, 1, 1, null)) over(partition by invoice_id
                             order by doc_date, oebs_dt, payment_number) as
                        fully_paid_pos,
       count(decode(is_early_payment_pre, 1, 1, null)) over(partition by invoice_id
                             order by doc_date, oebs_dt, payment_number) as
                        early_payment_pos,
       count(decode(is_early_payment_true_pre, 1, 1, null)) over(partition by invoice_id
                             order by doc_date, oebs_dt, payment_number) as
                        early_payment_true_pos
  from s_early_payment_pre          d
)
select d."CONTRACT_EID",d."CONTRACT_ID",d."INVOICE_EID",d."INVOICE_ID",d."INVOICE_DT",d."INVOICE_TYPE",d."CONTRACT_FROM_DT",d."CONTRACT_TILL_DT",d."CURRENCY",d."NDS",d."PAYMENT_TYPE",d."COMMISSION_TYPE",d."DISCOUNT_TYPE",d."DISCOUNT_TYPE_SRC",d."IS_AGENCY_CREDIT_LINE",d."INVOICE_TOTAL_SUM",d."INVOICE_TOTAL_SUM_W_NDS",d."PAYMENT_CONTROL_TYPE",d."DOC_DATE",d."OEBS_DT",d."PAYMENT_NUMBER",d."FROM_DT",d."TILL_DT",d."AMT",d."AMT_W_NDS",d."AMT_BY_INVOICE",d."PAYMENTS_CURR_BY_INVOICE_W_NDS",d."IS_FULLY_PAID_PRE",d."IS_EARLY_PAYMENT_PRE",d."IS_EARLY_PAYMENT_TRUE_PRE",d."IS_2018",d."FULLY_PAID_POS",d."EARLY_PAYMENT_POS",d."EARLY_PAYMENT_TRUE_POS",
           
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
       
       count(1)
        over(partition by contract_id, from_dt,
                          payment_number)                    as payment_count_by_contract,
       count(1)
        over(partition by contract_id, from_dt,
                          invoice_id, payment_number)        as payment_count_by_invoice,
       
       decode(fully_paid_pos, 1, is_fully_paid_pre, 0)       as is_fully_paid,
       decode(early_payment_pos, 1, is_early_payment_pre, 0) as is_early_payment,
       decode(early_payment_true_pos, 1, is_early_payment_true_pre, 0) as is_early_payment_true
  from s_early_payment_counted d;
  
  
  
CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."X_V_AR_AGENCIES_STATS" ("AGENCY_ID", "SERVICE_ID", "SERVICE_ORDER_ID", "CLIENT_ID", "MONTH", "AMT") AS 
  with
  counting_date as (select date'2019-07-03' as dt from dual),

s_dates as (
    select add_months(trunc((select dt from counting_date), 'MM'), -1)     as from_dt,
           trunc((select dt from counting_date), 'MM') - 1/24/60/60        as till_dt
      from dual
)
, act_div as (
    select d.act_id, d.service_id, d.group_service_order_id,
           d.service_order_id, d.inv_amount,
           sum(order_amount)                        as order_amount
      from bo.x_v_group_order_act_div     d
      join s_dates                      dt on d.dt between dt.from_dt and dt.till_dt
     where d.service_id = 7
    group by d.act_id, d.service_id, d.group_service_order_id,
             d.service_order_id, d.inv_amount
)
select /*+ parallel(8) */
       a.client_id                                              as agency_id,
       o.service_id,
       nvl(ad.service_order_id, o.service_order_id)             as service_order_id,
       o.client_id,
       trunc(a.dt, 'MM')                                        as month,
       sum(nvl(
            (at.amount - at.amount_nds - at.amount_nsp)*cr.rate*
                ad.order_amount/ad.inv_amount,
            (at.amount - at.amount_nds - at.amount_nsp))
       )                                                        as amt
  from bo.x_t_act         a
  join bo.x_t_invoice     i on i.id = a.invoice_id
  join bo.x_t_act_trans   at on at.act_id = a.id
                          and at.netting is null
  join bo.x_t_consume     q  on q.id = at.consume_id
  join bo.x_t_order       o  on o.id = q.parent_order_id
  join bo.x_mv_currency_rate    cr on cr.cc = i.currency
                                  and cr.rate_dt = trunc(a.dt)
  join s_dates          d on a.dt between d.from_dt and d.till_dt
  left outer
  join act_div          ad on ad.act_id = a.id
                          and ad.service_id = o.service_id
                          and ad.group_service_order_id = o.service_order_id
 where a.hidden < 4
   and nvl(at.commission_type,
            nvl(i.commission_type, i.discount_type)) in (7)
 group by a.client_id, trunc(a.dt, 'MM'),
          o.service_id, nvl(ad.service_order_id, o.service_order_id),
          o.client_id;



  CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."X_V_AR_REWARDS_HISTORY" ("TP", "CONTRACT_ID", "CONTRACT_EID", "FROM_DT", "TILL_DT", "NDS", "CURRENCY", "DISCOUNT_TYPE", "REWARD_TYPE", "TURNOVER_TO_CHARGE", "REWARD_TO_CHARGE", "DELKREDERE_TO_CHARGE", "DKV_TO_CHARGE", "TURNOVER_TO_PAY", "TURNOVER_TO_PAY_W_NDS", "REWARD_TO_PAY", "REWARD_TO_PAY_SRC", "DELKREDERE_TO_PAY", "DKV_TO_PAY", "INSERT_DT") AS 
  select 'base'   as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay,         reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_base_src  where contract_id like '%00000%' union all
select 'prof'   as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay,         reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_prof_src   where contract_id like '%00000%'  union all
--union all
--select 'sprav'  as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay, null as reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_sprav_src  union all
--select 'auto'   as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay, null as reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_auto_src   union all
--select 'spec'   as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay, null as reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_spec_src   union all
--select 'autoru' as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay, null as reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_autoru_src union all
--select 'estate' as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay,         reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_estate_src union all
--select 'nonrez' as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay, null as reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_nonrez_src union all
--select 'imho'   as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay, null as reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_imho_src   union all
select 'kazakh' as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay,         reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_kazakh_src where contract_id like '%00000%' union all
select 'belarus'as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay, null as reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_belarus_src    where contract_id like '%00000%';
--select 'audio'  as tp, contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, turnover_to_charge, reward_to_charge, delkredere_to_charge, dkv_to_charge, turnover_to_pay, turnover_to_pay_w_nds, reward_to_pay, null as reward_to_pay_src, delkredere_to_pay, dkv_to_pay, insert_dt from bo.t_comm_audio_src;



  CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."X_V_AR_REWARDS" ("CONTRACT_ID", "CONTRACT_EID", "FROM_DT", "TILL_DT", "NDS", "CURRENCY", "DISCOUNT_TYPE", "REWARD_TYPE", "TURNOVER_TO_CHARGE", "REWARD_TO_CHARGE", "DELKREDERE_TO_CHARGE", "DKV_TO_CHARGE", "TURNOVER_TO_PAY", "TURNOVER_TO_PAY_W_NDS", "REWARD_TO_PAY", "REWARD_TO_PAY_SRC", "DELKREDERE_TO_PAY", "DKV_TO_PAY", "INSERT_DT", "TP") AS 
  with
 s_last_insert_dt as (
    select /*+ materialize */
           tp, from_dt, till_dt, max(insert_dt) as insert_dt
      from bo.x_v_ar_rewards_history
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
      from bo.x_v_ar_rewards_history                d
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
-- where type not like '%_inline';
;



create or replace view bo.x_v_ar_acts_hy as
with
counting_date as (select date'2019-07-03' as dt from dual),

s_hy AS (
    -- Показываем только полугодия предыдущего и текущего года.
    -- Так как, например, 2019-01-01 мы должны показать полугодие 2018-09-01 -- 2019-02-28
    -- Поэтому показывать полугодия тек. года - неправильно
    select d.dt from_dt, add_months(d.dt, 6) - 1/24/60/60 as till_dt
      from (
         select add_months(add_months(trunc((select  dt from counting_date), 'Y'), -10), 6*(level-1)) as dt
           from dual
        connect by level <= 4
           ) d
)
, s_acts as (
    select decode(c.cons_type, 2, c.contract_id, a.contract_id) as contract_id,
           decode(c.cons_type, 2, c.contract_eid, a.contract_eid) as contract_eid,
           a.commission_type,
           a.discount_type,
           a.amt,
           a.amt_w_nds,
           a.brand_id,
           h.from_dt as from_dt,
           h.till_dt as till_dt
    from bo.x_v_opt_2015_acts_src a
    join s_hy h
        on trunc((select  dt from counting_date), 'MM')-1 between h.from_dt and h.till_dt
       and a.from_dt between h.from_dt and h.till_dt
    left outer join bo.x_v_ar_consolidations_hy c
                on c.linked_contract_id = a.contract_id
    where a.commission_type in (1, 2, 8, 21, 23)
      and a.discount_type in (1, 2, 3, 7, 12, 36, 37, 40)
)
, s_bok as (
    select d.contract_id, d.from_dt,
           max(ratio)               as max_ratio
      from (
        select d.*,
               nvl(ratio_to_report(amt)
                  over (partition by d.contract_id, d.from_dt), 0) as ratio
          from (
            select d.contract_id,
                   d.brand_id                                        as client_id,
                   d.from_dt,
                   sum(decode(d.discount_type, 7, d.amt, null))      as amt
              from s_acts d
             group by d.contract_id,
                      d.brand_id,
                      d.from_dt
               ) d
           ) d
     group by d.contract_id, d.from_dt
)
, acts as (
    select
        a.contract_id,
        a.contract_eid,
        a.discount_type,
        a.commission_type,
        a.from_dt,
        sum(a.amt) as amt,
        sum(a.amt_w_nds) as amt_w_nds
    from s_acts a
    group by a.contract_id,
             a.contract_eid,
             a.discount_type,
             a.commission_type,
             a.from_dt

)

select a.contract_id,
       a.contract_eid,
       a.discount_type,
       a.commission_type,
       a.amt,
       a.amt_w_nds,
       sum(a.amt) over (partition by nvl(c.contract_id, a.contract_id),
           a.discount_type, a.commission_type) as amt_cons,
       case
          when a.discount_type = 7
           and round(b.max_ratio, 2) >= 0.7 then 1
          else 0
        end              as failed_bok
from acts a
left outer join bo.x_v_ar_consolidations_hy c
    on c.linked_contract_id = a.contract_id
left outer join s_bok b
    on b.contract_id = a.contract_id
    and b.from_dt = a.from_dt;




CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."X_V_AR_CONSOLIDATIONS_HY" ("CONTRACT_ID", "CONTRACT_EID", "AGENCY_ID", "COMMISSION_TYPE", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "START_DT", "FINISH_DT", "SIGN_DT", "LINKED_CONTRACT_ID", "LINKED_AGENCY_ID", "CONS_TYPE") AS 
  with
counting_date as (select date'2019-07-03' as dt from dual),  
  
s_hy AS (
    -- Показываем только полугодия предыдущего и текущего года.
    -- Так как, например, 2019-01-01 мы должны показать полугодие 2018-09-01 -- 2019-02-28
    -- Поэтому показывать полугодия тек. года - неправильно
    select d.dt from_dt, add_months(d.dt, 6) - 1/24/60/60 as till_dt
      from (
         select add_months(add_months(trunc((select dt from counting_date), 'Y'), -10), 6*(level-1)) as dt
           from dual
        connect by level <= 4
           ) d
)
, s_consolidate_cls_src as (
    select cp.contract2_id                          as contract_id,
           cp.collateral_id,
           cp.cl_dt                                 as start_dt,
           fd.value_dt                              as finish_dt,
           nvl(s.is_signed, s.is_faxed)             as sign_dt
      from bo.x_mv_contract_signed_attr_hist     cp
      join bo.x_mv_contract_signed_attr_hist     fd on fd.code = 'CONSOLIDATION_FINISH_DT'
                                                 and fd.collateral_id = cp.collateral_id
      join bo.x_t_contract_collateral             s on s.id = cp.collateral_id
      -- Учитываются только те дополнительные соглашения, которые действовали на
      -- последний день отчетного периода.
      -- BALANCE-28305: Дата окончания допника на объединение (CONSOLIDATION_FINISH_DT)
      -- хранится без секунд (в транкнутом виду). Добавляем секунды явно, чтобы
      -- проверка на вхождение в интервал проходила
    join s_hy                                   q on q.till_dt between cp.cl_dt
                                                                   and trunc(fd.value_dt) + 1 - 1/24/60/60
                                                  -- смотрим только прошлое полугодие
                                                 and trunc((select dt from counting_date), 'MM')-1 between q.from_dt and q.till_dt
     where cp.code = 'REWARD_CONSOLIDATION_PERIOD'
        -- период консолидации - полугодие
       and cp.value_num = 3
)
, s_consolidations as (
    select *
      from (
        select d.*,
               max(d.sign_dt) over (partition by d.contract_id) as sign_dt_last
          from s_consolidate_cls_src     d
           ) d
        -- используется последнее подписанное дополнительное соглашение с
        -- максимальной датой подписания дополнительного соглашения независимо от
        -- того, входит дата подписания в отчетный период или нет
     where sign_dt = sign_dt_last
)
-- История действия подписанных атрибутов
, s_attrs_src as (
    select value_num,
           update_dt,
           contract2_id                                         as contract_id,
           code,
           cl_dt                                                as from_dt,
           nvl(
            lead(cl_dt) over(partition by code, contract2_id
                                 order by stamp),
            add_months(trunc((select dt from counting_date), 'MM'), 3)
           ) -1/24/60/60                                        as till_dt
      from bo.x_mv_contract_signed_attr_hist
)
-- Подписанные атрибуты на конец пред. месяца
, s_attrs as (
    select *
      from s_attrs_src
        -- BALANCE-25145: смотрим значение атрибута на конец пред.месяца
     where trunc((select dt from counting_date), 'MM') - 1 between from_dt and till_dt
)
select cp.contract_id,
       -- сразу получаем для главного договора:
       -- аг-во, тип премии, чтобы удобнее делать подмену
       c.external_id    as contract_eid,
       c.client_id      as agency_id,
       rt.value_num     as commission_type,
       fr.dt            as contract_from_dt,
       tl.value_dt      as contract_till_dt,
       cp.start_dt,
       cp.finish_dt,
       cp.sign_dt,
       lc.key_num       as linked_contract_id,
       cl.client_id     as linked_agency_id,
       -- 1 - пропорционально
       -- 2 - на главный
       ct.value_num     as cons_type
  from s_consolidations                    cp
  join bo.x_mv_contract_signed_attr_hist     lc on lc.code = 'LINKED_REWARD_CONTRACTS'
                                             and lc.collateral_id = cp.collateral_id
                                             and lc.value_num = 1
  join bo.x_mv_contract_signed_attr_hist     ct on ct.code = 'REWARD_CONSOLIDATION_TYPE'
                                             and ct.collateral_id = cp.collateral_id
  join bo.x_t_contract2                      c  on c.id = cp.contract_id
  join bo.x_t_contract2                      cl on cl.id = lc.key_num
  join s_attrs                             rt on rt.contract_id = c.id
                                             and rt.code = 'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE'
  -- начало договора
  join bo.x_t_contract_collateral            fr on fr.contract2_id = c.id
                                             and fr.num is null
  -- конец договора
  join bo.x_mv_contract_signed_attr          tl on tl.contract_id = c.id
                                             and tl.code = 'FINISH_DT';

