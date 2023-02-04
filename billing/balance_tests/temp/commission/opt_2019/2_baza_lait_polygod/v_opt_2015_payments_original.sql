
  CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."V_OPT_2015_PAYMENTS" ("CONTRACT_EID", "CONTRACT_ID", "INVOICE_EID", "INVOICE_ID", "INVOICE_DT", "INVOICE_TYPE", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "CURRENCY", "NDS", "PAYMENT_TYPE", "COMMISSION_TYPE", "DISCOUNT_TYPE", "DISCOUNT_TYPE_SRC", "IS_AGENCY_CREDIT_LINE", "INVOICE_TOTAL_SUM", "INVOICE_TOTAL_SUM_W_NDS", "DOC_DATE", "OEBS_DT", "PAYMENT_NUMBER", "FROM_DT", "TILL_DT", "AMT", "AMT_W_NDS", "AMT_BY_INVOICE", "PAYMENTS_CURR_BY_INVOICE_W_NDS", "IS_FULLY_PAID_PRE", "IS_EARLY_PAYMENT_PRE", "IS_2018", "FULLY_PAID_POS", "EARLY_PAYMENT_POS", "AMT_TTL_W_NDS", "AMT_TTL", "PAYMENT_COUNT_BY_CONTRACT", "PAYMENT_COUNT_BY_INVOICE", "IS_FULLY_PAID", "IS_EARLY_PAYMENT") AS 
  with
s_payments as (
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
       oebs.doc_date,
       oebs.dt                                      as oebs_dt,
       oebs.payment_number,
       trunc(oebs.doc_date, 'MM')                   as from_dt,
       add_months(
        trunc(oebs.doc_date, 'MM'), 1)-1/84600      as till_dt,
       oebs.sum*100/(100 + b.nds*b.nds_pct)         as amt,
       oebs.sum                                     as amt_w_nds,
       
       
       
       sum(oebs.sum*100/(100 + b.nds*b.nds_pct))
            over(partition by b.invoice_id
                                             
                     order by oebs.doc_date, oebs.dt, oebs.payment_number, oebs.source_type)   as amt_by_invoice,
       
        
       sum(oebs.sum)
            over(partition by b.invoice_id
                     order by oebs.doc_date, oebs.dt, oebs.payment_number, oebs.source_type)   as payments_curr_by_invoice_w_nds
  from bo.mv_opt_2015_invoices_f        b
  join bo.mv_oebs_receipts_f            oebs on oebs.invoice_eid = b.invoice_eid
                                            and oebs.doc_date >= date'2015-03-01'
                                            and oebs.doc_date is not null
 where 
          
          (
            b.commission_type in (1, 2, 8, 21, 23)
        and (
                
                b.invoice_dt >= date'2017-03-01'        and
                b.discount_type in (1, 2, 3, 7, 12, 36, 37) and
                b.currency = 'RUR'
             or
                
                b.invoice_dt >= date'2016-03-01'        and 
                b.invoice_dt  < date'2017-03-01'        and
                
                b.discount_type in (1, 2, 3, 7, 12, 25, 36) and
                
                
                b.currency = 'RUR'
             or b.invoice_dt  < date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12, 36)
            )
          )
          
       or (b.commission_type = 6 and b.discount_type in (1, 2, 3, 7, 12))
          
       or (b.commission_type = 3 and b.discount_type in (17))
          
       or (b.commission_type = 4 and b.discount_type in (19))
          
       or (b.commission_type = 5 and b.discount_type = 12)
          
       or (b.commission_type in (10, 16) and 1=1)
          
       or (b.commission_type = 11 and b.discount_type_src = 29)
          
       or (b.commission_type = 17 and b.discount_type = 25)
          
       or (b.commission_type in (12, 13) and b.discount_type_src = 11)
)
, s_early_payment_pre as (
select d.*,
       
       case
        when payments_curr_by_invoice_w_nds >= invoice_total_sum_w_nds
        then 1
        else 0
       end                                          as is_fully_paid_pre,
       (
        
        
        
        
            case
            when d.is_agency_credit_line = 1
            then (
                select case
                        when a.act_count = 1
                         and d.doc_date <= a.payment_term_dt - 1
                          
                         and a.act_amount = d.payments_curr_by_invoice_w_nds
                        then 1
                        else 0
                       end
                  from (
                    
                    
                    select min(a.payment_term_dt)   as payment_term_dt,
                           count(distinct a.id)     as act_count,
                           sum(a.amount)            as act_amount
                      from bo.t_act         a
                     where a.invoice_id = d.invoice_id
                       and a.hidden < 4
                       ) a
                 )
            else 0
            end
       )                                            as is_early_payment_pre
  from s_payments           d
    
 where not exists (
            select 1
              from bo.t_act a
             where a.is_loyal = 1
               and a.invoice_id = d.invoice_id
               and a.hidden < 4
               and a.dt >= date'2017-03-01'
               and d.invoice_dt >= date'2017-03-01'
       )
)
, s_early_payment_counted as (
select d.*,
       
       
       case
        when from_dt >= date'2018-03-01'
         and invoice_dt >= date'2018-03-01'
        then 1
        else 0
       end                                          as is_2018,
       count(decode(is_fully_paid_pre, 1, 1, null)) over(partition by invoice_id
                             order by doc_date, oebs_dt, payment_number) as
                        fully_paid_pos,
       count(decode(is_early_payment_pre, 1, 1, null)) over(partition by invoice_id
                             order by doc_date, oebs_dt, payment_number) as
                        early_payment_pos
  from s_early_payment_pre          d
)
select d."CONTRACT_EID",d."CONTRACT_ID",d."INVOICE_EID",d."INVOICE_ID",d."INVOICE_DT",d."INVOICE_TYPE",d."CONTRACT_FROM_DT",d."CONTRACT_TILL_DT",d."CURRENCY",d."NDS",d."PAYMENT_TYPE",d."COMMISSION_TYPE",d."DISCOUNT_TYPE",d."DISCOUNT_TYPE_SRC",d."IS_AGENCY_CREDIT_LINE",d."INVOICE_TOTAL_SUM",d."INVOICE_TOTAL_SUM_W_NDS",d."DOC_DATE",d."OEBS_DT",d."PAYMENT_NUMBER",d."FROM_DT",d."TILL_DT",d."AMT",d."AMT_W_NDS",d."AMT_BY_INVOICE",d."PAYMENTS_CURR_BY_INVOICE_W_NDS",d."IS_FULLY_PAID_PRE",d."IS_EARLY_PAYMENT_PRE",d."IS_2018",d."FULLY_PAID_POS",d."EARLY_PAYMENT_POS",
       
       
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
       
       count(1)
        over(partition by contract_id, from_dt,
                          payment_number)                    as payment_count_by_contract,
       count(1)
        over(partition by contract_id, from_dt,
                          invoice_id, payment_number)        as payment_count_by_invoice,
       
       decode(fully_paid_pos, 1, is_fully_paid_pre, 0)       as is_fully_paid,
       decode(early_payment_pos, 1, is_early_payment_pre, 0) as is_early_payment
  from s_early_payment_counted d;
