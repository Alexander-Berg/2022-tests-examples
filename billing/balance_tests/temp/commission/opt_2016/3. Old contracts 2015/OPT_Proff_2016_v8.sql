with
-- ������� ����
s_dates as (
        -- ������ ���. ����
    select date'2015-03-01'   as fin_year_dt
      from dual
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
                                                    -- ������ �����, ��������� � 2015
--                                                    invoice_dt >= date'2015-03-01' and
                                                    -- ������ �������/�����
                                                    c.contract_commission_type in (1, 2, 8) and
                                                    -- �� ��������� ��� � ��� 22
                                                    nvl(c.commission_type,
                                                        c.discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 19, 22, 25, 28, 29)
                                                )
                                                or
                                                (
                                                    -- �� ������������ ������� ��
                                                    c.contract_commission_type in (10) and 1=1
                                                )
                                               or
                                                (
                                                    -- �����, ��� �����
                                                    nvl(c.commission_type,
                                                        c.discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 19, 28, 29)
                                                )
                                              )
)
--select * from s_base order by  invoice_id ;
,

s_payments_src as (
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
           b.loyal_clients,
           oebs.comiss_date,
           trunc(oebs.comiss_date, 'MM')                 as from_dt,
           add_months(
            trunc(oebs.comiss_date, 'MM'), 1)-1/84600    as till_dt,
           oebs.oebs_payment*100/(100 + b.nds*b.nds_pct) as amt,
           oebs.oebs_payment                             as amt_w_nds
      from s_base        b
      join xxxx_oebs_cash_payment_test     oebs on oebs.invoice_id = b.invoice_id
                                                and oebs.comiss_date >= (select fin_year_dt from s_dates)
--                                                and oebs.comiss_date >= date'2015-03-01'   -- ������ ��� ������������� ������, ����� ������������
                                                and oebs.comiss_date is not null
   where b.commission_type in (1, 2, 3, 4, 5, 6, 8, 10, 11)
   and (
          -- base, prof
          (
            b.commission_type in (1, 2, 8)
        and (
                -- BALANCE-22085
                 b.invoice_dt >= date'2016-03-01'        and 
                 b.discount_type in (1, 2, 3, 7, 12, 25) and
                 -- BALANCE-22203
                 -- BALANCE-22331: �� ����� �������� ������� ������ �����
                 b.currency = 'RUR'
             or b.invoice_dt  < date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
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
       )
),
-- ----------------------------------------------------------------------------
-- �������� ������� �� �����
-- ----------------------------------------------------------------------------
-- ��� ���� �� ������
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
           case
           when nvl(xxx.is_loyal, 0) = 1 and b.discount_type = 7
            then 1
            else 0
           end                               as is_loyal,
           xxx.client_id                                      as client_id,
           xxx.act_dt                                         as act_dt,
           trunc(xxx.act_dt, 'MM')                            as from_dt,
           add_months(trunc(xxx.act_dt, 'MM'), 1) - 1/84600   as till_dt,
            count(distinct b.nds)      over (partition by b.contract_id) as nds_count,
           count(distinct b.currency) over (partition by b.contract_id) as currency_count,
           xxx.amount                                         as amt_w_nds,
           xxx.amount-xxx.amount_nds-xxx.amount_nsp           as amt,
           xxx.amount*cr.rate                                 as amt_w_nds_rub,
           (xxx.amount-xxx.amount_nds-xxx.amount_nsp)*cr.rate as amt_rub
      from s_base        b
      join xxxx_new_comm_contract_basic     xxx on b.invoice_id = xxx.invoice_id
                                              and xxx.hidden <4
                                              and xxx.act_dt >= (select fin_year_dt from s_dates)
      join xxxx_currency_rate              cr on cr.cc = b.currency
                                              and cr.rate_dt = trunc(xxx.act_dt)
     where b.commission_type in (1, 2, 3, 4, 5, 6, 8, 10, 11, 12, 13)
   and (
          -- base, prof
          (
            b.commission_type in (1, 2, 8)
        and (
                -- BALANCE-22085
                -- � ����� ����� �� ����� � ���������� ���������
                --  * ����������� ������ (11)
                --  * �������� ���� (25)
                xxx.act_dt >= date'2016-03-01'                and
                b.contract_till_dt >= date'2016-03-01'  and
                b.discount_type in (1, 2, 3, 7, 12, 25) and
                -- BALANCE-22203
                -- BALANCE-22331: � ����� �������� ��������� ������ �����
                b.currency = 'RUR'
                -- � ����� ����� �� ������������ ��������� � ������ �����
                -- ��������� ������ �������
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
       or (b.commission_type in (12, 13) and b.discount_type_src = 11)
          
)
)
--select * from s_acts order by invoice_id;
,
-- ���� ��� �������� ��������
-- ��� ���� �� ������
s_acts_all as (
    select b.*
      from s_acts   b
     where b.commission_type = 2
)
--select * from s_acts_all ;
,

s_acts_wo_lc as (
    select d.*
      from s_acts_all d
     where d.is_loyal = 0
),
-- ����� �� �������� ��������
s_acts_lc as (
    select d.*
      from s_acts_all d
     where d.is_loyal = 1
),

s_payments as (
    select b.*
      from s_payments_src b
     where b.commission_type = 2
),
-- ----------------------------------------------------------------------------
-- �������� ������� �� �������
-- ----------------------------------------------------------------------------

-- ----------------------------------------------------------------------------
-- ������� �� (��������)
-- ----------------------------------------------------------------------------
-- ���� �� ��. ����������� ��������, � ������� �� ����� < 50�
s_kv_acts_lc as (
    select contract_eid, contract_id,
           currency, discount_type,
           nds, from_dt, till_dt, payment_type,
           client_id,
           sum(amt_w_nds)       as amt_w_nds,
           sum(amt)             as amt
      from s_acts_lc
     group by contract_eid, contract_id,
              currency, discount_type,
              nds, from_dt, till_dt, payment_type,
              client_id
        -- BALANCE-19689
        -- ��� �� ������� ������ �� ������ �� ������� ��������������� ������,
        -- ���� � ������ ������ ����� �� ������� ���� ������� (�� �����)
        -- �� 50 000 �.�. (��� ���) � �����
     having sum(amt_rub) >= 50000
),
s_kv_lc as (
    select contract_eid, contract_id,
           currency, discount_type,
           nds, from_dt, till_dt, payment_type,
           sum(amt_w_nds)       as amt_w_nds,
           sum(amt)             as amt,
           sum(amt)*.1          as reward
      from s_kv_acts_lc
     group by contract_eid, contract_id,
              currency, discount_type,
              nds, from_dt, till_dt, payment_type
),
-- ���������� ���� � ������
s_kv_src as (
    select contract_eid, contract_id,
           currency, discount_type,
           contract_from_dt, contract_till_dt,
           nds, from_dt, till_dt, payment_type,
           sum(amt_acts)            as amt_to_charge,
           sum(amt_oebs_w_nds)      as amt_to_pay_w_nds,
           -- BALANCE-22195: ������� ����� ����� �� ������ ��������
           -- � �� ����� �������� ���������, ����� ��������� � ��� ������
           -- �������
           nvl(sum(case when invoice_dt < date'2016-03-01'
                          -- BALANCE-22330: ���� ���� ��������� �� ��
                          -- ����������� �������� (�� ����� �����),
                          -- �� ��� �� ������� ��� �������� �� �������� 2015
                          or invoice_dt >= date'2016-03-01' and
                             contract_till_dt < date'2016-03-01'
                    then amt_oebs
                    else 0
               end), 0)             as amt_to_pay_2015,
           nvl(sum(case when invoice_dt >= date'2016-03-01'
                          -- BALANCE-22330: ������ ��� ���������� ���������
                         and contract_till_dt >= date'2016-03-01'
                    then amt_oebs
                    else 0
               end), 0)             as amt_to_pay_2016,
           sum(amt_oebs)            as amt_to_pay
      from (
            select contract_eid, contract_id, currency, nds, discount_type,
                   contract_from_dt, contract_till_dt,
                   from_dt, till_dt, payment_type, invoice_dt,
                   amt  as amt_acts, amt_w_nds  as amt_acts_w_nds,
                   0    as amt_oebs, 0          as amt_oebs_w_nds
              from s_acts_wo_lc
             union all
            select contract_eid, contract_id, currency, nds, discount_type,
                   contract_from_dt, contract_till_dt,
                   from_dt, till_dt, payment_type, invoice_dt,
                   0    as amt_acts, 0          as amt_acts_w_nds,
                   amt  as amt_oebs, amt_w_nds  as amt_oebs_w_nds
              from s_payments
           )
     group by contract_eid, contract_id, currency, payment_type,
              contract_from_dt, contract_till_dt,
              nds, discount_type, from_dt, till_dt
)
--select * from s_kv_src order by from_dt;
,
-- ���������� � �������� (������� ������� ���������������� ��������)
s_kv_control_src as (
    select d.*,
           -- ����������� ������� �� ������� �� ������� �
           -- ������� �� �������� (���������)
           nvl(ratio_to_report(amt_rub_direct)
              over (partition by contract_id, from_dt), 0) as ratio
      from (
        select contract_id, from_dt, till_dt, client_id,
               contract_from_dt, contract_till_dt,
               nds_count, currency_count,
               sum(amt_rub)                                  as amt_rub,
               sum(decode(discount_type, 7, amt_rub, null))  as amt_rub_direct
          from s_acts_all d
            -- BALANCE-22205: ��������� ������� �.���� �� ������� ��� ��������
            -- �� �������, �������, �����������
         where discount_type <> 25
         group by contract_id, from_dt, till_dt, client_id,
                  contract_from_dt, contract_till_dt,
                  nds_count, currency_count
           ) d
)
--select * from s_kv_control_src;
,

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
            select contract_id, from_dt, till_dt,
                   contract_from_dt, contract_till_dt,
                   nds_count, currency_count,
                   sum(amt_rub)                 as amt_rub,
                   count(distinct client_id)    as client_count,
                   round(max(ratio), 2)         as max_client_ratio_by_direct
              from s_kv_control_src
             group by contract_id, from_dt, till_dt,
                      contract_from_dt, contract_till_dt,
                      nds_count, currency_count
               ) d
           ) d
)
--select * from s_kv_control_pre;
,

-- ������
--  - ������ �� �������� >= 100�/200k
--  - �������� >= 5
--  - ��� �������� � �������� > 70% (����������� �� ����� ���������) �� �������
--  - ��� ������������
--  - ������ 1 ������ � ������
s_kv_control as (
    select d.*,
           case
           when (
                    (from_dt < date'2015-07-01' and amt_rub >= 100000)
                    or
                    (from_dt >= date'2015-07-01' and amt_rub >= 200000)
                )
            and client_count >= 5
            and (
                    -- �������� 2015 ����
                    (from_dt < date'2016-03-01' and is_there_boc = 0)
                    or
                    -- �������� 2016 ����
                    (
                        from_dt >= date'2016-03-01'
                        -- BALANCE-22211: �� ����������, ���� � ����� ��
                        -- ���� ������ ������ ������� ��� ������� � ���
                    and (is_there_boc = 0 or is_there_boc_1m_ago = 0)
                    )
                )
            and nds_count = 1
            and currency_count = 1
           then 0
           else 1
            end as failed
      from s_kv_control_pre d
)
--select * from s_kv_control;
,
-- ����� �� ��� �������� ���� <= �����
s_kv_pre as (
    select d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           d.payment_type,
           d.nds, d.currency,
           d.discount_type,
           sum(d.amt_to_charge)         as turnover_to_charge,
           sum(case
                -- BALANCE-22195: ������ ����� ������� ��������� ��� �������
                -- "� ������������" �� ���� ����. ����� �������, 2016
                when d.from_dt >= date'2016-03-01' and
                     -- BALANCE-22330: ������ ��� ����� � ����������������
                     d.contract_till_dt >= date'2016-03-01'
                then
                    d.amt_to_charge*decode(d.discount_type,
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
                -- BALANCE-22195: ������ �������, 2015
                else d.amt_to_charge*decode(f.failed,
                        0, 0.1,
                        0)
               end)                     as reward_to_charge,
           sum(d.amt_to_pay)            as turnover_to_pay,
           sum(d.amt_to_pay_w_nds)      as turnover_to_pay_w_nds,
           -- BALANCE-19851
           -- ���������� �� ������� �� ������
           -- BALANCE-22195: ��������� ������ � ����� ������� ���������
           sum(d.amt_to_pay_2016*decode(d.discount_type,
                    -- ����.�� ������� �����
                    25, case
                            when d.amt_to_pay >= 5000000 then 0.2
                            when d.amt_to_pay >= 4000000 then 0.18
                            when d.amt_to_pay >= 3000000 then 0.16
                            when d.amt_to_pay >= 2000000 then 0.14
                            when d.amt_to_pay >= 1000000 then 0.12
                            -- BALANCE-22241: ������������ ������ ��
                            -- ������� ���, ������ ������ ��. ��
                            -- ������� �������� �� �����, ��� �� �����
                            else                              0.10
                        end,
                    -- ��� ��������� � 8%
                    0.08) +
                  d.amt_to_pay_2015*0.1)    as reward_to_pay
      from s_kv_src         d
      -- BALANCE-19851
      left outer
      join s_kv_control     f on f.contract_id = d.contract_id
                             and f.from_dt = d.from_dt
                             and f.till_dt = d.till_dt
     group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt, payment_type,
              d.discount_type, d.currency, d.nds
)
--select  * from s_kv_pre order by from_dt;

,

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
)
--select * from s_kv01;
,
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
                       d.from_dt, d.till_dt, payment_type,
                       d.nds, d.currency,
                       sum(turnover_to_charge)      as turnover_to_charge,
                       sum(reward_to_charge)        as reward_to_charge,
                       sum(turnover_to_pay)         as turnover_to_pay,
                       sum(turnover_to_pay_w_nds)   as turnover_to_pay_w_nds,
                       sum(reward_to_pay)           as reward_to_pay
                  from s_kv_pre         d
                    -- ������ ���������� ������ ��� ����������
                 where payment_type = 3
                 group by d.contract_eid, d.contract_id,
                          d.from_dt, d.till_dt, payment_type,
                          d.currency, d.nds
                   ) d
           ) s
)
--select * from s_kv10;

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
               1            as reward_type
          from s_kv01
         union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               71           as discount_type,
               currency, nds,
               amt          as turnover_to_charge,
               reward       as reward_to_charge,
               null         as turnover_to_pay,
               null         as turnover_to_pay_w_nds,
               reward       as reward_to_pay,
               1            as type
          from s_kv_lc
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
               10           as reward_type
          from s_kv10
       )

  order by contract_id, from_dt, discount_type, currency, nds;
