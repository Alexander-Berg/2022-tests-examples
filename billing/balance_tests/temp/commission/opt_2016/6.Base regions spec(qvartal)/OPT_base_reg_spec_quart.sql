with
s_dates as (
    select date'2016-03-01'   as fin_year_dt
      from dual
),
-- кварталы
s_quarters as (
    select d.dt from_dt, add_months(d.dt, 3)-1/24/60/60 as till_dt
      from (
         select add_months(date'2016-03-01', 3*(level - 1)) as dt
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
--select * from s_base;
,
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
                --  * включаем Авто (25)k
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
--select * from s_opt_2015_acts;
,
-- Акты по-месячно по каждому клиенту
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
        -- что услуги «Яндекс.Директ», связанные с размещением Материалов
        -- каждого такого Клиента, оказывались Агентству в течение каждого
        -- календарного месяца Отчетного квартала
    having sum(b.amt_rub) > 0
)
--select * from s_acts_m;
,

-- Акты по квартлам (контроль ежемесячных оборотов)
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
)
--select * from s_acts_q_pre;
,
-- Шкалы выплат: кол-во клиентов — тыс. рублей премия
s_scales as (
    select 15 as n_from, 20 as n_till, 30 as reward  from dual union all
    select 21          , 29          , 50            from dual union all
    select 30          , 39          , 70            from dual union all
    select 40          , 54          , 100           from dual union all
    select 55          , 79          , 130           from dual union all
    select 80          , 109         , 160           from dual union all
    select 110         , 270         , 200           from dual
),
s_acts_q as (
    select b.*,
           case
            when nds_count > 1 or currency_count > 1
            then 0
            else s.reward
            end                                 as reward
      from (
        select b.contract_eid,  b.contract_id,
               b.from_dt,       b.till_dt,
               b.nds_count,     b.currency_count,
               sum(b.amt_w_nds_rub)             as amt_w_nds_rub,
               sum(b.amt_rub)                   as amt_rub,
               count(client_id)                 as client_cnt
          from s_acts_q_pre     b
            -- считаем, сколько раз данный клиент появляется в течении
            -- квартала. Должен быть в каждом месяце квартала, то есть, 3 раза
         where client_in_q = 3
            -- стоимость фактически оказанных услуг «Яндекс.Директ» для
            -- которых, составила в сумме не менее 15 000 рублей (без НДС)
            -- в течение квартала (по каждому такому Клиенту в отдельности)
           and amt_rub >= 15000
         group by b.contract_eid,  b.contract_id,
                  b.nds_count,     b.currency_count,
                  b.from_dt,       b.till_dt
           ) b
      join s_scales s on b.client_cnt between s.n_from and s.n_till
),


s_opt_2015_base_spec_reg as (
select b.contract_eid,  b.contract_id,
       b.from_dt,       b.till_dt,
       sum(b.amt_w_nds_rub)                     as amt_w_nds_rub,
       sum(b.amt_rub)                           as amt_rub,
       sum(b.client_cnt)                        as client_cnt,
       sum(b.reward)*1000                       as reward
  from s_acts_q             b
 group by b.contract_eid,  b.contract_id,
          b.from_dt,       b.till_dt
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

