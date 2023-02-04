# -*- coding: utf-8 -*-

from balance import balance_steps as steps

"""
with contracts as (
    select c.client_id, c.person_id, c.id, c.external_id, col0.id col0_id, c.type,
            et.value contract_type,
           col0.dt start_dt, la_fin.value_dt end_dt, col0.is_signed, col0.is_faxed, sapt.value_num pay_type,
           (f.id || ' ' || f.title) firm, co.region_name,
           listagg(la_serv.key_num, ', ') within group (order by la_serv.key_num) services,
           cur.char_code currency, la_man.value_num manager_code
    from bo.t_contract2 c
         left join bo.t_contract_collateral col0 on col0.contract2_id = c.id
            and col0.collateral_type_id is null
         left join bo.t_contract_attributes ca
                   on ca.attribute_batch_id = col0.attribute_batch_id
                   and ca.code = 'COMMISSION'
         left join bo.t_enums_tree et on et.code = ca.value_num and et.parent_id = 1400
         left join bo.t_contract_attributes sapt on sapt.attribute_batch_id = col0.attribute_batch_id
            and sapt.code = 'PAYMENT_TYPE'
         left join bo.t_contract_attributes la_fin
                   on la_fin.attribute_batch_id = col0.attribute_batch_id
                    and la_fin.code in ('FINISH_DT', 'END_DT')
         left join bo.t_contract_attributes la_serv
                   on la_serv.attribute_batch_id = col0.attribute_batch_id
                        and la_serv.code = 'SERVICES'
                        and la_serv.value_num is not null
         left join bo.t_contract_attributes la_firm
             on la_firm.attribute_batch_id = col0.attribute_batch_id
                    and la_firm.code = 'FIRM'
         left join bo.t_firm f on f.id = la_firm.value_num
         left join bo.t_contract_attributes la_country
                   on la_country.attribute_batch_id = col0.attribute_batch_id
                      and la_country.code = 'COUNTRY'
         left join bo.t_country co on co.region_id = la_country.value_num
         left join bo.t_contract_attributes la_cur
             on la_cur.attribute_batch_id = col0.attribute_batch_id
                    and la_cur.code = 'CURRENCY'
         left join bo.t_currency cur on cur.num_code = la_cur.value_num
         left join bo.t_contract_attributes la_man
             on la_man.attribute_batch_id = col0.attribute_batch_id
                    and la_man.code = 'MANAGER_CODE'
    where 1 = 1
--   and c.client_id = 133439594--(select max(client_id) from bo.t_contract2 where id=288528)
  and c.id in (4426799)
--   and c.external_id in ('ОФ-422917')
--   and et.value = 'Корпоративные Клиенты'
--   and la_fin.value_dt is not null
--   and c.client_id = 61700587
--   and c.external_id like 'ДС-22598-03/18'
    group by c.client_id, c.person_id, c.id, c.external_id,
             col0.id, c.type, et.value,
             col0.dt, la_fin.value_dt, col0.is_signed, col0.is_faxed, sapt.value_num,
             f.id || ' ' || f.title, co.region_name, cur.char_code, la_man.value_num
    order by c.id
)
select c.client_id, c.person_id, c.id, c.external_id, c.col0_id, c.type,  c.contract_type,
       c.start_dt, c.end_dt, c.is_signed, c.is_faxed,
       c.pay_type, c.firm, c.region_name, c.services, c.currency, c.manager_code
       , i.id invoice_id, i.external_id invoice, ep.value_str service_code
--        , i.paysys_id, i.nds, i.nds_pct, i.firm_id, i.bank_details_id, i.base_iso_currency, i.currency_rate_src, i.iso_currency
--        , a.id act_id, a.external_id act, a.dt act_dt
--        , tt.*
--        , tc.*
from contracts c
left join bo.t_invoice i on i.contract_id = c.id
   and i.type = 'personal_account'
left join bo.t_extprops ep on ep.object_id = i.id
    and ep.classname = 'PersonalAccount'
    and ep.attrname = 'service_code'
-- left join bo.t_Act a on a.invoice_id = i.id
-- left join bo.t_act_trans at on at.act_id = a.id
-- left join bo.t_consume q on q.id = at.consume_id
-- left join bo.t_order o on o.id = q.parent_order_id
-- left join bo.t_thirdparty_transactions tt on tt.contract_id = c.id
-- left join bo.t_thirdparty_corrections tc on tc.contract_id = c.id
;

update
(
    select *
    from bo.t_export
    where object_id = 119053962 
      and classname = 'Act'
)
set state=1
;

"""


def test_export():
#     products = """513026
# """
#     pr_ids = [int(p) for p in products.split() if p]
#     for pr_id in pr_ids:
#         steps.ExportSteps.export_oebs(product_id=pr_id)
    # steps.CommonSteps.export('OEBS', 'ActivityType', 1167)

#
#
#     # try:
#     #     steps.ExportSteps.create_export_record(133104548, classname='Client', type='OEBS')
#     # except:
#     #     pass
#     steps.ExportSteps.export_oebs(client_id=135260371)
#
#     # try:
#     #     steps.ExportSteps.create_export_record(32537353, classname='Person', type='OEBS')
#     # except:
#     #     pass
#     steps.ExportSteps.export_oebs(person_id=12305746)
#
#     steps.ExportSteps.export_oebs(contract_id=4426832)
#     steps.ExportSteps.export_oebs(collateral_id='9548148')
#     steps.ExportSteps.export_oebs(manager_id=20453)
#     steps.ExportSteps.export_oebs(invoice_id=114260439)
#
#
#     steps.ExportSteps.export_oebs(product_id=511458)
#     steps.ExportSteps.export_oebs(product_id=511461)
#     steps.ExportSteps.export_oebs(product_id=511459)
#     steps.ExportSteps.export_oebs(product_id=511460)
#     # steps.ExportSteps.export_oebs(act_id=116039982) ## закрытый период, не экспортировался
#     steps.ExportSteps.export_oebs(act_id=119097452)
#
#     # steps.ExportSteps.create_export_record('36546279019', classname='ThirdPartyCorrection', type='OEBS')
#     # steps.ExportSteps.export_oebs(correction_id='36546279019')
#     # steps.ExportSteps.create_export_record('36546279029', classname='ThirdPartyCorrection', type='OEBS')
#     # steps.ExportSteps.export_oebs(correction_id='36546279029')
#     # steps.ExportSteps.create_export_record('66617310006410', classname='ThirdPartyTransaction', type='OEBS')
#     # steps.ExportSteps.export_oebs(transaction_id='66617310006410')
#     # steps.ExportSteps.create_export_record('36546089579', classname='ThirdPartyTransaction', type='OEBS')
#     # steps.ExportSteps.export_oebs(transaction_id='36546089579')
#
#
#
# #
# #     """
# #     update (
# #     select *
# #     from bo.t_thirdparty_transactions
# #     where 1 = 1 and id in (21838204239, 21838204339)
# # )
# # set id = id + 6600000000000
# # ;"""
#
# #     tt_ids = [
# # 21839586399, 21839586409, 21839586419, 21839586429, 21839586439, 21839586449, 21839586459, 21839586469, 21839586479,
# # 21839586489, 21839586499, 21839586509, 21839586519, 21839586529, 21839586539, 21839586549, 21839586559, 21839586569,
# # 21839586579, 21839586589, 21839586599, 21839586619, 21839586669, 21839586679, 21839586689, 21839586699, 21839586709,
# # 21839586719, 21839586729, 21839586739, 21839586749, 21839586769, 21839586779, 21839586789, 21839586799, 21839586809,
# # 21839586819, 21839586829, 21839586839, 21839586849, 21839586859, 21839586869, 21839586879, 21839586889, 21839586909,
# # 21839586919, 21839586929, 21839586939, 21839586949, 21839586959, 21839586989, 21839587029, 21839587039, 21839587049,
# # 21839587059, 21839587069, 21839587079, 21839587089, 21839587099, 21839587109, 21839587119, 21839587129, 21839587139,
# # 21839587149, 21839587159, 21839587169
# #     ]
# #
# #     for tt_id in tt_ids:
# #         str_tt_id = '66' + str(tt_id)
# #         steps.ExportSteps.create_export_record(str_tt_id, classname='ThirdPartyTransaction', type='OEBS')
# #         steps.ExportSteps.export_oebs(transaction_id=str_tt_id)
#
#
#         # steps.ExportSteps.export_oebs(transaction_id='21838204239')
#         # steps.ExportSteps.export_oebs(transaction_id='21838204339')
#
#         # steps.ExportSteps.create_export_record('66' + '21838204239', classname='ThirdPartyTransaction', type='OEBS')
#         # steps.ExportSteps.export_oebs(transaction_id='66'+'21838204239')
#
#         # steps.ExportSteps.create_export_record('66' + '21838204339', classname='ThirdPartyTransaction', type='OEBS')
#         # steps.ExportSteps.export_oebs(transaction_id='66'+'21838204339')