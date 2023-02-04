with

counting_date as (select date'2019-03-03' as dt from dual)
,

s_years as (
    -- Показываем предыдущий, текущий и будущий годы
    select d.dt from_dt, add_months(d.dt, 12) - 1/24/60/60 as till_dt
      from (
         select add_months(add_months(trunc((select dt from counting_date), 'YYYY'), 2), 12*(level-2)) as dt
           from dual
        connect by level <= 3
           ) d
)

,

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
		       29  as discount_type_src,
           c.payment_type                             as payment_type, 
                                              -- ?  as commission_payback_type
           c.commission_payback_pct                   as commission_payback_pct,
           c.contract_commission_type                 as commission_type
  from xxxx_new_comm_contract_basic c
  where (
                                                -- BALANCE-17175
                                                (
                                                    -- только счета, созданные в 2015
--                                                    invoice_dt >= date'2015-03-01' and
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
-- ----------------------------------------------------------------------------
-- основная выборка по актам
-- ----------------------------------------------------------------------------
-- все акты по счетам
s_acts as (
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
		       nvl(xxx.is_loyal, 0)                               as is_loyal,
           xxx.client_id                                      as client_id,
           xxx.act_dt                                         as act_dt,
           trunc(xxx.act_dt, 'MM')                            as from_dt,
           add_months(trunc(xxx.act_dt, 'MM'), 1) - 1/84600   as till_dt,
           --BALANCE-23693, BALANCE-24686
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
     where b.commission_type = 14
        -- Тип комиссии: Спецпроекты.Маркет (33)
   and  b.discount_type  in (33)
)
--select * from s_acts order by invoice_id;

,
s_skv_src as (
    select d.contract_eid, d.contract_id,
           d.contract_from_dt,
           d.contract_till_dt,
           d.currency, d.nds, d.discount_type, d.commission_type,
           d.client_id,
           -- для прогноза (для ЛК не нужно, т.к. ЛК не учитываем в прогнозе)
           -- чтобы знать, за какой период брать оборот для проноза
           d.till_dt                                    as till_dt_fc,
           -- период
           h.from_dt                                    as from_dt,
           trunc(h.till_dt)                             as till_dt,
           sum(d.amt_w_nds_rub)                         as amt_w_nds,
           sum(d.amt_rub)                               as amt

      from s_acts                   d
      
      join s_years                  h  on d.act_dt between h.from_dt
                                                       and h.till_dt
      --BALANCE-23693, BALANCE-24686
      --Проверяем, что по договору всегда была 1 валюта и 1 НДС
      where d.currency_count = 1
            and d.nds_count = 1
     group by h.from_dt, trunc(h.till_dt), d.till_dt,
              d.contract_from_dt,
              d.contract_till_dt,
              d.contract_eid, d.contract_id,
              d.currency, d.nds, d.discount_type, d.commission_type,
              d.client_id
)
,

s_opt_2016_market_spec_skv as (
      select d.contract_eid, d.contract_id,
       d.contract_from_dt,
       d.contract_till_dt,
       d.discount_type, d.commission_type,
       d.client_id,
       d.from_dt, d.till_dt, d.till_dt_fc,
       case
           when contract_eid in (select contract_eid
                                   from bo.v_opt_2015_exclusions)
           then 1
           else 0
       end                      as excluded,
       0                        as failed,
       d.amt_w_nds,
       d.amt                    as amt_for_forecast,
       d.amt
  from s_skv_src d
  )
  
--  select * From s_skv_src; 
,

s_skv as (
    select d.*,
           bo.pk_comm.calc_market_spec_skv(d.amt, d.from_dt) as reward
      from (
        select d.contract_eid, d.contract_id, d.from_dt, d.till_dt,
               -- BALANCE-15641
               sum(amt_w_nds)               as amt_w_nds,
               sum(amt)                     as amt
          from s_opt_2016_market_spec_skv d
         where failed = 0
         group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt
           ) d
)

--select  * from s_skv;

-- результирующий запрос
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
            select contract_eid, contract_id,
               from_dt, till_dt,
               null                             as discount_type,
               -- BALANCE-15641
               'RUR'                            as currency,
               1                                as nds,
               amt                              as turnover_to_charge,
               reward                           as reward_to_charge,
               amt                              as turnover_to_pay,
               amt_w_nds                        as turnover_to_pay_w_nds,
               reward                           as reward_to_pay,
               2                                as reward_type
          from s_skv
))
)            s
  order by contract_id, from_dt, discount_type, currency, nds;
