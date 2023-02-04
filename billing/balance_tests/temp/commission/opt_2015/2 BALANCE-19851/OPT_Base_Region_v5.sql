with
-- ������� ����
s_dates as (
        -- ������ ���. ����
    select date'2015-03-01'   as fin_year_dt
      from dual
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
--                                                -- BALANCE-17175
                                                (
--                                                    -- ������ �����, ��������� � 2015
--                                                    invoice_dt >= DATE'2015-03-01' and
                                                    -- ������ �������/�����
                                                    contract_commission_type in (1, 2, 8) and
                                                    -- �� ��������� ��� � ��� 22
                                                    nvl(commission_type,
                                                        discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 19, 22)
                                                )
                                                or
                                                (
                                                    -- �����, ��� �����
                                                    nvl(commission_type,
                                                        discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 19)
                                                )
                                              )
)

--select * from s_base;
,
-- ----------------------------------------------------------------------------
-- �������� ������� �� �����
-- ----------------------------------------------------------------------------
-- ��� ���� �� ������
s_acts_all as (
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
           b.discount_type,
           nvl(xxx.is_loyal, 0)                               as loyal_clients,
           xxx.client_id                                      as client_id,
           xxx.act_dt                                         as act_dt,
           trunc(xxx.act_dt, 'MM')                            as from_dt,
           add_months(trunc(xxx.act_dt, 'MM'), 1) - 1/84600   as till_dt,
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
     where b.commission_type = 8
        -- ��� ��������: ������, �������, ������, ����������
       and b.discount_type in (1, 2, 3, 7, 11, 12)
)

--select * from s_acts_all; 

,
-- ���� ��� �������� ��������
s_acts_wo_lc as (
    select d.*
      from s_acts_all d
     where not (d.loyal_clients = 1 and d.discount_type = 7)
),
-- ����� �� �������� ��������
s_acts_lc as (
    select d.*
      from s_acts_all d
     where (d.loyal_clients = 1 and d.discount_type = 7)
),
-- ----------------------------------------------------------------------------
-- �������� ������� �� �������
-- ----------------------------------------------------------------------------
s_payments as (
    select b.contract_eid,
           b.contract_id,
           b.invoice_eid,
           b.invoice_id,
           b.invoice_dt,
           b.currency,
           b.nds,
           b.payment_type,
           b.discount_type,
           b.loyal_clients,
           oebs.comiss_date,
           trunc(oebs.comiss_date, 'MM')                as from_dt,
           add_months(
           trunc(oebs.comiss_date, 'MM'), 1)-1/84600   as till_dt,
           oebs.oebs_payment*100/(100 + b.nds*b.nds_pct) as amt,
           oebs.oebs_payment                             as amt_w_nds
      from s_base       b
      join xxxx_oebs_cash_payment_test     oebs on oebs.invoice_id = b.invoice_id
                                                and oebs.comiss_date >= (select fin_year_dt from s_dates)
                                                and oebs.comiss_date is not null
     where b.commission_type = 8
        -- ��� ��������: ������, �������, ������, ����������
       and b.discount_type in (1, 2, 3, 7, 11, 12)
)

--select * from s_payments;
,
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
-- ����, ������� ���� ������. �� ����:
-- ��� ���� �� �� �� + ���� �� ��, ������� > 50�
s_kv_acts as (
    select contract_eid, contract_id, currency, nds, discount_type,
           from_dt, till_dt, payment_type,
           amt, amt_w_nds
      from s_acts_wo_lc
     union all
    select contract_eid, contract_id, currency, nds, discount_type,
           from_dt, till_dt, payment_type,
           amt, amt_w_nds
      from s_kv_acts_lc
),
-- ���������� ���� � ������
s_kv_src as (
    select contract_eid, contract_id,
           currency, discount_type,
           nds, from_dt, till_dt, payment_type,
           sum(amt_acts)            as amt_to_charge,
           sum(amt_oebs_w_nds)      as amt_to_pay_w_nds,
           sum(amt_oebs)            as amt_to_pay
      from (
            select contract_eid, contract_id, currency, nds, discount_type,
                   from_dt, till_dt, payment_type,
                   amt  as amt_acts, amt_w_nds  as amt_acts_w_nds,
                   0    as amt_oebs, 0          as amt_oebs_w_nds
              from s_kv_acts
             union all
            select contract_eid, contract_id, currency, nds, discount_type,
                   from_dt, till_dt, payment_type,
                   0    as amt_acts, 0          as amt_acts_w_nds,
                   amt  as amt_oebs, amt_w_nds  as amt_oebs_w_nds
              from s_payments
           )
     group by contract_eid, contract_id, currency, payment_type,
              nds, discount_type, from_dt, till_dt
)
--select * from s_kv_src;
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
               nds_count, currency_count,
               sum(amt_rub)                                  as amt_rub,
               sum(decode(discount_type, 7, amt_rub, null))  as amt_rub_direct
          from (
                select d.*,
                       -- ���������, ��� �� �������� ������ ����
                       -- 1 ������ � 1 ���
                       count(distinct nds)
                        over (partition by contract_id) as nds_count,
                       count(distinct currency)
                        over (partition by contract_id) as currency_count
                  from s_acts_all d
               )
         group by contract_id, from_dt, till_dt, client_id,
                  nds_count, currency_count
           ) d
),
-- ������
--  - ������ �� �������� >= 100�
--  - �������� >= 5
--  - ��� �������� � �������� > 70% (����������� �� ����� ���������) �� �������
--  - ��� ������������
--  - ������ 1 ������ � ������
s_kv_control as (
    select d.*,
           case
           when amt_rub >= 100000
            and client_count >= 5
            and is_there_boc = 0
            and nds_count = 1
            and currency_count = 1
           then 0
           else 1
            end as failed
      from (
        select d.*,
               case when max_client_ratio_by_direct >= 0.7 then 1 else 0
                end as is_there_boc
          from (
            select contract_id, from_dt, till_dt,
                   nds_count, currency_count,
                   sum(amt_rub)             as amt_rub,
                   count(client_id)         as client_count,
                   round(max(ratio), 2)     as max_client_ratio_by_direct
              from s_kv_control_src
             group by contract_id, from_dt, till_dt,
                      nds_count, currency_count
               ) d
           ) d
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
           sum(decode(f.failed,
                0, d.amt_to_charge*0.1,
                0))                     as reward_to_charge,
           sum(d.amt_to_pay)            as turnover_to_pay,
           sum(d.amt_to_pay_w_nds)      as turnover_to_pay_w_nds,
		   -- BALANCE-19851
           -- ���������� �� ������� �� ������
           sum(d.amt_to_pay*0.1)        as reward_to_pay
      from s_kv_src         d
      -- BALANCE-19851
      left outer
      join s_kv_control     f on f.contract_id = d.contract_id
                             and f.from_dt = d.from_dt
                             and f.till_dt = d.till_dt
     group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt, payment_type,
              d.discount_type, d.currency, d.nds
)

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
--select *from  s_kv10;
,
-- �������������� ������
s_opt_2015_base_region as (select contract_id,
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
               3           as reward_type
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
               10           as reward_type
          from s_kv10
       )
)


select distinct s."CONTRACT_ID",s."CONTRACT_EID",s."FROM_DT",s."TILL_DT",s."NDS",s."CURRENCY",s."DISCOUNT_TYPE",s."REWARD_TYPE",s."TURNOVER_TO_CHARGE",s."REWARD_TO_CHARGE",s."DELKREDERE_TO_CHARGE",s."DKV_TO_CHARGE",s."TURNOVER_TO_PAY_W_NDS",s."TURNOVER_TO_PAY",s."REWARD_TO_PAY",s."DELKREDERE_TO_PAY",s."DKV_TO_PAY"
  from (
 
    select contract_id,     contract_eid,
           from_dt,         till_dt,
           nds,             currency,
           discount_type,
           300 + reward_type    as reward_type,
           -- � ����������
           sum(turnover_to_charge) as turnover_to_charge,
           sum(reward_to_charge)  as reward_to_charge ,
           0                    as delkredere_to_charge,
           0                    as dkv_to_charge,
           -- � ������������
           sum(turnover_to_pay_w_nds) as turnover_to_pay_w_nds ,
           sum(turnover_to_pay) as turnover_to_pay,
           sum(reward_to_pay) as reward_to_pay,
           0                    as delkredere_to_pay,
           0                    as dkv_to_pay
      from 
	( select contract_id,     contract_eid,
         from_dt,         till_dt,
         nds,             currency,
         discount_type,
         reward_type,
         turnover_to_charge,
         reward_to_charge,
         turnover_to_pay_w_nds,
         turnover_to_pay,
         reward_to_pay
		from s_opt_2015_base_region)

	 group by contract_id,     contract_eid,
          from_dt,         till_dt,
            nds,             currency,
            discount_type,   reward_type

)            s
--where  reward_to_charge!=0 
--union all
--select distinct s."CONTRACT_ID",s."CONTRACT_EID",s."FROM_DT",s."TILL_DT",s."NDS",s."CURRENCY",s."DISCOUNT_TYPE",s."REWARD_TYPE",s."TURNOVER_TO_CHARGE",s."REWARD_TO_CHARGE",s."DELKREDERE_TO_CHARGE",s."DKV_TO_CHARGE",s."TURNOVER_TO_PAY_W_NDS",s."TURNOVER_TO_PAY",s."REWARD_TO_PAY",s."DELKREDERE_TO_PAY",s."DKV_TO_PAY"
--  from (
-- 
--    select contract_id,     contract_eid,
--           from_dt,         till_dt,
--           nds,             currency,
--           discount_type,
--           300 + reward_type    as reward_type,
--           -- � ����������
--           sum(turnover_to_charge) as turnover_to_charge,
--           sum(reward_to_charge)  as reward_to_charge ,
--           0                    as delkredere_to_charge,
--           0                    as dkv_to_charge,
--           -- � ������������
--           sum(turnover_to_pay_w_nds) as turnover_to_pay_w_nds ,
--           sum(turnover_to_pay) as turnover_to_pay,
--           sum(reward_to_pay) as reward_to_pay,
--           0                    as delkredere_to_pay,
--           0                    as dkv_to_pay
--      from 
--	( select contract_id,     contract_eid,
--         from_dt,         till_dt,
--         nds,             currency,
--         discount_type,
--         reward_type,
--         turnover_to_charge,
--         reward_to_charge,
--         turnover_to_pay_w_nds,
--         turnover_to_pay,
--         reward_to_pay
--		from s_opt_2015_base_region)
--
--	 group by contract_id,     contract_eid,
--          from_dt,         till_dt,
--            nds,             currency,
--            discount_type,   reward_type
--
--)            s
--where  reward_to_pay!=0 
--and reward_to_pay !=0

order by contract_id, from_dt, discount_type, currency, nds;
