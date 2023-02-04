
create or replace view bo.v_opt_2015_market_skv as
with
s_dates as (
     select date'2016-03-01'   as fin_year_dt
       from dual
 ),
 -- полугодия
 s_half_years as (
     select d.dt from_dt, add_months(d.dt, 6)-1/24/60/60 as till_dt
       from (
          select add_months(date'2016-03-01', 6*(level-1)) as dt
            from dual
         connect by level <= 10
            ) d
 ),


s_base as (
select distinct
           c.contract_eid                             as contract_eid,
               case
        when c.contract_commission_type in (4,10,12,13)
         and c.firm_id  = 1
        then
       nvl(
        (select distinct l.value_num
                from xxxx_new_comm_contract_basic l
               where l.contract_id  = c.contract_id
--                 and l.code = 'LINK_CONTRACT_ID'
--                 )
                 and l.value_num is not null
                 ),
           c.contract_id)
        else c.contract_id
       end                               as contract_id,
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
           decode(
            nvl(c.commission_type, c.discount_type),
            22, 1,
		      	29, 1,
            nvl(c.commission_type, c.discount_type)
           )                                        as discount_type,
		   nvl(c.commission_type, c.discount_type)  as discount_type_src,
           c.payment_type                             as payment_type, 
                                              -- ?  as commission_payback_type
           c.commission_payback_pct                   as commission_payback_pct,
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
                                                    (1, 2, 3, 7, 11, 12, 14, 17, 19, 22, 25, 28, 29)
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
                                                    -- По Недвижимости смотрим всё
                                                    c.contract_commission_type in (10) and 1=1
                                                )
                                               or
                                                (
                                                    -- иначе, как ранее
                                                    nvl(c.commission_type,
                                                        c.discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 17, 19, 28, 29)
                                                )
                                              )
)

,
s_acts_src as (
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
    case
    when nvl(xxx.is_loyal, 0) = 1 and b.discount_type = 7
      then 1
      else 0
    end                                              as is_loyal,
    xxx.client_id                                      as client_id,
    xxx.agency_id                                      as agency_id,
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
    join xxxx_currency_rate              cr on cr.cc = b.currency
                                       and cr.rate_dt = trunc(xxx.act_dt)
    where b.commission_type in (1, 2, 3, 4, 5, 6, 8, 10, 11, 12, 13)
   and (
          -- base, prof
          (
            b.commission_type in (1, 2, 8)
        and (
                -- BALANCE-22085
                -- В новых актах по новым и продленным договорам
                --  * отбрасываем маркет (11)
                --  * включаем Авто (25)
            xxx.act_dt >= date'2016-03-01' and 
            b.contract_till_dt >= date'2016-03-01' and 
            b.discount_type in (1, 2, 3, 7, 12, 25) and
            -- BALANCE-22203
            -- BALANCE-22331: в новых условиях оставляем только рубли
             b.currency = 'RUR'
            -- В новых актах по непродленным договорам и старых актах
            -- оставляем старые условия
            -- BALANCE-22319
             or xxx.act_dt >= date'2016-03-01' and b.contract_till_dt < date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
             or xxx.act_dt  < date'2016-03-01' and b.discount_type in  (1, 2, 3, 7, 11, 12)
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
          -- market
       or (b.commission_type in (12, 13) and b.discount_type = 11  
                                        -- BALANCE-22578
                                         and b.currency = 'RUR')
       )
)

,
-- акты без лояльных клиентов
-- все акты по счетам
s_acts as (
    select b.*
      from s_acts_src   b
     where b.commission_type in (12, 13)
)
--select * from s_acts ;
,
-- ----------------------------------------------------------------------------
s_skv_not_lc as (
    select d.contract_eid, d.contract_id,
           d.contract_from_dt,
           d.contract_till_dt,
           d.currency, d.nds, d.discount_type, d.commission_type,
           d.client_id, d.is_loyal,
           d.nds_count, d.currency_count,
           -- для прогноза
           -- чтобы знать, за какой период брать оборот для проноза
           d.till_dt                                    as till_dt_fc,
           -- периоды полугодия
           h.from_dt                                    as from_dt,
           trunc(h.till_dt)                             as till_dt,
           sum(d.amt_w_nds_rub)                         as amt_w_nds_rub,
           sum(d.amt_rub)                               as amt_rub
      from s_acts                       d
      join s_half_years                 h  on d.act_dt between h.from_dt
                                                           and h.till_dt
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
s_opt_2015_market_skv_less_70 as (select d.contract_id, d.from_dt, max(ratio) as max_ratio
  from (
    select d.*,
           nvl(ratio_to_report(amt_rub)
              over (partition by d.contract_id, d.from_dt), 0) as ratio
      from (
        select d.contract_id,
               d.client_id,
               h.from_dt,
               sum(decode(d.discount_type, 11, d.amt_rub, null))  as amt_rub
          from s_acts_src             d
          join s_half_years                   h  on d.act_dt between h.from_dt
                                                                 and h.till_dt
         where d.commission_type in (12, 13)
         group by h.from_dt,
                  d.contract_id,
                  d.client_id
           ) d
       ) d
 group by d.contract_id, d.from_dt)

--select * from  s_opt_2015_market_skv_less_70; 

 
-- результирующий запрос
select d."CONTRACT_EID",d."CONTRACT_ID",d."CONTRACT_FROM_DT",d."CONTRACT_TILL_DT",d."DISCOUNT_TYPE",d."COMMISSION_TYPE",d."CLIENT_ID",d."NDS",d."NDS_COUNT",d."CURRENCY_COUNT",d."FROM_DT",d."TILL_DT",d."TILL_DT_FC",d."AMT_W_NDS_RUB",d."AMT_RUB_NOT_LC",
       d.amt_rub_not_lc                as amt_rub,
       d.amt_rub_not_lc                as amt_for_forecast,
       -- ТЗ: Полугодовая премия начисляется Агентству только при условии, если
       -- стоимость фактически оказанных Яндексом в течение соответствующего
       -- Отчетного полугодия Услуг по сервису Директ, связанных с размещением
       -- Материалов одного Клиента Агентства < 70% (округляется до целых
       -- процентов)
       l.max_ratio,
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
               d.amt_rub            as amt_rub_not_lc
          from s_skv_not_lc d
       ) d
       -- смотрим максимальную долю по клиенту по директу за полугодие
  join s_opt_2015_market_skv_less_70   l on l.contract_id = d.contract_id
                                         and l.from_dt = d.from_dt

