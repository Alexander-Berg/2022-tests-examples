# -*- coding: utf-8 -*-

import datetime
import time
from decimal import Decimal as D

from balance import balance_steps as steps
from balance.balance_objects import Product
from balance import balance_db as db
from balance.tests.promocode_new.promocode_commons import reserve, create_and_reserve_promocode
from btestlib import utils
from btestlib.constants import Services, PersonTypes
from temp.igogor.balance_objects import Contexts

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

CURRENT_DAY = datetime.datetime(2020, 05, 15, 18, 0, 0)
ORDER_DT = datetime.datetime(2020, 03, 28)

# FIRST_COMPLETION = None
FIRST_COMPLETION = datetime.datetime(2020, 03, 30, 23, 59, 59)
COMPLETIONS_DTS = [
    datetime.datetime(2020, 04, 01, 0, 0, 0),
    datetime.datetime(2020, 04, 05),
    datetime.datetime(2020, 04, 10),
    datetime.datetime(2020, 04, 20),
    datetime.datetime(2020, 04, 30, 23, 59, 59),
]
COMPLETION_COUNT = len(COMPLETIONS_DTS)
if FIRST_COMPLETION:
    COMPLETION_COUNT += 1
INVOICE_DT = datetime.datetime(2020, 03, 28)
ACT_DT = datetime.datetime(2020, 04, 30, 23, 59, 59)

context = Contexts.DIRECT_FISH_RUB_CONTEXT
# context = Contexts.DIRECT_MONEY_RUB_CONTEXT
QTY = D('200')
PROMOCODE_QTY = D('100')

# Создаём клиента
# client_id = 10
client_id = steps.ClientSteps.create(params={'login': 'test_login'})

# Перевести клиента на мультивалютность
# steps.ClientSteps.migrate_to_currency(client_id, 'COPY')

# Создаём агентство:
# agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
agency_id = None

# Далее в скрипте будут фигурировать "владелец счёта": агентство или клиент и "владелей заказа": всегда клиент:
order_owner = client_id
invoice_owner = agency_id or client_id

# Привязать клиента к логину
steps.ClientSteps.link(client_id, 'Yndx-mivolgin')

# Создаём плательщика
person_params = {}
person_id = None or steps.PersonSteps.create(invoice_owner, context.person_type.code, person_params)

# Создаём договор:
contract_id = None

# --- --- --- --- --- --- ---
# Подготовительная часть для расчета скидки

# Создаём список заказов:
orders_list = []

service_order_id = steps.OrderSteps.next_id(context.service.id)
steps.OrderSteps.create(order_owner, service_order_id, service_id=context.service.id, product_id=context.product.id,
                        params={'AgencyID': agency_id, 'ManagerUID': context.manager.uid})
orders_list.append(
    {
        'ServiceID': context.service.id,
        'ServiceOrderID': service_order_id,
        'Qty': QTY * COMPLETION_COUNT,
        'BeginDT': ORDER_DT,
    },
)

# Создаём риквест
request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params=dict(InvoiceDesireDT=INVOICE_DT))

# Зарезервировать промокод за клиентом
# promocode_id = db.get_promocode_by_code(PROMOCODE.replace('-', ''))[0]['id']
# reserve(client_id, promocode_id, PROMOCODE_RESERVATION_DT)
promocode_id, promocode_code = create_and_reserve_promocode(
    calc_class_name='ActBonusPromoCodeGroup',
    client_id=client_id,
    start_dt=datetime.datetime(2020, 03, 01, 0, 0),
    end_dt=datetime.datetime(2020, 06, 01, 0, 0),
    reservation_dt=datetime.datetime(2020, 03, 28),
    firm_id=1,
    calc_params={
        'adjust_quantity': True,
        'apply_on_create': True,
        'act_bonus_pct': '30',
        'min_act_amount': '100',
        'max_bonus_amount': '100000',
        # 'max_discount_pct': '90',
        'currency': 'RUB',
        'act_month_count': 1,
        'act_product_ids': [context.product.id],
    },
    service_ids=[7],
    reservation_days=90,
    is_global_unique=True,
    need_unique_urls=False,
    new_clients_only=False,
    valid_until_paid=False,
)

# Выставляем счёт
invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                             contract_id=contract_id, overdraft=0, endbuyer_id=None)

# Оплачиваем счёт "быстрым" способом
steps.InvoiceSteps.pay(invoice_id)

# Отправляем честные открутки:
# steps.CampaignsSteps.update_campaigns(product.service_id, service_order_id, {'Bucks': 99.99, 'Money': 0}, 0, BASE_DT)

# Отправляем НЕчестные открутки:
completed = D('0')
if FIRST_COMPLETION:
    completed += QTY
    steps.CampaignsSteps.do_campaigns(
        context.product.service.id,
        orders_list[0]['ServiceOrderID'],
        {'Bucks': completed, 'Money': 0},
        0,
        FIRST_COMPLETION,
    )
    steps.ActsSteps.generate(invoice_owner, force=1, date=FIRST_COMPLETION)

for c_dt in COMPLETIONS_DTS:
    completed += QTY
    steps.CampaignsSteps.do_campaigns(
        context.product.service.id,
        orders_list[0]['ServiceOrderID'],
        {'Bucks': completed, 'Money': 0},
        0,
        c_dt,
    )

# Выставляем акт
steps.ActsSteps.generate(invoice_owner, force=1, date=ACT_DT)


# --- --- --- --- --- --- ---
# А теперь счёт, к которому скидка должна примениться

# Создаём список заказов:
orders_list = []

service_order_id = steps.OrderSteps.next_id(context.service.id)
steps.OrderSteps.create(order_owner, service_order_id, service_id=context.service.id, product_id=context.product.id,
                        params={'AgencyID': agency_id, 'ManagerUID': context.manager.uid})
orders_list.append(
    {
        'ServiceID': context.service.id,
        'ServiceOrderID': service_order_id,
        'Qty': PROMOCODE_QTY,
        'BeginDT': CURRENT_DAY,
    },
)

# Создаём риквест
request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params=dict(InvoiceDesireDT=CURRENT_DAY))

# Выставляем счёт
invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                             contract_id=contract_id, overdraft=0, endbuyer_id=None)

# Оплачиваем счёт "быстрым" способом
steps.InvoiceSteps.pay(invoice_id)
