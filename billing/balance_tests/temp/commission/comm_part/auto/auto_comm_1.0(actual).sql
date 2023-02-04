
with 
--��� ��������
 s_comm_type as (
    select 56 as id from dual
),
s_base_src as (  
   select  distinct 
           xxxx.invoice_id                               as invoice_id,
            decode(
            nvl(xxxx.comm_type, xxxx.discount_type),
            22, 1,
            nvl(xxxx.comm_type, xxxx.discount_type)
           )                                             as discount_type,
         decode(xxxx.endbuyer_id, 0, null, xxxx.endbuyer_id)		as endbuyer_id
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
           x.invoice_id                               as invoice_id,
           x.invoice_dt                               as invoice_dt,
           x.act_dt                                   as act_dt,  
           x.discount_type    					              as discount_type,
           x.commission_type                          as comm_type,    
  	       decode(x.endbuyer_id, 0, null, x.endbuyer_id)	as endbuyer_id,
           x.endbuyer_inn                              as endbuyer_inn,
           x.agency_inn                              as agency_inn
       from xxxx_new_comm_contract_basic  x
       )   xxxx                 
      where xxxx.discount_type in (19)
)

--select * from s_base_src;
,                   
s_base_acts as (select b.contract_eid,
           b.contract_id,
           b.currency,
           b.nds,
           b.discount_type,
           b.payment_type,
           b.commission_payback_type                as payback_type,
           trunc(b.act_dt, 'MM')                        as from_dt,
           add_months(
            trunc(b.act_dt, 'MM'), 1) - 1/84600         as till_dt,
           b.amount                                as amt_w_nds,
           b.amount-b.amount_nds-b.amount_nsp    as amt
      from xxxx_new_comm_contract_basic         b
      join s_base_src xxx on b.invoice_id = xxx.invoice_id
     where b.hidden < 4
     and b.contract_commission_type = (select id from s_comm_type)
 		-- BALANCE-19159
        -- ������� � 01.01.2015 ��������� ����, �� ������ �������
        -- �� ������ �������� ����������
       and (
            -- ������� � 2015, ��������� ������� �����
            -- � ��������� ������ �� �����, � ������� ���� �������� ����������
            (
                b.act_dt >= date'2015-01-01'
            -- BALANCE-19429
            and xxx.is_opt = 0
            )
            -- �� 2015 ���� ������ �� ������
            or b.act_dt < date'2015-01-01'
           )
),
-- 
-- �������� ������� �� �������
--
s_payments as (
    select b.contract_eid,
           b.contract_id,
           b.currency,
           b.nds,
           b.payment_type,
           b.discount_type,
           b.commission_payback_type                    as payback_type,
           trunc(oebs.comiss_date, 'MM')                as from_dt,
           add_months(
            trunc(oebs.comiss_date, 'MM'), 1) - 1/84600 as till_dt,
           oebs.oebs_payment                                     as amt_w_nds,
           oebs.oebs_payment*100/(100 + b.nds*b.nds_pct)         as amt
      from xxxx_new_comm_contract_basic         b
      join s_base_src xxx on b.invoice_id = xxx.invoice_id
      join xxxx_oebs_cash_payment_test     oebs on oebs.invoice_id = b.invoice_id
                                                and oebs.comiss_date is not null
      and b.contract_commission_type = (select id from s_comm_type) 
		-- BALANCE-19159
        -- ������� � 01.01.2015 ��������� ����, �� ������ �������
        -- �� ������ �������� ����������
       and (
            -- ������� � 2015, ��������� ������� �����
            -- � ��������� ������ �� �����, � ������� ���� �������� ����������
            (
                oebs.comiss_date >= date'2015-01-01'
            -- BALANCE-19429
            and xxx.is_opt = 0
            -- BALANCE-19449: ���� ������ ������ �� ������ �� 2015 ����,
            -- �� �������� �� ��������� ���������� �� ����.
            and xxx.is_there_old_acts = 0
            )
            -- BALANCE-19449: ���� ������ ����� 2015 � ����
            -- ���� �� �� ����� ����� �� 2015, �� ����� ������ ���� ������
            or
            (
                oebs.comiss_date >= date'2015-01-01'
            and xxx.is_there_old_acts != 0
            )
            -- �� 2015 ���� ������ �� ������
            or oebs.comiss_date < date'2015-01-01'
           )
),
--
 -- �������� ������� �� �������/�����
 -- (����� ������� ��������� ���� �����,
 -- ����� ����� ���, �� ���� ������)
--
  s_base as (
   select contract_eid, contract_id, currency, discount_type,
         nds, from_dt, till_dt,
        sum(amt_acts) as amt_to_charge,
        sum(decode(payment_type,
              -- ����������: ������ �� ������������ (�����)
                2, amt_acts_w_nds,
                  -- ����������: ������ ��� �� ������� ���
                 -- ������������ (�� ������� � ��������)
                 3, decode(payback_type,
                        1, amt_acts_w_nds,
                        2, amt_oebs_w_nds,
                        0),
                0))           as amt_to_pay_w_nds,
            sum(decode(payment_type,
                -- ����������: ������ �� ������������ (�����)
                2, amt_acts,
                -- ����������: ������ ��� �� ������� ���
                -- ������������ (�� ������� � ��������)
               3, decode(payback_type,
                       1, amt_acts,
                        2, amt_oebs,
                         0),
                 0))           as amt_to_pay
      from (
             select contract_eid, contract_id, currency, nds, discount_type,
                    payment_type, payback_type, from_dt, till_dt,
                    amt  as amt_acts, amt_w_nds  as amt_acts_w_nds,
                    0    as amt_oebs, 0          as amt_oebs_w_nds
              from s_base_acts
              union all
             select contract_eid, contract_id, currency, nds, discount_type,
                    payment_type, payback_type, from_dt, till_dt,
                  0    as amt_acts, 0          as amt_acts_w_nds,
                    amt  as amt_oebs, amt_w_nds  as amt_oebs_w_nds
               from s_payments
            )
     group by contract_eid, contract_id, currency,
              nds, discount_type, from_dt, till_dt
 )
-- select * from s_base order by contract_id;
--     
 -- �������������� ������
 --
 select contract_id,
       contract_eid,
        from_dt,
        till_dt,
       nds,
        currency,
        1                    as reward_type,
       discount_type,
        amt_to_charge        as turnover_to_charge,      -- ������ � ����������
        amt_to_charge*.08    as reward_to_charge,        -- � ����������
        amt_to_charge*.02    as delkredere_to_charge,     -- � ���������� (�����������)
        null                 as dkv_to_charge,
        amt_to_pay_w_nds     as turnover_to_pay_w_nds,   -- ������ � ������������ (� ���)
        amt_to_pay           as turnover_to_pay,         -- ������ � ������������
       amt_to_pay*.08       as reward_to_pay,            -- � ������������
       amt_to_pay*.02       as delkredere_to_pay,       -- � ������������ (�����������)
       null                 as dkv_to_pay

   from s_base
   order by contract_id,discount_type,turnover_to_charge,reward_to_charge,turnover_to_pay_w_nds;
   
   
--select * from xxxx_commission_part;