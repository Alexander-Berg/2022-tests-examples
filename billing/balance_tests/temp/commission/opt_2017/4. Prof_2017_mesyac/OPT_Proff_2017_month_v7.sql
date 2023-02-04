with
counting_date as (select date'2017-04-03' as dt from dual)
--select * from counting_date;
,

s_base as (   
select distinct
           c.contract_eid      as contract_eid,
 		-- BALANCE-21757, BALANCE-21758
       -- BALANCE-22838
       -- ������������, ����, ������: ��� ��������� � ����� 1
       -- ���������� ��������� �������� �� ��.
       -- ����� �������� "�����" �� ������ ��������� (����� 1),
       -- ����� �������� � �� ����� ��������� (����� ������ ������).
       -- ��� �������� ����� ���������� external_id, �� ������ id.
       -- ����� �� ������� �������� ��������, ������� �������� �� ������ id,
       -- ��������� id ��� ������ ���������, ��������� id ����� ���������.
       case
        when c.contract_commission_type in (4, 10, 12, 13)
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
       end                                            as contract_id,
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
           case nvl(c.commission_type, c.discount_type)
            when 22 then 1
            when 29 then 1  -- ������������ ����� ����������� ��� �������
            else nvl(c.commission_type, c.discount_type)
           end                                      as discount_type,
		       nvl(c.commission_type, c.discount_type)  as discount_type_src,
           c.payment_type                             as payment_type, 
           decode(nvl(c.commission_type, c.discount_type),
             -- ������ ��� �������
              7,
          nvl((
            -- ������ �������� �� ���� "�� ������", �� ���� ������� �� ��
            -- �� �� ������� � ���.�������� �� ������� subclient_id
            -- subclient_id is     null: ��������� ��������� �����
            -- subclient_id is not null: ���������� ��������� �����
            select decode(p.value_num, null, 1, 0)
              from xxxx_invoice_repayment   ir
              join xxxx_extprops            p on p.object_id = ir.invoice_id
                                             and p.classname = 'PersonalAccount'
                                             and p.attrname = 'subclient_id'
             where ir.repayment_invoice_id = c.invoice_id 
               union
                select 1
                   from xxxx_invoice_repayment   ir              
                      where ir.repayment_invoice_id = c.invoice_id 
                     and not exists (
                     select 1 from xxxx_extprops p 
                      where p.object_id = ir.invoice_id
                     and p.classname = 'PersonalAccount'
                     and p.attrname = 'subclient_id')
        -- ���� ������ �� �������, �� �� �� ������, ������� �� ��������� ��.�.
        ), 0),
        0)                                      as is_agency_credit_line,
           c.contract_commission_type                 as commission_type
  from xxxx_new_comm_contract_basic c
  where (
                                                -- BALANCE-17175
                                                (
                                                     -- ������ �������/�����
                                                    c.contract_commission_type in (1, 2, 8) and
                                                    -- �� ��������� ��� � ��� 22
                                                    nvl(c.commission_type,
                                                        c.discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 17, 19, 22, 25, 28, 29, 36)
                                                )
                                                 or
                                                (
                                                    -- ������
                                                    c.contract_commission_type in (12, 13) and
                                                    nvl(c.commission_type, c.discount_type) in
                                                    (11)
                                                )
                                                or
                                                (
                                                    -- ������, �����������
                                                    c.contract_commission_type in (14) and
                                                    nvl(c.commission_type, c.discount_type) in
                                                    (33)
                                                )
                                                or
                                                
                                                (
                                                    -- �� ������������ ������� ��
                                                    c.contract_commission_type in (10, 16) and 1=1
                                                )
                                                or
                                                (
                                                    -- �� ������� ���������� ���,
                                                    -- ����� ���� � 15/16 ����.
                                                    -- ����������� ����� �����
                                                    -- �� ������ v_opt_2015_acts
                                                    -- BALANCE-23716
                                                    c.contract_commission_type in (6) and
                                                    nvl(c.commission_type, c.discount_type) in
                                                    (0, 1, 2, 3, 7, 8, 12, 15, 32)
                                                )
                                               or
                                                (
                                                    -- �����, ��� �����
                                                    nvl(c.commission_type, c.discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 17, 19, 28, 29)
                                                )
                                              )
)
--select * from s_base order by  invoice_id ;
,

-- ������� �������� ����������� ���������
s_attrs_src as (
    select key_num   as value_num,
           contract_id                                         as contract_id,
           code,
           start_dt                                                as from_dt,
           nvl(
                lead(start_dt) over(partition by code, contract_id
                                     order by stamp),
                add_months(trunc((select dt from counting_date), 'MM'), 11)
           ) - 1/24/60/60                                       as till_dt
      from xxxx_contract_signed_attr
)
--select  *from s_attrs_src;
,


s_changes_payment_type as (
    select *
      from (
            select s.contract_id, s.from_dt, s.till_dt, s.value_num
              from s_attrs_src s
             where s.code in ('PAYMENT_TYPE')
               and exists (select 1 from xxxx_new_comm_contract_basic d
                            where d.contract_id = s.contract_id)
          )
)

--select * from s_changes_payment_type;
,

s_payments_temp as (
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
           b.discount_type_src,
           b.is_agency_credit_line,
           oebs.comiss_date,
           trunc(oebs.comiss_date, 'MM')                 as from_dt,
           add_months(
            trunc(oebs.comiss_date, 'MM'), 1) -1/84600    as till_dt,
           oebs.oebs_payment*100/(100 + b.nds*b.nds_pct) as amt,
           oebs.oebs_payment                             as amt_w_nds,
       -- ����� �������� ����������� ������ �� �����
       -- � ��������������� �������
           sum(oebs.oebs_payment*100/(100 + b.nds*b.nds_pct))
            over(partition by b.invoice_id
                     order by oebs.comiss_date)        as amt_by_invoice,
            sum(oebs.oebs_payment)
            over(partition by b.invoice_id
                     order by oebs.comiss_date)        as amt_by_invoice_w_nds
      from s_base        b
      join xxxx_oebs_cash_payment_test     oebs on oebs.invoice_id = b.invoice_id
                                                and oebs.comiss_date >= date'2015-03-01'
                                                and oebs.comiss_date is not null
   where 
         -- base, prof
          (
            b.commission_type in (1, 2, 8)
        and (
               -- BALANCE-24516
                b.invoice_dt >= date'2017-03-01'        and
                b.discount_type in (1, 2, 3, 7, 12, 36)     and
                b.currency = 'RUR'
             or
              -- BALANCE-22085
                b.invoice_dt >= date'2016-03-01'        and
                b.invoice_dt  < date'2017-03-01'        and
                -- BALANCE-24734: ��������� 36 ���, ����� ������ ����� ������
                b.discount_type in (1, 2, 3, 7, 12, 25, 36) and
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
       or (b.commission_type in (10, 16) and 1=1)
          -- audio
       or (b.commission_type = 11 and b.discount_type_src = 29)
          -- media verticals
       or (b.commission_type = 17 and b.discount_type = 25)
       
       
)
--select * from s_payments_temp;

,
s_payments_src as 
(
select d."CONTRACT_EID",d."CONTRACT_ID",d."INVOICE_EID",d."INVOICE_ID",d."INVOICE_DT",d."CONTRACT_FROM_DT",
d."CONTRACT_TILL_DT",d."CURRENCY",d."NDS",d."PAYMENT_TYPE",d."COMMISSION_TYPE",d."DISCOUNT_TYPE",
d."DISCOUNT_TYPE_SRC",d."IS_AGENCY_CREDIT_LINE",d."COMISS_DATE",d."FROM_DT",d."TILL_DT",d."AMT",
d."AMT_W_NDS",d."AMT_BY_INVOICE",d."AMT_BY_INVOICE_W_NDS",
       (
        -- ���� ������� �������� (�� ����� ��� �� 1 ���� ����� ����� ������)
        -- ���� ������� ���������. �� ��������� ������ ����� (���������� �� �����
        --    ��������� ������) �������������� �� ��������� ������ �� �����������
        --    � �� �������������
            case
            when d.is_agency_credit_line = 1
            then (
                select case
                        when 
                        a.act_count = 1
                         and 
                         d.comiss_date <= a.payment_term_dt - 1
                         and 
                         --BALANCE-24853
                         a.act_amount = d.amt_by_invoice_w_nds
                        then 1
                        else 0
                       end
                  from (
                    -- �.�. ��, �� �� ����� �� ������, ������ ���� ����� 1 ���
                    -- ����� ����� � ���� ������ ���������
                    select min(a.ACT_PAYMENT_TERM_DT)   as payment_term_dt,
                           count(distinct a.id)     as act_count,
                           sum(a.amount)            as act_amount
                      from XXXX_NEW_COMM_CONTRACT_BASIC        a
                     where a.invoice_id = d.invoice_id
                       and a.hidden < 4
                       ) a
                 )
            else 0
            end
       )         as is_early_payment
  from s_payments_temp          d)
  
-- select * from s_payments_src; 
,
-- ----------------------------------------------------------------------------
-- �������� ������� �� �����
-- ----------------------------------------------------------------------------
-- ��� ���� �� ������
s_brands as (
        select /*+ materialize */
               atr.key_num          as client_id,
               min(c.MAIN_CLIENT_ID)     as main_client_id
          from xxxx_contract_signed_attr    atr
          join xxxx_ui_contract_apex        c on c.contract_id = atr.contract_id
                                               -- ������ ����� ���.������
                                              and (c.finish_dt is null
                                                or c.finish_dt >= trunc((select dt from counting_date), 'MM') - 1)
         where atr.code = 'BRAND_CLIENTS'
         group by atr.key_num
)

--select  *from s_brands;
,

s_acts_temp as (
    select 
    b.contract_eid,
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
           b.discount_type_src,
           b.is_agency_credit_line,
           case
           when nvl(xxx.is_loyal, 0) = 1 and b.discount_type = 7
            then 1
            else 0
           end                                               as is_loyal,
           xxx.client_id                                      as client_id,
           nvl(brand.main_client_id, xxx.client_id)             as brand_id,
           xxx.act_id                                             as act_id,
           xxx.act_eid                                            as act_eid,
           xxx.act_dt                                         as act_dt,
           trunc(xxx.act_dt, 'MM')                            as from_dt,
           add_months(trunc(xxx.act_dt, 'MM'), 1) - 1/84600   as till_dt,
       -- ���������, ��� �� �������� ������ ���� -- 1 ������ � 1 ���
            count(distinct b.nds)      over (partition by b.contract_id) as nds_count,
           count(distinct b.currency) over (partition by b.contract_id) as currency_count,
           xxx.amount                                         as amt_w_nds,
           xxx.amount-xxx.amount_nds-xxx.amount_nsp           as amt,
           xxx.amount*cr.rate                                 as amt_w_nds_rub,
           (xxx.amount-xxx.amount_nds-xxx.amount_nsp)*cr.rate as amt_rub
      from s_base        b
      join xxxx_new_comm_contract_basic     xxx on b.invoice_id = xxx.invoice_id
                                              and xxx.hidden <4
                                              -- BALANCE-24627: ����� ������ ���� �� �����
                                              and xxx.act_dt >= date'2015-03-01'
                                           -- BALANCE-24798: ��������� ��
                                              and ( xxx.is_loyal = 0 and xxx.act_dt >= date'2017-03-01'
                                                or xxx.act_dt < date'2017-03-01')

                                             
                                             
      join xxxx_currency_rate              cr on cr.cc = b.currency
                                              and cr.rate_dt = trunc(xxx.act_dt)
      left outer
      join s_brands                               brand on brand.client_id = xxx.client_id
     where b.commission_type in (1, 2, 3, 4, 5, 6, 8, 10, 11, 12, 13, 16, 17)
   and (
          -- base, prof
          (
            b.commission_type in (1, 2, 8)
        and (
              
                -- BALANCE-24516: ����� ������� ��������� ������ ����� ������
                -- ���.���� ��� ��������, ��� ������� �������
                xxx.act_dt >= date'2017-03-01' and b.contract_till_dt > date'2017-03-01' and b.discount_type in (1, 2, 3, 7, 12,36)
                -- BALANCE-22085
                -- � ����� �� ����� � ���������� ��������� 2016:
                --  * ����������� ������ (11)
                --  * �������� ���� (25)
                --2016 ���
             or  xxx.act_dt >= date'2017-03-01' and b.contract_till_dt  <= date'2017-03-01' and b.discount_type in (1, 2, 3, 7, 12, 25, 36)
             -- BALANCE-24734: ��������� 36 ���, ����� ������ ����� ������
             or  xxx.act_dt >= date'2016-03-01' and xxx.act_dt < date'2017-03-01'   and b.discount_type in (1, 2, 3, 7, 12, 25)
             -- 2015 ���
             or  xxx.act_dt >= date'2016-03-01' and b.contract_till_dt  <= date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
             or  xxx.act_dt  < date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
            )
          )
          -- ua
       or (b.commission_type = 6 and(
                   -- BALANCE-22914
                   xxx.act_dt >= date'2016-04-01'
               and b.discount_type in (0, 1, 8, 7, 15)
                or  xxx.act_dt <  date'2016-04-01'
               and b.discount_type in (1, 2, 3, 7, 12)
                ))
          -- spec
       or (b.commission_type = 3 and b.discount_type in (17))
          -- auto
       or (b.commission_type = 4 and b.discount_type in (19))
          -- sprav
       or (b.commission_type = 5 and b.discount_type = 12)
          -- estate
       or (b.commission_type in (10, 16) and 1=1)
          -- audio
       or (b.commission_type = 11 and b.discount_type_src = 29)
          -- market
       or (b.commission_type in (12, 13) and b.discount_type_src = 11)
          -- media verticals
       or (b.commission_type = 17 and b.discount_type = 25)
       )
)
--select  *from s_acts_temp;
,

s_acts_src as ( select * from s_acts_temp
 where (
            (
            -- BALANCE-22203
            -- BALANCE-22331
            -- BALANCE-23542
            -- BALANCE-22578
            -- � ����� �������� ��������� ������ ���� ������
            -- BALANCE-24627: ����������� (from_dt -> act_dt)
                act_dt >= date'2016-03-01'
            and currency_count = 1
            and nds_count = 1
            )
            or
            (
                act_dt < date'2016-03-01'
            )
        )
           -- BALANCE-24852
       and (
                currency = 'RUR'
            or  commission_type = 6
          )
)
--select  *from s_acts_src;
,
-- ���� ��� �������� ��������
-- ��� ���� �� ������
s_acts as (
    select b.*
      from s_acts_src   b
     where b.commission_type = 2
)
--select  *from s_acts;
,
s_payments as (
    select b.*
      from s_payments_src b
     where b.commission_type = 2
)

--select * from s_payments;

,

-- ----------------------------------------------------------------------------
-- ������� �� (��������)
-- ----------------------------------------------------------------------------
-- ���������� ���� � ������ �� ������� �����
s_kv_src as (
    select d.*,
           case
                -- BALANCE-22195: ������ ����� ������� ��������� ��� �������
                -- "� ������������" �� ���� ����. ����� �������, 2017
                when d.from_dt >= date'2017-03-01' and
                     -- BALANCE-22330: ������ ��� ����� � ����������������
                     d.contract_till_dt > date'2017-03-01'
                then 1
                else 0
            end                     as is_2017
      from (
          select d.contract_eid, d.contract_id,
           d.currency, d.discount_type, discount_type_src,
           contract_from_dt, contract_till_dt,
           d.nds, from_dt, till_dt, payment_type,
           is_agency_credit_line,
           sum(amt_acts)            as amt_to_charge,
           sum(amt_oebs_w_nds)      as amt_to_pay_w_nds,
           -- BALANCE-25224: ������ �� ����� ��� ������� �� ���������.������
           --                � ���������.��������� (������ ��� ����������)
           nvl(sum(case when contract_till_dt > date'2017-03-01'
                         and d.discount_type in (1, 2, 3)
                         and payment_type = 3
                         and i.invoice_type = 'prepayment'
                    then amt_acts
                    else 0
                end), 0)             as amt_to_pay_2016_media_prep,
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
                          -- BALANCE-25224: ��������� ������ �� ���������.������
                          -- ���������.��������� �� ������� �� ���������� ���.
                         and not (
                                d.discount_type in (1, 2, 3)
                            and payment_type = 3
                            and contract_till_dt > date'2017-03-01'
                            and i.invoice_type = 'prepayment'
                             )
                    then amt_oebs
                    else 0
               end), 0)             as amt_to_pay_2016,
           nvl(sum(case when invoice_dt >= date'2017-03-01'
                          -- BALANCE-22330: ������ ��� ���������� ���������
                         and contract_till_dt > date'2017-03-01'
                          -- BALANCE-25224: ��������� ������ �� ���������.������
                          -- ���������.��������� �� �������
                         and not (
                                d.discount_type in (1, 2, 3)
                            and payment_type = 3
                            and i.invoice_type = 'prepayment'
                             )
                    then amt_oebs
                    else 0
               end), 0)             as amt_to_pay_2017,
           sum(amt_oebs)            as amt_to_pay
      from (
            select contract_eid, contract_id, currency, nds, discount_type,
                   contract_from_dt, contract_till_dt, discount_type_src,
                   from_dt, till_dt, payment_type, invoice_dt, invoice_id,
                   is_agency_credit_line,
                   amt  as amt_acts, amt_w_nds  as amt_acts_w_nds,
                   0    as amt_oebs, 0          as amt_oebs_w_nds
              from s_acts
             where act_dt between add_months(trunc((select dt from counting_date), 'MM'), -1)
                              and trunc((select dt from counting_date), 'MM') - 1/24/60/60
             union all
            select contract_eid, contract_id, currency, nds, discount_type,
                   contract_from_dt, contract_till_dt, discount_type_src,
                   from_dt, till_dt, payment_type, invoice_dt, invoice_id,
                   is_agency_credit_line,
                   0    as amt_acts, 0          as amt_acts_w_nds,
                   amt  as amt_oebs, amt_w_nds  as amt_oebs_w_nds
              from s_payments
             where comiss_date between add_months(trunc((select dt from counting_date), 'MM'), -1)
                                and trunc((select dt from counting_date), 'MM') - 1/24/60/60
           )                        d
      join xxxx_invoice i  on i.inv_id = invoice_id
     group by contract_eid, d.contract_id, d.currency, payment_type,
              contract_from_dt, contract_till_dt,
              is_agency_credit_line, discount_type_src,
              d.nds, d.discount_type, from_dt, till_dt
           ) d
)
--select * From s_kv_src order by discount_type;
--
,
-- ���������� � �������� (������� ������� ����������������� ��������)
s_kv_control_src as (
    select d.*,
           -- �������, �� ������� �� ������� ����� ����� �� ��������
           -- ������ ��������� �� ����� 1000 ���. ����������� ������ ����������
           -- ClientId ��������� ����� ��������.
           case
            when amt_rub >= 1000 then brand_id
            else null
           end                                              as over1k_brand_id,
           -- ����������� ������� �� ������� �� ������� �
           -- ������� �� �������� (���������)
           nvl(ratio_to_report(amt_rub_direct)
              over (partition by contract_id, from_dt), 0) as ratio
      from (
        select contract_id, from_dt, till_dt, brand_id, client_count_2016,
               contract_from_dt, contract_till_dt,commission_type,
               sum(amt_rub)                                  as amt_rub,
               sum(decode(discount_type, 7, amt_rub, null))  as amt_rub_direct
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
                  contract_from_dt, contract_till_dt, commission_type
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
            select contract_id, from_dt, till_dt, client_count_2016,
                   contract_from_dt, contract_till_dt,commission_type,
                   sum(amt_rub)                 as amt_rub,
                   count(distinct over1k_brand_id)  as client_count,
                   round(max(ratio), 2)         as max_client_ratio_by_direct
              from s_kv_control_src
             group by contract_id, from_dt, till_dt, client_count_2016,
                      contract_from_dt, contract_till_dt, commission_type
               ) d
           ) d
)
--select  *From s_kv_control_pre;
,
-- ������
--  - ������ �� �������� >= 200k
--  - �������� >= 5
--  - ��� �������� � �������� > 70% (����������� �� ����� ���������) �� �������
s_kv_control as (
    select d.*,
           case
           when amt_rub >= 200000
             and (
                    (client_count >= 5 and contract_till_dt > date'2017-03-01')
                 or (client_count_2016 >= 5 and contract_till_dt <= date'2017-03-01')
                )
            and (
                    is_there_boc = 0
                 or is_there_boc_1m_ago = 0
                )
           then 0
           else 1
            end as failed
      from s_kv_control_pre d
)
--select  *from s_kv_control;

--select  * from s_kv_src;
,
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
           sum(d.reward_to_pay)             as reward_to_pay
     from (
    select d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           d.payment_type,
           d.nds, d.currency,
           -- BALANCE-25071
           case
            when d.is_2017 = 0 and d.discount_type = 36 then 1
            else d.discount_type
           end                          as discount_type,
           sum(d.amt_to_charge)         as turnover_to_charge,
           sum(case
                when d.is_2017 = 1 then
                    d.amt_to_charge*decode(f.failed, 1, 0,
                                        case
                                        when d.discount_type in (36)  then 0.15
                                        when d.discount_type in (1, 2, 3) then 0.13
                                        when d.discount_type in (7) then
                                            case
                                                -- ��� ��������� �� ����������
                                                -- ��� ����� �� ��������� ���������
                                                -- ����� � 7%
                                                when payment_type = 3
                                                 and is_agency_credit_line = 1
                                                then 0.07
                                                -- ��������� � 8%
                                                else 0.08
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
           sum(d.amt_to_pay_2016*decode(d.discount_type,
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
--                -- BALANCE-25224
                d.amt_to_pay_2016_media_prep*0.13
                +
                d.amt_to_pay_2017* case
                                    when d.discount_type in (36) then 0.15
                                    when d.discount_type in (1, 2, 3) then 0.13
                                    when d.discount_type in (7) then
                                        case
                                            -- ��� ��������� �� ����������
                                            -- ��� ����� �� ��������� ���������
                                            -- ����� � 7%
                                            when payment_type = 3
                                             and is_agency_credit_line = 1
                                            then 0.07
                                            -- ��������� � 8%
                                            else 0.08
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
              d.discount_type, d.currency, d.nds, d.is_2017
     union all
    -- ��-����������� 1% �� �������
    select d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           3                        as payment_type,
           d.nds, d.currency,
           7                        as discount_type,
           0                        as turnover_to_charge,
           d.amt_by_invoice*0.01    as reward_to_charge,
           0                        as turnover_to_pay,
           0                        as turnover_to_pay_w_nds,
           d.amt_by_invoice*0.01    as reward_to_pay
      from s_payments_src       d
        -- �������������� ������� ��� ���������� �������������� �� ���������
        -- ������: � �������� �������, � ������� ��� ��������� ����, ���������
        -- ��������� ������� ������� ������� ������ ��� �������. ���� �������
        -- ������� ������� ������ �� �����������, �������������� �� ���������
        -- ������ ����� �� ����������� � �� �������������
      join s_kv_control                 f on f.contract_id = d.contract_id
                                         and d.invoice_dt between f.from_dt
                                                              and f.till_dt
                                                              --BALANCE-24868
                                                              and f.failed = 0
     where d.is_early_payment = 1
       and d.comiss_date between add_months(trunc((select dt from counting_date), 'MM'), -1)
                          and trunc((select dt from counting_date), 'MM') - 1/24/60/60
        -- BALANCE-25161: ������ �� ����� ������
       and d.invoice_dt >= date'2017-03-01'
           )  d
     group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt, d.payment_type,
              d.discount_type, d.currency, d.nds
)
--select  * from s_kv_pre; 


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
--select  *from s_kv01;
-- select  * from s_changes_payment_type ;
,
s_kv10_src as (
    select d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           d.nds, d.currency,
           d.turnover_to_charge,
           d.reward_to_charge,
           d.turnover_to_pay,
           d.turnover_to_pay_w_nds,
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
          from (
            -- BALANCE-25224: ������� ���� ����������
            select d.contract_id, d.contract_eid, d.reward_type,
                   d.from_dt, d.till_dt,
                   d.nds, d.currency,
                   d.turnover_to_charge,
                   d.reward_to_charge,
                   d.turnover_to_pay,
                   d.turnover_to_pay_w_nds,
                   d.reward_to_pay,
                   d.reward_to_pay_src
              from xxxx_commission_reward_2013 d
             union all
            select d.contract_id, contract_eid, d.reward_type,
                   d.from_dt, d.till_dt,
                   d.nds, d.currency,
                   d.turnover_to_charge,
                   d.reward_to_charge,
                   d.turnover_to_pay,
                   d.turnover_to_pay_w_nds,
                   d.reward_to_pay,
                   d.reward_to_pay_src
              from xxxx_commission_reward_2013_ d
              ) d
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
           and d.from_dt < add_months(trunc((select dt from counting_date), 'MM'), -1)
           ) d
  group by d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           d.nds, d.currency
)
  
--select * from s_kv10_src;

-- �� � ���������, ��� ����� �� �����, ��� �����
-- ��� �������� �� ����� �������
, s_kv10 as (
    select contract_eid, contract_id, from_dt, till_dt,
           currency, nds,
           -- � ������������ (��. s_kv01)
           0                                    as turnover_to_charge,
           0                                    as reward_to_charge,
--           reward_to_charge_sum,
--           reward_to_pay_sum,
           -- � ����������
           turnover_to_pay,
           turnover_to_pay_w_nds,
           reward_to_pay_src,
           (least(reward_to_charge_sum, reward_to_pay_sum) -
                least(reward_to_charge_sum_prev, reward_to_pay_sum_prev)
           )                                    as reward_to_pay
      from (
            select d.*,
                   reward_to_pay                        as reward_to_pay_src,
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
                    -- ����������� �����
                select d.contract_id, contract_eid,
                       d.from_dt, d.till_dt,
                       d.nds, d.currency,
                       sum(turnover_to_charge)      as turnover_to_charge,
                       sum(reward_to_charge)        as reward_to_charge,
                       sum(turnover_to_pay)         as turnover_to_pay,
                       sum(turnover_to_pay_w_nds)   as turnover_to_pay_w_nds,
                       sum(reward_to_pay)           as reward_to_pay
                  from s_kv10_src         d
                 group by d.contract_eid, d.contract_id,
                          d.from_dt, d.till_dt,
                          d.currency, d.nds
                   ) d
           ) s
        -- ���������� ������ ���������� �����
     where from_dt between add_months(trunc((select dt from counting_date), 'MM'), -1)
                       and trunc((select dt from counting_date), 'MM') - 1/24/60/60
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
       )
--where reward_to_charge<>0 or reward_to_pay<>0

  order by contract_id, from_dt, discount_type, currency, nds;
