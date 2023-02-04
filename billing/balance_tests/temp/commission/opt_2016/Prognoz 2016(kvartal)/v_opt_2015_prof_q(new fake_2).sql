
  CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."V_OPT_2015_PROF_Q" ("CONTRACT_EID", "CONTRACT_ID", "AGENCY_ID", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "DISCOUNT_TYPE", "FROM_DT", "TILL_DT", "TILL_DT_FC", "AMT_W_NDS_RUB", "AMT", "AMT_FOR_FORECAST", "AMT_AG", "AMT_AG_Q", "AMT_AG_PREV", "AMT_AG_PREV_FM", "FAILED", "EXCLUDED") AS 
  with
s_quarters AS (
    select d.dt from_dt, add_months(d.dt, 3) - 1/24/60/60 as till_dt
      from (
         select add_months(date'2015-03-01', 3*(level-1)) as dt
           from dual
        connect by level <= 20
           ) d
),

s_base as (
select distinct
           c.contract_eid                             as contract_eid,
               case
        when c.contract_commission_type in (4,10)
         and c.firm_id  = 1
        then
        nvl(
        (select distinct l.value_num
                from xxxx_new_comm_contract_basic l
               where l.contract_id  = c.contract_id
                 and l.code = 'LINK_CONTRACT_ID'
--                 )
                 and l.value_num is not null),
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
--select * from s_base order by  invoice_id ;

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

--select * from s_acts_src;
-- обороты по-квартально по каждому договору
-- 2016 года
, s_curr_contracts as (
    select d.contract_eid,        d.contract_id,        d.agency_id,
           d.contract_from_dt,    d.contract_till_dt, d.discount_type,
           q.from_dt, q.till_dt,
           d.from_dt                                        as month,
           sum(d.amt_w_nds_rub)                             as amt_w_nds_rub,
           sum(d.amt_rub)                                   as amt
      from s_acts_src     d
      join s_quarters               q on d.from_dt between q.from_dt and q.till_dt
     where d.commission_type in (2)
       and d.discount_type in (1, 2, 3, 7, 25)
        -- BALANCE-22819
       and d.nds_count = 1
        -- BALANCE-22850
       and d.currency_count = 1
     group by d.contract_eid,       d.contract_id,      d.agency_id,
           d.contract_from_dt,      d.contract_till_dt, d.discount_type,
           q.from_dt, q.till_dt,
           d.from_dt
)

--select * from s_curr_contracts;

-- Тек обороты по аг-вам, по которым есть обороты в 2016 году
, s_agency_stats_curr as (
 select d.*, sum(amt) over (partition by agency_id, from_dt) as amt_q
   from ( select a.agency_id                                              as agency_id,
           q.from_dt,
           trunc(a.act_dt, 'MM')                                        as month,
           sum((a.amount - a.amount_nds - a.amount_nsp)*cr.rate)              as amt
      from xxxx_new_comm_contract_basic         a
        
      join s_quarters       q on a.act_dt between q.from_dt and q.till_dt
      join xxxx_currency_rate              cr on cr.cc = a.currency
                                              and cr.rate_dt = trunc(a.act_dt)
     where a.hidden < 4
       and nvl(a.commission_type, a.discount_type) in    (1, 2, 3, 7, 25)
       and a.agency_id in (select agency_id from s_curr_contracts)
     group by a.agency_id, q.from_dt, trunc(a.act_dt, 'MM')
     )d
)

--select * from xxxx_new_comm_contract_basic a
--join s_quarters       q on a.act_dt between q.from_dt and q.till_dt;
--select  * from s_agency_stats_curr order by agency_id;
,

-- Прошлогодние обороты по аг-вам, по которым есть обороты в 2016 году
s_agency_stats_prev as (
    SELECT a.agency_id                                            as agency_id,
           add_months(q.from_dt, 12)                              as from_dt,
           sum(decode(q.from_dt,
                trunc(a.act_dt, 'MM'), (a.amount - a.amount_nds - a.amount_nsp)*cr.rate,
                0))                                               as amt_fm,
           sum((a.amount - a.amount_nds - a.amount_nsp)*cr.rate)            as amt
      from xxxx_new_comm_contract_basic         a
       join xxxx_currency_rate              cr on cr.cc = a.currency
                                              and cr.rate_dt = trunc(a.act_dt)
      join s_quarters       q on a.act_dt between q.from_dt and q.till_dt
     where a.hidden < 4
       and nvl(a.commission_type, a.discount_type) in (1, 2, 3, 7, 25)
       and a.agency_id in (select agency_id from s_curr_contracts)
     group by a.agency_id, add_months(q.from_dt, 12)
)

--select * from s_agency_stats_prev order by agency_id;



 select 
 d.contract_eid,        d.contract_id,        d.agency_id,
       d.contract_from_dt,    d.contract_till_dt,d.discount_type,
       d.from_dt, d.till_dt,
       d.month                  as till_dt_fc,
       d.amt_w_nds_rub          as amt_w_nds_rub,
       d.amt                    as amt,
       d.amt                    as amt_for_forecast,
       
       cs.amt                   as amt_ag,
       cs.amt_q                 as amt_ag_q,
       ps.amt                   as amt_ag_prev,
       ps.amt_fm                as amt_ag_prev_fm,
       decode(nvl(ps.amt_fm, 0),
        0, 1, 0)                as failed,
       case
           when contract_eid in (
               -- Arrow + MediaGuru
               '32294/15', '32262/15',
               -- Блондинка + Aori
               '32254/15', '32248/15',
               -- АйКонтекст + Регистратура + Р-Брокер
               '32242/15', '32388/15', '32287/15',
               -- АйПроспект + Трафик + Амнет
               '32290/15', '32428/14', '32260/15',
               -- Люмолинк
               '34139/15', '49678/16'
           )
           then 1
           else 0
       end                      as excluded

  from s_curr_contracts      d
  join s_agency_stats_curr   cs on cs.agency_id = d.agency_id
                               and cs.from_dt = d.from_dt
                               and cs.month = d.month
                               
  left outer
  join s_agency_stats_prev   ps on ps.agency_id = d.agency_id
                               and ps.from_dt = d.from_dt
;