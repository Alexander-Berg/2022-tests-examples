
  CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."V_OPT_2015_BASE_SKV" ("CONTRACT_EID", "CONTRACT_ID", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "DISCOUNT_TYPE", "NDS", "CLIENT_ID", "FROM_DT", "TILL_DT", "TILL_DT_FC", "NDS_COUNT", "CURRENCY_COUNT", "AMT_W_NDS_RUB", "AMT_RUB_NOT_LC", "AMT_RUB_LC", "AMT_RUB", "AMT_FOR_FORECAST", "MAX_RATIO", "EXCLUDED", "FAILED") AS 
  with

counting_date as (select date'2018-09-03' as dt from dual)
--select * from counting_date;
,

s_half_years as (
    -- ��������� � �� 6 �������
    select d.dt from_dt, add_months(d.dt, 6)-1/24/60/60 as till_dt
      from (
         select add_months(date'2017-03-01', 6*(level-1)) as dt
           from dual
        connect by level <= 10
           ) d
)
--select * from s_half_years;
,
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

s_brands as (
        select /*+ materialize */
               atr.key_num          as client_id,
               atr.start_dt         as collateral_dt,
               c.start_dt           as dt,
               trunc(nvl(c.finish_dt, date'2018-09-03')) + 1 - 1/24/60/60            as finish_dt,
               min(c.MAIN_CLIENT_ID)     as main_client_id
          from xxxx_contract_signed_attr    atr
          -- BALANCE-27379: ������� � ���.������ �������� ���������, �����
            -- �������� ����������� ������, � ���.�� ��� ��������
          join xxxx_ui_contract_apex        c on c.contract_id = atr.contract_id
                                              where atr.code = 'BRAND_CLIENTS'
         group by atr.key_num, atr.start_dt, c.start_dt, nvl(c.finish_dt, date'2018-09-03')
)

--select  *from s_brands;
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
           b.discount_type)                             as discount_type,
       -- BALANCE-26651
           nvl(xxx.commission_type, b.discount_type_src)     as discount_type_src,
           b.is_agency_credit_line,
           case
           when nvl(xxx.is_loyal, 0) = 1 and b.discount_type = 7
            then 1
            else 0
           end                                               as is_loyal,
           xxx.client_id                                     as client_id,
           xxx.agency_id                                      as agency_id,
--           nvl(brand.main_client_id, xxx.client_id)          as brand_id,
           xxx.act_id                                        as act_id,
           xxx.act_eid                                       as act_eid,
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
    --  left outer
  --   join s_brands                               brand on brand.client_id = xxx.client_id
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
     select  
     s.*,
       -- BALANCE-27709: ��� ������ ����� �� ����� ������, � ������� �����������
       --   ���, � �� �� "trunc(sysdate, 'MM') - 1", ����� �������
       --   ������� ������������ ��� ������ ����� ��������� ��
       --   ���� ������� �������
       nvl(b.main_client_id, s.client_id)               as brand_id   
       from s_ar_acts_src s
       left outer 
       join s_brands     b on b.client_id = s.client_id
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

--select * from s_acts_temp;
,


s_opt_2015_acts as ( select * from s_acts_temp
 where      -- BALANCE-22203
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

--select  *from s_opt_2015_acts;
,


s_acts as (
    select b.*
      from s_opt_2015_acts b
     where b.commission_type in (1, 8, 21)
        -- ������ �������� �������� ���.����
       and b.contract_till_dt > add_months(trunc((select dt from counting_date), 'YYYY'), 2)
)

--select * from s_acts;
,

s_opt_2015_base_skv_less_70 as (
-- �������� �� ������, �� ������� ���
-- �������� � �������� �� ������� > 70%
-- BALANCE-15995
select d.contract_id, d.from_dt, max(ratio) as max_ratio
  from (
    select d.*,
           -- BALANCE-15635
           nvl(ratio_to_report(amt_rub)
              over (partition by d.contract_id, d.from_dt), 0) as ratio
      from (
        select d.contract_id,
               d.brand_id                                        as client_id,
               h.from_dt,
               -- BALANCE-15635
               sum(decode(d.discount_type, 7, d.amt_rub, null))  as amt_rub
          from s_opt_2015_acts             d

          join s_half_years                   h  on d.act_dt between h.from_dt
                                                                 and h.till_dt
               where d.commission_type in (1, 8, 21)
         group by h.from_dt,
                  d.contract_id,
                  d.brand_id
           ) d
       ) d
 group by d.contract_id, d.from_dt
)
--select * from s_opt_2015_prof_skv_less_70;
,
-- ���� ��, � ������� ����������� ��������� � ���������
-- ���� ��� ���� ���� ����� �� ������ �� ���. ���, �.�. ������
-- ���������, ������� ���������� � ������� ���. ����. � �� �����
-- �� ���� ��������� ���� ������ �� ���������, � �� ������ ���,
-- ������� ��������� � ������� ���.����
-- BALANCE-18412
s_acts_lc_view as (
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
           b.discount_type,
		   b.commission_type,
           b.is_loyal,
           b.client_id,
           b.amt_w_nds,
           b.amt,
           b.amt_w_nds_rub,
           b.amt_rub,
           b.act_dt,
           h.from_dt,
           trunc(h.till_dt)                         as till_dt,
           b.nds_count,
           b.currency_count,
           lc.lc_turnover,
           -- BALANCE-20689
           round(abs(months_between(
              lc.collateral_end_dt, lc.collateral_first_dt)))      as lc_months_count,
           lc.collateral_first_dt                                as lc_start_dt,
           lc.collateral_end_dt                                as lc_end_dt
      from s_acts               b
      join XXXX_LOYAL_CLIENTS_CONTR_ATTR   lc on lc.contract_id = b.contract_id
                                              and lc.client_id = b.client_id
                                              -- ������ ������������� ��
                                              -- ������������ ���������
                                              -- TODO: ���������, ����� ���?
                                              and case
                                                    when (select dt from counting_date) >= date'2016-03-01' then date'2016-03-01'-1/24/60/60
                                                    when (select dt from counting_date) >= date'2015-09-01' then date'2015-09-01'-1/24/60/60
                                                    when (select dt from counting_date) >= date'2015-03-01' then date'2015-03-01'-1/24/60/60
                                                    else trunc((select dt from counting_date), 'MM')
                                                end > lc.collateral_end_dt
                                               join s_half_years                     h  on lc.collateral_end_dt between h.from_dt
                                                                and h.till_dt
     where b.is_loyal = 1
       and b.act_dt between lc.collateral_first_dt and lc.collateral_end_dt

)

--select * from s_acts_lc_view;
,
s_skv_lc as (
    select d.*
      from (
        select d.contract_eid, d.contract_id,
               d.contract_from_dt,
               d.contract_till_dt,
               d.currency, d.nds, d.discount_type, d.commission_type,
               d.client_id, d.is_loyal,
               d.lc_turnover,
               d.from_dt,
               d.till_dt,
               d.lc_months_count,
			   d.nds_count, d.currency_count,
               sum(d.amt_w_nds_rub)                     as amt_w_nds_rub,
               sum(d.amt_rub)                           as amt_rub
          from s_acts_lc_view                 d
         group by d.from_dt, d.till_dt,
                  d.contract_from_dt,
                  d.contract_till_dt,
                  d.contract_eid, d.contract_id,
                  d.currency, d.nds, d.discount_type, d.commission_type,
                  d.client_id, d.is_loyal, d.lc_months_count,
                  d.nds_count, d.currency_count,
				  d.lc_turnover
           ) d
     where d.amt_rub >= lc_turnover*lc_months_count*1.1
)
--select * from s_skv_lc;
,
s_skv_not_lc as (
    select d.contract_eid, d.contract_id,
           d.contract_from_dt,
           d.contract_till_dt,
           d.currency, d.nds, d.discount_type, d.commission_type,
           d.client_id, d.is_loyal,
		   d.nds_count, d.currency_count,
           -- ��� �������� (��� �� �� �����, �.�. �� �� ��������� � ��������)
           -- ����� �����, �� ����� ������ ����� ������ ��� �������
           d.till_dt                                    as till_dt_fc,
           -- ������� ���������
           h.from_dt                                    as from_dt,
           trunc(h.till_dt)                             as till_dt,
           sum(d.amt_w_nds_rub)                         as amt_w_nds_rub,
           sum(d.amt_rub)                               as amt_rub
      from s_acts             d
      join s_half_years                   h  on d.act_dt between h.from_dt
                                                             and h.till_dt
        -- ��� ��
       and d.is_loyal = 0
        -- ������, ������� (���), ����������
		-- BALANCE-22408
     where d.discount_type in (7, 1, 2, 3, 12, 36, 37)
     group by h.from_dt, trunc(h.till_dt), d.till_dt,
              d.contract_from_dt,
              d.contract_till_dt,
              d.contract_eid, d.contract_id,
              d.currency, d.nds, d.discount_type, d.commission_type,
              d.nds_count, d.currency_count,
              d.client_id, d.is_loyal
)
--select * from s_skv_not_lc;


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
       0                        as excluded,
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
               d.discount_type, d.commission_type,
               d.client_id, d.nds,
               d.nds_count, d.currency_count,
               d.from_dt, d.till_dt, d.till_dt_fc,
               d.amt_w_nds_rub,
               d.amt_rub            as amt_rub_not_lc,
               0                    as amt_rub_lc
          from s_skv_not_lc d
         union all
        select d.contract_eid, d.contract_id,
               d.contract_from_dt,
               d.contract_till_dt,
               d.discount_type, d.commission_type,
               d.client_id, d.nds,
               d.nds_count, d.currency_count,
               d.from_dt, d.till_dt, d.till_dt as till_dt_fc,
               d.amt_w_nds_rub,
               0                    as amt_rub_not_lc,
               d.amt_rub            as amt_rub_lc
          from s_skv_lc d
       ) d
       -- ������� ������������ ���� �� ������� �� ������� �� ���������
  join s_opt_2015_base_skv_less_70   l on l.contract_id = d.contract_id
                                         and l.from_dt = d.from_dt;
