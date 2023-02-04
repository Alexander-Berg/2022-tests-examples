with
s_comm_base_2014_dates as (
  select date'2014-04-01'       as app_start_dt,
         -- ������� ����� �������� � ������� �������
         date'2013-03-01'       as start_new_comm_rwrd
    from dual
),
--
-- ������� ����
--
s_dates as (
    select *
      from s_comm_base_2014_dates
),
s_years as (
    select to_date('01.03.2013 00:00:00', 'DD.MM.YYYY HH24:MI:SS') as from_dt,
           to_date('31.03.2014 23:59:59', 'DD.MM.YYYY HH24:MI:SS') as till_dt
      from dual
     union all
    select to_date('01.04.2014 00:00:00', 'DD.MM.YYYY HH24:MI:SS') as from_dt,
           to_date('31.03.2015 23:59:59', 'DD.MM.YYYY HH24:MI:SS') as till_dt
      from dual
),
--
-- �������� ������� �� �����
--
s_base_src as (  
   select  distinct
           xxxx.contract_eid                             as contract_eid,
           xxxx.contract_id                              as contract_id,
           xxxx.contract_from_dt                         as contract_from_dt,
           xxxx.contract_till_dt                         as contract_till_dt,
           xxxx.invoice_eid                              as invoice_eid,
           xxxx.invoice_id                               as invoice_id,
           xxxx.currency                                 as currency,
           xxxx.nds                                      as nds,    
           xxxx.nds_pct                                  as nds_pct,
           xxxx.discount_type                            as discount_type,
           xxxx.payment_type                             as payment_type,    
           xxxx.commission_type                          as commission_type,
           xxxx.loyal_clients                             as loyal_clients
		   ,decode(xxxx.endbuyer_id, 0, null, xxxx.endbuyer_id)		as endbuyer_id
 		   ,case
        when (decode(xxxx.endbuyer_id, 0, null, xxxx.endbuyer_id)) is null
          or xxxx.endbuyer_inn = xxxx.agency_inn
        then 1
        else 0
       end                                      as is_opt,
       -- BALANCE-19449
       -- ���� �� ���� �� 2015-01-01
       -- �� �����, ���������� �� 2015-01-01
       case when 
		( select count(1)
			from xxxx_new_comm_contract_basic b
				where xxxx.invoice_id = b.invoice_id
               and b.hidden < 4
               and b.act_dt < date'2015-01-01'
               and xxxx.invoice_dt < date'2015-01-01'
        ) >0 then 1 else 0
       end                                       as is_there_old_acts         
     from (
      select distinct
           x.contract_eid                             as contract_eid,
           x.contract_id                              as contract_id,
           x.contract_from_dt                         as contract_from_dt,
           x.contract_till_dt                         as contract_till_dt,
           x.invoice_eid                              as invoice_eid,
           x.invoice_id                               as invoice_id,
           x.invoice_dt                               as invoice_dt,
           x.act_dt                                   as act_dt,  
           x.currency                                 as currency,
           x.nds                                      as nds,    
           x.nds_pct                                  as nds_pct,
           nvl(x.commission_type, x.discount_type)    as discount_type,
           x.payment_type                             as payment_type,    
           x.contract_commission_type                 as commission_type,
           x.loyal_client                             as loyal_clients,
		       x.endbuyer_id                           		as endbuyer_id,
           x.endbuyer_inn                              as endbuyer_inn,
           x.agency_inn                              as agency_inn
       from xxxx_new_comm_contract_basic  x
       )   xxxx                 
      where xxxx.discount_type in (1, 2, 3, 7, 11, 12, 14)
)

--select * from s_base_src order by contract_eid;
,
s_comm_base_2014_acts_src as (
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
           nvl(xxx.is_loyal, 0)                                as loyal_clients,
           trunc(xxx.act_dt, 'MM')                             as from_dt,
           add_months(trunc(xxx.act_dt, 'MM'), 1) - 1/84600    as till_dt,
           xxx.client_id                                       as client_id,
           xxx.act_dt                                          as act_dt,
           xxx.amount                                          as amt_w_nds,
           xxx.amount-xxx.amount_nds-xxx.amount_nsp            as amt,
           (xxx.amount)*cr.rate                                as amt_rub_w_nds,
           (xxx.amount-xxx.amount_nds-xxx.amount_nsp)*cr.rate  as amt_rub
      from s_base_src                       b
      join xxxx_new_comm_contract_basic     xxx on b.invoice_id = xxx.invoice_id
      and xxx.hidden <4
      join xxxx_currency_rate               cr on cr.cc = b.currency
                                              and cr.rate_dt = trunc(xxx.act_dt)
 where b.commission_type = 47
   and b.contract_till_dt > (select app_start_dt
                               from bo.s_comm_base_2014_dates)

)
--select * from s_comm_base_2014_acts_src;
,
s_comm_base_2014_acts_m as (
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
       b.loyal_clients,
       b.from_dt,
       b.till_dt,
       b.client_id,
       b.act_dt,
       b.amt_w_nds,
       b.amt,
       b.amt_rub_w_nds,
       b.amt_rub
  from s_comm_base_2014_acts_src b
 where b.act_dt >= (select app_start_dt from bo.s_comm_base_2014_dates)
),


s_comm_base_2014_acts_m_new as (
select *
  from s_comm_base_2014_acts_m b
 where not exists (select 1 from xxxx_new_comm_contract_basic old_act
                        where old_act.invoice_first_act < (select start_new_comm_rwrd from s_dates)
                          and old_act.invoice_id = b.invoice_id
                          and old_act.hidden < 4
                          and rownum = 1)
        -- �� ���� ����� �� �������� �� 03-2013
       and not exists (select 1 from xxxx_new_comm_contract_basic fpay
                        where fpay.INVOICE_FIRST_PAYMENT < (select start_new_comm_rwrd 
							from s_dates)
                          and fpay.invoice_eid = b.invoice_eid
                          and rownum = 1)
)
--select * from s_comm_base_2014_acts_m_new;
,
s_comm_base_2014_acts_m_lc as (
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
       b.loyal_clients,
       b.from_dt,
       b.till_dt,
       b.client_id,
       b.act_dt,
       b.amt_w_nds,
       b.amt,
       b.amt_rub_w_nds,
       b.amt_rub
  from s_comm_base_2014_acts_src b
 where b.act_dt >= (select add_months(app_start_dt, -1)
                      from bo.s_comm_base_2014_dates)
   and b.loyal_clients = 1
   and b.discount_type = 7
),
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
           b.loyal_clients,
           trunc(oebs.comiss_date, 'MM')                            as from_dt,
           add_months(trunc(oebs.comiss_date, 'MM'), 1) - 1/84600   as till_dt,
			-- BALANCE-19238
           oebs.oebs_payment*100/(100 + b.nds*b.nds_pct)         as amt_all,
           oebs.oebs_payment                                     as amt_all_w_nds,
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
     where b.commission_type = 47
       and b.contract_till_dt > (select app_start_dt from s_dates)

),
-- BALANCE-16595
s_payments_new as (
    select *
      from s_payments d
        -- �� ���� ����� �� ������ �� 03-2013
     where not exists (select 1 from xxxx_new_comm_contract_basic old_act
                        where old_act.invoice_first_act < (select start_new_comm_rwrd from s_dates)
                          and old_act.invoice_id = d.invoice_id
                          and old_act.hidden < 4
                          and rownum = 1)
        -- �� ���� ����� �� �������� �� 03-2013
       and not exists (select 1 from xxxx_new_comm_contract_basic fpay
                        where fpay.INVOICE_FIRST_PAYMENT < (select start_new_comm_rwrd from s_dates)
                          and fpay.invoice_eid = d.invoice_eid
                          and rownum = 1)
        -- BALANCE-17474
       and from_dt >= (select app_start_dt from s_dates)
),
-- ---------------------------------------------------------------------------
-- ������� �������� �� �� 12/13 ���
-- ---------------------------------------------------------------------------
s_base_kv_old as (
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
                  from s_comm_base_2014_acts_m
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
     where exists (select 1 from xxxx_new_comm_contract_basic old_act
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
   group by contract_eid, contract_id, from_dt, till_dt,
            discount_type, currency, nds
),
-- ---------------------------------------------------------------------------
-- ��
-- ---------------------------------------------------------------------------
s_base_acts_monthly as (
    select b.*
      from s_comm_base_2014_acts_m_new b
),
-- ������� ��������� ������� ������� � ������ ������� �� ��������
-- �������� ������� � ������������ ��������� �������� (������ ��� �� �����)
s_kv_contract_ratio as (
    select d.contract_id, d.from_dt, d.till_dt,
           round(max(ratio*100)) as ratio
      from (
        select d.*,
               nvl(ratio_to_report(amt_rub)
                  over (partition by d.contract_id, d.from_dt), 0) as ratio
          from (
            select d.contract_id,
                   d.client_id,
                   d.from_dt,
                   d.till_dt,
                   sum(decode(d.discount_type, 7, d.amt_rub, null))  as amt_rub
              from s_base_acts_monthly              d
             group by d.from_dt, d.till_dt,
                      d.contract_id,
                      d.client_id
               ) d
           ) d
     group by d.contract_id, d.from_dt, d.till_dt
),
-- ��������� ������� ������� � ������� �� ��������
-- �� ��������� 3 ������ ����� �������
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
),
-- ������ �� ��������, �� ������� 3 ������ ������
-- ���� ������� � �������� ����� 70%
s_comm_base_2014_kv_over_70 as (
    select d.*,
    1 as failed
      from s_kv_contract_ratio_last_3m d
     where ratio >= 70
       and ratio_1m_ago >= 70
       and ratio_2m_ago >= 70
       and from_dt >= date'2014-06-01'
),
s_comm_base_2014_kv_src as (
select d."CONTRACT_EID",
       d."CONTRACT_ID",
       d."FROM_DT",
       d."TILL_DT",
       d."CLIENT_CNT",
       d."AMT_RUB_CONTR",
       d."CURRENCY",
       d."NDS",
       d."AMT_RUB",
       d."AMT_RUB_WO_LC",
       d."AMT_RUB_WO_LC_W_NDS",
       d."AMT_WO_LC",
       -- ��������� �� �� �������, ����� �������� �� �������
       -- ��� ���. �� ����� ��������� ��������� �������
       d.amt_wo_lc                                  as amt,
       case
          -- ���� ����� ��������� ����� �� ����� ��������� ����� 100 000 ���.
          -- ��� ���� � ��-�� ����� 5 �������� � �����,
          -- �� ������ ���������� ��: 0,01% + 2% �����������.
          when d.client_cnt < 5 or d.amt_rub_contr < 100000
          then
                1
          else  0
       end                                    as failed,
       -- BALANCE-17847
       -- ���� �� ������� ���������� ������ � ��������
       nvl(f.failed, 0)                       as failed_boc
  from (
    select d.contract_eid, d.contract_id, d.from_dt, d.till_dt,
           d.client_cnt, d.amt_rub_contr,
           d.currency, d.nds,
           sum(d.amt_rub)                                   as amt_rub,
           sum(case when d.loyal_clients = 1 and d.discount_type = 7
                    then 0
                    else d.amt_rub
               end)                                         as amt_rub_wo_lc,
           sum(case when d.loyal_clients = 1 and d.discount_type = 7
                    then 0
                    else d.amt_rub_w_nds
               end)                                         as amt_rub_wo_lc_w_nds,
           -- ������ ��� ������ ������� ��� �� � � ������ �����.
           -- �.�. �� ���������� ��������� ������� ��� ����� ���
           -- �������� ����� �� ��
           sum(case when d.loyal_clients = 1 and d.discount_type = 7
                    then 0
                    else d.amt
               end)                                         as amt_wo_lc
      from (
            -- BALANCE-17916
            select d.*,
                   -- ���-�� �������� �� �������� �� ������
                   count(distinct d.client_id)
                    over(partition by d.contract_id, d.from_dt) as client_cnt,
                   -- ������ ������� �� ���� (� �.�. � ��), � ���������
                   -- � �����, ����� ��������� ������ � ������
                   sum(d.amt_rub)
                    over(partition by d.contract_id, d.from_dt) as amt_rub_contr
              from s_comm_base_2014_acts_m_new d
           ) d
     group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt,
              d.client_cnt, d.amt_rub_contr,
              d.currency, d.nds
       )                                d
  left outer
  join s_comm_base_2014_kv_over_70   f on f.contract_id = d.contract_id
                           and f.from_dt = d.from_dt
                           and f.till_dt = d.till_dt
)

--select * from s_comm_base_2014_kv_src;
,
-- ��
s_kv as (
  select d.contract_eid,
         d.contract_id,
         d.from_dt,
         d.till_dt,
         d.currency,
         d.nds,
         d.amt,
         -- BALANCE-17847
         d.amt*
            case
              -- ���� ��� 5 ��������/<100� ��� ���� �� ������, �� ����������
            when d.failed = 1 or d.failed_boc = 1 then 0.01
            else 5
            end/100                             as reward,
         d.amt*2/100                            as delkredere
    from s_comm_base_2014_kv_src  d
)
--select * from s_kv;
,
-- ---------------------------------------------------------------------------
-- �� ��� ��
-- ---------------------------------------------------------------------------
s_comm_base_2014_kv_lc_src as (
select d.*,
       -- ������� ��� ��������� � ������������ ��������
       case
            when amt_rub < plan_rub*.8 then 1
            when (plan_rub - amt_rub) > 1500000 then 1
            else 0
       end                          as failed,
       -- ������� ��� �������� ��������
       case
            when amt_rub >= plan_rub*1.2 then 0
            else 1
       end                          as failed_skv
  from (
    select d.contract_eid, d.contract_id,
           d.from_dt, d.till_dt,
           d.currency, d.nds,
           d.client_id,
           -- BALANCE-17936
           lc.collateral_first_dt   as lc_start_dt,
           lc.collateral_end_dt     as lc_end_dt,
           -- �������� ���� �� ������ � ������
           lc.lc_turnover           as plan_rub,
           sum(amt_rub)             as amt_rub,
           sum(amt_rub_w_nds)       as amt_rub_w_nds,
           sum(amt)                 as amt
      from s_comm_base_2014_acts_m_lc    d
      join xxxx_loyal_clients_contr_attr   lc on lc.contract_id = d.contract_id
                                              and lc.client_id = d.client_id
                                              and lc.collateral_first_dt >= date'2013-03-01'
     where d.act_dt between lc.collateral_first_dt and lc.collateral_end_dt
  group by d.contract_eid, d.contract_id,
           d.currency, d.nds,
           d.from_dt, d.till_dt,
           lc.collateral_first_dt, lc.collateral_end_dt,
           d.client_id, lc.lc_turnover
       ) d
),
s_comm_base_2014_kv_lc_last_2m as (    
    select d.contract_eid, d.contract_id,
       d.from_dt, d.till_dt,
       d.currency, d.nds,
       d.client_id,
       -- BALANCE-17936
       lc_start_dt,
       lc_end_dt,
       plan_rub,
       amt_rub,
       amt_rub_w_nds,
       amt,
       failed,
       case
          -- ���� ���� �������� �� ������� �����,
          -- �� ��� � ����������
        when add_months(from_dt, -1) = from_dt_1m_ago
            then failed_1m_ago
          -- ���� �������� �� ������� ����� ���,
          -- � ��������� � ������� ������ ��� ��������,
          -- �� ����������
        when add_months(from_dt, -1) >= trunc(lc_start_dt, 'MM')
            then 1
          -- �� ���� ��������� ������� �������, �� �ok�
        else 0
       end                                                  as failed_1m_ago,
       case
        when add_months(from_dt, -1) = from_dt_1m_ago
            then from_dt_1m_ago
        else null
       end                                                  as from_dt_1m_ago
  from (
        select d.*,
               lag(d.from_dt, 1) over (partition by contract_id, client_id
                                           order by from_dt)    as from_dt_1m_ago,
               lag(d.failed, 1) over (partition by contract_id, client_id
                                          order by from_dt)     as failed_1m_ago
          from s_comm_base_2014_kv_lc_src d
       ) d
    -- ���������� ������ �����, ����������� � bo.s_comm_base_2014_acts_m_lc
 where from_dt >= (select app_start_dt from bo.s_comm_base_2014_dates)
),
-- �� ��, ������� �� ��������� ���� 2 ������ ������
s_kv_lc_failed as (
    select *
      from s_comm_base_2014_kv_lc_last_2m
     where failed = 1
       and failed_1m_ago = 1
),
-- �� ��� ��
s_kv_lc_src as (
    select d.contract_eid,
           d.contract_id,
           d.client_id,
           d.from_dt,
           d.till_dt,
           d.currency,
           d.nds,
           d.amt,
           d.amt_rub,
           d.amt_rub_w_nds,
           case
                -- ���� �� �������� ���� �� 5 �������� ��� 100� (��� �� �������)
                -- �� �� �� ���������� �� ������, �� ������ �� ���� �� ��
                when kv.failed = 1 or
                -- ���� ���� �� �� ��������, �� ������� �� ����������
                -- ����� �� ��
                f.failed = 1 then 1
                             else 0
           end                   as failed
      from s_comm_base_2014_acts_m_lc  d
      left outer
      join s_kv_lc_failed   f on f.contract_id = d.contract_id
                             and f.from_dt = d.from_dt
                              -- BALANCE-17430
                             and f.client_id = d.client_id
        -- BALANCE-17442
      join s_comm_base_2014_kv_src   kv on kv.contract_id = d.contract_id
                                          and kv.from_dt = d.from_dt
                                          and kv.currency = d.currency
                                          and kv.nds = d.nds
      -- BALANCE-17437
      -- ���������� ������ �����, ����������� � bo.s_comm_base_2014_acts_m_lc
  where d.from_dt >= (select app_start_dt from bo.s_comm_base_2014_dates)
),
s_kv_lc as (
    select d.contract_eid,
           d.contract_id,
           d.from_dt,
           d.till_dt,
           d.currency,
           d.nds,
           sum(d.amt)             as amt,
           sum(d.amt*decode(
                d.failed,
                    1, 0.01,
                    5)/100)       as reward,
           sum(d.amt*2/100)       as delkredere
      from s_kv_lc_src d
  group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt,
           d.currency, d.nds
),
-- ---------------------------------------------------------------------------
-- ��� (�����������)
-- ---------------------------------------------------------------------------
s_dkv_q_src as (
    select contract_eid,
           contract_id,
           from_dt,
           till_dt,
           -- BALANCE-17916
           1                        as nds,
           sum(amt_w_nds)           as amt_w_nds,
           sum(amt)                 as amt
      from (
        select a.contract_eid,
               a.contract_id,
               trunc(a.from_dt, 'Q')                              as from_dt,
               add_months(trunc(a.from_dt, 'Q'), 3)-1/24/60/60    as till_dt,
               -- BALANCE-18001
               -- �.�. ���� ������� �������� ��, �� �����
               -- ������ �� ���������, ����� �� �����������.
               -- kv.amt ������������ �� �����, �.�. ��� �������������
               -- �� discount_type, � ��� ���� ��������� ������
               case
                when a.loyal_clients = 1 and a.discount_type = 7
                then 0
                else a.amt_rub
               end                                                as amt,
               case
                when a.loyal_clients = 1 and a.discount_type = 7
                then 0
                else a.amt_rub_w_nds
               end                                                as amt_w_nds
          from bo.s_comm_base_2014_acts_m_new   a
            -- ������� ���� ������ �� �� �������, �� �������
            -- ������� ���������� ��
          join bo.s_comm_base_2014_kv_src   kv on kv.failed = 0
                                               -- BALANCE-17847
                                              and kv.failed_boc = 0
                                              and kv.from_dt = a.from_dt
                                              and kv.contract_id = a.contract_id
                                              and kv.currency = a.currency
                                              and kv.nds = a.nds
            -- BALANCE-17399:
            -- ������ ������ �� ������, ������, �������, ����������
         where a.discount_type in (7, 11, 1, 2, 3, 12)
            -- BALANCE-17847
            -- BALANCE-17848
            -- � �� ������ ������, ������� ����������� ������ �� ����
         union all
        select a.contract_eid,
               a.contract_id,
               trunc(a.from_dt, 'Q')                              as from_dt,
               add_months(trunc(a.from_dt, 'Q'), 3)-1/24/60/60    as till_dt,
               a.amt_rub        as amt,
               a.amt_rub_w_nds  as amt_w_nds
          from s_kv_lc_src a
         where a.failed = 0
           ) d
     group by contract_eid, contract_id,
              from_dt, till_dt
),
s_dkv_q as (
    select d.*,
           case
            when amt >= 3000000     then 108000 + (amt - 3000000)*6/100
            when amt >= 1500000     then  33000 + (amt - 1500000)*5/100
            when amt >=  900000     then  27000 + (amt -  900000)*1/100
            when amt >=  600000     then           amt*3/100
                                    else 0
           end                      as reward
      from s_dkv_q_src d
),
-- ---------------------------------------------------------------------------
-- ������������ ���������
-- ---------------------------------------------------------------------------
s_comm_base_2014_region_src as (
select b."CONTRACT_EID",
       b."CONTRACT_ID",
       b."FROM_DT",
       b."TILL_DT",
       b."CLIENT_ID",
       b."AMT_DIRECT",
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
      from s_comm_base_2014_acts_m b
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
        -- BALANCE-16017: � 2013-10 15� ������� ������ �� �������.
        -- � ��������� ��������� ���� ������� ����������
        -- BALANCE-18014: ������� �� ������ � ����� ���� �� 1 �������
    having sum(decode(b.discount_type, 7, b.amt, null)) > 0
       ) b
),
s_comm_base_2014_region as (
 select b.contract_eid, b.contract_id,
       trunc(b.from_dt, 'Q')                    as from_dt,
       add_months(
        trunc(b.till_dt, 'Q'), 3) - 1/84600     as till_dt,
       -- BALANCE-14914
       -- BALANCE-14976
       'RUR'                                    as currency,
       1                                        as nds,
       count(distinct b.client_id)              as client_cnt
  from s_comm_base_2014_region_src b
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
            when client_cnt >= 101   then 160000
            when client_cnt >=  75   then 130000
            when client_cnt >=  51   then 100000
            when client_cnt >=  36   then  70000
            when client_cnt >=  21   then  50000
            when client_cnt >=  15   then  30000
                                     else      0
           end                    as reward
      from s_comm_base_2014_region
),
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
           sum(amt_all)             as amt_to_pay,
           sum(amt_all_w_nds)       as amt_to_pay_w_nds
      from s_payments_new
     group by contract_eid, contract_id, from_dt, till_dt,
              nds, currency
           ) d  
        -- ������ �� ������, �� ������ ������� �� ���� �����
     where not exists(
            select 1 from s_comm_base_2014_acts_m_new a
             where a.contract_id = d.contract_id
               and a.from_dt = d.from_dt
           )
)

--select * from s_kv_total;
,
-- ----------------------------------------------------------------------------
-- ��� (���)
-- ----------------------------------------------------------------------------
s_comm_base_2014_skv_src as (
select d."CONTRACT_EID",
       d."CONTRACT_ID",
       d."FROM_DT",
       d."TILL_DT",
       d."CURRENCY",
       d."NDS",
       d."CLIENT_ID",
       d."DISCOUNT_TYPE",
       d."AMT_NEW",
       d."AMT_W_NDS",
       d."AMT",
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
         y.from_dt,
         y.till_dt,
         d.currency, d.nds, d.client_id, d.discount_type,
         sum(case
            when 0 = (
                select count(1) from xxxx_new_comm_contract_basic old_act
                 where old_act.invoice_first_act < (select start_new_comm_rwrd
                                       from bo.s_comm_base_2014_dates)
                   and old_act.invoice_id = d.invoice_id
                   and old_act.hidden < 4
                   and rownum = 1)
             and 0 = (
                select count(1) from bo.xxxx_new_comm_contract_basic fpay
                 where fpay.INVOICE_FIRST_PAYMENT < (select start_new_comm_rwrd
                                            from bo.s_comm_base_2014_dates)
                   and fpay.invoice_eid = d.invoice_eid
                   and rownum = 1)
            then amt
            else 0
             end)                                       as amt_new,
         sum(d.amt_w_nds)                               as amt_w_nds,
         sum(d.amt) as amt
    from s_comm_base_2014_acts_m  d
    join s_years     y on d.act_dt between y.from_dt and y.till_dt
      -- ������, ������� (���), ����������.
   where discount_type in (7, 1, 2, 3, 12)
      -- �������� ������ ��� ��,
      -- ������� ��������� ���� �� 120%
     and (
              (
                  d.loyal_clients = 1 and
                  d.discount_type = 7 and
                  not exists (
                      select 1 from bo.s_comm_base_2014_kv_lc_src f
                       where f.from_dt = d.from_dt
                         and f.contract_id = d.contract_id
                         and f.client_id = d.client_id
                         and f.failed_skv = 1)
              )
              or not (d.loyal_clients = 1 and d.discount_type = 7)
         )
   group by d.contract_eid, d.contract_id, y.from_dt, y.till_dt,
            d.currency, d.nds, d.client_id, d.discount_type
       ) d
),
-- ��� (���)
s_skv as (
    select d.*,
           case
            when amt > 42000000 then amt_new*0.03
                                else 0
           end as reward
      from (
        select d.contract_eid, d.contract_id, d.from_dt, d.till_dt,
               d.currency, d.nds,
               -- BALANCE-15635
               max(round(nvl(direct_client_ratio*100, 0), 2)) as max_client_ratio,
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
          from s_comm_base_2014_skv_src d
         group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt,
                  d.currency, d.nds
           ) d
     where max_client_ratio < 70
),
--
-- 
--
s_old_src as (
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
       amt_to_pay       as turnover_to_pay,         -- ������ � ������������
       amt_to_pay_w_nds as turnover_to_pay_w_nds,
       reward_to_pay,                               -- � ������������
       delkredere_to_pay,                           -- � ������������ (�����������)
       dkv_to_pay                                   -- ��� �� 2012 ��� � ����������
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
          from s_kv
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
          from s_kv_lc
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
          from s_base_kv_old
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
          from s_skv
            -- BALANCE-17099
         where reward != 0
        union all
        select contract_eid, contract_id,
               from_dt, till_dt,
               null         as discount_type,
               'RUB'        as currency,
               nds,
               amt          as amt_to_charge,
               amt          as amt_to_pay,
               amt_w_nds    as amt_to_pay_w_nds,
               reward       as reward_to_charge,
               reward       as reward_to_pay,
               0            as delkredere_to_charge,
               0            as delkredere_to_pay,
               null         as dkv_to_charge,
               null         as dkv_to_pay,
               20           as type         -- BALANCE-17453
          from s_dkv_q
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
          from s_region
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
 )    ,
	 
s_old as (
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
       delkredere_to_charge,                        -- � ���������� (�����������)
       dkv_to_charge,                               -- ��� �� 2012 ��� � ����������
       turnover_to_pay,                             -- ������ � ������������
       turnover_to_pay_w_nds,                       -- ������ � ������������ (� ���)
       reward_to_pay,                               -- � ������������
       delkredere_to_pay,                           -- � ������������ (�����������)
       dkv_to_pay                                   -- ��� �� 2012 ��� � ����������
  from s_old_src)
,

  s_pre as (
    select opt.contract_id,
           opt.contract_eid,
           opt.from_dt,
           opt.till_dt,
           opt.nds,
           opt.currency,
           opt.discount_type,
           200 + opt.reward_type                                        as reward_type,
           -- � ����������
           opt.turnover_to_charge   - nvl(com.turnover_to_charge, 0)    as turnover_to_charge,
           opt.reward_to_charge     - nvl(com.reward_to_charge, 0)      as reward_to_charge,
           opt.delkredere_to_charge - nvl(com.delkredere_to_charge, 0)  as delkredere_to_charge,
           opt.dkv_to_charge        - nvl(com.dkv_to_charge, 0)         as dkv_to_charge,
           -- � ������������
           opt.turnover_to_pay_w_nds - nvl(com.turnover_to_pay_w_nds,0 ) as turnover_to_pay_w_nds,
           opt.turnover_to_pay       - nvl(com.turnover_to_pay, 0)      as turnover_to_pay,
           opt.reward_to_pay     - nvl(com.reward_to_pay, 0)            as reward_to_pay,
           opt.delkredere_to_pay - nvl(com.delkredere_to_pay, 0)        as delkredere_to_pay,
           opt.dkv_to_pay        - nvl(com.dkv_to_pay, 0)               as dkv_to_pay
      from s_old              opt
      left outer
        -- �������, ��� ���� ��������� � ���.�����
        -- ����� �������
      join xxxx_commission_part     com on com.contract_id = opt.contract_id
                                                   and com.from_dt = opt.from_dt
                                                   and com.till_dt = opt.till_dt
                                                   and com.nds = opt.nds
                                                   and com.currency = opt.currency
                                                   and com.reward_type = opt.reward_type
                                                   and nvl(com.discount_type, -1) = nvl(opt.discount_type, -1)
        -- BALANCE-19554: �������� �� ������� ������� ������ �� ���������,
        -- ���� �������� 2015-{���,���}. ��� �����:
            -- * �� �� ���, ���
            -- * ������ �� ������� (�� ������, ���� �� �������)
            -- * ��� �� ���������
     where date'2015-01-01' between opt.from_dt and opt.till_dt
        or date'2015-02-01' between opt.from_dt and opt.till_dt
        or date'2015-03-01' between opt.from_dt and opt.till_dt
)
select contract_id,     contract_eid,
       date'2015-01-01'                         as from_dt,
       date'2015-04-01' - 1/24/60/60            as till_dt,
       nds,             currency,
       discount_type,
       reward_type,
       -- � ����������
       sum(turnover_to_charge)                  as turnover_to_charge,
       sum(reward_to_charge)                    as reward_to_charge,
       sum(delkredere_to_charge)                as delkredere_to_charge,
       sum(dkv_to_charge)                       as dkv_to_charge,
       -- � ������������
       sum(turnover_to_pay_w_nds)               as turnover_to_pay_w_nds,
       sum(turnover_to_pay)                     as turnover_to_pay,
       sum(reward_to_pay)                       as reward_to_pay,
       sum(delkredere_to_pay)                   as delkredere_to_pay,
       sum(dkv_to_pay)                          as dkv_to_pay
  from s_pre
 group by contract_id, contract_eid,
          nds, currency, discount_type, reward_type
  order by contract_id , from_dt,discount_type, reward_type ,nds, currency,turnover_to_charge ,turnover_to_pay;