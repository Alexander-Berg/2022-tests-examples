
  CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."V_OPT_2015_BASE" ("CONTRACT_ID", "CONTRACT_EID", "FROM_DT", "TILL_DT", "NDS", "CURRENCY", "DISCOUNT_TYPE", "REWARD_TYPE", "TURNOVER_TO_CHARGE", "REWARD_TO_CHARGE", "TURNOVER_TO_PAY_W_NDS", "TURNOVER_TO_PAY", "REWARD_TO_PAY_SRC", "REWARD_TO_PAY") AS 
  with
-- ������� �������� ����������� ���������
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
-- ��� ���� �� ������
s_acts as (
    select b.*
      from bo.v_opt_2015_acts_f   b
           -- 1 - ������� 2015
           -- 8 - �������, ������� 2015
           -- 21 - ������� ���
     where b.commission_type in (1, 8, 21)
       -- BALANCE-30356: ����� ���� ��� �� ������� (������� � python)
       and b.act_dt < date'2019-03-01'
),
-- ----------------------------------------------------------------------------
-- �������� ������� �� �������
-- ----------------------------------------------------------------------------
s_payments as (
    select b.*
      from bo.v_opt_2015_payments b
     where b.commission_type in (1, 8, 21)
       and (
                -- ������� � 2018-03-01 ��������� ������ �������� ���������� �����
                is_2018 = 1 and is_fully_paid = 1
             or is_2018 = 0
                -- BALANCE-28169: ���������� ������ �������� ����� ������� �������
                -- BALANCE-28622: ������ �������� �� ������ � ���� �� ����� �� ����������
             or amt < 0 and payment_count_by_invoice = 1
                         -- ���� ������ � ����� �� ������� �� �� ������� �����
                        and payment_count_by_contract > 1
           )
        -- BALANCE-30356: ������� � 2019-03-01 ������� �� ������ � � ������.
        --                � ������ ������� ������ ������ �� ������ ������ �������
       and b.invoice_dt < date'2019-03-01'
),
-- ----------------------------------------------------------------------------
-- ������� �� (��������)
-- ----------------------------------------------------------------------------
-- ���������� ���� � ������
s_kv_src as (
    select d.*,
           case
                -- BALANCE-28798: ������ ����� ������� ��������� ��� �������
                -- "� ������������" �� ���� ����. ����� �������, 2018
                when d.from_dt >= date'2018-03-01'
                     -- BALANCE-22330: ������ ��� ����� � ����������������
                 and d.contract_till_dt > date'2018-03-01'
                then 1
                else 0
            end                     as is_2018,
           case
                -- BALANCE-22195: ������ ����� ������� ��������� ��� �������
                -- "� ������������" �� ���� ����. ����� �������, 2017
                when d.from_dt >= date'2017-03-01'
                     -- BALANCE-22330: ������ ��� ����� � ����������������
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
           -- BALANCE-22195: ������� ����� ����� �� ������ ��������
           -- � �� ����� �������� ���������, ����� ��������� � ��� ������
           -- �������
           nvl(sum(case when (invoice_dt < date'2017-03-01'
                          -- BALANCE-22330: ���� ���� ��������� �� ��
                          -- ����������� �������� (������� �����),
                          -- �� ��� �� ������� ��� �������� �� �������� 2016
                          or invoice_dt >= date'2017-03-01' and
                             contract_till_dt <= date'2017-03-01'
                             )
                          -- BALANCE-25535
                          -- BALANCE-25224: ��������� ������ �� ���������.������
                         and not i.type = 'prepayment'
                    then amt_oebs
                    else 0
               end), 0)             as amt_to_pay_2016,
           nvl(sum(case when
                          -- BALANCE-22330: ���� ���� ��������� �� ��
                          -- ����������� �������� (������� �����),
                          -- �� ��� �� ������� ��� �������� �� �������� 2016
                             contract_till_dt <= date'2017-03-01'
                          -- BALANCE-25535, BALANCE-25224: ������ ����������
                         and i.type = 'prepayment'
                    then amt_acts
                    else 0
               end), 0)             as amt_to_charge_2016,
           nvl(sum(case when invoice_dt >= date'2017-03-01'
                          -- BALANCE-22330: ������ ��� ���������� ���������
                         and contract_till_dt > date'2017-03-01'
                          -- BALANCE-25535
                          -- BALANCE-25224: ��������� ������ �� ���������.������
                         and not i.type = 'prepayment'
                    then amt_oebs
                    else 0
               end), 0)             as amt_to_pay_2017,
           nvl(sum(case when
                          -- BALANCE-22330: ������ ��� ���������� ���������
                             contract_till_dt > date'2017-03-01'
                          -- BALANCE-25535, BALANCE-25224: ������ ����������
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
                   -- BALANCE-28169: ��� ��������� ���� ��������� �����
                   -- ������������� (��� is_2018 ��� ����� ���� ����� �����)
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
-- ���������� � �������� (������� ������� ���������������� ��������)
s_kv_control_src as (
    select d.*,
           -- �������, �� ������� �� ������� ����� ����� �� ��������
           -- ������ ��������� �� ����� 1000 ���. ����������� ������ ����������
           -- ClientId ��������� ����� ��������.
           case
            when amt_rub >= 1000 and contract_till_dt <= date'2018-03-01'
            then brand_id
            when amt_rub >= 1000 and contract_till_dt  > date'2018-03-01'
             and discount_type = 7
            then brand_id
            else null
           end                                              as over1k_brand_id,
           -- ����������� ������� �� ������� �� ������� �
           -- ������� �� �������� (���������)
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
            -- BALANCE-22205: ��������� ������� �.���� �� ������� ��� ��������
            --                �� �������, �������, �����������
         where discount_type <> 25
               )
         group by contract_id, from_dt, till_dt, brand_id, client_count_2016,
                  contract_from_dt, contract_till_dt, commission_type, discount_type
           ) d
),
s_kv_control_pre as (
    select d.*,
           -- ���������, ��� � �����-�� ������ ����� �� ���� �������
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
-- ������
--  - ������ �� �������� >= 200�
--  - �������� >= 5
--  - ��� �������� � �������� > 70% (����������� �� ����� ���������) �� �������
--  - ��� ������������
--  - ������ 1 ������ � ������
s_kv_control as (
    select d.*,
           case
           when (
                    -- 2018
                    (contract_till_dt > date'2018-03-01'
                     -- ���� ������� �� ���������, �� ��� ��������
                     -- �������� �� ������
                        and (client_count >= 5 or amt_rub_direct = 0)
                    )
                    -- 2017
                 or (client_count >= 5 and contract_till_dt > date'2017-03-01'
                                       and contract_till_dt <=date'2018-03-01')
                 or (client_count_2016 >= 5 and contract_till_dt <= date'2017-03-01')
                )
            and (
                 -- ���� ������� �� ����, �� ���� ���� �� �����
                    is_there_boc = 0
                 or is_there_boc_1m_ago = 0
                )
           then 0
           else 1
            end as failed_bok,
           case
           when (    (  -- BALANCE-25262: �� ����������� ��������, ������ �������
                        amt_rub >= 100000
                    and contract_till_dt <= date'2017-03-01'
                    and commission_type = 8
                     )
                     -- ������� 2017
                  or (
                        amt_rub >= 200000
                    and contract_till_dt > date'2017-03-01'
                    and contract_till_dt <= date'2018-03-01'
                     )
                     -- ������� 2018
                  or (
                        amt_rub > decode(commission_type, 1, 500000, 200000)
                    and contract_till_dt > date'2018-03-01'
                     )
                )
           then 0
           else 1
            end as failed_amt,
           case
           when (    (  -- BALANCE-25262: �� ����������� ��������, ������ �������
                        amt_rub >= 100000
                    and contract_till_dt <= date'2017-03-01'
                    and commission_type = 8
                     )
                     -- ������� 2017
                  or (
                        amt_rub >= 200000
                    and contract_till_dt > date'2017-03-01'
                    and contract_till_dt <= date'2018-03-01'
                     )
                     -- ������� 2018
                  or (
                        amt_rub > decode(commission_type, 1, 500000, 200000)
                    and contract_till_dt > date'2018-03-01'
                     )
                )
            and (
                    -- 2018
                    (contract_till_dt > date'2018-03-01'
                     -- ���� ������� �� ���������, �� ��� ��������
                     -- �������� �� ������
                        and (client_count >= 5 or amt_rub_direct = 0)
                    )
                    -- 2017
                 or (client_count >= 5 and contract_till_dt > date'2017-03-01'
                                       and contract_till_dt <=date'2018-03-01')
                 or (client_count_2016 >= 5 and contract_till_dt <= date'2017-03-01')
                )
            and (
                 -- ���� ������� �� ����, �� ���� ���� �� �����
                    is_there_boc = 0
                 or is_there_boc_1m_ago = 0
                )
           then 0
           else 1
            end as failed
      from s_kv_control_pre d
),
-- ����� �� ��� �������� ���� <= �����
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
           -- BALANCE-25071: ��� ������������ ��������� �������� ������ �� 36 �
           -- 1 ���� �������� ����� �������
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
                                        -- BALANCE-28798: � ����������� �� ����
                                        -- ������� �� ������ ��������
                                        decode(d.is_2017, 1, f.failed, f.failed_amt),
                                        1, 0,
                                        -- ���� ���������� �� �� ���, ��� �������
                                        case
                                        when d.discount_type in (36) then 0.15
                                        when d.discount_type in (1, 2, 3) then 0.13
                                        when d.discount_type in (7, 37) then
                                            case
                                                 -- BALANCE-28739
                                                when d.discount_type = 7
                                                 and f.failed_bok = 1
                                                then 0
                                                -- ��� ��������� �� ����������
                                                -- ��� ����� �� ��������� ���������
                                                -- ����� � 7/12 %
                                                when payment_type = 3
                                                 and is_agency_credit_line = 1
                                                then decode(d.discount_type,
                                                        7, 0.07,
                                                        37, 0.12)
                                                -- ��������� � 8/13 %
                                                else decode(d.discount_type,
                                                        7, 0.08,
                                                        37, 0.13)
                                            end
                                        when d.discount_type in (12) then 0.08
                                        else 0
                                        end
                                    )
                -- BALANCE-22195: ������ �������, 2016
                else d.amt_to_charge*decode(d.discount_type,
                         -- ����.�� ������� �� �����
                         25, case
                                 when d.amt_to_charge >= 5000000 then 0.2
                                 when d.amt_to_charge >= 4000000 then 0.18
                                 when d.amt_to_charge >= 3000000 then 0.16
                                 when d.amt_to_charge >= 2000000 then 0.14
                                 when d.amt_to_charge >= 1000000 then 0.12
                                 when d.amt_to_charge >=   50000 then 0.10
                                 else 0
                             end,
                         -- ��� ��������� � 8%
                         decode(f.failed,
                             0, 0.08,
                             0))
               end)                     as reward_to_charge,
           sum(d.amt_to_pay)            as turnover_to_pay,
           sum(d.amt_to_pay_w_nds)      as turnover_to_pay_w_nds,
           -- BALANCE-19851
           -- ���������� �� ������� �� ������
           -- BALANCE-22195: ��������� ������ � ����� ������� ���������
           sum((d.amt_to_pay_2016 + decode(f.failed,    -- BALANCE-25801
                                        0, d.amt_to_charge_2016, 0)) *
             decode(d.discount_type,
                    -- ����.�� ������� �����
                    25, case
                            when d.amt_to_pay_2016 >= 5000000 then 0.2
                            when d.amt_to_pay_2016 >= 4000000 then 0.18
                            when d.amt_to_pay_2016 >= 3000000 then 0.16
                            when d.amt_to_pay_2016 >= 2000000 then 0.14
                            when d.amt_to_pay_2016 >= 1000000 then 0.12
                            -- BALANCE-22241: ������������ ������ ��
                            -- ������� ���, ������ ������ ��. ��
                            -- ������� �������� �� �����, ��� �� �����
                            else                              0.10
                        end,
                    -- ��� ��������� � 8%
                    0.08)
                +
                (d.amt_to_pay_2017 +
                    -- BALANCE-25801
                    -- BALANCE-28739
                    case
                      -- BALANCE-28798: ���� ������� ����������, �� ������� ��
                      -- ������ ��������
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
                                            -- ��� ��������� �� ����������
                                            -- ��� ����� �� ��������� ���������
                                            -- ����� � 7/12 %
                                            when payment_type = 3
                                             and is_agency_credit_line = 1
                                            then decode(d.discount_type,
                                                    7, 0.07,
                                                    37, 0.12)
                                            -- ��������� � 8/13 %
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
    -- ��-����������� 1% �� �������
    select d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           3                        as payment_type,
           d.nds, d.currency,
           -- ���� ����� ��� �� ���������, ������� ���� �������� � reward_to_pay_src
           0                        as do_not_fill_src,
           discount_type,
           0                        as turnover_to_charge,
           d.amt_by_invoice*0.01    as reward_to_charge,
           0                        as turnover_to_pay,
           0                        as turnover_to_pay_w_nds,
           d.amt_by_invoice*0.01    as reward_to_pay
      from bo.v_opt_2015_payments       d
        -- �������������� ������� ��� ���������� �������������� �� ���������
        -- ������: � �������� �������, � ������� ��� ��������� ����, ���������
        -- ��������� ������� ������� ������� ������ ��� �������. ���� �������
        -- ������� ������� ������ �� �����������, �������������� �� ���������
        -- ������ ����� �� ����������� � �� �������������
      join s_kv_control                 f on f.contract_id = d.contract_id
                                         and d.invoice_dt between f.from_dt
                                                              and f.till_dt
                                          -- BALANCE-28814: ���� �� 37 � 7 ��
                                          -- ������ ��������� � 2018 ����
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
        -- BALANCE-25161: ������ �� ����� ������
       and d.invoice_dt >= date'2017-03-01'
           )  d
     group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt, d.payment_type,
              d.discount_type, d.currency, d.nds
),
-- �� � ��������� �� ����� �������, ��� ��������, ��� ����� �� �����, ��� �����
s_kv01 as (
    select contract_eid, contract_id, from_dt, till_dt,
           discount_type, currency, nds,
           -- � ������������
           turnover_to_charge,
           reward_to_charge,
           -- � ���������� (��. s_kv10)
           0                as turnover_to_pay,
           0                as turnover_to_pay_w_nds,
           decode(payment_type,
                -- BALANCE-19979: ��� ���������� ������ �� �����
                2, reward_to_charge,
                -- ����������
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
        -- ������ ���������� ������ ��� ����������
     where payment_type = 3
     union all
        -- BALANCE-24627
        -- �������, ����� ������� "�����", ���� ������� ��������
        -- � ������� �������
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
                   -- ��� �� ������� �� �����, ������� ���� ������ reward_to_pay
                 when d.discount_type = 71
                 then d.reward_to_pay
                      -- ���� � ��� ����� ��� ��������������, �� ������ �� �����
                 when nvl(chpt.value_num, 3) = 2
                   -- ������: contract_id = 239691, 2016-10. ������ �����������
                   -- ������ � ����. ������, �� ��� � 2016-10 ���� 310 ������,
                   -- �� ������� ���� ������� �������� ������, � �� ����
                  and d.reward_type = 301
                      -- ����������� � ����������
                 then d.reward_to_pay
                 else d.reward_to_pay_src
                  end                       as  reward_to_pay_src
           -- BALANCE-25224: ������� ���� ����������
           -- BALANCE-26154: ������� ��� �������, � �� ������ � ��-0-��� ��������
           --                ����� �� ������ ������
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
            -- BALANCE-24877: ��������� ������� �� ���.������, ���� ���
            --                �� ������ ������ �� ������.������
           and d.from_dt < add_months(trunc(sysdate, 'MM'), -1)
           ) d
  group by d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           d.nds, d.currency
),
-- �� � ���������, ��� ����� �� �����, ��� �����
-- ��� �������� �� ����� �������
s_kv10 as (
    select contract_eid, contract_id, from_dt, till_dt,
           currency, nds,
           -- � ������������ (��. s_kv01)
           0                                    as turnover_to_charge,
           0                                    as reward_to_charge,
           -- � ����������
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
                    -- ������� ����������� �� ����� �������
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
        -- ���������� ������ ���������� �����
     where from_dt between add_months(trunc(sysdate, 'MM'), -1)
                       and trunc(sysdate, 'MM') - 1/24/60/60
)
-- ��� (���������)
-- BALANCE-19948, BALANCE-19941
-- BALANCE-27476: ������� � ������
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
    -- �.�. ����� � �������� ������� � ������ ������� �����������������,
    -- �� �������� ������� �� ��� � ������ ������.
    -- �������� ������:
    --      ���. ������ �� �������� � ���������� �� �������
    --      ���. ������ �� ��-��    � �������������
    --      ������� ������ �� ��-�� � �������������
    select /*+ parallel(16)
               opt_param('optimizer_index_caching' 0)
               opt_param('optimizer_index_cost_adj' 1000)
           */
           contract_eid, contract_id, from_dt, till_dt, agency_id,
           till_dt_fc, amt_ag_prev, amt_ag_prev_fm, discount_type,
           amt_ag_q                     as amt_ag,
           -- ���. ������ �� �������� � ��������� �� ������
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
-- ������ ����������������� �� ������� ��������,
-- � ������� (������� �� ���� ���������������)
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
            -- ������ ������� ��������� �� ������ ��������
        select d.contract_eid, d.contract_id, d.from_dt, d.till_dt, d.agency_id,
               discount_type,
               -- ������� ������ �� ��-�� � ��� �� �������, ������ �� ������
               amt_ag_prev,
               amt_ag_prev_fm,
               -- ���. ������ �� �������� � ��������� �� ��������
               sum(amt_rub)                 as amt_rub,
               sum(amt_w_nds_rub)           as amt_w_nds_rub,
               amt_ag
          from s_q_by_month d
         group by contract_eid, contract_id, from_dt, till_dt, agency_id,
                  discount_type,
                  amt_ag_prev, amt_ag_prev_fm, amt_ag
           ) d
)
-- �������� �������� ������� ���������, �� ������� ���� �������
-- ���������������� ������������
, s_q_growth_not_consdted as (
    select d.contract_id, d.contract_eid,
           d.main_contract_id,
           d.discount_type,
           d.from_dt,
           sum(d.amt)                       as amt_rub,
           sum(d.amt_w_nds_rub)             as amt_w_nds_rub
      from (
            -- ����������������� ��������
        select d.contract_id_orig                       as contract_id,
               d.contract_eid_orig                      as contract_eid,
               d.contract_id                            as main_contract_id,
               d.from_dt,
               d.discount_type,
               d.act_dt,
               d.amt_rub                                as amt,
               d.amt_w_nds_rub
            -- BALANCE-28455: ������� ��� �������, �.�. ����� ����� ���� �������
          from bo.v_ar_acts_q         d
            -- ����� � ������ ������������
         where d.commission_type in (1, 8, 21)
            -- ��������������� �������
           and d.cons_type = 1
            -- ������ ��, ��� �� ���������
           and d.failed_bok = 0
           and d.failed_media = 0
            ) d
     group by d.contract_id, d.contract_eid, d.main_contract_id, d.discount_type, d.from_dt
)
, s_q_growth_proply_consolidated as (
    select d.contract_eid, d.contract_id,
           r.from_dt, r.till_dt, d.discount_type,
           d.amt_rub, d.amt_w_nds_rub,
           -- ��������������� ������������ ������
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
                -- ��� ������:
                --  - ������������������� ��������
                --  - ����������������� �������� � ������������� �� ������� �������
            select contract_eid, contract_id, from_dt, till_dt,
                   discount_type,
                   amt_rub, amt_w_nds_rub, reward
              from s_q_growth
             where contract_id not in (
                    select contract_id
                      from bo.mv_ar_consolidations_q
                     where cons_type = 1)
             union all
                -- ��������������� �����������������
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
-- �������������� ������
select contract_id,
       contract_eid,
       from_dt,
       till_dt,
       nds,
       currency,
       discount_type,
       reward_type,
       turnover_to_charge,                          -- ������ � ����������
       reward_to_charge,                            -- � ����������
       turnover_to_pay_w_nds,
       turnover_to_pay,                             -- ������ � ������������
       reward_to_pay_src,                           -- � ������������
       reward_to_pay                                -- � ������������
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
--            -- BALANCE-24627: ������� ������� ������ 2 ���� � ���
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
            -- BALANCE-24627: ����������� ������ ������� �� ������
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
