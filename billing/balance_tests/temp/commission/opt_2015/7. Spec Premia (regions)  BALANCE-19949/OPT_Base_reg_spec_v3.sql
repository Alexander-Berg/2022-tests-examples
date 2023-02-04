with
s_dates as (
    select date'2015-04-01'   as fin_year_dt
      from dual
)
,
 -- полугоди€
 s_half_years as (
     -- первое полугодие Ч из 5 мес€цев
     select d.dt from_dt, add_months(d.dt, 5)-1/24/60/60 as till_dt
       from (
          select date'2015-04-01' as dt
            from dual
            ) d
      union all
     -- остальные Ч по 6 мес€цев
     select d.dt from_dt, add_months(d.dt, 6)-1/24/60/60 as till_dt
       from (
          select add_months(date'2015-09-01', 6*(level-1)) as dt
            from dual
         connect by level <= 10
            ) d
 ),
-- кварталы
s_quarters as (
    select date'2015-04-01' as from_dt, add_months(date'2015-04-01', 2)-1/24/60/60 as till_dt from dual
     union all
    -- остальные Ч по 6 мес€цев
    select d.dt from_dt, add_months(d.dt, 3)-1/24/60/60 as till_dt
      from (
         select add_months(date'2015-06-01', 3*(level - 1)) as dt
           from dual
        connect by level <= 10
           ) d
),
s_base as (
select distinct
           contract_eid                             as contract_eid,
           contract_id                              as contract_id,
           invoice_eid                              as invoice_eid,
           invoice_id                               as invoice_id,
           invoice_dt                               as invoice_dt,
           contract_from_dt                         as contract_from_dt,
           contract_till_dt                         as contract_till_dt,
           currency                                 as currency,
           nds                                      as nds, 
           nds_pct                                  as nds_pct,
           loyal_client                             as loyal_clients,
       -- BALANCE-17175
           decode(
            nvl(commission_type, discount_type),
            22, 1,
            nvl(commission_type, discount_type)
           )                                        as discount_type,
           payment_type                             as payment_type, 
                                              -- ?  as commission_payback_type
           commission_payback_pct                   as commission_payback_pct,
           contract_commission_type                 as commission_type
  from xxxx_new_comm_contract_basic
  where (
                                                -- BALANCE-17175
                                                (
                                                    -- только счета, созданные в 2015
--                                                    invoice_dt >= date'2015-03-01' and
                                                    -- только базовые/профы
                                                    contract_commission_type in (1, 2, 8) and
                                                    -- то учитываем еще и код 22
                                                    nvl(commission_type,
                                                        discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 17, 19, 22, 28, 29)
                                                )
                                                or
                                                (
                                                    -- иначе, как ранее
                                                    nvl(commission_type,
                                                        discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 17, 19, 28, 29)
                                                )
                                              )
),

  
 s_opt_2015_acts as (
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
       xxx.act_id                                             as act_id,
--       xxx.external_id                                    as act_eid,
       xxx.act_dt                                             as act_dt,
       trunc(xxx.act_dt, 'MM')                                as from_dt,
       add_months(trunc(xxx.act_dt, 'MM'), 1) - 1/84600       as till_dt,
       -- ѕровер€ем, что по договору всегда была -- 1 валюта и 1 Ќƒ—
      count(distinct b.nds)      over (partition by b.contract_id) as nds_count,
      count(distinct b.currency) over (partition by b.contract_id) as currency_count,
       xxx.amount                                        as amt_w_nds,
       xxx.amount-xxx.amount_nds-xxx.amount_nsp            as amt,
       xxx.amount*cr.rate                                as amt_w_nds_rub,
       (xxx.amount-xxx.amount_nds-xxx.amount_nsp)*cr.rate  as amt_rub
  from s_base        b
  join xxxx_new_comm_contract_basic      xxx  on xxx.invoice_id = b.invoice_id
                                          and xxx.hidden < 4
  join xxxx_currency_rate            cr on cr.cc = b.currency
                                          and cr.rate_dt = trunc(xxx.act_dt)
   where b.commission_type in (1, 2, 3, 4, 5, 6, 7, 8)
   and (
          -- base, prof, ua
          (
            b.commission_type in (1, 2, 8)
        and b.discount_type in (1, 2, 3, 7, 11, 12)
          )       
          -- ua
       or (b.commission_type = 6 and b.discount_type in (1, 2, 3, 7, 12))
          -- spec
       or (b.commission_type = 3 and b.discount_type in (17))
          -- auto
       or (b.commission_type = 4 and b.discount_type in (19))
          -- sprav
       or (b.commission_type = 5 and b.discount_type = 12)
       )
)

,
-- јкты по-мес€чно по каждому клиенту
s_acts_m as (
    select b.contract_eid,  b.contract_id,
           b.from_dt,       b.till_dt,
           b.client_id,
           b.nds_count,     b.currency_count,
           sum(b.amt_w_nds_rub)         as amt_w_nds_rub,
           sum(b.amt_rub)               as amt_rub
      from s_opt_2015_acts b
     where b.commission_type in (8)
       and b.discount_type = 7
       and b.act_dt >= (select fin_year_dt from s_dates)
     group by b.contract_eid,  b.contract_id,
              b.from_dt,       b.till_dt,
              b.nds_count,     b.currency_count,
              b.client_id
        -- что услуги Ђяндекс.ƒиректї, св€занные с размещением ћатериалов
        -- каждого такого  лиента, оказывались јгентству в течение каждого
        -- календарного мес€ца ќтчетного квартала
    having sum(b.amt_rub) > 0
),

-- јкты по квартлам (контроль ежемес€чных оборотов)
s_acts_q_pre as (
    select b.contract_eid,  b.contract_id,
           q.from_dt,       q.till_dt,
           b.nds_count,     b.currency_count,
           b.client_id,
           sum(b.amt_w_nds_rub)                             as amt_w_nds_rub,
           sum(b.amt_rub)                                   as amt_rub,
           count(1)                                         as client_in_q
      from s_acts_m           b
      join s_quarters         q on b.from_dt between q.from_dt and q.till_dt
     group by b.contract_eid, b.contract_id,
              q.from_dt,       q.till_dt,
              b.nds_count,     b.currency_count,
              b.client_id
),
s_scales as (
    select 15 as n_from, 20 as n_till, 20 as reward1, 30 as reward2 from dual union all
    select 21          , 29          , 33           , 50            from dual union all
    select 30          , 39          , 47           , 70            from dual union all
    select 40          , 54          , 67           , 100           from dual union all
    select 55          , 79          , 87           , 130           from dual union all
    select 80          , 109         , 107          , 160           from dual union all
    select 110         , 270         , 133          , 200           from dual
),
s_acts_q as (
    select b.*,
          case 
              when nds_count > 1 or currency_count > 1
              then 0
              else
           decode(to_char(from_dt, 'YYYYMM'),
                '201504', s.reward1,
                s.reward2)     
                end          as reward
      from (
        select b.contract_eid,  b.contract_id,
               b.from_dt,       b.till_dt,
               b.nds_count,     b.currency_count,
               sum(b.amt_w_nds_rub)             as amt_w_nds_rub,
               sum(b.amt_rub)                   as amt_rub,
               count(client_id)                 as client_cnt
          from s_acts_q_pre     b
            -- считаем, сколько раз данный клиент по€вл€етс€ в течении
            -- квартала. ƒолжен быть в каждом мес€це квартала, то есть,
            -- обычно 3 раза и 2 раза в 1ом квартале, т.к. он начинаетс€
            -- с апрел€, а не с марта.
         where client_in_q = decode(to_char(from_dt, 'YYYYMM'), '201504', 2, 3)
            -- стоимость фактически оказанных услуг Ђяндекс.ƒиректї дл€
            -- которых, составила в сумме не менее 10 000 (без Ќƒ—) в течение
            -- ѕервого квартала и 15 000 рублей (без Ќƒ—) в течение ¬торого
            -- квартала (по каждому такому  лиенту в отдельности)
           and amt_rub >= decode(to_char(from_dt, 'YYYYMM'), '201504', 10000, 15000)
         group by b.contract_eid,  b.contract_id,
         b.nds_count,     b.currency_count,
                  b.from_dt,       b.till_dt
           ) b
      join s_scales s on b.client_cnt between s.n_from and s.n_till
),


s_opt_2015_base_spec_reg as (
	select b.contract_eid,  b.contract_id,
       h.from_dt,       h.till_dt,
       sum(b.amt_w_nds_rub)                     as amt_w_nds_rub,
       sum(b.amt_rub)                           as amt_rub,
       sum(b.client_cnt)                        as client_cnt,
       sum(b.reward)*1000                       as reward
  from s_acts_q             b
  join s_half_years         h on b.till_dt between h.from_dt and h.till_dt
 group by b.contract_eid,  b.contract_id,
          h.from_dt,       h.till_dt
)

-- select  * from s_opt_2015_base_spec_reg;

select distinct s."CONTRACT_ID",s."CONTRACT_EID",s."FROM_DT",s."TILL_DT",s."NDS",s."CURRENCY",s."DISCOUNT_TYPE",s."REWARD_TYPE",s."TURNOVER_TO_CHARGE",s."REWARD_TO_CHARGE",s."DELKREDERE_TO_CHARGE",s."DKV_TO_CHARGE",s."TURNOVER_TO_PAY",s."TURNOVER_TO_PAY_W_NDS",s."REWARD_TO_PAY",s."DELKREDERE_TO_PAY",s."DKV_TO_PAY"
  from (
    select contract_id,     contract_eid,
           from_dt,         till_dt,
           nds,             currency,
           discount_type,
           300 + reward_type            as reward_type,
           -- к начислению
           sum(turnover_to_charge)      as turnover_to_charge,
           sum(reward_to_charge)        as reward_to_charge,
           0                            as delkredere_to_charge,
           0                            as dkv_to_charge,
           -- к перечислению
           sum(turnover_to_pay)         as turnover_to_pay,
           sum(turnover_to_pay_w_nds)   as turnover_to_pay_w_nds,
           sum(reward_to_pay)           as reward_to_pay,
           0                            as delkredere_to_pay,
           0                            as dkv_to_pay
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
       turnover_to_pay_w_nds,
       turnover_to_pay,                             -- оборот к перечислению
       reward_to_pay                                -- к перечислению
  from (
-- результирующий запрос
 select contract_eid, contract_id,
     from_dt, till_dt,
        null                             as discount_type,
       'RUR'                             as currency,
        1                                as nds,
        amt_rub                          as turnover_to_charge,
        reward                           as reward_to_charge,
        amt_rub                          as turnover_to_pay,
        amt_w_nds_rub                    as turnover_to_pay_w_nds,
        reward                           as reward_to_pay,
        3                                as reward_type
        from s_opt_2015_base_spec_reg))
         group by contract_id,     contract_eid,
              from_dt,         till_dt,
              nds,             currency,
              discount_type,   reward_type)s
order by contract_id, from_dt, discount_type, currency, nds;

