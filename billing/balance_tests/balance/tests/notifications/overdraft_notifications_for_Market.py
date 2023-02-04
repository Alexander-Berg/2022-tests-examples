# -*- coding: utf-8 -*-
__author__ = 'torvald'

import copy
import time
import json
from datetime import datetime, timedelta
from decimal import Decimal

import allure
import pytest
from hamcrest import has_entries

import balance.balance_db as db
import balance.balance_steps as steps
import btestlib.utils as utils
from balance.balance_objects import Product
from balance.features import Features
from btestlib.matchers import has_entries_casted, matches_in_time
from btestlib.constants import Services, Products, Paysyses
from btestlib import shared

ORDER_OPCODE = 1
CLIENT_OPCODE = 10
CONTRACT_OPCODE = 30

SERVICE_ID = 7
PRODUCT_ID = 1475
DIRECT_RUB_PRODUCT_ID = 503162
PAYSYS_ID = 1003
QTY = 118

DIRECT_RUS_CONTEXT = utils.aDict({'firm_id': 1,
                                  'product': Products.DIRECT_FISH,
                                  'person_type': 'ur',
                                  'paysys_id': Paysyses.BANK_UR_RUB.id})

DIRECT_UKR_CONTEXT = utils.aDict({'firm_id': 2,
                                  'product': Products.DIRECT_FISH,
                                  'person_type': 'ua',
                                  'paysys_id': Paysyses.BANK_UA_UR_UAH.id})

MARKET_BU_CONTEXT = utils.aDict({'firm_id': 111,
                                 'product': Products.MARKET,
                                 'person_type': 'ur',
                                 'paysys_id': 11101003}),

MARKET_UKR_CONTEXT = utils.aDict({'firm_id': 2,
                                  'product': Products.MARKET,
                                  'person_type': 'ua',
                                  'paysys_id': Paysyses.BANK_UA_UR_UAH.id}),

MARKET = Services.MARKET.id
DIRECT = Services.DIRECT.id

DIRECT_FISH = Products.DIRECT_FISH
MARKET_PRODUCT = Products.MARKET

to_iso = utils.Date.date_to_iso_format
NOW = datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - timedelta(days=180))
A_DAY_AFTER_TOMORROW = NOW + timedelta(days=2)
EXPIRE_DATE_STRING = (NOW + timedelta(days=180)).strftime('%Y-%m-%d')

MANAGER_UID = '244916211'

DEFAULT_LIMIT = 1000
DEFAULT_TIMEOUT = 300

PAYMENT_TOKEN = "41003321285570.1B462EA1771C281DDAE52391854A0D92F12B3B51E4F87C238DAC2512302EA127E3319B74DF6AB8008DA3B4AA24A4F8ACF602878BE8FA92359D5825CFC" \
                "E886A99A8A975FD3654DCE430C3AC28A442E0FA980ADB2DFDEA1F56EE254CCC2DE76D2177CA038282D76E7DE4E7CAB9D93C8010C8BE1CA53A79B53F50E1506275111A47"

pytestmark = [pytest.mark.slow,
              pytest.mark.priority('mid'),
              allure.feature(Features.NOTIFICATION, Features.CLIENT, Features.ORDER,
                             Features.OVERDRAFT, Features.MULTICURRENCY, Features.CREDIT),
              pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/notification')]


# ----------------------------------------------------------------------------------------------------------------------
# Notify client

class OverdraftNotification(object):
    DEFAULT_VALUES = {'BusinessUnit': '0',
                      'ClientCurrency': '',
                      # 'ClientID': client_id,
                      # TODO: убираем проверку: слишком сложная (нужен соответствующий Биллингу учёт праздников)
                      # 'MinPaymentTerm': '0000-00-00',
                      'NonResident': '0',
                      'OverdraftBan': '0',
                      'OverdraftLimit': '0',
                      'OverdraftSpent': '0.00',
                      # 'Tid': '20160420140303745'
                      }

    def __init__(self, parameters={}):
        self.values = copy.deepcopy(OverdraftNotification.DEFAULT_VALUES)
        self.values.update(parameters)



def get_overdraft_notification_object_id(client_id, service_id, firm_id):
    return str(firm_id * 10 + service_id * 100000 + client_id * 1000000000)



def get_clients_group(p, shared_data):
    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'brand_client_id', 'equal_client_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id = steps.ClientSteps.create()

        # Бренд
        brand_client_id = steps.ClientSteps.create_client_brand_contract(client_id)

        # Эквивалентный
        equal_client_id = steps.ClientSteps.create_equal_client(client_id)

    return client_id, brand_client_id, equal_client_id



def get_client_group_with_fair_overdraft(p, shared_data):
    client_id, brand_client_id, equal_client_id = get_clients_group(p, shared_data)

    steps.ClientSteps.set_overdraft(client_id, service_id=p.product.service.id, limit=DEFAULT_LIMIT, firm_id=p.firm_id)

    return client_id, brand_client_id, equal_client_id



def get_client_group_with_force_overdraft(p, shared_data):
    client_id, brand_client_id, equal_client_id = get_clients_group(p, shared_data)

    for client in [client_id, brand_client_id, equal_client_id]:
        steps.ClientSteps.set_force_overdraft(client, service_id=p.product.service.id, limit=DEFAULT_LIMIT,
                                              firm_id=p.firm_id)

    return client_id, brand_client_id, equal_client_id



def check_notifications_for_group(client_id, brand_client_id, equal_client_id, service_id, firm_id, expected_params):
    for client_id, description in [(client_id, u'Основной клиент'),
                                   (brand_client_id, u'Клиент из бренда'),
                                   (equal_client_id, u'Эквивалентный клиент')]:
        object_id = get_overdraft_notification_object_id(client_id, service_id, firm_id)

        utils.check_that(lambda: steps.CommonSteps.get_last_notification(CLIENT_OPCODE, object_id),
                         matches_in_time(has_entries_casted(OverdraftNotification(expected_params).values),
                                         timeout=DEFAULT_TIMEOUT),
                         step=u'{0}: проверяем нотификацию'.format(description))



@pytest.mark.shared(block=steps.SharedBlocks.REFRESH_MV_CLIENT_DIRECT_BRAND)
@pytest.mark.parametrize('p', [pytest.mark.smoke(DIRECT_RUS_CONTEXT),
                               DIRECT_UKR_CONTEXT,
                               MARKET_BU_CONTEXT,
                               MARKET_UKR_CONTEXT
])
def test_fair_overdraft_calculation(p, shared_data):
    client_id, brand_client_id, equal_client_id = get_client_group_with_fair_overdraft(p, shared_data)

    expected_params = {'OverdraftLimit': Decimal(DEFAULT_LIMIT)}
    check_notifications_for_group(client_id, brand_client_id, equal_client_id, p.product.service.id, p.firm_id,
                                  expected_params)



@pytest.mark.shared(block=steps.SharedBlocks.REFRESH_MV_CLIENT_DIRECT_BRAND)
@pytest.mark.parametrize('p', [DIRECT_RUS_CONTEXT,
                               DIRECT_UKR_CONTEXT,
                               MARKET_BU_CONTEXT,
                               MARKET_UKR_CONTEXT
])
def test_fair_overdraft_nullify_recalculation(p, shared_data):
    qty = 100

    client_id, brand_client_id, equal_client_id = get_client_group_with_force_overdraft(p, shared_data)

    person_id = steps.PersonSteps.create(client_id, p.person_type)

    campaigns_list = [{'service_id': p.product.service.id, 'product_id': p.product.id, 'qty': qty, 'begin_dt': NOW}]
    invoice_id, _, _, _ = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                  person_id=person_id,
                                                                  campaigns_list=campaigns_list,
                                                                  paysys_id=p.paysys_id,
                                                                  invoice_dt=NOW,
                                                                  agency_id=None,
                                                                  credit=0,
                                                                  contract_id=None,
                                                                  overdraft=1,
                                                                  )

    expected_params = {'OverdraftLimit': Decimal(DEFAULT_LIMIT),
                       'OverdraftSpent': Decimal(qty)}
    check_notifications_for_group(client_id, brand_client_id, equal_client_id, p.product.service.id, p.firm_id,
                                  expected_params)

    steps.CommonSteps.export('OVERDRAFT', 'Client', client_id)

    expected_params = {'OverdraftLimit': Decimal('0'),
                       'OverdraftSpent': Decimal(qty)}
    check_notifications_for_group(client_id, brand_client_id, equal_client_id, p.product.service.id, p.firm_id,
                                  expected_params)



@pytest.mark.shared(block=steps.SharedBlocks.REFRESH_MV_CLIENT_DIRECT_BRAND)
@pytest.mark.parametrize('p', [DIRECT_RUS_CONTEXT,
                               DIRECT_UKR_CONTEXT,
                               MARKET_BU_CONTEXT,
                               MARKET_UKR_CONTEXT
])
def test_fair_overdraft_recalculation(p, shared_data):
    client_id, brand_client_id, equal_client_id = get_client_group_with_fair_overdraft(p, shared_data)

    person_id = steps.PersonSteps.create(client_id, p.person_type)

    additional_qty = 50000
    additional_act_dt = utils.add_months_to_date(NOW, -2)
    campaigns_list = [{'service_id': p.product.service.id, 'product_id': p.product.id, 'qty': additional_qty,
                       'begin_dt': additional_act_dt}]
    invoice_id, _, _, orders_list = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                            person_id=person_id,
                                                                            campaigns_list=campaigns_list,
                                                                            paysys_id=p.paysys_id,
                                                                            invoice_dt=additional_act_dt,
                                                                            agency_id=None,
                                                                            credit=0,
                                                                            contract_id=None,
                                                                            overdraft=0,
                                                                            )

    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(p.product.service.id, orders_list[0]['ServiceOrderID'],
                                      {p.product.type.code: additional_qty}, 0, additional_act_dt)
    steps.ActsSteps.generate(client_id, force=1, date=additional_act_dt)

    steps.CommonSteps.export('OVERDRAFT', 'Client', client_id)

    expected_params = {'OverdraftLimit': Decimal('5200')}
    check_notifications_for_group(client_id, brand_client_id, equal_client_id, p.product.service.id, p.firm_id,
                                  expected_params)


@pytest.mark.shared(block=steps.SharedBlocks.REFRESH_MV_CLIENT_DIRECT_BRAND)
@pytest.mark.parametrize('p', [DIRECT_RUS_CONTEXT,
                               DIRECT_UKR_CONTEXT,
                               MARKET_BU_CONTEXT,
                               MARKET_UKR_CONTEXT
])
def test_overdraft_usage(p, shared_data):
    qty = 100

    client_id, brand_client_id, equal_client_id = get_client_group_with_force_overdraft(p, shared_data)

    person_id = steps.PersonSteps.create(client_id, p.person_type)

    campaigns_list = [{'service_id': p.product.service.id, 'product_id': p.product.id, 'qty': qty, 'begin_dt': NOW}]
    invoice_id, _, _, _ = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                  person_id=person_id,
                                                                  campaigns_list=campaigns_list,
                                                                  paysys_id=p.paysys_id,
                                                                  invoice_dt=NOW,
                                                                  agency_id=None,
                                                                  credit=0,
                                                                  contract_id=None,
                                                                  overdraft=1,
                                                                  )

    expected_params = {'OverdraftLimit': Decimal(DEFAULT_LIMIT),
                       'OverdraftSpent': Decimal(qty)}
    check_notifications_for_group(client_id, brand_client_id, equal_client_id, p.product.service.id, p.firm_id,
                                  expected_params)


@pytest.mark.shared(block=steps.SharedBlocks.REFRESH_MV_CLIENT_DIRECT_BRAND)
@pytest.mark.parametrize('p', [DIRECT_RUS_CONTEXT,
                               DIRECT_UKR_CONTEXT,
                               MARKET_BU_CONTEXT,
                               MARKET_UKR_CONTEXT
])
def test_notify_client_overdraft_invoice_rollback(p, shared_data):
    qty = 100

    client_id, brand_client_id, equal_client_id = get_client_group_with_force_overdraft(p, shared_data)

    person_id = steps.PersonSteps.create(client_id, p.person_type)

    campaigns_list = [{'service_id': p.product.service.id, 'product_id': p.product.id, 'qty': qty, 'begin_dt': NOW}]
    invoice_id, _, _, _ = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                  person_id=person_id,
                                                                  campaigns_list=campaigns_list,
                                                                  paysys_id=p.paysys_id,
                                                                  invoice_dt=NOW,
                                                                  agency_id=None,
                                                                  credit=0,
                                                                  contract_id=None,
                                                                  overdraft=1,
                                                                  )

    expected_params = {'OverdraftLimit': Decimal(DEFAULT_LIMIT),
                       'OverdraftSpent': Decimal(qty)}
    check_notifications_for_group(client_id, brand_client_id, equal_client_id, p.product.service.id, p.firm_id,
                                  expected_params)

    steps.OverdraftSteps.expire_overdraft_invoice(invoice_id)
    steps.OverdraftSteps.reset_overdraft_invoices(client_id)

    expected_params = {'OverdraftLimit': Decimal(DEFAULT_LIMIT),
                       'MinPaymentTerm': '0000-00-00',
                       'OverdraftSpent': Decimal('0')}
    check_notifications_for_group(client_id, brand_client_id, equal_client_id, p.product.service.id, p.firm_id,
                                  expected_params)


@pytest.mark.parametrize('p', [DIRECT_RUS_CONTEXT,
                               DIRECT_UKR_CONTEXT,
                               MARKET_BU_CONTEXT,
                               MARKET_UKR_CONTEXT
])
def test_client_convert_to_currency(p):
    qty = 100

    client_id = steps.ClientSteps.create()

    # Бренд
    brand_client_id = create_client_brand_contract(client_id)

    # Эквивалентный
    equal_client_id = create_equal_client(client_id)

    for client in [client_id, brand_client_id, equal_client_id]:
        steps.ClientSteps.set_force_overdraft(client, service_id=p.product.service.id, limit=1000, firm_id=p.firm_id)

    steps.ClientSteps.migrate_to_currency(client_id, 'MODIFY')
    pass


@pytest.mark.parametrize('p', [DIRECT_RUS_CONTEXT,
                               DIRECT_UKR_CONTEXT,
                               MARKET_BU_CONTEXT,
                               MARKET_UKR_CONTEXT
])
def test_overdraft_usage(p):
    qty = 100

    client_id = None or steps.ClientSteps.create()
    person_id = None or steps.PersonSteps.create(client_id, p.person_type)

    # Бренд
    # brand_client_id = create_client_brand_contract(client_id)

    # Эквивалентный
    # equal_client_id = create_equal_client(client_id)

    # for client in [client_id, brand_client_id, equal_client_id]:
    for client in [client_id]:
        steps.ClientSteps.set_force_overdraft(client, service_id=p.product.service.id, limit=1000, firm_id=p.firm_id)

    campaigns_list = [{'service_id': p.product.service.id, 'product_id': p.product.id, 'qty': qty, 'begin_dt': NOW}]
    invoice_id, _, _, _ = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                  person_id=person_id,
                                                                  campaigns_list=campaigns_list,
                                                                  paysys_id=p.paysys_id,
                                                                  invoice_dt=NOW,
                                                                  agency_id=None,
                                                                  credit=0,
                                                                  contract_id=None,
                                                                  overdraft=1,
                                                                  )
    pass


def create_invoice_with_act(orders, client_id, agency_id, person_id, paysys_id, contract_id=None,
                            credit=0, overdraft=0):
    order_owner = client_id
    invoice_owner = agency_id or client_id
    orders_list = []

    # Create invoice with all requested orders
    for product, qty, completions in orders:
        service_order_id = steps.OrderSteps.next_id(product.service_id)
        order_id = steps.OrderSteps.create(order_owner, service_order_id, service_id=product.service_id,
                                           product_id=product.id, params={'AgencyID': agency_id})
        orders_list.append({'ServiceID': product.service_id, 'ServiceOrderID': service_order_id,
                            'OrderID': order_id, 'Qty': qty, 'BeginDT': NOW})
    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=credit,
                                                 contract_id=contract_id, overdraft=overdraft)

    # Pay for prepayment invoice
    if not (credit or overdraft):
        steps.InvoiceSteps.pay(invoice_id)
    # Invoice with SW CreditCard invoice won't by automatically turned on, even after full payment. Turn it on manually
    if paysys_id == CC_NON_RES_7_RUB:
        steps.InvoiceSteps.turn_on(invoice_id)

    # 'order' structure doesn't contain 'ServiceOrderID', use it from orders_list
    for num, (product, qty, completions) in enumerate(orders):
        steps.CampaignsSteps.do_campaigns(product.service_id, orders_list[num]['ServiceOrderID'],
                                          {product.shipment_type: completions}, do_stop=0, campaigns_dt=NOW)

    steps.ActsSteps.generate(invoice_owner, force=1, date=NOW)

    # Return orders to user 'OrderID' and 'ServiceOrderID' futher
    return orders_list


if __name__ == "__main__":
    # convertation_on_empty_order()
    # KZ_offer_flag_accept()
    # json_notification()
    # pytest.main('-v -k test_notify_client --collect-only --docs "1"')
    pytest.main('test_notification_sending_rules.py -k "test_fair_overdraft_calculation" --shared=before -v')
    # pytest.main(
    # '-v --connect "{\"--connectmedium_url\": \"http://ashchek-xmlrpc-medium.greed-dev4f.yandex.ru\", \"testbalance_url\": \"
