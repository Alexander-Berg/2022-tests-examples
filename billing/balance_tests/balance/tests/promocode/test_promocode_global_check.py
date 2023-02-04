# coding: utf-8
import datetime

import hamcrest
import pytest
from xmlrpclib import Fault
import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features
from btestlib.constants import PromocodeClass, Products, Firms, Regions, Currencies, Services, Paysyses
from temp.igogor.balance_objects import Contexts

pytestmark = [pytest.mark.priority('mid'),
              reporter.feature(Features.PROMOCODE, Features.REQUEST, Features.INVOICE)]

dt = datetime.datetime.now()

dt_1_day_before = dt - datetime.timedelta(days=1)
dt_1_day_after = dt + datetime.timedelta(days=1)

dt_11_month_ago = utils.Date.shift_date(dt, months=-11)  # should be 12, but didnt work at the last of the month
dt_13_month_ago = utils.Date.shift_date(dt, months=-13)

DIRECT_FISH_RUB_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, product=Products.DIRECT_FISH,
                                                               region=Regions.RU, currency=Currencies.RUB)
MARKET_RUB_CONTEXT = Contexts.MARKET_RUB_CONTEXT.new()
AUTORU_RUB_CONTEXT = Contexts.MARKET_RUB_CONTEXT.new(service=Services.AUTORU)
QTY = 500
PROMOCODE_BONUS = 20


def create_request(context, client_id, promocode_code=None, invoice_dt=None, agency_id=None, deny_promocode=None):
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id, params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': dt}]
    request_id = steps.RequestSteps.create(client_id=agency_id if agency_id else client_id, orders_list=orders_list,
                                           additional_params={'PromoCode': promocode_code,
                                                              'InvoiceDesireDT': invoice_dt,
                                                              'DenyPromocode': deny_promocode})
    return request_id, orders_list


def create_and_reserve_promocode(client_id=None, firm_id=1, is_global_unique=0, start_dt=None, end_dt=None,
                                     new_clients_only=0, valid_until_paid=1, need_unique_urls=0, middle_dt=None,
                                     bonus1=0, bonus2=0, discount_pct=0, service_ids=None,
                                     skip_reservation_check=False):
    promocode_type = PromocodeClass.LEGACY_PROMO
    code = steps.PromocodeSteps.generate_code()
    start_dt = dt_1_day_before if start_dt is None else start_dt
    middle_dt = middle_dt or (start_dt + datetime.timedelta(seconds=1))
    end_dt = dt_1_day_after if end_dt is None else end_dt
    calc_params = steps.PromocodeSteps.fill_calc_params(promocode_type=promocode_type,
                                                        middle_dt=middle_dt,
                                                        discount_pct=discount_pct,
                                                        bonus1=bonus1, bonus2=bonus2,
                                                        minimal_qty=100)
    result = steps.PromocodeSteps.create_new(calc_class_name=promocode_type,
                                             end_dt=end_dt,
                                             promocodes=[code],
                                             start_dt=start_dt,
                                             calc_params=calc_params,
                                             firm_id=firm_id,
                                             is_global_unique=is_global_unique,
                                             valid_until_paid=valid_until_paid,
                                             service_ids=service_ids,
                                             new_clients_only=new_clients_only,
                                             need_unique_urls=need_unique_urls,
                                             skip_reservation_check=skip_reservation_check)[0]
    if client_id:
        steps.PromocodeSteps.make_reservation(client_id, result['id'], start_dt, end_dt)
    return result['id'], result['code']


def create_payed_invoice(request_id, person_id, context):
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=context.paysys.id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id)
    return invoice_id


def create_act(client_id, person_id, context, dt, promocode_code=None):
    request_id, orders_list = create_request(context, client_id, promocode_code=promocode_code,
                                             invoice_dt=dt)
    create_payed_invoice(request_id=request_id, person_id=person_id, context=context)
    if promocode_code:
        discount = steps.PromocodeSteps.calculate_static_discount(qty=QTY, bonus=PROMOCODE_BONUS)
        qty = steps.PromocodeSteps.calculate_qty_with_static_discount(qty=QTY, discount=discount, precision='0.000001')
    else:
        qty = QTY
    steps.CampaignsSteps.do_campaigns(context.service.id, orders_list[0]['ServiceOrderID'],
                                      {context.product.type.code: qty}, 0, dt)
    return steps.ActsSteps.generate(client_id, force=1, date=dt)[0], request_id


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('service_id', [DIRECT_FISH_RUB_CONTEXT])
def test_invalid_promo_code_request(service_id):
    client_id = steps.ClientSteps.create()
    promocode_code = steps.PromocodeSteps.generate_code()
    with pytest.raises(Fault) as exc:
        create_request(service_id, client_id, promocode_code)
    utils.check_that('Invalid promo code: ID_PC_UNKNOWN',
                     hamcrest.equal_to(steps.CommonSteps.get_exception_code(exc.value, 'contents')))


def test_create_discount_promo():
    client_id = steps.ClientSteps.create()
    create_and_reserve_promocode(client_id=client_id, start_dt=dt_1_day_before,
                                 end_dt=dt_1_day_after, middle_dt=dt_1_day_after,
                                 bonus1=0, bonus2=0, discount_pct=1)


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('with_subclient', [True, False])
@pytest.mark.parametrize('context', [DIRECT_FISH_RUB_CONTEXT])
def test_promo_code_agency_request(context, with_subclient):
    agency_id = steps.ClientSteps.create_agency()
    promocode_id, promocode_code = create_and_reserve_promocode(client_id=None, bonus1=PROMOCODE_BONUS,
                                                                bonus2=PROMOCODE_BONUS)
    if with_subclient:
        client_id = steps.ClientSteps.create()
    else:
        agency_id, client_id = None, agency_id
    with pytest.raises(Fault) as exc:
        create_request(context, client_id=client_id, promocode_code=promocode_code, agency_id=agency_id)
    utils.check_that('Invalid promo code: ID_PC_NON_DIRECT_CLIENT',
                     hamcrest.equal_to(steps.CommonSteps.get_exception_code(exc.value, 'contents')))


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('context', [DIRECT_FISH_RUB_CONTEXT])
def test_promo_code_turn_on_non_payed_invoice(context):
    client_id = steps.ClientSteps.create()
    promocode_id, promocode_code = create_and_reserve_promocode(client_id=client_id, bonus1=PROMOCODE_BONUS,
                                                                bonus2=PROMOCODE_BONUS)
    request_id, _ = create_request(context, client_id)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=Paysyses.CC_UR_RUB.id, credit=0, contract_id=None, overdraft=0,
                                                 endbuyer_id=None)
    steps.InvoiceSteps.turn_on(invoice_id)
    utils.check_that(steps.PromocodeSteps.is_invoice_with_promo(promocode_id, invoice_id), hamcrest.equal_to(True))
    steps.PromocodeSteps.check_invoice_is_with_discount(invoice_id, bonus=PROMOCODE_BONUS, is_with_discount=True,
                                                        qty=QTY)


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('context', [DIRECT_FISH_RUB_CONTEXT])
def test_promo_code_is_reserved_on_client(context):
    client_id = steps.ClientSteps.create()
    promocode_id, promocode_code = create_and_reserve_promocode(client_id=client_id, bonus1=PROMOCODE_BONUS,
                                                                bonus2=PROMOCODE_BONUS)

    # промокод зарезервирован за клиентом, но к первому счету он не сможет примениться
    request_id, _ = create_request(context, client_id)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=context.paysys.id,
                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)

    # а второй счет уже подходит по параметрам
    request_id, _ = create_request(context, client_id)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    invoice_id = create_payed_invoice(request_id, person_id, context)
    utils.check_that(steps.PromocodeSteps.is_invoice_with_promo(promocode_id, invoice_id), hamcrest.equal_to(True))
    steps.PromocodeSteps.check_invoice_is_with_discount(invoice_id, bonus=PROMOCODE_BONUS, is_with_discount=True,
                                                        qty=QTY)


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('context', [DIRECT_FISH_RUB_CONTEXT])
@pytest.mark.parametrize('params',
                         [
                             {'firm_id': 1, 'expected': True},
                             {'firm_id': 2, 'expected': False}
                         ]
                         )
def test_promo_code_firm_mismatch(params, context):
    client_id = steps.ClientSteps.create()
    promocode_id, promocode_code = create_and_reserve_promocode(client_id=client_id, bonus1=PROMOCODE_BONUS,
                                                                bonus2=PROMOCODE_BONUS,
                                                                firm_id=params['firm_id'])
    request_id, _ = create_request(context, client_id)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    invoice_id = create_payed_invoice(request_id=request_id, person_id=person_id, context=context)

    invoice = db.get_invoice_by_id(invoice_id)[0]
    firm_id = invoice['firm_id']
    if params['expected']:
        utils.check_that(params['firm_id'], hamcrest.equal_to(firm_id))
    else:
        utils.check_that(params['firm_id'], hamcrest.is_not(firm_id))
    utils.check_that(steps.PromocodeSteps.is_invoice_with_promo(promocode_id, invoice_id),
                     hamcrest.equal_to(params['expected']))
    steps.PromocodeSteps.check_invoice_is_with_discount(invoice_id, bonus=PROMOCODE_BONUS,
                                                        is_with_discount=params['expected'], qty=QTY)


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('context', [DIRECT_FISH_RUB_CONTEXT])
@pytest.mark.parametrize('params',
                         [
                             {'is_global_unique': 1, 'is_first_invoice_without_promo': True,
                              'is_with_exception': True},
                             {'is_global_unique': 1, 'is_first_invoice_without_promo': False,
                              'is_with_exception': False},
                             {'is_global_unique': 0, 'is_first_invoice_without_promo': False,
                              'is_with_exception': True}
                         ]
                         )
def test_promo_code_is_global_unique_request(params, context):
    client_id = steps.ClientSteps.create()
    promocode_id, promocode_code = create_and_reserve_promocode(client_id=None, bonus1=PROMOCODE_BONUS,
                                                                bonus2=PROMOCODE_BONUS,
                                                                is_global_unique=params['is_global_unique'])
    request_id, orders_list = create_request(context, client_id, promocode_code)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    create_payed_invoice(request_id=request_id, person_id=person_id, context=context)
    if params['is_first_invoice_without_promo']:
        steps.PromocodeSteps.delete_promocode_from_invoices(promocode_id)
    try:
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                               additional_params=dict(PromoCode=promocode_code))
    except Exception, exc:
        utils.check_that(hamcrest.equal_to(steps.CommonSteps.get_exception_code(exc, 'contents')),
                         'Invalid promo code: ID_PC_USED')
        utils.check_that(params['is_with_exception'], hamcrest.equal_to(False))
    else:
        utils.check_that(params['is_with_exception'], hamcrest.equal_to(True))
        utils.check_that(steps.PromocodeSteps.is_request_with_promo(promocode_id, request_id),
                         hamcrest.equal_to(True))


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('context', [DIRECT_FISH_RUB_CONTEXT])
@pytest.mark.parametrize('params',
                         [
                             {'is_global_unique': 1, 'is_first_invoice_without_promo': True,
                              'is_invoice_with_promo': True},
                             {'is_global_unique': 1, 'is_first_invoice_without_promo': False,
                              'is_invoice_with_promo': False},
                             {'is_global_unique': 0, 'is_first_invoice_without_promo': False,
                              'is_invoice_with_promo': True}
                         ]
                         )
def test_promo_code_is_global_unique_invoice(context, params):
    client_id = steps.ClientSteps.create()
    promocode_id, promocode_code = create_and_reserve_promocode(client_id=client_id, bonus1=PROMOCODE_BONUS,
                                                                bonus2=PROMOCODE_BONUS,
                                                                is_global_unique=params['is_global_unique'])
    request_id, _ = create_request(context, client_id)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    create_payed_invoice(request_id=request_id, person_id=person_id, context=context)
    if params['is_first_invoice_without_promo']:
        steps.PromocodeSteps.delete_promocode_from_invoices(promocode_id)
    invoice_id = create_payed_invoice(request_id=request_id, person_id=person_id, context=context)

    utils.check_that(steps.PromocodeSteps.is_request_with_promo(promocode_id, request_id),
                     hamcrest.equal_to(False))
    utils.check_that(steps.PromocodeSteps.is_invoice_with_promo(promocode_id, invoice_id),
                     hamcrest.equal_to(params['is_invoice_with_promo']))
    steps.PromocodeSteps.check_invoice_is_with_discount(invoice_id, bonus=PROMOCODE_BONUS,
                                                        is_with_discount=params['is_invoice_with_promo'], qty=QTY)


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('context', [DIRECT_FISH_RUB_CONTEXT])
@pytest.mark.parametrize('params',
                         [
                             {'promocode_services': None,
                              'is_request_with_promo': True},

                             {'promocode_services': [AUTORU_RUB_CONTEXT],
                              'is_request_with_promo': False},

                             {'promocode_services': [AUTORU_RUB_CONTEXT, DIRECT_FISH_RUB_CONTEXT],
                              'is_request_with_promo': True},
                         ]
                         )
def test_promo_code_service_check_request(context, params):
    client_id = steps.ClientSteps.create()

    if params['promocode_services']:
        services_list = [promo_context.service.id for promo_context in params['promocode_services']]
    else:
        services_list = None

    promocode_id, promocode_code = create_and_reserve_promocode(
        client_id=None,
        bonus1=PROMOCODE_BONUS,
        bonus2=PROMOCODE_BONUS,
        service_ids=services_list
    )

    try:
        request_id, _ = create_request(context, client_id, promocode_code)
    except Exception, exc:
        utils.check_that('Invalid promo code: ID_PC_NO_MATCHING_ROWS',
                         hamcrest.equal_to(steps.CommonSteps.get_exception_code(exc, 'contents')))
        utils.check_that(params['is_request_with_promo'], hamcrest.equal_to(False))
    else:
        utils.check_that(params['is_request_with_promo'], hamcrest.equal_to(True))
        utils.check_that(steps.PromocodeSteps.is_request_with_promo(promocode_id, request_id),
                         hamcrest.equal_to(True))


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('context', [DIRECT_FISH_RUB_CONTEXT])
@pytest.mark.parametrize('params',
                         [
                             {'promocode_services': None, 'is_invoice_with_promo': True},

                             {'promocode_services': [AUTORU_RUB_CONTEXT], 'is_invoice_with_promo': False},

                             {'promocode_services': [AUTORU_RUB_CONTEXT, DIRECT_FISH_RUB_CONTEXT],
                              'is_invoice_with_promo': True},
                         ]
                         )
def test_promo_code_service_check_invoice(context, params):
    client_id = steps.ClientSteps.create()
    request_id, _ = create_request(context, client_id)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    if params['promocode_services']:
        services_list = [context_from_param.service.id for context_from_param in params['promocode_services']]
    else:
        services_list = None

    promocode_id, promocode_code = create_and_reserve_promocode(
        client_id=client_id,
        bonus1=PROMOCODE_BONUS,
        bonus2=PROMOCODE_BONUS,
        service_ids=services_list
    )

    invoice_id = create_payed_invoice(request_id, person_id, context)
    utils.check_that(steps.PromocodeSteps.is_invoice_with_promo(promocode_id, invoice_id),
                     hamcrest.equal_to(params['is_invoice_with_promo']))
    steps.PromocodeSteps.check_invoice_is_with_discount(invoice_id, bonus=PROMOCODE_BONUS,
                                                        is_with_discount=params['is_invoice_with_promo'],
                                                        qty=QTY)


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('context', [DIRECT_FISH_RUB_CONTEXT])
@pytest.mark.parametrize('params',
                         [
                             {'dt': dt_11_month_ago, 'new_clients_only': 1, 'is_without_exception': False},
                             {'dt': dt_11_month_ago, 'new_clients_only': 0, 'is_without_exception': True},
                             {'dt': dt_13_month_ago, 'new_clients_only': 1, 'is_without_exception': True},

                         ]
                         )
def test_promo_code_is_new_clients_only_request(context, params):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    create_act(client_id, person_id, context, params['dt'])
    promocode_id, promocode_code = create_and_reserve_promocode(client_id=None, bonus1=PROMOCODE_BONUS,
                                                                bonus2=PROMOCODE_BONUS,
                                                                new_clients_only=params['new_clients_only'])
    try:
        request_id, _ = create_request(context, client_id, promocode_code)
        utils.check_that(params['is_without_exception'], hamcrest.equal_to(True))
        utils.check_that(steps.PromocodeSteps.is_request_with_promo(promocode_id, request_id),
                         hamcrest.equal_to(True))
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc, 'contents'),
                         hamcrest.equal_to('Invalid promo code: ID_PC_NOT_NEW_CLIENT'))
        utils.check_that(params['is_without_exception'], hamcrest.equal_to(False))


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('context', [DIRECT_FISH_RUB_CONTEXT])
@pytest.mark.parametrize('params',
                         [
                             {'dt': dt_11_month_ago, 'new_clients_only': 1, 'is_without_exception': False},
                             {'dt': dt_11_month_ago, 'new_clients_only': 0, 'is_without_exception': True},
                             {'dt': dt_13_month_ago, 'new_clients_only': 1, 'is_without_exception': True}]
                         )
def test_promo_code_is_new_clients_only_invoice(context, params):
    client_id = steps.ClientSteps.create()
    promocode_id, promocode_code = create_and_reserve_promocode(client_id=client_id, bonus1=PROMOCODE_BONUS,
                                                                bonus2=PROMOCODE_BONUS,
                                                                new_clients_only=params['new_clients_only'],
                                                                start_dt=params['dt'])
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    act_id, request_id = create_act(client_id, person_id, context, params['dt'])
    invoice_id = create_payed_invoice(request_id, person_id, context)
    utils.check_that(steps.PromocodeSteps.is_invoice_with_promo(promocode_id, invoice_id),
                     hamcrest.equal_to(params['is_without_exception']))


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('context', [DIRECT_FISH_RUB_CONTEXT])
@pytest.mark.parametrize('params',
                         [
                             {'valid_until_paid': 0, 'first_promo_on_another_client': True,
                              'is_request_with_promo': True},
                             {'valid_until_paid': 0, 'first_promo_on_another_client': False,
                              'is_request_with_promo': False},
                             {'valid_until_paid': 1, 'first_promo_on_another_client': True,
                              'is_request_with_promo': True},
                             {'valid_until_paid': 1, 'first_promo_on_another_client': False,
                              'is_request_with_promo': True},
                         ]
                         )
def test_promo_code_is_valid_until_paid_request(context, params):
    client_id = steps.ClientSteps.create()
    client_for_promo = client_id
    if params['first_promo_on_another_client']:
        another_client = steps.ClientSteps.create()
        client_for_promo = another_client
    promocode_id, promocode_code = create_and_reserve_promocode(client_id=None, bonus1=PROMOCODE_BONUS,
                                                                bonus2=PROMOCODE_BONUS,
                                                                valid_until_paid=params['valid_until_paid'])
    request_id, orders_list = create_request(context, client_for_promo, promocode_code)
    person_id = steps.PersonSteps.create(client_for_promo, context.person_type.code)
    create_payed_invoice(request_id=request_id, person_id=person_id, context=context)
    steps.PromocodeSteps.delete_reservation_for_client(client_for_promo)
    try:

        request_id, orders_list = create_request(context, client_id, promocode_code)
        utils.check_that(params['is_request_with_promo'], hamcrest.equal_to(True))
        utils.check_that(steps.PromocodeSteps.is_request_with_promo(promocode_id, request_id),
                         hamcrest.equal_to(True))
    except Exception, exc:
        utils.check_that('Invalid promo code: ID_PC_MANY_ISSUED_INVOICES',
                         hamcrest.equal_to(steps.CommonSteps.get_exception_code(exc, 'contents')))
        utils.check_that(params['is_request_with_promo'], hamcrest.equal_to(False))


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('context', [DIRECT_FISH_RUB_CONTEXT])
@pytest.mark.parametrize('params', [
    {'need_unique_urls': 1, 'deny_promocode': 1, 'is_invoice_with_promo': False}
    , {'need_unique_urls': 1, 'deny_promocode': 0, 'is_invoice_with_promo': True}
    , {'need_unique_urls': 0, 'deny_promocode': 1, 'is_invoice_with_promo': True}
    , {'need_unique_urls': 0, 'deny_promocode': 0, 'is_invoice_with_promo': True}
])
def test_unique_urls_and_deny_promocode(params, context):
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(client_id, context.person_type.code,
                                         {'email': 'test-balance-notify@yandex-team.ru'})
    promocode_id, promocode_code = create_and_reserve_promocode(client_id=client_id, bonus1=PROMOCODE_BONUS,
                                                                bonus2=PROMOCODE_BONUS,
                                                                need_unique_urls=params['need_unique_urls'])

    request_id, _ = create_request(context, client_id, deny_promocode=params['deny_promocode'])
    invoice_id = create_payed_invoice(request_id=request_id, person_id=person_id, context=context)
    utils.check_that(steps.PromocodeSteps.is_invoice_with_promo(promocode_id, invoice_id),
                     hamcrest.equal_to(params['is_invoice_with_promo']))
    steps.PromocodeSteps.check_invoice_is_with_discount(invoice_id, bonus=PROMOCODE_BONUS,
                                                        is_with_discount=params['is_invoice_with_promo'], qty=QTY)




    # перенести тест is_global_unique
