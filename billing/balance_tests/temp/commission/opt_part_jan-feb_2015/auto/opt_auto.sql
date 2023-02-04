
with 
--тип комиссии
 s_comm_type as (
    select 56 as id from dual
),                   
--
-- основная выборка по актам
--
s_base_acts as (
select b.contract_eid,
           b.contract_id,
           b.currency,
           b.nds,
           b.discount_type,
           b.payment_type,
           b.commission_payback_type                as payback_type,
           trunc(b.act_dt, 'MM')                        as from_dt,
           add_months(
            trunc(b.act_dt, 'MM'), 1) - 1/84600         as till_dt,
           b.amount                                as amt_w_nds,
           b.amount-b.amount_nds-b.amount_nsp    as amt
      from xxxx_new_comm_contract_basic      b
     where b.hidden < 4
     and b.contract_commission_type = (select id from s_comm_type)
       and b.act_dt between date'2015-01-01' and date'2015-03-01'-1/24/60/60
)
--select * from s_base_acts order by CONTRACT_ID;
,
 --    
-- основная выборка по оплатам
--
s_payments as (
    select b.contract_eid as contract_eid,
           b.contract_id  as contract_id,
           b.currency as currency,
           b.nds as nds,
           b.payment_type as payment_type,
           b.discount_type as discount_type,
           b.commission_payback_type                    as payback_type,
           trunc(oebs.comiss_date, 'MM')                as from_dt,
           add_months(
            trunc(oebs.comiss_date, 'MM'), 1) - 1/84600 as till_dt,
           oebs.oebs_payment                                     as amt_w_nds,
           oebs.oebs_payment*100/(100 + b.nds*b.nds_pct)         as amt
--      From 
--    (Select Distinct  
--            X.Contract_Id As Contract_Id,
--            X.Contract_Eid As Contract_Eid,
--            X.Currency As Currency,
--            X.Nds As Nds,
--            X.Nds_Pct  As  Nds_Pct,
--            X.Discount_Type  As Discount_Type,
--            X.Commission_Payback_Type  As Commission_Payback_Type,
--            X.Invoice_Id As  Invoice_Id,
--            X.Invoice_Eid  As Invoice_Eid,
--            X.Invoice_Dt As  Invoice_Dt ,
--            X.Payment_Type As  Payment_Type,
--            X.Contract_Commission_Type   As Contract_Commission_Type
--      From Xxxx_New_Comm_Contract_Basic X )   B
      from xxxx_new_comm_contract_basic b
      join xxxx_oebs_cash_payment_test     oebs on oebs.invoice_id = b.invoice_id
                                                and oebs.comiss_date is not null
      and b.contract_commission_type = (select t.id from s_comm_type t) 
	 and oebs.comiss_date between date'2015-01-01' and date'2015-03-01'-1/24/60/60
)

--select * from s_payments ; 
,
--
-- основная выборка по оплатам/актам
-- (чтобы договор выводился даже тогда,
-- когда актов нет, но есть оплаты)
--
s_base as (
   select contract_eid, contract_id, currency, discount_type,
         nds, from_dt, till_dt,
        sum(amt_acts) as amt_to_charge,
        sum(decode(payment_type,
              -- предоплата: платим по начисленному (актам)
                2, amt_acts_w_nds,
                  -- постоплата: платим или по деньгам или
                 -- начисленному (от условий в договоре)
                 3, decode(payback_type,
                        1, amt_acts_w_nds,
                        2, amt_oebs_w_nds,
                        0),
                0))           as amt_to_pay_w_nds,
            sum(decode(payment_type,
                -- предоплата: платим по начисленному (актам)
                2, amt_acts,
                -- постоплата: платим или по деньгам или
                -- начисленному (от условий в договоре)
               3, decode(payback_type,
                       1, amt_acts,
                        2, amt_oebs,
                         0),
                 0))           as amt_to_pay
      from (
             select contract_eid, contract_id, currency, nds, discount_type,
                    payment_type, payback_type, from_dt, till_dt,
                    amt  as amt_acts, amt_w_nds  as amt_acts_w_nds,
                    0    as amt_oebs, 0          as amt_oebs_w_nds
              from s_base_acts
              union all
             select contract_eid, contract_id, currency, nds, discount_type,
                    payment_type, payback_type, from_dt, till_dt,
                  0    as amt_acts, 0          as amt_acts_w_nds,
                    amt  as amt_oebs, amt_w_nds  as amt_oebs_w_nds
               from s_payments
            )
     group by contract_eid, contract_id, currency,
              nds, discount_type, from_dt, till_dt
 )
-- select * from s_base order by contract_id;
 ,

-- Подготовка к результату: вычитаем посчитанную ком.часть
s_pre as (
select opt.contract_id,
       opt.contract_eid,
       opt.from_dt,
       opt.till_dt,
       opt.nds,
       opt.currency,
       opt.discount_type,
       -- к начислению
       opt.amt_to_charge                                        as turnover_to_charge,
       opt.amt_to_charge*.08 - nvl(com.reward_to_charge, 0)     as reward_to_charge,
       opt.amt_to_charge*.02 - nvl(com.delkredere_to_charge, 0) as delkredere_to_charge,
       -- к перечислению
       opt.amt_to_pay_w_nds                                     as turnover_to_pay_w_nds,
       opt.amt_to_pay                                           as turnover_to_pay,
       opt.amt_to_pay*.08 - nvl(com.reward_to_pay, 0)           as reward_to_pay,
       opt.amt_to_pay*.02 - nvl(com.delkredere_to_pay, 0)       as delkredere_to_pay
  from s_base                               opt
  left outer
    -- смотрим, что было посчитано в ком.части
    -- чтобы вычесть
  join xxxx_commission_part     com on com.contract_id = opt.contract_id
                                               and com.from_dt = opt.from_dt
                                               and com.till_dt = opt.till_dt
                                               and com.nds = opt.nds
                                               and com.currency = opt.currency
                                               and com.reward_type = 1
                                               and com.discount_type = opt.discount_type
											                        
)
     select * from s_pre;
      
 -- результирующий запрос
 --
 select contract_id,
       contract_eid,
       min(from_dt)    as from_dt,
       max(till_dt)    as till_dt,
       nds,
       currency,
       201                    as reward_type,
       discount_type,
       -- к начислению
        sum(turnover_to_charge)                  as turnover_to_charge,
       sum(reward_to_charge)                    as reward_to_charge,
       sum(delkredere_to_charge)                as delkredere_to_charge,
       null                                     as dkv_to_charge,
       -- к перечислению
       sum(turnover_to_pay_w_nds)               as turnover_to_pay_w_nds,
       sum(turnover_to_pay)                     as turnover_to_pay,
       sum(reward_to_pay)                       as reward_to_pay,
       sum(delkredere_to_pay)                   as delkredere_to_pay,
       null                                     as dkv_to_pay
  from s_pre
 group by contract_id, contract_eid,
          nds, currency, discount_type
  order by contract_id,discount_type,turnover_to_charge,reward_to_charge,dkv_to_charge,turnover_to_pay_w_nds,turnover_to_pay;