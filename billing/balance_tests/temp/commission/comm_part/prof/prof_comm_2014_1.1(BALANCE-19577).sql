  with
--
-- опорные даты
--
s_dates as (
    select date'2013-03-01'   as new_acts_dt,
        -- начало фин. года
           date'2014-03-01'   as fin_year_dt
      from dual
),
-- ----------------------------------------------------------------------------
-- основная выборка для базовой комиссии за 13/14 фин. год
-- ----------------------------------------------------------------------------
s_base as (
    select distinct
           xxxx.contract_eid                             as contract_eid,
           xxxx.contract_id                              as contract_id,
           xxxx.contract_from_dt                         as contract_from_dt,
           xxxx.contract_till_dt                         as contract_till_dt,
           xxxx.invoice_eid                              as invoice_eid,
           xxxx.invoice_id                               as invoice_id,
           xxxx.invoice_dt                               as invoice_dt,

           xxxx.currency                                 as currency,
           xxxx.nds                                      as nds, 
           xxxx.nds_pct                                  as nds_pct,
           -- BALANCE-17175
           decode(
            nvl(xxxx.comm_type, xxxx.discount_type),
            22, 1,
            nvl(xxxx.comm_type, xxxx.discount_type)
           )                                             as discount_type,
           xxxx.payment_type                             as payment_type, 
           xxxx.commission_payback_pct                   as commission_payback_pct,
           xxxx.commission_type                          as commission_type,   -----договора
           xxxx.comm_type                                as comm_type,         -----обычный
           xxxx.loyal_clients                             as loyal_clients 
		   ,decode(xxxx.endbuyer_id, 0, null, xxxx.endbuyer_id)  as endbuyer_id
			,case
      --         BALANCE-19564: S-счета (нерезиденты) всегда
         -- включаем в ком. часть
        when xxxx.paysys_id in (1025, 1026, 1027)
        then 0
        when (decode(xxxx.endbuyer_id, 0, null, xxxx.endbuyer_id)) is null
          or xxxx.endbuyer_inn = xxxx.agency_inn
        then 1
        else 0
       end                                      as is_opt,
       -- BALANCE-19449
       -- Были ли акты до 2015-01-01
       -- по счету, созданному до 2015-01-01
       case when 
		( select count(1)
			from xxxx_new_comm_contract_basic b
				where xxxx.invoice_id = b.invoice_id
               and b.hidden < 4
               and b.act_dt < date'2015-01-01'
               and xxxx.invoice_dt < date'2015-01-01'
        ) >0 then 1 else 0
       end                                       as is_there_old_acts   
      from (
      select distinct
           x.contract_eid                             as contract_eid,
           x.contract_id                              as contract_id,
           x.contract_from_dt                         as contract_from_dt,
           x.contract_till_dt                         as contract_till_dt,
           x.invoice_eid                              as invoice_eid,
           x.invoice_id                               as invoice_id,
           x.invoice_dt                               as invoice_dt,
           x.act_dt                                   as act_dt,  
           x.currency                                 as currency,
           x.nds                                      as nds,    
           x.nds_pct                                  as nds_pct,
           x.discount_type    					              as discount_type,
           x.payment_type                             as payment_type, 
		       x.commission_payback_pct                   as commission_payback_pct,   
           x.contract_commission_type                 as commission_type,
           x.commission_type                          as comm_type,         -----обычный
           x.loyal_client                             as loyal_clients,
		   x.endbuyer_id                           		as endbuyer_id,
           x.endbuyer_inn                              as endbuyer_inn,
           x.agency_inn                              as agency_inn,
           x.paysys_id                                    as paysys_id
       from xxxx_new_comm_contract_basic  x
       )   xxxx                                
      where (
              -- BALANCE-17175
              (
                  -- только счета, созданные в 2014
                  xxxx.invoice_dt >= date'2014-03-01' and
                  -- только базовые/профы
                  xxxx.commission_type in (47, 48) and
                  -- то учитываем еще и код 22
                  nvl(xxxx.comm_type,
                      xxxx.discount_type) in
                  (1, 2, 3, 7, 11, 12, 14,19, 22)
              )
              or
              (
                  -- иначе, как ранее
                  nvl(xxxx.comm_type,
                      xxxx.discount_type) in
                  (1, 2, 3, 7, 11, 12, 14,19)
              )
            )
)

--select * from s_base order by contract_eid , discount_type;
,
-- ----------------------------------------------------------------------------
-- основная выборка по актам
-- ----------------------------------------------------------------------------
s_comm_prof_2014_acts as (
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
           nvl(xxx.is_loyal, 0)                       as loyal_clients,
           xxx.client_id                                  as client_id,
           xxx.act_dt                                     as act_dt,
           trunc(xxx.act_dt, 'MM')                        as from_dt,
           add_months(
           trunc(xxx.act_dt, 'MM'), 1) - 1/84600         as till_dt,
           xxx.amount                                     as amt_w_nds,
           xxx.amount-xxx.amount_nds-xxx.amount_nsp       as amt
      from s_base        b
      join xxxx_new_comm_contract_basic     xxx on b.invoice_id = xxx.invoice_id
                                              and xxx.hidden < 4
                                              and xxx.act_dt >= (select fin_year_dt from s_dates)
     where b.commission_type = 48
       and b.contract_till_dt > (select fin_year_dt from s_dates)
-- BALANCE-19159
        -- начиная с 01.01.2015 исключаем акты, по счетам которых
        -- не указан конечный получатель
       and (
            -- начиная с 2015, исключаем оптовую часть
            -- и оставляем только те счета, в которых есть конечный покупатель
            (
                xxx.act_dt >= date'2015-01-01'
             and b.is_opt = 0
            )
            or
            -- до 2015 года ничего не делаем
            (
                xxx.act_dt < date'2015-01-01'
            )
           )
),


s_comm_prof_2014_acts_region as (
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
           nvl(xxx.is_loyal, 0)                       as loyal_clients,
           xxx.client_id                                  as client_id,
           xxx.act_dt                                     as act_dt,
           trunc(xxx.act_dt, 'MM')                        as from_dt,
           add_months(
           trunc(xxx.act_dt, 'MM'), 1) - 1/84600         as till_dt,
           xxx.amount                                     as amt_w_nds,
           xxx.amount-xxx.amount_nds-xxx.amount_nsp       as amt
      from s_base        b
      join xxxx_new_comm_contract_basic     xxx on b.invoice_id = xxx.invoice_id
                                              and xxx.hidden < 4
                                              and xxx.act_dt >= date'2014-01-01'
      where b.commission_type = 48
       and b.contract_till_dt > (select fin_year_dt from s_dates)
-- BALANCE-19159
        -- начиная с 01.01.2015 исключаем акты, по счетам которых
        -- не указан конечный получатель
       and (
            -- начиная с 2015, исключаем оптовую часть
            -- и оставляем только те счета, в которых есть конечный покупатель
            (
                xxx.act_dt >= date'2015-01-01'
            -- BALANCE-19429
            and b.is_opt = 0
            )
            or
            -- до 2015 года ничего не делаем
            (
                xxx.act_dt < date'2015-01-01'
            )
           )
)
,


s_comm_prof_2014_acts_new_wolc as (
select "CONTRACT_EID","CONTRACT_ID","INVOICE_EID","INVOICE_ID","INVOICE_DT","CONTRACT_FROM_DT","CONTRACT_TILL_DT","CURRENCY","NDS","PAYMENT_TYPE","DISCOUNT_TYPE","LOYAL_CLIENTS","CLIENT_ID","ACT_DT","FROM_DT","TILL_DT","AMT_W_NDS","AMT"
      from s_comm_prof_2014_acts d
        -- не было актов по счетам до 03-2013
     where not exists (select 1 from xxxx_new_comm_contract_basic old_act
                        where old_act.invoice_first_act < (select new_acts_dt  from s_dates)
                          and old_act.invoice_id = d.invoice_id
                          and old_act.hidden < 4
                          and rownum = 1)
        -- не было оплат по договору до 03-2013
       and not exists (select 1 from xxxx_new_comm_contract_basic fpay
                        where fpay.INVOICE_FIRST_PAYMENT < (select new_acts_dt  from s_dates)
                          and fpay.invoice_eid = d.invoice_eid
                          and rownum = 1)
        -- BALANCE-15196
        -- BALANCE-16330
       and not (d.loyal_clients = 1 and d.discount_type = 7)
)
--select * from s_comm_prof_2014_acts_new_wolc;
,


-- «старые акты» за 2013, без ЛК
s_acts_old_wo_lc as (
    select *
      from s_comm_prof_2014_acts d
        -- были акты по счетам до 03-2013
     where (exists (select 1 from xxxx_new_comm_contract_basic old_act
                        where old_act.invoice_first_act < (select new_acts_dt  from s_dates)
                          and old_act.invoice_id = d.invoice_id
                          and old_act.hidden < 4
                          and rownum = 1)
        -- были оплаты по договору до 03-2013
        or exists (select 1 from xxxx_new_comm_contract_basic fpay
                        where fpay.INVOICE_FIRST_PAYMENT < (select new_acts_dt  from s_dates)
                          and fpay.invoice_eid = d.invoice_eid
                          and rownum = 1)
           )
        -- BALANCE-15196
        -- BALANCE-16330
       and not (d.loyal_clients = 1 and d.discount_type = 7)
)
,
-- ----------------------------------------------------------------------------
-- основная выборка по оплатам
-- ----------------------------------------------------------------------------
s_payments as (
    select b.contract_eid,
           b.contract_id,
           b.invoice_eid,
           b.invoice_id,
           b.invoice_dt,
           b.currency,
           b.nds,
           b.payment_type,
           b.discount_type,
           b.loyal_clients,
           b.commission_payback_pct,
           oebs.comiss_date,
           trunc(oebs.comiss_date, 'MM')                as from_dt,
           add_months(
            trunc(oebs.comiss_date, 'MM'), 1)-1/84600   as till_dt,
           oebs.oebs_payment*100/(100 + b.nds*b.nds_pct)         as amt,
           oebs.oebs_payment                                     as amt_w_nds
      from s_base         b
      -- платежи
     join xxxx_oebs_cash_payment_test             oebs on oebs.invoice_id = b.invoice_id
                                                 -- BALANCE-14988
                                                and oebs.comiss_date >= date'2014-03-01'
                                                 -- BALANCE-15631
                                                and oebs.comiss_date is not null
     where b.commission_type = 48
       and b.contract_till_dt > (select fin_year_dt from s_dates)
 -- BALANCE-19159
        -- начиная с 01.01.2015 исключаем акты, по счетам которых
        -- не указан конечный получатель
       and (
            -- начиная с 2015, исключаем оптовую часть
            -- и оставляем только те счета, в которых есть конечный покупатель
            (
                oebs.comiss_date >= date'2015-01-01'
           -- BALANCE-19429
            and b.is_opt = 0
			-- BALANCE-19449: если оплата пришла за период до 2015 года,
            -- то смотреть на конечного покупателя не надо.
            and b.is_there_old_acts = 0
            )
            -- BALANCE-19449: если оплата после 2015 и есть
            -- акты по по этому счету до 2015, то такую оплату надо учесть

            or

            (
               oebs.comiss_date >= date'2015-01-01'
            and b.is_there_old_acts != 0
            )
            -- до 2015 года ничего не делаем
            or oebs.comiss_date < date'2015-01-01'
           )
)

--select * from s_payments;
,


s_payments_new as (
    select *
      from s_payments d
        -- не было актов по счетам до 03-2013
     where not exists (select 1 from xxxx_new_comm_contract_basic old_act
                        where old_act.invoice_first_act < (select new_acts_dt  from s_dates)
                          and old_act.invoice_id = d.invoice_id
                          and old_act.hidden < 4
                          and rownum = 1)
        -- не было оплат по договору до 03-2013
       and not exists (select 1 from xxxx_new_comm_contract_basic fpay
                        where fpay.INVOICE_FIRST_PAYMENT < (select new_acts_dt  from s_dates)
                          and fpay.invoice_eid = d.invoice_eid
                          and rownum = 1)
)
,


s_payments_old_wo_lc as (
    select *
      from s_payments d
        -- были акты по счетам до 03-2013
     where ( exists (select 1 from xxxx_new_comm_contract_basic old_act
                        where old_act.invoice_first_act < (select new_acts_dt  from s_dates)
                          and old_act.invoice_id = d.invoice_id
                          and old_act.hidden < 4
                          and rownum = 1)
        -- были оплаты по договору до 03-2013
        or exists (select 1 from xxxx_new_comm_contract_basic fpay
                        where fpay.INVOICE_FIRST_PAYMENT < (select new_acts_dt  from s_dates)
                          and fpay.invoice_eid = d.invoice_eid
                          and rownum = 1)
           )
        -- BALANCE-15196
       and not (d.loyal_clients = 1 and d.discount_type = 7)
)
,
-- ----------------------------------------------------------------------------
-- КВ Медийка, Маркет и Справочник
-- ----------------------------------------------------------------------------
-- считаем отношение оборота клиента к общему обороту по договору
-- получаем договор и максимальное отношение оборотов (клиент уже не важен)
-- BALANCE-16952
s_kv_contract_ratio as (
    select d.contract_id, d.from_dt, d.till_dt,
           round(max(ratio)*100) as ratio
      from (
        select d.*,
               nvl(ratio_to_report(amt_rub)
                  over (partition by d.contract_id, d.from_dt), 0) as ratio
          from (
            select d.contract_id,
                   d.client_id,
                   d.from_dt,
                   d.till_dt,
                   sum(decode(d.discount_type, 7, d.amt*cr.rate, null))  as amt_rub
              from s_comm_prof_2014_acts    d
              join xxxx_currency_rate            cr on cr.cc = d.currency
                                                      and cr.rate_dt = trunc(d.act_dt)
             group by d.from_dt, d.till_dt,
                      d.contract_id,
                      d.client_id
               ) d
           ) d
     group by d.contract_id, d.from_dt, d.till_dt
)
,
-- отношение оборота клиента к обороту по договору
-- за последние 3 месяца одной строкой
-- BALANCE-16952
s_kv_contract_ratio_last_3m as (
    select d.contract_id,
           d.from_dt,
           d.ratio,
           d.till_dt,
           -- учитываем, что в каком-то месяце может не быть оборота
           case
            when add_months(from_dt, -1) = from_dt_1m_ago then ratio_1m_ago
            when add_months(from_dt, -1) = from_dt_2m_ago then ratio_2m_ago
            else 0
           end                                          as ratio_1m_ago,
           case
            when add_months(from_dt, -2) = from_dt_1m_ago then ratio_1m_ago
            when add_months(from_dt, -2) = from_dt_2m_ago then ratio_2m_ago
            else 0
           end                                          as ratio_2m_ago
      from (
        select d.contract_id,
               d.from_dt,
               d.till_dt,
               d.ratio,
               lag(d.from_dt, 1) over (partition by contract_id
                                            order by from_dt)   as from_dt_1m_ago,
               lag(d.from_dt, 2) over (partition by contract_id
                                            order by from_dt)   as from_dt_2m_ago,
               lag(d.ratio, 1, 0) over (partition by contract_id
                                            order by from_dt)   as ratio_1m_ago,
               lag(d.ratio, 2, 0) over (partition by contract_id
                                            order by from_dt)   as ratio_2m_ago
          from s_kv_contract_ratio d
           ) d
)
,
-- только те договора, по которым 3 месяца подряд
-- были клиенты с оборотом более 70%
-- BALANCE-16952
s_comm_prof_2014_kv_over_70 as (
    select d."CONTRACT_ID",d."FROM_DT",d."RATIO",d."TILL_DT",d."RATIO_1M_AGO",d."RATIO_2M_AGO", 1 as failed
      from s_kv_contract_ratio_last_3m d
     where ratio >= 70
       and ratio_1m_ago >= 70
       and ratio_2m_ago >= 70
       and from_dt >= date'2014-05-01'
)
,

s_kv as (
    select d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           nds, currency,
           discount_type,
           sum(amt)                 as amt_to_charge,
           null                     as amt_to_pay,
           null                     as amt_to_pay_w_nds,
           sum(amt*decode(f.failed,
                1, 0.0001, 0.08))   as reward_to_charge,
           sum(amt*0.02)            as delkredere_to_charge,
           sum(decode(payment_type,
                -- предоплата
                2, amt*decode(f.failed, 1, 0.0001, 0.08),
                -- постоплата
                3, null))           as reward_to_pay,
           sum(decode(payment_type,
                -- предоплата
                2, amt*0.02,
                -- постоплата
                3, null))           as delkredere_to_pay
      from s_comm_prof_2014_acts_new_wolc        d
        -- BALANCE-16952
      left outer
      join s_comm_prof_2014_kv_over_70 f on f.contract_id = d.contract_id
                                           and f.from_dt = d.from_dt
                                           and f.till_dt = d.till_dt
     where d.discount_type in (1, 2, 3, 11, 12, 14)
     group by contract_eid, d.contract_id, d.from_dt, d.till_dt,
              discount_type, currency, nds
)
,
-- ----------------------------------------------------------------------------
-- КВ, старое
-- ----------------------------------------------------------------------------
s_kv_old as (
    select contract_id, contract_eid, from_dt, till_dt,
           discount_type, currency, nds,
           sum(amt_to_charge)                               as amt_to_charge,
           sum(amt_to_pay)                                  as amt_to_pay,
           sum(amt_to_pay_w_nds)                            as amt_to_pay_w_nds,
           sum(case
                  when discount_type in (
                          7, 11, 12, 14, 1, 2, 3) then amt_to_charge*0.1
                                                  else 0
              end)                                          as reward_to_charge,
           sum(case
                  when discount_type in (
                          7, 11, 12, 14, 1, 2, 3) then amt_to_pay*0.1
                                                  else 0
              end)                                          as reward_to_pay,
           sum(case
                  when discount_type in (
                          7, 11, 12, 14, 1, 2, 3) then amt_to_charge*0.03
                                                  else 0
              end)                                          as delkredere_to_charge,
           sum(case
                  when discount_type in (
                          7, 11, 12, 14, 1, 2, 3) then amt_to_pay*0.03
                                                  else 0
              end)                                          as delkredere_to_pay,
           sum(case
                  when discount_type in (12)      then amt_to_charge*0.12
                  when discount_type in (1, 2, 3) then amt_to_charge*0.03
                                                  else 0
              end)                                          as dkv_to_charge,
           sum(case
                  when discount_type in (12)      then amt_to_pay*0.12
                  when discount_type in (1, 2, 3) then amt_to_pay*0.03
                                                  else 0
              end)                                          as dkv_to_pay
        from (
                  select contract_id, contract_eid,
                         from_dt, till_dt, discount_type, currency, nds,
                         amt                                as amt_to_charge,
                         0                                  as amt_to_pay,
                         0                                  as amt_to_pay_w_nds
                    from s_acts_old_wo_lc
                   union all
                  select contract_id, contract_eid,
                         from_dt, till_dt, discount_type, currency, nds,
                         0                                  as amt_to_charge,
                         decode(payment_type,
                          -- предоплата
                          2, 0,
                          -- постоплата
                          3, amt) as amt_to_pay,
                         decode(payment_type,
                          -- предоплата
                          2, 0,
                          -- постоплата
                          3, amt_w_nds)                     as amt_to_pay_w_nds
                    from s_payments_old_wo_lc
             ) d
     group by contract_eid, contract_id, from_dt, till_dt,
              discount_type, currency, nds
),
-- ----------------------------------------------------------------------------
-- Региональная программа
-- ----------------------------------------------------------------------------
s_region_src as (
    select b.contract_eid, b.contract_id,
           trunc(b.from_dt, 'Q')                    as from_dt,
           add_months(
            trunc(b.till_dt, 'Q'), 3) - 1/84600     as till_dt,
           'RUR'                                    as currency,
           1                                        as nds,
           count(distinct b.client_id)              as client_cnt
      from (
        select b.*,
                -- Оборот по клиенту за квартал
               sum(amt_direct) over(partition by
                    b.contract_id, trunc(b.from_dt, 'Q'),
                    client_id)                      as client_amt_in_Q,
               -- считаем, сколько раз данный клиент появляется в течении
               -- текущего квартала. Должен быть в каждом месяце квартала,
               -- то есть, ровно 3 раза
               count(1) over(partition by
                    b.contract_id, trunc(b.from_dt, 'Q'),
                    client_id)                      as client_cnt_in_Q
          from (
            select b.contract_eid, b.contract_id,
                   b.from_dt, b.till_dt,
                   b.client_id,
                   sum(decode(b.discount_type, 7, b.amt, null)) as amt_direct
              from s_comm_prof_2014_acts_region                       b
                -- Региональная программа 2013
              join xxxx_contract_signed_attr   a on a.code = 'SUPERCOMMISSION_BONUS'
                                                 and a.key_num = 140
                                                 and a.contract_id = b.contract_id
             where b.act_dt between b.contract_from_dt
                                -- дата окончания договора, обычно, в виде 01.04.2014,
                                -- то есть, границей справа должно быть 31.03.2014 23:59:59
                                and b.contract_till_dt - 1/84600
             group by b.contract_eid, b.contract_id,
                      b.from_dt, b.till_dt,
                      b.client_id
            -- учитываются только клиенты агентства, у которых в течение
            -- каждого календарного месяца расчётного периода сумма
            -- оказанных услуг за Директ была не менее 5000 руб. без НДС
                -- BALANCE-16017: с 2013-10 15к смотрим только за квартал.
                -- а помесячно проверяем лишь наличие активности
            having sum(decode(b.discount_type, 7, b.amt, null)) >= 1
               ) b
           ) b
        -- в обычном квартале должно быть ровно 3 записи по каждмоу клиенту
     where client_cnt_in_Q = 3
        -- BALANCE-16017, BALANCE-16754: проверяем 15к за квартал
       and client_amt_in_Q >= 15000
     group by b.contract_eid, b.contract_id,
              trunc(b.from_dt, 'Q'),
              add_months(trunc(b.till_dt, 'Q'), 3)
),


s_region as (
    select contract_eid, contract_id,
           from_dt, till_dt,
           currency, nds,
           case
            when client_cnt < 15                        then 0
            when client_cnt >= 15 and client_cnt <= 25  then 50000
            when client_cnt >= 26 and client_cnt <= 40  then 70000
            when client_cnt >= 41 and client_cnt <= 75  then 85000
            when client_cnt >= 76                       then 130000
           end                    as reward
      from s_region_src
),

-- ----------------------------------------------------------------------------
-- ЛК
-- ----------------------------------------------------------------------------
s_lc_acts_src as (
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
           nvl(xxx.is_loyal, 0)                       as loyal_clients,
           xxx.client_id                              as client_id,
           xxx.act_dt                                     as act_dt,
           trunc(xxx.act_dt, 'MM')                        as from_dt,
           add_months(
            trunc(xxx.act_dt, 'MM'), 1) - 1/84600         as till_dt,
           xxx.amount                                as amt_w_nds,
           xxx.amount-xxx.amount_nds-xxx.amount_nsp    as amt
      from s_base       b
      join xxxx_new_comm_contract_basic   xxx  on b.invoice_id = xxx.invoice_id
                                              and xxx.hidden < 4
                                              and xxx.act_dt >= (select
                                                add_months(fin_year_dt, -1) from s_dates)
      
     where b.commission_type = 48
       and b.contract_till_dt > (select fin_year_dt from s_dates)
        -- BALANCE-19159
        -- начиная с 01.01.2015 исключаем акты, по счетам которых
        -- не указан конечный получатель
       and (
            -- начиная с 2015, исключаем оптовую часть
            -- и оставляем только те счета, в которых есть конечный покупатель
            (
                xxx.act_dt >= date'2015-01-01'

            -- BALANCE-19429
            and b.is_opt = 0
            )
            or
            -- до 2015 года ничего не делаем
            (
                xxx.act_dt < date'2015-01-01'
            )
           )
)
,
-- лояльные акты
-- BALANCE-16000
s_lc_acts as (
    select d.*,
           lc.collateral_first_dt             as lc_start_dt,
           d.amt*cr.rate                as amt_rub,
           lc.lc_turnover                  as lc_monthly_amt
      from s_lc_acts_src         d
      join xxxx_loyal_clients_contr_attr   lc on lc.contract_id = d.contract_id
                                              and lc.client_id = d.client_id
                                              -- Расчет применяем до тех пор, пока не
                                              -- истечет срок действия (в ДС)
                                              and d.act_dt between lc.collateral_first_dt
                                                               and lc.collateral_end_dt
      join xxxx_currency_rate            cr on cr.cc = d.currency
                                              and cr.rate_dt = trunc(d.act_dt)
     where d.loyal_clients = 1
       and d.discount_type in (7)
)
,


s_lc_by_clients as (
    select d.contract_id, d.contract_eid,
           d.payment_type,
           d.from_dt, d.till_dt,
           d.nds, d.currency,
           d.client_id,
           d.lc_monthly_amt,
           d.lc_start_dt,
           d.discount,
           sum(amt_to_charge)       as amt_to_charge,
           sum(amt_to_charge_rub)   as amt_to_charge_rub
      from (
            -- Логика такая же, как и в s_kv_direct
            -- разница лишь в том, что используется
            -- s_lc_acts вместо v_comm_prof_2014_acts_newwolc
            select contract_id, contract_eid,
                   -- BALANCE-16000:
                   -- для предоплатников выводим акты, как оплаты
                   -- поднимаем признак на самый верх, чтобы там
                   -- показывать суммы только для предоплатников
                   payment_type,
                   from_dt, till_dt,
                   nds, currency,
                   d.client_id,
                   d.lc_monthly_amt,
                   d.lc_start_dt,
                   case
                    when dd.client_avg_discount <12 or dd.client_avg_discount is null then 0.08
                    when dd.client_avg_discount >= 12 and dd.client_avg_discount < 18 then 0.06
                    when dd.client_avg_discount >= 18                      then 0.05
                   end                              as discount,
                   amt                              as amt_to_charge,
                   amt_rub                          as amt_to_charge_rub
              from s_lc_acts                        d
              left outer
              join xxxx_client_discount_m   dd on dd.client_id = d.client_id
                                                      and dd.dt = d.from_dt
           ) d
     group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt,
              d.currency, d.nds, d.client_id, d.discount, d.payment_type,
              d.lc_monthly_amt, d.lc_start_dt
)
,


s_comm_prof_2014_lc_src as (
    select contract_id, contract_eid, payment_type,
           from_dt, till_dt,
           nds, currency,
			client_id, 
            lc_monthly_amt, lc_start_dt,
			discount, amt_to_charge,amt_to_charge_rub,
			from_dt_prev, amt_to_charge_rub_prev,
			
           -- BALANCE-15170:
           -- в делькредере всегда будет 0
           -- в КВ будет делькредере+КВ
           -- BALANCE-15113: добавлен контроль оборота
           -- BALANCE-15306: контроль оборота работает по-клиентно,
           --                а не по договору вцелом
           case 
                 -- если же есть данные за пред.период, то
                 -- штрафуем в след. ситуациях:
            when amt_to_charge_rub_prev is not null
             and (
                    -- если оборот за тек. месяц и предыдущий
                    -- меньше планового
                     (
                      amt_to_charge_rub_prev < lc_monthly_amt*.8 and
                      amt_to_charge_rub      < lc_monthly_amt*.8)
                    -- если разница между плановым оборотом и фактическим
                    -- больше 1.5М в тек. и пред. периоде
                  or (
                      (lc_monthly_amt - amt_to_charge_rub_prev ) > 1500000 and
                      (lc_monthly_amt - amt_to_charge_rub      ) > 1500000
                     )
                    -- если есть оборот, но он не за предыдущий период
                    -- и при этом, за тек.период недобрал до плана
                    -- BALANCE-15339
                  or (
                        from_dt != add_months(from_dt_prev, 1) and
                        (
                            amt_to_charge_rub  < lc_monthly_amt*.8
                         or (lc_monthly_amt - amt_to_charge_rub) > 1500000
                        )
                     )
                 )
             then amt_to_charge*0.0201
				-- если нет оборота за предыдущий период и
               -- текущий месяц — второй месяц программы и
               -- оборот тек.месяца менее 80% плана, то штрафуем
               -- BALANCE-16340
             when amt_to_charge_rub_prev is null
              and from_dt = add_months(trunc(lc_start_dt, 'MM'), 1)
              and amt_to_charge_rub  < lc_monthly_amt*.8
             then amt_to_charge*0.0201
               -- во всех остальных случаях платим положенное
             else amt_to_charge*(discount + 0.02)
           end                                  as reward_to_charge,
           -- Диагностика
           case
                 -- если денег за пред.период — null
                 -- то, значит, что текущий период у клиента — первый
                 -- и надо просто платит по % из договора
            when amt_to_charge_rub_prev is not null
                 -- если же есть данные за пред.период, то
                 -- штрафуем в след. ситуациях:
             and (
                    -- если оборот за тек. месяц и предыдущий
                    -- меньше планового
                     (
                      amt_to_charge_rub_prev < lc_monthly_amt*.8 and
                      amt_to_charge_rub      < lc_monthly_amt*.8
                     )
                 )
            then 'Оборот за пред. ('|| amt_to_charge_rub_prev ||') и тек. ('||
                    amt_to_charge_rub||') месяцы менее 80% от плана (план: '||
                    lc_monthly_amt||', 80% от него: '||lc_monthly_amt*.8||')'
            when amt_to_charge_rub_prev is not null
                 -- если же есть данные за пред.период, то
                 -- штрафуем в след. ситуациях:
             and (
                    -- если разница между плановым оборотом и фактическим
                    -- больше 1.5М в тек. и пред. периоде
                     (
                      (lc_monthly_amt - amt_to_charge_rub_prev ) > 1500000 and
                      (lc_monthly_amt - amt_to_charge_rub      ) > 1500000
                     )
                 )
            then 'Разница между планом и и фактом более 1.5М руб. в прошлом ('||
                 (lc_monthly_amt - amt_to_charge_rub_prev )||') и текущем ('||
                 (lc_monthly_amt - amt_to_charge_rub)||') месяце'
            when amt_to_charge_rub_prev is not null
                 -- если же есть данные за пред.период, то
                 -- штрафуем в след. ситуациях:
             and (
                    -- если есть оборот, но он не за предыдущий период
                    -- и при этом, за тек.период недобрал до плана
                    -- BALANCE-15339
                     (
                        from_dt != add_months(from_dt_prev, 1) and
                        (
                            amt_to_charge_rub  < lc_monthly_amt*.8
                         or (lc_monthly_amt - amt_to_charge_rub) > 1500000
                        )
                     )
                 )
            then 'Нет оборота за пред. месяц и не выполнил план в текущем:'||
                    from_dt||',оборот='||amt_to_charge_rub||',80% от плана='||
                    lc_monthly_amt*.8||'. Есть оборот за месяц:'||from_dt_prev
-- если нет оборота за предыдущий период и
               -- текущий месяц — второй месяц программы и
               -- оборот тек.месяца менее 80% плана, то штрафуем
               -- BALANCE-16340
             when amt_to_charge_rub_prev is null
              and from_dt = add_months(trunc(lc_start_dt), 1)
              and amt_to_charge_rub  < lc_monthly_amt*.8
             then 'Нет оборота за первый месяц программы (начало программы'||
                    to_char(lc_start_dt, 'DD.MM.YYYY')||'), '||
                  'во втором — оборот ('||amt_to_charge_rub||') '||
                  '< 80% от плана ('||lc_monthly_amt*.8||')'
            else null
           end                                  as failed_desc
      from (
            select d.*,
                   lag(d.from_dt)
                    over(partition by contract_id, client_id
                             order by from_dt)          as from_dt_prev,
                   -- оборот в рублях за предыдущий месяц по клиенту
                   lag(d.amt_to_charge_rub)
                    over(partition by contract_id, client_id
                             order by from_dt)          as amt_to_charge_rub_prev
              from (
                    select d.contract_id, d.contract_eid, d.payment_type,
                           d.from_dt, d.till_dt,
                           d.nds, d.currency,
                           d.client_id,
                           d.lc_monthly_amt,
                           d.lc_start_dt,
                           d.discount,
                           d.amt_to_charge,
                           d.amt_to_charge_rub
                      from s_lc_by_clients  d
                   ) d
           )d
),

---


s_lc_src as (
    select contract_id, contract_eid, payment_type,
           from_dt, till_dt,
           nds, currency,
           sum(amt_to_charge)           as amt_to_charge,
           sum(reward_to_charge)        as reward_to_charge
      from s_comm_prof_2014_lc_src d
     group by contract_eid, contract_id, from_dt, till_dt, payment_type,
              currency, nds
),


-- BALANCE-16389:
--  проверяем, чтобы заплатили не более, чем начислили
s_lc as (
    select contract_id, contract_eid,
           from_dt, till_dt,
           currency, nds,
           amt_to_charge,
           reward_to_charge,
           -- показываем акты как оплаты для предоплатников
           decode(payment_type, 2, reward_to_charge, null) as reward_to_pay
      from s_lc_src d
),

-- ----------------------------------------------------------------------------
-- СКВ
-- ----------------------------------------------------------------------------
-- полугодия
s_half_years as (
    select d.dt from_dt, add_months(d.dt, 6)-1/24/60/60 as till_dt
      from (
         select add_months(date'2013-03-01', 6*(level-1)) as dt
           from dual
        connect by level <= 10
           ) d
),


s_acts_2014_lc as (
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
           lc.lc_turnover    ,
           lc.collateral_first_dt                         as lc_start_dt,
           lc.collateral_end_dt                                as lc_end_dt,
           nvl(xxx.is_loyal, 0)                       as loyal_clients,
           xxx.client_id                              as client_id,
           xxx.act_dt                                     as act_dt,
           h.from_dt,
           trunc(h.till_dt)                         as till_dt,
           xxx.amount                                as amt_w_nds,
           xxx.amount-xxx.amount_nds-xxx.amount_nsp    as amt
      from s_base       b
      join xxxx_new_comm_contract_basic   xxx  on b.invoice_id = xxx.invoice_id
                                              and xxx.hidden < 4
      join xxxx_loyal_clients_contr_attr lc on lc.contract_id = b.contract_id
                                            and lc.client_id = xxx.client_id
                                            and lc.collateral_first_dt >= date'2013-03-01'
                                             -- только закончившиеся ДС
                                             -- относительно полугодия
                                            and case
                                                    when sysdate >= date'2016-03-01' then date'2016-03-01'-1/24/60/60
                                                    when sysdate >= date'2015-09-01' then date'2015-09-01'-1/24/60/60
                                                    when sysdate >= date'2015-03-01' then date'2015-03-01'-1/24/60/60
                                                    when sysdate >= date'2014-09-01' then date'2014-09-01'-1/24/60/60
                                                    when sysdate >= date'2014-03-01' then date'2014-03-01'-1/24/60/60
                                                    else trunc(sysdate, 'MM')
                                                end > lc.collateral_end_dt
      join s_half_years                   h  on lc.collateral_end_dt between h.from_dt
                                                              and h.till_dt
     where nvl(xxx.is_loyal, 0) = 1
       and b.discount_type = 7
       and xxx.act_dt between lc.collateral_first_dt and lc.collateral_end_dt
       and b.commission_type = 48
        -- только договора текущего фин.года
       and b.contract_till_dt > (select fin_year_dt from s_dates)
        -- BALANCE-19159
        -- начиная с 01.01.2015 исключаем акты, по счетам которых
        -- не указан конечный получатель
       and (
            -- начиная с 2015, исключаем оптовую часть
            -- и оставляем только те счета, в которых есть конечный покупатель
            (
                xxx.act_dt >= date'2015-01-01'
            -- BALANCE-19429
            and b.is_opt = 0
            )
            or
            -- до 2015 года ничего не делаем
            (
                xxx.act_dt < date'2015-01-01'
            )
           )
),



-- оборот без ЛК
s_skv_not_lc as (
    select d.contract_eid, d.contract_id,
           d.currency, d.nds, d.discount_type,
           d.client_id, d.loyal_clients,
           h.from_dt                                    as from_dt,
           trunc(h.till_dt)                             as till_dt,
           sum(case
              when 0 = (
                  select count(1) from xxxx_new_comm_contract_basic old_act
                   where old_act.invoice_first_act < (select new_acts_dt from s_dates)
                     and old_act.invoice_id = d.invoice_id
                     and old_act.hidden < 4
                     and rownum = 1)
               and 0 = (
                      -- BALANCE-15636
                  select count(1) from xxxx_new_comm_contract_basic fpay
                   where fpay.INVOICE_FIRST_PAYMENT < (select new_acts_dt from s_dates)
                     and fpay.invoice_eid = d.invoice_eid
                     and rownum = 1)
              then amt*cr.rate
              else 0
               end)                                       as amt_new_rub,
           sum(d.amt_w_nds*cr.rate)                       as amt_w_nds_rub,
           sum(d.amt*cr.rate)                             as amt_rub
           -- выбираем все акты, а на «выходе» раскладываем
           -- по колонкам «весь оборот» и «новый оборот»
      from s_comm_prof_2014_acts       d
      join xxxx_currency_rate          cr on cr.cc = d.currency
                                            and cr.rate_dt = trunc(d.act_dt)
      join s_half_years                   h  on d.act_dt between h.from_dt
                                                             and h.till_dt
        -- Без ЛК
       and not (loyal_clients = 1 and discount_type = 7)
        -- Директ, Медийка (вся), Справочник
     where discount_type in (7, 1, 2, 3, 12)
     group by h.from_dt, trunc(h.till_dt),
              d.contract_eid, d.contract_id,
              d.currency, d.nds, d.discount_type,
              d.client_id, d.loyal_clients
),



-- ЛК, которые выполнили оборот
s_skv_lc as (
    select d.*
      from (
        select d.contract_eid, d.contract_id,
               d.currency, d.nds, d.discount_type,
               d.client_id, d.loyal_clients,
               d.lc_turnover   as turnover,
               d.from_dt                                    as from_dt,
               trunc(d.till_dt)                             as till_dt,
               -- Т.к. смотрим только на обороты в рамках действия
               -- ДС, которое должно быть заключено после 01-03-13,
               -- все обороты будут новыми
               sum(d.amt*cr.rate)                             as amt_new_rub,
               sum(d.amt_w_nds*cr.rate)                       as amt_w_nds_rub,
               sum(d.amt*cr.rate)                             as amt_rub
               -- выбираем все акты, а на «выходе» раскладываем
               -- по колонкам «весь оборот» и «новый оборот»
          from s_acts_2014_lc         d
          join xxxx_currency_rate          cr on cr.cc = d.currency
                                                and cr.rate_dt = trunc(d.act_dt)
			group by d.from_dt, d.till_dt,
                  d.contract_eid, d.contract_id,
                  d.currency, d.nds, d.discount_type,
                  d.client_id, d.loyal_clients,
                  d.lc_turnover
           ) d
        -- только если факт больше, либо равен 120% от плана*6 месяцев
        -- 6 месяцев — это типовой срок ДС для ЛК
     where d.amt_rub >= turnover*6*1.2
),



-- договора за период, по которым нет
-- клиентов с оборотом по директу > 70%
-- BALANCE-15995
s_comm_prof_2014_skv_less_70 as (
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
                   sum(decode(d.discount_type, 7, d.amt*cr.rate, null))  as amt_rub
              from s_comm_prof_2014_acts         d
              join xxxx_currency_rate            cr on cr.cc = d.currency
                                                      and cr.rate_dt = trunc(d.act_dt)
              join s_half_years                   h  on d.act_dt between h.from_dt
                                                                     and h.till_dt
             group by h.from_dt,
                      d.contract_id,
                      d.client_id
               ) d
           ) d
     group by d.contract_id, d.from_dt
           -- BALANCE-15982
    having round(max(ratio), 4) <= 0.7
),



s_comm_prof_2014_skv_src as (
        select d."CONTRACT_EID",d."CONTRACT_ID",d."DISCOUNT_TYPE",d."CLIENT_ID",d."FROM_DT",d."TILL_DT",d."TURNOVER",d."AMT_NEW_RUB",d."AMT_W_NDS_RUB",d."AMT_RUB"
          from (
                -- базой для расчета являются обороты по счетам,
                -- по которым до 1 марта 2013 года не были ни оплат, ни актов ("новые"),
                -- обороты берем за период расчета. ЛК по умолчанию не считаем, см. далее.
                select d.contract_eid, d.contract_id,
                       d.discount_type,
                       d.client_id,
                       d.from_dt, d.till_dt,
                       null     as turnover,
                       d.amt_new_rub,
                       d.amt_w_nds_rub,
                       d.amt_rub
                  from s_skv_not_lc d
                 union all
                -- Если на момент расчета СКВ есть закончившиеся ДС на лояльных клиентов,
                -- дата начала которых больше либо равна 1 марта 2013 года, то:
                -- - по каждому клиенту определяем % оборота к плану в ДС, умноженному
                --   на 6 (кол-во месяцев в периоде),
                -- - если факт больше либо равен 120% от плана, то добавляем эти обороты
                --   в основные при определении точки на шкале и при расчете базы.
                select d.contract_eid, d.contract_id,
                       d.discount_type,
                       d.client_id,
                       d.from_dt, d.till_dt,
                       d.turnover,
                       d.amt_new_rub,
                       d.amt_w_nds_rub,
                       d.amt_rub
                  from s_skv_lc d
               ) d
               -- BALANCE-15674
               -- BALANCE-15995
               -- Если доля одного клиента в Директе за период была более 70%
               -- за период расчета, СКВ не платим
          join s_comm_prof_2014_skv_less_70  l on l.contract_id = d.contract_id
                                                 and l.from_dt = d.from_dt
),

s_skv as (
    select d.*,
           case
            when (to_char(from_dt, 'MM-DD') = '03-01' and amt_rub > 19000000)
              or (to_char(from_dt, 'MM-DD') = '09-01' and amt_rub > 23000000)
            then amt_new*0.03
            else 0
           end                          as reward_regress,
           case
            when to_char(from_dt, 'MM-DD') = '03-01' then
               case
                when amt_rub >=520000000 then 26650000 + (amt_rub - 520000000)*0.07
                when amt_rub >=250000000 then 10450000 + (amt_rub - 250000000)*0.06
                when amt_rub >= 92000000 then  2550000 + (amt_rub -  92000000)*0.05
                when amt_rub >= 44000000 then   630000 + (amt_rub -  44000000)*0.04
                when amt_rub >= 23000000 then            (amt_rub -  23000000)*0.03
                else 0
               end
            when to_char(from_dt, 'MM-DD') = '09-01' then
               case
                when amt_rub >= 650000000 then 33170000 + (amt_rub - 650000000)*0.07
                when amt_rub >= 330000000 then 13970000 + (amt_rub - 330000000)*0.06
                when amt_rub >= 115000000 then  3220000 + (amt_rub - 115000000)*0.05
                when amt_rub >=  54000000 then   780000 + (amt_rub -  54000000)*0.04
                when amt_rub >=  28000000 then            (amt_rub -  28000000)*0.03
                else 0
               end
            else 0
           end                          as reward_excess
      from (
        select d.contract_eid, d.contract_id, d.from_dt, d.till_dt,
               sum(amt_new_rub)                 as amt_new,
               sum(amt_rub)                     as amt_rub,
               -- BALANCE-15641
               sum(amt_w_nds_rub)               as amt_w_nds,
               sum(amt_rub)                     as amt
          from s_comm_prof_2014_skv_src d
         group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt
           ) d
),
-- ----------------------------------------------------------------------------
-- КВ Директ (только акты)
-- ----------------------------------------------------------------------------
s_kv_direct as (
    select d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           nds, currency,
           discount_type,
           type,
           sum(amt_to_charge)       as amt_to_charge,
           null                     as amt_to_pay,
           null                     as amt_to_pay_w_nds,
           sum(amt_to_charge*decode(f.failed, 1, 0.0001,
                decode(type,
                    4, 0.08,
                    5, 0.06,
                    6, 0.05)))      as reward_to_charge,
           sum(amt_to_pay*decode(f.failed, 1, 0.0001,
                decode(type,
                    4, 0.08,
                    5, 0.06,
                    6, 0.05)))      as reward_to_pay,
           sum(amt_to_charge*0.02)  as delkredere_to_charge,
           sum(amt_to_pay   *0.02)  as delkredere_to_pay
      from (
            select contract_id, contract_eid,
                   from_dt, till_dt,
                   nds, currency,
                   discount_type,
                   payment_type,
                   case
                    when dd.client_avg_discount <12 or dd.client_avg_discount is null then 4
                    when dd.client_avg_discount >= 12 and dd.client_avg_discount < 18 then 5
                    when dd.client_avg_discount >= 18                      then 6
                   end                              as type,
                   amt                              as amt_to_charge,
                   decode(payment_type,
                        -- предоплата
                        2, amt_w_nds,
                        -- постоплата
                        3, null)                    as amt_to_pay_w_nds,
                   decode(payment_type,
                        -- предоплата
                        2, amt,
                        -- постоплата
                        3, null)                    as amt_to_pay
              from s_comm_prof_2014_acts_new_wolc d
              left outer
              join xxxx_client_discount_m   dd on dd.client_id = d.client_id
                                                      and dd.dt = d.from_dt
             where d.discount_type in (7)
           ) d
        -- BALANCE-16952
      left outer
      join s_comm_prof_2014_kv_over_70  f on f.contract_id = d.contract_id
                                            and f.from_dt = d.from_dt
                                            and f.till_dt = d.till_dt
     group by contract_eid, d.contract_id, d.from_dt, d.till_dt,
              discount_type, type, currency, nds
)

--select * from s_kv_direct;
,
-- ----------------------------------------------------------------------------
-- КВ оплаты
-- ----------------------------------------------------------------------------
s_kv_total as (
    select contract_id, contract_eid,
           from_dt, till_dt,
           nds, currency,
           sum(amt)                 as amt_to_pay,
           sum(amt_w_nds)           as amt_to_pay_w_nds,
           sum(decode(payment_type,
                -- предоплата
                2, null,
                -- постоплата
                -- BALANCE-14885
                3, amt*commission_payback_pct/100)
               )                    as reward_to_pay,
           sum(decode(payment_type,
                -- предоплата
                2, null,
                -- постоплата
                3, amt*0.02))       as delkredere_to_pay
      from s_payments_new
     group by contract_eid, contract_id, from_dt, till_dt,
              nds, currency
)

--select * from s_kv_total;
--
-- результирующий запрос
--
select contract_id,
       contract_eid,
       from_dt,
       till_dt,
       nds                   as nds,
       currency              as curr,
       discount_type         as disc,
       type                  as type,
       amt_to_charge         as turn_c,      -- оборот к начислению
       reward_to_charge      as rew_c,      -- к начислению
       delkredere_to_charge  as del_c,      -- к начислению (делькредере)
       dkv_to_charge         as dkv_c,      -- ДКВ за 2012 год к начислению
       amt_to_pay_w_nds      as turn_p_nds,
       amt_to_pay            as turn_p,      -- оборот к перечислению       
       reward_to_pay         as rew_p,      -- к перечислению
       delkredere_to_pay     as del_p,      -- к перечислению (делькредере)       
       dkv_to_pay            as dkv_p      -- ДКВ за 2012 год к начислению 
  from (
        select contract_eid, contract_id,
               from_dt, till_dt,
               discount_type,
               currency, nds,
               amt_to_charge,
               null         as amt_to_pay,
               null         as amt_to_pay_w_nds,
               reward_to_charge,
               reward_to_pay,
               delkredere_to_charge,
               delkredere_to_pay,
               null         as dkv_to_charge,
               null         as dkv_to_pay,
               1            as type
          from s_kv
         union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               null                             as discount_type,
               -- BALANCE-15641
               'RUR'                            as currency,
               1                                as nds,
               amt                              as amt_to_charge,
               amt                              as amt_to_pay,
               amt_w_nds                        as amt_to_pay_w_nds,
               nvl(reward_regress, 0)
                + nvl(reward_excess, 0)         as reward_to_charge,
               nvl(reward_regress, 0)
                + nvl(reward_excess, 0)         as reward_to_pay,
               0                                as delkredere_to_charge,
               0                                as delkredere_to_pay,
               null                             as dkv_to_charge,
               null                             as dkv_to_pay,
               2                                as type
          from s_skv
            -- BALANCE-17099
         where (nvl(reward_regress, 0) + nvl(reward_excess, 0)) > 0
         union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               discount_type,
               currency, nds,
               amt_to_charge,
               amt_to_pay,
               amt_to_pay_w_nds,
               reward_to_charge,
               reward_to_pay,
               delkredere_to_charge,
               delkredere_to_pay,
               null         as dkv_to_charge,
               null         as dkv_to_pay,
               type
          from s_kv_direct
         union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               null         as discount_type,
               currency, nds,
               null         as amt_to_charge,
               amt_to_pay,
               amt_to_pay_w_nds,
               null         as reward_to_charge,
               reward_to_pay,
               null         as delkredere_to_charge,
               delkredere_to_pay,
               null         as dkv_to_charge,
               null         as dkv_to_pay,
               10           as type
          from s_kv_total
         union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               discount_type,
               currency, nds,
               amt_to_charge,
               amt_to_pay,
               amt_to_pay_w_nds,
               reward_to_charge,
               reward_to_pay,
               delkredere_to_charge,
               delkredere_to_pay,
               dkv_to_charge,
               dkv_to_pay,
               0            as type
          from s_kv_old
        union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               null         as discount_type,
               currency, nds,
               null         as amt_to_charge,
               null         as amt_to_pay,
               null         as amt_to_pay_w_nds,
               reward       as reward_to_charge,
               reward       as reward_to_pay,
               null         as delkredere_to_charge,
               null         as delkredere_to_pay,
               null         as dkv_to_charge,
               null         as dkv_to_pay,
               3            as type
          from s_region
         union all
            -- BALANCE-16000
        select contract_eid, contract_id,
               from_dt, till_dt,
               71           as discount_type,
               currency, nds,
               amt_to_charge,
               null         as amt_to_pay,
               null         as amt_to_pay_w_nds,
               reward_to_charge,
               -- BALANCE-16000: для предоплатников платим по актам
               reward_to_pay,
               0            as delkredere_to_charge,
               -- делькредере входит в reward
               0            as delkredere_to_pay,
               null         as dkv_to_charge,
               null         as dkv_to_pay,
               1            as type
          from s_lc
       )
order by contract_id , from_dt,till_dt, disc, type, turn_c,rew_c;
