# -*- coding: utf-8 -*-

import datetime
import time
from decimal import Decimal as D

from balance import balance_steps as steps
from balance.balance_objects import Product
from btestlib import utils
from btestlib.constants import Services, PersonTypes
from temp.igogor.balance_objects import Contexts

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
ORDER_DT = NOW
INVOICE_DT = NOW
COMPLETIONS_DT = NOW
ACT_DT = NOW

context = Contexts.DIRECT_FISH_SW_EUR_CONTEXT # DIRECT_FISH_RUB_CONTEXT
QTY = D('0.1')
COMPLETIONS = D('100')

# Создаём клиента
# client_id = 10
# client_id = steps.ClientSteps.create(params={'login': 'scientistnik'})
client_id = 1354192629

# Сделать клиента НЕрезидентом:
# query = "Update t_client set FULLNAME = u'UL Nonres', CURRENCY_PAYMENT = 'USD', ISO_CURRENCY_PAYMENT = 'USD', IS_NON_RESIDENT = 1 where ID = :client_id"
# query_params = {'client_id': client_id}
# steps.sql(query, query_params)

# Перевести клиента на мультивалютность
# steps.ClientSteps.migrate_to_currency(client_id, 'COPY')

# Выдать клиенту овердрафт:
# steps.ClientSteps.set_force_overdraft(client_id, 7, 1000)

# Создаём агентство:
agency_id = 1354192282 # steps.ClientSteps.create({'IS_AGENCY': 1})
# agency_id = None

# Далее в скрипте будут фигурировать "владелец счёта": агентство или клиент и "владелей заказа": всегда клиент:
order_owner = client_id
invoice_owner = agency_id or client_id

# Привязать клиента к логину
# steps.ClientSteps.link(client_id, 'scientistnik')

# Создаём плательщика
person_params = {}
# person_params = {
#             "city": "city QtrIf",
#             "file": "/testDocs.txt",
#             "fname": "MTL_sw_ytph",
#             "verified-docs": "1",
#             "region": "21595",
#             "fax": "+41 32 2814442",
#             "email": "Hc`F@lmsS.Sta",
#             "lname": u"Физ. лицо-нерезидент, Швейцарияzbey",
#             "phone": "+41 32 9126742",
#             "postcode": "53431",
#             "postaddress": u"Улица 3",
#             "country_id": "225",
#         }
# person_params = {'fias_guid': '29251dcf-00a1-4e34-98d4-5c47484a36d4'}
person_id = 17624166 # steps.PersonSteps.create(invoice_owner, context.person_type.code, person_params)

# Создаём договор:
# contract_id, _ = steps.ContractSteps.create_contract('opt_agency_prem_post',
#                                                      {'CLIENT_ID': client_id,
#                                                      'PERSON_ID': person_id,
#                                                       'FIRM': '1',
#                                                       'CURRENCY': '810',  # RUR
#                                                       'BANK_DETAILS_ID': '7627',  # АО Юникредит Банк
#                                                       'MANAGER_CODE': '28133',  # Яковенко Екатерина Сергеевна
#                                                       'MANAGER_BO_CODE': '30726',  # Артельная Анна Витальевна
#                                                       'DT': to_iso(datetime.datetime(2020, 3, 1)),
#                                                       'FINISH_DT': to_iso(datetime.datetime(2022, 3, 1)),
#                                                       'UNILATERAL': '1',
#                                                       'TICKETS': 'BALANCEDUTY-1438',
#                                                       # 'IS_SIGNED': to_iso(datetime.datetime(2020, 3, 1)),
#                                                       'SERVICES': [Services.DIRECT.id],
#                                                       # 'PRINT_TEMPLATE': '/sales/processing/Billing-agreements/YandexGSAP/opt/premium/2/',
#                                                       'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': '2',  # Премиум 2015
#                                                       'CREDIT_TYPE': '1',  # по сроку
#                                                       'PAYMENT_TERM': '45',  # 45 дней
#                                                       'CALC_DEFERMANT': '0',  # от даты акта
#                                                       # # 'COMMISSION_TYPE': 48,
#                                                       # # 'NON_RESIDENT_CLIENTS': 0,
#                                                       # # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
#                                                       # 'REPAYMENT_ON_CONSUME': 0,
#                                                       # 'PERSONAL_ACCOUNT': 1,
#                                                       # 'LIFT_CREDIT_ON_PAYMENT': 0,
#                                                       # 'PERSONAL_ACCOUNT_FICTIVE': 1,
#                                                       'CREDIT_LIMIT_SINGLE': '1158648553',
#                                                       })
contract_id = 15012340 # 14989342

# Создаём доп.соглашение:
# steps.ContractSteps.create_collateral(1033, {'contract2_id': contract_id, 'dt': '2015-04-30T00:00:00',
#                                              'is_signed': '2015-01-01T00:00:00'})

# Создаём список заказов:
orders_list = []

for _ in xrange(1):
    service_order_id = 67228854 # steps.OrderSteps.next_id(context.service.id)
    # order_id = steps.OrderSteps.create(
    #     order_owner,
    #     service_order_id,
    #     service_id=context.service.id,
    #     product_id=context.product.id,
    #     params={'AgencyID': agency_id, 'ManagerUID': context.manager.uid},
    # )
    # steps.InvoiceSteps.pay_with_certificate_or_compensation(order_id, QTY, 'ce')

    orders_list.append(
        {
            'ServiceID': context.service.id,
            'ServiceOrderID': service_order_id,
            'Qty': QTY,
            'BeginDT': ORDER_DT,
        },
    )


# Создаём реквест
request_id = 3829793871 # steps.RequestSteps.create(invoice_owner, orders_list, additional_params=dict(InvoiceDesireDT=INVOICE_DT))

# Выставляем счёт
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
#                                              contract_id=contract_id, overdraft=0, endbuyer_id=None)
invoice_id = 146326281

# Оплачиваем счёт "быстрым" способом
# steps.InvoiceSteps.pay_fair(invoice_id)

# # Отправляем честные открутки:
# # steps.CampaignsSteps.update_campaigns(product.service_id, service_order_id, {'Bucks': 99.99, 'Money': 0}, 0, BASE_DT)

# Отправляем НЕчестные открутки:
# order_count = len(orders_list)
# for o in orders_list:
#     steps.CampaignsSteps.do_campaigns(
#         o['ServiceID'],
#         o['ServiceOrderID'],
#         {'Bucks': COMPLETIONS, 'Money': COMPLETIONS},
#         # {'Days': 20300},
#         0,
#         datetime.datetime.now(),
#     )

# # Выставляем акт
# steps.ActsSteps.generate(client_id, force=1, date=datetime.datetime.now())
# steps.ActsSteps.generate(agency_id, force=1, date=datetime.datetime.now())
# pass

#steps.ExportSteps.export_oebs(client_id=agency_id)

# steps.ExportSteps.export_oebs(person_id=12163363)
# steps.ExportSteps.export_oebs(invoice_id=114080352)
# надо чтобы или открыли периоды, или если дата не важна, замени ее и external_id (если акт уже выгружался в oebs)
# steps.ExportSteps.export_oebs(act_id=119053610)

# select * from bo.t_export
# where type in ('OEBS', 'OEBS_API')
# and ((classname='Act' and object_id=119053610)
# or (classname='Client' and object_id=135006670)
# or (classname='Person' and object_id=12163363)
# or (classname='Product' and object_id=2136)
# or (classname='Invoice' and object_id=114080352))
# ;


RETRO_DSC = 10
act_id = 183574733

fact_retrodsc = steps.ActsSteps.get_act_retrodiscount(act_id)
# if is_retrodsc:

# act_amt = steps.ActsSteps.get_act_amount_by_act_id(act_id)[0]
# calc_retro = D(act_amt['amount']) * RETRO_DSC / 100
calc_retro = 0

print(calc_retro, fact_retrodsc['retrodsc'])
# utils.check_that(fact_retrodsc['retrodsc'], equal_to(str(calc_retro)))
# else:
#    utils.check_that(fact_retrodsc, equal_to(None))