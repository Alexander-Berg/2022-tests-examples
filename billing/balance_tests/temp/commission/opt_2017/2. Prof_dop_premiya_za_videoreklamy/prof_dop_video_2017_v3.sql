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
        when c.contract_commission_type in (4, 10, 12,13)
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
                                                    -- Маркет, спецпроекты
                                                    c.contract_commission_type in (14) and
                                                    nvl(c.commission_type, c.discount_type) in
                                                    (33)
                                                )

                                                or
                                                (
                                                    -- По Недвижимости смотрим всё
                                                    c.contract_commission_type in (10) and 1=1
                                                )
                                                or
                                                (
                                                    -- По Украине пропускаем все,
                                                    -- чтобы было в 15/16 году.
                                                    -- Отфильтруем более точно
                                                    -- на уровне v_opt_2015_acts
                                                    -- BALANCE-23716
                                                    c.contract_commission_type in (6) and
                                                    nvl(c.commission_type,
                                                        c.discount_type) in
                                                    (0, 1, 2, 3, 7, 8, 12, 15, 32)
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
	select * from (
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
	  							-- BALANCE-24627: более ранние акты не нужны
	                                          and xxx.act_dt>= date'2016-01-01'
	    join xxxx_currency_rate              cr on cr.cc = b.currency
	                                       and cr.rate_dt = trunc(xxx.act_dt)
	    where b.commission_type in (1, 2, 3, 4, 5, 6, 8, 10, 11, 12, 13, 36)
	
	   and (
	          -- base, prof
	          (
	            b.commission_type in (1, 2, 8)
	        and (
		-- BALANCE-24516: новые условия применимы только актов нового
					-- фин.года при условиия, что договор продлен
	                xxx.act_dt >= date'2017-03-01' and b.contract_till_dt >= date'2017-03-01' and b.discount_type in (1, 2, 3, 7, 12, 36)
	                -- BALANCE-22085
	                -- В актах по новым и продленным договорам 2016:
	                --  * отбрасываем маркет (11)
	                --  * включаем Авто (25)
	             or xxx.act_dt >= date'2017-03-01' and b.contract_till_dt  < date'2017-03-01' and b.discount_type in (1, 2, 3, 7, 12, 25)
	             or xxx.act_dt >= date'2016-03-01' and b.contract_till_dt >= date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 12, 25)
	             or xxx.act_dt >= date'2016-03-01' and b.contract_till_dt  < date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
	             or xxx.act_dt < date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
	 )
	          )
	          -- ua
	       or (
	                b.commission_type = 6
	            and (
	                   -- BALANCE-22914
	                   xxx.act_dt >= date'2016-04-01'
	               and b.discount_type in (0, 1, 8, 7, 15)
	                or xxx.act_dt <  date'2016-04-01'
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
	       or (b.commission_type = 10 and 1=1)
	          -- audio
	       or (b.commission_type = 11 and b.discount_type_src = 29)
	          -- market
	       or (b.commission_type in (12, 13) and b.discount_type = 11)
	       ))
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
            ))
)

--select * from s_acts_src;

,
s_curr_contracts as (
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
        -- BALANCE-23037
	   and d.contract_till_dt > date'2016-03-01'
	
     group by d.contract_eid,       d.contract_id,      d.agency_id,
           d.contract_from_dt,      d.contract_till_dt,d.discount_type,
           q.from_dt, q.till_dt,
           d.from_dt
)

--select * from s_curr_contracts;

-- Тек обороты по аг-вам, по которым есть обороты в 2016 году
, s_agency_stats_curr as (
  select d.*,
   -- нарастающий итог по аг-ву с начала квартала
              sum(amt) over (partition by agency_id, from_dt
                                  order by month)                      as amt_m,
              -- итог за квартал
             sum(amt) over (partition by agency_id, from_dt)          as amt_q
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
,

s_opt_2015_prof_q as (
 select 
 d.contract_eid,        d.contract_id,        d.agency_id,
       d.contract_from_dt,    d.contract_till_dt,d.discount_type,
       d.from_dt, d.till_dt,
       d.month                  as till_dt_fc,
       d.amt_w_nds_rub          as amt_w_nds_rub,
       d.amt                    as amt,
       d.amt                    as amt_for_forecast,
       -- За месяц (нарастающий итог)
       cs.amt_m                 as amt_ag,
       cs.amt_q                 as amt_ag_q,
       ps.amt                   as amt_ag_prev,
       ps.amt_fm                as amt_ag_prev_fm,
       decode(nvl(ps.amt_fm, 0),
        0, 1, 0)                as failed,
       case
           when contract_eid in (select contract_eid
                         from bo.v_opt_2015_exclusions)
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
)

--select * From s_opt_2015_prof_q ;
,
s_q_by_month as (
    -- Т.к. суммы в исходных строках с разным уровнем гранулированности,
    -- то приводим сначала их все к уровню месяца.
    -- Исходные данные:
    --      тек. оборот по договору — помесячный по сервису
    --      тек. оборот по аг-ву    — поквартальный
    --      прошлый оборот по аг-ву — поквартальный
    select contract_eid, contract_id, from_dt, till_dt, agency_id,
           till_dt_fc, amt_ag_prev, amt_ag_prev_fm,
           amt_ag_q                     as amt_ag,
           -- тек. оборот по договору — поднимаем до месяца
           sum(amt)                     as amt_rub,
           sum(amt_w_nds_rub)           as amt_w_nds_rub
      from s_opt_2015_prof_q
     where failed = 0
       and excluded = 0
     group by contract_eid, contract_id, from_dt, till_dt, agency_id,
              till_dt_fc, amt_ag_q, amt_ag_prev, amt_ag_prev_fm
)
--select * from s_q_by_month;
,

s_q as (
    select d.*,
           bo.pk_comm.calc_prof_q(
                amt_rub,
                amt_ag,
                amt_ag_prev,
                amt_ag_prev_fm
           )                                as reward
      from (
        select d.contract_eid, d.contract_id, d.from_dt, d.till_dt, d.agency_id,
                  -- прошлый оборот по аг-ву — уже за квартал, ничего не делаем
       amt_ag_prev,
       amt_ag_prev_fm,
       -- тек. оборот по договору — поднимаем до квартала
       sum(amt_rub)                 as amt_rub,
       sum(amt_w_nds_rub)           as amt_w_nds_rub,
       -- тек. оборот по агентству — поднимаем до квартала
       amt_ag
  from s_q_by_month d
 group by contract_eid, contract_id, from_dt, till_dt, agency_id,
          amt_ag_prev, amt_ag_prev_fm, amt_ag
           ) d
),

--select * from s_q order by agency_id;
-- Обороты за 01-2017 — 02-2017
s_acts_src_dop as (
    select i.contract_id,
           i.contract_eid,
           i.invoice_eid,
           i.invoice_id,
           i.invoice_dt,
           i.currency,
           i.nds,
           i.nds_pct,
           trunc(xxx.act_dt, 'MM')                                as from_dt,
           add_months(trunc(xxx.act_dt, 'MM'), 1) - 1/84600       as till_dt,
           xxx.amount                                             as amt_w_nds,
           xxx.amount-xxx.amount_nds-xxx.amount_nsp               as amt,
           (xxx.amount)*cr.rate                                   as amt_rub_w_nds,
           (xxx.amount-xxx.amount_nds-xxx.amount_nsp)*cr.rate     as amt_rub
      from s_base        i
     join xxxx_new_comm_contract_basic     xxx on i.invoice_id = xxx.invoice_id
                                              and xxx.hidden < 4
                                              and xxx.act_dt between date'2017-01-01'
                                                           and date'2017-03-01'-1/86400
                                                   and xxx.product_id in (
                                                    508046, 508047, 508048,
                                                    508049, 508050, 508051,
                                                    508052, 508053, 508054,
                                                    508055, 508056, 508057,
                                                    508058, 508059, 508060,
                                                    506550, 508065, 508066,
                                                    508067, 506549, 507150,
                                                    504696, 507226, 506604)
      join xxxx_currency_rate              cr on cr.cc = i.currency
                                      and cr.rate_dt = trunc(xxx.act_dt)
        -- начисляется для договоров Премиум
        -- за продажу продуктов видеорекламы (по актам).
     where i.commission_type = 2
)
--select *from s_acts_src_dop;
,

 s_acts_dop as (
    select contract_id, contract_eid,
           sum(amt_rub_w_nds)   as amt_w_nds,
           sum(amt_rub)         as amt
      from s_acts_src_dop
     group by contract_id, contract_eid
)
--select * from s_acts_dop;
,

 s_reward_dop as (
    select contract_id, contract_eid,
           amt_w_nds,
           amt,
           amt * case
            when amt >= 10000000 then 0.12
            when amt >=  7000000 then 0.11
            when amt >=  4700000 then 0.10
            when amt >=  2300000 then 0.09
            when amt >=  1400000 then 0.08
            when amt >=   700000 then 0.07
                                 else 0
           end              as reward
      from s_acts_dop
)
--select * from s_reward_dop;

,
-- результирующий запрос
s_opt_2017_prof_dop as(
select "CONTRACT_EID", "CONTRACT_ID", "AMT_RUB", "AMT_RUB_W_NDS", "REWARD"
 from (
	select contract_eid, contract_id,
		   amt          as amt_rub,
		   amt_w_nds    as amt_rub_w_nds,
		   reward
	  from s_reward_dop)
)
--select * from s_opt_2017_prof_dop;
-- результирующий запрос

select distinct s."CONTRACT_ID",s."CONTRACT_EID",s."FROM_DT",s."TILL_DT",s."NDS",s."CURRENCY",s."DISCOUNT_TYPE",s."REWARD_TYPE",s."TURNOVER_TO_CHARGE",s."REWARD_TO_CHARGE",s."DELKREDERE_TO_CHARGE",s."DKV_TO_CHARGE",s."TURNOVER_TO_PAY_W_NDS",s."TURNOVER_TO_PAY",s."REWARD_TO_PAY",s."DELKREDERE_TO_PAY",s."DKV_TO_PAY"
  from (
    select contract_id,     contract_eid,
           from_dt,         till_dt,
           nds,             currency,
           discount_type,
           300 + reward_type    as reward_type,
           -- к начислению
           turnover_to_charge,
           reward_to_charge,
           0                    as delkredere_to_charge,
           0                    as dkv_to_charge,
           -- к перечислению
           turnover_to_pay_w_nds,
           turnover_to_pay,
           reward_to_pay,
           0                    as delkredere_to_pay,
           0                    as dkv_to_pay
      from
(select contract_id,
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
       reward_to_pay                                -- к перечислению
  from ( 
  
  
     select nvl(q.contract_eid, d.contract_eid)          as contract_eid,
               nvl(q.contract_id, d.contract_id)            as contract_id,
               --BALANCE-24507, BALANCE-24689
               nvl(q.from_dt, date'2016-12-01')             as from_dt,
               nvl(q.till_dt, date'2017-03-01' - 1/86400)   as till_dt,
               null                                         as discount_type,
               'RUR'                                        as currency,
               1                                            as nds,
--               nvl(q.amt_rub, 0) + nvl(d.amt_rub, 0)        as turnover_to_charge,
               nvl(q.amt_rub, 0)                            as turnover_to_charge,
               nvl(q.reward, 0) + nvl(d.reward, 0)          as reward_to_charge,
--               nvl(q.amt_rub, 0) + nvl(d.amt_rub, 0)        as turnover_to_pay,
--               nvl(q.amt_w_nds_rub, 0)
--                    + nvl(d.amt_rub_w_nds, 0)               as turnover_to_pay_w_nds,
               nvl(q.amt_rub, 0)                            as turnover_to_pay,
               nvl(q.amt_w_nds_rub, 0)                      as turnover_to_pay_w_nds,
               nvl(q.reward, 0) + nvl(d.reward, 0)          as reward_to_pay,
               20                                           as reward_type
          from s_q                      q
		  full outer
          join s_opt_2017_prof_dop   d on d.contract_id = q.contract_id
		  
  ))) s
    order by contract_id, from_dt, discount_type, currency, nds;
