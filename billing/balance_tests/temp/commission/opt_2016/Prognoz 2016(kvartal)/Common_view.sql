with
-- опорные даты

s_quarters AS (
    select d.dt from_dt, add_months(d.dt, 3)-1/24/60/60 as till_dt
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
        when c.contract_commission_type in (4,10)
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
--select * from s_base order by  invoice_id ;
,

-- ----------------------------------------------------------------------------
-- основная выборка по актам
-- ----------------------------------------------------------------------------
-- все акты по счетам
s_acts_src as (
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
                --  * включаем Авто (25)
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
, s_q as (
    select contract_eid,        contract_id,
           contract_from_dt,    contract_till_dt,
           case
            -- BALANCE-22667
            -- Все обороты перекидываем в одну шкалу
            -- (в базовых/профах маркета быть не должно)
            when discount_type = 11 then 12
            else commission_type
           end                                              as commission_type,
           discount_type,
           agency_id, q.from_dt, q.till_dt,
           d.till_dt as till_dt_fc,
           -- обороты за певый месяц квартала по договору
           sum(decode(q.from_dt, d.from_dt, amt_rub, 0))    as amt_fm,
           sum(amt_rub)                                     as amt
      from s_acts_src     d
      join s_quarters               q on d.from_dt between q.from_dt and q.till_dt
        -- BALANCE-22667: маркет есть по commission_type in (1, 2, 8) в
        --                v_opt_2015_acts_f только до 2016-03-01
     where (commission_type in (1, 2, 8) and discount_type in (1, 2, 3, 7, 11, 25))
        or commission_type in (12, 13)
     group by contract_eid,        contract_id,
           contract_from_dt,    contract_till_dt,
           discount_type,       commission_type,
           agency_id, d.till_dt,
           q.from_dt, q.till_dt
)
-- стат. данные по агенству за квартал и квартал год назад
, s_agency_stats as (
    select d.*,
           lag(from_dt, 4) over(partition by agency_id, commission_type order by from_dt) as from_dt_prev_q,
           lag(amt_fm, 4) over(partition by agency_id, commission_type order by from_dt) as amt_fm_prev_q,
           lag(amt, 4) over(partition by agency_id, commission_type order by from_dt) as amt_prev_q
      from (
        select agency_id, from_dt, commission_type,
               count(distinct contract_id) as cnt,
               sum(amt_fm) as amt_fm,
               sum(amt) as amt
          from s_q
         group by agency_id, from_dt, commission_type
           ) d
)
, s_src as (
    select d.contract_eid,          d.contract_id,
           d.contract_from_dt,      d.contract_till_dt,
           d.discount_type,         d.commission_type,
           d.from_dt,               d.till_dt,
           d.agency_id,             d.till_dt_fc,
           d.amt                as amt_for_forecast,
           s.amt_fm_prev_q,
           s.amt_prev_q,
           s.from_dt_prev_q,
           0                    as failed,
           -- Исключать только реальном расчете,
           -- в прогнозе должны быть
           case
               when contract_eid in (
                   -- Arrow + MediaGuru
                   '32294/15', '32262/15',
                   -- Блондинка + Aori
                   '32254/15', '32248/15',
                   -- АйКонтекст + Регистратура + Р-Брокер
                   '32242/15', '32388/15', '32287/15',
                   -- АйПроспект + Трафик + Амнет
                   '32290/15', '32428/14', '32260/15',
                   -- Люмолинк
                   '34139/15', '49678/16'
               )
               then 1
               else 0
           end                     as excluded
      from s_q              d
      join s_agency_stats   s on s.agency_id = d.agency_id
                             and s.from_dt = d.from_dt
                             and s.commission_type = d.commission_type
)
select contract_eid||decode(excluded, 1, '(исключение)', '') as contract_eid,
       contract_id,
       contract_from_dt,
       contract_till_dt,
       discount_type,
       commission_type,
       agency_id,
       from_dt, till_dt,
       till_dt_fc,
       amt_for_forecast,
       amt_fm_prev_q,
       amt_prev_q,
       from_dt_prev_q,
       failed,
       excluded
  from s_src;
--select contract_eid, contract_id,
--                  contract_from_dt, contract_till_dt,
--                  amt_fact, amt_forecast,
--                  bo.pk_comm.calc_prof_skv(amt_forecast, date'2016-03-01') as reward
--             from (
--                select contract_eid, contract_id,
--                       from_dt, till_dt,
--                       contract_from_dt,
--                       contract_till_dt,
--                       sum(amt_for_forecast)*100/18    as amt_forecast,
--                       sum(amt_for_forecast)               AS amt_fact
--                  from bo.v_opt_2015_prof_skv
--                 WHERE FAILED = 0
--                   and till_dt_fc between date'2016-03-01' and date'2016-04-01'-1/24/60/60 
--                 group by contract_eid, contract_id,
--                          from_dt, till_dt,
--                          contract_from_dt,
--                          contract_till_dt
--                  );