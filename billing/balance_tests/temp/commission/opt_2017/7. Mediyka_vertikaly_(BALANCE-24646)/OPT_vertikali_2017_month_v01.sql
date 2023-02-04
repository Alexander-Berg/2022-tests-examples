with
counting_date as (select date'2017-12-03' as dt from dual)
--select * from counting_date;
,

s_dates as (
    select /*+ materialize */
           level                                        as lvl,
           add_months(trunc((select dt from counting_date), 'mm'), -level)     as dt
      from dual
   connect by level <= bo.pk_comm.get_months_for_calc
)

--select  *from s_dates;
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
        -- Если ничего не найдено, то не ЛС вообще, поэтому не агентская кр.л.
        ), 0),
        0)                                      as is_agency_credit_line,
           c.contract_commission_type                 as commission_type
  from xxxx_new_comm_contract_basic c
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
                                                    (0, 1, 2, 3, 7, 8, 12, 15, 32)
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
--select * from s_base order by  invoice_id ;
,

s_payments_temp as (
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
           b.discount_type_src,
           b.is_agency_credit_line,
           oebs.comiss_date,
           trunc(oebs.comiss_date, 'MM')                 as from_dt,
           add_months(
            trunc(oebs.comiss_date, 'MM'), 1) -1/84600    as till_dt,
           oebs.oebs_payment*100/(100 + b.nds*b.nds_pct) as amt,
           oebs.oebs_payment                             as amt_w_nds,
       -- Сумма платежей нарастающим итогом по счету
       -- в хронологическом порядке
           sum(oebs.oebs_payment*100/(100 + b.nds*b.nds_pct))
            over(partition by b.invoice_id
                     order by oebs.comiss_date)        as amt_by_invoice,
            sum(oebs.oebs_payment)
            over(partition by b.invoice_id
                     order by oebs.comiss_date)        as amt_by_invoice_w_nds
      from s_base        b
      join xxxx_oebs_cash_payment_test     oebs on oebs.invoice_id = b.invoice_id
                                                and oebs.comiss_date >= date'2015-03-01'
                                                and oebs.comiss_date is not null
   where 
         -- base, prof
          (
            b.commission_type in (1, 2, 8)
        and (
               -- BALANCE-24516
                b.invoice_dt >= date'2017-03-01'        and
                b.discount_type in (1, 2, 3, 7, 12, 36)     and
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
             or b.invoice_dt  < date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
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
       or (b.commission_type = 10 and 1=1)
          -- audio
       or (b.commission_type = 11 and b.discount_type_src = 29)
         -- media verticals
       or (b.commission_type = 17 and b.discount_type = 25)
       
       
)
--select * from s_payments_temp;

,
s_payments_src as 
(
select d."CONTRACT_EID",d."CONTRACT_ID",d."INVOICE_EID",d."INVOICE_ID",d."INVOICE_DT",d."CONTRACT_FROM_DT",
d."CONTRACT_TILL_DT",d."CURRENCY",d."NDS",d."PAYMENT_TYPE",d."COMMISSION_TYPE",d."DISCOUNT_TYPE",
d."DISCOUNT_TYPE_SRC",d."IS_AGENCY_CREDIT_LINE",d."COMISS_DATE",d."FROM_DT",d."TILL_DT",d."AMT",
d."AMT_W_NDS",d."AMT_BY_INVOICE",d."AMT_BY_INVOICE_W_NDS",
       (
        -- Счет оплачен досрочно (не менее чем на 1 день ранее срока оплаты)
        -- Счет оплачен полностью. За частичную оплату счета (независимо от срока
        --    частичной оплаты) вознаграждение за досрочную оплату не начисляется
        --    и не перечисляется
            case
            when d.is_agency_credit_line = 1
            then (
                select case
                        when 
                        a.act_count = 1
                         and 
                         d.comiss_date <= a.payment_term_dt - 1
                         and 
                         --BALANCE-24853
                         a.act_amount = d.amt_by_invoice_w_nds
                        then 1
                        else 0
                       end
                  from (
                    -- Т.к. ЛС, то по счету на оплату, должен быть ровно 1 акт
                    -- Сумма счета и акта должна совпадать
                    select min(a.ACT_PAYMENT_TERM_DT)   as payment_term_dt,
                           count(distinct a.id)     as act_count,
                           sum(a.amount)            as act_amount
                      from XXXX_NEW_COMM_CONTRACT_BASIC        a
                     where a.invoice_id = d.invoice_id
                       and a.hidden < 4
                       ) a
                 )
            else 0
            end
       )         as is_early_payment
  from s_payments_temp          d)
  
-- select * from s_payments_src; 
,
-- ----------------------------------------------------------------------------
-- основная выборка по актам
-- ----------------------------------------------------------------------------
-- все акты по счетам
s_acts_temp as (
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
       -- Проверяем, что по договору всегда была -- 1 валюта и 1 НДС
            count(distinct b.nds)      over (partition by b.contract_id) as nds_count,
           count(distinct b.currency) over (partition by b.contract_id) as currency_count,
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
                                          and xxx.is_loyal = 0
      join xxxx_currency_rate              cr on cr.cc = b.currency
                                              and cr.rate_dt = trunc(xxx.act_dt)
      left outer
      join (
        -- Если субклиент входит в несколько брендов,
        -- то берем главного клиента с минимальным id
        select atr.key_num          as client_id,
               min(c.MAIN_CLIENT_ID)     as main_client_id
          from xxxx_contract_signed_attr    atr
          join xxxx_ui_contract_apex        c on c.contract_id = atr.contract_id
                                               -- только живые тех.связки
                                              and (c.finish_dt is null
                                                or c.finish_dt >= trunc((select dt from counting_date), 'MM') - 1)
         where atr.code = 'BRAND_CLIENTS'
         group by atr.key_num
       )                                brand on brand.client_id = xxx.client_id
     where b.commission_type in (1, 2, 3, 4, 5, 6, 8, 10, 11, 12, 13, 16, 17)
   and (
          -- base, prof
          (
            b.commission_type in (1, 2, 8)
        and (
              
                -- BALANCE-24516: новые условия применимы только актов нового
                -- фин.года при условиия, что договор продлен
                xxx.act_dt >= date'2017-03-01' and b.contract_till_dt >= date'2017-03-01' and b.discount_type in (1, 2, 3, 7, 12,36)
                -- BALANCE-22085
                -- В актах по новым и продленным договорам 2016:
                --  * отбрасываем маркет (11)
                --  * включаем Авто (25)
                --2016 год
             or  xxx.act_dt >= date'2017-03-01' and b.contract_till_dt  < date'2017-03-01' and b.discount_type in (1, 2, 3, 7, 12, 25, 36)
             --BALANCE-24843
             -- BALANCE-24734: добавляем 36 тип, чтобы старые счета учесть
             or  xxx.act_dt >= date'2016-03-01' and xxx.act_dt < date'2017-03-01'   and b.discount_type in (1, 2, 3, 7, 12, 25)
             -- 2015 год
             or  xxx.act_dt >= date'2016-03-01' and b.contract_till_dt  < date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
             or  xxx.act_dt  < date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
            )
          )
          -- ua
       or (b.commission_type = 6 and(
                   -- BALANCE-22914
                   xxx.act_dt >= date'2016-04-01'
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
       or (b.commission_type = 10 and 1=1)
          -- audio
       or (b.commission_type = 11 and b.discount_type_src = 29)
          -- market
       or (b.commission_type in (12, 13) and b.discount_type_src = 11)
       -- media verticals
       or (b.commission_type = 17 and b.discount_type = 25)
       
       )
)
--select  *from s_acts_temp;
,

s_acts_src as ( select * from s_acts_temp
 where (
            (
            -- BALANCE-22203
            -- BALANCE-22331
            -- BALANCE-23542
            -- BALANCE-22578
            -- в новых условиях оставляем только одна валюта
            -- BALANCE-24627: оптимизация (from_dt -> act_dt)
                act_dt >= date'2016-03-01'
            and currency_count = 1
            and nds_count = 1
            )
            or
            (
                act_dt < date'2016-03-01'
            )
        )
           -- BALANCE-24852
       and (
                currency = 'RUR'
            or  commission_type = 6
          )
)
--select  *from s_acts_src;
,


-- акты без лояльных клиентов
-- все акты по счетам
s_acts as (
    select b.*
      from s_acts_src   b
     where b.commission_type in (17)
)
--select * from s_acts;
,

s_acts_last_month as (
    select b.*
      from s_acts b
     where act_dt between add_months(trunc((select dt from counting_date), 'MM'), -1)
                      and trunc((select dt from counting_date), 'MM') - 1/24/60/60
)
--select * From s_acts_last_month;
--select trunc((select dt from counting_date), 'MM') - 1/24/60/60 from dual;
,
s_payments as (
    select b.*
      from s_payments_src b
     where b.commission_type in (17)
)

--select * from s_payments;

, 


s_kv_src as (
    select contract_eid, contract_id,
           currency, nds, payment_type, discount_type,
           from_dt, till_dt,
           sum(amt_acts)            as amt_to_charge,
           sum(amt_oebs_w_nds)      as amt_to_pay_w_nds,
           sum(amt_oebs)            as amt_to_pay
      from (
            select contract_eid, contract_id, currency, nds, discount_type,
                   from_dt, till_dt, payment_type,
                   amt  as amt_acts, amt_w_nds  as amt_acts_w_nds,
                   0    as amt_oebs, 0          as amt_oebs_w_nds
              from s_acts_last_month
             union all
            select contract_eid, contract_id, currency, nds, discount_type,
                   from_dt, till_dt, payment_type,
                   0    as amt_acts, 0          as amt_acts_w_nds,
                   amt  as amt_oebs, amt_w_nds  as amt_oebs_w_nds
              from s_payments
             where comiss_date between add_months(trunc((select dt from counting_date), 'MM'), -1)
                                and trunc((select dt from counting_date), 'MM') - 1/24/60/60
           )
     group by contract_eid, contract_id, currency, discount_type,
              nds, from_dt, till_dt, payment_type
)
--select * from s_kv_src;

, s_kv_control as (
    select d.*,
           case
           when amt_rub >= 50000
           then 0
           else 1
            end as failed
      from (
                select contract_id,
                       sum(amt_rub)             as amt_rub
                  from s_acts_last_month
                 group by contract_id
           ) d
)

--select * from s_kv_control;
, 


s_kv_pre as (
    select d.contract_id, d.contract_eid, d.discount_type,
           d.from_dt, d.till_dt, d.payment_type,
           d.nds, d.currency,
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
     group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt,
              d.currency, d.nds, d.payment_type, d.discount_type
)
-- КВ с контролем, что оплат не более, чем актов
--select * from s_kv_pre;


, 


s_kv as (
    select contract_eid, contract_id, from_dt, till_dt,
           currency, nds, discount_type,
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
            select 
                  d.*
                   ,(select l.payment_type from s_kv_pre l
                     where l.contract_id = d.contract_id
                   )                                    as payment_type
                   ,
                   (select l.discount_type from s_kv_pre l
                     where l.contract_id = d.contract_id
                   )                                    as discount_type,
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
                       sum(d.reward_to_pay_src)     as reward_to_pay
                  from xxxx_commission_reward_2013 d
                 where d.contract_id in (
                            select contract_id from s_kv_pre
                       )
                   and d.reward_type in (310, 410, 510, 301, 401, 501)
                    -- BALANCE-24877: исключаем расчеты за тек.период, если это
                    --                не первый расчет за расчет.период
                   and d.from_dt < add_months(trunc((select dt from counting_date), 'MM'), -1)
              group by d.contract_id, contract_eid,
                       d.from_dt, d.till_dt,
                       d.nds, d.currency
                   ) d
           ) s
)

--select * from s_kv;


select distinct s."CONTRACT_ID",s."CONTRACT_EID",s."FROM_DT",s."TILL_DT",
s."NDS",s."CURRENCY",s."DISCOUNT_TYPE",s."REWARD_TYPE",
s."TURNOVER_TO_CHARGE",s."REWARD_TO_CHARGE",s."TURNOVER_TO_PAY",s."TURNOVER_TO_PAY_W_NDS",s."REWARD_TO_PAY",
s."REWARD_TO_PAY_SRC"
  from (
    select contract_id,     contract_eid,
           from_dt,         till_dt,
           nds,             currency,
           discount_type,
           300 + reward_type            as reward_type,
           -- к начислению
           sum(turnover_to_charge)      as turnover_to_charge,
           sum(reward_to_charge)        as reward_to_charge,
           0                            as delkredere_to_charge,
           0                            as dkv_to_charge,
           -- к перечислению
           sum(turnover_to_pay)         as turnover_to_pay,
           sum(turnover_to_pay_w_nds)   as turnover_to_pay_w_nds,
           sum(reward_to_pay)           as reward_to_pay,
           sum(reward_to_pay_src)       as reward_to_pay_src,
           0                            as delkredere_to_pay,
           0                            as dkv_to_pay
      from (
         select 
         
         contract_id,     contract_eid,
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
                        from 
                -- результирующий запрос
              ( 
              
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
                       reward_to_pay_src,                          -- к перечислению
                       reward_to_pay
                  from (
                        select contract_eid, contract_id,
                               from_dt, till_dt,
                               discount_type,
                               currency, nds,
                               turnover_to_charge,
                               reward_to_charge,
                               turnover_to_pay,
                               turnover_to_pay_w_nds,
                               reward_to_pay_src,
                               reward_to_pay,
                               1            as reward_type
                          from s_kv   
                        )
                       
              )
                       
         )   
 group by contract_id,     contract_eid,
              from_dt,         till_dt,
              nds,             currency,
              discount_type,   reward_type
)            s
join s_dates d on d.dt between s.from_dt and s.till_dt

  order by contract_id, from_dt, discount_type, currency, nds;          
