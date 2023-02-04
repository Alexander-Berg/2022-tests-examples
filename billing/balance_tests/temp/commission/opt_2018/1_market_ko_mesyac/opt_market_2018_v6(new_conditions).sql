with
counting_date as (select date'2018-10-03' as dt from dual),


  s_cltrl as (
      select d.*, 
             start_dt                                                 as from_dt,
             nvl(
                lead(start_dt) over(partition by code, contract_id
                                     order by collateral_id),
                add_months(trunc((select dt from counting_date), 'MM'), 3)
           ) - 1/24/60/60                                       as till_dt
      from (
        select distinct contract_id, collateral_id, start_dt , code, stamp
          from xxxx_contract_signed_attr
           ) d
)
--select  *from s_cltrl;
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
		   c.contract_from_dt                         as contract_from_dt,
           c.contract_till_dt                         as contract_till_dt,
           c.invoice_eid                              as invoice_eid,
           c.invoice_id                               as invoice_id,
           c.invoice_dt                               as invoice_dt,
		   i.invoice_type                             as invoice_type,
           i.total_sum                                as total_sum, 
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
           case
           -- ������ ��� �������/������� � �������
           when nvl(c.commission_type, c.discount_type) in (7, 37) then
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
        ), 0)
        else 0
        end                                   as is_agency_credit_line,
           c.contract_commission_type                 as commission_type
  from xxxx_new_comm_contract_basic c
  left join XXXX_INVOICE i on c.invoice_id = i.inv_id
  where (
                                                -- BALANCE-17175
                                                (
                                                     -- ������ �������/�����
                                                    c.contract_commission_type in (1, 2, 8, 21) and
                                                    -- �� ��������� ��� � ��� 22
                                                    nvl(c.commission_type,
                                                        c.discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 17, 19, 22, 25, 28, 29, 36, 37)
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
                                                    (0, 1, 2, 3, 7, 8, 12, 15, 36)
                                                )
                                               or
                                                (
                                                    -- ������� ��������� � �������-����.��
                                                    c.contract_commission_type in (17) and
                                                nvl(c.commission_type, c.discount_type) in (25)
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
    select value_num, value_str,
           d.contract_id                                         as contract_id,
           d.code,
           d.key_num                                              as client_id,
           c.from_dt,
           c.till_dt
      from xxxx_contract_signed_attr d
      join s_cltrl                          c on c.collateral_id = d.collateral_id
                                             and c.contract_id = d.contract_id
                                             and c.code = d.code
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

--select  *from s_changes_payment_type;
,
s_payments_temp as (
    select b.contract_eid,
           b.contract_id,
           b.invoice_eid,
           b.invoice_id,
           b.invoice_dt,
           b.invoice_type,
           b.contract_from_dt,
           b.contract_till_dt,
           b.currency,
           b.nds,
           b.payment_type,
           b.commission_type,
           b.discount_type,
           b.discount_type_src,
           b.is_agency_credit_line,
           b.total_sum*100/(100 + b.nds*b.nds_pct)      as invoice_total_sum,
           b.total_sum                                  as invoice_total_sum_w_nds,
           oebs.comiss_date,
           oebs.dt                                      as oebs_dt,
           oebs.payment_number,                          
           trunc(oebs.comiss_date, 'MM')                 as from_dt,
           add_months(
            trunc(oebs.comiss_date, 'MM'), 1) -1/84600    as till_dt,
           oebs.oebs_payment*100/(100 + b.nds*b.nds_pct) as amt,
           oebs.oebs_payment                             as amt_w_nds,
       -- ����� �������� ����������� ������ �� �����
       -- � ��������������� �������
-- (������������ ��� ������� ������ � ������ ��������������)
           sum(oebs.oebs_payment*100/(100 + b.nds*b.nds_pct))
            over(partition by b.invoice_id
                     order by oebs.comiss_date,oebs.dt,oebs.payment_number)        as amt_by_invoice,
   -- (������������ ��� ����������� ������ ������, ������ ������ �������
         -- �������������)            
            sum(oebs.oebs_payment)
            over(partition by b.invoice_id
                     order by oebs.comiss_date, oebs.dt,oebs.payment_number)       as  payments_curr_by_invoice_w_nds
      from s_base        b
      join xxxx_oebs_cash_payment_test     oebs on oebs.invoice_id = b.invoice_id
                                                and oebs.comiss_date >= date'2015-03-01'
                                                and oebs.comiss_date is not null
   where 
         -- base, prof
          (
            b.commission_type in (1, 2, 8, 21)
        and (
               -- BALANCE-24516
                b.invoice_dt >= date'2017-03-01'        and
                b.discount_type in (1, 2, 3, 7, 12, 36, 37)     and
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
             or b.invoice_dt  < date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12, 36)
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
          -- market, BALANCE-27251, BALANCE-26854
       or (b.commission_type in (12, 13) and b.discount_type_src = 11)
          
)
--select * from s_payments_temp;

,

 s_early_payment_pre as 
(
select d.*,
-- ��������� ���������� ����
       case
        when payments_curr_by_invoice_w_nds >= invoice_total_sum_w_nds
        then 1
        else 0
       end               as is_fully_paid_pre,
       (
        -- ���� ������� �������� (�� ����� ��� �� 1 ���� ����� ����� ������)
        -- ���� ������� ���������. �� ��������� ������ ����� (���������� �� �����
        --    ��������� ������) �������������� �� ��������� ������ �� �����������
        --    � �� �������������
            case
            when d.is_agency_credit_line = 1
            then (
                select case
                       when a.act_count = 1
                         and d.comiss_date <= a.payment_term_dt - 1
                          -- BALANCE-24851
                         and a.act_amount = d.payments_curr_by_invoice_w_nds
                        then 1
                        else 0
                       end
                  from (
                    -- �.�. ��, �� �� ����� �� ������, ������ ���� ����� 1 ���
                    -- ����� ����� � ���� ������ ���������
                    select min(a.ACT_PAYMENT_TERM_DT)   as payment_term_dt,
                           count(distinct a.act_id)     as act_count,
                           sum(a.amount)            as act_amount
                      from XXXX_NEW_COMM_CONTRACT_BASIC        a
                     where a.invoice_id = d.invoice_id
                       and a.hidden < 4
                       ) a
                 )
            else 0
            end
       )       as is_early_payment_pre
  from s_payments_temp          d
  where not exists (
            select 1
              from XXXX_NEW_COMM_CONTRACT_BASIC a
             where a.is_loyal = 1
               and a.invoice_id = d.invoice_id
               and a.hidden < 4
               and a.act_dt >= date'2017-03-01'
               and d.invoice_dt >= date'2017-03-01' 
)
)
-- select * from s_early_payment_pre; 
,
 s_early_payment_counted as (
select d.*,
-- BALANCE-27396: ������� �� �������� 2018 ���� ����� ��������� ������ ���

       -- ������, ��������� ����� 2018-03
       case
        when from_dt >= date'2018-03-01'
         and invoice_dt >= date'2018-03-01'
        then 1
        else 0
       end                                          as is_2018,
       count(decode(is_fully_paid_pre, 1, 1, null)) over(partition by invoice_id
                             order by comiss_date, oebs_dt, payment_number) as
                        fully_paid_pos,
       count(decode(is_early_payment_pre, 1, 1, null)) over(partition by invoice_id
                             order by comiss_date, oebs_dt, payment_number) as
                        early_payment_pos
  from s_early_payment_pre          d
)

--select *from s_early_payment_counted;
,
s_payments_src as (
select d."CONTRACT_EID",d."CONTRACT_ID",d."INVOICE_EID",d."INVOICE_ID",d."INVOICE_DT",d."INVOICE_TYPE",d."CONTRACT_FROM_DT",d."CONTRACT_TILL_DT",d."CURRENCY",d."NDS",d."PAYMENT_TYPE",d."COMMISSION_TYPE",d."DISCOUNT_TYPE",d."DISCOUNT_TYPE_SRC",d."IS_AGENCY_CREDIT_LINE",d."INVOICE_TOTAL_SUM",d."INVOICE_TOTAL_SUM_W_NDS",d."COMISS_DATE",d."OEBS_DT",d."PAYMENT_NUMBER",d."FROM_DT",d."TILL_DT",d."AMT",d."AMT_W_NDS",d."AMT_BY_INVOICE",d."PAYMENTS_CURR_BY_INVOICE_W_NDS",d."IS_FULLY_PAID_PRE",d."IS_EARLY_PAYMENT_PRE",d."IS_2018",d."FULLY_PAID_POS",d."EARLY_PAYMENT_POS",
 -- BALANCE-27395/27396/27397: ������� � 2018-03 ������ ������ ��
       -- ��������� ���������� ������, � �� �� ������ ������
       case
        when is_2018 = 1
        then invoice_total_sum_w_nds
        else amt_w_nds
       end                                                   as amt_ttl_w_nds,
       case
        when is_2018 = 1
        then invoice_total_sum
        else amt
       end                                                   as amt_ttl,
       -- BALANCE-28622: ���-�� �������� � ����� ������� � ������� �������
       count(1)
        over(partition by contract_id, from_dt, payment_number)                    as payment_count_by_contract,
       count(1)
        over(partition by contract_id, from_dt,
                         invoice_id, payment_number)        as payment_count_by_invoice,
       -- BALANCE-26692: ���������� ������ ������ ��������� ��������
 decode(fully_paid_pos, 1, is_fully_paid_pre, 0)       as is_fully_paid,
       decode(early_payment_pos, 1, is_early_payment_pre, 0) as is_early_payment
  from s_early_payment_counted d
  )
  
--select * from s_payments_src;
,
s_brands as (
        select /*+ materialize */
               atr.key_num          as client_id,
               atr.start_dt         as collateral_dt,
               c.start_dt           as dt,
               trunc(nvl(c.finish_dt, date'2018-04-03'))
                -- BALANCE-28403: ��������� ������� ��� � ���������
                + 1 - 1/24/60/60            as finish_dt,
               min(c.MAIN_CLIENT_ID)     as main_client_id
          from xxxx_contract_signed_attr    atr
          -- BALANCE-27379: ������� � ���.������ �������� ���������, �����
            -- �������� ����������� ������, � ���.�� ��� ��������
          join xxxx_ui_contract_apex        c on c.contract_id = atr.contract_id
                                               -- ������ ����� ���.������
--                                              and (c.finish_dt is null
--                                                or c.finish_dt >= trunc((select dt from counting_date), 'MM') - 1)
                                                -- �� ����� ���� ������
--                                             and d.end_prev_month between c.start_dt
--                                               and nvl(c.finish_dt, (select dt from counting_date))
                                                
         where atr.code = 'BRAND_CLIENTS'
         group by atr.key_num, atr.start_dt, c.start_dt, nvl(c.finish_dt, date'2018-04-03')
)

--select  *from s_brands;

--select * from s_payments;
,
s_ar_acts_src as (
    select 
    b.contract_eid,
           b.contract_id,
           b.invoice_eid,
           b.invoice_id,
           b.invoice_dt,
	       b.invoice_type,
           b.contract_from_dt,
           b.contract_till_dt,
           b.currency,
           b.nds,
           b.payment_type,
           b.commission_type,
		   -- BALANCE-26651,  BALANCE-17175
           nvl(
             case xxx.commission_type 
            when 22 then 1
            when 29 then 1  -- ������������ == �������
            else xxx.commission_type
           end,
           b.discount_type)           as discount_type,
		   -- BALANCE-26651
           nvl(xxx.commission_type, b.discount_type_src)     as discount_type_src,
           b.is_agency_credit_line,
           case
           when nvl(xxx.is_loyal, 0) = 1 and b.discount_type = 7
            then 1
            else 0
           end                                               as is_loyal,
           xxx.client_id                                      as client_id,
         --  nvl(brand.main_client_id, xxx.client_id)             as brand_id,
           xxx.act_id                                             as act_id,
           xxx.act_eid                                            as act_eid,
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
                                              -- BALANCE-24627: ����� ������ ���� �� �����
                                              and xxx.act_dt >= date'2015-03-01'
                                           -- BALANCE-24798: ��������� ��
                                          and ( xxx.is_loyal = 0 and xxx.act_dt >= date'2017-03-01'
                                                or xxx.act_dt < date'2017-03-01')

      join xxxx_currency_rate              cr on cr.cc = b.currency
                                              and cr.rate_dt = trunc(xxx.act_dt)
--      left outer
--      join s_brands                               brand on brand.client_id = xxx.client_id
     where b.commission_type in (1, 2, 3, 4, 5, 6, 8, 10, 11, 12, 13, 16, 17, 19, 21)
   and (
          -- base, prof
          (
            b.commission_type in (1, 2, 8, 21)
        and (
              
                -- BALANCE-24516: ����� ������� ��������� ������ ����� ������
                -- ���.���� ��� ��������, ��� ������� �������
                xxx.act_dt >= date'2017-03-01' and b.contract_till_dt > date'2017-03-01' and b.discount_type in (1, 2, 3, 7, 12, 36, 37)
               -- BALANCE-22085
                -- � ����� �� ����� � ���������� ��������� 2016:
                --  * ����������� ������ (11)
                --  * �������� ���� (25)
                --2016 ���
-- BALANCE-24734: ��������� 36 ���, ����� ������ ����� ������
            or  xxx.act_dt >= date'2017-03-01' and b.contract_till_dt  <= date'2017-03-01' and b.discount_type in (1, 2, 3, 7, 12, 25, 36)
             or  xxx.act_dt >= date'2016-03-01' and xxx.act_dt < date'2017-03-01'   and b.discount_type in (1, 2, 3, 7, 12, 25)
             -- 2015 ���
             or  xxx.act_dt >= date'2016-03-01' and b.contract_till_dt  <= date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
             or  xxx.act_dt  < date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
            )
          )
          -- ua
       or (
             b.commission_type = 6 
  and (
                   -- BALANCE-25339
                   xxx.act_dt >= date'2017-04-01'
               and b.discount_type in (0, 1, 7, 8, 12, 36)
                   -- BALANCE-22914
                or   xxx.act_dt >= date'2016-04-01' and xxx.act_dt < date'2017-04-01'
               and b.discount_type in (0, 1, 8, 7, 15)
                or  xxx.act_dt <  date'2016-04-01'
               and b.discount_type in (1, 2, 3, 7, 12)
                )
)
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
          -- verticals ico
       or (b.commission_type = 19 and b.discount_type = 7)     
       )
)
--select  *from s_ar_acts_src;
,
s_ar_acts as (
     select  s.*,
       -- BALANCE-27709: ��� ������ ����� �� ����� ������, � ������� �����������
       --   ���, � �� �� "trunc(sysdate, 'MM') - 1", ����� �������
       --   ������� ������������ ��� ������ ����� ��������� ��
       --   ���� ������� �������
--        nvl((
--            select min(c.MAIN_CLIENT_ID)
--              from xxxx_contract_signed_attr    atr
--              join xxxx_ui_contract_apex        c on c.contract_id = atr.contract_id
--             where atr.code = 'BRAND_CLIENTS'
--               and atr.key_num = s.client_id
--                -- BALANCE-27379: ������� � ���.������ �������� ���������, �����
--                -- �������� ����������� ������, � ���.�� ��� ��������
--               and atr.start_dt <= s.till_dt
--                -- BALANCE-27363: ������ ����� ���.������ �� ����� ���� ������
--               and s.till_dt between c.start_dt and nvl(c.finish_dt, (select dt from counting_date))
--       ), s.client_id)           as brand_id
       nvl(b.main_client_id, s.client_id)               as brand_id   
       from s_ar_acts_src s
       left join s_brands     b on b.client_id = s.client_id
            -- BALANCE-27379: ������� � ���.������ �������� ���������, �����
            -- �������� ����������� ������, � ���.�� ��� ��������
            and b.collateral_dt <= s.till_dt
        -- BALANCE-27363: ������ ����� ���.������ �� ����� ���� ������
          and s.till_dt between b.dt and b.finish_dt
)
--select  *from s_ar_acts;
,
s_acts_temp as (
    select b.*,
           -- ���������, ��� �� �������� ������ ���� -- 1 ������ � 1 ���
           count(distinct b.nds)      over (partition by b.contract_id) as nds_count,
           count(distinct b.currency) over (partition by b.contract_id) as currency_count
      from s_ar_acts  b
        -- BALANCE-24627: ������ ���� �� ���.���
     where b.act_dt >= date'2017-03-01'
)
--select  *from s_acts_temp;
,

s_acts_src as ( select * from s_acts_temp
 where 
            -- BALANCE-22203
            -- BALANCE-22331
            -- BALANCE-23542
            -- BALANCE-22578
            -- � ����� �������� ��������� ������ ���� ������
            -- BALANCE-24627: ����������� (from_dt -> act_dt)
                act_dt >= date'2016-03-01'
            and currency_count = 1
            and nds_count = 1
            -- BALANCE-27062: belarus added
        and currency in ('RUR', 'BYN', 'BYR')
          
)
--select * from s_acts_src order by invoice_id;
,
-- ���� ��� �������� ��������
-- ��� ���� �� ������
s_acts as (
    select b.*
      from s_acts_src   b
     where b.commission_type in (12, 13)
     -- BALANCE-22203
     and b.currency = 'RUR'
)
--select * from s_acts ;
,

s_payments as (
    select b.*
      from s_payments_src b
     where b.commission_type in (12, 13)
        -- BALANCE-22203
       and b.currency = 'RUR'
-- BALANCE-28692: ������� � 2018-09-01 ������� �� ������ � � ������.
        --                ������ ������ �� ������ ������ �������
       and b.invoice_dt < date'2018-09-01'
)

--select  *from s_payments;
,
-- ----------------------------------------------------------------------------
-- ������� �� (��������)
-- ----------------------------------------------------------------------------

-- ���������� ���� � ������
s_kv_src as (
    select contract_eid, contract_id,
           currency, discount_type,
           commission_type,
           nds, from_dt, till_dt, payment_type,
             count(distinct case when invoice_dt >= date'2018-09-01'
                               then invoice_id
                               else null
                          end)      as invoice_cnt,
           count(distinct
                    case when invoice_type = 'prepayment'
                     and invoice_dt >= date'2018-09-01'
                    then invoice_id
                    else null
                end)                as invoice_prep_cnt,
           sum(amt_acts)            as amt_to_charge,
           sum(amt_oebs_w_nds)      as amt_to_pay_w_nds,
           sum(amt_oebs)            as amt_to_pay
      from (
            select contract_eid, contract_id, currency, nds, discount_type,
                   commission_type,  invoice_type, invoice_id,
                   from_dt, till_dt, payment_type, invoice_dt,
                   amt  as amt_acts, amt_w_nds  as amt_acts_w_nds,
                     0    as amt_oebs, 0          as amt_oebs_w_nds
            from s_acts
            where act_dt between add_months(trunc((select dt from counting_date), 'MM'), -1)
                              and trunc((select dt from counting_date), 'MM') - 1/24/60/60
            union all
            select contract_eid, contract_id, currency, nds, discount_type,
                   commission_type, invoice_type, invoice_id,
                   from_dt, till_dt, payment_type, invoice_dt,
                   0    as amt_acts, 0          as amt_acts_w_nds,
                   amt  as amt_oebs, amt_w_nds  as amt_oebs_w_nds
              from s_payments
              where comiss_date between add_months(trunc((select dt from counting_date), 'MM'), -1)
                                and trunc((select dt from counting_date), 'MM') - 1/24/60/60
       )
     group by contract_eid, contract_id, currency, payment_type,
              commission_type,
              nds, discount_type, from_dt, till_dt
)
--select  * from  s_kv_src;
,
-- ���������� � �������� (������� ������� ���������������� ��������)
s_kv_control_src as (
    select d.*,
           -- ����������� ������� �� ������� �� ������� �
           -- ������� �� �������� (���������)
           nvl(ratio_to_report(amt_rub)
              over (partition by contract_id, from_dt), 0) as ratio
      from (
        select contract_id, from_dt, till_dt, client_id,
               commission_type, contract_till_dt,
               nds_count, currency_count,
               sum(amt_rub)                                   as amt_rub
          from s_acts
         group by contract_id, from_dt, till_dt, client_id,
                  commission_type, contract_till_dt,
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
                   commission_type, contract_till_dt,
                   nds_count, currency_count,
                   sum(amt_rub)                 as amt_rub,
                   count(distinct client_id)    as client_count,
                   round(max(ratio), 2)         as max_client_ratio_by_direct
              from s_kv_control_src
             group by contract_id, from_dt, till_dt,
                      commission_type, contract_till_dt,
                      nds_count, currency_count
            ) d
      ) d

)

--select * from s_kv_control_pre;

,
-- ������
s_kv_control as (
    select d.*,
           case
   -- BALANCE-24639: �� ��������������� �������, ������ �������
 		when contract_till_dt < date'2017-03-01' then
                case
                when amt_rub >= decode(commission_type,
                                 12, 25000,
                                 13, 16000)
                 and client_count >= 3
                 and (   
                         -- �� ����������, ���� � ����� ��
                         -- ���� ������ ������ ������� ��� ������� � ���
                         is_there_boc = 0 or
                         is_there_boc_1m_ago = 0
                     )
                 and nds_count = 1
                 and currency_count = 1
                then 0
                else 1
                end 
            -- ����� �������
            else 
                case 
                when amt_rub >= decode(commission_type,
                				12, 200000,
                				13, 100000)
                 and client_count >= 3
                 and nds_count = 1
                 and currency_count = 1
                then 0
                else 1
                end
            end as failed
      from s_kv_control_pre d
)

--select * from s_kv_control;
--select * from s_kv_src;

,
-- ����� �� ��� �������� ���� <= �����
s_kv_pre as (
    select 
    d.contract_id, contract_eid,
           d.from_dt, d.till_dt,
           d.commission_type,
           d.nds, d.currency,
           d.discount_type,
		       d.payment_type,
           sum(d.amt_to_charge)         as turnover_to_charge,
           sum(
           case
                when d.from_dt >= date'2018-09-01'
                then 
                decode(f.failed,
                    0, bo.pk_comm.calc_market_m(d.amt_to_charge, d.commission_type),
                    0)        
                else
                d.amt_to_charge*
                decode(f.failed,
                    0, 0.08,
                    0)               
    end) 
    as reward_to_charge,
           sum(d.amt_to_pay)            as turnover_to_pay,
           sum(d.amt_to_pay_w_nds)      as turnover_to_pay_w_nds,
           -- BALANCE-19851
           -- ���������� �� ������� �� ������
           -- BALANCE-22195: ��������� ������ � ����� ������� ���������
         sum(
            case
                when d.from_dt >= date'2018-09-01'
                 and d.invoice_cnt = d.invoice_prep_cnt
                   then
                    decode(f.failed,
                        0, bo.pk_comm.calc_market_m(d.amt_to_charge, d.commission_type),
                       0)
                 else 0
                end
             +  d.amt_to_pay*0.08
  )                    as reward_to_pay
      from s_kv_src         d
      -- BALANCE-19851
      left outer
      join s_kv_control     f on f.contract_id = d.contract_id
                             and f.from_dt = d.from_dt
                             and f.till_dt = d.till_dt
     group by d.contract_eid, d.contract_id, d.from_dt, d.till_dt, d.commission_type,
		     	 d.payment_type,
           d.discount_type, d.currency, d.nds
)
--select * from s_kv_pre;

--select  *from s_kv_src;
,
-- �� � ��������� �� ����� �������, ��� ��������, ��� ����� �� �����, ��� �����
s_kv01 as (
    select contract_eid, contract_id, from_dt, till_dt,
           discount_type, currency, nds,
           -- � ������������
           turnover_to_charge,
           reward_to_charge,
           -- � ����������
           decode(payment_type,
                -- ����������
                2, turnover_to_charge,
                -- ����������
                3, turnover_to_pay)             as turnover_to_pay,
           decode(payment_type,
                2, 0,
                3, turnover_to_pay_w_nds)       as turnover_to_pay_w_nds,
           decode(payment_type,
                2, reward_to_charge,
                reward_to_pay)                  as reward_to_pay_src,
           decode(payment_type,
                2, reward_to_charge,
                (least(reward_to_charge_sum, reward_to_pay_sum) -
                    least(reward_to_charge_sum_prev, reward_to_pay_sum_prev)
                ))                              as reward_to_pay
      from (
            select d.*        ,
                   (select l.payment_type from s_kv_pre l
                     where l.contract_id = d.contract_id
                   )                                    as payment_type,
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
                select d.contract_id, contract_eid, d.discount_type,
                       d.from_dt, d.till_dt,
                       d.nds, d.currency,
                       d.turnover_to_charge,
                       d.reward_to_charge,
                       d.turnover_to_pay,
                       d.turnover_to_pay_w_nds,
                       d.reward_to_pay
                  from s_kv_pre         d
                 union all
                    -- BALANCE-24627
                    -- �������, ����� ������� "�����", ���� ������� ��������
                    -- � ������� �������
                    -- BALANCE-25301: ��������� ������ � ������������ ��������
                select d.contract_id, contract_eid, d.discount_type,
                       d.from_dt, d.till_dt,
                       d.nds, d.currency,
                       sum(d.turnover_to_charge)    as turnover_to_charge,
                       sum(d.reward_to_charge)      as reward_to_charge,
                       sum(d.turnover_to_pay)       as turnover_to_pay,
                       sum(d.turnover_to_pay_w_nds) as turnover_to_pay_w_nds,
                       sum(d.reward_to_pay_src)     as reward_to_pay
                  from xxxx_commission_reward_2013        d
                 where d.contract_id in (
                            select contract_id from s_kv_pre
                       )
                    -- BALANCE-24877: ��������� ������� �� ���.������, ���� ���
                    --                �� ������ ������ �� ������.������
                   and d.from_dt < add_months(trunc((select dt from counting_date), 'MM'), -1)
                   and d.reward_type in (301)
              group by d.contract_id, contract_eid,
                       d.from_dt, d.till_dt,
                       d.discount_type,
                       d.nds, d.currency
                   ) d
           ) s
)
--select * from s_kv01;

-- �������������� ������
select  distinct s."CONTRACT_ID",
s."CONTRACT_EID",
s."FROM_DT",
s."TILL_DT",
s."NDS",
s."CURRENCY",
s."DISCOUNT_TYPE", 
s."REWARD_TYPE",
s."TURNOVER_TO_CHARGE",
s."REWARD_TO_CHARGE",
s."TURNOVER_TO_PAY",
s."TURNOVER_TO_PAY_W_NDS",
s."REWARD_TO_PAY",
s."REWARD_TO_PAY_SRC" 
from (
     select contract_id,     contract_eid,
           from_dt,         till_dt,
           nds,             currency,
           discount_type,
           300 + reward_type            as reward_type,
           -- � ����������
           sum(turnover_to_charge)      as turnover_to_charge,
           sum(reward_to_charge)        as reward_to_charge,
           -- � ������������
           sum(turnover_to_pay)         as turnover_to_pay,
           sum(turnover_to_pay_w_nds)   as turnover_to_pay_w_nds,
           sum(reward_to_pay)           as reward_to_pay,
           sum(reward_to_pay_src)       as reward_to_pay_src
  from (
  select contract_id,     contract_eid,
                   from_dt,         till_dt,
                   nds,             currency,
                   discount_type,
                   reward_type,
                   turnover_to_charge,
                   reward_to_charge,
                   turnover_to_pay_w_nds,
                   turnover_to_pay,
                   reward_to_pay_src,
                   reward_to_pay
    from(
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
			         reward_to_pay_src,
               -- BALANCE-22359: ��� ���+����� � ������ ������ ���������� 1
               -- (��� � �������)
               1                    as reward_type
          from s_kv01
       ))
       )
        group by contract_id,     contract_eid,
              from_dt,         till_dt,
              nds,             currency,
              discount_type,   reward_type
)            s

where s.turnover_to_charge <>0 or s.turnover_to_pay<>0
  order by from_dt, contract_id, discount_type, currency, nds;
;