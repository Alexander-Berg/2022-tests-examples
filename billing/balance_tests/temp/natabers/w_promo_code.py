# -*- coding: utf-8 -*-

import datetime
import time
from decimal import Decimal as D

from balance import balance_steps as steps
from balance.balance_objects import Product
from balance.tests.promocode_new.promocode_commons import reserve, create_and_reserve_promocode
from btestlib import utils
from btestlib.constants import Services, PersonTypes
from temp.igogor.balance_objects import Contexts

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
YESTERDAY = NOW - datetime.timedelta(days=1)
HALF_YEAR_AFTER_NOW = NOW + datetime.timedelta(days=180)
HALF_YEAR_AFTER_NOW_ISO = to_iso(HALF_YEAR_AFTER_NOW)
HALF_YEAR_BEFORE_NOW = NOW - datetime.timedelta(days=180)
HALF_YEAR_BEFORE_NOW_ISO = to_iso(HALF_YEAR_BEFORE_NOW)
ORDER_DT = NOW
INVOICE_DT = NOW
COMPLETIONS_DT = NOW
ACT_DT = NOW

context = Contexts.DIRECT_FISH_RUB_CONTEXT
QTY = D('120')
COMPLETIONS = D('20')

# Создаём клиента
# client_id = 10
client_id = steps.ClientSteps.create(params={'login': 'test_login'})

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
# steps.ClientSteps.link(client_id, 'natabers')

# Создаём плательщика
person_params = {}
person_id = None or steps.PersonSteps.create(invoice_owner, context.person_type.code, person_params)

# Создаём договор:
contract_id = None

# Создаём список заказов:
orders_list = []

for _ in xrange(2):
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    order_id = steps.OrderSteps.create(order_owner, service_order_id, service_id=context.service.id, product_id=context.product.id,
                            params={'AgencyID': agency_id, 'ManagerUID': context.manager.uid})

    orders_list.append(
        {
            'ServiceID': context.service.id,
            'ServiceOrderID': service_order_id,
            'Qty': QTY,
            'BeginDT': ORDER_DT,
        },
    )


# Создаём риквест
request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params=dict(InvoiceDesireDT=INVOICE_DT))

# Зарезервировать промокод за клиентом
# promocode_id = db.get_promocode_by_code(PROMOCODE.replace('-', ''))[0]['id']
# reserve(client_id, promocode_id, PROMOCODE_RESERVATION_DT)
promocode_id, promocode_code = create_and_reserve_promocode(
    calc_class_name='FixedSumBonusPromoCodeGroup',
    # client_id=invoice_owner,
    start_dt=HALF_YEAR_BEFORE_NOW,
    end_dt=HALF_YEAR_AFTER_NOW,
    reservation_dt=YESTERDAY,
    firm_id=1,
    calc_params={
        # adjust_quantity и apply_on_create общие для всех типов промокодов
        'adjust_quantity': 1,  # увеличиваем количество (иначе уменьшаем сумму)
        'apply_on_create': 1,  # применяем при создании счёта иначе при включении (оплате)
        # остальные зависят от типа
        'currency_bonuses': {"RUB": 500},
        'reference_currency': 'RUB',
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

# # Отправляем честные открутки:
# # steps.CampaignsSteps.update_campaigns(product.service_id, service_order_id, {'Bucks': 99.99, 'Money': 0}, 0, BASE_DT)

# Отправляем НЕчестные открутки:
completed = 0
for _ in range(1):
    completed += COMPLETIONS
    for order in orders_list:
        steps.CampaignsSteps.do_campaigns(
            order['ServiceID'],
            order['ServiceOrderID'],
            {'Bucks': completed},
            0,
            NOW,
        )

# # Выставляем акт
# steps.ActsSteps.generate(135006670, force=1, date=NOW)
