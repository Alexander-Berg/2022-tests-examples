with
--
-- ������� ����
--
s_dates as (
    select * from bo.v_comm_prof_2014_dates
),
-- ----------------------------------------------------------------------------
-- �������� ������� �� �����
-- ----------------------------------------------------------------------------
s_comm_prof_2014_acts as (
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
           nvl(a.is_loyal, 0)                       as loyal_clients,
           o.client_id                              as client_id,
           a.dt                                     as act_dt,
           trunc(a.dt, 'MM')                        as from_dt,
           add_months(
           trunc(a.dt, 'MM'), 1) - 1/84600         as till_dt,
           at.amount                                as amt_w_nds,
           at.amount-at.amount_nds-at.amount_nsp    as amt
      from bo.mv_comm_2013_base_src_f       b
      join bo.t_act                         a  on a.invoice_id = b.invoice_id
                                              and a.hidden < 4
                                              and a.dt >= (select fin_year_dt from s_dates)
      join bo.t_act_trans                   at on at.act_id = a.id
      join bo.t_consume                     q  on q.id = at.consume_id
      join bo.t_order                       o  on o.id = q.parent_order_id
     where b.commission_type = 48
       and b.contract_till_dt > (select fin_year_dt from s_dates)
        -- BALANCE-19159
        -- ������� � 01.01.2015 ��������� ����, �� ������ �������
        -- �� ������ �������� ����������
       and (
            -- ������� � 2015, ��������� ������� �����
            -- � ��������� ������ �� �����, � ������� ���� �������� ����������
            (
                a.dt >= date'2015-01-01'
            -- BALANCE-19429
            and b.is_opt = 0
            )
            or
            -- �� 2015 ���� ������ �� ������
            (
                a.dt < date'2015-01-01'
            )
           )
),

s_comm_prof_2014_acts_region as (

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
           nvl(a.is_loyal, 0)                       as loyal_clients,
           o.client_id                              as client_id,
           a.dt                                     as act_dt,
           trunc(a.dt, 'MM')                        as from_dt,
           add_months(
            trunc(a.dt, 'MM'), 1) - 1/84600         as till_dt,
           at.amount                                as amt_w_nds,
           at.amount-at.amount_nds-at.amount_nsp    as amt
      from bo.mv_comm_2013_base_src_f       b
      join bo.t_act                         a  on a.invoice_id = b.invoice_id
                                              and a.hidden < 4
                                              and a.dt >= date'2014-01-01'
      join bo.t_act_trans                   at on at.act_id = a.id
      join bo.t_consume                     q  on q.id = at.consume_id
      join bo.t_order                       o  on o.id = q.parent_order_id
     where b.commission_type = 48
       and b.contract_till_dt > (select fin_year_dt from s_dates)
        -- BALANCE-19159
        -- ������� � 01.01.2015 ��������� ����, �� ������ �������
        -- �� ������ �������� ����������
       and (
            -- ������� � 2015, ��������� ������� �����
            -- � ��������� ������ �� �����, � ������� ���� �������� ����������
            (
                a.dt >= date'2015-01-01'
            -- BALANCE-19429
            and b.is_opt = 0
            )
            or
            -- �� 2015 ���� ������ �� ������
            (
                a.dt < date'2015-01-01'
            )
           )
)
,
s_comm_prof_2014_acts_new_wolc as (
 select "CONTRACT_EID","CONTRACT_ID","INVOICE_EID","INVOICE_ID","INVOICE_DT","CONTRACT_FROM_DT","CONTRACT_TILL_DT","CURRENCY","NDS","PAYMENT_TYPE","DISCOUNT_TYPE","LOYAL_CLIENTS","CLIENT_ID","ACT_DT","FROM_DT","TILL_DT","AMT_W_NDS","AMT"
      from bo.v_comm_prof_2014_acts d
        -- �� ���� ����� �� ������ �� 03-2013
     where not exists (select 1 from bo.t_act old_act
                        where old_act.dt < (select new_acts_dt from s_dates)
                          and old_act.invoice_id = d.invoice_id
                          and old_act.hidden < 4
                          and rownum = 1)
        -- �� ���� ����� �� �������� �� 03-2013
       and not exists (select 1 from bo.mv_oebs_receipts_f oebs
                        where oebs.doc_date < (select new_acts_dt from s_dates)
                          and oebs.invoice_eid = d.invoice_eid
                          and rownum = 1)
        -- BALANCE-15196
        -- BALANCE-16330
       and not (d.loyal_clients = 1 and d.discount_type = 7)),


-- ������� ����� �� 2013, ��� ��
s_acts_old_wo_lc as (
    select *
      from bo.v_comm_prof_2014_acts d
        -- ���� ���� �� ������ �� 03-2013
     where (exists (select 1 from bo.t_act old_act
                        where old_act.dt < (select new_acts_dt from s_dates)
                          and old_act.invoice_id = d.invoice_id
                          and old_act.hidden < 4
                          and rownum = 1)
        -- ���� ������ �� �������� �� 03-2013
        or exists (select 1 from bo.mv_oebs_receipts_f oebs
                        where oebs.doc_date < (select new_acts_dt from s_dates)
                          and oebs.invoice_eid = d.invoice_eid
                          and rownum = 1)
           )
        -- BALANCE-15196
        -- BALANCE-16330
       and not (d.loyal_clients = 1 and d.discount_type = 7)
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
           b.commission_payback_pct,
           oebs.comiss_date,
           trunc(oebs.comiss_date, 'MM')                as from_dt,
           add_months(
            trunc(oebs.comiss_date, 'MM'), 1)-1/84600   as till_dt,
           oebs.sum*100/(100 + b.nds*b.nds_pct)         as amt,
           oebs.sum                                     as amt_w_nds
      from bo.mv_comm_2013_base_src_f       b
      -- �������
      join bo.mv_oebs_receipts_f            oebs on oebs.invoice_eid = b.invoice_eid
                                                 -- BALANCE-14988
                                                and oebs.comiss_date >= date'2014-03-01'
                                                 -- BALANCE-15631
                                                and oebs.comiss_date is not null
     where b.commission_type = 48
       and b.contract_till_dt > (select fin_year_dt from s_dates)
        -- BALANCE-19159
        -- ������� � 01.01.2015 ��������� ����, �� ������ �������
        -- �� ������ �������� ����������
       and (
            -- ������� � 2015, ��������� ������� �����
            -- � ��������� ������ �� �����, � ������� ���� �������� ����������
            (
                oebs.comiss_date >= date'2015-01-01'
            -- BALANCE-19429
            and b.is_opt = 0
            -- BALANCE-19449: ���� ������ ������ �� ������ �� 2015 ����,
            -- �� �������� �� ��������� ���������� �� ����.
            and b.is_there_old_acts = 0
            )
            -- BALANCE-19449: ���� ������ ����� 2015 � ����
            -- ���� �� �� ����� ����� �� 2015, �� ����� ������ ���� ������
            or
            (
                oebs.comiss_date >= date'2015-01-01'
            and b.is_there_old_acts != 0
            )
            -- �� 2015 ���� ������ �� ������
            or oebs.comiss_date < date'2015-01-01'
           )
),
s_payments_new as (
    select *
      from s_payments d
        -- �� ���� ����� �� ������ �� 03-2013
     where not exists (select 1 from bo.t_act old_act
                        where old_act.dt < (select new_acts_dt from s_dates)
                          and old_act.invoice_id = d.invoice_id
                          and old_act.hidden < 4
                          and rownum = 1)
        -- �� ���� ����� �� �������� �� 03-2013
       and not exists (select 1 from bo.mv_oebs_receipts_f oebs
                        where oebs.doc_date < (select new_acts_dt from s_dates)
                          and oebs.invoice_eid = d.invoice_eid
                          and rownum = 1)
),
s_payments_old_wo_lc as (
    select *
      from s_payments d
        -- ���� ���� �� ������ �� 03-2013
     where ( exists (select 1 from bo.t_act old_act
                        where old_act.dt < (select new_acts_dt from s_dates)
                          and old_act.invoice_id = d.invoice_id
                          and old_act.hidden < 4
                          and rownum = 1)
        -- ���� ������ �� �������� �� 03-2013
        or exists (select 1 from bo.mv_oebs_receipts_f oebs
                        where oebs.doc_date < (select new_acts_dt from s_dates)
                          and oebs.invoice_eid = d.invoice_eid
                          and rownum = 1)
           )
        -- BALANCE-15196
       and not (d.loyal_clients = 1 and d.discount_type = 7)
),
-- ----------------------------------------------------------------------------
-- �� �������, ������ � ����������
-- ----------------------------------------------------------------------------
s_kv_contract_ratio as (
    select d.contract_id, d.from_dt, d.till_dt,
           round(max(ratio)*100) as ratio
      from (
        select d.*,
               nvl(ratio_to_report(amt_rub)
                  over (partition by d.contract_id, d.from_dt), 0) as ratio
          from (
            select d.contract_id,
                   d.client_id,
                   d.from_dt,
                   d.till_dt,
                   sum(decode(d.discount_type, 7, d.amt*cr.rate, null))  as amt_rub
              from bo.v_comm_prof_2014_acts         d
              join biee.mv_currency_rate            cr on cr.cc = d.currency
                                                      and cr.rate_dt = trunc(d.act_dt)
             group by d.from_dt, d.till_dt,
                      d.contract_id,
                      d.client_id
               ) d
           ) d
     group by d.contract_id, d.from_dt, d.till_dt
),
-- ��������� ������� ������� � ������� �� ��������
-- �� ��������� 3 ������ ����� �������
-- BALANCE-16952
s_kv_contract_ratio_last_3m as (
    select d.contract_id,
           d.from_dt,
           d.ratio,
           d.till_dt,
           -- ���������, ��� � �����-�� ������ ����� �� ���� �������
           case
            when add_months(from_dt, -1) = from_dt_1m_ago then ratio_1m_ago
            when add_months(from_dt, -1) = from_dt_2m_ago then ratio_2m_ago
            else 0
           end                                          as ratio_1m_ago,
           case
            when add_months(from_dt, -2) = from_dt_1m_ago then ratio_1m_ago
            when add_months(from_dt, -2) = from_dt_2m_ago then ratio_2m_ago
            else 0
           end                                          as ratio_2m_ago
      from (
        select d.contract_id,
               d.from_dt,
               d.till_dt,
               d.ratio,
               lag(d.from_dt, 1) over (partition by contract_id
                                            order by from_dt)   as from_dt_1m_ago,
               lag(d.from_dt, 2) over (partition by contract_id
                                            order by from_dt)   as from_dt_2m_ago,
               lag(d.ratio, 1, 0) over (partition by contract_id
                                            order by from_dt)   as ratio_1m_ago,
               lag(d.ratio, 2, 0) over (partition by contract_id
                                            order by from_dt)   as ratio_2m_ago
          from s_kv_contract_ratio d
           ) d
)
,-- ������ �� ��������, �� ������� 3 ������ ������
-- ���� ������� � �������� ����� 70%
-- BALANCE-16952
s_comm_prof_2014_kv_over_70 as (
    select d."CONTRACT_ID",d."FROM_DT",d."RATIO",d."TILL_DT",d."RATIO_1M_AGO",d."RATIO_2M_AGO", 1 as failed
      from s_kv_contract_ratio_last_3m d
     where ratio >= 70
       and ratio_1m_ago >= 70
       and ratio_2m_ago >= 70
       and from_dt >= date'2014-05-01'
)
,

s_kv as (
    select d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           nds, currency,
           discount_type,
           sum(amt)                 as amt_to_charge,
           null                     as amt_to_pay,
           null                     as amt_to_pay_w_nds,
           sum(amt*decode(f.failed,
                1, 0.0001, 0.08))   as reward_to_charge,
           sum(amt*0.02)            as delkredere_to_charge,
           sum(decode(payment_type,
                -- ����������
                2, amt*decode(f.failed, 1, 0.0001, 0.08),
                -- ����������
                3, null))           as reward_to_pay,
           sum(decode(payment_type,
                -- ����������
                2, amt*0.02,
                -- ����������
                3, null))           as delkredere_to_pay
      from bo.v_comm_prof_2014_acts_newwolc        d
        -- BALANCE-16952
      left outer
      join bo.v_comm_prof_2014_kv_over_70 f on f.contract_id = d.contract_id
                                           and f.from_dt = d.from_dt
                                           and f.till_dt = d.till_dt
     where d.discount_type in (1, 2, 3, 11, 12, 14)
     group by contract_eid, d.contract_id, d.from_dt, d.till_dt,
              discount_type, currency, nds
),
-- ----------------------------------------------------------------------------
-- ��, ������
-- ----------------------------------------------------------------------------
s_kv_old as (
    select contract_id, contract_eid, from_dt, till_dt,
           discount_type, currency, nds,
           sum(amt_to_charge)                               as amt_to_charge,
           sum(amt_to_pay)                                  as amt_to_pay,
           sum(amt_to_pay_w_nds)                            as amt_to_pay_w_nds,
           sum(case
                  when discount_type in (
                          7, 11, 12, 14, 1, 2, 3) then amt_to_charge*0.1
                                                  else 0
              end)                                          as reward_to_charge,
           sum(case
                  when discount_type in (
                          7, 11, 12, 14, 1, 2, 3) then amt_to_pay*0.1
                                                  else 0
              end)                                          as reward_to_pay,
           sum(case
                  when discount_type in (
                          7, 11, 12, 14, 1, 2, 3) then amt_to_charge*0.03
                                                  else 0
              end)                                          as delkredere_to_charge,
           sum(case
                  when discount_type in (
                          7, 11, 12, 14, 1, 2, 3) then amt_to_pay*0.03
                                                  else 0
              end)                                          as delkredere_to_pay,
           sum(case
                  when discount_type in (12)      then amt_to_charge*0.12
                  when discount_type in (1, 2, 3) then amt_to_charge*0.03
                                                  else 0
              end)                                          as dkv_to_charge,
           sum(case
                  when discount_type in (12)      then amt_to_pay*0.12
                  when discount_type in (1, 2, 3) then amt_to_pay*0.03
                                                  else 0
              end)                                          as dkv_to_pay
        from (
                  select contract_id, contract_eid,
                         from_dt, till_dt, discount_type, currency, nds,
                         amt                                as amt_to_charge,
                         0                                  as amt_to_pay,
                         0                                  as amt_to_pay_w_nds
                    from s_acts_old_wo_lc
                   union all
                  select contract_id, contract_eid,
                         from_dt, till_dt, discount_type, currency, nds,
                         0                                  as amt_to_charge,
                         decode(payment_type,
                          -- ����������
                          2, 0,
                          -- ����������
                          3, amt) as amt_to_pay,
                         decode(payment_type,
                          -- ����������
                          2, 0,
                          -- ����������
                          3, amt_w_nds)                     as amt_to_pay_w_nds
                    from s_payments_old_wo_lc
             ) d
     group by contract_eid, contract_id, from_dt, till_dt,
              discount_type, currency, nds
),
-- ----------------------------------------------------------------------------
-- ������������ ���������
-- ----------------------------------------------------------------------------
s_region_src as (
    select b.contract_eid, b.contract_id,
           trunc(b.from_dt, 'Q')                    as from_dt,
           add_months(
            trunc(b.till_dt, 'Q'), 3) - 1/84600     as till_dt,
           'RUR'                                    as currency,
           1                                        as nds,
           count(distinct b.client_id)              as client_cnt
      from (
        select b.*,
                -- ������ �� ������� �� �������
               sum(amt_direct) over(partition by
                    b.contract_id, trunc(b.from_dt, 'Q'),
                    client_id)                      as client_amt_in_Q,
               -- �������, ������� ��� ������ ������ ���������� � �������
               -- �������� ��������. ������ ���� � ������ ������ ��������,
               -- �� ����, ����� 3 ����
               count(1) over(partition by
                    b.contract_id, trunc(b.from_dt, 'Q'),
                    client_id)                      as client_cnt_in_Q
          from (
            select b.contract_eid, b.contract_id,
                   b.from_dt, b.till_dt,
                   b.client_id,
                   sum(decode(b.discount_type, 7, b.amt, null)) as amt_direct
              from bo.v_comm_prof_2014_acts_region   b
                -- ������������ ��������� 2013
              join bo.mv_contract_signed_attr   a on a.code = 'SUPERCOMMISSION_BONUS'
                                                 and a.key_num = 140
                                                 and a.contract_id = b.contract_id
             where b.act_dt between b.contract_from_dt
                                -- ���� ��������� ��������, ������, � ���� 01.04.2014,
                                -- �� ����, �������� ������ ������ ���� 31.03.2014 23:59:59
                                and b.contract_till_dt - 1/84600
             group by b.contract_eid, b.contract_id,
                      b.from_dt, b.till_dt,
                      b.client_id
            -- ����������� ������ ������� ���������, � ������� � �������
            -- ������� ������������ ������ ���������� ������� �����
            -- ��������� ����� �� ������ ���� �� ����� 5000 ���. ��� ���
                -- BALANCE-16017: � 2013-10 15� ������� ������ �� �������.
                -- � ��������� ��������� ���� ������� ����������
            having sum(decode(b.discount_type, 7, b.amt, null)) >= 1
               ) b
           ) b
        -- � ������� �������� ������ ���� ����� 3 ������ �� ������� �������
     where client_cnt_in_Q = 3
        -- BALANCE-16017, BALANCE-16754: ��������� 15� �� �������
       and client_amt_in_Q >= 15000
     group by b.contract_eid, b.contract_id,
              trunc(b.from_dt, 'Q'),
              add_months(trunc(b.till_dt, 'Q'), 3)
),
s_region as (
    select contract_eid, contract_id,
           from_dt, till_dt,
           currency, nds,
           case
            when client_cnt < 15                        then 0
            when client_cnt >= 15 and client_cnt <= 25  then 50000
            when client_cnt >= 26 and client_cnt <= 40  then 70000
            when client_cnt >= 41 and client_cnt <= 75  then 85000
            when client_cnt >= 76                       then 130000
           end                    as reward
      from s_region_src
),
-- ----------------------------------------------------------------------------
-- ��
-- ----------------------------------------------------------------------------
s_lc_acts_src as (
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
           nvl(a.is_loyal, 0)                       as loyal_clients,
           o.client_id                              as client_id,
           a.dt                                     as act_dt,
           trunc(a.dt, 'MM')                        as from_dt,
           add_months(
            trunc(a.dt, 'MM'), 1) - 1/84600         as till_dt,
           at.amount                                as amt_w_nds,
           at.amount-at.amount_nds-at.amount_nsp    as amt
      from bo.mv_comm_2013_base_src_f       b
      join bo.t_act                         a  on a.invoice_id = b.invoice_id
                                              and a.hidden < 4
                                              and a.dt >= (select
                                                add_months(fin_year_dt, -1) from s_dates)
      join bo.t_act_trans                   at on at.act_id = a.id
      join bo.t_consume                     q  on q.id = at.consume_id
      join bo.t_order                       o  on o.id = q.parent_order_id
     where b.commission_type = 48
       and b.contract_till_dt > (select fin_year_dt from s_dates)
        -- BALANCE-19159
        -- ������� � 01.01.2015 ��������� ����, �� ������ �������
        -- �� ������ �������� ����������
       and (
            -- ������� � 2015, ��������� ������� �����
            -- � ��������� ������ �� �����, � ������� ���� �������� ����������
            (
                a.dt >= date'2015-01-01'
            -- BALANCE-19429
            and b.is_opt = 0
            )
            or
            -- �� 2015 ���� ������ �� ������
            (
                a.dt < date'2015-01-01'
            )
           )
),
-- �������� ����
-- BALANCE-16000
s_lc_acts as (
    select d.*,
           lc.collateral_dt             as lc_start_dt,
           d.amt*cr.rate                as amt_rub,
           lc.turnover                  as lc_monthly_amt
      from s_lc_acts_src                    d
      join bo.mv_loyal_clients_contr_attr   lc on lc.contract_id = d.contract_id
                                              and lc.client_id = d.client_id
                                              -- ������ ��������� �� ��� ���, ���� ��
                                              -- ������� ���� �������� (� ��)
                                              and d.act_dt between lc.collateral_dt
                                                               and lc.todate
      join biee.mv_currency_rate            cr on cr.cc = d.currency
                                              and cr.rate_dt = trunc(d.act_dt)
     where d.loyal_clients = 1
       and d.discount_type in (7)
),
s_lc_by_clients as (
    select d.contract_id, d.contract_eid,
           d.payment_type,
           d.from_dt, d.till_dt,
           d.nds, d.currency,
           d.client_id,
           d.lc_monthly_amt,
           d.lc_start_dt,
           d.discount,
           sum(amt_to_charge)       as amt_to_charge,
           sum(amt_to_charge_rub)   as amt_to_charge_rub
      from (
            -- ������ ����� ��, ��� � � s_kv_direct
            -- ������� ���� � ���, ��� ������������
            -- s_lc_acts ������ bo.v_comm_prof_2014_acts_newwolc
            select contract_id, contract_eid,
                   -- BALANCE-16000:
                   -- ��� �������������� ������� ����, ��� ������
                   -- ��������� ������� �� ����� ����, ����� ���
                   -- ���������� ����� ������ ��� ��������������
                   payment_type,
                   from_dt, till_dt,
                   nds, currency,
                   d.client_id,
                   d.lc_monthly_amt,
                   d.lc_start_dt,
                   case
                    when dd.discount <12 or dd.discount is null then 0.08
                    when dd.discount >= 12 and dd.discount < 18 then 0.06
                    when dd.discount >= 18                      then 0.05
                   end                              as discount,
                   amt                              as amt_to_charge,
                   amt_rub                          as amt_to_charge_rub
              from s_lc_acts                        d
              left outer
              join bo.mv_client_direct_discount_m   dd on dd.client_id = d.client_id
                                                      and dd.dt = d.from_dt
           ) d
     group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt,
              d.currency, d.nds, d.client_id, d.discount, d.payment_type,
              d.lc_monthly_amt, d.lc_start_dt
)
    select d."CONTRACT_ID",d."CONTRACT_EID",d."PAYMENT_TYPE",d."FROM_DT",d."TILL_DT",d."NDS",d."CURRENCY",d."CLIENT_ID",d."LC_MONTHLY_AMT",d."LC_START_DT",d."DISCOUNT",d."AMT_TO_CHARGE",d."AMT_TO_CHARGE_RUB",d."FROM_DT_PREV",d."AMT_TO_CHARGE_RUB_PREV",
           -- BALANCE-15170:
           -- � ����������� ������ ����� 0
           -- � �� ����� �����������+��
           -- BALANCE-15113: �������� �������� �������
           -- BALANCE-15306: �������� ������� �������� ��-��������,
           --                � �� �� �������� ������
           case
                 -- ���� �� ���� ������ �� ����.������, ��
                 -- �������� � ����. ���������:
            when amt_to_charge_rub_prev is not null
             and (
                    -- ���� ������ �� ���. ����� � ����������
                    -- ������ ���������
                     (
                      amt_to_charge_rub_prev < lc_monthly_amt*.8 and
                      amt_to_charge_rub      < lc_monthly_amt*.8)
                    -- ���� ������� ����� �������� �������� � �����������
                    -- ������ 1.5� � ���. � ����. �������
                  or (
                      (lc_monthly_amt - amt_to_charge_rub_prev ) > 1500000 and
                      (lc_monthly_amt - amt_to_charge_rub      ) > 1500000
                     )
                    -- ���� ���� ������, �� �� �� �� ���������� ������
                    -- � ��� ����, �� ���.������ �������� �� �����
                    -- BALANCE-15339
                  or (
                        from_dt != add_months(from_dt_prev, 1) and
                        (
                            amt_to_charge_rub  < lc_monthly_amt*.8
                         or (lc_monthly_amt - amt_to_charge_rub) > 1500000
                        )
                     )
                 )
             then amt_to_charge*0.0201
               -- ���� ��� ������� �� ���������� ������ �
               -- ������� ����� � ������ ����� ��������� �
               -- ������ ���.������ ����� 80% �����, �� ��������
               -- BALANCE-16340
             when amt_to_charge_rub_prev is null
              and from_dt = add_months(trunc(lc_start_dt, 'MM'), 1)
              and amt_to_charge_rub  < lc_monthly_amt*.8
             then amt_to_charge*0.0201
               -- �� ���� ��������� ������� ������ ����������
             else amt_to_charge*(discount + 0.02)
           end                                  as reward_to_charge,
           -- �����������
           case
                 -- ���� ����� �� ����.������ � null
                 -- ��, ������, ��� ������� ������ � ������� � ������
                 -- � ���� ������ ������ �� % �� ��������
            when amt_to_charge_rub_prev is not null
                 -- ���� �� ���� ������ �� ����.������, ��
                 -- �������� � ����. ���������:
             and (
                    -- ���� ������ �� ���. ����� � ����������
                    -- ������ ���������
                     (
                      amt_to_charge_rub_prev < lc_monthly_amt*.8 and
                      amt_to_charge_rub      < lc_monthly_amt*.8
                     )
                 )
            then '������ �� ����. ('|| amt_to_charge_rub_prev ||') � ���. ('||
                    amt_to_charge_rub||') ������ ����� 80% �� ����� (����: '||
                    lc_monthly_amt||', 80% �� ����: '||lc_monthly_amt*.8||')'
            when amt_to_charge_rub_prev is not null
                 -- ���� �� ���� ������ �� ����.������, ��
                 -- �������� � ����. ���������:
             and (
                    -- ���� ������� ����� �������� �������� � �����������
                    -- ������ 1.5� � ���. � ����. �������
                     (
                      (lc_monthly_amt - amt_to_charge_rub_prev ) > 1500000 and
                      (lc_monthly_amt - amt_to_charge_rub      ) > 1500000
                     )
                 )
            then '������� ����� ������ � � ������ ����� 1.5� ���. � ������� ('||
                 (lc_monthly_amt - amt_to_charge_rub_prev )||') � ������� ('||
                 (lc_monthly_amt - amt_to_charge_rub)||') ������'
            when amt_to_charge_rub_prev is not null
                 -- ���� �� ���� ������ �� ����.������, ��
                 -- �������� � ����. ���������:
             and (
                    -- ���� ���� ������, �� �� �� �� ���������� ������
                    -- � ��� ����, �� ���.������ �������� �� �����
                    -- BALANCE-15339
                     (
                        from_dt != add_months(from_dt_prev, 1) and
                        (
                            amt_to_charge_rub  < lc_monthly_amt*.8
                         or (lc_monthly_amt - amt_to_charge_rub) > 1500000
                        )
                     )
                 )
            then '��� ������� �� ����. ����� � �� �������� ���� � �������:'||
                    from_dt||',������='||amt_to_charge_rub||',80% �� �����='||
                    lc_monthly_amt*.8||'. ���� ������ �� �����:'||from_dt_prev
               -- ���� ��� ������� �� ���������� ������ �
               -- ������� ����� � ������ ����� ��������� �
               -- ������ ���.������ ����� 80% �����, �� ��������
               -- BALANCE-16340
             when amt_to_charge_rub_prev is null
              and from_dt = add_months(trunc(lc_start_dt), 1)
              and amt_to_charge_rub  < lc_monthly_amt*.8
             then '��� ������� �� ������ ����� ��������� (������ ���������'||
                    to_char(lc_start_dt, 'DD.MM.YYYY')||'), '||
                  '�� ������ � ������ ('||amt_to_charge_rub||') '||
                  '< 80% �� ����� ('||lc_monthly_amt*.8||')'
            else null
           end                                  as failed_desc
      from (
            select d.*,
                   lag(d.from_dt)
                    over(partition by contract_id, client_id
                             order by from_dt)          as from_dt_prev,
                   -- ������ � ������ �� ���������� ����� �� �������
                   lag(d.amt_to_charge_rub)
                    over(partition by contract_id, client_id
                             order by from_dt)          as amt_to_charge_rub_prev
              from (
                    select d.contract_id, d.contract_eid, d.payment_type,
                           d.from_dt, d.till_dt,
                           d.nds, d.currency,
                           d.client_id,
                           d.lc_monthly_amt,
                           d.lc_start_dt,
                           d.discount,
                           d.amt_to_charge,
                           d.amt_to_charge_rub
                      from s_lc_by_clients  d
                   ) d
           ) d
),

---


s_lc_src as (
    select contract_id, contract_eid, payment_type,
           from_dt, till_dt,
           nds, currency,
           sum(amt_to_charge)           as amt_to_charge,
           sum(reward_to_charge)        as reward_to_charge
      from bo.v_comm_prof_2014_lc_src d
     group by contract_eid, contract_id, from_dt, till_dt, payment_type,
              currency, nds
),
-- BALANCE-16389:
--  ���������, ����� ��������� �� �����, ��� ���������
s_lc as (
    select contract_id, contract_eid,
           from_dt, till_dt,
           currency, nds,
           amt_to_charge,
           reward_to_charge,
           -- ���������� ���� ��� ������ ��� ��������������
           decode(payment_type, 2, reward_to_charge, null) as reward_to_pay
      from s_lc_src d
),
-- ----------------------------------------------------------------------------
-- ���
-- ----------------------------------------------------------------------------
-- ���������
s_half_years as (
    select d.dt from_dt, add_months(d.dt, 6)-1/24/60/60 as till_dt
      from (
         select add_months(date'2013-03-01', 6*(level-1)) as dt
           from dual
        connect by level <= 10
           ) d
s_acts_2014_lc as (
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
           lc.turnover,
           lc.collateral_dt                         as lc_start_dt,
           lc.todate                                as lc_end_dt,
           nvl(a.is_loyal, 0)                       as loyal_clients,
           o.client_id                              as client_id,
           a.dt                                     as act_dt,
           h.from_dt,
           trunc(h.till_dt)                         as till_dt,
           at.amount                                as amt_w_nds,
           at.amount-at.amount_nds-at.amount_nsp    as amt
      from bo.mv_comm_2013_base_src_f       b
      join bo.t_act                         a  on a.invoice_id = b.invoice_id
                                              and a.hidden < 4
      join bo.t_act_trans                   at on at.act_id = a.id
      join bo.t_consume                     q  on q.id = at.consume_id
      join bo.t_order                       o  on o.id = q.parent_order_id
      join bo.mv_loyal_clients_contr_attr lc on lc.contract_id = b.contract_id
                                            and lc.client_id = o.client_id
                                            and lc.collateral_dt >= date'2013-03-01'
                                             -- ������ ������������� ��
                                             -- ������������ ���������
                                            and case
                                                    when sysdate >= date'2016-03-01' then date'2016-03-01'-1/24/60/60
                                                    when sysdate >= date'2015-09-01' then date'2015-09-01'-1/24/60/60
                                                    when sysdate >= date'2015-03-01' then date'2015-03-01'-1/24/60/60
                                                    when sysdate >= date'2014-09-01' then date'2014-09-01'-1/24/60/60
                                                    when sysdate >= date'2014-03-01' then date'2014-03-01'-1/24/60/60
                                                    else trunc(sysdate, 'MM')
                                                end > lc.todate
      join s_half_years                   h  on lc.todate between h.from_dt
                                                              and h.till_dt
     where nvl(a.is_loyal, 0) = 1
       and b.discount_type = 7
       and a.dt between lc.collateral_dt and lc.todate
       and b.commission_type = 48
        -- ������ �������� �������� ���.����
       and b.contract_till_dt > (select fin_year_dt from s_dates)
        -- BALANCE-19159
        -- ������� � 01.01.2015 ��������� ����, �� ������ �������
        -- �� ������ �������� ����������
       and (
            -- ������� � 2015, ��������� ������� �����
            -- � ��������� ������ �� �����, � ������� ���� �������� ����������
            (
                a.dt >= date'2015-01-01'
            -- BALANCE-19429
            and b.is_opt = 0
            )
            or
            -- �� 2015 ���� ������ �� ������
            (
                a.dt < date'2015-01-01'
            )
           )
),
-- ������ ��� ��
s_skv_not_lc as (
    select d.contract_eid, d.contract_id,
           d.currency, d.nds, d.discount_type,
           d.client_id, d.loyal_clients,
           h.from_dt                                    as from_dt,
           trunc(h.till_dt)                             as till_dt,
           sum(case
              when 0 = (
                  select count(1) from bo.t_act old_act
                   where old_act.dt < (select new_acts_dt from s_dates)
                     and old_act.invoice_id = d.invoice_id
                     and old_act.hidden < 4
                     and rownum = 1)
               and 0 = (
                      -- BALANCE-15636
                  select count(1) from bo.mv_oebs_receipts_f oebs
                   where oebs.doc_date < (select new_acts_dt from s_dates)
                     and oebs.invoice_eid = d.invoice_eid
                     and rownum = 1)
              then amt*cr.rate
              else 0
               end)                                       as amt_new_rub,
           sum(d.amt_w_nds*cr.rate)                       as amt_w_nds_rub,
           sum(d.amt*cr.rate)                             as amt_rub
           -- �������� ��� ����, � �� ������� ������������
           -- �� �������� ����� ������ � ������ ������
      from bo.v_comm_prof_2014_acts       d
      join biee.mv_currency_rate          cr on cr.cc = d.currency
                                            and cr.rate_dt = trunc(d.act_dt)
      join s_half_years                   h  on d.act_dt between h.from_dt
                                                             and h.till_dt
        -- ��� ��
       and not (loyal_clients = 1 and discount_type = 7)
        -- ������, ������� (���), ����������
     where discount_type in (7, 1, 2, 3, 12)
     group by h.from_dt, trunc(h.till_dt),
              d.contract_eid, d.contract_id,
              d.currency, d.nds, d.discount_type,
              d.client_id, d.loyal_clients
),
-- ��, ������� ��������� ������
s_skv_lc as (
    select d.*
      from (
        select d.contract_eid, d.contract_id,
               d.currency, d.nds, d.discount_type,
               d.client_id, d.loyal_clients,
               d.turnover,
               d.from_dt,
               d.till_dt,
               -- �.�. ������� ������ �� ������� � ������ ��������
               -- ��, ������� ������ ���� ��������� ����� 01-03-13,
               -- ��� ������� ����� ������
               sum(d.amt*cr.rate)                             as amt_new_rub,
               sum(d.amt_w_nds*cr.rate)                       as amt_w_nds_rub,
               sum(d.amt*cr.rate)                             as amt_rub
               -- �������� ��� ����, � �� ������� ������������
               -- �� �������� ����� ������ � ������ ������
          from s_acts_2014_lc                 d
          join biee.mv_currency_rate          cr on cr.cc = d.currency
                                                and cr.rate_dt = trunc(d.act_dt)
         group by d.from_dt, d.till_dt,
                  d.contract_eid, d.contract_id,
                  d.currency, d.nds, d.discount_type,
                  d.client_id, d.loyal_clients,
                  d.turnover
           ) d
        -- ������ ���� ���� ������, ���� ����� 120% �� �����*6 �������
        -- 6 ������� � ��� ������� ���� �� ��� ��
     where d.amt_rub >= turnover*6*1.2
),


-- �������� �� ������, �� ������� ���
-- �������� � �������� �� ������� > 70%
-- BALANCE-15995
s_comm_prof_2014_skv_less_70 as (
    select d.contract_id, d.from_dt, max(ratio) as max_ratio
      from (
        select d.*,
               -- BALANCE-15635
               nvl(ratio_to_report(amt_rub)
                  over (partition by d.contract_id, d.from_dt), 0) as ratio
          from (
            select d.contract_id,
                   d.client_id,
                   h.from_dt,
                   -- BALANCE-15635
                   sum(decode(d.discount_type, 7, d.amt*cr.rate, null))  as amt_rub
              from bo.v_comm_prof_2014_acts         d
              join biee.mv_currency_rate            cr on cr.cc = d.currency
                                                      and cr.rate_dt = trunc(d.act_dt)
              join s_half_years                   h  on d.act_dt between h.from_dt
                                                                     and h.till_dt
             group by h.from_dt,
                      d.contract_id,
                      d.client_id
               ) d
           ) d
     group by d.contract_id, d.from_dt
           -- BALANCE-15982
    having round(max(ratio), 4) <= 0.7
),



s_comm_prof_2014_skv_src as (
        select d."CONTRACT_EID",d."CONTRACT_ID",d."DISCOUNT_TYPE",d."CLIENT_ID",d."FROM_DT",d."TILL_DT",d."TURNOVER",d."AMT_NEW_RUB",d."AMT_W_NDS_RUB",d."AMT_RUB"
          from (
                -- ����� ��� ������� �������� ������� �� ������,
                -- �� ������� �� 1 ����� 2013 ���� �� ���� �� �����, �� ����� ("�����"),
                -- ������� ����� �� ������ �������. �� �� ��������� �� �������, ��. �����.
                select d.contract_eid, d.contract_id,
                       d.discount_type,
                       d.client_id,
                       d.from_dt, d.till_dt,
                       null     as turnover,
                       d.amt_new_rub,
                       d.amt_w_nds_rub,
                       d.amt_rub
                  from s_skv_not_lc d
                 union all
                -- ���� �� ������ ������� ��� ���� ������������� �� �� �������� ��������,
                -- ���� ������ ������� ������ ���� ����� 1 ����� 2013 ����, ��:
                -- - �� ������� ������� ���������� % ������� � ����� � ��, �����������
                --   �� 6 (���-�� ������� � �������),
                -- - ���� ���� ������ ���� ����� 120% �� �����, �� ��������� ��� �������
                --   � �������� ��� ����������� ����� �� ����� � ��� ������� ����.
                select d.contract_eid, d.contract_id,
                       d.discount_type,
                       d.client_id,
                       d.from_dt, d.till_dt,
                       d.turnover,
                       d.amt_new_rub,
                       d.amt_w_nds_rub,
                       d.amt_rub
                  from s_skv_lc d
               ) d
               -- BALANCE-15674
               -- BALANCE-15995
               -- ���� ���� ������ ������� � ������� �� ������ ���� ����� 70%
               -- �� ������ �������, ��� �� ������
          join bo.v_comm_prof_2014_skv_less_70  l on l.contract_id = d.contract_id
                                                 and l.from_dt = d.from_dt
),

s_skv as (
    select d.*,
           case
            when (to_char(from_dt, 'MM-DD') = '03-01' and amt_rub > 19000000)
              or (to_char(from_dt, 'MM-DD') = '09-01' and amt_rub > 23000000)
            then amt_new*0.03
            else 0
           end                          as reward_regress,
           case
            when to_char(from_dt, 'MM-DD') = '03-01' then
               case
                when amt_rub >=520000000 then 26650000 + (amt_rub - 520000000)*0.07
                when amt_rub >=250000000 then 10450000 + (amt_rub - 250000000)*0.06
                when amt_rub >= 92000000 then  2550000 + (amt_rub -  92000000)*0.05
                when amt_rub >= 44000000 then   630000 + (amt_rub -  44000000)*0.04
                when amt_rub >= 23000000 then            (amt_rub -  23000000)*0.03
                else 0
               end
            when to_char(from_dt, 'MM-DD') = '09-01' then
               case
                when amt_rub >= 650000000 then 33170000 + (amt_rub - 650000000)*0.07
                when amt_rub >= 330000000 then 13970000 + (amt_rub - 330000000)*0.06
                when amt_rub >= 115000000 then  3220000 + (amt_rub - 115000000)*0.05
                when amt_rub >=  54000000 then   780000 + (amt_rub -  54000000)*0.04
                when amt_rub >=  28000000 then            (amt_rub -  28000000)*0.03
                else 0
               end
            else 0
           end                          as reward_excess
      from (
        select d.contract_eid, d.contract_id, d.from_dt, d.till_dt,
               sum(amt_new_rub)                 as amt_new,
               sum(amt_rub)                     as amt_rub,
               -- BALANCE-15641
               sum(amt_w_nds_rub)               as amt_w_nds,
               sum(amt_rub)                     as amt
          from bo.v_comm_prof_2014_skv_src d
         group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt
           ) d
),
-- ----------------------------------------------------------------------------
-- �� ������ (������ ����)
-- ----------------------------------------------------------------------------
s_kv_direct as (
    select d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           nds, currency,
           discount_type,
           type,
           sum(amt_to_charge)       as amt_to_charge,
           null                     as amt_to_pay,
           null                     as amt_to_pay_w_nds,
           sum(amt_to_charge*decode(f.failed, 1, 0.0001,
                decode(type,
                    4, 0.08,
                    5, 0.06,
                    6, 0.05)))      as reward_to_charge,
           sum(amt_to_pay*decode(f.failed, 1, 0.0001,
                decode(type,
                    4, 0.08,
                    5, 0.06,
                    6, 0.05)))      as reward_to_pay,
           sum(amt_to_charge*0.02)  as delkredere_to_charge,
           sum(amt_to_pay   *0.02)  as delkredere_to_pay
      from (
            select contract_id, contract_eid,
                   from_dt, till_dt,
                   nds, currency,
                   discount_type,
                   payment_type,
                   case
                    when dd.discount <12 or dd.discount is null then 4
                    when dd.discount >= 12 and dd.discount < 18 then 5
                    when dd.discount >= 18                      then 6
                   end                              as type,
                   amt                              as amt_to_charge,
                   decode(payment_type,
                        -- ����������
                        2, amt_w_nds,
                        -- ����������
                        3, null)                    as amt_to_pay_w_nds,
                   decode(payment_type,
                        -- ����������
                        2, amt,
                        -- ����������
                        3, null)                    as amt_to_pay
              from bo.v_comm_prof_2014_acts_newwolc d
              left outer
              join bo.mv_client_direct_discount_m   dd on dd.client_id = d.client_id
                                                      and dd.dt = d.from_dt
             where d.discount_type in (7)
           ) d
        -- BALANCE-16952
      left outer
      join bo.v_comm_prof_2014_kv_over_70  f on f.contract_id = d.contract_id
                                            and f.from_dt = d.from_dt
                                            and f.till_dt = d.till_dt
     group by contract_eid, d.contract_id, d.from_dt, d.till_dt,
              discount_type, type, currency, nds
),
-- ----------------------------------------------------------------------------
-- �� ������
-- ----------------------------------------------------------------------------
s_kv_total as (
    select contract_id, contract_eid,
           from_dt, till_dt,
           nds, currency,
           sum(amt)                 as amt_to_pay,
           sum(amt_w_nds)           as amt_to_pay_w_nds,
           sum(decode(payment_type,
                -- ����������
                2, null,
                -- ����������
                -- BALANCE-14885
                3, amt*commission_payback_pct/100)
               )                    as reward_to_pay,
           sum(decode(payment_type,
                -- ����������
                2, null,
                -- ����������
                3, amt*0.02))       as delkredere_to_pay
      from s_payments_new
     group by contract_eid, contract_id, from_dt, till_dt,
              nds, currency
)
--
-- �������������� ������
--
select contract_id,
       contract_eid,
       from_dt,
       till_dt,
       nds,
       currency,
       discount_type,
       type             as reward_type,
       amt_to_charge    as turnover_to_charge,      -- ������ � ����������
       reward_to_charge,                            -- � ����������
       delkredere_to_charge,                        -- � ���������� (�����������)
       dkv_to_charge,                               -- ��� �� 2012 ��� � ����������
       amt_to_pay_w_nds as turnover_to_pay_w_nds,
       amt_to_pay       as turnover_to_pay,          -- ������ � ������������
       reward_to_pay,                               -- � ������������
       delkredere_to_pay,                           -- � ������������ (�����������)
       dkv_to_pay                                   -- ��� �� 2012 ��� � ����������
  from (
        select contract_eid, contract_id,
               from_dt, till_dt,
               discount_type,
               currency, nds,
               amt_to_charge,
               null         as amt_to_pay,
               null         as amt_to_pay_w_nds,
               reward_to_charge,
               reward_to_pay,
               delkredere_to_charge,
               delkredere_to_pay,
               null         as dkv_to_charge,
               null         as dkv_to_pay,
               1            as type
          from s_kv
         union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               null                             as discount_type,
               -- BALANCE-15641
               'RUR'                            as currency,
               1                                as nds,
               amt                              as amt_to_charge,
               amt                              as amt_to_pay,
               amt_w_nds                        as amt_to_pay_w_nds,
               nvl(reward_regress, 0)
                + nvl(reward_excess, 0)         as reward_to_charge,
               nvl(reward_regress, 0)
                + nvl(reward_excess, 0)         as reward_to_pay,
               0                                as delkredere_to_charge,
               0                                as delkredere_to_pay,
               null                             as dkv_to_charge,
               null                             as dkv_to_pay,
               2                                as type
          from s_skv
            -- BALANCE-17099
         where (nvl(reward_regress, 0) + nvl(reward_excess, 0)) > 0
         union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               discount_type,
               currency, nds,
               amt_to_charge,
               amt_to_pay,
               amt_to_pay_w_nds,
               reward_to_charge,
               reward_to_pay,
               delkredere_to_charge,
               delkredere_to_pay,
               null         as dkv_to_charge,
               null         as dkv_to_pay,
               type
          from s_kv_direct
         union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               null         as discount_type,
               currency, nds,
               null         as amt_to_charge,
               amt_to_pay,
               amt_to_pay_w_nds,
               null         as reward_to_charge,
               reward_to_pay,
               null         as delkredere_to_charge,
               delkredere_to_pay,
               null         as dkv_to_charge,
               null         as dkv_to_pay,
               10           as type
          from s_kv_total
         union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               discount_type,
               currency, nds,
               amt_to_charge,
               amt_to_pay,
               amt_to_pay_w_nds,
               reward_to_charge,
               reward_to_pay,
               delkredere_to_charge,
               delkredere_to_pay,
               dkv_to_charge,
               dkv_to_pay,
               0            as type
          from s_kv_old
        union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               null         as discount_type,
               currency, nds,
               null         as amt_to_charge,
               null         as amt_to_pay,
               null         as amt_to_pay_w_nds,
               reward       as reward_to_charge,
               reward       as reward_to_pay,
               null         as delkredere_to_charge,
               null         as delkredere_to_pay,
               null         as dkv_to_charge,
               null         as dkv_to_pay,
               3            as type
          from s_region
         union all
            -- BALANCE-16000
        select contract_eid, contract_id,
               from_dt, till_dt,
               71           as discount_type,
               currency, nds,
               amt_to_charge,
               null         as amt_to_pay,
               null         as amt_to_pay_w_nds,
               reward_to_charge,
               -- BALANCE-16000: ��� �������������� ������ �� �����
               reward_to_pay,
               0            as delkredere_to_charge,
               -- ����������� ������ � reward
               0            as delkredere_to_pay,
               null         as dkv_to_charge,
               null         as dkv_to_pay,
               1            as type
          from s_lc
       )
order by contract_id , from_dt,till_dt, disc, type, turn_c,rew_c;