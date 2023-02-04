with
s_dates as (
  select date'2015-03-01'     as start_dt
    from dual
),

s_acts_name as (  
   select  
           xxxx.contract_eid                             as contract_eid,
           xxxx.contract_id                              as contract_id,
		   xxxx.invoice_eid                              as invoice_eid,
           xxxx.invoice_id                               as invoice_id,
           xxxx.invoice_dt                               as invoice_dt,
           xxxx.currency                                 as currency,
           xxxx.nds                                      as nds,    
           xxxx.nds_pct                                  as nds_pct,
           nvl(xxxx.commission_type, xxxx.discount_type) as discount_type,
           trunc(xxxx.act_dt, 'MM')                                as from_dt,
           add_months(trunc(xxxx.act_dt, 'MM'), 1) - 1/84600       as till_dt,
           xxxx.client_id                             as client_id,
	         xxxx.act_dt                                             as act_dt,
           xxxx.amount                                        as amt_w_nds,
           xxxx.amount-xxxx.amount_nds-xxxx.amount_nsp            as amt,
           33                                            as pct_main,
			     2                                                as pct_delc,        
     	     1                                                as reward_type
from   xxxx_new_comm_contract_basic  xxxx
                                             
      join xxxx_currency_rate               cr on cr.cc = xxxx.currency
                                              and cr.rate_dt = trunc(xxxx.act_dt)
	   where  xxxx.hidden <4 
			and xxxx.contract_eid = '45182/15'
      and nvl(xxxx.commission_type, xxxx.discount_type) in (29)
    )         

,

s_acts_non_name as (  
   select '45182/15'                                       as contract_eid,
           225601                                           as contract_id,
           xxxx.invoice_eid                              as invoice_eid,
           xxxx.invoice_id                               as invoice_id,
           xxxx.invoice_dt                               as invoice_dt,
           xxxx.currency                                 as currency,
           xxxx.nds                                      as nds,    
           xxxx.nds_pct                                  as nds_pct,
           nvl(xxxx.commission_type, xxxx.discount_type) as discount_type,
           trunc(xxxx.act_dt, 'MM')                                as from_dt,
           add_months(trunc(xxxx.act_dt, 'MM'), 1) - 1/84600       as till_dt,
           xxxx.client_id                             as client_id,
	         xxxx.act_dt                                             as act_dt,
           xxxx.amount                                        as amt_w_nds,
           xxxx.amount-xxxx.amount_nds-xxxx.amount_nsp            as amt,
           10                                          as pct_main,
 		      	0                                                as pct_delc,
           2                                                as reward_type        
     from   xxxx_new_comm_contract_basic  xxxx
                                             
      join xxxx_currency_rate               cr on cr.cc = xxxx.currency
                                              and cr.rate_dt = trunc(xxxx.act_dt)
	    where  xxxx.hidden <4 
			and nvl(xxxx.contract_eid , -1) != '45182/15'
			and xxxx.person_id != 2520961
           and nvl(xxxx.commission_type, xxxx.discount_type) in (29)
    )         

,
s_acts_pre as (
    select * from s_acts_name
    union all
    select * from s_acts_non_name
),
s_acts as (
    select contract_id, contract_eid, reward_type,
           from_dt, till_dt,
           nds, currency,
           discount_type,
           sum(amt_w_nds)           as amt_w_nds,
           sum(amt)                 as amt,
           sum(amt)*pct_main/100    as reward,
           sum(amt)*pct_delc/100    as delkredere
      from s_acts_pre
     group by contract_id, contract_eid, reward_type,
              from_dt, till_dt, pct_main, pct_delc,
              nds, currency,
              discount_type
),

s_payments_name as (
    select xxxx.contract_eid                                        as contract_eid,
           xxxx.contract_id                                         as contract_id,
           xxxx.invoice_eid                                         as invoice_eid,
           xxxx.invoice_id                                          as invoice_id,
           xxxx.invoice_dt                                          as invoice_dt,
           xxxx.currency                                            as currency,
           xxxx.nds                                                 as nds,
           xxxx.nds_pct                                             as nds_pct,
           nvl(xxxx.commission_type, xxxx.discount_type)            as discount_type,
           trunc(oebs.comiss_date, 'MM')                            as from_dt,
           add_months( trunc(oebs.comiss_date, 'MM'), 1)-1/84600    as till_dt,
           oebs.oebs_payment                                            as amt_w_nds,
           oebs.oebs_payment*100/(100 + xxxx.nds*xxxx.nds_pct )         as amt,
           33                                                   as pct_main,
           2                                                    as pct_delc,
           1                                                    as reward_type
       from   xxxx_new_comm_contract_basic  xxxx
     join xxxx_oebs_cash_payment_test     oebs on oebs.invoice_id = xxxx.invoice_id
     where xxxx.contract_eid = '45182/15'
       and nvl(xxxx.commission_type, xxxx.discount_type) in (29)
),
s_payments as (
    select contract_id, contract_eid, reward_type,
           from_dt, till_dt,
           nds, currency,
           discount_type,
           sum(amt_w_nds)           as amt_w_nds,
           sum(amt)                 as amt,
           sum(amt)*pct_main/100    as reward,
           sum(amt)*pct_delc/100    as delkredere
      from s_payments_name
     group by contract_id, contract_eid, reward_type,
              from_dt, till_dt, pct_main, pct_delc,
              nds, currency,
              discount_type
),
s_kv as (
    -- для первой части считаем акты/оплаты
    -- BALANCE-21178
    select contract_eid, contract_id,
           from_dt, till_dt,
           discount_type,
           currency, nds,
           sum(turnover_to_charge)      as turnover_to_charge,
           sum(reward_to_charge)        as reward_to_charge,
           sum(delkredere_to_charge)    as delkredere_to_charge,
           sum(turnover_to_pay)         as turnover_to_pay,
           sum(turnover_to_pay_w_nds)   as turnover_to_pay_w_nds,
           sum(reward_to_pay)           as reward_to_pay,
           sum(delkredere_to_pay)       as delkredere_to_pay,
           reward_type
      from (
              select contract_id, contract_eid, reward_type,
                     from_dt, till_dt, discount_type, currency, nds,
                     amt                                as turnover_to_charge,
                     reward                             as reward_to_charge,
                     delkredere                         as delkredere_to_charge,
                     0                                  as turnover_to_pay,
                     0                                  as turnover_to_pay_w_nds,
                     0                                  as reward_to_pay,
                     0                                  as delkredere_to_pay
                from s_acts
               where reward_type = 1
               union all
              select contract_id, contract_eid, reward_type,
                     from_dt, till_dt, discount_type, currency, nds,
                     0                                  as turnover_to_charge,
                     0                                  as reward_to_charge,
                     0                                  as delkredere_to_charge,
                     amt                                as turnover_to_pay,
                     amt_w_nds                          as turnover_to_pay_w_nds,
                     reward                             as reward_to_pay,
                     delkredere                         as delkredere_to_pay
                from s_payments
                where reward_type = 1
           )
     group by contract_eid, contract_id, from_dt, till_dt,
              discount_type, currency, nds, reward_type
     union all
    -- для второй части все считаем от актов
    select contract_eid, contract_id,
           from_dt, till_dt,
           discount_type,
           currency, nds,
           amt          as turnover_to_charge,
           reward       as reward_to_charge,
           delkredere   as delkredere_to_charge,
           amt          as turnover_to_pay,
           amt_w_nds    as turnover_to_pay_w_nds,
           reward       as reward_to_pay,
           delkredere   as delkredere_to_pay,
           reward_type
      from s_acts
     where reward_type = 2
),
-- результирующий запрос
s_comm_2015_audio as (select contract_id,
       contract_eid,
       from_dt,
       till_dt,
       nds,
       currency,
       discount_type,
       reward_type,
       turnover_to_charge,                          -- оборот к начислению
       reward_to_charge,                            -- к начислению
       delkredere_to_charge,
       turnover_to_pay_w_nds,
       turnover_to_pay,                             -- оборот к перечислению
       reward_to_pay,                               -- к перечислению
       delkredere_to_pay
  from (
        select contract_eid, contract_id,
               from_dt, till_dt,
               discount_type,
               currency, nds,
               turnover_to_charge,
               reward_to_charge,
               delkredere_to_charge,
               turnover_to_pay,
               turnover_to_pay_w_nds,
               reward_to_pay,
               delkredere_to_pay,
               reward_type
          from s_kv
       )
      )
 
 
 
 select s.contract_id,
       s.contract_eid,
       s.from_dt,
       s.till_dt,
       s.nds,
       s.currency,
       s.discount_type,
       s.reward_type,
       s.reward_to_charge,
       s.reward_to_pay,
       s.delkredere_to_charge,
       s.delkredere_to_pay,
       s.dkv_to_charge,
       s.dkv_to_pay,
       s.turnover_to_charge,
       s.turnover_to_pay_w_nds,
       s.turnover_to_pay
  from (
    select contract_id,     contract_eid,
           from_dt,         till_dt,
           nds,             currency,
           discount_type,
           reward_type,
           -- к начислению
           turnover_to_charge,
           reward_to_charge,
           delkredere_to_charge,
           null             as dkv_to_charge,
           -- к перечислению
           turnover_to_pay_w_nds,
           turnover_to_pay,
           reward_to_pay,
           delkredere_to_pay,
           null             as dkv_to_pay
      from s_comm_2015_audio
     )       s
     
order by contract_id, from_dt, discount_type, currency, nds;
