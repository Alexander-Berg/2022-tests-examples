# -*- coding: utf-8 -*-
__author__ = 'torvald'

import copy
import time
from datetime import datetime, timedelta
from decimal import Decimal

import pytest
from hamcrest import has_entries

import balance.balance_db as db
import balance.balance_steps as steps
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance.balance_objects import Product
from balance.features import Features
from btestlib import shared
from btestlib.constants import Firms, Services
from btestlib.matchers import has_entries_casted, matches_in_time

ORDER_OPCODE = 1
CLIENT_OPCODE = 10
OVERDRAFT_OPCODE = 11
CONTRACT_OPCODE = 30

SERVICE_ID = 7
PRODUCT_ID = 1475
DIRECT_RUB_PRODUCT_ID = 503162
PAYSYS_ID = 1003
QTY = 118

to_iso = utils.Date.date_to_iso_format
NOW = datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - timedelta(days=180))
EXPIRE_DATE_STRING = (NOW + timedelta(days=180)).strftime('%Y-%m-%d')

KZ_DISCOUNT_POLICY_ID = 18

MANAGER_UID = '244916211'

PAYMENT_TOKEN = "41003321285570.1B462EA1771C281DDAE52391854A0D92F12B3B51E4F87C238DAC2512302EA127E3319B74DF6AB8008DA3B4AA24A4F8ACF602878BE8FA92359D5825CFC" \
                "E886A99A8A975FD3654DCE430C3AC28A442E0FA980ADB2DFDEA1F56EE254CCC2DE76D2177CA038282D76E7DE4E7CAB9D93C8010C8BE1CA53A79B53F50E1506275111A47"

pytestmark = [pytest.mark.slow,
              pytest.mark.priority('mid'),
              reporter.feature(Features.NOTIFICATION, Features.CLIENT, Features.ORDER,
                               Features.OVERDRAFT, Features.MULTICURRENCY, Features.CREDIT),
              pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/notification')]


# ----------------------------------------------------------------------------------------------------------------------
# Notify client

class ClientNotification(object):
    DEFAULT_VALUES = {'BusinessUnit': '0',
                      'CanBeForceCurrencyConverted': '0',
                      'ClientCurrency': '',
                      # 'ClientID': client_id,
                      'MigrateToCurrencyDone': '0',
                      # TODO: убираем проверку: слишком сложная (нужен соответствующий Биллингу учёт праздников)
                      # 'MinPaymentTerm': '0000-00-00',
                      'NonResident': '0',
                      'OverdraftBan': '0',
                      'OverdraftLimit': Decimal('0.00'),
                      'OverdraftSpent': Decimal('0.00'),
                      # 'Tid': '20160420140303745'
                      }

    def __init__(self, parameters={}):
        self.values = copy.deepcopy(ClientNotification.DEFAULT_VALUES)
        self.values.update(parameters)


def get_overdraft_object_id(firm_id, service_id, client_id):
    # To long int to use it in xmlrpc requests.
    return str(firm_id * 10 + service_id * 100000 + client_id * 1000000000)


@pytest.mark.docs(u'При создании\редактировании клиента')
@pytest.mark.shared(block=shared.NO_ACTIONS)
def test_notify_client_creation(shared_data):
    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id = steps.ClientSteps.create()

    utils.check_that(lambda: steps.CommonSteps.get_last_notification(CLIENT_OPCODE, client_id),
                     matches_in_time(has_entries_casted(ClientNotification().values), timeout=300))


@pytest.mark.docs(u'При создании\редактировании клиента')
@pytest.mark.shared(block=shared.NO_ACTIONS)
def test_notify_client_edition(shared_data):
    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id = steps.ClientSteps.create()
        steps.ClientSteps.create({'CLIENT_ID': client_id, 'REGION_ID': 159})

    utils.check_that(lambda: steps.CommonSteps.get_last_notification(CLIENT_OPCODE, client_id),
                     matches_in_time(has_entries_casted(ClientNotification().values), timeout=300))


@pytest.mark.docs(u'Установка параметра intercompany')
@pytest.mark.shared(block=shared.NO_ACTIONS)
def test_notify_client_intercompany_param(shared_data):
    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id = steps.ClientSteps.create()

        steps.CommonSteps.set_extprops('Client', client_id, 'intercompany', {'value_str': 'UA10'})
        # Edit client to get 2nd notification
        steps.ClientSteps.create({'CLIENT_ID': client_id, 'REGION_ID': 159})

    utils.check_that(lambda: steps.CommonSteps.get_last_notification(CLIENT_OPCODE, client_id),
                     matches_in_time(has_entries_casted(ClientNotification({'BusinessUnit': '1'}).values), timeout=300))


@pytest.mark.shared(block=shared.NO_ACTIONS)
def test_notify_client_fair_overdraft_calculation(shared_data):
    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['service_id', 'client_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        service_id = Services.DIRECT.id
        client_id = steps.ClientSteps.create()
        steps.ClientSteps.set_overdraft(client_id, service_id=service_id, limit=1000)

    object_id = get_overdraft_object_id(Firms.YANDEX_1.id, service_id, client_id)

    utils.check_that(lambda: steps.CommonSteps.get_last_notification(OVERDRAFT_OPCODE, object_id),
                     matches_in_time(has_entries_casted(ClientNotification({
                         'CanBeForceCurrencyConverted': '1',
                         'OverdraftLimit': Decimal('1000')
                     }).values), timeout=300))


@pytest.mark.shared(block=shared.NO_ACTIONS)
def test_notify_client_fair_multicurrency_overdraft_calculation(shared_data):
    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['service_id', 'client_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        service_id = Services.DIRECT.id
        client_id = steps.ClientSteps.create({
            'REGION_ID': '225',
            'CURRENCY': 'RUB',
            'MIGRATE_TO_CURRENCY': datetime(2000, 1, 1),
            'SERVICE_ID': SERVICE_ID,
            'CURRENCY_CONVERT_TYPE': 'MODIFY',
            'NAME': u'Custom_name'})

        steps.CommonSteps.export('MIGRATE_TO_CURRENCY', 'Client', client_id)

        steps.ClientSteps.set_overdraft(client_id, service_id=service_id, limit=30000,
                                        currency='RUB', invoice_currency='RUB')

    object_id = get_overdraft_object_id(Firms.YANDEX_1.id, service_id, client_id)

    utils.check_that(lambda: steps.CommonSteps.get_last_notification(OVERDRAFT_OPCODE, object_id),
                     matches_in_time(has_entries_casted(ClientNotification({
                         'ClientCurrency': 'RUB',
                         'MigrateToCurrencyDone': '1',
                         'OverdraftLimit': Decimal('30000')
                     }).values), timeout=300))


@pytest.mark.shared(block=shared.NO_ACTIONS)
def test_notify_client_force_multicurrency_overdraft_usage(shared_data):
    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['service_id', 'client_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        service_id = Services.DIRECT.id
        client_id = steps.ClientSteps.create({
            'REGION_ID': '225',
            'CURRENCY': 'RUB',
            'MIGRATE_TO_CURRENCY': datetime(2000, 1, 1),
            'SERVICE_ID': SERVICE_ID,
            'CURRENCY_CONVERT_TYPE': 'MODIFY',
            'NAME': u'Custom_name'})

        steps.CommonSteps.export('MIGRATE_TO_CURRENCY', 'Client', client_id)

        steps.OverdraftSteps.set_force_overdraft(client_id, service_id=SERVICE_ID, limit=30000, currency='RUB')
        person_id = steps.PersonSteps.create(client_id, 'ur')

        service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
        steps.OrderSteps.create(client_id, service_order_id, service_id=SERVICE_ID, product_id=DIRECT_RUB_PRODUCT_ID)
        orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW}]
        request_id = steps.RequestSteps.create(client_id, orders_list)
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=None,
                                                     overdraft=1, endbuyer_id=None)

    object_id = get_overdraft_object_id(Firms.YANDEX_1.id, service_id, client_id)

    utils.check_that(lambda: steps.CommonSteps.get_last_notification(OVERDRAFT_OPCODE, object_id),
                     matches_in_time(has_entries_casted(ClientNotification({
                         'ClientCurrency': 'RUB',
                         'MigrateToCurrencyDone': '1',
                         # TODO: убираем проверку: слишком сложная (нужен соответствующий Биллингу учёт праздников)
                         # 'MinPaymentTerm': (datetime.now() + timedelta(days=15)).strftime('%Y-%m-%d'),
                         'OverdraftLimit': Decimal('30000'),
                         'OverdraftSpent': Decimal('118')
                     }).values), timeout=300))


@pytest.mark.skip(reason='TODO: "cannot marshal None unless allow_none is enabled" error')
def test_notify_client_force_multicurrency_overdraft_usage_with_fast_payment():
    client_id = steps.ClientSteps.create({
        'REGION_ID': '225',
        'CURRENCY': 'RUB',
        'MIGRATE_TO_CURRENCY': datetime(2000, 1, 1),
        'SERVICE_ID': SERVICE_ID,
        'CURRENCY_CONVERT_TYPE': 'MODIFY',
        'NAME': u'Custom_name'})
    steps.ClientSteps.link(client_id, 'clientuid32')

    steps.OverdraftSteps.set_force_overdraft(client_id, service_id=SERVICE_ID, limit=30000, currency='RUB')
    steps.PersonSteps.create(client_id, 'ur')

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    steps.OrderSteps.create(client_id, service_order_id, service_id=SERVICE_ID, product_id=DIRECT_RUB_PRODUCT_ID)

    # Wait for previous notification to prevent notification's collapse
    utils.wait_until(lambda: steps.CommonSteps.get_last_notification(CLIENT_OPCODE, client_id),
                     has_entries_casted(ClientNotification({
                         'ClientCurrency': 'RUB',
                         'MigrateToCurrencyDone': '1',
                         'MinPaymentTerm': '0000-00-00',
                         'OverdraftLimit': Decimal('30000'),
                         'OverdraftSpent': Decimal('0'),
                     }).values),
                     timeout=300)

    items = [{'service_id': SERVICE_ID, 'service_order_id': service_order_id, 'qty': 10}]
    steps.InvoiceSteps.create_fast_payment(SERVICE_ID, client_id, 1000, items, overdraft=1, passport_uid=167265047,
                                           payment_token=PAYMENT_TOKEN)

    utils.check_that(lambda: steps.CommonSteps.get_last_notification(CLIENT_OPCODE, client_id),
                     matches_in_time(has_entries_casted(ClientNotification({
                         'ClientCurrency': 'RUB',
                         'MigrateToCurrencyDone': '1',
                         # TODO: убираем проверку: слишком сложная (нужен соответствующий Биллингу учёт праздников)
                         # 'MinPaymentTerm': (datetime.now() + timedelta(days=15)).strftime('%Y-%m-%d'),
                         'OverdraftLimit': Decimal('30000'),
                         'OverdraftSpent': Decimal('10')
                     }).values), timeout=300))


# SharedBlock: NO_ACTIONS не добавлен, чтобы не удлиннять BEFORE
# на непонятно сколько работающий reset_overdraft_invoices
def test_notify_client_overdraft_invoice_rollback():
    steps.CloseMonth.resolve_task('monthly_limits')
    service_id = Services.DIRECT.id
    client_id = steps.ClientSteps.create()

    person_id = steps.PersonSteps.create(client_id, 'ur')
    steps.OverdraftSteps.set_force_overdraft(client_id, service_id, 1000, 1)

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, service_id)

    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 10, 'BeginDT': NOW}
    ]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=1, endbuyer_id=None)

    steps.OverdraftSteps.expire_overdraft_invoice(invoice_id)

    steps.OverdraftSteps.reset_overdraft_invoices(client_id)

    object_id = get_overdraft_object_id(Firms.YANDEX_1.id, service_id, client_id)

    utils.check_that(lambda: steps.CommonSteps.get_last_notification(OVERDRAFT_OPCODE, object_id),
                     matches_in_time(has_entries_casted(ClientNotification({'OverdraftLimit': Decimal('1000'),
                                                                            'MinPaymentTerm': '0000-00-00',
                                                                            'OverdraftSpent': Decimal('0')}).values),
                                     timeout=300))


@pytest.mark.shared(block=shared.NO_ACTIONS)
def test_notify_client_with_persons_in_several_firms(shared_data):
    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id = steps.ClientSteps.create()
        person_ur_id = steps.PersonSteps.create(client_id, 'ur')
        steps.PersonSteps.hide_person(person_ur_id)
        steps.PersonSteps.create(client_id, 'sw_yt')
        steps.PersonSteps.unhide_person(person_ur_id)

        steps.ClientSteps.migrate_to_currency(client_id, 'MODIFY')

    utils.check_that(lambda: steps.CommonSteps.get_last_notification(CLIENT_OPCODE, client_id),
                     matches_in_time(has_entries_casted(ClientNotification({'ClientCurrency': 'RUB',
                                                                            'MigrateToCurrencyDone': '1'}).values),
                                     timeout=300))


@pytest.mark.docs(u'Принятие флага оферты')
@pytest.mark.tickets('BALANCE-23934')
@pytest.mark.shared(block=shared.NO_ACTIONS)
@pytest.mark.parametrize("paysys_id, region_id, person_type",
                         [
                             (1003, 225, 'ur'),
                             (1020, 159, 'kzu'),
                             # pytest.mark.skip(reason='UKRAINE WAS TURNED OFF')
                             # (1017, 187, 'ua'),
                             # TURKEY WAS TURNED OFF
                             # (1050, 983, 'tru')
                         ],
                         ids=lambda paysys_id, region_id, person_type: person_type)
def test_notify_client_offer_flag_accept_prepayment_without_contract(shared_data, paysys_id, region_id, person_type):
    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id = steps.ClientSteps.create({'REGION_ID': region_id})
        order_owner = client_id
        invoice_owner = client_id

        person_id = steps.PersonSteps.create(invoice_owner, person_type)

        service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
        steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                                params={'ManagerUID': MANAGER_UID})

        orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW}]
        request_id = steps.RequestSteps.create(invoice_owner, orders_list)
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id,
                                                     contract_id=None, credit=0, overdraft=0)
        steps.InvoiceSteps.pay(invoice_id)

    utils.check_that(lambda: steps.CommonSteps.get_last_notification(CLIENT_OPCODE, client_id),
                     matches_in_time(has_entries_casted(ClientNotification({
                         'CanBeForceCurrencyConverted': '1'
                     }).values), timeout=300))


@pytest.mark.docs(u'Принятие флага оферты')
@pytest.mark.tickets('BALANCE-23934')
@pytest.mark.shared(block=shared.NO_ACTIONS)
@pytest.mark.parametrize("paysys_id, region_id, person_type, contract_type, contract_params",
                         [
                             (1060, 159, 'yt_kzu', 'opt_agency',
                              {
                                  'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                  'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                  'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                  'SERVICES': [7, 37, 67, 70, 77],
                                  'DISCOUNT_POLICY_TYPE': KZ_DISCOUNT_POLICY_ID,
                                  'PERSONAL_ACCOUNT': 1,
                                  'LIFT_CREDIT_ON_PAYMENT': 1,
                                  'PERSONAL_ACCOUNT_FICTIVE': 1,
                                  'CURRENCY': 398,
                                  'BANK_DETAILS_ID': 320,
                                  'DEAL_PASSPORT': HALF_YEAR_BEFORE_NOW_ISO,
                                  'PAYMENT_TYPE': 3
                              }),

                             # pytest.mark.skip(reason='UKRAINE WAS TURNED OFF')
                             #     (1017, 187, 'ua', 'ua_opt_agency_prem',
                             #      {
                             #          'DT': HALF_YEAR_BEFORE_NOW_ISO,
                             #          'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                             #          'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                             #          'SERVICES': [SERVICE_ID],
                             #          'PAYMENT_TYPE': 3
                             #      }),
                             (1003, 225, 'ur', 'no_agency',
                              {
                                  'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                  'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                  'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                  'SERVICES': [7, 37, 67, 70, 77],
                                  'PAYMENT_TYPE': 3
                              }),
                             # TURKEY WAS TURNED OFF
                             # (1050, 983, 'tru', 'tr_opt_agency',
                             #  {'DT': HALF_YEAR_BEFORE_NOW_ISO,
                             #   'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                             #   'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                             #   'SERVICES': [7, 37, 67, 70, 77],
                             #   'PAYMENT_TYPE': 2})

                         ],
                         ids=lambda paysys_id, region_id, person_type, contract_type, contract_params: person_type)
def test_notify_client_offer_flag_accept_prepayment_with_contract(shared_data, paysys_id, region_id, person_type,
                                                                  contract_type,
                                                                  contract_params):
    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id = steps.ClientSteps.create()
        agency_id = steps.ClientSteps.create({'IS_AGENCY': 1, 'REGION_ID': region_id})
        order_owner = client_id
        invoice_owner = agency_id

        person_id = steps.PersonSteps.create(invoice_owner, person_type)

        contract_params.update({'CLIENT_ID': agency_id, 'PERSON_ID': person_id})
        contract_id, contract_eid = steps.ContractSteps.create_contract_new(contract_type, contract_params)

        service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
        steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                                params={'AgencyID': agency_id, 'ManagerUID': MANAGER_UID})

        orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW}]
        request_id = steps.RequestSteps.create(invoice_owner, orders_list)
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id,
                                                     contract_id=contract_id, credit=0, overdraft=0)
        steps.InvoiceSteps.pay(invoice_id)

    utils.check_that(lambda: steps.CommonSteps.get_last_notification(CLIENT_OPCODE, client_id),
                     matches_in_time(has_entries_casted(ClientNotification({
                         'CanBeForceCurrencyConverted': '1'
                     }).values), timeout=300))


@pytest.mark.docs(u'Принятие флага оферты')
@pytest.mark.tickets('BALANCE-23934')
@pytest.mark.shared(block=shared.NO_ACTIONS)
@pytest.mark.parametrize("paysys_id, region_id, person_type, contract_type, contract_params",
                         [
                             (1060, 159, 'yt_kzu', 'opt_agency',
                              {
                                  'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                  'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                  'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                  'SERVICES': [7, 37, 67, 70, 77],
                                  'DISCOUNT_POLICY_TYPE': KZ_DISCOUNT_POLICY_ID,
                                  'PERSONAL_ACCOUNT': 1,
                                  'LIFT_CREDIT_ON_PAYMENT': 1,
                                  'PERSONAL_ACCOUNT_FICTIVE': 1,
                                  'CURRENCY': 398,
                                  'BANK_DETAILS_ID': 320,
                                  'DEAL_PASSPORT': HALF_YEAR_BEFORE_NOW_ISO,
                                  'PAYMENT_TYPE': 3
                              }),
                             # pytest.mark.skip(reason='UKRAINE WAS TURNED OFF')
                             #     (1017, 187, 'ua', 'ua_opt_agency_prem',
                             #      {
                             #          'DT': HALF_YEAR_BEFORE_NOW_ISO,
                             #          'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                             #          'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                             #          'SERVICES': [SERVICE_ID],
                             #          'PAYMENT_TYPE': 3
                             #      }),
                             (1003, 225, 'ur', 'no_agency',
                              {
                                  'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                  'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                  'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                  'SERVICES': [7, 37, 67, 70, 77],
                                  'PAYMENT_TYPE': 3
                              }),
                             # TURKEY WAS TURNED OFF
                             # (11153, 983, 'yt', 'tr_opt_agency',
                             #  {'DT': HALF_YEAR_BEFORE_NOW_ISO,
                             #   'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                             #   'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                             #   'SERVICES': [7, 37, 67, 70, 77],
                             #   'PAYMENT_TYPE': 3})
                         ],
                         ids=lambda paysys_id, region_id, person_type, contract_type, contract_params: person_type)
def test_notify_client_offer_flag_accept_credit(shared_data, paysys_id, region_id, person_type, contract_type,
                                                contract_params):
    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id = steps.ClientSteps.create()
        agency_id = steps.ClientSteps.create({'IS_AGENCY': 1, 'REGION_ID': region_id})
        order_owner = client_id
        invoice_owner = agency_id

        person_id = steps.PersonSteps.create(invoice_owner, person_type)

        contract_params.update({'CLIENT_ID': agency_id, 'PERSON_ID': person_id})
        contract_id, contract_eid = steps.ContractSteps.create_contract_new(contract_type, contract_params)

        service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
        steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                                params={'AgencyID': agency_id, 'ManagerUID': MANAGER_UID})

        orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW}]
        request_id = steps.RequestSteps.create(invoice_owner, orders_list)
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id,
                                                     contract_id=contract_id, credit=1, overdraft=0)

    utils.check_that(lambda: steps.CommonSteps.get_last_notification(CLIENT_OPCODE, client_id),
                     matches_in_time(has_entries_casted(ClientNotification({
                         'CanBeForceCurrencyConverted': '1'
                     }).values), timeout=300))



def test_notify_client_offer_flag_accept_overdraft():
    client_id = steps.ClientSteps.create()
    agency_id = None
    order_owner = client_id
    invoice_owner = agency_id or client_id
    steps.OverdraftSteps.set_force_overdraft(client_id, 7, 10000)
    # steps.OverdraftSteps.set_force_overdraft(client_id, 11, 10000, firm_id=111)

    person_id = steps.PersonSteps.create(invoice_owner, 'ur')
    contract_id = None

    service_id = DIRECT_SERVICE_ID
    # service_id = MARKET_SERVICE_ID
    product = FISH_PRODUCT
    # product = MARKET_PRODUCT
    OPCODE = CLIENT_OPCODE
    # OPCODE = 11

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(order_owner, service_order_id, service_id=service_id, product_id=product.id,
                            params={'AgencyID': agency_id, 'ManagerUID': MANAGER_UID})

    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 1003,
                                                 contract_id=contract_id, credit=0, overdraft=1)

    steps.CampaignsSteps.do_campaigns(product.service_id, service_order_id,
                                      {product.shipment_type: QTY}, do_stop=0, campaigns_dt=NOW)
    steps.ActsSteps.generate(client_id, 1, NOW)

    steps.InvoiceSteps.pay(invoice_id)

    # notify_object_id = (111 + 11 * 10000 + client_id * 100000000) * 10

    utils.check_that(lambda: steps.CommonSteps.get_last_notification(CLIENT_OPCODE, client_id),
                     matches_in_time(has_entries_casted(ClientNotification({
                         'CanBeForceCurrencyConverted': '1',
                         'OverdraftLimit': Decimal('10000.00')
                     }).values), timeout=300))

    # utils.check_that(lambda: steps.CommonSteps.get_last_notification(OPCODE, str(notify_object_id)),
    #                  matches_in_time(has_entries(ClientNotification({
    #                      'CanBeForceCurrencyConverted': '1',
    #                      'OverdraftLimit': '10000.00'
    #                  }).values), timeout=300))


# ---------------------------------------------------------------------
# Notify order 2


EMPTY_ORDER_NOTIFICATION = {  # 'CompletionFixedMoneyQty': '0',
    # 'CompletionFixedQty': '0',
    'CompletionQty': '0',
    # 'ConsumeMoneyQty': '0',
    'ConsumeQty': '0',
    'ConsumeSum': '0',
    'ProductCurrency': '',
    'ServiceID': SERVICE_ID,
    # 'ServiceOrderID': service_order_id,
    'Signal': 1,
    'SignalDescription': 'Order balance have been changed',
    # 'Tid': '8737658629837'
}

# def test_notify_order2_empty():
#     client_id = steps.ClientSteps.create()
#     order_owner = client_id
#
#     service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
#
#     order_id = steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
#                                        params={'ManagerUID': MANAGER_UID})
#
#     steps.ClientSteps.migrate_to_currency(client_id, 'MODIFY', NOW)
#
#     expected = copy.deepcopy(EMPTY_ORDER_NOTIFICATION)
#     expected.update({'CompletionFixedMoneyQty': '0',
#                      'CompletionFixedQty': '0',
#                      'ConsumeMoneyQty': '0'})
#     utils.check_that(lambda: steps.CommonSteps.get_last_notification(ORDER_OPCODE, order_id),
#                      matches_in_time(has_entries(expected), timeout=300))
#
#
DIRECT_SERVICE_ID = 7
MARKET_SERVICE_ID = 11
BYN_PRODUCT = Product(7, 507261, 'Money', None)
FISH_PRODUCT = Product(7, 1475, 'Bucks', 'Money')
MARKET_PRODUCT = Product(11, 2136, 'Bucks', 'Money')
CC_NON_RES_7_RUB = 1075
BANK_NON_RES_1_BYN = 1100
BANK_NON_RES_1_RUB = 1014


#
#
# @pytest.mark.parametrize('scenario', [
#     # {'orders': [(FISH_PRODUCT, None, None),
#     #          (FISH_PRODUCT, Decimal('119'), Decimal('0.9')),
#     #          (FISH_PRODUCT, Decimal('120'), Decimal('16.9'))],
#     #  'expected': {'TotalConsumeQty': '239'}},
#     #
#     # {'orders': [(FISH_PRODUCT, None, None),
#     #          (FISH_PRODUCT, Decimal('119'), Decimal('0')),
#     #          (FISH_PRODUCT, Decimal('120'), Decimal('0'))],
#     #  'expected': {'TotalConsumeQty': '239'}},
#     #
#     # {'orders': [(FISH_PRODUCT, None, None),
#     #          (FISH_PRODUCT, None, None),
#     #          (FISH_PRODUCT, None, None)],
#     #  'expected': {'TotalConsumeQty': '0'}},
#
#     {'orders': [(FISH_PRODUCT, Decimal('20.34'), Decimal('10.12')),
#                 (FISH_PRODUCT, Decimal('119.5'), Decimal('10.12345')),
#                 (FISH_PRODUCT, Decimal('120.46'), Decimal('10.12'))],
#      'expected': {'CompletionQty': '10.12',
#                   'ConsumeAmount': '610.2',
#                   'ConsumeCurrency': 'RUB',
#                   'ConsumeQty': '20.34',
#                   'ConsumeSum': '610.2',
#                   'TotalConsumeQty': '260.3'}},
#
#     {'orders': [(FISH_PRODUCT, Decimal('20.34'), Decimal('10.12')),
#                 (FISH_PRODUCT, None, None),
#                 (FISH_PRODUCT, None, None)],
#      'expected': {'CompletionQty': '10.12',
#                   'ConsumeAmount': '610.2',
#                   'ConsumeCurrency': 'RUB',
#                   'ConsumeQty': '20.34',
#                   'ConsumeSum': '610.2',
#                   'TotalConsumeQty': '20.34'}},
# ])
# def test_on_orders_simple_group_creation(scenario):
#     client_id = steps.ClientSteps.create()
#
#     person_id = steps.PersonSteps.create(client_id, 'ur')
#     # orders = [(FISH_PRODUCT, None, None),
#     #          (FISH_PRODUCT, Decimal('119'), Decimal('0.9')),
#     #          (FISH_PRODUCT, Decimal('120'), Decimal('16.9')),
#     #          (FISH_PRODUCT, Decimal('120'), Decimal('16.9'))]
#
#     extended_orders = []
#     for product, qty, completions in scenario['orders']:
#         service_order_id = steps.OrderSteps.next_id(product.service_id)
#         order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=product.service_id,
#                                            product_id=product.id, params={'AgencyID': None})
#         extended_orders.append({'ServiceID': product.service_id, 'ServiceOrderID': service_order_id,
#                                 'OrderID': order_id})
#         if qty:
#             orders_list = [{'ServiceID': product.service_id, 'ServiceOrderID': service_order_id,
#                             'OrderID': order_id, 'Qty': qty, 'BeginDT': NOW}]
#             request_id = steps.RequestSteps.create(client_id, orders_list)
#             invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=None,
#                                                          overdraft=0)
#             steps.InvoiceSteps.pay(invoice_id)
#             steps.CampaignsSteps.do_campaigns(product.service_id, service_order_id,
#                                               {product.shipment_type: completions}, do_stop=0, campaigns_dt=NOW)
#
#     main = extended_orders[0]['OrderID']
#     slave_list = [item['OrderID'] for item in extended_orders[1:]]
#     steps.OrderSteps.merge(main, slave_list)
#     expected = copy.deepcopy(EMPTY_ORDER_NOTIFICATION)
#     expected.update(scenario['expected'])
#     utils.check_that(lambda: steps.CommonSteps.get_last_notification(ORDER_OPCODE, main),
#                      matches_in_time(has_entries(expected), timeout=300))
#     # steps.OrderSteps.ua_enqueue([client_id])
#     # steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)


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


def migrate_to_currency(client_id, service_id, currency, region_id, dt=None, convert_type='COPY'):
    # 'MIGRATE_TO_CURRENCY' should be in future, so now()+2sec
    if not dt:
        dt = datetime.now() + timedelta(seconds=2)
    steps.ClientSteps.create({'CLIENT_ID': client_id, 'REGION_ID': region_id, 'CURRENCY': currency,
                              'SERVICE_ID': service_id,
                              'MIGRATE_TO_CURRENCY': dt,
                              'CURRENCY_CONVERT_TYPE': convert_type})
    # Wait for the 3 seconds, 'MIGRATE_TO_CURRENCY' now in the past.
    time.sleep(3)
    steps.CommonSteps.export('MIGRATE_TO_CURRENCY', 'Client', client_id)


# def test_on_orders_group_tree_creation():
#     client_id = None or steps.ClientSteps.create()
#     db.balance().execute(
#         "Update t_client set REGION_ID = 149 where ID = :client_id",
#         {'client_id': client_id})
#
#     # Invoice with fish-orders and acts
#     person_id = None or steps.PersonSteps.create(client_id, 'by_ytph')
#     orders = [(FISH_PRODUCT, Decimal('118'), Decimal('13.9')),
#               (FISH_PRODUCT, Decimal('119'), Decimal('0.9')),
#               (FISH_PRODUCT, Decimal('120'), Decimal('16.9'))]
#     orders_list = create_invoice_with_act(orders, client_id=client_id, agency_id=None, person_id=person_id,
#                                           paysys_id=CC_NON_RES_7_RUB, contract_id=None)
#     main = orders_list[0]['OrderID']
#     slave_list = [item['OrderID'] for item in orders_list[1:]]
#     steps.OrderSteps.merge(main, slave_list)
#     # steps.OrderSteps.ua_enqueue([client_id])
#     # steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
#     # TODO: put assert for main order here
#
#     # Migration
#     migrate_to_currency(client_id, DIRECT_SERVICE_ID, 'BYN', 149)
#
#     byn_orders = []
#     for fish_order in orders_list:
#         service_order_id = steps.OrderSteps.next_id(BYN_PRODUCT.service_id)
#         byn_order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=BYN_PRODUCT.service_id,
#                                                product_id=BYN_PRODUCT.id)
#         byn_orders.append({'ServiceID': BYN_PRODUCT.service_id, 'ServiceOrderID': service_order_id,
#                            'OrderID': byn_order_id})
#         steps.OrderSteps.merge(byn_order_id, [fish_order['OrderID']])
#
#     # TODO: put assert for each order from byn_orders list here
#
#     main = byn_orders[0]['OrderID']
#     slave_list = [item['OrderID'] for item in byn_orders[1:]]
#     steps.OrderSteps.merge(main, slave_list)
#     pass
#     # steps.OrderSteps.ua_enqueue([client_id])
#     # steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)


# ----------------------------------------------------------------------------------------------------------------------
# Notify order

NOTIFY_ORDER_SERVICE_ID = 98
NOTIFY_ORDER_PRODUCT_ID = 504700

# SIMPLE_ORDER_FOR_TOURS_NOTIFICATION = [98,
#                                        # 22359865,
#                                        # '8747441198975',
#                                        1,
#                                        'Order balance have been changed',
#                                        '118',
#                                        '118']


# @pytest.mark.docs(u'NotifyOrder for Market')
# def test_notify_order_simple_for_tours():
#     client_id = steps.ClientSteps.create()
#     agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
#     order_owner = client_id
#     invoice_owner = agency_id
#
#     person_id = steps.PersonSteps.create(invoice_owner, 'ur')
#
#     service_order_id = steps.OrderSteps.next_id(NOTIFY_ORDER_SERVICE_ID)
#     order_id = steps.OrderSteps.create(order_owner, service_order_id,
#                                        service_id=NOTIFY_ORDER_SERVICE_ID, product_id=NOTIFY_ORDER_PRODUCT_ID,
#                                        params={'AgencyID': agency_id, 'ManagerUID': MANAGER_UID})
#
#     orders_list = [
#         {'ServiceID': NOTIFY_ORDER_SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW}]
#     request_id = steps.RequestSteps.create(invoice_owner, orders_list)
#     invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID,
#                                                  contract_id=None, credit=0, overdraft=0)
#     steps.InvoiceSteps.pay(invoice_id)
#
#     utils.check_that(lambda: filter_old_notification(ORDER_OPCODE, order_id),
#                      matches_in_time(equal_to(SIMPLE_ORDER_FOR_TOURS_NOTIFICATION), timeout=300))


def filter_old_notification(opcode, item_id):
    notification = steps.CommonSteps.get_last_old_notification(opcode, item_id)
    return notification[:1] + notification[3:] if notification else None


# ----------------------------------------------------------------------------------------------------------------------
# Notify contract

NON_RESIDENT_CONTRACT_NOTIFICATION = {'AdditionalCurrencies': [{'Currency': 'RUB',
                                                                'ExpireDate': EXPIRE_DATE_STRING},
                                                               {'Currency': 'USD',
                                                                'ExpireDate': EXPIRE_DATE_STRING},
                                                               {'Currency': 'EUR',
                                                                'ExpireDate': EXPIRE_DATE_STRING}],
                                      # 'ClientID': 12753580
                                      }


@pytest.mark.docs(u'NotifyAgencyAdditionalCurrencies')
@pytest.mark.shared(block=shared.NO_ACTIONS)
@pytest.mark.parametrize("subclient_region_id, person_type, contract_type, contract_params",
                         [
                             (134, 'ur', 'comm_post', {
                                 'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                 'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                 'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                 'SERVICES': [7, 77],
                                 'COMMISSION_TYPE': 57,
                                 'NON_RESIDENT_CLIENTS': 1
                             })
                         ],
                         ids=lambda subclient_region_id, person_type, contract_type, contract_params: person_type)
def test_notify_contract_non_residents(shared_data, subclient_region_id, person_type, contract_type, contract_params):
    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id = steps.ClientSteps.create()

        query = "Update t_client set REGION_ID = :region_id where ID = :client_id"
        query_params = {'client_id': client_id, 'region_id': subclient_region_id}
        db.balance().execute(query, query_params)

        query = "Update t_client set FULLNAME = u'UL Nonres', CURRENCY_PAYMENT = 'USD', ISO_CURRENCY_PAYMENT = 'USD', IS_NON_RESIDENT = 1 where ID = :client_id"
        query_params = {'client_id': client_id}
        db.balance().execute(query, query_params)

        agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
        steps.ClientSteps.link(client_id, 'clientuid32')

        invoice_owner = agency_id

        person_id = steps.PersonSteps.create(invoice_owner, person_type)

        contract_params.update({'CLIENT_ID': agency_id, 'PERSON_ID': person_id})
        contract_id, contract_eid = steps.ContractSteps.create_contract(contract_type, contract_params)

    utils.check_that(lambda: steps.CommonSteps.get_last_notification(CONTRACT_OPCODE, contract_id),
                     matches_in_time(has_entries(NON_RESIDENT_CONTRACT_NOTIFICATION), timeout=300))


# def json_notification():
#     SERVICE_ID = 9999
#     # PRODUCT_ID = 502953 ##502918
#     PRODUCT_ID = 99999999
#     PAYSYS_ID = 1003
#     QTY = 118
#
#     client_id = None or steps.ClientSteps.create()
#     agency_id = None or steps.ClientSteps.create(params={'IS_AGENCY': 1})
#     order_owner = client_id
#     invoice_owner = agency_id or client_id
#
#     person_id = None or steps.PersonSteps.create(invoice_owner, 'ur')
#     contract_id = None
#
#     orders_list = []
#     service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
#     order_id = steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
#                                        params={'AgencyID': agency_id, 'ManagerUID': MANAGER_UID})
#     orders_list.append({'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW})
#     request_id = steps.RequestSteps.create(invoice_owner, orders_list)
#     invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
#                                                  overdraft=0, endbuyer_id=None)
#     steps.InvoiceSteps.pay(invoice_id)
#     pass


if __name__ == "__main__":
    # convertation_on_empty_order()
    # KZ_offer_flag_accept()
    # json_notification()
    # pytest.main('-v -k test_notify_client --collect-only --docs "1"')
    pytest.main('-v -s --shared=before')
    # pytest.main(
    # '-v --connect "{\"--connectmedium_url\": \"http://ashchek-xmlrpc-medium.greed-dev4f.yandex.ru\", \"testbalance_url\": \"
