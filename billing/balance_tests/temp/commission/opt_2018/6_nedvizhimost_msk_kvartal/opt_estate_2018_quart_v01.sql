--сам квартальный расчет начинается с 808 строки
with
counting_date as (select date'2018-04-03' as dt from dual),   

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
--select * from s_base;
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

--select * from s_changes_payment_type;
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
                     order by oebs.comiss_date, oebs.dt,oebs.payment_number)       as payments_curr_by_invoice_w_nds
      from s_base        b
      join xxxx_oebs_cash_payment_test     oebs on oebs.invoice_id = b.invoice_id
                                                and oebs.comiss_date >= date'2015-03-01'
                                                and oebs.comiss_date is not null
   where 
         -- base, prof
          (
            b.commission_type in (1, 2, 8, 21)
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
       )           as is_early_payment_pre
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

-- select * from s_early_payment_pre where invoice_id = 100354;
 
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

--select *from s_early_payment_counted where invoice_id = 100354;
,
s_payments_src as (
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
       -- BALANCE-26692: отображаем только первое появление признака
       decode(fully_paid_pos, 1, is_fully_paid_pre, 0)       as is_fully_paid,
       decode(early_payment_pos, 1, is_early_payment_pre, 0) as is_early_payment
  from s_early_payment_counted d
  )
  
--select * from s_payments_src;
,
-- ----------------------------------------------------------------------------
-- основная выборка по актам
-- ----------------------------------------------------------------------------
---- все акты по счетам
--s_dts as (
--    select trunc((select dt from counting_date), 'MM') - 1 as end_prev_month
--      from dual
--)
----select * from  s_dts;
----select * from xxxx_contract_signed_attr;
--,
--
--s_brands as (
--        select /*+ materialize */
--               atr.key_num          as client_id,
--               min(c.MAIN_CLIENT_ID)     as main_client_id
--          from xxxx_contract_signed_attr    atr
--          -- BALANCE-27379: клиента в тех.связке начинаем учитывать, когда
--            -- начинает действовать допник, в кот.он был добавлен
--          join s_dts                         d on atr.start_dt <= d.end_prev_month
--          join xxxx_ui_contract_apex        c on c.contract_id = atr.contract_id
--                                               -- только живые тех.связки
----                                              and (c.finish_dt is null
----                                                or c.finish_dt >= trunc((select dt from counting_date), 'MM') - 1)
--                                                -- на конец пред месяца
--                                             and d.end_prev_month between c.start_dt
--                                               and nvl(c.finish_dt, (select dt from counting_date))
--                                                
--         where atr.code = 'BRAND_CLIENTS'
--         group by atr.key_num
--)

--select  *from s_brands;
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
        nvl((
            select min(c.MAIN_CLIENT_ID)
              from xxxx_contract_signed_attr    atr
              join xxxx_ui_contract_apex        c on c.contract_id = atr.contract_id
             where atr.code = 'BRAND_CLIENTS'
               and atr.key_num = s.client_id
                -- BALANCE-27379: клиента в тех.связке начинаем учитывать, когда
                -- начинает действовать допник, в кот.он был добавлен
               and atr.start_dt <= s.till_dt
                -- BALANCE-27363: только живые тех.связки на конец пред месяца
               and s.till_dt between c.start_dt and nvl(c.finish_dt, (select dt from counting_date))
       ), s.client_id)           as brand_id
       from s_ar_acts_src s
        
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
,


s_acts as (
    select b.*
      from s_acts_src   b
     where b.commission_type = 16
)

--select * from s_acts;
,

s_payments as (
    select b.*
      from s_payments_src b
     where b.commission_type = 16
)
--select * from s_payments;

,
-- ----------------------------------------------------------------------------
-- Базовое КВ (месячное)
-- ----------------------------------------------------------------------------
-- Акты и оплаты за прошлый месяц
s_kv_src as (
    select contract_eid, contract_id,
           currency, nds, payment_type,
           invoice_type, invoice_dt,
           contract_from_dt, contract_till_dt,
           from_dt, till_dt,
           sum(amt_acts)            as amt_to_charge,
           sum(amt_oebs_w_nds)      as amt_to_pay_w_nds,
           sum(amt_oebs)            as amt_to_pay
      from (
            select contract_eid, contract_id, currency, nds, discount_type,
                   from_dt, till_dt, payment_type,
                   contract_from_dt, contract_till_dt,
                   invoice_type, invoice_dt,
                   amt  as amt_acts, amt_w_nds  as amt_acts_w_nds,
                   0    as amt_oebs, 0          as amt_oebs_w_nds
              from s_acts

             where act_dt between add_months(trunc((select dt from counting_date), 'MM'), -1)
                              and trunc((select dt from counting_date), 'MM') - 1/24/60/60
             union all
            select contract_eid, contract_id, currency, nds, discount_type,
                   from_dt, till_dt, payment_type,
                   contract_from_dt, contract_till_dt,
                   invoice_type, invoice_dt,
                   0    as amt_acts, 0          as amt_acts_w_nds,
                   amt  as amt_oebs, amt_w_nds  as amt_oebs_w_nds
              from s_payments
             where comiss_date between add_months(trunc((select dt from counting_date), 'MM'), -1)
                                and trunc((select dt from counting_date), 'MM') - 1/24/60/60
           )
     group by contract_eid, contract_id, currency,
     invoice_type, invoice_dt,
     contract_from_dt, contract_till_dt,
     nds, from_dt, till_dt, payment_type
)

--select * from s_kv_src;
,
s_kv_control as (
    select d.*,
           case
           when amt_rub >= 50000
            and nds_count = 1
            and currency_count = 1
            and client_count >= 3
           then 0
           else 1
            end as failed
      from (
        select d.contract_id, d.from_dt, d.till_dt,
               nds_count, currency_count,
               count(distinct client_id)                    as client_count,
               sum(amt_rub)                                 as amt_rub
          from s_acts   d
         group by d.contract_id, d.from_dt, d.till_dt,
                  d.nds_count, d.currency_count
           ) d
)


--select  * from s_kv_control;
,
s_pcts as (
     select date'2000-01-01' as from_dt, date'2018-01-01'-1/86400 as till_dt, 0.25 as pct
       from dual
      union all
     select date'2018-01-01' as from_dt, date'2020-01-01'-1/86400 as till_dt, 0.18 as pct
       from dual
)
,

s_kv_pre as (
    select d.contract_id, contract_eid,
           d.from_dt, d.till_dt, d.payment_type,
           d.nds, d.currency,
           sum(d.amt_to_charge)         as turnover_to_charge,
           sum(decode(f.failed,
                0, d.amt_to_charge*pa.pct,
                0))                     as reward_to_charge,
           sum(d.amt_to_pay)            as turnover_to_pay,
           sum(d.amt_to_pay_w_nds)      as turnover_to_pay_w_nds,
           -- BALANCE-19851
           -- Штрафовать по оплатам не должны
          
           sum(
            case
              -- BALANCE-26669:
              -- предоплата, новый счет, договор продлен - платим от актов по новым условиям
            when d.invoice_type = 'prepayment'
             and d.invoice_dt >= date'2018-01-01'
             and d.contract_till_dt > date'2018-01-01'
             and d.from_dt >= date'2018-01-01'
            then d.amt_to_charge*pa.pct
              -- Постоплата, новый счет, договор продлен - платим от денег по новым условиям
            when d.invoice_type <> 'prepayment'
             and d.invoice_dt >= date'2018-01-01'
             and d.contract_till_dt > date'2018-01-01'
             and d.from_dt >= date'2018-01-01'
            then d.amt_to_pay*pa.pct
              -- все остальные случаи - платим от денег по старым условиям
              --    - договор не проден, либо
              --    - старый счет, либо
              --    - отчетный период: < 2018-01-01
              -- Важно: по старым предоплатным счетам не должны учитывать акты
              -- вместо оплат, т.к. эти оплаты уже есть в истории в предыдущих
              -- периодах.
            else d.amt_to_pay*pp.pct
            end)                        as reward_to_pay
      from s_kv_src         d
      -- BALANCE-26669
      -- проценты для актов
     join s_pcts           pa on d.from_dt between pa.from_dt and pa.till_dt
      -- проценты для оплат (смотрим на дату счета)
     join s_pcts           pp on d.invoice_dt between pp.from_dt and pp.till_dt
      left outer
      join s_kv_control     f on f.contract_id = d.contract_id
                             and f.from_dt = d.from_dt
                             and f.till_dt = d.till_dt
     group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt,
              d.currency, d.nds, d.payment_type
)
--select * from s_kv_pre;
,
-- КВ с контролем, что оплат не более, чем актов
s_kv as (
    select 
contract_eid, contract_id, from_dt, till_dt,
           currency, nds,
           -- к перечислению
           turnover_to_charge,
           reward_to_charge,
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
                select d.contract_id, contract_eid,
                       d.from_dt, d.till_dt,
                       d.nds, d.currency,
                       d.turnover_to_charge,
                       d.reward_to_charge,
                       d.turnover_to_pay,
                       d.turnover_to_pay_w_nds,
                       d.reward_to_pay
                  from s_kv_pre         d
                 union all
                    -- BALANCE-24627
                    -- История, чтобы раздать "долги", если таковые остались
                    -- с прошлых месяцев
                select d.contract_id, contract_eid,
                       d.from_dt, d.till_dt,
                       d.nds, d.currency,
                       sum(d.turnover_to_charge)    as turnover_to_charge,
                       sum(d.reward_to_charge)      as reward_to_charge,
                       sum(d.turnover_to_pay)       as turnover_to_pay,
                       sum(d.turnover_to_pay_w_nds) as turnover_to_pay_w_nds,
                       sum(case
                              -- если в это время был предоплатником, то платим от актов
                         when nvl(chpt.value_num, 3) = 2
                           -- Пример: contract_id = 239691, 2016-10. Станет постоплатой
                           -- только в след. месяце, но уже в 2016-10 есть 310 строка,
                           -- из которой надо достать реальную оплату, а не акты
                          and d.reward_type = 301
                              -- заполняется у предоплаты
                         then d.reward_to_pay
                         else d.reward_to_pay_src
                          end)                      as  reward_to_pay
                  from XXXX_COMMISSION_REWARD_2013 d
                  left outer
                  join s_changes_payment_type chpt on chpt.contract_id = d.contract_id
                                                  and d.till_dt between chpt.from_dt
                                                                    and chpt.till_dt
                 where d.contract_id in (
                            select contract_id from s_kv_pre
                       )
                    -- BALANCE-24877: исключаем расчеты за тек.период, если это
                    --                не первый расчет за расчет.период
                   and d.from_dt < add_months(trunc((select dt from counting_date), 'MM'), -1)
				and d.reward_type in (310, 410, 510, 301, 401, 501)
              group by d.contract_id, contract_eid,
                       d.from_dt, d.till_dt,
                       d.nds, d.currency
                   ) d
           ) s
)
--select * from s_kv;

 ,

----------квартал
s_q_plans_src as (
    select contract_id                                         as contract_id,
           max(start_dt)                                       as cl_last_dt
      from Xxxx_Contract_Signed_Attr
     where code = 'AUTORU_Q_PLAN'
        -- дата начала действия ДС — не позднее последнего дня прошного месяца
       and start_dt< trunc((select dt from counting_date), 'MM')
     group by contract_id
)
--select  * from s_q_plans_src;


-- последний подписанный план на конец про
, 
s_q_plans as (
    select a.contract_id                                       as contract_id,
           a.start_dt                                          as plan_dt,
           a.value_num                                         as plan
      from Xxxx_Contract_Signed_Attr  a
      join s_q_plans_src                    l on l.contract_id = a.contract_id
                                             and l.cl_last_dt = a.start_dt
     where a.code = 'AUTORU_Q_PLAN'
)

--select  *from s_q_plans;

, 
s_quarters AS (
    select d.dt from_dt, add_months(d.dt, 3) - 1/86400 as till_dt
      from (
         select add_months(date'2018-01-01', 3*(level-1)) as dt
           from dual
        connect by level <= 20
           ) d
)
--select * from s_quarters;
--select add_months(q.from_dt, 1) + 1 from s_quarters q;
--select  *from s_kv_pre;
, s_acts_last_q as (
    select 
    a.contract_id, a.contract_eid,
           q.from_dt, q.till_dt,
           a.nds, a.currency,
           p.plan,
           sum(a.amt_rub)           as amt_rub,
           sum(a.amt_w_nds)         as amt_w_nds_rub
      from s_acts       a
      join s_quarters   q on a.act_dt between q.from_dt and q.till_dt
                          -- только прошлый квартал
                         and trunc((select dt from counting_date), 'MM') -1/86400 between q.from_dt and q.till_dt
                          -- только, если закончился квартал
                         and to_char((select dt from counting_date), 'MM') in ('04', '07', '10', '01')
          left outer
      join s_q_plans     p on p.contract_id = a.contract_id
         -- BALANCE-27742: проверяем, что была премия по месячному расчету
        -- Особенность в том, что при расчете кваратала, одновременно,
        -- расчитывается и ежемесячная премия. То есть, 2 месяца из квартала
        -- мы смотрим из фактов, а последний месяц проверяем online
      where
             2 = (
                select 
                count(r.from_dt)
                  from Xxxx_Commission_Reward_2013      r
                 where 
--                    r.tp = 'estate' and
                   r.reward_type in (301)
                   and r.contract_id = a.contract_id
--                   смотрим только первые 2 месяца кваратала (+1 на всякий)
                   and r.from_dt between q.from_dt  and add_months(q.from_dt, 1) + 1
                   and r.reward_to_charge > 0
              )
          and 
          1= (
          select count(r.from_dt)
          from s_kv       r
          where r.contract_id = a.contract_id
          and r.reward_to_charge > 0
          and r.from_dt = add_months(q.from_dt, 2)
           )
     group by a.contract_id, a.contract_eid,
           q.from_dt, q.till_dt,
           a.nds, a.currency, p.plan
)
--select * from s_acts_last_q;

, s_q as (
    select a.*,
           case
            when a.amt_rub >= a.plan
            then (a.amt_rub - a.plan)*0.3
            else 0
           end as reward
      from s_acts_last_q    a
)
,
-- результирующий запрос
s_opt_2017_estate_msk as (select contract_id,
       contract_eid,
       from_dt,
       till_dt,
       nds,
       currency,
       97                   as discount_type,
       1                    as reward_type,
       turnover_to_charge,                          -- оборот к начислению
       reward_to_charge,                            -- к начислению
       turnover_to_pay_w_nds,
       turnover_to_pay,                             -- оборот к перечислению
       reward_to_pay_src,                           -- к перечислению
       reward_to_pay                                -- к перечислению
  from s_kv
  union all
select contract_id,
       contract_eid,
       from_dt,
       till_dt,
       nds,
       currency,
       97                   as discount_type,
       20                   as reward_type,
       amt_rub              as turnover_to_charge,
       reward               as reward_to_charge,
       amt_w_nds_rub        as turnover_to_pay_w_nds,
       amt_rub              as turnover_to_pay,
       reward               as reward_to_pay_src,
       reward               as reward_to_pay
  from s_q
      
)
,

s_dts as (
    select /*+ materialize */
           level                                                     as lvl,
           add_months(trunc((select dt from counting_date), 'mm'), -level)     as dt
      from dual
   connect by level <= bo.pk_comm.get_months_for_calc
)

--select  *from s_dts;
--select  *from s_acts;
--select * from s_opt_2017_estate_msk;
select distinct
    s.contract_id,
       s.contract_eid,
       s.from_dt,
       s.till_dt,
       s.nds,
       s.currency,
       s.discount_type,
       s.reward_type,
       s.turnover_to_charge,
       s.reward_to_charge,
       s.turnover_to_pay,
       s.turnover_to_pay_w_nds,
       s.reward_to_pay,
       --null                     as delkredere_to_charge,
       --null                     as delkredere_to_pay,
       --null                     as dkv_to_charge,
       --null                     as dkv_to_pay,
   
     
       
       s.reward_to_pay_src
  from (
    select contract_id,     contract_eid,
           from_dt,         till_dt,
           nds,             currency,
           discount_type,
           300 + reward_type    as reward_type,
           -- к начислению
           turnover_to_charge,
           reward_to_charge,
           -- к перечислению
           turnover_to_pay_w_nds,
           turnover_to_pay,
           reward_to_pay_src,
           reward_to_pay
      from s_opt_2017_estate_msk
     )       s
join s_dts d on d.dt between s.from_dt and s.till_dt
order by from_dt, contract_id, discount_type, currency, nds;
