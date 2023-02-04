# coding: utf-8

from datetime import datetime
import pytest
from dateutil.relativedelta import relativedelta

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import Services, Paysyses, Products, PersonTypes, Firms, Regions
# from btestlib.data.defaults import GeneralPartnerContractDefaults as GenDefParams
from btestlib.matchers import equal_to
from balance import balance_steps
#
import balance.balance_api as api


from balance import balance_db as db
from balance import balance_steps as steps
from btestlib.constants import Currencies, Services, Products, PlaceType, \
    TransactionType, Export, OrderIDLowerBounds
from btestlib.data.defaults import Date
# from temp.igogor.balance_objects import Contexts, ContractCommissionType, ContractCreditType, ContractPaymentType
# DIRECT_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT
#
#
# login = 'yndx-static-yb-free-funds-'
# res = ''
# for i in range(1,11):
#     res += login + str(i) + ', '
# print res

# steps.CommonPartnerSteps.export_payment(4341951110)
# steps.CommonPartnerSteps.export_payment(4341951550)
# steps.CommonPartnerSteps.export_payment(4341363673)
# steps.CommonPartnerSteps.export_payment(4341364171)

# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code, params={'inn': '7710140679'})

# steps.ExportSteps.export_oebs(client_id=str(1349050428), person_id=13177230, contract_id=2443754,
#                               transaction_id=str(14862364476999))
# steps.ExportSteps.export_oebs(transaction_id=str(14862364509599))
# steps.ExportSteps.export_oebs(transaction_id=str(14862364550269))

# steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(1349050428, 2443754, datetime.today())
# steps.CommonPartnerSteps.generate_partner_acts_fair(3698868, datetime(2020,12,31))
# steps.CommonSteps.export(Export.Type.MONTH_PROC, Export.Classname.CLIENT, 1349179806)

# steps.ExportSteps.export_oebs(client_id=str(1349050428), person_id=13177230, contract_id=2443754,
#                               transaction_id=str(14862371957609))
# steps.ExportSteps.export_oebs(transaction_id=str(14862371977199))
# steps.ExportSteps.export_oebs(transaction_id=str(14862371978039))
# steps.ExportSteps.export_oebs(transaction_id=str(14862373388269))
#
# steps.ExportSteps.export_oebs(client_id=str(1349179806), person_id=13490371, contract_id=3698868,
#                               transaction_id=str(14862371960569))

# steps.ExportSteps.export_oebs(invoice_id=126309536 ,act_id=134111466)

# steps.ExportSteps.export_oebs(transaction_id=str(14862364550269))
# steps.ExportSteps.export_oebs(transaction_id=str(14862367163649))
# steps.ExportSteps.export_oebs(transaction_id=str(14862367142359))
# steps.ExportSteps.export_oebs(transaction_id=str(14862367572439))
# steps.ExportSteps.export_oebs(transaction_id=str(14862367572429))
# steps.ExportSteps.export_oebs(transaction_id=str(14862373388269))
# steps.ExportSteps.export_oebs(transaction_id=str(14862367473379))
# steps.ExportSteps.export_oebs(transaction_id=str(14862371978039))
# steps.ExportSteps.export_oebs(transaction_id=str(14862364476999))
# steps.ExportSteps.export_oebs(transaction_id=str(14862364599079))
# steps.ExportSteps.export_oebs(transaction_id=str(14862364587809))
# steps.ExportSteps.export_oebs(transaction_id=str(14862367473369))
# steps.ExportSteps.export_oebs(transaction_id=str(14862371977199))
# steps.ExportSteps.export_oebs(transaction_id=str(14862371957609))

# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)

# from balance import balance_steps as steps
# import datetime
# from btestlib.constants import PersonTypes
# from btestlib import utils
# from btestlib.constants import PersonTypes, Paysyses
# import balance.balance_db as db
# to_iso = utils.Date.date_to_iso_format
# NOW = datetime.datetime.now()
# client_id = steps.ClientSteps.create()
# db.balance().execute(
#     'UPDATE t_passport SET client_id=:client_id WHERE passport_id=4062172658',
# {'client_id': client_id})
# person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
# contract_id, _ = steps.ContractSteps.create_contract('no_agency_post',
#                                                      {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
#                                                       'DT': datetime.datetime(2020,11,16),
#                                                       'FINISH_DT': to_iso(datetime.datetime(2021,3,7)),
#                                                       'IS_SIGNED': to_iso(NOW),
#                                                       'SERVICES': [7],
#                                                       'COMMISSION_TYPE': 48,
#                                                       'NON_RESIDENT_CLIENTS': 0,
#                                                       'DEAL_PASSPORT': '2015-12-01T00:00:00',
#                                                       'REPAYMENT_ON_CONSUME': 0,
#                                                       'PERSONAL_ACCOUNT': 1,
#                                                       'LIFT_CREDIT_ON_PAYMENT': 0,
#                                                       'PERSONAL_ACCOUNT_FICTIVE': 1,
#                                                       'CREDIT_LIMIT_SINGLE': 6000
#                                                       })
# orders_list = []
# service_order_id = steps.OrderSteps.next_id(7)
# steps.OrderSteps.create(client_id, service_order_id, service_id=7, product_id=1475)
# orders_list.append(
#         {'ServiceID': 7, 'ServiceOrderID': service_order_id, 'Qty': 5, 'BeginDT':datetime.datetime.now() })
# request_id = steps.RequestSteps.create(client_id, orders_list)

import datetime
from datetime import timedelta

import pytest
import balance.balance_db as db
from hamcrest import equal_to
from decimal import Decimal
from dateutil.relativedelta import relativedelta
from btestlib.data.defaults import Date

import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance import balance_web as web
from balance.features import Features
from btestlib.constants import Services, Users
from temp.igogor.balance_objects import Contexts, Products, Firms, Paysyses, PersonTypes, ContractCommissionType, \
    Currencies


MAIN_DT = datetime.datetime.now()

DIRECT_CONTEXT_FIRM_1 = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                             contract_type=ContractCommissionType.OPT_CLIENT)
DIRECT_CONTEXT_FIRM_4 = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_INC_4,
                                                             person_type=PersonTypes.USU,
                                                             paysys=Paysyses.BANK_US_UR_USD,
                                                             contract_type=ContractCommissionType.USA_OPT_CLIENT)
DIRECT_CONTEXT_FIRM_7 = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.EUROPE_AG_7,
                                                             person_type=PersonTypes.SW_UR,
                                                             paysys=Paysyses.BANK_SW_UR_CHF,
                                                             contract_type=ContractCommissionType.SW_OPT_CLIENT)

TODAY = utils.Date.nullify_time_of_date(datetime.datetime.today())
CONTRACT_START_DT = utils.Date.date_to_iso_format(TODAY - relativedelta(months=4))
CONTRACT_END_DT = utils.Date.date_to_iso_format(TODAY + relativedelta(months=3))
ALMOST_OVERDUE_DT = utils.Date.date_to_iso_format(TODAY - relativedelta(months=3) - relativedelta(days=1))
ALMOST_OVERDUE_DT_NOT_RUB = utils.Date.date_to_iso_format(TODAY - relativedelta(months=3) - relativedelta(days=6))
OVERDUE_DT = utils.Date.date_to_iso_format(TODAY - relativedelta(months=3) - relativedelta(days=21))

CREDIT_LIMIT_RUB = Decimal('5700')
CREDIT_LIMIT_CHF_USD = Decimal('78')
QTY = Decimal('50')
# богомерзкий хардкод
OVERDUE_CHF = '22,08 CHF'
OVERDUE_USD = '20,50 USD'


SERVICES = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
            Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
            Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]

# DIRECT_CONTEXT_FIRM_7 = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.EUROPE_AG_7,
#                                                              person_type=PersonTypes.SW_UR,
#                                                              paysys=Paysyses.BANK_SW_UR_CHF,
#                                                              contract_type=ContractCommissionType.SW_OPT_CLIENT)
# currency = Currencies.CHF.num_code
# context = DIRECT_CONTEXT_FIRM_7
# client_id = 189162329
# person_id = 14428132
#
# contract_params = {'CLIENT_ID': client_id,
#                    'PERSON_ID': person_id,
#                    'DT': CONTRACT_START_DT,
#                    'FINISH_DT': CONTRACT_END_DT,
#                    'IS_SIGNED': CONTRACT_START_DT,
#                    'PAYMENT_TYPE': 3,
#                    'PAYMENT_TERM': 100,
#                    'CREDIT_TYPE': 2,
#                    'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT_CHF_USD,
#                    'SERVICES': SERVICES,
#                    'PERSONAL_ACCOUNT': 0,
#                    'LIFT_CREDIT_ON_PAYMENT': 0,
#                    'PERSONAL_ACCOUNT_FICTIVE': 0,
#                    'CURRENCY': str(currency),
#                    'FIRM': context.firm.id,
#                    }
#
# contract_id, contract_eid = steps.ContractSteps.create_contract_new(context.contract_type, contract_params)


# agency_id = 147562635
# client_id = steps.ClientSteps.create({'NAME': 'Клиент Баланса'})
# person_id = steps.PersonSteps.create(client_id, PersonTypes.YTPH.code, params={'is-partner': '1', 'yamoney_wallet': '123123123'})
# Создаём список заказов:
# orders_amount=2
# context = DIRECT_CONTEXT_FIRM_1
# order_owner = client_id
# invoice_owner = client_id
# service_order_id_list = []
# orders_list = []
# qty = Decimal('50')
# ORDER_DT = TODAY
# INVOICE_DT = TODAY
#
# for i in range(6,0,-1):
#     orders_list = []
#     for _ in xrange(orders_amount):
#         service_order_id = steps.OrderSteps.next_id(context.service.id)
#         steps.OrderSteps.create(order_owner, service_order_id, service_id=context.service.id, product_id=context.product.id,
#                                 params={'AgencyID': None, 'ManagerUID': context.manager.uid})
#         orders_list.append(
#             {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': ORDER_DT-relativedelta(months=i)})
#         service_order_id_list.append(service_order_id)
#
#     request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params=dict(InvoiceDesireDT=INVOICE_DT-relativedelta(months=i)))
#     invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id)
#     steps.InvoiceSteps.pay(invoice_id, payment_dt=INVOICE_DT-relativedelta(months=i), payment_sum=30 * qty * (i+1))
#     steps.InvoiceSteps.set_turn_on_dt(invoice_id, INVOICE_DT-relativedelta(months=i))
#
#     steps.CampaignsSteps.do_campaigns(context.product.service.id, orders_list[0]['ServiceOrderID'],
#                                               {'Bucks': 10 + 10*i, 'Money': 0}, 0, INVOICE_DT-relativedelta(months=i))
#     steps.ActsSteps.generate(client_id, force=1, date=INVOICE_DT-relativedelta(months=i))

# steps.InvoiceSteps.pay(137485540)
# api.medium().GetClientPersons(1351417510, 1)
# steps.ClientSteps.get_client_persons(1351417510)

# steps.ExportSteps.export_oebs(client_id=1351354973)
# steps.ExportSteps.export_oebs(person_id=15622767)
# steps.ExportSteps.export_oebs(person_id=15622978)
# steps.ExportSteps.export_oebs(person_id=15622979)
# steps.ExportSteps.export_oebs(person_id=15623068)
# steps.ExportSteps.export_oebs(person_id=15624212)
# steps.ExportSteps.export_oebs(person_id=15624448)
# steps.ExportSteps.export_oebs(client_id=1350942326)
# steps.ExportSteps.export_oebs(person_id=15624635)
# steps.ExportSteps.export_oebs(person_id=15624621)
# steps.ExportSteps.export_oebs(person_id=15624622)
# steps.ExportSteps.export_oebs(client_id=1350942769)
# steps.ExportSteps.export_oebs(person_id=15624637)
# steps.ExportSteps.export_oebs(client_id=1350942802)
# steps.ExportSteps.export_oebs(person_id=15624623)
# steps.ExportSteps.export_oebs(person_id=15624681)
# steps.ExportSteps.export_oebs(client_id=7084345)
# steps.ExportSteps.export_oebs(person_id=2416610)
# steps.ExportSteps.export_oebs(client_id=1350943724)
# steps.ExportSteps.export_oebs(person_id=15625130)
# #

to_iso = utils.Date.date_to_iso_format
START_DT = datetime.datetime(year=2020, month=1, day=1)
NOW = datetime.datetime.now()
# def create_request(client_id):
#     service_order_id_list = []
#     orders_list = []
#
#     for _ in xrange(2):
#         service_order_id = steps.OrderSteps.next_id(Services.TOLOKA.id)
#         steps.OrderSteps.create(client_id, service_order_id, service_id=Services.TOLOKA.id,
#                                 product_id=507130)
#         orders_list.append(
#             {'ServiceID': Services.TOLOKA.id, 'ServiceOrderID': service_order_id, 'Qty': 10, 'BeginDT': NOW})
#         service_order_id_list.append(service_order_id)
#
#     # Создаём риквест
#     request_id = steps.RequestSteps.create(client_id, orders_list,
#                                            additional_params=dict(InvoiceDesireDT=NOW))
#     # invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=credit,
#     #                                                        contract_id=contract_id)
#     # steps.CampaignsSteps.do_campaigns(context.product.service.id, orders_list[0]['ServiceOrderID'],
#     #                                   {context.product.type.code: completions}, 0, completions_dt)
#     # # Выставляем акт
#     # steps.ActsSteps.generate(client_id, force=1, date=act_dt)
#     # act_id = steps.ActsSteps.get_all_act_data(client_id)[act_num]['id']
#     return request_id
#
#
# client_id_sw_yt_no_contract = steps.ClientSteps.create()
# person_id_sw_yt_no_contract = steps.PersonSteps.create(client_id_sw_yt_no_contract, PersonTypes.SW_YT.code)
# request_id_sw_yt_no_contract = create_request(client_id_sw_yt_no_contract)
#
#
# client_id_sw_yt_not_postpay = steps.ClientSteps.create()
# person_id_sw_yt_not_postpay = steps.PersonSteps.create(client_id_sw_yt_not_postpay, PersonTypes.SW_YT.code)
# contract_params = {'DT': to_iso(START_DT),
#                        'PAYMENT_TYPE': 2,
#                        # 'PAYMENT_TERM': 100,
#                        # 'CREDIT_TYPE': 1,
#                        # 'CREDIT_LIMIT_SINGLE': 123,
#                        'SERVICES': [Services.TOLOKA.id],
#                        'CURRENCY': str(Currencies.USD.num_code),
#                        'FIRM': Firms.SERVICES_AG_16.id,
#                                                       'IS_SIGNED': to_iso(NOW),
#                        }
# contract_params.update({'CLIENT_ID': client_id_sw_yt_not_postpay, 'PERSON_ID': person_id_sw_yt_not_postpay})
# contract_id_sw_yt_not_postpay, contract_eid_sw_yt_not_postpay = steps.ContractSteps.create_contract_new(ContractCommissionType.SW_OPT_CLIENT, contract_params)
# request_id_sw_yt_not_postpay = create_request(client_id_sw_yt_not_postpay)
#
#
# client_id_sw_yt_postpay = steps.ClientSteps.create()
# person_id_sw_yt_postpay = steps.PersonSteps.create(client_id_sw_yt_postpay, PersonTypes.SW_YT.code)
# contract_params = {'DT': to_iso(START_DT),
#                        'PAYMENT_TYPE': 3,
#                        'PAYMENT_TERM': 100,
#                        'CREDIT_TYPE': 1,
#                        'CREDIT_LIMIT_SINGLE': 123123,
#                        'SERVICES': [Services.TOLOKA.id],
#                        'CURRENCY': str(Currencies.USD.num_code),
#                        'FIRM': Firms.SERVICES_AG_16.id,
#                                                       'IS_SIGNED': to_iso(NOW),
#
#                        }
# contract_params.update({'CLIENT_ID': client_id_sw_yt_postpay, 'PERSON_ID': person_id_sw_yt_postpay})
# contract_id_sw_yt_postpay, contract_eid_sw_yt_postpay = steps.ContractSteps.create_contract_new(ContractCommissionType.SW_OPT_CLIENT, contract_params)
# request_id_sw_yt_postpay = create_request(client_id_sw_yt_postpay)
#
#
# client_id_sw_ph_no_contract = steps.ClientSteps.create()
# person_id_sw_ph_no_contract = steps.PersonSteps.create(client_id_sw_ph_no_contract, PersonTypes.SW_PH.code)
# request_id_sw_ph_no_contract = create_request(client_id_sw_ph_no_contract)
#
# client_id_sw_ph_not_postpay = steps.ClientSteps.create()
# person_id_sw_ph_not_postpay = steps.PersonSteps.create(client_id_sw_ph_not_postpay, PersonTypes.SW_PH.code)
# contract_params = {'DT': to_iso(START_DT),
#                        'PAYMENT_TYPE': 2,
#                        # 'PAYMENT_TERM': 100,
#                        # 'CREDIT_TYPE': 1,
#                        # 'CREDIT_LIMIT_SINGLE': 123,
#                        'SERVICES': [Services.TOLOKA.id],
#                        'CURRENCY': str(Currencies.USD.num_code),
#                        'FIRM': Firms.SERVICES_AG_16.id,
#                                                       'IS_SIGNED': to_iso(NOW),
#                        }
# contract_params.update({'CLIENT_ID': client_id_sw_ph_not_postpay, 'PERSON_ID': person_id_sw_ph_not_postpay})
# contract_id_sw_ph_not_postpay, contract_eid_sw_ph_not_postpay = steps.ContractSteps.create_contract_new(ContractCommissionType.SW_OPT_CLIENT, contract_params)
# request_id_sw_ph_not_postpay = create_request(client_id_sw_ph_not_postpay)
#
#
# client_id_sw_ph_postpay = steps.ClientSteps.create()
# person_id_sw_ph_postpay = steps.PersonSteps.create(client_id_sw_ph_postpay, PersonTypes.SW_PH.code)
# contract_params = {'DT': to_iso(START_DT),
#                        'PAYMENT_TYPE': 3,
#                        'PAYMENT_TERM': 100,
#                        'CREDIT_TYPE': 1,
#                        'CREDIT_LIMIT_SINGLE': 123123,
#                        'SERVICES': [Services.TOLOKA.id],
#                        'CURRENCY': str(Currencies.USD.num_code),
#                        'FIRM': Firms.SERVICES_AG_16.id,
#                                                       'IS_SIGNED': to_iso(NOW),
#                        }
# contract_params.update({'CLIENT_ID': client_id_sw_ph_postpay, 'PERSON_ID': person_id_sw_ph_postpay})
# contract_id_sw_ph_postpay, contract_eid_sw_ph_postpay = steps.ContractSteps.create_contract_new(ContractCommissionType.SW_OPT_CLIENT, contract_params)
# request_id_sw_ph_postpay = create_request(client_id_sw_ph_postpay)
#
#
# client_id_sw_ur_no_contract = steps.ClientSteps.create()
# person_id_sw_ur_no_contract = steps.PersonSteps.create(client_id_sw_ur_no_contract, PersonTypes.SW_UR.code)
# request_id_sw_ur_no_contract = create_request(client_id_sw_ur_no_contract)
#
# client_id_sw_ur_not_postpay = steps.ClientSteps.create()
# person_id_sw_ur_not_postpay = steps.PersonSteps.create(client_id_sw_ur_not_postpay, PersonTypes.SW_UR.code)
# contract_params = {'DT': to_iso(START_DT),
#                        'PAYMENT_TYPE': 2,
#                        # 'PAYMENT_TERM': 100,
#                        # 'CREDIT_TYPE': 1,
#                        # 'CREDIT_LIMIT_SINGLE': 123123,
#                        'SERVICES': [Services.TOLOKA.id],
#                        'CURRENCY': str(Currencies.USD.num_code),
#                        'FIRM': Firms.SERVICES_AG_16.id,
#                                                       'IS_SIGNED': to_iso(NOW),
#                        }
# contract_params.update({'CLIENT_ID': client_id_sw_ur_not_postpay, 'PERSON_ID': person_id_sw_ur_not_postpay})
# contract_id_sw_ur_not_postpay, contract_eid_sw_ur_not_postpay = steps.ContractSteps.create_contract_new(ContractCommissionType.SW_OPT_CLIENT, contract_params)
# request_id_sw_ur_not_postpay = create_request(client_id_sw_ur_not_postpay)
#
# client_id_sw_ur_postpay = steps.ClientSteps.create()
# person_id_sw_ur_postpay = steps.PersonSteps.create(client_id_sw_ur_postpay, PersonTypes.SW_UR.code)
# contract_params = {'DT': to_iso(START_DT),
#                        'PAYMENT_TYPE': 3,
#                        'PAYMENT_TERM': 100,
#                        'CREDIT_TYPE': 1,
#                        'CREDIT_LIMIT_SINGLE': 123123,
#                        'SERVICES': [Services.TOLOKA.id],
#                        'CURRENCY': str(Currencies.USD.num_code),
#                        'FIRM': Firms.SERVICES_AG_16.id,
#                                                       'IS_SIGNED': to_iso(NOW),
#                        }
# contract_params.update({'CLIENT_ID': client_id_sw_ur_postpay, 'PERSON_ID': person_id_sw_ur_postpay})
# contract_id_sw_ur_postpay, contract_eid_sw_ur_postpay = steps.ContractSteps.create_contract_new(ContractCommissionType.SW_OPT_CLIENT, contract_params)
# request_id_sw_ur_postpay = create_request(client_id_sw_ur_postpay)
#
#
# client_id_sw_ytph_no_contract = steps.ClientSteps.create()
# person_id_sw_ytph_no_contract = steps.PersonSteps.create(client_id_sw_ytph_no_contract, PersonTypes.SW_YTPH.code)
# request_id_sw_ytph_no_contract = create_request(client_id_sw_ytph_no_contract)
#
#
# client_id_sw_ytph_not_postpay = steps.ClientSteps.create()
# person_id_sw_ytph_not_postpay = steps.PersonSteps.create(client_id_sw_ytph_not_postpay, PersonTypes.SW_YTPH.code)
# contract_params = {'DT': to_iso(START_DT),
#                        'PAYMENT_TYPE': 2,
#                        # 'PAYMENT_TERM': 100,
#                        # 'CREDIT_TYPE': 1,
#                        # 'CREDIT_LIMIT_SINGLE': 123123,
#                        'SERVICES': [Services.TOLOKA.id],
#                        'CURRENCY': str(Currencies.USD.num_code),
#                        'FIRM': Firms.SERVICES_AG_16.id,
#                                                       'IS_SIGNED': to_iso(NOW),
#                        }
# contract_params.update({'CLIENT_ID': client_id_sw_ytph_not_postpay, 'PERSON_ID': person_id_sw_ytph_not_postpay})
# contract_id_sw_ytph_not_postpay, contract_eid_sw_ytph_not_postpay = steps.ContractSteps.create_contract_new(ContractCommissionType.SW_OPT_CLIENT, contract_params)
# request_id_sw_ytph_not_postpay = create_request(client_id_sw_ytph_not_postpay)
#
# client_id_sw_ytph_postpay = steps.ClientSteps.create()
# person_id_sw_ytph_postpay = steps.PersonSteps.create(client_id_sw_ytph_postpay, PersonTypes.SW_YTPH.code)
# contract_params = {'DT': to_iso(START_DT),
#                        'PAYMENT_TYPE': 3,
#                        'PAYMENT_TERM': 100,
#                        'CREDIT_TYPE': 1,
#                        'CREDIT_LIMIT_SINGLE': 123123,
#                        'SERVICES': [Services.TOLOKA.id],
#                        'CURRENCY': str(Currencies.USD.num_code),
#                        'FIRM': Firms.SERVICES_AG_16.id,
#                                                       'IS_SIGNED': to_iso(NOW),
#                        }
# contract_params.update({'CLIENT_ID': client_id_sw_ytph_postpay, 'PERSON_ID': person_id_sw_ytph_postpay})
# contract_id_sw_ytph_postpay, contract_eid_sw_ytph_postpay = steps.ContractSteps.create_contract_new(ContractCommissionType.SW_OPT_CLIENT, contract_params)
# request_id_sw_ytph_postpay = create_request(client_id_sw_ytph_postpay)
#
# print 'sw_ph без договора, клиент {}, плательщик {}, реквест {}'.format(client_id_sw_ph_no_contract, person_id_sw_ph_no_contract, request_id_sw_ph_no_contract)
# print 'sw_ph предоплата, клиент {}, плательщик {}, договор {}, реквест {}'.format(client_id_sw_ph_not_postpay, person_id_sw_ph_not_postpay, contract_id_sw_ph_not_postpay, request_id_sw_ph_not_postpay)
# print 'sw_ph постоплата, клиент {}, плательщик {}, договор {}, реквест {}'.format(client_id_sw_ph_postpay, person_id_sw_ph_postpay, contract_id_sw_ph_postpay, request_id_sw_ph_postpay)
#
# print 'sw_ur без договора, клиент {}, плательщик {}, реквест {}'.format(client_id_sw_ur_no_contract, person_id_sw_ur_no_contract, request_id_sw_ur_no_contract)
# print 'sw_ur предоплата, клиент {}, плательщик {}, договор {}, реквест {}'.format(client_id_sw_ur_not_postpay, person_id_sw_ur_not_postpay, contract_id_sw_ur_not_postpay, request_id_sw_ur_not_postpay)
# print 'sw_ur постоплата, клиент {}, плательщик {}, договор {}, реквест {}'.format(client_id_sw_ur_postpay, person_id_sw_ur_postpay, contract_id_sw_ur_postpay, request_id_sw_ur_postpay)
#
# print 'sw_ytph без договора, клиент {}, плательщик {}, реквест {}'.format(client_id_sw_ytph_no_contract, person_id_sw_ytph_no_contract, request_id_sw_ytph_no_contract)
# print 'sw_ytph предоплата, клиент {}, плательщик {}, договор {}, реквест {}'.format(client_id_sw_ytph_not_postpay, person_id_sw_ytph_not_postpay, contract_id_sw_ytph_not_postpay, request_id_sw_ytph_not_postpay)
# print 'sw_ytph постоплата, клиент {}, плательщик {}, договор {}, реквест {}'.format(client_id_sw_ytph_postpay, person_id_sw_ytph_postpay, contract_id_sw_ytph_postpay, request_id_sw_ytph_postpay)
#
# print 'sw_yt без договора, клиент {}, плательщик {}, реквест {}'.format(client_id_sw_yt_no_contract, person_id_sw_yt_no_contract, request_id_sw_yt_no_contract)
# print 'sw_yt предоплата, клиент {}, плательщик {}, договор {}, реквест {}'.format(client_id_sw_yt_not_postpay, person_id_sw_yt_not_postpay, contract_id_sw_yt_not_postpay, request_id_sw_yt_not_postpay)
# print 'sw_yt постоплата, клиент {}, плательщик {}, договор {}, реквест {}'.format(client_id_sw_yt_postpay, person_id_sw_yt_postpay, contract_id_sw_yt_postpay, request_id_sw_yt_postpay)
# person_id_2 = steps.PersonSteps.create(client_id, PersonTypes.UR.code, {'inn': 7838298476})

# import datetime
# from balance import balance_steps as steps
# from btestlib.data.partner_contexts import CLOUD_KZ_CON        TEXT
# context = CLOUD_KZ_CONTEXT
# product_id = 509071
# client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, is_offer=1)
# service_order_id = steps.OrderSteps.next_id(context.service.id)
# steps.OrderSteps.create(client_id, service_order_id, product_id=product_id,
#                                 service_id=context.service.id)
# service_order_id_1 = steps.OrderSteps.next_id(context.service.id)
# steps.OrderSteps.create(client_id, service_order_id_1, product_id=product_id,
#                                 service_id=context.service.id)
# service_order_id_2 = steps.OrderSteps.next_id(context.service.id)
# steps.OrderSteps.create(client_id, service_order_id_2, product_id=product_id,
#                                 service_id=context.service.id)
# orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 10.5,
#                     'BeginDT': datetime.datetime.now()},
#                {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_1, 'Qty': 130.5,
#                 'BeginDT': datetime.datetime.now()},
#                {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_2, 'Qty': 1330.5,
#                     'BeginDT': datetime.datetime.now()}
#                ]
# request_id = steps.RequestSteps.create(client_id, orders_list,
#                                            additional_params={'InvoiceDesireDT': datetime.datetime.now(),
#                                                               'InvoiceDesireType': 'charge_note'})
# paysys_id = api.medium().GetRequestChoices({'OperatorUid': 16571028,
#                                                     'RequestID': request_id})['paysys_list'][0]['id']
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id,
#                                                  credit=0, contract_id=contract_id)
# client_id = steps.ClientSteps.create()
# for i in range(13):
#     person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)

# invoice_id, external_id, _ = steps.InvoiceSteps.create(3826135855, 16529936, 1003)


# api.medium().GetClientPersons(1352593898)

# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(1352639600, PersonTypes.SW_UR.code)
# api.medium().ImportPromoCodes([{
#         'StartDt': datetime.datetime.now().strftime('%Y-%m-%mT%H:%M:%S'),
#         'EndDt': datetime.datetime(2100, 1, 1),  # бессрочный
#         # количество дней резервации промокода за клиентом
#         # от даты резервации до min((reservation_dt + reservation_days), end_dt)
#         'ReservationDays': 90,
#         'CalcClassName': 'FixedDiscountPromoCodeGroup',  # варьируемый параметр
#         'CalcParams': {  # варьируемый параметр
#             # adjust_quantity и apply_on_create общие для всех типов промокодов
#             'adjust_quantity': 0,  # увеличиваем количество (иначе уменьшаем сумму)
#             'apply_on_create': 1,  # применяем при создании счёта иначе при включении (оплате)
#             # остальные зависят от типа
#             'discount_pct': 15
#         },
#         'Promocodes': [
#
#             {"code": "YAVO-DITE-LNLO-4444"},  # при указании клиента промокод будет доступен только ему
#         ],
#         'MinimalAmounts': {'RUB': 10},  # минимальная сумма счёта при которой применится промокод
#         'FirmId': 1,
#         'TicketId': 'FAKE-1',  # по возможности указывайте тикет в рамках которого создаете промокод
#         'ServiceIds': [7],  # сервисы, для которых будут доступны промокоды
#         'ProductIds': [1475], # продукты, для которых доступны промокоды
#         'NewClientsOnly': 1,  # промокоды доступны только для новых клиентов (у клиента нет актов в указанном сервисе за последние 12 месяцев)
#         'ValidUntilPaid': 0,  # может быть использовать 1 клиентом в 1 счёте
#         'IsGlobalUnique': 1,  # может быть использован только в 1 счёте
#         'NeedUniqueUrls': 0,  # нужны уникальные url (текущий url не фигурирует в актах сервиса за последние 12 месяцев)
#         'SkipReservationCheck': 1,  # в момент резервации не проверяет был ли привязан промокод к этому же клиенту ранее
#     }])


# steps.ExportSteps.export_oebs(contract_id=14349833)
# steps.ExportSteps.export_oebs(contract_id=14263066)

# client_id = steps.ClientSteps.create()
# steps.PersonSteps.create(client_id, PersonTypes.UR.code, {'ownership_type': 'SELFEMPLOYED'})
# steps.PersonSteps.create(client_id, PersonTypes.UR.code)
# steps.PersonSteps.create(client_id, PersonTypes.UR.code, {'is-partner': '1'})
# steps.PersonSteps.create(client_id, PersonTypes.UR.code, {'is-partner': '1'})
# steps.PersonSteps.create(client_id, PersonTypes.PH.code)
# steps.PersonSteps.create(client_id, PersonTypes.PH.code)
# steps.PersonSteps.create(client_id, PersonTypes.PH.code, {'is-partner': '1'})
# steps.PersonSteps.create(client_id, PersonTypes.PH.code, {'is-partner': '1'})
#
#
# from  balance.real_builders.common_defaults import FIXED_UR_PARAMS
# client_id = steps.ClientSteps.create()
# # steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY',
# #                                       dt=datetime.datetime.today()-timedelta(days=1),
# #                                        currency='USD', region_id=29387
# #                                        )
# person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code, params = FIXED_UR_PARAMS)
# service_order_id = steps.OrderSteps.next_id(Services.DIRECT.id)
# steps.OrderSteps.create(client_id, service_order_id, product_id=1475,
#                                 service_id=Services.DIRECT.id)
# orders_list = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id, 'Qty': 10,
#                     'BeginDT': datetime.datetime.now()}]
# request_id = steps.RequestSteps.create(client_id, orders_list)
# invoice_id_1, _, _ = steps.InvoiceSteps.create(request_id, person_id, Paysyses.BANK_UR_RUB.id)
#
# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code, params = FIXED_UR_PARAMS)
# service_order_id = steps.OrderSteps.next_id(Services.DIRECT.id)
# steps.OrderSteps.create(client_id, service_order_id, product_id=1475,
#                                 service_id=Services.DIRECT.id)
# orders_list = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id, 'Qty': 10,
#                     'BeginDT': datetime.datetime.now()}]
# request_id = steps.RequestSteps.create(client_id, orders_list)
# invoice_id_2, _, _ = steps.InvoiceSteps.create(request_id, person_id, Paysyses.BANK_UR_RUB.id)
#
# steps.InvoiceSteps.pay(invoice_id_1, payment_sum=30000)

# steps.InvoiceSteps.pay_fair(invoice_id=143653442, payment_sum=10000)
# steps.OverdraftSteps.run_overdraft_ban(1353854164)

# steps.CampaignsSteps.do_campaigns(7, 56777682,
#                                       {'Bucks': 90, 'Money': 0}, 0, datetime.datetime(2021,07,20))

# steps.ActsSteps.generate(client_id=7453165, date=datetime.datetime(2021, 8, 1))

# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
# client_id = steps.ClientSteps.create()
# contract_params = {'DT': to_iso(START_DT),
#                        'PAYMENT_TYPE': 3,
#                        'PAYMENT_TERM': 100,
#                        'CREDIT_TYPE': 1,
#                        'CREDIT_LIMIT_SINGLE': 123123,
#                        'SERVICES': [Services.DIRECT.id],
#                        'CURRENCY': str(Currencies.RUB.num_code),
#                        'FIRM': Firms.YANDEX_1.id,
#                                                       'IS_SIGNED': to_iso(NOW),
#                        }
# contract_params.update({'CLIENT_ID': client_id, 'PERSON_ID': person_id})
# contract_id, _ = steps.ContractSteps.create_contract_new(ContractCommissionType.NO_AGENCY, contract_params)

# steps.ExportSteps.export_oebs(client_id=1353956255, person_id=17624187, contract_id=15012496)
# client_id= steps.ClientSteps.create()
# steps.PersonSteps.create(client_id, PersonTypes.UR.code, {'is-partner': 1})
# steps.PersonSteps.create(client_id, PersonTypes.PH.code, {'is-partner': 1})


# a = 1
# steps.InvoiceSteps.create_cash_payment_fact(
#     invoice_eid=u'Б-3831622456-1',
#     amount=-3000,
#     dt=datetime.datetime.now(),
#     type='ACTIVITY',
#     orig_id=6614130191,
#     invoice_id=146988038
# )
#
# a = 2
# steps.InvoiceSteps.create_cash_payment_fact(
#     invoice_eid=u'Б-3831622491-1',
#     amount=3000,
#     dt=datetime.datetime.now(),
#     type='ACTIVITY',
#     orig_id=6614130191,
#     invoice_id=146988220
# )

# for i in range(30):
#     invoice_transfer_id = api.test_balance().CreateInvoiceTransfer(Users.YB_ADM.uid, 146987684, 146987581, 1, False)


# steps.OverdraftSteps.run_overdraft_ban(1355083272)

# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, PersonTypes.SW_UR.code, full=True, params={'purchase_order': 'po12'})
# person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code, full=True, params={'inn': '','is-partner': '1'})
# steps.ClientSteps.link(client_id, 'yndx-yuelyasheva')

# api.medium().CreatePerson(Users.YB_ADM.uid, {'id': 19751245, 'purchase_order': '123', 'client_id': 1355903540, 'type': 'usp'})

client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code, full=True, params={'is-partner': 1})
# steps.ClientSteps.link(client_id, 'yndx-balance-assessor-100')
api.medium().CreatePerson(Users.TESTUSER_BALANCE1.uid, {'id': person_id, 'live-signature': '0', 'client_id': client_id, 'type': 'ur'})