
  CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."V_OPT_2015_PROF_SKV" ("CONTRACT_EID", "CONTRACT_ID", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "DISCOUNT_TYPE", "NDS", "CLIENT_ID", "FROM_DT", "TILL_DT", "TILL_DT_FC", "NDS_COUNT", "CURRENCY_COUNT", "AMT_W_NDS_RUB", "AMT_RUB_NOT_LC", "AMT_RUB_LC", "AMT_RUB", "AMT_FOR_FORECAST", "MAX_RATIO", "EXCLUDED", "FAILED") AS 
  with
-- ���������
s_half_years as (
    select d.dt from_dt, add_months(d.dt, 6)-1/24/60/60 as till_dt
      from (
         select add_months(date'2017-03-01', 6*(level-1)) as dt
           from dual
        connect by level <= 10
           ) d
),
-- ���� ��, � ������� ����������� ��������� � ���������
-- ���� ��� ���� ���� ����� �� ������ �� ���. ���, �.�. ������
-- ���������, ������� ���������� � ������� ���. ����. � �� �����
-- �� ���� ��������� ���� ������ �� ���������, � �� ������ ���,
-- ������� ��������� � ������� ���.����
-- BALANCE-18412
s_acts_lc as (
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
           a.is_loyal,
           o.client_id,
           at.amount                                        as amt_w_nds,
           at.amount-at.amount_nds-at.amount_nsp            as amt,
           at.amount*cr.rate                                as amt_w_nds_rub,
           (at.amount-at.amount_nds-at.amount_nsp)*cr.rate  as amt_rub,
           a.dt                                             as act_dt,
           h.from_dt,
           trunc(h.till_dt)                         as till_dt,
           -- ���������, ��� �� �������� ������ ����
           -- 1 ������ � 1 ���
           count(distinct b.nds)      over (partition by b.contract_id) as nds_count,
           count(distinct b.currency) over (partition by b.contract_id) as currency_count,
           lc.turnover_ttl,
           -- BALANCE-20689
           round(abs(months_between(
                lc.todate, lc.collateral_dt)))      as lc_months_count,
           lc.collateral_dt                         as lc_start_dt,
           lc.todate                                as lc_end_dt
      from bo.mv_opt_2015_invoices_f        b
      join bo.t_act                         a  on a.invoice_id = b.invoice_id
                                              and a.hidden < 4
                                              and a.dt >= date'2017-03-01'
                                              and a.is_loyal = 1
      join bo.t_act_trans                   at on at.act_id = a.id
      join bo.t_consume                     q  on q.id = at.consume_id
      join bo.t_order                       o  on o.id = q.parent_order_id
      join (
            select d.*, hy.till_dt,
                   -- BALANCE-25878
                   sum(d.turnover) over(partition by contract_id, hy.till_dt) as turnover_ttl
              from bo.mv_loyal_clients_contr_attr   d 
              join s_half_years                     hy on d.todate between hy.from_dt and hy.till_dt
             where collateral_dt >= add_months(trunc(sysdate, 'YYYY'), 2)
           )    lc on lc.contract_id = b.contract_id
                  and lc.client_id = o.client_id
                  and a.dt between lc.collateral_dt and lc.todate
      join biee.mv_currency_rate            cr on cr.cc = b.currency
                                              and cr.rate_dt = trunc(a.dt)
      join s_half_years                     h  on lc.todate between h.from_dt
                                                                and h.till_dt
     where b.commission_type = 2
        -- ������ �������� �������� ���.����
       and b.contract_till_dt > add_months(trunc(sysdate, 'YYYY'), 2)
),
s_skv_lc as (
    select d.*
      from (
        select d.contract_eid, d.contract_id,
               d.contract_from_dt,
               d.contract_till_dt,
               d.currency, d.nds, d.discount_type,
               d.is_loyal,
               d.turnover_ttl,
               d.from_dt,
               d.till_dt,
               d.lc_months_count,
               d.nds_count, d.currency_count,
               sum(d.amt_w_nds_rub)                     as amt_w_nds_rub,
               sum(d.amt_rub)                           as amt_rub
          from s_acts_lc                 d
         group by d.from_dt, d.till_dt,
                  d.contract_from_dt,
                  d.contract_till_dt,
                  d.contract_eid, d.contract_id,
                  d.currency, d.nds, d.discount_type,
                  d.is_loyal, d.lc_months_count,
                  d.nds_count, d.currency_count,
                  d.turnover_ttl
           ) d
     where d.amt_rub >= d.turnover_ttl*d.lc_months_count*1.2
),
s_skv_not_lc as (
    select d.contract_eid, d.contract_id,
           d.contract_from_dt,
           d.contract_till_dt,
           d.currency, d.nds, d.discount_type,
           d.client_id, d.is_loyal,
           -- ��� �������� (��� �� �� �����, �.�. �� �� ��������� � ��������)
           -- ����� �����, �� ����� ������ ����� ������ ��� �������
           d.till_dt                                    as till_dt_fc,
           -- ������� ���������
           h.from_dt                                    as from_dt,
           -- ���������, ��� �� �������� ������ ����
           -- 1 ������ � 1 ���
           d.nds_count,
           d.currency_count,
           trunc(h.till_dt)                             as till_dt,
           sum(d.amt_w_nds_rub)                         as amt_w_nds_rub,
           sum(d.amt_rub)                               as amt_rub
      from bo.v_opt_2015_acts             d
      join s_half_years                   h  on d.act_dt between h.from_dt
                                                             and h.till_dt
        -- ��� ��
       and d.is_loyal = 0
       and d.commission_type = 2
        -- ������, ������� (���), ����������
        -- BALANCE-22408
     where d.discount_type in (7, 1, 2, 3, 12, 36, 37)
     group by h.from_dt, trunc(h.till_dt), d.till_dt,
              d.nds_count, d.currency_count,
              d.contract_from_dt,
              d.contract_till_dt,
              d.contract_eid, d.contract_id,
              d.currency, d.nds, d.discount_type,
              d.client_id, d.is_loyal
)
select d.contract_eid, d.contract_id,
       d.contract_from_dt,
       d.contract_till_dt,
       decode(d.discount_type,
            -- ���������� ������� ��� ������, �.�. ����� �� ������ �������
            12, 7,
            -- ��� ������� ��� ������� 1 �����
            2, 1, 3, 1, 37, 1,
            d.discount_type) as discount_type,
       d.nds,
       d.client_id,
       d.from_dt, d.till_dt, d.till_dt_fc,
       d.nds_count, d.currency_count,
       d.amt_w_nds_rub,
       d.amt_rub_not_lc,
       d.amt_rub_lc,
       d.amt_rub_not_lc + d.amt_rub_lc as amt_rub,
       d.amt_rub_not_lc                as amt_for_forecast,
       -- ��: ����������� ������ ����������� ��������� ������ ��� �������, ����
       -- ��������� ���������� ��������� �������� � ������� ����������������
       -- ��������� ��������� ����� �� ������� ������, ��������� � �����������
       -- ���������� ������ ������� ��������� < 70% (����������� �� �����
       -- ���������)
       l.max_ratio,
       0                                as excluded,
       case
          -- BALANCE-20688
        when nds_count > 1                  then 1
        when currency_count > 1             then 1
          -- BALANCE-25859: ��� ������ ��� ������� � �����������
        when round(l.max_ratio, 2) >= 0.7
         and discount_type in (7, 12)       then 1
        else 0
       end          as failed
  from (
        select d.contract_eid, d.contract_id,
               d.contract_from_dt,
               d.contract_till_dt,
               d.discount_type, d.nds,
               d.client_id,
               d.from_dt, d.till_dt, d.till_dt_fc,
               d.nds_count, d.currency_count,
               d.amt_w_nds_rub,
               d.amt_rub            as amt_rub_not_lc,
               0                    as amt_rub_lc
          from s_skv_not_lc d
         union all
        select d.contract_eid, d.contract_id,
               d.contract_from_dt,
               d.contract_till_dt,
               d.discount_type, d.nds,
               null                 as client_id,
               d.from_dt, d.till_dt, d.till_dt as till_dt_fc,
               d.nds_count, d.currency_count,
               d.amt_w_nds_rub,
               0                    as amt_rub_not_lc,
               d.amt_rub            as amt_rub_lc
          from s_skv_lc d
       ) d
       -- ������� ������������ ���� �� ������� �� ������� �� ���������
  join bo.v_opt_2015_prof_skv_less_70   l on l.contract_id = d.contract_id
                                         and l.from_dt = d.from_dt;
