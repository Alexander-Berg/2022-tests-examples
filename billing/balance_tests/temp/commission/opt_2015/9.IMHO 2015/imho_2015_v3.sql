with
s_dates as (
  select date'2015-03-01'     as start_dt
    from dual
),

s_acts_imho as (  
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
           (xxxx.amount)*cr.rate                              as amt_rub_w_nds,
           (xxxx.amount-xxxx.amount_nds-xxxx.amount_nsp)*cr.rate  as amt_rub,
          24.93                                            as pct_main,
           2                                                as pct_delc,
           1                                                as reward_type
     from   xxxx_new_comm_contract_basic  xxxx
                                             
      join xxxx_currency_rate               cr on cr.cc = xxxx.currency
                                              and cr.rate_dt = trunc(xxxx.act_dt)
	   where  xxxx.hidden <4 
			and xxxx.contract_eid = '23562/13'

      and nvl(xxxx.commission_type, xxxx.discount_type) in (1, 2, 3)
    )         

,

s_acts_by_products as (  
   select  '23562/13'                                       as contract_eid,
           176800                                           as contract_id,
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
           (xxxx.amount)*cr.rate                              as amt_rub_w_nds,
           (xxxx.amount-xxxx.amount_nds-xxxx.amount_nsp)*cr.rate  as amt_rub,
          10.2                                             as pct_main,
           0                                                as pct_delc,
           2                                                as reward_type
     from   xxxx_new_comm_contract_basic  xxxx
                                             
      join xxxx_currency_rate               cr on cr.cc = xxxx.currency
                                              and cr.rate_dt = trunc(xxxx.act_dt)
	    where  xxxx.hidden <4 
			and xxxx.contract_eid != '23562/13'
            and xxxx.firm_id = 1
            and nvl(xxxx.commission_type, xxxx.discount_type) in (1, 2, 3)
			and xxxx.product_id in (504096
                                                    , 504109
                                                    , 504118
                                                    , 504127
                                                    , 504115
                                                    , 504130
                                                    , 504121
                                                    , 504112
                                                    , 504124
                                                    , 504095
                                                    , 504100
                                                    , 504098
                                                    , 504108
                                                    , 504117
                                                    , 504126
                                                    , 504114
                                                    , 504129
                                                    , 504120
                                                    , 504111
                                                    , 504123
                                                    , 504154
                                                    , 504468
                                                    , 504286
                                                    , 504443
                                                    , 504094
                                                    , 504099
                                                    , 504692
                                                    , 504097
                                                    , 504107
                                                    , 505131
                                                    , 504116
                                                    , 504125
                                                    , 504113
                                                    , 504128
                                                    , 504119
                                                    , 504110
                                                    , 504122)
    )         

,
s_acts as (
    select * from s_acts_imho
    union all
    select * from s_acts_by_products
),
s_kv as (
    select contract_id, contract_eid,reward_type,
           from_dt, till_dt,
           nds, currency,
           discount_type,
           sum(amt_w_nds)   as amt_w_nds,
           sum(amt)         as amt,
           sum(amt)*pct_main/100    as reward,
           sum(amt)*pct_delc/100    as delkredere
      from s_acts
     group by contract_id, contract_eid,reward_type,
              from_dt, till_dt,pct_main, pct_delc,
              nds, currency,
              discount_type
),
-- результирующий запрос
s_comm_2015_imho as (select contract_id,
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
               amt          as turnover_to_charge,
               reward       as reward_to_charge,
               delkredere   as delkredere_to_charge,
               amt          as turnover_to_pay,
               amt_w_nds    as turnover_to_pay_w_nds,
               reward       as reward_to_pay,
               delkredere   as delkredere_to_pay,
               reward_type
          from s_kv )
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
           0                    as dkv_to_charge,
           -- к перечислению
           turnover_to_pay_w_nds,
           turnover_to_pay,
           reward_to_pay,
           delkredere_to_pay,
           0                    as dkv_to_pay
      from s_comm_2015_imho
     )       s
     
          order by contract_id, from_dt, discount_type, currency, nds;
