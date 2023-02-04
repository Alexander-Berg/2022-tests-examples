select 
distinct 
contract_id,     contract_eid,
       date'2015-01-01'                         as from_dt,
       date'2015-04-01' - 1/24/60/60            as till_dt,
       nds,             currency,
       discount_type,
       reward_type,
       -- к начислению
       sum(turnover_to_charge)                  as turnover_to_charge,
       sum(reward_to_charge)                    as reward_to_charge,
       sum(delkredere_to_charge)                as delkredere_to_charge,
       sum(dkv_to_charge)                       as dkv_to_charge,
       -- к перечислению
       sum(turnover_to_pay_w_nds)               as turnover_to_pay_w_nds,
       sum(turnover_to_pay)                     as turnover_to_pay,
       sum(reward_to_pay)                       as reward_to_pay,
       sum(delkredere_to_pay)                   as delkredere_to_pay,
       sum(dkv_to_pay)                          as dkv_to_pay
from (
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
       delkredere_to_charge,                        -- к начислению (делькредере)
       dkv_to_charge,                               -- ДКВ за 2012 год к начислению
       turnover_to_pay,                             -- оборот к перечислению
       turnover_to_pay_w_nds,                       -- оборот к перечислению (с НДС)
       reward_to_pay,                               -- к перечислению
       delkredere_to_pay,                           -- к перечислению (делькредере)
       dkv_to_pay                                   -- ДКВ за 2012 год к начислению
  from bo.v_comm_base_2013
   where date'2015-01-01' between from_dt and till_dt
        or date'2015-02-01' between from_dt and till_dt
        or date'2015-03-01' between from_dt and till_dt
 union all
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
       delkredere_to_charge,                        -- к начислению (делькредере)
       dkv_to_charge,                               -- ДКВ за 2012 год к начислению
       turnover_to_pay,                             -- оборот к перечислению
       turnover_to_pay_w_nds,                       -- оборот к перечислению (с НДС)
       reward_to_pay,                               -- к перечислению
       delkredere_to_pay,                           -- к перечислению (делькредере)
       dkv_to_pay                                   -- ДКВ за 2012 год к начислению
  from bo.v_comm_base_2014
   where date'2015-01-01' between from_dt and till_dt
        or date'2015-02-01' between from_dt and till_dt
        or date'2015-03-01' between from_dt and till_dt
)            s
 where reward_type = 10
group by contract_id, contract_eid,
          nds, currency, discount_type, reward_type
  order by  contract_id  , from_dt, discount_type, reward_type, currency,nds,turnover_to_charge, reward_to_charge, turnover_to_pay,reward_to_pay;
