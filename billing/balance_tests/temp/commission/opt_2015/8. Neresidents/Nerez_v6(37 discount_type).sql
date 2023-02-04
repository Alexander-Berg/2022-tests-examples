with

--
-- основная выборка по актам
--
s_base_src as (  
   select  distinct
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
           xxxx.discount_type                            as discount_type,
           xxxx.payment_type                             as payment_type,    
           xxxx.commission_type                          as commission_type,
           xxxx.loyal_clients                             as loyal_clients
		   ,decode(xxxx.endbuyer_id, 0, null, xxxx.endbuyer_id)		as endbuyer_id
 		   ,case
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
           nvl(x.commission_type, x.discount_type)    as discount_type,
           x.payment_type                             as payment_type,    
           x.contract_commission_type                 as commission_type,
           x.loyal_client                             as loyal_clients,
		       x.endbuyer_id                           		as endbuyer_id,
           x.endbuyer_inn                              as endbuyer_inn,
           x.agency_inn                              as agency_inn,
           x.paysys_id                                    as paysys_id
       from xxxx_new_comm_contract_basic  x
       )   xxxx                 
      where xxxx.discount_type in (1, 2, 3, 7, 11, 12, 14, 19, 36, 37)
)

--select * from s_base_src order by contract_eid;
,
s_acts as (
    select b.contract_eid,
           b.contract_id,
           b.invoice_eid,
           b.invoice_id,
           b.currency,
           b.nds,
           b.payment_type,
           -- BALANCE-24734
          decode(b.discount_type,
                  36, 1,
                   37, 1,
              b.discount_type)                              as discount_type,
           nvl(xxx.is_loyal, 0)                                as loyal_clients,
           trunc(xxx.act_dt, 'MM')                             as from_dt,
           add_months(trunc(xxx.act_dt, 'MM'), 1) - 1/84600    as till_dt,
           xxx.client_id                                       as client_id,
           xxx.act_dt                                          as act_dt,
           xxx.amount                                          as amt_w_nds,
           xxx.amount-xxx.amount_nds-xxx.amount_nsp            as amt
      from s_base_src                       b
      join xxxx_new_comm_contract_basic     xxx on b.invoice_id = xxx.invoice_id
                                              and xxx.hidden <4
                                              and xxx.act_dt >= date'2015-06-01'

 where b.commission_type = 57
)
--select * from s_acts;
,

-- основная выборка по оплатам
--
s_payments as (
    select b.contract_eid,
           b.contract_id,
           b.invoice_eid,
           b.invoice_id,
           b.invoice_dt,
           b.currency,
           b.nds,
           b.nds_pct,
           b.payment_type,
           -- BALANCE-24734
          decode(b.discount_type,
                  36, 1,
                   37, 1,
              b.discount_type)                              as discount_type,
           b.loyal_clients,
           trunc(oebs.comiss_date, 'MM')                            as from_dt,
           add_months(trunc(oebs.comiss_date, 'MM'), 1) - 1/84600   as till_dt,
			-- BALANCE-19238
           oebs.oebs_payment*100/(100 + b.nds*b.nds_pct)         as amt,
           oebs.oebs_payment                                     as amt_w_nds
      from s_base_src                       b
      -- платежи
      join xxxx_oebs_cash_payment_test     oebs on oebs.invoice_id = b.invoice_id
                                                 -- BALANCE-14988
                                                and oebs.comiss_date >= date'2015-06-01'
                                                 -- BALANCE-15631
                                                and oebs.comiss_date is not null

     where b.commission_type = 57
    
)
--select * from s_payments;
,

-- ежемесячное КВ по актам
s_kv_src as (
select contract_id, contract_eid, from_dt, till_dt, payment_type,
         discount_type, currency, nds,
         sum(amt_to_charge)     as turnover_to_charge,
         sum(amt_to_pay)        as turnover_to_pay,
         sum(amt_to_pay_w_nds)  as turnover_to_pay_w_nds,
         sum(amt_to_charge*case
                when discount_type in (7, 1, 2, 3, 36,37)  then 0.13
                when discount_type in (11)          then 0.08
                                                    else 0
            end)                as reward_to_charge,
         sum(amt_to_pay*case
                when discount_type in (7, 1, 2, 3, 36,37)  then 0.13
                when discount_type in (11)          then 0.08
                                                    else 0
            end)                as reward_to_pay,
         sum(amt_to_charge*case
                when discount_type in (7, 11, 1, 2, 3, 36,37)
                then 0.02
                else 0
            end)                as delkredere_to_charge,
         sum(amt_to_pay*case
                when discount_type in (7, 11, 1, 2, 3, 36,37)
                then 0.02
                else 0
            end)                as delkredere_to_pay
  from (
             select contract_id, contract_eid, invoice_eid, invoice_id,
                       from_dt, till_dt, discount_type, currency, nds,
                       payment_type,act_dt as control_dt,
                       amt          as amt_to_charge,
                       0            as amt_to_pay,
                       0            as amt_to_pay_w_nds
                  from s_acts
                 union all
                select contract_id, contract_eid, invoice_eid, invoice_id,
                       from_dt, till_dt, discount_type, currency, nds,
                       payment_type,invoice_dt as control_dt,
                       0            as amt_to_charge,
                       amt          as amt_to_pay,
                       amt_w_nds    as amt_to_pay_w_nds
                  from s_payments
     ) d
     -- BALANCE-21122
    
      where (
            -- BALANCE-28299
            control_dt >= date'2018-03-01' and discount_type in (7, 1, 2, 3, 36, 37)
            -- BALANCE-22085
         or control_dt >= date'2016-03-01' and control_dt < date'2018-03-01'
                                           and discount_type in (7, 1, 2, 3, 36)
         or control_dt  < date'2016-03-01' and discount_type in (7, 11, 1, 2, 3, 36)
         )
    group by contract_eid, contract_id, from_dt, till_dt, payment_type,
            discount_type, currency, nds

)
--select * from s_kv_src  order by from_dt;
,

s_kv01 as (
    select contract_eid, contract_id, from_dt, till_dt,
           discount_type, currency, nds,
           -- к перечислению
           turnover_to_charge,
           reward_to_charge,
           delkredere_to_charge,
           -- к начислению (см. s_kv10)
           0                as turnover_to_pay,
           0                as turnover_to_pay_w_nds,
           -- для предоплаты платим от актов
           decode(payment_type,
                2, reward_to_charge,
                -- постоплата
                3, 0)       as reward_to_pay,
           decode(payment_type,
           --BALANCE-21120
                2, delkredere_to_charge,
                -- постоплата
                3, 0)       as delkredere_to_pay
      from s_kv_src
)
--select * from s_kv01;
,

-- КВ с контролем, что оплат не более, чем актов
-- без разбивки по типам рекламы
s_kv10 as (
    select contract_eid, contract_id, from_dt, till_dt,
           currency, nds,
           -- к перечислению (см. s_kv01)
           0                                    as turnover_to_charge,
           0                                    as reward_to_charge,
           0                                    as delkredere_to_charge,
           -- к начислению
           turnover_to_pay,
           turnover_to_pay_w_nds,
            reward_to_pay,
            delkredere_to_pay
      from (
--            select d.*,   
--                   sum(reward_to_charge)
--                    over(partition by contract_id
--                             order by from_dt)          as reward_to_charge_sum,
--                   sum(reward_to_charge)
--                    over(partition by contract_id
--                             order by from_dt) -
--                                    reward_to_charge    as reward_to_charge_sum_prev,
--                   sum(reward_to_pay)
--                    over(partition by contract_id
--                             order by from_dt)          as reward_to_pay_sum,
--                   sum(reward_to_pay)
--                    over(partition by contract_id
--                             order by from_dt) -
--                                    reward_to_pay       as reward_to_pay_sum_prev,
--                   -- delkredere
--                   sum(delkredere_to_charge)
--                    over(partition by contract_id
--                             order by from_dt)          as delkredere_to_charge_sum,
--                   sum(delkredere_to_charge)
--                    over(partition by contract_id
--                             order by from_dt) - delkredere_to_charge
--                                                        as delkredere_to_charge_sum_prev,
--                   sum(delkredere_to_pay)
--                    over(partition by contract_id
--                             order by from_dt)          as delkredere_to_pay_sum,
--                   sum(delkredere_to_pay)
--                    over(partition by contract_id
--                             order by from_dt) - delkredere_to_pay
--                                                        as delkredere_to_pay_sum_prev
--              from (
                    -- Убираем детализацию по типам рекламы
                select d.contract_id, contract_eid,
                       d.from_dt, d.till_dt, payment_type,
                       d.nds, d.currency,
                       sum(turnover_to_charge)      as turnover_to_charge,
                       sum(reward_to_charge)        as reward_to_charge,
                       sum(delkredere_to_charge)    as delkredere_to_charge,
                       sum(turnover_to_pay)         as turnover_to_pay,
                       sum(turnover_to_pay_w_nds)   as turnover_to_pay_w_nds,
                       sum(reward_to_pay)           as reward_to_pay,
                       sum(delkredere_to_pay)       as delkredere_to_pay
                  from s_kv_src         d
                    -- оплаты показываем только для постоплаты
                 where d.payment_type = 3
                 group by d.contract_eid, d.contract_id,
                          d.from_dt, d.till_dt, payment_type,
                          d.currency, d.nds
--                   ) d
           ) s
)

--select * from s_kv10;
,
--
-- результирующий запрос v_comm_nonrez_2015
--
s_comm_nonrez_2015 as (select contract_id,
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
               1            as reward_type
          from s_kv01
         union all
       select contract_eid, contract_id,
               from_dt, till_dt,
               null         as discount_type,
               currency, nds,
               null         as turnover_to_charge,
               null         as reward_to_charge,
               null         as delkredere_to_charge,
               turnover_to_pay,
               turnover_to_pay_w_nds,
               reward_to_pay,
               delkredere_to_pay,
               10           as type
          from s_kv10
       )
) 

--select * from s_comm_nonrez_2015;

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
      from s_comm_nonrez_2015
     )s--12711.8644

order by contract_id, from_dt, discount_type, currency, nds;
       