select * from v_opt_2015_prof_skv where CONTRACT_ID  =202998
;


  with
s_dates as (
    select date'2015-03-01'   as fin_year_dt
--    ,
--    202821   as qwe
      from dual
),

contracts as (
select    	203010	 as contract_id   from dual    union all
select    	203107	 as contract_id   from dual    union all
select    	202835	 as contract_id   from dual    union all
select    	202880	 as contract_id   from dual    union all
select    	202791	 as contract_id   from dual    union all
select    	202792	 as contract_id   from dual    union all
select    	203103	 as contract_id   from dual    union all
select    	202859	 as contract_id   from dual    union all
select    	202793	 as contract_id   from dual    union all
select    	202810	 as contract_id   from dual    union all
select    	202979	 as contract_id   from dual    union all
select    	202794	 as contract_id   from dual    union all
select    	202845	 as contract_id   from dual    union all
select    	202888	 as contract_id   from dual    union all
select    	202884	 as contract_id   from dual    union all
select    	202802	 as contract_id   from dual    union all
select    	202820	 as contract_id   from dual    union all
select    	202987	 as contract_id   from dual    union all
select    	202981	 as contract_id   from dual    union all
select    	202789	 as contract_id   from dual    union all
select    	202834	 as contract_id   from dual    union all
select    	202807	 as contract_id   from dual    union all
select    	202822	 as contract_id   from dual    union all
select    	202848	 as contract_id   from dual    union all
select    	202851	 as contract_id   from dual    union all
select    	202826	 as contract_id   from dual    union all
select    	202812	 as contract_id   from dual    union all
select    	202790	 as contract_id   from dual    union all
select    	202856	 as contract_id   from dual    union all
select    	202804	 as contract_id   from dual    union all
select    	202839	 as contract_id   from dual    union all
select    	202808	 as contract_id   from dual    union all
select    	202974	 as contract_id   from dual    union all
select    	202813	 as contract_id   from dual    union all
select    	202883	 as contract_id   from dual    union all
select    	202801	 as contract_id   from dual    union all
select    	202824	 as contract_id   from dual    union all
select    	202817	 as contract_id   from dual    union all
select    	202879	 as contract_id   from dual    union all
select    	202829	 as contract_id   from dual    union all
select    	202805	 as contract_id   from dual    union all
select    	202870	 as contract_id   from dual    union all
select    	202860	 as contract_id   from dual    union all
select    	202863	 as contract_id   from dual    union all
select    	202814	 as contract_id   from dual    union all
select    	202853	 as contract_id   from dual    union all
select    	202868	 as contract_id   from dual    union all
select    	202842	 as contract_id   from dual    union all
select    	202857	 as contract_id   from dual    union all
select    	202803	 as contract_id   from dual    union all
select    	202785	 as contract_id   from dual    union all
select    	202821	 as contract_id   from dual    union all
select    	202784	 as contract_id   from dual    union all
select    	202855	 as contract_id   from dual    union all
select    	202882	 as contract_id   from dual    union all
select    	202864	 as contract_id   from dual    union all
select    	202841	 as contract_id   from dual    union all
select    	202850	 as contract_id   from dual    union all
select    	202786	 as contract_id   from dual    union all
select    	202788	 as contract_id   from dual    union all
select    	202828	 as contract_id   from dual    union all
select    	202782	 as contract_id   from dual    union all
select    	202825	 as contract_id   from dual    union all
select    	202800	 as contract_id   from dual    union all
select    	202846	 as contract_id   from dual    union all
select    	202795	 as contract_id   from dual    union all
select    	202874	 as contract_id   from dual    union all
select    	202998	 as contract_id   from dual    union all
select    	202887	 as contract_id   from dual    union all
select    	202843	 as contract_id   from dual    union all
select    	202873	 as contract_id   from dual    union all
select    	202823	 as contract_id   from dual    union all
select    	202865	 as contract_id   from dual    union all
select    	202797	 as contract_id   from dual    union all
select    	202854	 as contract_id   from dual    union all
select    	202862	 as contract_id   from dual    union all
select    	202852	 as contract_id   from dual    union all
select    	202815	 as contract_id   from dual    union all
select    	203012	 as contract_id   from dual    union all
select    	202977	 as contract_id   from dual    union all
select    	203023	 as contract_id   from dual    union all
select    	202832	 as contract_id   from dual    union all
select    	202833	 as contract_id   from dual    union all
select    	203017	 as contract_id   from dual    union all
select    	203051	 as contract_id   from dual    
 
)
,
-- полугодия
s_half_years as (
    select d.dt from_dt, add_months(d.dt, 6)-1/24/60/60 as till_dt
      from (
         select add_months(date'2015-03-01', 6*(level-1)) as dt
           from dual
        connect by level <= 10
           ) d
),
-- Акты ЛК, у которых закончились программы в полугодии
-- Акты при этом надо брать не только за тек. год, т.к. бывают
-- программы, которые начинаются в прошлом фин. году. И по таким
-- ЛК надо учитывать весь оборот по программе, а не только тот,
-- которьй получился в текущем фин.году
-- BALANCE-18412
s_acts_lc as (
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
           b.is_loyal,
           b.client_id,
           b.amt_w_nds,
           b.amt,
           b.amt_w_nds_rub,
           b.amt_rub,
           b.act_dt,
           h.from_dt,
           trunc(h.till_dt)                         as till_dt,
           lc.turnover,
           lc.collateral_dt                         as lc_start_dt,
           lc.todate                                as lc_end_dt
      from bo.v_opt_2015_acts               b
      join bo.mv_loyal_clients_contr_attr   lc on lc.contract_id = b.contract_id
                                              and lc.client_id = b.client_id
                                              -- только закончившиеся ДС
                                              -- относительно полугодия
                                              -- TODO: вспомнить, зачем это?
                                              and case
                                                    when sysdate >= date'2016-03-01' then date'2016-03-01'-1/24/60/60
                                                    when sysdate >= date'2015-09-01' then date'2015-09-01'-1/24/60/60
                                                    when sysdate >= date'2015-03-01' then date'2015-03-01'-1/24/60/60
                                                    else trunc(sysdate, 'MM')
                                                end > lc.todate
      join s_half_years                     h  on lc.todate between h.from_dt
                                                                and h.till_dt
     where b.is_loyal = 1
       and b.act_dt between lc.collateral_dt and lc.todate
       and b.commission_type = 2
        -- только договора текущего фин.года
       and b.contract_till_dt > (select fin_year_dt from s_dates)
       and b.contract_id in (select contract_id from contracts)
),
s_skv_lc as (
    select d.*
      from (
        select d.contract_eid, d.contract_id,
               d.contract_from_dt,
               d.contract_till_dt,
               d.currency, d.nds, d.discount_type,
               d.client_id, d.is_loyal,
               d.turnover,
               d.from_dt,
               d.till_dt,
               sum(d.amt_w_nds_rub)                     as amt_w_nds_rub,
               sum(d.amt_rub)                           as amt_rub
          from s_acts_lc                 d
         group by d.from_dt, d.till_dt,
                  d.contract_from_dt,
                  d.contract_till_dt,
                  d.contract_eid, d.contract_id,
                  d.currency, d.nds, d.discount_type,
                  d.client_id, d.is_loyal,
                  d.turnover
           ) d
     where d.amt_rub >= turnover*6*1.1
     
     where  d.contract_id in (select contract_id from contracts)
),
s_skv_not_lc as (
    select d.contract_eid, d.contract_id,
           d.contract_from_dt,
           d.contract_till_dt,
           d.currency, d.nds, d.discount_type,
           d.client_id, d.is_loyal,
           -- для прогноза (для ЛК не нужно, т.к. ЛК не учитываем в прогнозе)
           -- чтобы знать, за какой период брать оборот для проноза
           d.till_dt                                    as till_dt_fc,
           -- периоды полугодия
           h.from_dt                                    as from_dt,
           trunc(h.till_dt)                             as till_dt,
           sum(d.amt_w_nds_rub)                         as amt_w_nds_rub,
           sum(d.amt_rub)                               as amt_rub
      from bo.v_opt_2015_acts             d
      join s_half_years                   h  on d.act_dt between h.from_dt
                                                             and h.till_dt
        -- Без ЛК
       and d.is_loyal = 0
       and d.commission_type = 2
       
       
       
       and d.contract_id  in (select contract_id from contracts)
       
       
       
        -- Директ, Медийка (вся), Справочник
     where d.discount_type in (7, 1, 2, 3, 12)
     group by h.from_dt, trunc(h.till_dt), d.till_dt,
              d.contract_from_dt,
              d.contract_till_dt,
              d.contract_eid, d.contract_id,
              d.currency, d.nds, d.discount_type,
              d.client_id, d.is_loyal
),
v as(select d."CONTRACT_EID",d."CONTRACT_ID",d."CONTRACT_FROM_DT",d."CONTRACT_TILL_DT",d."DISCOUNT_TYPE",d."CLIENT_ID",d."FROM_DT",d."TILL_DT",d."TILL_DT_FC",d."AMT_W_NDS_RUB",d."AMT_RUB_NOT_LC",d."AMT_RUB_LC",
       d.amt_rub_not_lc + d.amt_rub_lc as amt_rub,
       -- ТЗ: Полугодовая премия начисляется Агентству только при условии, если
       -- стоимость фактически оказанных Яндексом в течение соответствующего
       -- Отчетного полугодия Услуг по сервису Директ, связанных с размещением
       -- Материалов одного Клиента Агентства < 70% (округляется до целых
       -- процентов)
       l.max_ratio,
       case
        when round(l.max_ratio, 2) < 0.7 then 0
        else 1
       end          as failed
  from (
        select d.contract_eid, d.contract_id,
               d.contract_from_dt,
               d.contract_till_dt,
               d.discount_type,
               d.client_id,
               d.from_dt, d.till_dt, d.till_dt_fc,
               d.amt_w_nds_rub,
               d.amt_rub            as amt_rub_not_lc,
               0                    as amt_rub_lc
          from s_skv_not_lc d
         union all
        select d.contract_eid, d.contract_id,
               d.contract_from_dt,
               d.contract_till_dt,
               d.discount_type,
               d.client_id,
               d.from_dt, d.till_dt, d.till_dt as till_dt_fc,
               d.amt_w_nds_rub,
               0                    as amt_rub_not_lc,
               d.amt_rub            as amt_rub_lc
          from s_skv_lc d
       ) d
       -- смотрим максимальную долю по клиенту по директу за полугодие
  join bo.v_opt_2015_prof_skv_less_70   l on l.contract_id = d.contract_id
                                         and l.from_dt = d.from_dt
                                         
                                         )
                                    
                                    
                                         
  select contract_eid, contract_id,
                   from_dt, till_dt,
                   contract_from_dt,
                   contract_till_dt,
                   sum(amt_rub_not_lc)*100/17    as amt_forecast,
                   sum(amt_rub_lc)              as amt_fact
              from v d
--             where failed = 0       -- Исключаем фактических БОК
--               and till_dt_fc between p_from_dt and p_till_dt
             group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt,
                      d.contract_from_dt,
                      d.contract_till_dt
                                         ;