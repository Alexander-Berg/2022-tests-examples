
  CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."V_OPT_2015_BASE" ("CONTRACT_ID", "CONTRACT_EID", "FROM_DT", "TILL_DT", "NDS", "CURRENCY", "DISCOUNT_TYPE", "REWARD_TYPE", "TURNOVER_TO_CHARGE", "REWARD_TO_CHARGE", "TURNOVER_TO_PAY_W_NDS", "TURNOVER_TO_PAY", "REWARD_TO_PAY_SRC", "REWARD_TO_PAY") AS 
  with
-- История действия подписанных атрибутов
s_attrs_src as (
    select value_num,
           contract2_id                                         as contract_id,
           code,
           cl_dt                                                as from_dt,
           nvl(
                lead(cl_dt) over(partition by code, contract2_id
                                     order by stamp),
                add_months(trunc(sysdate, 'MM'), 11)
           ) - 1/24/60/60                                       as till_dt
      from bo.mv_contract_signed_attr_hist
),
s_changes_payment_type as (
    select *
      from (
            select s.contract_id, s.from_dt, s.till_dt, s.value_num
              from s_attrs_src s
             where s.code in ('PAYMENT_TYPE')
               and exists (select 1 from s_attrs_src d
                            where d.code in ('WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE')
                              and d.contract_id = s.contract_id)
          )
),
-- все акты по счетам
s_acts as (
    select b.*
      from bo.v_opt_2015_acts_f   b
           -- 1 - Базовая 2015
           -- 8 - Базовая, регионы 2015
           -- 21 - Базовая СПб
     where b.commission_type in (1, 8, 21)
       -- BALANCE-30356: новые акты тут не смотрим (смотрим в python)
       and b.act_dt < date'2019-03-01'
),
-- ----------------------------------------------------------------------------
-- основная выборка по оплатам
-- ----------------------------------------------------------------------------
s_payments as (
    select b.*
      from bo.v_opt_2015_payments b
     where b.commission_type in (1, 8, 21)
       and (
                -- Начиная с 2018-03-01 учитываем только полносью оплаченные счета
                is_2018 = 1 and is_fully_paid = 1
             or is_2018 = 0
                -- BALANCE-28169: показываем только переносы между разными счетами
                -- BALANCE-28622: откаты платежей по одному и тому же счету не показываем
             or amt < 0 and payment_count_by_invoice = 1
                         -- есть платеж с таким же номером но по другому счету
                        and payment_count_by_contract > 1
           )
        -- BALANCE-30356: начиная с 2019-03-01 считаем по новому и в питоне.
        --                в старом расчете оплаты только по старым счетам смотрим
       and b.invoice_dt < date'2019-03-01'
),
-- ----------------------------------------------------------------------------
-- Базовое КВ (месячное)
-- ----------------------------------------------------------------------------
-- Складываем акты и оплаты
s_kv_src as (
    select d.*,
           case
                -- BALANCE-28798: решаем какие условия применять для расчета
                -- "к перечислению" по дате акта. новые условия, 2018
                when d.from_dt >= date'2018-03-01'
                     -- BALANCE-22330: только для новых и пролонгированных
                 and d.contract_till_dt > date'2018-03-01'
                then 1
                else 0
            end                     as is_2018,
           case
                -- BALANCE-22195: решаем какие условия применять для расчета
                -- "к перечислению" по дате акта. новые условия, 2017
                when d.from_dt >= date'2017-03-01'
                     -- BALANCE-22330: только для новых и пролонгированных
                 and d.contract_till_dt > date'2017-03-01'
                 and d.contract_till_dt <= date'2018-03-01'
                then 1
                else 0
            end                     as is_2017
      from (
    select d.contract_eid, d.contract_id,
           d.currency, d.discount_type, discount_type_src,
           contract_from_dt, contract_till_dt,
           d.nds, from_dt, till_dt, payment_type,
           is_agency_credit_line,
           -- BALANCE-25700
           case
            when invoice_dt < date'2017-06-01'
             and invoice_type = 'prepayment'
            then 1
            else 0
           end                      as do_not_fill_src,
           sum(amt_acts)            as amt_to_charge,
           sum(amt_oebs_w_nds)      as amt_to_pay_w_nds,
           -- BALANCE-22195: считаем сумму оплат по старым условиям
           -- и по новым условиям раздельно, чтобы применить к ним разный
           -- процент
           nvl(sum(case when (invoice_dt < date'2017-03-01'
                          -- BALANCE-22330: если счет выставлен по не
                          -- продленному договору (неважно когда),
                          -- то так же считаем его оборотом по условиям 2016
                          or invoice_dt >= date'2017-03-01' and
                             contract_till_dt <= date'2017-03-01'
                             )
                          -- BALANCE-25535
                          -- BALANCE-25224: исключаем оплаты по предоплат.счетам
                         and not i.type = 'prepayment'
                    then amt_oebs
                    else 0
               end), 0)             as amt_to_pay_2016,
           nvl(sum(case when
                          -- BALANCE-22330: если счет выставлен по не
                          -- продленному договору (неважно когда),
                          -- то так же считаем его оборотом по условиям 2016
                             contract_till_dt <= date'2017-03-01'
                          -- BALANCE-25535, BALANCE-25224: только предоплата
                         and i.type = 'prepayment'
                    then amt_acts
                    else 0
               end), 0)             as amt_to_charge_2016,
           nvl(sum(case when invoice_dt >= date'2017-03-01'
                          -- BALANCE-22330: только для продленных договоров
                         and contract_till_dt > date'2017-03-01'
                          -- BALANCE-25535
                          -- BALANCE-25224: исключаем оплаты по предоплат.счетам
                         and not i.type = 'prepayment'
                    then amt_oebs
                    else 0
               end), 0)             as amt_to_pay_2017,
           nvl(sum(case when
                          -- BALANCE-22330: только для продленных договоров
                             contract_till_dt > date'2017-03-01'
                          -- BALANCE-25535, BALANCE-25224: только предоплата
                         and i.type = 'prepayment'
                    then amt_acts
                    else 0
               end), 0)             as amt_to_charge_2017,
           sum(amt_oebs)            as amt_to_pay
      from (
            select contract_eid, contract_id, currency, nds, discount_type,
                   contract_from_dt, contract_till_dt, discount_type_src,
                   from_dt, till_dt, payment_type, invoice_dt, invoice_id,
                   is_agency_credit_line, invoice_type,
                   amt  as amt_acts, amt_w_nds  as amt_acts_w_nds,
                   0    as amt_oebs, 0          as amt_oebs_w_nds
              from s_acts
             where act_dt between add_months(trunc(sysdate, 'MM'), -1)
                              and trunc(sysdate, 'MM') - 1/24/60/60
             union all
            select contract_eid, contract_id, currency, nds, discount_type,
                   contract_from_dt, contract_till_dt, discount_type_src,
                   from_dt, till_dt, payment_type, invoice_dt, invoice_id,
                   is_agency_credit_line, invoice_type,
                   0    as amt_acts, 0          as amt_acts_w_nds,
                   -- BALANCE-28169: для переносов надо учитывать сумму
                   -- корректировки (для is_2018 тут может быть сумма счета)
                   case when amt < 0 then amt else amt_ttl end as amt_oebs,
                   case when amt < 0 then amt_w_nds else amt_ttl_w_nds end as amt_oebs_w_nds
              from s_payments
             where doc_date between add_months(trunc(sysdate, 'MM'), -1)
                                and trunc(sysdate, 'MM') - 1/24/60/60
           )                        d
      join bo.t_invoice             i on i.id = invoice_id
     group by contract_eid, d.contract_id, d.currency, payment_type,
              contract_from_dt, contract_till_dt,
              case when invoice_dt < date'2017-06-01'
                   and invoice_type = 'prepayment' then 1 else 0 end,
              is_agency_credit_line, discount_type_src,
              d.nds, d.discount_type, from_dt, till_dt
           ) d
),
-- Подготовка к контролю (заранее считаем бюджеообразующих клиентов)
s_kv_control_src as (
    select d.*,
           -- клиенты, по каждому из которых сумма актов за отчетный
           -- период составила не менее 1000 руб. Техническая связка нескольких
           -- ClientId считается одним клиентом.
           case
            when amt_rub >= 1000 and contract_till_dt <= date'2018-03-01'
            then brand_id
            when amt_rub >= 1000 and contract_till_dt  > date'2018-03-01'
             and discount_type = 7
            then brand_id
            else null
           end                                              as over1k_brand_id,
           -- Соотношение оборота по Директу по клиенту к
           -- обороту по договору (Агентства)
           nvl(ratio_to_report(amt_rub_direct)
              over (partition by contract_id, from_dt), 0) as ratio
      from (
        select contract_id, from_dt, till_dt, brand_id, client_count_2016,
               contract_from_dt, contract_till_dt, commission_type, discount_type,
               sum(amt_rub)                                  as amt_rub,
               sum(decode(discount_type, 7, amt_rub, 0))     as amt_rub_direct
          from (
        select d.*,
               count(distinct client_id) over
                (partition by contract_id, from_dt)         as client_count_2016
          from s_acts d
            -- BALANCE-25020
            -- BALANCE-22205: исключаем Медийку Я.Авто из оборота для проверок
            --                по Директу, Медийке, Справочнику
         where discount_type <> 25
               )
         group by contract_id, from_dt, till_dt, brand_id, client_count_2016,
                  contract_from_dt, contract_till_dt, commission_type, discount_type
           ) d
),
s_kv_control_pre as (
    select d.*,
           -- учитываем, что в каком-то месяце может не быть оборота
           case
            when add_months(from_dt, -1) = from_dt_1m_ago
            then case when ratio_1m_ago >= 0.7 then 1 else 0 end
            else 0
           end                                          as is_there_boc_1m_ago
      from (
        select d.*,
               lag(from_dt, 1) over (partition by contract_id
                                         order by from_dt)   as from_dt_1m_ago,
               lag(max_client_ratio_by_direct, 1, 0)
                               over (partition by contract_id
                                         order by from_dt)   as ratio_1m_ago,
               case when max_client_ratio_by_direct >= 0.7 then 1 else 0
                end as is_there_boc
          from (
            select contract_id, from_dt, till_dt, client_count_2016,
                   contract_from_dt, contract_till_dt, commission_type,
                   sum(amt_rub)                 as amt_rub,
                   sum(amt_rub_direct)          as amt_rub_direct,
                   count(distinct over1k_brand_id)    as client_count,
                   round(max(ratio), 2)         as max_client_ratio_by_direct
              from s_kv_control_src
             group by contract_id, from_dt, till_dt, client_count_2016,
                      contract_from_dt, contract_till_dt, commission_type
               ) d
           ) d
),
-- Штрафы
--  - оборот по договору >= 200к
--  - клиентов >= 5
--  - нет клиентов с оборотом > 70% (округляется до целых процентов) по Директу
--  - нет нерезидентов
--  - только 1 валюта в счетах
s_kv_control as (
    select d.*,
           case
           when (
                    -- 2018
                    (contract_till_dt > date'2018-03-01'
                     -- если директа не крутилось, то эта проверка
                     -- работать не должна
                        and (client_count >= 5 or amt_rub_direct = 0)
                    )
                    -- 2017
                 or (client_count >= 5 and contract_till_dt > date'2017-03-01'
                                       and contract_till_dt <=date'2018-03-01')
                 or (client_count_2016 >= 5 and contract_till_dt <= date'2017-03-01')
                )
            and (
                 -- Если директа не было, то БОКа тоже не будет
                    is_there_boc = 0
                 or is_there_boc_1m_ago = 0
                )
           then 0
           else 1
            end as failed_bok,
           case
           when (    (  -- BALANCE-25262: по завершенным регионам, старые условия
                        amt_rub >= 100000
                    and contract_till_dt <= date'2017-03-01'
                    and commission_type = 8
                     )
                     -- Условия 2017
                  or (
                        amt_rub >= 200000
                    and contract_till_dt > date'2017-03-01'
                    and contract_till_dt <= date'2018-03-01'
                     )
                     -- Условия 2018
                  or (
                        amt_rub > decode(commission_type, 1, 500000, 200000)
                    and contract_till_dt > date'2018-03-01'
                     )
                )
           then 0
           else 1
            end as failed_amt,
           case
           when (    (  -- BALANCE-25262: по завершенным регионам, старые условия
                        amt_rub >= 100000
                    and contract_till_dt <= date'2017-03-01'
                    and commission_type = 8
                     )
                     -- Условия 2017
                  or (
                        amt_rub >= 200000
                    and contract_till_dt > date'2017-03-01'
                    and contract_till_dt <= date'2018-03-01'
                     )
                     -- Условия 2018
                  or (
                        amt_rub > decode(commission_type, 1, 500000, 200000)
                    and contract_till_dt > date'2018-03-01'
                     )
                )
            and (
                    -- 2018
                    (contract_till_dt > date'2018-03-01'
                     -- если директа не крутилось, то эта проверка
                     -- работать не должна
                        and (client_count >= 5 or amt_rub_direct = 0)
                    )
                    -- 2017
                 or (client_count >= 5 and contract_till_dt > date'2017-03-01'
                                       and contract_till_dt <=date'2018-03-01')
                 or (client_count_2016 >= 5 and contract_till_dt <= date'2017-03-01')
                )
            and (
                 -- Если директа не было, то БОКа тоже не будет
                    is_there_boc = 0
                 or is_there_boc_1m_ago = 0
                )
           then 0
           else 1
            end as failed
      from s_kv_control_pre d
),
-- Сумма КВ без контроля акты <= оплат
s_kv_pre as (
    select d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           d.payment_type,
           d.nds, d.currency,
           d.discount_type,
           sum(d.turnover_to_charge)        as turnover_to_charge,
           sum(d.reward_to_charge)          as reward_to_charge,
           sum(d.turnover_to_pay)           as turnover_to_pay,
           sum(d.turnover_to_pay_w_nds)     as turnover_to_pay_w_nds,
           sum(decode(d.do_not_fill_src,
                0, d.reward_to_pay,
                0))                         as reward_to_pay_src,
           sum(d.reward_to_pay)             as reward_to_pay
     from (
    select d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           d.payment_type,
           d.nds, d.currency,
           d.do_not_fill_src,
           -- BALANCE-25071: Для непродленных договоров выводить расчет по 36 и
           -- 1 типу комиссии одной строкой
           case
            when d.discount_type = 36
             and d.is_2017 = 0
             and d.is_2018 = 0
            then 1
            else d.discount_type
           end                          as discount_type,
           sum(d.amt_to_charge)         as turnover_to_charge,
           sum(case
                when d.is_2017 = 1 or d.is_2018 = 1 then
                    d.amt_to_charge*decode(
                                        -- BALANCE-28798: в зависимости от года
                                        -- смотрим на разные признаки
                                        decode(d.is_2017, 1, f.failed, f.failed_amt),
                                        1, 0,
                                        -- если наказывать не за что, что считаем
                                        case
                                        when d.discount_type in (36) then 0.15
                                        when d.discount_type in (1, 2, 3) then 0.13
                                        when d.discount_type in (7, 37) then
                                            case
                                                 -- BALANCE-28739
                                                when d.discount_type = 7
                                                 and f.failed_bok = 1
                                                then 0
                                                -- для договоров по постоплате
                                                -- для счета по агентской кредитной
                                                -- линии — 7/12 %
                                                when payment_type = 3
                                                 and is_agency_credit_line = 1
                                                then decode(d.discount_type,
                                                        7, 0.07,
                                                        37, 0.12)
                                                -- остальным — 8/13 %
                                                else decode(d.discount_type,
                                                        7, 0.08,
                                                        37, 0.13)
                                            end
                                        when d.discount_type in (12) then 0.08
                                        else 0
                                        end
                                    )
                -- BALANCE-22195: старые условия, 2016
                else d.amt_to_charge*decode(d.discount_type,
                         -- Авто.ру считаем по шкале
                         25, case
                                 when d.amt_to_charge >= 5000000 then 0.2
                                 when d.amt_to_charge >= 4000000 then 0.18
                                 when d.amt_to_charge >= 3000000 then 0.16
                                 when d.amt_to_charge >= 2000000 then 0.14
                                 when d.amt_to_charge >= 1000000 then 0.12
                                 when d.amt_to_charge >=   50000 then 0.10
                                 else 0
                             end,
                         -- Для остальных — 8%
                         decode(f.failed,
                             0, 0.08,
                             0))
               end)                     as reward_to_charge,
           sum(d.amt_to_pay)            as turnover_to_pay,
           sum(d.amt_to_pay_w_nds)      as turnover_to_pay_w_nds,
           -- BALANCE-19851
           -- Штрафовать по оплатам не должны
           -- BALANCE-22195: применяем старые и новые условия раздельно
           sum((d.amt_to_pay_2016 + decode(f.failed,    -- BALANCE-25801
                                        0, d.amt_to_charge_2016, 0)) *
             decode(d.discount_type,
                    -- Авто.ру считаем иначе
                    25, case
                            when d.amt_to_pay_2016 >= 5000000 then 0.2
                            when d.amt_to_pay_2016 >= 4000000 then 0.18
                            when d.amt_to_pay_2016 >= 3000000 then 0.16
                            when d.amt_to_pay_2016 >= 2000000 then 0.14
                            when d.amt_to_pay_2016 >= 1000000 then 0.12
                            -- BALANCE-22241: минимального порога по
                            -- оплатам нет, должны учесть всё. но
                            -- реально выплатим не более, чем по актам
                            else                              0.10
                        end,
                    -- Для остальных — 8%
                    0.08)
                +
                (d.amt_to_pay_2017 +
                    -- BALANCE-25801
                    -- BALANCE-28739
                    case
                      -- BALANCE-28798: если договор закончился, то штрафем по
                      -- старым условиям
                    when d.is_2017 = 1 and f.failed = 0            then d.amt_to_charge_2017
                    when d.is_2018 = 1 and d.discount_type =  7
                                       and f.failed_amt = 0
                                       and f.failed_bok = 0        then d.amt_to_charge_2017
                    when d.is_2018 = 1 and d.discount_type != 7
                                       and f.failed_amt = 0        then d.amt_to_charge_2017
                    else 0
                    end
                ) * case
                                    when d.discount_type in (36) then 0.15
                                    when d.discount_type in (1, 2, 3) then 0.13
                                    when d.discount_type in (7, 37) then
                                        case
                                            -- для договоров по постоплате
                                            -- для счета по агентской кредитной
                                            -- линии — 7/12 %
                                            when payment_type = 3
                                             and is_agency_credit_line = 1
                                            then decode(d.discount_type,
                                                    7, 0.07,
                                                    37, 0.12)
                                            -- остальным — 8/13 %
                                            else decode(d.discount_type,
                                                    7, 0.08,
                                                    37, 0.13)
                                        end
                                    when d.discount_type in (12) then 0.08
                                    else 0
                                   end)         as reward_to_pay
      from s_kv_src         d
      -- BALANCE-19851
      left outer
      join s_kv_control     f on f.contract_id = d.contract_id
                             and f.from_dt = d.from_dt
                             and f.till_dt = d.till_dt
     group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt, d.payment_type,
              d.discount_type, d.currency, d.nds, d.is_2017, d.is_2018, d.do_not_fill_src
     union all
    -- до-перечисляем 1% по Директу
    select d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           3                        as payment_type,
           d.nds, d.currency,
           -- этих денег еще не светилось, поэтому надо выводить в reward_to_pay_src
           0                        as do_not_fill_src,
           discount_type,
           0                        as turnover_to_charge,
           d.amt_by_invoice*0.01    as reward_to_charge,
           0                        as turnover_to_pay,
           0                        as turnover_to_pay_w_nds,
           d.amt_by_invoice*0.01    as reward_to_pay
      from bo.v_opt_2015_payments       d
        -- Дополнительные условия для начисления вознаграждения за досрочную
        -- оплату: В отчетном периоде, в котором был выставлен счет, агентство
        -- выполнило условия выплаты базовой премии для Директа. Если условия
        -- выплаты базовой премии не выполняются, вознаграждение за досрочную
        -- оплату счета не начисляется и не перечисляется
      join s_kv_control                 f on f.contract_id = d.contract_id
                                         and d.invoice_dt between f.from_dt
                                                              and f.till_dt
                                          -- BALANCE-28814: фейл по 37 и 7 по
                                          -- разным критериям в 2018 году
                                         and case
                                             when invoice_dt > date'2018-03-01'
                                             then decode(discount_type,
                                                    7, f.failed,
                                                    f.failed_amt)
                                             else f.failed
                                             end = 0
     where d.is_early_payment = 1
       and d.doc_date between add_months(trunc(sysdate, 'MM'), -1)
                          and trunc(sysdate, 'MM') - 1/24/60/60
        -- BALANCE-25161: только по новым счетам
       and d.invoice_dt >= date'2017-03-01'
           )  d
     group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt, d.payment_type,
              d.discount_type, d.currency, d.nds
),
-- КВ с разбивкой по типам рекламы, без контроля, что оплат не более, чем актов
s_kv01 as (
    select contract_eid, contract_id, from_dt, till_dt,
           discount_type, currency, nds,
           -- к перечислению
           turnover_to_charge,
           reward_to_charge,
           -- к начислению (см. s_kv10)
           0                as turnover_to_pay,
           0                as turnover_to_pay_w_nds,
           decode(payment_type,
                -- BALANCE-19979: для предоплаты платим от актов
                2, reward_to_charge,
                -- постоплата
                3, 0)       as reward_to_pay
      from s_kv_pre
),
s_kv10_src as (
    select d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           d.nds, d.currency,
           d.turnover_to_charge,
           d.reward_to_charge,
           d.turnover_to_pay,
           d.turnover_to_pay_w_nds,
           d.reward_to_pay_src,
           d.reward_to_pay
      from s_kv_pre         d
        -- оплаты показываем только для постоплаты
     where payment_type = 3
     union all
        -- BALANCE-24627
        -- История, чтобы раздать "долги", если таковые остались
        -- с прошлых месяцев
    select d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           d.nds, d.currency,
           sum(d.turnover_to_charge)    as turnover_to_charge,
           sum(d.reward_to_charge)      as reward_to_charge,
           sum(d.turnover_to_pay)       as turnover_to_pay,
           sum(d.turnover_to_pay_w_nds) as turnover_to_pay_w_nds,
           sum(d.reward_to_pay_src)     as reward_to_pay_src,
           sum(d.reward_to_pay_src)     as reward_to_pay
      from (
          select d.contract_id, d.contract_eid, d.reward_type,
                 d.from_dt, d.till_dt,
                 d.nds, d.currency,
                 d.turnover_to_charge,
                 d.reward_to_charge,
                 d.turnover_to_pay,
                 d.turnover_to_pay_w_nds,
                 case
                   -- Для ЛК платили от актов, поэтому надо учесть reward_to_pay
                 when d.discount_type = 71
                 then d.reward_to_pay
                      -- если в это время был предоплатником, то платим от актов
                 when nvl(chpt.value_num, 3) = 2
                   -- Пример: contract_id = 239691, 2016-10. Станет постоплатой
                   -- только в след. месяце, но уже в 2016-10 есть 310 строка,
                   -- из которой надо достать реальную оплату, а не акты
                  and d.reward_type = 301
                      -- заполняется у предоплаты
                 then d.reward_to_pay
                 else d.reward_to_pay_src
                  end                       as  reward_to_pay_src
           -- BALANCE-25224: историю ищем совместную
           -- BALANCE-26154: смотрим всю историю, а не только с не-0-ыми премиями
           --                чтобы не терять оплаты
         from bo.v_ar_rewards d
         left outer
         join s_changes_payment_type chpt on chpt.contract_id = d.contract_id
                                         and d.till_dt between chpt.from_dt
                                                           and chpt.till_dt
         where d.contract_id in (
                    select contract_id from s_kv_pre
                     where payment_type = 3
               )
           and d.reward_type in (310, 410, 510, 301, 401, 501)
            -- BALANCE-24877: исключаем расчеты за тек.период, если это
            --                не первый расчет за расчет.период
           and d.from_dt < add_months(trunc(sysdate, 'MM'), -1)
           ) d
  group by d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           d.nds, d.currency
),
-- КВ с контролем, что оплат не более, чем актов
-- без разбивки по типам рекламы
s_kv10 as (
    select contract_eid, contract_id, from_dt, till_dt,
           currency, nds,
           -- к перечислению (см. s_kv01)
           0                                    as turnover_to_charge,
           0                                    as reward_to_charge,
           -- к начислению
           turnover_to_pay,
           reward_to_pay_src,
           turnover_to_pay_w_nds,
           (least(reward_to_charge_sum, reward_to_pay_sum) -
                least(reward_to_charge_sum_prev, reward_to_pay_sum_prev)
           )                                    as reward_to_pay
      from (
            select d.*,
                   sum(reward_to_charge)
                    over(partition by contract_id
                             order by from_dt)          as reward_to_charge_sum,
                   sum(reward_to_charge)
                    over(partition by contract_id
                             order by from_dt) -
                                    reward_to_charge    as reward_to_charge_sum_prev,
                   sum(reward_to_pay)
                    over(partition by contract_id
                             order by from_dt)          as reward_to_pay_sum,
                   sum(reward_to_pay)
                    over(partition by contract_id
                             order by from_dt) -
                                    reward_to_pay       as reward_to_pay_sum_prev
              from (
                    -- Убираем детализацию по типам рекламы
                select d.contract_id, contract_eid,
                       d.from_dt, d.till_dt,
                       d.nds, d.currency,
                       sum(turnover_to_charge)      as turnover_to_charge,
                       sum(reward_to_charge)        as reward_to_charge,
                       sum(turnover_to_pay)         as turnover_to_pay,
                       sum(turnover_to_pay_w_nds)   as turnover_to_pay_w_nds,
                       sum(reward_to_pay_src)       as reward_to_pay_src,
                       sum(reward_to_pay)           as reward_to_pay
                  from s_kv10_src         d
                 group by d.contract_eid, d.contract_id,
                          d.from_dt, d.till_dt,
                          d.currency, d.nds
                   ) d
           ) s
        -- Показываем только предыдущий месяц
     where from_dt between add_months(trunc(sysdate, 'MM'), -1)
                       and trunc(sysdate, 'MM') - 1/24/60/60
)
-- СКВ (полугодие)
-- BALANCE-19948, BALANCE-19941
-- BALANCE-27476: считаем в питоне
--s_skv as (
--    select contract_eid, contract_id, from_dt, till_dt, nds, discount_type,
--           sum(amt_rub)         as amt_rub,
--           sum(amt_w_nds_rub)   as amt_w_nds_rub,
--           sum(reward)          as reward
--      from (
--    select d.*,
--           case
--            when nds_count = 1 and currency_count = 1
--            then bo.pk_comm.calc_base_skv(d.amt_rub, d.from_dt, d.discount_type)
--            else 0
--           end                                  as reward
--      from (
--        select d.contract_eid, d.contract_id, d.from_dt, d.till_dt, d.nds,
--               d.nds_count, d.currency_count, d.discount_type,
--               -- BALANCE-15641
--               sum(amt_w_nds_rub)               as amt_w_nds_rub,
--               sum(amt_rub)                     as amt_rub
--          from bo.v_opt_2015_base_skv d
--         where failed = 0
--           and excluded = 0
--         group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt, d.nds,
--                  d.nds_count, d.currency_count, d.discount_type
--           ) d
--           )
--     group by contract_eid, contract_id, from_dt, till_dt, nds, discount_type
--)
-- BALANCE-22696
-- BALANCE-22698
, s_q_by_month as (
    -- Т.к. суммы в исходных строках с разным уровнем гранулированности,
    -- то приводим сначала их все к уровню месяца.
    -- Исходные данные:
    --      тек. оборот по договору — помесячный по сервису
    --      тек. оборот по аг-ву    — поквартальный
    --      прошлый оборот по аг-ву — поквартальный
    select /*+ parallel(16)
               opt_param('optimizer_index_caching' 0)
               opt_param('optimizer_index_cost_adj' 1000)
           */
           contract_eid, contract_id, from_dt, till_dt, agency_id,
           till_dt_fc, amt_ag_prev, amt_ag_prev_fm, discount_type,
           amt_ag_q                     as amt_ag,
           -- тек. оборот по договору — поднимаем до месяца
           sum(amt)                     as amt_rub,
           sum(amt_w_nds_rub)           as amt_w_nds_rub
      from bo.v_opt_2015_base_q
     where failed = 0
       and excluded = 0
       and failed_bok = 0
       and failed_media = 0
       and trunc(sysdate, 'MM') - 1 between from_dt and till_dt
     group by contract_eid, contract_id, from_dt, till_dt, agency_id,
              till_dt_fc, amt_ag_q, amt_ag_prev, amt_ag_prev_fm, discount_type
)
-- только консолидированные на главный договоры,
-- и обычные (которые не надо консолидировать)
, s_q_growth as (
    select d.*,
               bo.pk_comm.calc_base_q(
                    amt_rub,
                    amt_ag,
                    amt_ag_prev,
                    amt_ag_prev_fm,
                    discount_type
               )                             as reward
      from (
            -- Теперь обороты поднимаем до уровня квартала
        select d.contract_eid, d.contract_id, d.from_dt, d.till_dt, d.agency_id,
               discount_type,
               -- прошлый оборот по аг-ву — уже за квартал, ничего не делаем
               amt_ag_prev,
               amt_ag_prev_fm,
               -- тек. оборот по договору — поднимаем до квартала
               sum(amt_rub)                 as amt_rub,
               sum(amt_w_nds_rub)           as amt_w_nds_rub,
               amt_ag
          from s_q_by_month d
         group by contract_eid, contract_id, from_dt, till_dt, agency_id,
                  discount_type,
                  amt_ag_prev, amt_ag_prev_fm, amt_ag
           ) d
)
-- отбираем исходные обороты договоров, по которым надо сделать
-- пропорциональную консолидацию
, s_q_growth_not_consdted as (
    select d.contract_id, d.contract_eid,
           d.main_contract_id,
           d.discount_type,
           d.from_dt,
           sum(d.amt)                       as amt_rub,
           sum(d.amt_w_nds_rub)             as amt_w_nds_rub
      from (
            -- консолидированные договоры
        select d.contract_id_orig                       as contract_id,
               d.contract_eid_orig                      as contract_eid,
               d.contract_id                            as main_contract_id,
               d.from_dt,
               d.discount_type,
               d.act_dt,
               d.amt_rub                                as amt,
               d.amt_w_nds_rub
            -- BALANCE-28455: смотрим все обороты, т.к. шкалы могут быть разными
          from bo.v_ar_acts_q         d
            -- Шкала с учетом консолидации
         where d.commission_type in (1, 8, 21)
            -- Пропорционально обороту
           and d.cons_type = 1
            -- только те, кто не сфейлился
           and d.failed_bok = 0
           and d.failed_media = 0
            ) d
     group by d.contract_id, d.contract_eid, d.main_contract_id, d.discount_type, d.from_dt
)
, s_q_growth_proply_consolidated as (
    select d.contract_eid, d.contract_id,
           r.from_dt, r.till_dt, d.discount_type,
           d.amt_rub, d.amt_w_nds_rub,
           -- пропорционально раскладываем премию
           r.reward*d.amt_rub/r.amt_rub     as reward
      from s_q_growth_not_consdted              d
      join s_q_growth                           r on r.contract_id = d.main_contract_id
                                                 and r.from_dt = d.from_dt
                                                 and r.discount_type = d.discount_type
)
, s_q_audio as (
    select *
      from bo.v_opt_2017_audio_q
     where commission_type in (1, 8, 21)
)
, s_q as (
             -- PAYSYSADMIN-3791
        select /*+ OPTIMIZER_FEATURES_ENABLE('12.1.0.1') */
               contract_eid, contract_id, from_dt, till_dt,
               discount_type,
               sum(amt_rub)         as amt_rub,
               sum(amt_w_nds_rub)   as amt_w_nds_rub,
               sum(reward)          as reward
          from (
                -- Тут только:
                --  - неконсолидированные договоры
                --  - консолидированные договоры с консолидацией на главный договор
            select contract_eid, contract_id, from_dt, till_dt,
                   discount_type,
                   amt_rub, amt_w_nds_rub, reward
              from s_q_growth
             where contract_id not in (
                    select contract_id
                      from bo.mv_ar_consolidations_q
                     where cons_type = 1)
             union all
                -- пропорционально консолидированные
            select contract_eid, contract_id, from_dt, till_dt,
                   discount_type,
                   amt_rub, amt_w_nds_rub, reward
              from s_q_growth_proply_consolidated
             union all
            select contract_eid, contract_id, from_dt, till_dt,
                   -- BALANCE-27450#1525795196000
                   1        as discount_type,
                   amt_rub, amt_w_nds_rub, reward
              from s_q_audio
               )
        group by contract_eid, contract_id, from_dt, till_dt, discount_type
)
-- результирующий запрос
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
       reward_to_pay_src,                           -- к перечислению
       reward_to_pay                                -- к перечислению
  from (
        select contract_eid, contract_id,
               from_dt, till_dt,
               discount_type,
               currency, nds,
               turnover_to_charge,
               reward_to_charge,
               turnover_to_pay,
               turnover_to_pay_w_nds,
               reward_to_pay,
               reward_to_pay as reward_to_pay_src,
               1            as reward_type
          from s_kv01
         union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               null         as discount_type,
               currency, nds,
               turnover_to_charge,
               reward_to_charge,
               turnover_to_pay,
               turnover_to_pay_w_nds,
               reward_to_pay,
               reward_to_pay_src,
               10           as reward_type
          from s_kv10
         union all
--        select contract_eid, contract_id,
--               from_dt, till_dt,
--               discount_type,
--               -- BALANCE-15641
--               'RUR'                            as currency,
--               nds,
--               amt_rub                          as turnover_to_charge,
--               reward                           as reward_to_charge,
--               amt_rub                          as turnover_to_pay,
--               amt_w_nds_rub                    as turnover_to_pay_w_nds,
--               reward                           as reward_to_pay,
--               reward                           as reward_to_pay_src,
--               2                                as type
--          from s_skv
--            -- BALANCE-24627: полугод считаем только 2 раза в год
--         where to_char(sysdate, 'MM') in ('03', '09')
--         union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               discount_type,
               'RUR'                            as currency,
               1                                as nds,
               amt_rub                          as turnover_to_charge,
               reward                           as reward_to_charge,
               amt_rub                          as turnover_to_pay,
               amt_w_nds_rub                    as turnover_to_pay_w_nds,
               reward                           as reward_to_pay,
               reward                           as reward_to_pay_src,
               20                               as type
          from s_q
            -- BALANCE-24627: квартальные премии считаем не всегда
         where to_char(sysdate, 'MM') in ('03', '06', '09', '12')
         union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               null                             as discount_type,
               'RUR'                            as currency,
               1                                as nds,
               amt_rub                          as turnover_to_charge,
               reward                           as reward_to_charge,
               amt_rub                          as turnover_to_pay,
               amt_w_nds_rub                    as turnover_to_pay_w_nds,
               reward                           as reward_to_pay,
               reward                           as reward_to_pay_src,
               3                                as type
          from bo.v_opt_2015_base_spec_reg
       );
