# coding: utf-8

import datetime

import pytest
import xmlrpclib
from hamcrest import equal_to

from balance import snout_steps
from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import Features, AuditFeatures
from btestlib import utils as utils, reporter
from btestlib.constants import Services
from simpleapi.matchers.deep_equals import deep_equals_to, deep_contains
from temp.igogor.balance_objects import Contexts, Products, Firms, Paysyses, PersonTypes, Currencies, Regions

dt = datetime.datetime.now()

DIRECT_YANDEX_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1)

DIRECT_YANDEX_FIRM_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, product=Products.DIRECT_RUB,
                                                              region=Regions.RU, currency=Currencies.RUB)

DIRECT_YANDEX_FIRM_FISH_NON_RESIDENT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                                            person_type=PersonTypes.YT,
                                                                            paysys=Paysyses.BANK_YT_RUB)

DIRECT_BEL_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_FISH,
                                                            firm=Firms.REKLAMA_BEL_27, person_type=PersonTypes.BYU,
                                                            paysys=Paysyses.BANK_BY_UR_BYN)

DIRECT_BEL_FIRM_BYN = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_BYN,
                                                           firm=Firms.REKLAMA_BEL_27, person_type=PersonTypes.BYU,
                                                           paysys=Paysyses.BANK_BY_UR_BYN, region=Regions.BY,
                                                           currency=Currencies.BYN)

DIRECT_KZ_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_FISH,
                                                           firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                           paysys=Paysyses.BANK_KZ_UR_TG)

DIRECT_KZ_FIRM_KZU = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_KZT_508892,
                                                          firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                          paysys=Paysyses.BANK_KZ_UR_TG, region=Regions.KZ,
                                                          currency=Currencies.KZT)

DIRECT_QUASI_BEL_FIRM_BYN = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_BYN_QUASI,
                                                                 firm=Firms.REKLAMA_BEL_27, person_type=PersonTypes.BYU,
                                                                 paysys=Paysyses.BANK_BY_UR_BYN, region=Regions.BY,
                                                                 currency=Currencies.BYN)

DIRECT_QUASI_KZ_FIRM_KZU = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_KZT_QUASI,
                                                                firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                                paysys=Paysyses.BANK_KZ_UR_TG, region=Regions.KZ)

MARKET_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MARKET, product=Products.MARKET,
                                                        firm=Firms.MARKET_111)

AUTORU_FIRM_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.VERTICAL_12, product=Products.AUTORU_505123,
                                                       region=Regions.RU, currency=Currencies.RUB,
                                                       service=Services.AUTORU, paysys=Paysyses.BANK_UR_RUB_VERTICAL)

GEO_YANDEX_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, product=Products.GEO_509780,
                                                            currency=Currencies.RUB, service=Services.GEO)

GEO_BEL_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.GEO, product=Products.GEO_510792,
                                                         firm=Firms.REKLAMA_BEL_27, person_type=PersonTypes.BYU,
                                                         paysys=Paysyses.BANK_BY_UR_BYN, currency=Currencies.BYN)

GEO_KZ_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.GEO, product=Products.GEO_510794,
                                                        firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                        paysys=Paysyses.BANK_KZ_UR_TG, currency=Currencies.KZT)

VENDORS_MARKET_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.MARKET_111, product=Products.VENDOR,
                                                                currency=Currencies.RUB, service=Services.VENDORS)

ENOUGH_LIMIT_VALUE = 100


def get_active_price(product_id, iso_currency, on_dt=None):
    if not on_dt:
        on_dt = datetime.datetime.now()
    prices = db.get_prices_by_product_id(product_id)
    active_prices = sorted(
        filter(lambda price: price['dt'] <= on_dt and price['iso_currency'] == iso_currency, prices),
        key=lambda price: price['dt']
    )
    if active_prices:
        return active_prices[-1]


@reporter.feature(Features.TRUST)
@pytest.mark.no_parallel('fast_payment_overdraft')
@pytest.mark.parametrize('params', [
    {'overdraft_given_to': DIRECT_YANDEX_FIRM_FISH, 'overdraft_taken_by': DIRECT_YANDEX_FIRM_FISH}])
@pytest.mark.parametrize('overdraft_value', ['enough_limit', 'non_enough', 'null'])
def test_overdraft_usage_via_fast_payment(params, overdraft_value,get_free_user):
    client_id = steps.ClientSteps.create()
    user = get_free_user()
    steps.ClientSteps.link(client_id, user.login)
    overdraft_given_to_context = params['overdraft_given_to']
    if overdraft_value != 'null':
        if overdraft_value == 'enough_limit':
            limit = 100
        else:
            limit = 50

        steps.OverdraftSteps.set_force_overdraft(client_id, overdraft_given_to_context.service.id, limit,
                                                 overdraft_given_to_context.firm.id)
        actual_limit = db.balance().get_overdraft_limit_by_firm(client_id, overdraft_given_to_context.service.id,
                                                                overdraft_given_to_context.firm.id)
        assert limit == actual_limit

    overdraft_taken_by_context = params['overdraft_taken_by']
    steps.PersonSteps.create(client_id, overdraft_taken_by_context.person_type.code)
    service_order_id = steps.OrderSteps.next_id(overdraft_taken_by_context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=overdraft_taken_by_context.service.id,
                            product_id=overdraft_taken_by_context.product.id)
    result = steps.InvoiceSteps.create_fast_payment(service_id=overdraft_taken_by_context.service.id,
                                                    client_id=client_id,
                                                    payment_token='41003321285570.1B462EA1771C281DDAE52391854A0D92F12B3B51E4F87C238DAC2512302EA127E3319B74DF6AB8008DA3B4AA24A4F8ACF602878BE8FA92359D5825CFCE886A99A8A975FD3654DCE430C3AC28A442E0FA980ADB2DFDEA1F56EE254CCC2DE76D2177CA038282D76E7DE4E7CAB9D93C8010C8BE1CA53A79B53F50E1506275111A47',
                                                    paysys_id=1000,
                                                    items=[
                                                        {'service_id': overdraft_taken_by_context.service.id,
                                                         'service_order_id': service_order_id,
                                                         'qty': '100'}],
                                                    overdraft=1,
                                                    login=user.login,
                                                    passport_uid=user.id_)
    if overdraft_given_to_context == overdraft_taken_by_context:
        if overdraft_value == 'enough_limit':
            utils.check_that(result, deep_contains({'overdraft': 1,
                                                    'status_code': 0,
                                                    'status_desc': 'Overdraft given'}))
        elif overdraft_value == 'non_enough':
            utils.check_that(result, deep_equals_to({'overdraft': {'available_sum': '1500',
                                                                   'available_sum_ue': '50',
                                                                   'overdraft_sum': '1500',
                                                                   'overdraft_sum_ue': '50',
                                                                   'currency': None,
                                                                   'expired_sum': '0',
                                                                   'expired_sum_ue': '0',
                                                                   'is_available': False,
                                                                   'is_present': True,
                                                                   'iso_currency': None,
                                                                   'min_days_to_live': None,
                                                                   'nearly_expired_sum': '0',
                                                                   'nearly_expired_sum_ue': '0',
                                                                   'overdraft_iso_currency': None,
                                                                   'show_notify': True,
                                                                   'spent_sum': '0',
                                                                   'spent_sum_ue': '0',
                                                                   'skip_sms_notification': None,
                                                                   'warnings': {}},
                                                     'status_code': 3,
                                                     'status_desc': 'Overdraft not available'}))
        elif overdraft_value == '':
            utils.check_that(result, deep_equals_to({'overdraft': {'available_sum': '0',
                                                                   'available_sum_ue': '0',
                                                                   'currency': None,
                                                                   'expired_sum': '0',
                                                                   'expired_sum_ue': '0',
                                                                   'is_available': False,
                                                                   'is_present': False,
                                                                   'iso_currency': None,
                                                                   'min_days_to_live': None,
                                                                   'nearly_expired_sum': '0',
                                                                   'nearly_expired_sum_ue': '0',
                                                                   'overdraft_iso_currency': None,
                                                                   'show_notify': True,
                                                                   'spent_sum': '0',
                                                                   'spent_sum_ue': '0',
                                                                   'warnings': {}},
                                                     'status_code': 3,
                                                     'status_desc': 'Overdraft not available'}))


@pytest.mark.no_parallel('fast_payment_overdraft')
@pytest.mark.parametrize('params', [
    {'overdraft_given_to': DIRECT_YANDEX_FIRM_FISH_NON_RESIDENT,
     'overdraft_taken_by': DIRECT_YANDEX_FIRM_FISH_NON_RESIDENT},

])
@pytest.mark.parametrize('overdraft_value', ['enough_limit'])
def overdraft_usage_via_fast_payment_non_resident(params, overdraft_value):
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, 'aikawa-test-10')
    overdraft_given_to_context = params['overdraft_given_to']
    if overdraft_value != 'null':
        if overdraft_value == 'enough_limit':
            limit = 100
        else:
            limit = 50

        steps.OverdraftSteps.set_force_overdraft(client_id, overdraft_given_to_context.service.id, limit,
                                                 overdraft_given_to_context.firm.id)
        actual_limit = db.balance().get_overdraft_limit_by_firm(client_id, overdraft_given_to_context.service.id,
                                                                overdraft_given_to_context.firm.id)
        assert limit == actual_limit

    overdraft_taken_by_context = params['overdraft_taken_by']
    steps.PersonSteps.create(client_id, overdraft_taken_by_context.person_type.code)
    service_order_id = steps.OrderSteps.next_id(overdraft_taken_by_context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=overdraft_taken_by_context.service.id,
                            product_id=overdraft_taken_by_context.product.id)
    result = steps.InvoiceSteps.create_fast_payment(service_id=overdraft_taken_by_context.service.id,
                                                    client_id=client_id,
                                                    payment_token='41003321285570.1B462EA1771C281DDAE52391854A0D92F12B3B51E4F87C238DAC2512302EA127E3319B74DF6AB8008DA3B4AA24A4F8ACF602878BE8FA92359D5825CFCE886A99A8A975FD3654DCE430C3AC28A442E0FA980ADB2DFDEA1F56EE254CCC2DE76D2177CA038282D76E7DE4E7CAB9D93C8010C8BE1CA53A79B53F50E1506275111A47',
                                                    paysys_id=1000,
                                                    items=[
                                                        {'service_id': overdraft_taken_by_context.service.id,
                                                         'service_order_id': service_order_id,
                                                         'qty': '100'}],
                                                    overdraft=1,
                                                    login='aikawa-test-10',
                                                    passport_uid=327225081)
    utils.check_that(result, deep_equals_to({'overdraft': {'available_sum': '3000',
                                                           'available_sum_ue': '100',
                                                           'currency': None,
                                                           'expired_sum': '0',
                                                           'expired_sum_ue': '0',
                                                           'is_available': False,
                                                           'is_present': False,
                                                           'iso_currency': None,
                                                           'min_days_to_live': None,
                                                           'nearly_expired_sum': '0',
                                                           'nearly_expired_sum_ue': '0',
                                                           'overdraft_iso_currency': None,
                                                           'show_notify': True,
                                                           'spent_sum': '0',
                                                           'spent_sum_ue': '0',
                                                           'warnings': {}},
                                             'status_code': 3,
                                             'status_desc': 'Overdraft not available'}))


@pytest.mark.parametrize('params, overdraft_value, fixed_currency', [
    pytest.param({'overdraft_given_to': DIRECT_YANDEX_FIRM_FISH, 'overdraft_taken_by': DIRECT_YANDEX_FIRM_FISH},
                 'non_enough',
                 False,
                 id='Direct fish not enough limit'),
    pytest.param({'overdraft_given_to': DIRECT_BEL_FIRM_FISH, 'overdraft_taken_by': DIRECT_BEL_FIRM_FISH},
                 'enough_limit',
                 False,
                 id='Direct fish enough limit'),

    pytest.param({'overdraft_given_to': MARKET_FIRM_FISH, 'overdraft_taken_by': MARKET_FIRM_FISH},
                 'non_enough',
                 False,
                 marks=pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_11)),
                 id='Market not enough limit'),

    pytest.param({'overdraft_given_to': MARKET_FIRM_FISH, 'overdraft_taken_by': MARKET_FIRM_FISH},
                 'enough_limit',
                 False,
                 id='Market enough limit'),

    pytest.param({'overdraft_given_to': GEO_YANDEX_FIRM_FISH, 'overdraft_taken_by': GEO_YANDEX_FIRM_FISH},
                 'non_enough',
                 True,
                 id='Geo not enough limit'),
    pytest.param({'overdraft_given_to': GEO_YANDEX_FIRM_FISH, 'overdraft_taken_by': GEO_YANDEX_FIRM_FISH},
                 'enough_limit',
                 True,
                 id='Geo enough limit'),

    pytest.param({'overdraft_given_to': GEO_KZ_FIRM_FISH, 'overdraft_taken_by': GEO_KZ_FIRM_FISH},
                 'non_enough',
                 True,
                 id='Geo Kz not enough limit'),
    pytest.param({'overdraft_given_to': GEO_KZ_FIRM_FISH, 'overdraft_taken_by': GEO_KZ_FIRM_FISH},
                 'enough_limit',
                 True,
                 id='Geo Kz enough limit'),

    pytest.param({'overdraft_given_to': GEO_BEL_FIRM_FISH, 'overdraft_taken_by': GEO_BEL_FIRM_FISH},
                 'non_enough',
                 True,
                 id='Geo Bel not enough limit'),
    pytest.param({'overdraft_given_to': GEO_BEL_FIRM_FISH, 'overdraft_taken_by': GEO_BEL_FIRM_FISH},
                 'enough_limit',
                 True,
                 id='Geo Bel enough limit'),

    pytest.param({'overdraft_given_to': VENDORS_MARKET_FIRM_FISH, 'overdraft_taken_by': VENDORS_MARKET_FIRM_FISH},
                 'non_enough',
                 True,
                 id='Vendors not enough limit'),
    pytest.param({'overdraft_given_to': VENDORS_MARKET_FIRM_FISH, 'overdraft_taken_by': VENDORS_MARKET_FIRM_FISH},
                 'enough_limit',
                 True,
                 id='Vendors enough limit'),
])
def test_overdraft_usage_medium(params, overdraft_value, fixed_currency):
    client_id = steps.ClientSteps.create()
    overdraft_given_to_context = params['overdraft_given_to']
    if overdraft_value == 'enough_limit':
        limit = 100
    else:
        limit = 50

    currency = None if not fixed_currency else overdraft_given_to_context.currency.iso_code
    if currency:
        price = get_active_price(overdraft_given_to_context.product.id, currency)['price']
        limit *= price
    steps.OverdraftSteps.set_force_overdraft(client_id, overdraft_given_to_context.service.id, limit,
                                             overdraft_given_to_context.firm.id, currency=currency)
    actual_limit = db.balance().get_overdraft_limit_by_firm(client_id, overdraft_given_to_context.service.id,
                                                            overdraft_given_to_context.firm.id)
    assert limit == actual_limit

    overdraft_taken_by_context = params['overdraft_taken_by']
    person_id = steps.PersonSteps.create(client_id, overdraft_taken_by_context.person_type.code)
    service_order_id = steps.OrderSteps.next_id(overdraft_taken_by_context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=overdraft_taken_by_context.service.id,
                            product_id=overdraft_taken_by_context.product.id)

    orders_list = [
        {'ServiceID': overdraft_taken_by_context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 100,
         'BeginDT': dt}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': dt})
    try:
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, overdraft_taken_by_context.paysys.id,
                                                     credit=0, overdraft=1, contract_id=None)

        assert overdraft_value == 'enough_limit' and overdraft_given_to_context == overdraft_taken_by_context
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc), equal_to(u'NOT_ENOUGH_OVERDRAFT_LIMIT'))
        assert overdraft_value != 'enough_limit' or overdraft_given_to_context != overdraft_taken_by_context


@pytest.mark.parametrize('params, overdraft_value', [
    pytest.param({'overdraft_given_to': DIRECT_YANDEX_FIRM_RUB, 'overdraft_taken_by': DIRECT_YANDEX_FIRM_RUB},
                 'enough_limit', id='Direct Yandex RUB enough limit'),
    pytest.param({'overdraft_given_to': DIRECT_BEL_FIRM_BYN, 'overdraft_taken_by': DIRECT_BEL_FIRM_BYN},
                 'enough_limit', id='Direct Bel BYN enough limit'),
    pytest.param({'overdraft_given_to': DIRECT_KZ_FIRM_KZU, 'overdraft_taken_by': DIRECT_KZ_FIRM_KZU},
                 'enough_limit', id='Direct Kz KZU enough limit'),
    pytest.param({'overdraft_given_to': DIRECT_QUASI_BEL_FIRM_BYN, 'overdraft_taken_by': DIRECT_QUASI_BEL_FIRM_BYN},
                 'enough_limit', id='Direct quasi Bel BYN enough limit'),
    pytest.param({'overdraft_given_to': DIRECT_QUASI_KZ_FIRM_KZU, 'overdraft_taken_by': DIRECT_QUASI_KZ_FIRM_KZU},
                 'enough_limit', id='Direct quasi Kz KZU enough limit'),
    pytest.param({'overdraft_given_to': DIRECT_YANDEX_FIRM_RUB, 'overdraft_taken_by': DIRECT_YANDEX_FIRM_RUB},
                 'non_enough',
                 marks=pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_11)),
                 id='Direct Yandex RUB not enough limit'),
    pytest.param({'overdraft_given_to': DIRECT_BEL_FIRM_BYN, 'overdraft_taken_by': DIRECT_BEL_FIRM_BYN},
                 'non_enough',
                 id='Direct Bel BYN not enough limit'),
    pytest.param({'overdraft_given_to': DIRECT_KZ_FIRM_KZU, 'overdraft_taken_by': DIRECT_KZ_FIRM_KZU},
                 'non_enough',
                 id='Direct Kz KZU not enough limit'),
    pytest.param({'overdraft_given_to': DIRECT_QUASI_BEL_FIRM_BYN, 'overdraft_taken_by': DIRECT_QUASI_BEL_FIRM_BYN},
                 'non_enough',
                 id='Direct quasi Bel BYN not enough limit'),
    pytest.param({'overdraft_given_to': DIRECT_QUASI_KZ_FIRM_KZU, 'overdraft_taken_by': DIRECT_QUASI_KZ_FIRM_KZU},
                 'non_enough',
                 id='Direct quasi Kz KZU not enough limit'),
    pytest.param({'overdraft_given_to': AUTORU_FIRM_RUB, 'overdraft_taken_by': AUTORU_FIRM_RUB},
                 'enough_limit',
                 id='Autoru enough limit'),
    pytest.param({'overdraft_given_to': AUTORU_FIRM_RUB, 'overdraft_taken_by': AUTORU_FIRM_RUB},
                 'non_enough',
                 marks=pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_11)),
                 id='Autoru not enough limit'),
])
def test_overdraft_usage_currency_medium(params, overdraft_value):
    overdraft_given_to_context = params['overdraft_given_to']
    client_id = steps.ClientSteps.create_multicurrency(currency_convert_type='COPY',
                                                       service_id=overdraft_given_to_context.service.id,
                                                       region_id=overdraft_given_to_context.region.id,
                                                       currency=overdraft_given_to_context.currency.iso_code)

    if overdraft_value == 'enough_limit':
        limit = 100
    else:
        limit = 50

    steps.OverdraftSteps.set_force_overdraft(client_id, overdraft_given_to_context.service.id, limit,
                                             overdraft_given_to_context.firm.id,
                                             currency=overdraft_given_to_context.currency.iso_code)
    actual_limit = db.balance().get_overdraft_limit_by_firm(client_id, overdraft_given_to_context.service.id,
                                                            overdraft_given_to_context.firm.id)
    assert limit == actual_limit

    overdraft_taken_by_context = params['overdraft_taken_by']
    person_id = steps.PersonSteps.create(client_id, overdraft_taken_by_context.person_type.code)
    service_order_id = steps.OrderSteps.next_id(overdraft_taken_by_context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=overdraft_taken_by_context.service.id,
                            product_id=overdraft_taken_by_context.product.id)

    orders_list = [
        {'ServiceID': overdraft_taken_by_context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 80,
         'BeginDT': dt}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': dt + datetime.timedelta(days=1)})
    try:
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, overdraft_taken_by_context.paysys.id,
                                                     credit=0, overdraft=1, contract_id=None)

        assert overdraft_value == 'enough_limit' and overdraft_given_to_context == overdraft_taken_by_context
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc), equal_to(u'NOT_ENOUGH_OVERDRAFT_LIMIT'))
        assert overdraft_value != 'enough_limit' or overdraft_given_to_context != overdraft_taken_by_context


@pytest.mark.parametrize('params', [
    {'overdraft_given_to': DIRECT_YANDEX_FIRM_FISH_NON_RESIDENT,
     'overdraft_taken_by': DIRECT_YANDEX_FIRM_FISH_NON_RESIDENT}])
@pytest.mark.parametrize('overdraft_value', ['enough_limit'])
def test_overdraft_usage_non_resident(params, overdraft_value):
    client_id = steps.ClientSteps.create()
    overdraft_given_to_context = params['overdraft_given_to']
    if overdraft_value != 'null':
        if overdraft_value == 'enough_limit':
            limit = 100
        else:
            limit = 50

        steps.OverdraftSteps.set_force_overdraft(client_id, overdraft_given_to_context.service.id, limit,
                                                 overdraft_given_to_context.firm.id)
        actual_limit = db.balance().get_overdraft_limit_by_firm(client_id, overdraft_given_to_context.service.id,
                                                                overdraft_given_to_context.firm.id)
        assert limit == actual_limit

        overdraft_taken_by_context = params['overdraft_taken_by']
        person_id = steps.PersonSteps.create(client_id, overdraft_taken_by_context.person_type.code)
        service_order_id = steps.OrderSteps.next_id(overdraft_taken_by_context.service.id)
        steps.OrderSteps.create(client_id, service_order_id, service_id=overdraft_taken_by_context.service.id,
                                product_id=overdraft_taken_by_context.product.id)

        orders_list = [
            {'ServiceID': overdraft_taken_by_context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 10,
             'BeginDT': dt}]
        request_id = steps.RequestSteps.create(client_id, orders_list,
                                               additional_params={'InvoiceDesireDT': dt})
        try:
            invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, overdraft_taken_by_context.paysys.id,
                                                         credit=0, overdraft=1, contract_id=None)
        except Exception, exc:
            utils.check_that(steps.CommonSteps.get_exception_code(exc), equal_to(u'NOT_ENOUGH_OVERDRAFT_LIMIT'))


def test_overdraft_usage_multi_service_cart():
    client_id = steps.ClientSteps.create(enable_single_account=True, single_account_activated=True)
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    steps.ClientSteps.create_single_account(client_id)

    steps.OverdraftSteps.set_force_overdraft(client_id, Services.DIRECT.id, 100, Firms.YANDEX_1.id)
    session, token = snout_steps.CartSteps.get_session_and_token(client_id)

    for context in [DIRECT_YANDEX_FIRM_FISH, GEO_YANDEX_FIRM_FISH]:
        service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)  # внешний ID заказа

        steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                product_id=context.product.id, params={'AgencyID': None})

        add_res = snout_steps.CartSteps.post_item_add(session, context.service.id, service_order_id, 100, None, token)

    item_ids = [i['id'] for i in add_res.json()['data']['items']]
    snout_steps.CartSteps.post_create_request(session, _csrf=token, item_ids=item_ids)
    request_id = db.get_requests_by_client(client_id)[0]['id']

    with pytest.raises(xmlrpclib.Fault) as exc_info:
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id,
                                                     credit=0, overdraft=1, contract_id=None)
    utils.check_that(steps.CommonSteps.get_exception_code(exc_info.value), equal_to(u'NOT_ENOUGH_OVERDRAFT_LIMIT'))


@pytest.mark.parametrize('single_account_activated', [True, False])
def test_overdraft_usage_mono_service_cart(single_account_activated):
    client_id = steps.ClientSteps.create(enable_single_account=True, single_account_activated=single_account_activated)
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)

    steps.OverdraftSteps.set_force_overdraft(client_id, Services.DIRECT.id, 100, Firms.YANDEX_1.id)
    session, token = snout_steps.CartSteps.get_session_and_token(client_id)

    for context in [DIRECT_YANDEX_FIRM_FISH]:
        service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
        steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                product_id=context.product.id, params={'AgencyID': None})

        add_res = snout_steps.CartSteps.post_item_add(session, context.service.id, service_order_id, 100, None, token)

    item_ids = [i['id'] for i in add_res.json()['data']['items']]

    snout_steps.CartSteps.post_create_request(session, _csrf=token, item_ids=item_ids)
    request = db.get_requests_by_client(client_id)

    if not single_account_activated:
        assert len(request) == 0
        return

    request_id = request[0]['id']

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id,
                                                 credit=0, overdraft=1, contract_id=None)
    invoice = db.get_invoice_by_id(invoice_id)[0]
    assert invoice['type'] == 'overdraft'