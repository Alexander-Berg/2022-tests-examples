 with
  
   
  counting_date as (select date'2018-04-03' as dt from dual)
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

--select * from s_early_payment_pre ;
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

--select *from s_early_payment_counted ;
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
  )
  
--select * from s_payments_src;

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
,
--select  *from s_cur_dt;

s_brands as (
        select /*+ materialize */
               atr.key_num          as client_id,
               atr.start_dt         as collateral_dt,
               c.start_dt           as dt,
               trunc(nvl(c.finish_dt, date'2018-04-03'))
                -- BALANCE-28403: добавляем границу дня с секундами
                + 1 - 1/24/60/60            as finish_dt,
               min(c.MAIN_CLIENT_ID)     as main_client_id
          from xxxx_contract_signed_attr    atr
          -- BALANCE-27379: клиента в тех.связке начинаем учитывать, когда
            -- начинает действовать допник, в кот.он был добавлен
          join xxxx_ui_contract_apex        c on c.contract_id = atr.contract_id
                                               -- только живые тех.связки
--                                              and (c.finish_dt is null
--                                                or c.finish_dt >= trunc((select dt from counting_date), 'MM') - 1)
                                                -- на конец пред месяца
--                                             and d.end_prev_month between c.start_dt
--                                               and nvl(c.finish_dt, (select dt from counting_date))
                                                
         where atr.code = 'BRAND_CLIENTS'
         group by atr.key_num, atr.start_dt, c.start_dt, nvl(c.finish_dt, date'2018-04-03')
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
--        nvl((
--            select min(c.MAIN_CLIENT_ID)
--              from xxxx_contract_signed_attr    atr
--              join xxxx_ui_contract_apex        c on c.contract_id = atr.contract_id
--             where atr.code = 'BRAND_CLIENTS'
--               and atr.key_num = s.client_id
--                -- BALANCE-27379: клиента в тех.связке начинаем учитывать, когда
--                -- начинает действовать допник, в кот.он был добавлен
--               and atr.start_dt <= s.till_dt
--                -- BALANCE-27363: только живые тех.связки на конец пред месяца
--               and s.till_dt between c.start_dt and nvl(c.finish_dt, (select dt from counting_date))
--       ), s.client_id)           as brand_id
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
-- акты без лояльных клиентов
-- все акты по счетам
s_acts as (
    select b.*
      from s_acts_src   b
      -- 1 - Базовая 2015
           -- 8 - Базовая, регионы 2015
           -- 21 - Базовая СПб
     where b.commission_type in (1, 8, 21)
)
--select * from s_acts;
--select  *from s_acts order by invoice_id;

,
s_payments as (
    select b.*
      from s_payments_src b
      where b.commission_type in (1, 8, 21)
       and (
                -- Начиная с 2018-03-01 учитываем только полносью оплаченные счета
                is_2018 = 1 and is_fully_paid = 1
             or is_2018 = 0
              -- BALANCE-28169: показываем только переносы между разными счетами
              -- BALANCE-28622: откаты платежей по одному и тому же счету не показываем
              or amt < 0 and payment_count_by_invoice = 1
              -- есть платеж с таким же номером но по другому счету
              and payment_count_by_contract > 1
           )
)

--select * from s_payments;


,
--
---- ----------------------------------------------------------------------------
---- Базовое КВ (месячное)
---- ----------------------------------------------------------------------------
---- Складываем акты и оплаты за прошлый месяц
s_kv_src as (
    select d.*,
           case
               -- BALANCE-28798: решаем какие условия применять для расчета
                -- "к перечислению" по дате акта. новые условия, 2018
                when d.from_dt >= date'2018-03-01' 
                    -- BALANCE-22330: только для новых и пролонгированных
                     and d.contract_till_dt > date'2018-03-01'
                then 1
                else 0
            end                     as is_2018,
           case
                -- BALANCE-22195: решаем какие условия применять для расчета
                -- "к перечислению" по дате акта. новые условия, 2017
                when d.from_dt >= date'2017-03-01' 
                     -- BALANCE-22330: только для новых и пролонгированных
                     and d.contract_till_dt > date'2017-03-01'
                     and d.contract_till_dt <= date'2018-03-01'
                then 1
                else 0
            end                     as is_2017
      from (
    select d.contract_eid, d.contract_id,
           d.currency, d.discount_type, discount_type_src,
           contract_from_dt, contract_till_dt,
           d.nds, from_dt, till_dt, payment_type,
           is_agency_credit_line, 
           -- BALANCE-25700
           case
            when invoice_dt < date'2017-06-01'
            and d.invoice_type = 'prepayment'
            then 1
            else 0
           end                      as do_not_fill_src,
           sum(amt_acts)            as amt_to_charge,
           sum(amt_oebs_w_nds)      as amt_to_pay_w_nds,
           -- BALANCE-22195: считаем сумму оплат по старым условиям
           -- и по новым условиям раздельно, чтобы применить к ним разный
           -- процент
           nvl(sum(case when (invoice_dt < date'2017-03-01'
                          -- BALANCE-22330: если счет выставлен по не
                          -- продленному договору (неважно когда),
                          -- то так же считаем его оборотом по условиям 2016
                          or invoice_dt >= date'2017-03-01' and
                             contract_till_dt <= date'2017-03-01'
                             )
                          -- BALANCE-25535
                          -- BALANCE-25224: исключаем оплаты по предоплат.счетам
                         and not i.invoice_type = 'prepayment'
                    then amt_oebs
                    else 0
               end), 0)             as amt_to_pay_2016,
           nvl(sum(case when
                          -- BALANCE-22330: если счет выставлен по не
                          -- продленному договору (неважно когда),
                          -- то так же считаем его оборотом по условиям 2016
                             contract_till_dt <= date'2017-03-01'
                          -- BALANCE-25535, BALANCE-25224: только предоплата
                         and i.invoice_type = 'prepayment'
                    then amt_acts
                    else 0
               end), 0)             as amt_to_charge_2016,
           nvl(sum(case when invoice_dt >= date'2017-03-01'
                          -- BALANCE-22330: только для продленных договоров
                         and contract_till_dt > date'2017-03-01'
                          -- BALANCE-25535
                          -- BALANCE-25224: исключаем оплаты по предоплат.счетам
                         and not i.invoice_type = 'prepayment'
                    then amt_oebs
                    else 0
               end), 0)             as amt_to_pay_2017,
           nvl(sum(case when
                          -- BALANCE-22330: только для продленных договоров
                             contract_till_dt > date'2017-03-01'
                          -- BALANCE-25535, BALANCE-25224: только предоплата
                         and i.invoice_type = 'prepayment'
                    then amt_acts
                    else 0
               end), 0)             as amt_to_charge_2017,
           sum(amt_oebs)            as amt_to_pay
      from (
            select contract_eid, contract_id, currency, nds, discount_type,
                   contract_from_dt, contract_till_dt, discount_type_src,
                   from_dt, till_dt, payment_type, invoice_dt, invoice_id,
                   is_agency_credit_line, invoice_type,
                   amt  as amt_acts, amt_w_nds  as amt_acts_w_nds,
                   0    as amt_oebs, 0          as amt_oebs_w_nds
              from s_acts
             where act_dt between add_months(trunc((select dt from counting_date), 'MM'), -1)
                              and trunc((select dt from counting_date), 'MM') - 1/24/60/60
             union all
            select contract_eid, contract_id, currency, nds, discount_type,
                   contract_from_dt, contract_till_dt, discount_type_src,
                   from_dt, till_dt, payment_type, invoice_dt, invoice_id,
                   is_agency_credit_line, invoice_type,
                   0    as amt_acts, 0          as amt_acts_w_nds,
                   -- BALANCE-28169: для переносов надо учитывать сумму
                   -- корректировки (для is_2018 тут может быть сумма счета)
                   case when amt < 0 then amt else amt_ttl end as amt_oebs,
                   case when amt < 0 then amt_w_nds else amt_ttl_w_nds end as amt_oebs_w_nds
--                   amt_ttl  as amt_oebs, amt_ttl_w_nds  as amt_oebs_w_nds
              from s_payments
             where comiss_date between add_months(trunc((select dt from counting_date), 'MM'), -1)
                                and trunc((select dt from counting_date), 'MM') - 1/24/60/60
           )                        d
      join xxxx_invoice i  on i.inv_id = invoice_id
     group by contract_eid, d.contract_id, d.currency, payment_type,
              contract_from_dt, contract_till_dt,
              case when invoice_dt < date'2017-06-01'
                   and d.invoice_type = 'prepayment' then 1 else 0 end,
              is_agency_credit_line, discount_type_src,
              d.nds, d.discount_type, from_dt, till_dt
           ) d
)
--select * From s_kv_src order by contract_id;

,
-- Подготовка к контролю (заранее считаем бюджетообразующих клиентов)
s_kv_control_src as (
    select d.*,
           -- клиенты, по каждому из которых сумма актов за отчетный
           -- период составила не менее 1000 руб. Техническая связка нескольких
           -- ClientId считается одним клиентом.
           case
              when amt_rub >= 1000 and contract_till_dt <= date'2018-03-01'
              then brand_id
              when amt_rub >= 1000 and contract_till_dt  > date'2018-03-01'
                and discount_type = 7
              then brand_id
              else null
           end                                              as over1k_brand_id,
           -- Соотношение оборота по Директу по клиенту к
           -- обороту по договору (Агентства)
           nvl(ratio_to_report(amt_rub_direct)
              over (partition by contract_id, from_dt), 0) as ratio
          
      from (
        select contract_id, from_dt, till_dt, brand_id, client_count_2016,
               contract_from_dt, contract_till_dt, commission_type, discount_type,
               sum(amt_rub)                                  as amt_rub,
               sum(decode(discount_type, 7, amt_rub, 0))  as amt_rub_direct
          from (
        select d.*,
               count(distinct client_id) over
                (partition by contract_id, from_dt)         as client_count_2016
          from s_acts d
            -- BALANCE-25020
            -- BALANCE-22205: исключаем Медийку Я.Авто из оборота для проверок
            --                по Директу, Медийке, Справочнику
         where discount_type <> 25
               )
         group by contract_id, from_dt, till_dt, brand_id, client_count_2016,
                  contract_from_dt, contract_till_dt, commission_type,discount_type
           ) d
)
--select * from s_kv_control_src;

--select * from s_acts;
,

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
            select contract_id, from_dt, till_dt, client_count_2016,
                   contract_from_dt, contract_till_dt, commission_type,
                   sum(amt_rub)                 as amt_rub,
				   sum(amt_rub_direct)          as amt_rub_direct,
                   count(distinct over1k_brand_id)  as client_count,
                   round(max(ratio), 2)         as max_client_ratio_by_direct
              from s_kv_control_src
             group by contract_id, from_dt, till_dt, client_count_2016,
                      contract_from_dt, contract_till_dt, commission_type 
               ) d
           ) d
) 
--select  *From s_kv_control_pre;
,
-- Штрафы
--  - оборот по договору >= 200к
--  - клиентов >= 5
--  - нет клиентов с оборотом > 70% (округляется до целых процентов) по Директу
--  - нет нерезидентов
--  - только 1 валюта в счетах
s_kv_control as (
    select d.*,
           case
              when (
                    -- 2018
                    (contract_till_dt > date'2018-03-01'
                     -- если директа не крутилось, то эта проверка
                     -- работать не должна
                        and (client_count >= 5 or amt_rub_direct = 0)
                    )
                    -- 2017
                 or (client_count >= 5 and contract_till_dt > date'2017-03-01'
                                       and contract_till_dt <=date'2018-03-01')
                 or (client_count_2016 >= 5 and contract_till_dt <= date'2017-03-01')
                )
            and (
                 -- Если директа не было, то БОКа тоже не будет
                    is_there_boc = 0
                 or is_there_boc_1m_ago = 0
                )
           then 0
           else 1
            end as failed_bok,
           case
           when (    (  -- BALANCE-25262: по завершенным регионам, старые условия
                        amt_rub >= 100000
                    and contract_till_dt <= date'2017-03-01'
                    and commission_type = 8
                     )
                     -- Условия 2017
                  or (
                        amt_rub >= 200000
                    and contract_till_dt > date'2017-03-01'
                    and contract_till_dt <= date'2018-03-01'
                     )
                     -- Условия 2018
                  or (
                        amt_rub > decode(commission_type, 1, 500000, 200000)
                    and contract_till_dt > date'2018-03-01'
                     )
                )
           then 0
           else 1
            end as failed_amt,
           case
           when 
           (    
                  (  -- BALANCE-25262: по завершенным регионам, старые условия
                        amt_rub >= 100000
                    and contract_till_dt <= date'2017-03-01'
                    and commission_type = 8
                     )
                     -- Условия 2017
                  or 
                  (
                        amt_rub >= 200000
                    and contract_till_dt > date'2017-03-01'
                    and contract_till_dt <= date'2018-03-01'
                     )
                     -- Условия 2018
                  or 
                  (
                        amt_rub > decode(commission_type, 1, 500000, 200000)
                    and contract_till_dt > date'2018-03-01'
                     )
                )
            and 
            (
                    -- 2018
                    (contract_till_dt > date'2018-03-01' and (client_count >= 5 
                     -- если директа не крутилось, то эта проверка
                     -- работать не должна
                   or amt_rub_direct = 0)
                    )
                    -- 2017
                 or 
                 (client_count >= 5 and contract_till_dt > date'2017-03-01'
                                       and contract_till_dt <=date'2018-03-01')
                 or (client_count_2016 >= 5 and contract_till_dt <= date'2017-03-01')
                )
            and (
					-- Если директа не было, то БОКа тоже не будет
                    is_there_boc = 0
                 or is_there_boc_1m_ago = 0
                )
           then 0
           else 1
            end as failed
      from s_kv_control_pre d
)
--select  *from s_kv_control;
--select  case
----            when d.is_2017 = 0 and d.discount_type = 36 then 1
--            when d.is_2018 = 0 and d.discount_type = 36 then 1
--            else d.discount_type
--           end                          as discount_type, d.*
--           from s_kv_src d;

,
-- Сумма КВ без контроля акты <= оплат
s_kv_pre as (
    select d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           d.payment_type,
           d.nds, d.currency,
           d.discount_type,
           sum(d.turnover_to_charge)        as turnover_to_charge,
           sum(d.reward_to_charge)          as reward_to_charge,
           sum(d.turnover_to_pay)           as turnover_to_pay,
           sum(d.turnover_to_pay_w_nds)     as turnover_to_pay_w_nds,
           sum(decode(d.do_not_fill_src,
                0, d.reward_to_pay,
                0))                         as reward_to_pay_src,
           sum(d.reward_to_pay)             as reward_to_pay
     from (
     select d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           d.payment_type,
           d.nds, d.currency,
           d.do_not_fill_src,
           -- BALANCE-25071: Для непродленных договоров выводить расчет по 36 и
           -- 1 типу комиссии одной строкой
           case
             when d.discount_type = 36
             and d.is_2017 = 0
             and d.is_2018 = 0
            then 1
            else d.discount_type
           end                          as discount_type,
           sum(d.amt_to_charge)         as turnover_to_charge,
           sum(case
--                when d.is_2017 = 1 then
--                    d.amt_to_charge*decode(f.failed_amt, 1, 0,
                     when d.is_2017 = 1 or d.is_2018 = 1 then
                    d.amt_to_charge*decode(
                                        -- BALANCE-28798: в зависимости от года
                                        -- смотрим на разные признаки
                                        decode(d.is_2017, 1, f.failed, f.failed_amt),
                                        1, 0,
                                        -- если наказывать не за что, что считаем
                                        case
                                        when d.discount_type in (36)  then 0.15
                                        when d.discount_type in (1, 2, 3) then 0.13
                                        when d.discount_type in (7, 37) then
                                            case
                                             -- BALANCE-28739
                                                when d.discount_type = 7
                                                 and f.failed_bok = 1
                                                then 0
                                                -- для договоров по постоплате
                                                -- для счета по агентской кредитной
                                                -- линии — 7/12 %
                                                when payment_type = 3
                                                 and is_agency_credit_line = 1
                                                 then decode(d.discount_type,
                                                        7, 0.07,
                                                        37, 0.12)
                                                -- остальным — 8/13 %
                                                else decode(d.discount_type,
                                                        7, 0.08,
                                                        37, 0.13)
                                            end
                                        when d.discount_type in (12) then 0.08
                                        else 0
                                        end
                                    )
                -- BALANCE-22195: старые условия, 2016
                else d.amt_to_charge*decode(d.discount_type,
                         -- Авто.ру считаем по шкале
                         25, case
                                 when d.amt_to_charge >= 5000000 then 0.2
                                 when d.amt_to_charge >= 4000000 then 0.18
                                 when d.amt_to_charge >= 3000000 then 0.16
                                 when d.amt_to_charge >= 2000000 then 0.14
                                 when d.amt_to_charge >= 1000000 then 0.12
                                 when d.amt_to_charge >=   50000 then 0.10
                                 else 0
                             end,
                         -- Для остальных — 8%
                         decode(f.failed,
                             0, 0.08,
                             0))
               end)                     as reward_to_charge,
           sum(d.amt_to_pay)            as turnover_to_pay,
           sum(d.amt_to_pay_w_nds)      as turnover_to_pay_w_nds,
           -- BALANCE-19851
           -- Штрафовать по оплатам не должны
           -- BALANCE-22195: применяем старые и новые условия раздельно
           sum((d.amt_to_pay_2016 + decode(f.failed,    -- BALANCE-25801
                             0, d.amt_to_charge_2016, 0)) *            decode(d.discount_type,
                    -- Авто.ру считаем иначе
                    25, case
                            when d.amt_to_pay_2016 >= 5000000 then 0.2
                            when d.amt_to_pay_2016 >= 4000000 then 0.18
                            when d.amt_to_pay_2016 >= 3000000 then 0.16
                            when d.amt_to_pay_2016 >= 2000000 then 0.14
                            when d.amt_to_pay_2016 >= 1000000 then 0.12
                            -- BALANCE-22241: минимального порога по
                            -- оплатам нет, должны учесть всё. но
                            -- реально выплатим не более, чем по актам
                            else                              0.10
                        end,
                    -- Для остальных — 8%
                    0.08)
                +
                  (   d.amt_to_pay_2017 +
                    -- BALANCE-25801
                    -- BALANCE-28739
                                case
                      -- BALANCE-28798: если договор закончился, то штрафем по
                      -- старым условиям
                          when d.is_2017 = 1 and f.failed = 0            then d.amt_to_charge_2017
                          when d.is_2018 = 1 and d.discount_type =  7
                                             and f.failed_amt = 0
                                             and f.failed_bok = 0        then d.amt_to_charge_2017
                          when d.is_2018 = 1 and d.discount_type != 7
                                             and f.failed_amt = 0        then d.amt_to_charge_2017
                                      else 0
                                      end
                            ) * case
                                    when d.discount_type in (36) then 0.15
                                    when d.discount_type in (1, 2, 3) then 0.13
                                    when d.discount_type in (7, 37) then
                                        case
                                            -- для договоров по постоплате
                                            -- для счета по агентской кредитной
                                            -- линии —  7/12 %
                                            when payment_type = 3
                                             and is_agency_credit_line = 1
                                             then decode(d.discount_type,
                                                    7, 0.07,
                                                    37, 0.12)
                                            -- остальным — 8/13 %
                                            else decode(d.discount_type,
                                                    7, 0.08,
                                                    37, 0.13)
                                        end
                                    when d.discount_type in (12) then 0.08
                                    else 0
                                   end)         as reward_to_pay
      from s_kv_src         d
      -- BALANCE-19851
      left outer
      join s_kv_control     f on f.contract_id = d.contract_id
                             and f.from_dt = d.from_dt
                             and f.till_dt = d.till_dt
     group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt, d.payment_type,
              d.discount_type, d.currency, d.nds, d.is_2017, d.is_2018, d.do_not_fill_src
     union all
     --до-перечисляем 1% по Директу
    select d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           3                        as payment_type,
           d.nds, d.currency,
           -- этих денег еще не светилось, поэтому надо выводить в reward_to_pay_src
           0                        as do_not_fill_src,
           discount_type            ,
           0                        as turnover_to_charge,
           d.amt_by_invoice*0.01    as reward_to_charge,
           0                        as turnover_to_pay,
           0                        as turnover_to_pay_w_nds,
           d.amt_by_invoice*0.01    as reward_to_pay
      from s_payments_src       d
--         Дополнительные условия для начисления вознаграждения за досрочную
--         оплату: В отчетном периоде, в котором был выставлен счет, агентство
--         выполнило условия выплаты базовой премии для Директа. Если условия
--         выплаты базовой премии не выполняются, вознаграждение за досрочную
--         оплату счета не начисляется и не перечисляется
      join s_kv_control                 f on f.contract_id = d.contract_id
                                         and d.invoice_dt between f.from_dt
                                                              and f.till_dt
                                           -- BALANCE-28814: фейл по 37 и 7 по
                                           -- разным критериям в 2018 году
                                         and case
                                             when invoice_dt > date'2018-03-01'
                                             then decode(discount_type,
                                                    7, f.failed,
                                                    f.failed_amt)
                                             else f.failed
                                             end = 0
     where 
     d.is_early_payment = 1
       and d.comiss_date between add_months(trunc((select dt from counting_date), 'MM'), -1)
                          and trunc((select dt from counting_date), 'MM') - 1/24/60/60
        -- BALANCE-25161: только по новым счетам
       and d.invoice_dt >= date'2017-03-01'
           )  d
     group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt, d.payment_type,
              d.discount_type, d.currency, d.nds
)

--select  * from s_kv_pre; 

--select * from s_payments_src;
,
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
)
--select  * from s_kv01;
,


s_kv10_src as (
    select d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           d.nds, d.currency,
           d.turnover_to_charge,
           d.reward_to_charge,
           d.turnover_to_pay,
           d.turnover_to_pay_w_nds,
           d.reward_to_pay_src,
           d.reward_to_pay
      from s_kv_pre         d
        -- оплаты показываем только для постоплаты
     where payment_type = 3
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
           sum(d.reward_to_pay_src)     as reward_to_pay_src,
           sum(d.reward_to_pay_src)     as reward_to_pay
      from (
          select d.contract_id, d.contract_eid, d.reward_type,
                 d.from_dt, d.till_dt,
                 d.nds, d.currency,
                 d.turnover_to_charge,
                 d.reward_to_charge,
                 d.turnover_to_pay,
                 d.turnover_to_pay_w_nds,
                 case
                 -- Для ЛК платили от актов, поэтому надо учесть reward_to_pay
                 when d.discount_type = 71 
                 then d.reward_to_pay
                      -- если в это время был предоплатником, то платим от актов
                 when nvl(chpt.value_num, 3) = 2
                   -- Пример: contract_id = 239691, 2016-10. Станет постоплатой
                   -- только в след. месяце, но уже в 2016-10 есть 310 строка,
                   -- из которой надо достать реальную оплату, а не акты
                  and d.reward_type = 301
                      -- заполняется у предоплаты
                 then d.reward_to_pay
                 else d.reward_to_pay_src
                  end                       as  reward_to_pay_src
          from (
            -- BALANCE-25224: историю ищем совместную
            select d.contract_id, d.contract_eid, d.reward_type, d.discount_type,
                   d.from_dt, d.till_dt,
                   d.nds, d.currency,
                   d.turnover_to_charge,
                   d.reward_to_charge,
                   d.turnover_to_pay,
                   d.turnover_to_pay_w_nds,
                   d.reward_to_pay,
                   d.reward_to_pay_src
              from xxxx_commission_reward_2013 d
--              where id = 1
--             union all
--            select d.contract_id, d.contract_eid, d.reward_type, d.discount_type,
--                   d.from_dt, d.till_dt,
--                   d.nds, d.currency,
--                   d.turnover_to_charge,
--                   d.reward_to_charge,
--                   d.turnover_to_pay,
--                   d.turnover_to_pay_w_nds,
--                   d.reward_to_pay,
--                   d.reward_to_pay_src
--              from xxxx_commission_reward_2013_ d
              ) d
         left outer
         join s_changes_payment_type chpt on chpt.contract_id = d.contract_id
                                         and d.till_dt between chpt.from_dt
                                                           and chpt.till_dt
         where 
         d.contract_id in (
                    select contract_id from s_kv_pre
                     where payment_type = 3
               )
           and 
           d.reward_type in (310, 410, 510, 301, 401, 501)
            -- BALANCE-24877: исключаем расчеты за тек.период, если это
            --                не первый расчет за расчет.период
           and d.from_dt < add_months(trunc((select dt from counting_date), 'MM'), -1)
           ) d
  group by d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           d.nds, d.currency
)
  
  

--select * from s_kv10_src;

-- КВ с контролем, что оплат не более, чем актов
-- без разбивки по типам рекламы
,

 s_kv10 as (
    select contract_eid, contract_id, from_dt, till_dt,
           currency, nds,
           -- к перечислению (см. s_kv01)
           0                                    as turnover_to_charge,
           0                                    as reward_to_charge,
           -- к начислению
           turnover_to_pay,
           turnover_to_pay_w_nds,
           reward_to_pay_src,
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
                       d.from_dt, d.till_dt,
                       d.nds, d.currency,
                       sum(turnover_to_charge)      as turnover_to_charge,
                       sum(reward_to_charge)        as reward_to_charge,
                       sum(turnover_to_pay)         as turnover_to_pay,
                       sum(turnover_to_pay_w_nds)   as turnover_to_pay_w_nds,
                       sum(reward_to_pay_src)       as reward_to_pay_src,
                       sum(reward_to_pay)           as reward_to_pay
                  from s_kv10_src         d
                 group by d.contract_eid, d.contract_id,
                          d.from_dt, d.till_dt,
                          d.currency, d.nds
                   ) d
           ) s

        -- Показываем только предыдущий месяц
     where from_dt between add_months(trunc((select dt from counting_date), 'MM'), -1)
                       and trunc((select dt from counting_date), 'MM') - 1/24/60/60
)
--select * from s_kv10;
 
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
       turnover_to_pay,                          
       turnover_to_pay_w_nds,
          -- оборот к перечислению
       reward_to_pay ,                               -- к перечислению
       reward_to_pay_src                           -- к перечислению
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
               reward_to_pay as reward_to_pay_src,
               1            as reward_type
          from s_kv01
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
               reward_to_pay_src,
               10           as reward_type
          from s_kv10
       )
--where reward_to_charge<>0 or reward_to_pay<>0



  order by contract_id, from_dt, discount_type, currency, nds;
