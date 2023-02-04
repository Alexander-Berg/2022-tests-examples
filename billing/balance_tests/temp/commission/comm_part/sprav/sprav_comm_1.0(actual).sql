with
--
-- ������� ����
--
s_dates as (
    select date'2013-03-01'   as start_dt
      from dual
),

s_base_src as (  
select xxx.contract_eid                             as contract_eid,
           xxx.contract_id                              as contract_id,
           xxx.contract_from_dt                         as contract_from_dt,
           xxx.contract_till_dt                         as contract_till_dt,
           xxx.invoice_eid                              as invoice_eid,
           xxx.invoice_id                               as invoice_id,
           xxx.invoice_dt                               as invoice_dt,
           xxx.currency                                 as currency,
           xxx.nds                                      as nds,
           xxx.nds_pct                                  as nds_pct,
           xxx.discount_type                            as discount_type,
		       xxx.payment_type                             as payment_type, 
           xxx.commission_type                          as commission_type,
		       xxx.commission_payback_type                  as commission_payback_type,
		       xxx.endbuyer_id								              as endbuyer_id,
           xxx.endbuyer_inn                             as endbuyer_inn,
           xxx.agency_inn                               as agency_inn,
           -- ���� ��������� � ������� �����, ���� ��� ��������� ����������
           -- ��� ����, �� ��� ��-�� � ��������� ���������� ���������.
           case
            when (decode(xxx.endbuyer_id, 0, null, xxx.endbuyer_id)) is null
              or xxx.endbuyer_inn = xxx.agency_inn
            then 1
            else 0
           end                                      as is_opt,
           -- BALANCE-19449
           -- ���� �� ���� �� 2015-01-01
           -- �� �����, ���������� �� 2015-01-01
           case when
           (
                select count(1)
                  from xxxx_new_comm_contract_basic         b
                 where xxx.invoice_id = b.invoice_id
                   and b.hidden < 4
                   and b.act_dt < date'2015-01-01'
                   and xxx.invoice_dt < date'2015-01-01'
            ) > 0 then 1 else 0
           end                                       as is_there_old_acts
    from 
    (select distinct
           x.contract_eid                             as contract_eid,
           x.contract_id                              as contract_id,
           x.contract_from_dt                         as contract_from_dt,
           x.contract_till_dt                         as contract_till_dt,
           x.invoice_eid                              as invoice_eid,
           x.invoice_id                               as invoice_id,
           x.invoice_dt                               as invoice_dt,
           x.currency                                 as currency,
           x.nds                                      as nds,
           x.nds_pct                                  as nds_pct,
           nvl(x.commission_type, x.discount_type)    as discount_type,
		       x.payment_type                             as payment_type, 
           x.contract_commission_type                 as commission_type,
           x. commission_payback_pct                  as commission_payback_type,  
		       x.endbuyer_id                              as endbuyer_id,
           x.endbuyer_inn                             as endbuyer_inn,
           x.agency_inn                               as agency_inn
            from xxxx_new_comm_contract_basic  x)    xxx
		where xxx.discount_type in (1, 2, 3, 7, 11, 12, 14)
)
--select * from xxxx_new_comm_contract_basic;
,
--
-- �������� ������� �� �����
--
s_acts as (
  select * from (
    select b.contract_eid,
           b.contract_id,
           b.invoice_eid,
           b.invoice_id,
           b.currency,
           b.nds,
           b.payment_type,
           b.discount_type,
           -- ��� �������:
           -- 1  �� ���������� (����)
           -- 2  �� ������������ (������ � oebs)
           b.commission_payback_type,
           xxx.client_id                                as client_id,
           trunc(xxx.act_dt, 'MM')                      as from_dt,
           add_months(
               trunc(xxx.act_dt, 'MM'), 1) - 1/84600    as till_dt,
           xxx.amount                                   as amt_w_nds, 
           xxx.amount-xxx.amount_nds-xxx.amount_nsp     as amt, 
           xxx.act_dt                                   as act_dt,
           b.commission_type                            as commission_type  
      from s_base_src                       b
      join xxxx_new_comm_contract_basic     xxx on b.invoice_id = xxx.invoice_id
      and xxx.hidden <4
      and xxx.act_dt >= (select start_dt from s_dates)
       -- BALANCE-19159
         -- ������� � 01.01.2015 ��������� ����, �� ������ �������
         -- �� ������ �������� ����������
        and (
             -- ������� � 2015, ��������� ������� �����
             -- � ��������� ������ �� �����, � ������� ���� �������� ����������
             (
                 xxx.act_dt >= date'2015-01-01'
             -- BALANCE-19429
             and b.is_opt = 0
             )
             or
             -- �� 2015 ���� ������ �� ������
             (
                 xxx.act_dt < date'2015-01-01'
             )
            )
  ) b
  where b.commission_type = 50
    and b.discount_type = 12  

),
s_acts_2013 as (
    select *
      from s_acts d
        -- �� ���� ����� �� ������ �� 03-2013
     where not exists (select 1 from xxxx_new_comm_contract_basic old_act
                        where old_act.invoice_first_act < (select start_dt from s_dates)
                          and old_act.invoice_id = d.invoice_id
                          and old_act.hidden < 4
                          and rownum = 1)
        -- �� ���� ����� �� �������� �� 03-2013
       and not exists (select 1 from xxxx_new_comm_contract_basic fpay
                        where fpay.INVOICE_FIRST_PAYMENT < (select start_dt from s_dates)
                          and fpay.invoice_eid = d.invoice_eid
                          and rownum = 1)
),
s_acts_2012 as (
    select *
      from s_acts d
        -- ���� ���� �� ������ �� 03-2013
     where exists (select 1 from xxxx_new_comm_contract_basic old_act
                        where old_act.invoice_first_act < (select start_dt from s_dates)
                          and old_act.invoice_id = d.invoice_id
                          and old_act.hidden < 4
                          and rownum = 1)
        -- ���� ������ �� �������� �� 03-2013
        or exists (select 1 from xxxx_new_comm_contract_basic fpay
                        where fpay.INVOICE_FIRST_PAYMENT < (select start_dt from s_dates)
                          and fpay.invoice_eid = d.invoice_eid
                          and rownum = 1)
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
           b.payment_type,
           b.discount_type,
           -- ��� �������:
           -- 1  �� ���������� (����)
           -- 2  �� ������������ (������ � oebs)
           b.commission_payback_type,
           trunc(oebs.comiss_date, 'MM')                       as from_dt,
           add_months(
               trunc(oebs.comiss_date, 'MM'), 1) - 1/84600     as till_dt,
           oebs.oebs_payment*100/(100 + b.nds*b.nds_pct)       as amt,
           oebs.oebs_payment                                   as amt_w_nds
      from s_base_src                       b
      join xxxx_oebs_cash_payment_test     oebs on oebs.invoice_id = b.invoice_id
                                               and oebs.comiss_date is not null
      where b.commission_type = 50
      and b.discount_type = 12
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
             or
             -- �� 2015 ���� ������ �� ������
             (
                 oebs.comiss_date < date'2015-01-01'
             )
            )
),
s_payments_2013 as (
    select *
      from s_payments d
        -- �� ���� ����� �� ������ �� 03-2013
     where not exists (select 1 from xxxx_new_comm_contract_basic old_act
                        where old_act.invoice_first_act < (select start_dt from s_dates)
                          and old_act.invoice_id = d.invoice_id
                          and old_act.hidden < 4
                          and rownum = 1)
        -- �� ���� ����� �� �������� �� 03-2013
       and not exists (select 1 from xxxx_new_comm_contract_basic fpay
                        where fpay.INVOICE_FIRST_PAYMENT < (select start_dt from s_dates)
                          and fpay.invoice_eid = d.invoice_eid
                          and rownum = 1)
),
s_payments_2012 as (
    select *
      from s_payments d
        -- ���� ���� �� ������ �� 03-2013
     where exists (select 1 from xxxx_new_comm_contract_basic old_act
                        where old_act.invoice_first_act < (select start_dt from s_dates)
                          and old_act.invoice_id = d.invoice_id
                          and old_act.hidden < 4
                          and rownum = 1)
        -- ���� ������ �� �������� �� 03-2013
        or exists (select 1 from xxxx_new_comm_contract_basic fpay
                        where fpay.INVOICE_FIRST_PAYMENT < (select start_dt from s_dates)
                          and fpay.invoice_eid = d.invoice_eid
                          and rownum = 1)
),
--
-- ��
--
s_kv as (
    select contract_id, contract_eid,
           from_dt, till_dt,
           nds, currency,
           discount_type,
           -- ����������
           sum(amt_to_charge)               as amt_to_charge,
           sum(amt_to_charge*0.23)          as reward_to_charge,
           sum(amt_to_charge*0.02)          as delkredere_to_charge,
           -- ������
           sum(decode(commission_payback_type,
                1, null,
                2, amt_to_pay))             as amt_to_pay,
           sum(decode(commission_payback_type,
                1, null,
                2, amt_to_pay_w_nds))       as amt_to_pay_w_nds,
           sum(0.23*decode(commission_payback_type,
                1, amt_to_charge,
                2, amt_to_pay))             as reward_to_pay,
           sum(0.02*decode(commission_payback_type,
                1, amt_to_charge,
                2, amt_to_pay))             as delkredere_to_pay
      from (
            select contract_id, contract_eid,
                   from_dt, till_dt, commission_payback_type,
                   discount_type, currency, nds,
                   amt          as amt_to_charge,
                   amt_w_nds    as amt_to_charge_w_nds,
                   0            as amt_to_pay,
                   0            as amt_to_pay_w_nds
              from s_acts_2013
             union all
            select contract_id, contract_eid,
                   from_dt, till_dt, commission_payback_type,
                   discount_type, currency, nds,
                   0            as amt_to_charge,
                   0            as amt_to_charge_w_nds,
                   amt          as amt_to_pay,
                   amt_w_nds    as amt_to_pay_w_nds
              from s_payments_2013
           )
     group by contract_eid, contract_id, from_dt, till_dt,
              discount_type, currency, nds
),
--
-- �� 2012 ���
--
s_kv_2012 as (
    select contract_id, contract_eid,
           from_dt, till_dt,
           nds, currency,
           discount_type,
           -- ����������
           sum(amt_to_charge)               as amt_to_charge,
           sum(amt_to_charge*0.10)          as reward_to_charge,
           sum(amt_to_charge*0.03)          as delkredere_to_charge,
           sum(amt_to_charge*0.12)          as dkv_to_charge,
           -- ������
           sum(decode(commission_payback_type,
                1, null,
                2, amt_to_pay))             as amt_to_pay,
           sum(decode(commission_payback_type,
                1, null,
                2, amt_to_pay_w_nds))       as amt_to_pay_w_nds,
           sum(0.10*decode(commission_payback_type,
                1, amt_to_charge,
                2, amt_to_pay))             as reward_to_pay,
           sum(0.03*decode(commission_payback_type,
                1, amt_to_charge,
                2, amt_to_pay))             as delkredere_to_pay,
           sum(0.12*decode(commission_payback_type,
                1, amt_to_charge,
                2, amt_to_pay))             as dkv_to_pay
      from (
            select contract_id, contract_eid,
                   from_dt, till_dt,
                   payment_type, commission_payback_type,
                   discount_type, currency, nds,
                   amt          as amt_to_charge,
                   amt_w_nds    as amt_to_charge_w_nds,
                   0            as amt_to_pay,
                   0            as amt_to_pay_w_nds
              from s_acts_2012
             union all
            select contract_id, contract_eid,
                   from_dt, till_dt,
                   payment_type, commission_payback_type,
                   discount_type, currency, nds,
                   0            as amt_to_charge,
                   0            as amt_to_charge_w_nds,
                   amt          as amt_to_pay,
                   amt_w_nds    as amt_to_pay_w_nds
              from s_payments_2012
           )
     group by contract_eid, contract_id, from_dt, till_dt,
              discount_type, currency, nds
)
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
               1            as type
          from s_kv
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
          from s_kv_2012
       )
order by contract_id , from_dt, type,discount_type,currency, disc,turn_c,rew_c,del_c,dkv_c, turn_p_nds,turn_p,rew_p,del_p,dkv_p;

