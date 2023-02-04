# -*- coding: utf-8 -*-

import datetime
import time
from decimal import Decimal as D

from balance import balance_db as db
from balance import balance_steps as steps
from balance.balance_objects import Product
from btestlib import utils
from btestlib.constants import Services, PersonTypes, Paysyses
from temp.igogor.balance_objects import Contexts
from btestlib import reporter

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

context = Contexts.DIRECT_MONEY_RUB_CONTEXT
QTY = D('100')
COMPLETIONS = D('50')

# Создаём клиента
# client_id = 2214487
client_id = steps.ClientSteps.create(params={'login': 'nataberskam', 'REGION_ID': 225})

# Сделать клиента НЕрезидентом:
# query = "Update t_client set FULLNAME = u'UL Nonres', CURRENCY_PAYMENT = 'USD', ISO_CURRENCY_PAYMENT = 'USD', IS_NON_RESIDENT = 1 where ID = :client_id"
# query_params = {'client_id': client_id}
# steps.sql(query, query_params)

# Перевести клиента на мультивалютность
# steps.ClientSteps.migrate_to_currency(client_id, 'COPY')

# Выдать клиенту овердрафт:
# steps.ClientSteps.set_force_overdraft(client_id, 7, 1000)

# Создаём агентство:
# agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
agency_id = None

# Далее в скрипте будут фигурировать "владелец счёта": агентство или клиент и "владелей заказа": всегда клиент:
order_owner = client_id
invoice_owner = agency_id or client_id

# Привязать клиента к логину
# steps.ClientSteps.link(client_id, 'nataberskam')

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
# person_id = None or steps.PersonSteps.create(invoice_owner, context.person_type.code, person_params)
person_id = steps.PersonSteps.create(invoice_owner, PersonTypes.PH.code, person_params)
# person_id = steps.PersonSteps.create(invoice_owner, PersonTypes.UR.code, person_params)

# Создаём договор:
# contract_id, _ = steps.ContractSteps.create_contract('opt_prem_post',
#                                                      {'CLIENT_ID': client_id,
#                                                      'PERSON_ID': person_id,
#                                                       'FIRM': '1085',
#                                                       'CURRENCY': '840',
#                                                       'MANAGER_CODE': '28133',  # Яковенко Екатерина Сергеевна
#                                                       'DT': to_iso(datetime.datetime(2022, 01, 01)),
#                                                       'FINISH_DT': to_iso(datetime.datetime(2022, 06, 01)),
#                                                       'TICKETS': 'AAA-111',
#                                                       })
# тип 18, 19
contract_id = None

# Создаём доп.соглашение:
# steps.ContractSteps.create_collateral(1033, {'contract2_id': contract_id, 'dt': '2015-04-30T00:00:00',
#                                              'is_signed': '2015-01-01T00:00:00'})

# Создаём список заказов:
# orders_list = [{'ServiceID': 7, 'ServiceOrderID': 548892401, 'Qty': '100', 'BeginDT': ORDER_DT}]
orders_list = []

for _ in xrange(2):
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    order_id = steps.OrderSteps.create(
        order_owner,
        service_order_id,
        service_id=context.service.id,
        product_id=context.product.id,
        params={'AgencyID': agency_id, 'ManagerUID': context.manager.uid},
    )
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
request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params=dict())

# Выставляем счёт
invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, Paysyses.CC_PH_RUB.id, credit=0,
                                             contract_id=contract_id, overdraft=0, endbuyer_id=None)

# Оплачиваем счёт "быстрым" способом
steps.InvoiceSteps.pay_fair(invoice_id, payment_sum=QTY * len(orders_list))

# # Отправляем честные открутки:
# # steps.CampaignsSteps.update_campaigns(product.service_id, service_order_id, {'Bucks': 99.99, 'Money': 0}, 0, BASE_DT)

# # Отправляем НЕчестные открутки:
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
#
# # # Выставляем акт
# steps.ActsSteps.generate(client_id, force=1, date=datetime.datetime.now())

# pass

# steps.ExportSteps.export_oebs(client_id=135006670)
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
