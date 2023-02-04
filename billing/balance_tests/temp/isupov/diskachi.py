# -*- coding: utf-8 -*-
import datetime
import decimal

from balance import balance_steps as steps
from btestlib.constants import Currencies, Users, Services, Firms, Products, Nds, TransactionType, PaymentType, \
    Regions, ActType, InvoiceType, Paysyses, Export, PersonTypes, CurrencyRateSource

from btestlib.data.simpleapi_defaults import ThirdPartyData

from btestlib.data.partner_contexts import TAXI_RU_CONTEXT

Decimal = decimal.Decimal

TAXI_UBER_BY_CONTEXT = TAXI_RU_CONTEXT.new(
        name='TAXI_UBER_BY_CONTEXT',
        person_type=PersonTypes.EU_YT,
        currency=Currencies.BYN,
        payment_currency=Currencies.BYN,
        firm=Firms.UBER_115,
        region=Regions.BY,
        paysys=Paysyses.BANK_UR_UBER_BYN,
        third_party_data=ThirdPartyData.TAXI_UBER_BY,
        nds=Nds.NONE,
        is_offer=True,
        contract_services=[Services.TAXI.id, Services.TAXI_111.id, Services.TAXI_128.id,
                           Services.UBER.id, Services.UBER_ROAMING.id],
        commission_pct=Decimal('2.44'),
        min_commission=Decimal('0'),
        currency_rate_source=CurrencyRateSource.NBRB,
        precision=2,
        special_contract_params={'personal_account': 1, 'country': Regions.BY.id,
                                 'partner_commission_pct2': 0,
                                 'unilateral': 0,
                                 'netting': 1,
                                 'netting_pct': 100,
                                 'ind-bel-nds': 20,
                                 # 'region': CountryRegion.RUS, 'nds_for_receipt': NdsNew.DEFAULT.nds_id
                                 },
    )

context_BV = TAXI_UBER_BY_CONTEXT

res = steps.ContractSteps.create_partner_contract(context_BV, additional_params={'memo': '2323'}, is_postpay=0)

print(res)

#
# client_id, contract_id, person_id = steps.TaxiSteps.create_taxi_contract_prepay(datetime.datetime(2019,5,1),
#                                                                                         0,
#                                                                                         netting_pct=70,
#                                                                                         firm=context_BV.firm,
#                                                                                         person_type=context_BV.person_type.code,
#                                                                                         region=context_BV.region,
#                                                                                         currency=context_BV.currency,
#                                                                                         nds=context_BV.nds,
#                                                                                         services=context_BV.services)

# for card_register in [1404386, 1404766, 1404813, 1404829, 1413674, 1413727, 1413763]:
#     res = steps.CommonSteps.export(Export.Type.OEBS, 'CardRegister', card_register)
#     print(res)

# print(steps.CommonPartnerSteps.export_payment(3204470973))
# print(steps.CommonPartnerSteps.export_payment(3204470976)) -- refund
# print(steps.CommonPartnerSteps.export_payment(3204511490))
# print(steps.CommonPartnerSteps.export_payment(3204511501))  # refund

# # th = [3204511498, 3204511505]
# th = [3204517967, 3204517968]
# for t in th:
#     print(steps.CommonPartnerSteps.export_payment(t))


# print(steps.CommonPartnerSteps.export_payment(3204470975))
raise 6666

th = [
    '44105814169',
    '44105814179']

steps.CommonSteps.export(Export.Type.OEBS, Export.Classname.CLIENT, 133541669)
# steps.CommonSteps.export(Export.Type.OEBS_API, Export.Classname.PERSON, 11140977)
steps.CommonSteps.export(Export.Type.OEBS, Export.Classname.CONTRACT, 1581337)
steps.CommonSteps.export(Export.Type.OEBS, Export.Classname.CONTRACT_COLLATERAL, 1995867)
steps.CommonSteps.export(Export.Type.OEBS, Export.Classname.CONTRACT, 1581369)
# steps.CommonSteps.export(Export.Type.OEBS_API, Export.Classname.CONTRACT, 1581369)
steps.CommonSteps.export(Export.Type.OEBS, Export.Classname.CONTRACT_COLLATERAL, 1995899)

for t in th:
    res = steps.CommonSteps.export(Export.Type.OEBS, Export.Classname.TRANSACTION, t)
    print(res)

# for card_register in [1404383, 1413672]:
#     res = steps.CommonSteps.export(Export.Type.OEBS, 'CardRegister', card_register)
#     print(res)

raise 666

steps.ExportSteps.export_oebs(client_id=132783110)
steps.ExportSteps.export_oebs(person_id=32281321)
steps.ExportSteps.export_oebs(contract_id=1601495)
steps.ExportSteps.export_oebs(collateral_id='1898418')
steps.ExportSteps.export_oebs(person_id=32281324)
steps.ExportSteps.export_oebs(contract_id=1601498)
steps.ExportSteps.export_oebs(collateral_id='1898422')
# steps.ExportSteps.export_oebs(manager_id=21902)
steps.ExportSteps.export_oebs(transaction_id='17309925430')

######################
steps.ExportSteps.export_oebs(client_id=132783114)
steps.ExportSteps.export_oebs(person_id=32281327)
steps.ExportSteps.export_oebs(contract_id=1601501)
steps.ExportSteps.export_oebs(collateral_id='1898425')
steps.ExportSteps.export_oebs(person_id=32281329)
steps.ExportSteps.export_oebs(contract_id=1601502)
steps.ExportSteps.export_oebs(collateral_id='1898426')
# steps.ExportSteps.export_oebs(manager_id=21902)
steps.ExportSteps.export_oebs(transaction_id='17309925440')

# import datetime
# from decimal import Decimal as D
#
# from dateutil.relativedelta import relativedelta
#
# from balance import balance_steps as steps
# from balance.balance_objects import Context
# from btestlib.constants import *
# from btestlib.data.defaults import *
# from btestlib.matchers import contains_dicts_with_entries, contains_dicts_equal_to
#
# # from balance.balance_templates import Clients, Persons
# from btestlib.data import person_defaults
#
#
# CONTRACT_START_DT = utils.Date.first_day_of_month() - relativedelta(months=3)
#
# SrvDiskB2B = Services.Service(id=671, name=u'Еда: Курьеры (прием платежей)',
#                               token='eat_delivery_agent_4d53f26a1bf3e99b340c27626e3a8395')
#
# DISKACHI_CONTEXT = Context().new(
#         # common
#         name='DISKACHI_CONTEXT',
#         service=SrvDiskB2B,
#         person_type=PersonTypes.UR,
#         firm=Firms.YANDEX_1,
#         currency=Currencies.RUB,
#         payment_currency=Currencies.RUB,
#         nds=NdsNew.DEFAULT,
#         invoice_type=InvoiceType.PERSONAL_ACCOUNT,
#         contract_type=ContractSubtype.GENERAL,
# )
#
#
# FIRST_MONTH = utils.Date.first_day_of_month() - relativedelta(months=2)
# SECOND_MONTH = FIRST_MONTH + relativedelta(months=1)
#
#
# def create_person(context, client_id):
#     return steps.PersonSteps.create(client_id, context.person_type.code,
#                                     full=True,
#                                     inn_type=person_defaults.InnType.RANDOM,
#                                     name_type=person_defaults.NameType.RANDOM,
#                                     params={'is-partner':
#                                                 str(
#                                                     1 if context.contract_type == ContractSubtype.SPENDABLE else 0)},
#                                     )
#
#
# def create_completions(context, client_id, dt, amount, coef):
#     steps.PartnerSteps.create_fake_product_completion(
#         dt,
#         client_id=client_id,
#         service_id=context.service.id,
#         service_order_id=0,
#         commission_sum=coef * amount,
#         type='disk_b2b'
#     )
#
#
# def test_diskachi_vvvvvvvvvvvvvvvvvvvery_simple():
#     client_id = steps.ClientSteps.create()
#     person_id = create_person(DISKACHI_CONTEXT, client_id)
#     client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
#         DISKACHI_CONTEXT,
#         client_id=client_id, person_id=person_id,
#         is_postpay=1, additional_params={'start_dt': CONTRACT_START_DT,
#                                          'offer_confirmation_type': 'no',
#                                          'personal_account': '1'
#
#                                          })
#
#     invoice_id, invoice_eid = steps.InvoiceSteps.get_invoice_ids(client_id)
#
#     # steps.ExportSteps.export_oebs(client_id=agent_client_id)
#     #
#     # steps.ExportSteps.export_oebs(person_id=agent_person_id)
#     # steps.ExportSteps.export_oebs(contract_id=agent_contract_id)
#     # steps.ExportSteps.export_oebs(invoice_id=agent_invoice_id)
#
#     default_amount = D('666.66')
#
#     payment_dt = datetime.now()
#
#     create_completions(DISKACHI_CONTEXT, client_id=client_id, dt=payment_dt, amount=default_amount, coef=1)
#
#     # Обратнопартнерский акта
#     steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, payment_dt)
#     act_data = steps.ActsSteps.get_act_data_by_client(client_id)
#
#     assert len(act_data) == 1
#     assert D(act_data[0]['amount']) == default_amount
