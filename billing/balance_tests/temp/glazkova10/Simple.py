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

context = Contexts.DIRECT_FISH_RUB_CONTEXT
QTY = D('250')
COMPLETIONS = D('99.99')

# Создаём клиента
client_id = None or steps.ClientSteps.create(params=None)

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
# steps.ClientSteps.link(393872, 'clientuid34')

# Создаём плательщика
person_params = {}
# person_params = {'fias_guid': '807648a6-7adb-4d82-ac78-251dcce950f4',
#                  'legal_fias_guid': '8e05359f-282e-45a0-8e04-645b1573a06f',
#                  'is-partner': '1'}
# person_params = {'inn': '590579876860'}
person_id = None or steps.PersonSteps.create(invoice_owner, context.person_type.code, person_params)

#Создаём договор:

# contract_id, _ = steps.ContractSteps.create_contract('no_agency_post',
#                                                      {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
#                                                       'DT': HALF_YEAR_BEFORE_NOW_ISO,
#                                                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
#                                                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
#                                                       'SERVICES': [Services.DIRECT.id, Services.GEO.id,
#                                                                    Services.MEDIA_70.id],
#                                                       # 'COMMISSION_TYPE': 48,
#                                                       # 'NON_RESIDENT_CLIENTS': 0,
#                                                       # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
#                                                       'REPAYMENT_ON_CONSUME': 0,
#                                                       'PERSONAL_ACCOUNT': 1,
#                                                       'LIFT_CREDIT_ON_PAYMENT': 0,
#                                                       'PERSONAL_ACCOUNT_FICTIVE': 1,
#                                                       'CREDIT_LIMIT_SINGLE': 6000
#                                                       })
contract_id = None

# Создаём доп.соглашение:
# steps.ContractSteps.create_collateral(1033, {'contract2_id': contract_id, 'dt': '2015-04-30T00:00:00',
#                                              'is_signed': '2015-01-01T00:00:00'})

# Создаём список заказов:
orders_list = []

for _ in xrange(1):
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    steps.OrderSteps.create(order_owner, service_order_id, service_id=context.service.id, product_id=context.product.id,
                            params={'AgencyID': agency_id, 'ManagerUID': context.manager.uid})
    orders_list.append(
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': ORDER_DT})

# Создаём риквест
request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params=dict(InvoiceDesireDT=INVOICE_DT))

# Выставляем счёт
invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                             contract_id=contract_id, overdraft=0, endbuyer_id=None)

# Оплачиваем счёт "быстрым" способом
steps.InvoiceSteps.pay(invoice_id)

# Отправляем честные открутки:
# steps.CampaignsSteps.update_campaigns(product.service_id, service_order_id, {'Bucks': 99.99, 'Money': 0}, 0, BASE_DT)

# Отправляем НЕчестные открутки:
steps.CampaignsSteps.do_campaigns(context.product.service.id, orders_list[0]['ServiceOrderID'],
                                  {'Bucks': COMPLETIONS, 'Money': 0}, 0, COMPLETIONS_DT)

# Выставляем акт
steps.ActsSteps.generate(invoice_owner, force=1, date=ACT_DT)
pass