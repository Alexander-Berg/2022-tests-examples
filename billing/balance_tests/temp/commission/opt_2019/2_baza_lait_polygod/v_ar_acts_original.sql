
  CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."V_AR_ACTS" ("CONTRACT_EID", "CONTRACT_ID", "INVOICE_EID", "INVOICE_ID", "INVOICE_DT", "INVOICE_TYPE", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "CURRENCY", "NDS", "PAYMENT_TYPE", "COMMISSION_TYPE", "DISCOUNT_TYPE", "DISCOUNT_TYPE_SRC", "IS_AGENCY_CREDIT_LINE", "IS_LOYAL", "CLIENT_ID", "ORDER_ID", "SERVICE_ID", "SERVICE_ORDER_ID", "AGENCY_ID", "ACT_ID", "ACT_EID", "ACT_DT", "FROM_DT", "TILL_DT", "AMT_W_NDS", "AMT", "AMT_W_NDS_RUB", "AMT_RUB", "BRAND_ID") AS 
  with
s_brands as (
        select 
               atr.key_num                  as client_id,
               atr.collateral_dt,
               c.dt,
               trunc(nvl(c.finish_dt, sysdate))
                
                + 1 - 1/24/60/60            as finish_dt,
               min(c.client_id)             as main_client_id
          from bo.mv_contract_signed_attr    atr
            
            
          join bo.mv_ui_contract_apex        c on c.contract_id = atr.contract_id
         where atr.code = 'BRAND_CLIENTS'
            
           and atr.value_num = 1
         group by atr.key_num, atr.collateral_dt, c.dt, nvl(c.finish_dt, sysdate)
),
s_src as (
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
       
       nvl(
           case at.commission_type
            when 22 then 1
            when 29 then 1  
            
            when 10 then 97
            when 16 then 97
            else at.commission_type
           end,
           case
                when b.commission_type in (10, 16) then 97
                else b.discount_type
           end)                                         as discount_type,
       
       nvl(at.commission_type, b.discount_type_src)     as discount_type_src,
       b.is_agency_credit_line,
       case
        when nvl(a.is_loyal, 0) = 1 and b.discount_type = 7
         then 1
         else 0
       end                                              as is_loyal,
       o.client_id                                      as client_id,
       o.id                                             as order_id,
       o.service_id,
       o.service_order_id,
       a.client_id                                      as agency_id,
       a.id                                             as act_id,
       a.external_id                                    as act_eid,
       a.dt                                             as act_dt,
       trunc(a.dt, 'MM')                                as from_dt,
       add_months(trunc(a.dt, 'MM'), 1) - 1/84600       as till_dt,
       at.amount                                        as amt_w_nds,
       at.amount-at.amount_nds-at.amount_nsp            as amt,
       at.amount*cr.rate                                as amt_w_nds_rub,
       (at.amount-at.amount_nds-at.amount_nsp)*cr.rate  as amt_rub
  from bo.mv_opt_2015_invoices_f        b
  join bo.t_act                         a  on a.invoice_id = b.invoice_id
                                          and a.hidden < 4
                                          and a.dt >= date'2015-03-01'
                                           
                                          and ( a.is_loyal = 0 and a.dt >= date'2017-03-01'
                                             or a.dt <  date'2017-03-01')
  join bo.t_act_trans                   at on at.act_id = a.id
  join bo.t_consume                     q  on q.id = at.consume_id
  join bo.t_order                       o  on o.id = q.parent_order_id
  join biee.mv_currency_rate            cr on cr.cc = b.currency
                                          and cr.rate_dt = trunc(a.dt)
 where b.commission_type in (1, 2, 3, 4, 5, 6, 8, 10, 11, 12, 13, 16, 17, 19, 20, 21, 23)
   and (
          
          (
            b.commission_type in (1, 2, 8, 21, 23)
        and (
                
                
                a.dt >= date'2017-03-01' and b.contract_till_dt > date'2017-03-01' and b.discount_type in (1, 2, 3, 7, 12, 36, 37, 38)
                
                
                
                
             
             
             or a.dt >= date'2017-03-01' and b.contract_till_dt <= date'2017-03-01' and b.discount_type in (1, 2, 3, 7, 12, 25, 36)
             or a.dt >= date'2016-03-01' and a.dt < date'2017-03-01'                and b.discount_type in (1, 2, 3, 7, 12, 25)
             
             or a.dt >= date'2016-03-01' and b.contract_till_dt <= date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
             or a.dt  < date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
            )
          )
          
       or (
                b.commission_type = 6
            and (
                   
                   a.dt >= date'2017-04-01'
               and b.discount_type in (0, 1, 7, 8, 12, 36)
                   
                or a.dt >= date'2016-04-01' and a.dt < date'2017-04-01'
               and b.discount_type in (0, 1, 8, 7, 15)
                or a.dt <  date'2016-04-01'
               and b.discount_type in (1, 2, 3, 7, 12)
                )
          )
          
       or (b.commission_type = 3 and b.discount_type in (17))
          
       or (b.commission_type = 4 and b.discount_type in (19))
          
       or (b.commission_type = 5 and b.discount_type = 12)
          
       or (b.commission_type in (10, 16) and 1=1)
          
       or (b.commission_type = 11 and b.discount_type_src = 29)
          
       or (b.commission_type in (12, 13) and b.discount_type_src = 11)
          
       or (b.commission_type = 17 and b.discount_type = 25)
          
       or (b.commission_type = 19 and b.discount_type = 7)
          
       or (b.commission_type in (20) and at.commission_type in (7, 37))
       )
)
select s."CONTRACT_EID",s."CONTRACT_ID",s."INVOICE_EID",s."INVOICE_ID",s."INVOICE_DT",s."INVOICE_TYPE",s."CONTRACT_FROM_DT",s."CONTRACT_TILL_DT",s."CURRENCY",s."NDS",s."PAYMENT_TYPE",s."COMMISSION_TYPE",s."DISCOUNT_TYPE",s."DISCOUNT_TYPE_SRC",s."IS_AGENCY_CREDIT_LINE",s."IS_LOYAL",s."CLIENT_ID",s."ORDER_ID",s."SERVICE_ID",s."SERVICE_ORDER_ID",s."AGENCY_ID",s."ACT_ID",s."ACT_EID",s."ACT_DT",s."FROM_DT",s."TILL_DT",s."AMT_W_NDS",s."AMT",s."AMT_W_NDS_RUB",s."AMT_RUB",
       
       
       
       
       nvl(b.main_client_id, s.client_id)               as brand_id
  from s_src        s
  left outer
  join s_brands     b on b.client_id = s.client_id
                      
                      
                     and b.collateral_dt <= s.till_dt
                      
                     and s.till_dt between b.dt and b.finish_dt;
