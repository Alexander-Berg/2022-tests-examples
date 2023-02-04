CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."XXX_OPT_2015_PAYMENTS" ("CONTRACT_EID", "CONTRACT_ID", "INVOICE_EID", "INVOICE_ID", "INVOICE_DT", "INVOICE_TYPE", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "CURRENCY", "NDS", "PAYMENT_TYPE", "COMMISSION_TYPE", "DISCOUNT_TYPE", "DISCOUNT_TYPE_SRC", "IS_AGENCY_CREDIT_LINE", "INVOICE_TOTAL_SUM", "INVOICE_TOTAL_SUM_W_NDS", "DOC_DATE", "OEBS_DT", "PAYMENT_NUMBER", "FROM_DT", "TILL_DT", "AMT", "AMT_W_NDS", "AMT_BY_INVOICE", "PAYMENTS_CURR_BY_INVOICE_W_NDS", "IS_FULLY_PAID_PRE", "IS_EARLY_PAYMENT_PRE", "IS_2018", "FULLY_PAID_POS", "EARLY_PAYMENT_POS", "AMT_TTL_W_NDS", "AMT_TTL", "PAYMENT_COUNT_BY_CONTRACT", "PAYMENT_COUNT_BY_INVOICE", "IS_FULLY_PAID", "IS_EARLY_PAYMENT") AS 
 with
counting_date as (select date'2018-12-03' as dt from dual),


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
                                                -- BALANCE-17175
                                                (
                                                     -- только базовые/профы
                                                    c.contract_commission_type in (1, 2, 8, 21, 23) and
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

--select  *from s_changes_payment_type;
,
s_payments_temp as (
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
      from s_base        b
      join xxxx_oebs_cash_payment_test     oebs on oebs.invoice_id = b.invoice_id
                                                and oebs.comiss_date >= date'2015-03-01'
                                                and oebs.comiss_date is not null
   where 
         -- base, prof
          (
            b.commission_type in (1, 2, 8, 21, 23)
        and (
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
                    select min(a.ACT_PAYMENT_TERM_DT)   as payment_term_dt,
                           count(distinct a.act_id)     as act_count,
                           sum(a.amount)            as act_amount
                      from XXXX_NEW_COMM_CONTRACT_BASIC        a
                     where a.invoice_id = d.invoice_id
                       and a.hidden < 4
                       ) a
                 )
            else 0
            end
       )       as is_early_payment_pre
  from s_payments_temp          d
  where not exists (
            select 1
              from XXXX_NEW_COMM_CONTRACT_BASIC a
             where a.is_loyal = 1
               and a.invoice_id = d.invoice_id
               and a.hidden < 4
               and a.act_dt >= date'2017-03-01'
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
                        early_payment_pos
  from s_early_payment_pre          d
)

--select *from s_early_payment_counted;

select d."CONTRACT_EID",d."CONTRACT_ID",d."INVOICE_EID",d."INVOICE_ID",d."INVOICE_DT",d."INVOICE_TYPE",d."CONTRACT_FROM_DT",d."CONTRACT_TILL_DT",d."CURRENCY",d."NDS",d."PAYMENT_TYPE",d."COMMISSION_TYPE",d."DISCOUNT_TYPE",d."DISCOUNT_TYPE_SRC",d."IS_AGENCY_CREDIT_LINE",d."INVOICE_TOTAL_SUM",d."INVOICE_TOTAL_SUM_W_NDS",d."COMISS_DATE",d."OEBS_DT",d."PAYMENT_NUMBER",d."FROM_DT",d."TILL_DT",d."AMT",d."AMT_W_NDS",d."AMT_BY_INVOICE",d."PAYMENTS_CURR_BY_INVOICE_W_NDS",d."IS_FULLY_PAID_PRE",d."IS_EARLY_PAYMENT_PRE",d."IS_2018",d."FULLY_PAID_POS",d."EARLY_PAYMENT_POS",
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
       decode(early_payment_pos, 1, is_early_payment_pre, 0) as is_early_payment
  from s_early_payment_counted d
  
 
;