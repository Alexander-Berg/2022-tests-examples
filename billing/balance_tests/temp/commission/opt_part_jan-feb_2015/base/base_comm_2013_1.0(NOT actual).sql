with
--
-- ������� ����
--
s_dates as (
         -- ���� ������ �������� ����� ���������
  select date'2013-03-01'     as start_new_comm_rwrd,
         -- ���� ������ �������� ��������
         add_months(trunc(sysdate, 'Q'),15)  as start_curr_qtr
    from dual
),
s_base_src as (  
    select distinct
           contract_eid                             as contract_eid,
           contract_id                              as contract_id,
           contract_from_dt                         as contract_from_dt,
           contract_till_dt                         as contract_till_dt,
           invoice_eid                              as invoice_eid,
           invoice_id                               as invoice_id,
           currency                                 as currency,
           nds                                      as nds,    
           nds_pct                                  as nds_pct,
           nvl(commission_type, discount_type)      as discount_type,
           payment_type                             as payment_type,    
           contract_commission_type                 as commission_type,
           loyal_client                             as loyal_clients,
           endbuyer_id                              as endbuyer_id
      from xxxx_new_comm_contract_basic                   
      where discount_type in (1, 2, 3, 7, 11, 12, 14)
)

--seleCt *  From S_Base_Src
--Order By Contract_Eid, Invoice_Eid;

,
s_last_src as (
    select b.contract_eid, b.contract_id,
           b.contract_from_dt, b.contract_till_dt,
           b.invoice_eid, b.invoice_id,
           b.discount_type,
           b.currency, b.nds, b.nds_pct,
           t.value_dt                                           as terminate_dt,
           trunc(t.value_dt, 'Q')                               as from_dt,
           add_months( trunc(t.value_dt, 'Q'), 3) - 1/84600     as till_dt
      from s_base_src      b
      join xxxx_contract_signed_attr       t  on t.contract_id = b.contract_id
                                               and t.code = 'CALC_TERMINATION'
     where b.commission_type = 47
        -- ����������
       and b.payment_type = 2
)

--select *  from s_last_src;

,
-- ����
s_last_acts as (
    select b.contract_eid, b.contract_id,
           b.contract_from_dt, b.contract_till_dt,
           b.invoice_eid, b.invoice_id,
           b.discount_type,
           b.currency, b.nds, b.nds_pct,
           b.from_dt,
           b.till_dt,
           sum(xxx.amount)                                      as amt_w_nds,
           sum(xxx.amount-xxx.amount_nds-xxx.amount_nsp)          as amt
      from s_last_src        b
      join xxxx_new_comm_contract_basic     xxx on b.invoice_id = xxx.invoice_id
      and xxx.hidden <4
      and xxx.act_dt >= date'2013-03-01'
      and xxx.act_dt <= nvl(b.terminate_dt, xxx.act_dt)
     group by b.contract_eid, b.contract_id,
           b.contract_from_dt, b.contract_till_dt,
           b.invoice_eid, b.invoice_id,
           b.discount_type,
           b.currency, b.nds, b.nds_pct,
           b.from_dt, b.till_dt
)

--select *  from s_last_acts;

,
-- ������
s_last_pays as (
    select b.contract_eid, b.contract_id,
           b.contract_from_dt, b.contract_till_dt,
           b.invoice_eid, b.invoice_id,
           b.discount_type,
           b.currency, b.nds, b.nds_pct,
           b.from_dt,
           b.till_dt,
           sum(oebs.oebs_payment)                                       as amt_w_nds,
           sum(oebs.oebs_payment*100/(100 + b.nds*b.nds_pct))           as amt
      from s_last_src             b
      join xxxx_oebs_cash_payment_test     oebs on oebs.invoice_id = b.invoice_id
                                                and oebs.comiss_date is not null
                                                and oebs.comiss_date >= date'2013-03-01'   
                                                and oebs.comiss_date <= nvl(b.terminate_dt, oebs.comiss_date)                                                   
     group by b.contract_eid, b.contract_id,
              b.contract_from_dt, b.contract_till_dt,
              b.invoice_eid, b.invoice_id,
              b.discount_type,
              b.currency, b.nds, b.nds_pct,
              b.from_dt, b.till_dt
)

--select *  from s_last_pays;

,
s_comm_base_2013_term as (
 -- ������� �� ����� (������ - ���� --�� ��� ����� �����-- � 01.03.2013)
    select p.contract_eid, p.contract_id,
           p.contract_from_dt, p.contract_till_dt,
           p.invoice_eid, p.invoice_id,
           p.discount_type,
           p.currency, p.nds,
           p.from_dt, p.till_dt,
--           round(sum(p.amt - nvl(a.amt, 0)), 2)       as amt,
--           sum(p.amt_w_nds - nvl(a.amt_w_nds, 0))     as amt_w_nds
--      from s_last_pays    p
--      left outer
--      join s_last_acts    a on a.invoice_id = p.invoice_id

sum(p_amt - nvl(a_amt, 0))                   as amt,
       sum(p_amt_w_nds - nvl(a_amt_w_nds, 0))       as amt_w_nds
  from (
        select p.contract_eid, p.contract_id,
               p.contract_from_dt, p.contract_till_dt,
               p.invoice_eid, p.invoice_id,
               p.discount_type,
               p.currency, p.nds,
               p.from_dt, p.till_dt,
               0                            as a_amt,
               0                            as a_amt_w_nds,
               p.amt                        as p_amt,
               p.amt_w_nds                  as p_amt_w_nds
          from s_last_pays p
         union all
        select p.contract_eid, p.contract_id,
               p.contract_from_dt, p.contract_till_dt,
               p.invoice_eid, p.invoice_id,
               p.discount_type,
               p.currency, p.nds,
               p.from_dt, p.till_dt,
               p.amt                        as a_amt,
               p.amt_w_nds                  as a_amt_w_nds,
               0                            as p_amt,
               0                            as p_amt_w_nds
          from s_last_acts p
       ) p
     group by p.contract_eid, p.contract_id,
           p.contract_from_dt, p.contract_till_dt,
           p.invoice_eid, p.invoice_id,
           p.discount_type,
           p.currency, p.nds,
           p.from_dt, p.till_dt
)

--select *  from s_comm_base_2013_term;

,
--------------------------------------------------------------------------------------------------------------
--
-- �������� ������� �� �����
--
s_base_acts as (
    select b.contract_eid,
           b.contract_id,
           b.contract_from_dt,
           b.contract_till_dt,
           b.invoice_eid,
           b.invoice_id,
           b.currency,
           b.nds,
           b.payment_type,
           b.discount_type,
           case
            when xxx.act_dt < date'2013-10-01' then 
                b.loyal_clients
            else nvl(xxx.is_loyal, 0)
           end                                      as loyal_clients,
           trunc(xxx.act_dt, 'Q')                       as from_dt,
           add_months(
               trunc(xxx.act_dt, 'Q'), 3) - 1/84600     as till_dt,
           xxx.client_id                                as client_id,
           xxx.act_dt                                   as act_dt,
           xxx.amount                                   as amt_w_nds,
           xxx.amount-xxx.amount_nds-xxx.amount_nsp     as amt
 --          b.commission_type                            as commission_type  
      from s_base_src                       b
      join xxxx_new_comm_contract_basic     xxx on b.invoice_id = xxx.invoice_id
      and xxx.hidden <4
      and xxx.act_dt >= (select start_new_comm_rwrd
                     from s_dates)
      left outer
      join xxxx_contract_signed_attr       t  on t.contract_id = b.contract_id
                                              and t.code = 'CALC_TERMINATION'
      where b.commission_type = 47  
        -- BALANCE-17454
       and (
            -- ���� ���� ���� �� ������������ ���������
            (    xxx.act_dt >= date'2014-04-01'
             and b.contract_till_dt <= date'2014-04-01'
            )
            or
            -- ���� � �������� �������� ��������
            (
                xxx.act_dt < date'2014-04-01'
            )
           )
        -- BALANCE-17634
        -- ���� ���� �� �� ���������� ������������ (��������� ��� ����������),
        -- �� ������� �� ���� ������ �� ����, ��������� � ��
        -- � ����� ���� ������� �� ����������/�������.
       and xxx.act_dt <= nvl(t.value_dt, xxx.act_dt)
 -- BALANCE-19159
         -- ������� � 01.01.2015 ��������� ����, �� ������ �������
         -- �� ������ �������� ����������
        and (
             -- ������� � 2015, ��������� ������� �����
             -- � ��������� ������ �� �����, � ������� ���� �������� ����������
             (
                 xxx.act_dt >= date'2015-01-01'
             and b.endbuyer_id is not null
             )
             or
             -- �� 2015 ���� ������ �� ������
             (
                 xxx.act_dt < date'2015-01-01'
             
			  )
            )
)

--select *  FROM s_base_acts
--ORDER BY contract_eid, invoice_eid;

,
s_base_acts_monthly as (
    select b.contract_eid,
           b.contract_id,
           b.contract_from_dt,
           b.contract_till_dt,
           b.invoice_eid,
           b.invoice_id,
           b.currency,
           b.nds,
           b.payment_type,
           b.discount_type,
           trunc(xxx.act_dt, 'MM')                       as from_dt,
           add_months(
            trunc(xxx.act_dt, 'MM'), 1) - 1/84600     as till_dt,
           xxx.client_id                                as client_id,
           xxx.act_dt                                   as act_dt,
           xxx.amount-xxx.amount_nds-xxx.amount_nsp     as amt
      from s_base_src                       b
      join xxxx_new_comm_contract_basic     xxx on b.invoice_id = xxx.invoice_id
      and xxx.hidden <4
      and xxx.act_dt >= (select start_new_comm_rwrd
                     from s_dates)
      left outer join xxxx_contract_signed_attr       t  on t.contract_id = b.contract_id
                                             and t.code = 'CALC_TERMINATION'
      where b.commission_type = 47  
        -- BALANCE-7454
       and (
            (    xxx.act_dt >= date'2014-04-01'
             and b.contract_till_dt <= date'2014-04-01'
            )
            or
            (
                xxx.act_dt < date'2014-04-01'
            )
           )
        -- BALANCE-17634
        -- ���� ���� �� �� ���������� ������������ (��������� ��� ����������),
        -- �� ������� �� ���� ������ �� ����, ��������� � ��
        -- � ����� ���� ������� �� ����������/�������.
       and xxx.act_dt <= nvl(t.value_dt, xxx.act_dt)
 -- BALANCE-19159
         -- ������� � 01.01.2015 ��������� ����, �� ������ �������
         -- �� ������ �������� ����������
        and (
             -- ������� � 2015, ��������� ������� �����
             -- � ��������� ������ �� �����, � ������� ���� �������� ����������
             (
                 xxx.act_dt >= date'2015-01-01'
             and b.endbuyer_id is not null
             )
             or
             -- �� 2015 ���� ������ �� ������
             (
                 xxx.act_dt < date'2015-01-01'
             )
            )
)

--select * from s_base_acts_monthly;


,
--
-- �������� ������� �� �������
--
s_payments as (
    select b.contract_eid,
           b.contract_id,
           b.invoice_eid,
           b.invoice_id,
           b.currency,
           b.nds,
           b.nds_pct,
           b.payment_type,
           b.discount_type,
           oebs.oebs_payment*100/(100 + b.nds*b.nds_pct)         as ttl_amt,
           oebs.oebs_payment                                     as ttl_amt_w_nds,
           b.loyal_clients,
           trunc(oebs.comiss_date, 'Q')                 as from_dt,
           add_months(
            trunc(oebs.comiss_date, 'Q'), 3) - 1/84600  as till_dt,
           decode(b.payment_type,
                -- ����������
                2, 0,
                -- ����������
                3, oebs.oebs_payment*100/
                        (100 + b.nds*b.nds_pct))        as amt,
           decode(b.payment_type,
                -- ����������
                2, 0,
                -- ����������
                3, oebs.oebs_payment)                         as amt_w_nds
      from s_base_src                       b
      -- �������
      join xxxx_oebs_cash_payment_test     oebs on oebs.invoice_id = b.invoice_id
                                                 -- BALANCE-14988
                                                and oebs.comiss_date >= date'2013-03-01'
                                                 -- BALANCE-15631
                                                and oebs.comiss_date is not null
      left outer
      join xxxx_contract_signed_attr        t  on t.contract_id = b.contract_id
                                              and t.code = 'CALC_TERMINATION'
     where b.commission_type = 47
        -- BALANCE-17454
       and (
            (    oebs.comiss_date >= date'2014-04-01'
             and b.contract_till_dt <= date'2014-04-01'
            )
            or
            (
                oebs.comiss_date < date'2014-04-01'
            )
           )
        -- BALANCE-17634
        -- ���� ���� �� �� ���������� ������������ (��������� ��� ����������),
        -- �� ������� �� ������ ������ �� ����, ��������� � ��
        -- � ����� ���� ������� �� ����������/�������.
       and oebs.comiss_date <= nvl(t.value_dt, oebs.comiss_date)
 -- BALANCE-19159
         -- ������� � 01.01.2015 ��������� ����, �� ������ �������
         -- �� ������ �������� ����������
        and (
             -- ������� � 2015, ��������� ������� �����
             -- � ��������� ������ �� �����, � ������� ���� �������� ����������
             (
                 oebs.comiss_date >= date'2015-01-01'
             and b.endbuyer_id is not null
             )
             or
             -- �� 2015 ���� ������ �� ������
             (
                 oebs.comiss_date < date'2015-01-01'
             )
            )
  )

--select * from s_payments;

,
-- BALANCE-16595
s_payments_2013 as (
    select *
      from s_payments d
        -- �� ���� ����� �� ������ �� 03-2013
     where not exists (select 1 from xxxx_new_comm_contract_basic old_act
                        where old_act.invoice_first_act < (select start_new_comm_rwrd
                                                from s_dates)
                          and old_act.invoice_id = d.invoice_id
                          and old_act.hidden < 4
                          and rownum = 1)
        -- �� ���� ����� �� �������� �� 03-2013
       and not exists (select 1 from xxxx_new_comm_contract_basic fpay
                        where fpay.INVOICE_FIRST_PAYMENT < (select start_new_comm_rwrd
                                                  from s_dates)
                          and fpay.invoice_eid = d.invoice_eid
                          and rownum = 1)
        -- �� ���������� ������� � �� ����������
        -- �.�. ������������� ��������� ������ � 2013Q4
        -- ������� ������������ ������� �� ����� ������ ��������
       and from_dt >= date'2013-10-01'
)

--select * from s_payments_2013;

,
--
-- ������� �������� �� �� 12/13 ���
--
s_base_kv_2012 as (
  select contract_id, contract_eid, from_dt, till_dt,
         discount_type, currency, nds,
         sum(amt_to_charge)     as amt_to_charge,
         sum(amt_to_pay)        as amt_to_pay,
         sum(amt_to_pay_w_nds)  as amt_to_pay_w_nds,
         sum(case
                when discount_type in (
                        7, 11, 12, 14, 1, 2, 3) then amt_to_charge*0.1
                                                else 0
            end) as reward_to_charge,
         sum(case
                when discount_type in (
                        7, 11, 12, 14, 1, 2, 3) then amt_to_pay*0.1
                                                else 0
            end) as reward_to_pay,
         sum(case
                when discount_type in (
                        7, 11, 12, 14, 1, 2, 3) then amt_to_charge*0.03
                                                else 0
            end) as delkredere_to_charge,
         sum(case
                when discount_type in (
                        7, 11, 12, 14, 1, 2, 3) then amt_to_pay*0.03
                                                else 0
            end) as delkredere_to_pay,
         sum(case
                when discount_type in (12)      then amt_to_charge*0.12
                when discount_type in (1, 2, 3) then amt_to_charge*0.03
                                                else 0
            end) as dkv_to_charge,
         sum(case
                when discount_type in (12)      then amt_to_pay*0.12
                when discount_type in (1, 2, 3) then amt_to_pay*0.03
                                                else 0
            end) as dkv_to_pay
      from (
                select contract_id, contract_eid, invoice_eid, invoice_id,
                       from_dt, till_dt, discount_type, currency, nds,
                       amt as amt_to_charge, 0 as amt_to_pay, 0 as amt_to_pay_w_nds
                  from s_base_acts
                    -- BALANCE-15132
                    -- BALANCE-15196
                 where not (loyal_clients = 1 and discount_type = 7)
                 union all
                select contract_id, contract_eid, invoice_eid, invoice_id,
                       from_dt, till_dt, discount_type, currency, nds,
                       0            as amt_to_charge,
                       -- BALANCE-15154
                       amt          as amt_to_pay,
                       amt_w_nds    as amt_to_pay_w_nds
                  from s_payments
                    -- BALANCE-15132
                    -- BALANCE-15196
                 where not (loyal_clients = 1 and discount_type = 7)
           ) d
        -- ���� ���� �� ������ �� 03-2013
     where (exists (select 1 from xxxx_new_comm_contract_basic old_act
                        where old_act.invoice_first_act < (select start_new_comm_rwrd
                                              from s_dates)
                          and old_act.invoice_id = d.invoice_id
                          and old_act.hidden < 4
                          and rownum = 1)
        -- ���� ������ �� �������� �� 03-2013
        or exists (select 1 from xxxx_new_comm_contract_basic fpay
                        where fpay.INVOICE_FIRST_PAYMENT < (select start_new_comm_rwrd
                                                from s_dates)
                          and fpay.invoice_eid = d.invoice_eid
                          and rownum = 1)
           )
   group by contract_eid, contract_id, from_dt, till_dt,
            discount_type, currency, nds
)

--select * from s_base_kv_2012;
,
-- BALANCE-17634: ����������� ������ �� ������
s_base_kv_2012_last as (
    select contract_id, contract_eid, from_dt, till_dt,
           discount_type, currency, nds,
           sum(amt)         as amt_to_charge,
           sum(amt*0.01)    as reward_to_charge,
           sum(amt*0.02)    as delkredere_to_charge,
           0 as dkv_to_charge,
           0 as amt_to_pay,
           0 as amt_to_pay_w_nds,
           0 as reward_to_pay,
           0 as delkredere_to_pay,
           0 as dkv_to_pay
      from s_comm_base_2013_term d
        -- ���� ���� �� ������ �� 03-2013
     where (exists (select 1 from xxxx_new_comm_contract_basic old_act
                        where old_act.invoice_first_act < (select start_new_comm_rwrd
                                                from s_dates)
                          and old_act.invoice_id = d.invoice_id
                          and old_act.hidden < 4
                          and rownum = 1)
        -- �� ���� ����� �� �������� �� 03-2013
       or exists (select 1 from xxxx_new_comm_contract_basic fpay
                        where fpay.INVOICE_FIRST_PAYMENT < (select start_new_comm_rwrd
                                                  from s_dates)
                          and fpay.invoice_eid = d.invoice_eid
                          and rownum = 1)
           )
    group by contract_eid, contract_id, from_dt, till_dt,
             discount_type, currency, nds
)

--select * from s_base_kv_2012_last;
,
-- ���� �� ��, �������� ������ �������� ��
s_base_kv_lc_all as (
    select d.contract_eid, d.contract_id, d.contract_from_dt, d.contract_till_dt,
           d.currency, d.nds,
           d.from_dt, d.till_dt, d.client_id, lc.lc_turnover,
           lc.lc_turnover *
            case
                -- ������ ������� � ��������. ������� ������ �� 1 ������.
                -- ������� � ���� ��� ���� ������� �� 1 ������.
                when d.from_dt = date'2013-01-01' then 1
                -- ��������� ������� ������� ������ �� ������������
                -- ���������� �� ���� �������� � �������
                -- �������� ��
                -- BALANCE-15421
                else 3*( -- ���-�� ���� �� ������� ��, ������� ����������� �� �������
                        least(d.till_dt, lc.collateral_end_dt) - greatest(d.from_dt, lc.collateral_first_dt)
                       )
                       -- ���-�� ���� � ��������
                       / (d.till_dt - d.from_dt)
            end         as turnover_q,
           -- BALANCE-15676
           sum(amt*cr.rate)     as amt_rub,
           sum(amt)     as amt
      from s_base_acts                      d
      join xxxx_loyal_clients_contr_attr    lc on lc.contract_id = d.contract_id
                                              and lc.client_id = d.client_id
                                              and lc.collateral_first_dt >= date'2013-03-01'
      join xxxx_currency_rate               cr on cr.cc = d.currency
                                              and cr.rate_dt = trunc(d.act_dt)
     where d.loyal_clients = 1
       and d.discount_type = 7
       and d.act_dt between lc.collateral_first_dt and lc.collateral_end_dt
  group by d.contract_eid, d.contract_id, d.contract_from_dt, d.contract_till_dt,
           d.currency, d.nds,
           d.from_dt, d.till_dt, d.client_id, 
           lc.lc_turnover, lc.collateral_end_dt, lc.collateral_first_dt
)


--select * from s_base_kv_lc_all;
,
-- ��, ������� �� ���������� �������� �������.
s_base_kv_lc_failed as (
    select *
      from s_base_kv_lc_all
       where turnover_q > amt_rub
)


--select * from s_base_kv_lc_failed;
,
-- ��, ������� ���������� �������� �������.
s_base_kv_lc_passed as (
    select *
      from s_base_kv_lc_all
       where turnover_q <= amt_rub
)

--select * from s_base_kv_lc_passed; 
,
-- ������ �� ��, ������� �� ����� �� ������,
-- ��������������� �� �������� � ������ ���������
s_base_kv_lc as (
    select contract_eid,
           contract_id,
           from_dt,
           till_dt,
           currency,
           nds,
           sum(amt)             as amt,
           sum(amt*0.01/100)    as reward,
           sum(amt*2/100)       as delkredere
      from s_base_kv_lc_failed
  group by contract_eid, contract_id, from_dt, till_dt,
           currency, nds
)

--select * from s_base_kv_lc;
 ,
--
-- �������� ������ ��� ������� �������� ��
--
s_base_kv_src as (
  select res.*,
         -- ������� ���������� ����� � �������� � ���-�� ������������
         -- (nds=0), ���� ���� ���� �� ���� �� ���, �� �������� ��
         -- �������
         -- (���� ����� ������ �� �������)
         count(distinct nds)        over (partition by contract_id, from_dt, till_dt) as nds_cnt,
         count(distinct decode(nds, 1, null, nds))
                                    over (partition by contract_id, from_dt, till_dt) as nds_not_1_cnt,
         count(distinct currency)   over (partition by contract_id, from_dt, till_dt) as currency_cnt,
         count(distinct decode(currency, 'RUR', null, currency))
                                    over (partition by contract_id, from_dt, till_dt) as currency_not_rub_cnt
    from (
      select d.contract_eid, d.contract_id, d.from_dt, d.till_dt,
             d.currency, d.nds, d.client_cnt,
             sum(d.amt) as amt
        from (
                select d.*,
                       -- BALANCE-14785: �������� ������� �� ����� ������� �� ������
                       count(distinct d.client_id) over
                            (partition by contract_id, from_dt, till_dt) as client_cnt
                  from s_base_acts d
             ) d
          -- �� ���� ����� �� ������ �� 03-2013
       where not exists (select 1 from xxxx_new_comm_contract_basic old_act
                          where old_act.invoice_first_act < (select start_new_comm_rwrd
                                                from s_dates)
                            and old_act.invoice_id = d.invoice_id
                            and old_act.hidden < 4
                            and rownum = 1)
          -- �� ���� ����� �� �������� �� 03-2013
         and not exists (select 1 from xxxx_new_comm_contract_basic fpay
                          where fpay.INVOICE_FIRST_PAYMENT < (select start_new_comm_rwrd
                                                  from s_dates)
                            and fpay.invoice_eid = d.invoice_eid
                            and rownum = 1)
          -- �������� ���, ��� �������� ��
          -- � �������� ���� �� ������
         and (
                ( d.loyal_clients = 1 and
                  d.discount_type = 7 and
                  exists (select 1 from s_base_kv_lc_passed f
                                  where f.from_dt = d.from_dt
                                    and f.contract_id = d.contract_id
                                    and f.client_id = d.client_id
                                    and rownum = 1)
                )
             or not (d.loyal_clients = 1 and d.discount_type = 7)
             )
       group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt,
                d.currency, d.nds, d.client_cnt
         ) res
)


--select * from  s_base_kv_src;

,
--
-- �������������� ������ ��� ������� �������� ��
-- (������ ��������)
s_base_kv as (
  select contract_eid,
         contract_id,
         from_dt,
         till_dt,
         currency,
         nds,
         amt,
         --
         -- �������� ��������������
         --
         case
          -- ���� ������ ����� ������ ��� ���� ���������� (nds=0), �� 0
          when nds_cnt <> 1 or currency_cnt <> 1 or
               currency_not_rub_cnt > 0 or nds_not_1_cnt <> 0 then null
          -- �������� ������ �������
          when from_dt = date'2013-01-01' then
            case
              when client_cnt < 5                   then amt*0.01
              when amt <  133500                    then amt*0.01
              when amt >= 133500 and amt <= 249999  then 13350
              when amt >= 250000 and amt <= 334999  then 27000
              when amt >= 335000 and amt <= 499999  then 40000
              when amt >= 500000 and amt <= 834999  then 70000
              when amt >= 835000                    then amt*0.08
            end
          -- ��������� ��������
          else
            case
              when client_cnt < 5                     then amt*0.01
              when amt <   400000                     then amt*0.01
              when amt >=  400000 and amt <=  749999  then 40000
              when amt >=  750000 and amt <=  999999  then 80000
              when amt >= 1000000 and amt <= 1499999  then 120000
              when amt >= 1500000 and amt <= 2499999  then 210000
              when amt >= 2500000                     then amt*0.08
            end
         end                    as reward,
         --
         -- �������������� �� �����������
         --
         case
          -- ���� ������ ����� ������ ��� ���� ���������� (nds=0), �� 0
          when nds_cnt <> 1 or currency_cnt <> 1 or
               currency_not_rub_cnt > 0 or nds_not_1_cnt <> 0 then null
          -- �������� ������ �������
          when from_dt = date'2013-01-01' then
            case
              when client_cnt < 5                   then amt*0.02
              when amt <  133500                    then amt*0.02
              when amt >= 133500 and amt <= 249999  then  3350
              when amt >= 250000 and amt <= 334999  then  6700
              when amt >= 335000 and amt <= 499999  then 10000
              when amt >= 500000 and amt <= 834999  then 13350
              when amt >= 835000                    then amt*0.02
            end
          -- ��������� ��������
          else
            case
              when client_cnt < 5                     then amt*0.02
              when amt <   400000                     then amt*0.02
              when amt >=  400000 and amt <=  749999  then 10000
              when amt >=  750000 and amt <=  999999  then 20000
              when amt >= 1000000 and amt <= 1499999  then 30000
              when amt >= 1500000 and amt <= 2499999  then 40000
              when amt >= 2500000                     then amt*0.02
            end
         end                    as delkredere
    from s_base_kv_src
)



--select * from s_base_kv;
,
-- BALANCE-17634: ����������� ������
s_base_kv_last as (
    select contract_id, contract_eid, from_dt, till_dt,
                      currency, nds,
           sum(amt)         as amt,
           sum(amt*0.01)    as reward,
           sum(amt*0.02)    as delkredere
      from s_comm_base_2013_term d
        -- �� ���� ����� �� ������ �� 03-2013
       where not exists (select 1 from xxxx_new_comm_contract_basic old_act
                          where old_act.invoice_first_act < (select start_new_comm_rwrd
                                                from s_dates)
                            and old_act.invoice_id = d.invoice_id
                            and old_act.hidden < 4
                            and rownum = 1)
          -- �� ���� ����� �� �������� �� 03-2013
         and not exists (select 1 from xxxx_new_comm_contract_basic fpay
                          where fpay.INVOICE_FIRST_PAYMENT < (select start_new_comm_rwrd
                                                  from s_dates)
                            and fpay.invoice_eid = d.invoice_eid
                            and rownum = 1)
    group by contract_eid, contract_id, from_dt, till_dt,
             currency, nds
)


--select * from s_base_kv_last;

,
--
-- ������������ ���������
--
s_base_region_src as (
    select b.contract_eid, b.contract_id,
           trunc(b.from_dt, 'Q')                    as from_dt,
           add_months(
            trunc(b.till_dt, 'Q'), 3) - 1/84600     as till_dt,
           -- BALANCE-14914
           -- BALANCE-14976
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
              from s_base_acts_monthly      b
                -- ������������ ��������� 2013
              join xxxx_contract_signed_attr  a on a.code = 'SUPERCOMMISSION_BONUS'
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
            having (sum(decode(b.discount_type, 7, b.amt, null)) >= 5000 and
                    b.from_dt < date'2013-10-01')
                -- BALANCE-16017: � 2013-10 15� ������� ������ �� �������.
                -- � ��������� ��������� ���� ������� ����������
                or (sum(decode(b.discount_type, 7, b.amt, null)) >= 1 and
                    b.from_dt >= date'2013-10-01')
               ) b
           ) b
        -- � ������� �������� ������ ���� ����� 3 ������ �� ������� �������
        -- � ������ �������� (�.�. �� ������� ������ �� 03-2013) ������
        -- ���� ����� 1 ������ ��� ������� �������
     where (
                trunc(b.from_dt, 'Q') = date'2013-01-01'
            and client_cnt_in_Q = 1
           )
        or (
                trunc(b.from_dt, 'Q') <> date'2013-01-01'
            and client_cnt_in_Q = 3
             -- BALANCE-16017, BALANCE-16754: ��������� 15� �� �������
            and client_amt_in_Q >= 15000
           )
     group by b.contract_eid, b.contract_id,
              trunc(b.from_dt, 'Q'),
              add_months(trunc(b.till_dt, 'Q'), 3)
)



--select * from s_base_region_src;

,
s_base_region as (
    select contract_eid, contract_id,
           from_dt, till_dt,
           currency, nds,
           case
            -- �������� ������ �������
            when from_dt = date'2013-01-01' then
              case
                when client_cnt < 15                        then 0
                when client_cnt >= 15 and client_cnt <= 25  then 16500
                when client_cnt >= 26 and client_cnt <= 40  then 23300
                when client_cnt >= 41 and client_cnt <= 75  then 28300
                when client_cnt >= 76                       then 43300
              end
            -- ��������� ��������
            else
              case
                when client_cnt < 15                        then 0
                when client_cnt >= 15 and client_cnt <= 25  then 50000
                when client_cnt >= 26 and client_cnt <= 40  then 70000
                when client_cnt >= 41 and client_cnt <= 75  then 85000
                when client_cnt >= 76                       then 130000
              end
           end                    as reward
      from s_base_region_src
)

--select * from s_base_region;
,
-- ----------------------------------------------------------------------------
-- �� ������
-- ----------------------------------------------------------------------------
-- BALANCE-16595
s_kv_total as (
    select *
      from (
    select contract_id, contract_eid,
           from_dt, till_dt,
           nds, currency, 
           sum(amt)                 as amt_to_pay,
           sum(amt_w_nds)           as amt_to_pay_w_nds
      from s_payments_2013
     group by contract_eid, contract_id, from_dt, till_dt,
              nds, currency
           ) d
       where not exists(
              select 1 from s_base_acts a
               where a.contract_id = d.contract_id
                 and a.from_dt = d.from_dt
             )
)

--
--select * from s_kv_total;

,
--
-- �������� ������ ��� ������� �������� ���
--

s_years as (
 select to_date('01.03.2013 00:00:00', 'DD.MM.YYYY HH24:MI:SS') as from_dt,
 to_date('31.03.2014 23:59:59', 'DD.MM.YYYY HH24:MI:SS') as till_dt
 from dual
 union all
select to_date('01.04.2014 00:00:00', 'DD.MM.YYYY HH24:MI:SS') as from_dt,
  to_date('31.03.2015 23:59:59', 'DD.MM.YYYY HH24:MI:SS') as till_dt
  from dual
),


s_base_dkv_src as (
    select d.*,
           count(distinct nds)        over (partition by contract_id) as nds_cnt,
           count(distinct decode(nds, 1, null, nds))
                                      over (partition by contract_id) as nds_not_1_cnt,
           count(distinct currency)   over (partition by contract_id) as currency_cnt,
           count(distinct decode(currency, 'RUR', null, currency))
                                      over (partition by contract_id) as currency_not_rub_cnt,
           -- ���������� ���� ������� � ������� �� �������� (������ �� �������)
           -- ���� ���� ���� ���-��, � ����� > .7, �� ������� �� ����������
           ratio_to_report(decode(d.discount_type, 7, d.amt, 0))
                over(partition by d.contract_id) as direct_client_ratio
      from (
      select d.contract_eid, d.contract_id,
              -- BALANCE-18189
              y.from_dt,
              y.till_dt,
             d.currency, d.nds, d.client_id, d.discount_type,
             sum(case
                when 0 = (
                    select count(1) from xxxx_new_comm_contract_basic old_act
                     where old_act.invoice_first_act < (select start_new_comm_rwrd
                                           from s_dates)
                       and old_act.invoice_id = d.invoice_id
                       and old_act.hidden < 4
                       and rownum = 1)
                 and 0 = (
                    select count(1) from xxxx_new_comm_contract_basic fpay
                     where fpay.INVOICE_FIRST_PAYMENT < (select start_new_comm_rwrd
                                                from s_dates)
                       and fpay.invoice_eid = d.invoice_eid
                       and rownum = 1)
                then amt
                else 0
                 end)                                       as amt_new,
             sum(d.amt_w_nds)                               as amt_w_nds,
             sum(d.amt) as amt
        from s_base_acts d
        join s_years     y on d.act_dt between y.from_dt and y.till_dt
          -- ������, ������� (���), ����������.
       where discount_type in (7, 1, 2, 3, 12)
          -- ��� ��
         and not (loyal_clients = 1 and discount_type = 7)
       group by d.contract_eid, d.contract_id, y.from_dt, y.till_dt,
                d.currency, d.nds, d.client_id, d.discount_type
           ) d
)


--select * from s_base_dkv_src;

,
--
-- �������������� ������ ��� ������� �������� ���
-- (������ ��������)
--
s_base_dkv as (
    select d.*,
           case
            when amt > 35000000 then amt_new*0.03
                                else 0
           end as reward
      from (
        select d.contract_eid, d.contract_id, d.from_dt, d.till_dt,
               d.currency, d.nds,
               -- BALANCE-15635
               max(nvl(direct_client_ratio, 0)) as max_client_ratio,
               sum(amt_new) as amt_new,
               sum(case
                    -- ���� ������ ����� ������ ��� ���� ���������� (nds=0), �� 0
                    when nds_cnt <> 1 or currency_cnt <> 1 or
                         currency_not_rub_cnt > 0 or nds_not_1_cnt <> 0
                    then null
                    else amt_w_nds
                   end) as amt_w_nds,
               sum(case
                    -- ���� ������ ����� ������ ��� ���� ���������� (nds=0), �� 0
                    when nds_cnt <> 1 or currency_cnt <> 1 or
                         currency_not_rub_cnt > 0 or nds_not_1_cnt <> 0
                    then null
                    else amt
                   end) as amt
          from s_base_dkv_src d
         group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt,
                  d.currency, d.nds
           ) d
     where max_client_ratio <= 0.7
)

--select * from s_base_dkv;
--
-- �������������� ������
--
select contract_id,
       contract_eid,
       from_dt,
       till_dt,
       nds                   as nds,
       currency              as curr,
       discount_type         as disc,
       type                  as type,
       amt_to_charge         as turn_c,      -- ������ � ����������
       reward_to_charge      as rew_c,      -- � ����������
       delkredere_to_charge  as del_c,      -- � ���������� (�����������)
       dkv_to_charge         as dkv_c,      -- ��� �� 2012 ��� � ����������
       amt_to_pay_w_nds      as turn_p_nds,
       amt_to_pay            as turn_p,      -- ������ � ������������       
       reward_to_pay         as rew_p,      -- � ������������
       delkredere_to_pay     as del_p,      -- � ������������ (�����������)       
       dkv_to_pay            as dkv_p      -- ��� �� 2012 ��� � ����������  
  from (
        select contract_eid, contract_id,
               from_dt, till_dt,
               null as discount_type,
               currency, nds,
               amt          as amt_to_charge,
               null         as amt_to_pay,
               null         as amt_to_pay_w_nds,
               reward       as reward_to_charge,
               reward       as reward_to_pay,
               delkredere   as delkredere_to_charge,
               delkredere   as delkredere_to_pay,
               null         as dkv_to_charge,
               null         as dkv_to_pay,
               1            as type
          from s_base_kv
        union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               null         as discount_type,           
               currency, nds,
               amt          as amt_to_charge,
               null         as amt_to_pay,
               null         as amt_to_pay_w_nds,
               reward       as reward_to_charge,
               reward       as reward_to_pay,
               delkredere   as delkredere_to_charge,
               delkredere   as delkredere_to_pay,
               null         as dkv_to_charge,
               null         as dkv_to_pay,
               101            as type
          from s_base_kv_last
        union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               71 as discount_type,
               currency, nds,
               amt          as amt_to_charge,
               null         as amt_to_pay,
               null         as amt_to_pay_w_nds,
               reward       as reward_to_charge,
               reward       as reward_to_pay,
               delkredere   as delkredere_to_charge,
               delkredere   as delkredere_to_pay,
               null         as dkv_to_charge,
               null         as dkv_to_pay,
               1            as type
          from s_base_kv_lc
        union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               discount_type,
               currency, nds,
               amt_to_charge,           amt_to_pay,     amt_to_pay_w_nds,
               reward_to_charge,        reward_to_pay,
               delkredere_to_charge,    delkredere_to_pay,
               dkv_to_charge,           dkv_to_pay,
               0            as type
          from s_base_kv_2012
         union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               discount_type,
               currency, nds,
               amt_to_charge,           amt_to_pay,     amt_to_pay_w_nds,
               reward_to_charge,        reward_to_pay,
               delkredere_to_charge,    delkredere_to_pay,
               dkv_to_charge,           dkv_to_pay,
               100            as type
          from s_base_kv_2012_last
        union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               null as discount_type,
               currency, nds,
               amt          as amt_to_charge,
               amt          as amt_to_pay,
               amt_w_nds    as amt_to_pay_w_nds,
               reward       as reward_to_charge,
               reward       as reward_to_pay,
               0            as delkredere_to_charge,
               0            as delkredere_to_pay,
               null         as dkv_to_charge,
               null         as dkv_to_pay,
               2            as type
          from s_base_dkv
            -- BALANCE-14764
         where reward > 0
        union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               null as discount_type,
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
          from s_base_region
        union all
            -- BALANCE-16595
        select contract_eid, contract_id,
               from_dt, till_dt,
               null         as discount_type,
               currency, nds,
               null         as amt_to_charge,
                -- BALANCE-18829
               amt_to_pay,
               amt_to_pay_w_nds,
               null         as reward_to_charge,
               null         as reward_to_pay,
               null         as delkredere_to_charge,
               null         as delkredere_to_pay,
               null         as dkv_to_charge,
               null         as dkv_to_pay,
               10           as type
          from s_kv_total
       )
--       where (type= 101 and discount_type is null) or type= 100
       order by contract_id , from_dt, type, disc, currency, nds;