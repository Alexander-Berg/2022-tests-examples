# coding: utf-8

import hamcrest
import pytest
from xmlrpclib import Fault

from balance import snout_steps
import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features
from btestlib.constants import PromocodeClass, Services, PersonTypes, Firms, Products
from promocode_commons import (DT_1_DAY_AFTER, DT_1_DAY_BEFORE, NOW,
                               create_and_reserve_promocode,
                               fill_calc_params,
                               create_request,
                               create_payed_invoice,
                               is_request_with_promocode,
                               is_invoice_with_promocode,
                               check_invoice_consumes_discount,
                               DIRECT_YANDEX_FIRM_FISH,
                               delete_promocode_from_invoices,
                               create_act,
                               delete_reservation_for_client,
                               reserve)

pytestmark = [pytest.mark.priority('mid'),
              reporter.feature(Features.PROMOCODE, Features.REQUEST, Features.INVOICE)]

AUTORU_RUB_CONTEXT = DIRECT_YANDEX_FIRM_FISH.new(service=Services.AUTORU)
GEO_YANDEX_FIRM_FISH = DIRECT_YANDEX_FIRM_FISH.new(firm=Firms.YANDEX_1, service=Services.GEO,
                                                   product=Products.GEO)
FIXED_DISCOUNT_VALUE = 5
QTY = 10

dt_11_month_ago = utils.Date.shift_date(NOW, months=-11)  # should be 12, but didnt work at the last of the month
dt_13_month_ago = utils.Date.shift_date(NOW, months=-13)

PROMOCODE_BONUS = 20


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH])
@pytest.mark.parametrize('promocode_class', [PromocodeClass.FIXED_DISCOUNT])
def test_invalid_promo_code_request(context, promocode_class):
    client_id = steps.ClientSteps.create()
    promocode_code = steps.PromocodeSteps.generate_code()
    with pytest.raises(Fault) as exc:
        create_request(context, client_id, qty=QTY, promocode_code=promocode_code)
    utils.check_that('Invalid promo code: ID_PC_UNKNOWN',
                     hamcrest.equal_to(steps.CommonSteps.get_exception_code(exc.value, 'contents')))


@pytest.mark.parametrize('with_subclient', [True, False])
@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH])
@pytest.mark.parametrize('promocode_class', [PromocodeClass.FIXED_DISCOUNT])
def test_promo_code_agency_request(context, with_subclient, promocode_class):
    agency_id = steps.ClientSteps.create_agency()
    if with_subclient:
        client_id = steps.ClientSteps.create()
    else:
        agency_id, client_id = None, agency_id
    calc_params = fill_calc_params(calc_class_name=promocode_class, discount_pct=FIXED_DISCOUNT_VALUE)
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=promocode_class,
                                                                client_id=None,
                                                                start_dt=DT_1_DAY_BEFORE,
                                                                end_dt=DT_1_DAY_AFTER,
                                                                firm_id=context.firm.id,
                                                                calc_params=calc_params,
                                                                service_ids=[context.service.id])
    with pytest.raises(Fault) as exc:
        create_request(context, client_id=client_id, agency_id=agency_id, qty=QTY, promocode_code=promocode_code)
    utils.check_that('Invalid promo code: ID_PC_NON_DIRECT_CLIENT',
                     hamcrest.equal_to(steps.CommonSteps.get_exception_code(exc.value, 'contents')))


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH])
@pytest.mark.parametrize('promocode_class', [PromocodeClass.FIXED_DISCOUNT])
def test_promo_code_turn_on_non_payed_invoice(context, promocode_class):
    client_id = steps.ClientSteps.create()
    calc_params = fill_calc_params(calc_class_name=promocode_class, discount_pct=FIXED_DISCOUNT_VALUE)
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=promocode_class,
                                                                client_id=client_id,
                                                                start_dt=DT_1_DAY_BEFORE,
                                                                end_dt=DT_1_DAY_AFTER,
                                                                firm_id=context.firm.id,
                                                                calc_params=calc_params,
                                                                service_ids=[context.service.id])
    request_id, _ = create_request(context, client_id, qty=QTY)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=context.paysys.id, credit=0, contract_id=None, overdraft=0,
                                                 endbuyer_id=None)
    steps.InvoiceSteps.turn_on(invoice_id)
    utils.check_that(is_invoice_with_promocode(promocode_id, invoice_id), hamcrest.equal_to(True))
    check_invoice_consumes_discount(discount_pct=FIXED_DISCOUNT_VALUE, invoice_id=invoice_id,
                                    precision=context.precision,
                                    qty_before=[QTY], adjust_quantity=True,
                                    apply_on_create=False, classname=promocode_class)


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH])
@pytest.mark.parametrize('promocode_class', [PromocodeClass.FIXED_DISCOUNT])
def test_promo_code_is_reserved_on_client(context, promocode_class):
    client_id = steps.ClientSteps.create()
    calc_params = fill_calc_params(calc_class_name=promocode_class, discount_pct=FIXED_DISCOUNT_VALUE)
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=promocode_class,
                                                                client_id=client_id,
                                                                start_dt=DT_1_DAY_BEFORE,
                                                                end_dt=DT_1_DAY_AFTER,
                                                                firm_id=context.firm.id,
                                                                calc_params=calc_params,
                                                                service_ids=[context.service.id])

    # создаем реквест, счет, но не применяем промокод
    request_id, _ = create_request(context, client_id, qty=QTY)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=context.paysys.id,
                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)

    # к другому счету будет применен
    request_id, _ = create_request(context, client_id, qty=QTY)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    invoice_id = create_payed_invoice(request_id, person_id, context.paysys.id)
    utils.check_that(is_invoice_with_promocode(promocode_id, invoice_id), hamcrest.equal_to(True))
    check_invoice_consumes_discount(discount_pct=FIXED_DISCOUNT_VALUE, invoice_id=invoice_id,
                                    precision=context.precision,
                                    qty_before=[QTY], adjust_quantity=True,
                                    apply_on_create=False, classname=promocode_class)


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH])
@pytest.mark.parametrize('params',
                         [
                             {'firm_id': 1, 'is_invoice_with_promocode': True},
                             {'firm_id': 2, 'is_invoice_with_promocode': False}
                         ]
                         )
@pytest.mark.parametrize('promocode_class', [PromocodeClass.FIXED_DISCOUNT])
def test_promo_code_firm_mismatch(params, context, promocode_class):
    client_id = steps.ClientSteps.create()
    calc_params = fill_calc_params(calc_class_name=promocode_class, discount_pct=FIXED_DISCOUNT_VALUE)
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=promocode_class,
                                                                client_id=client_id,
                                                                start_dt=DT_1_DAY_BEFORE,
                                                                end_dt=DT_1_DAY_AFTER,
                                                                firm_id=params['firm_id'],
                                                                calc_params=calc_params,
                                                                service_ids=[context.service.id])
    request_id, _ = create_request(context, client_id, qty=QTY)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    invoice_id = create_payed_invoice(request_id=request_id, person_id=person_id, paysys_id=context.paysys.id)
    invoice_firm_id = db.get_invoice_by_id(invoice_id)[0]['firm_id']
    if params['is_invoice_with_promocode']:
        utils.check_that(params['firm_id'], hamcrest.equal_to(invoice_firm_id))
    else:
        utils.check_that(params['firm_id'], hamcrest.is_not(invoice_firm_id))
    utils.check_that(is_invoice_with_promocode(promocode_id, invoice_id),
                     hamcrest.equal_to(params['is_invoice_with_promocode']))
    discount_pct = FIXED_DISCOUNT_VALUE if params['is_invoice_with_promocode'] else 0
    check_invoice_consumes_discount(discount_pct=discount_pct, invoice_id=invoice_id, precision=context.precision,
                                    qty_before=[QTY], adjust_quantity=True,
                                    apply_on_create=False, classname=promocode_class)


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH])
@pytest.mark.parametrize('params',
                         [
                             {'is_global_unique': None, 'is_first_invoice_without_promo': True,
                              'is_with_exception': False},
                             {'is_global_unique': None, 'is_first_invoice_without_promo': False,
                              'is_with_exception': False},
                             {'is_global_unique': 1, 'is_first_invoice_without_promo': True,
                              'is_with_exception': False},
                             {'is_global_unique': 1, 'is_first_invoice_without_promo': False,
                              'is_with_exception': True},
                             {'is_global_unique': 0, 'is_first_invoice_without_promo': False,
                              'is_with_exception': False},
                             {'is_global_unique': 0, 'is_first_invoice_without_promo': True,
                              'is_with_exception': False}
                         ]
                         )
@pytest.mark.parametrize('promocode_class', [PromocodeClass.FIXED_DISCOUNT])
def test_promo_code_is_global_unique_request(params, context, promocode_class):
    client_id = steps.ClientSteps.create()
    calc_params = fill_calc_params(calc_class_name=promocode_class, discount_pct=FIXED_DISCOUNT_VALUE)
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=promocode_class,
                                                                client_id=None,
                                                                start_dt=DT_1_DAY_BEFORE,
                                                                end_dt=DT_1_DAY_AFTER,
                                                                firm_id=context.firm.id,
                                                                calc_params=calc_params,
                                                                service_ids=[context.service.id],
                                                                is_global_unique=params['is_global_unique'],
                                                                valid_until_paid=1)
    request_id, orders_list = create_request(context=context, client_id=client_id, promocode_code=promocode_code,
                                             qty=QTY)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    create_payed_invoice(request_id=request_id, person_id=person_id, paysys_id=context.paysys.id)
    if params['is_first_invoice_without_promo']:
        steps.PromocodeSteps.delete_promocode_from_invoices(promocode_id)
    try:
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                               additional_params=dict(PromoCode=promocode_code))
    except Exception, exc:
        utils.check_that(hamcrest.equal_to(steps.CommonSteps.get_exception_code(exc, 'contents')),
                         'Invalid promo code: ID_PC_USED')
        utils.check_that(params['is_with_exception'], hamcrest.equal_to(True))
    else:
        utils.check_that(params['is_with_exception'], hamcrest.equal_to(False))
        utils.check_that(is_request_with_promocode(promocode_id, request_id), hamcrest.equal_to(True))


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH])
@pytest.mark.parametrize('params',
                         [
                             {'is_global_unique': None, 'is_first_invoice_without_promo': True,
                              'is_with_exception': False},
                             {'is_global_unique': None, 'is_first_invoice_without_promo': False,
                              'is_with_exception': False},
                             {'is_global_unique': 1, 'is_first_invoice_without_promo': True,
                              'is_with_exception': False},
                             {'is_global_unique': 1, 'is_first_invoice_without_promo': False,
                              'is_with_exception': True},
                             {'is_global_unique': 0, 'is_first_invoice_without_promo': False,
                              'is_with_exception': False},
                             {'is_global_unique': 0, 'is_first_invoice_without_promo': True,
                              'is_with_exception': False}
                         ]
                         )
@pytest.mark.parametrize('promocode_class', [PromocodeClass.FIXED_DISCOUNT])
def test_promo_code_is_global_unique_invoice(context, params, promocode_class):
    client_id = steps.ClientSteps.create()
    calc_params = fill_calc_params(calc_class_name=promocode_class, discount_pct=FIXED_DISCOUNT_VALUE,
                                   apply_on_create=False, adjust_quantity=True)
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=promocode_class,
                                                                client_id=client_id,
                                                                start_dt=DT_1_DAY_BEFORE,
                                                                end_dt=DT_1_DAY_AFTER,
                                                                firm_id=context.firm.id,
                                                                calc_params=calc_params,
                                                                service_ids=[context.service.id],
                                                                is_global_unique=params['is_global_unique'],
                                                                valid_until_paid=1)
    request_id, orders_list = create_request(context=context, client_id=client_id, qty=QTY)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    create_payed_invoice(request_id=request_id, person_id=person_id, paysys_id=context.paysys.id)
    if params['is_first_invoice_without_promo']:
        delete_promocode_from_invoices(promocode_id)
    invoice_id = create_payed_invoice(request_id=request_id, person_id=person_id, paysys_id=context.paysys.id)

    utils.check_that(is_request_with_promocode(promocode_id, request_id), hamcrest.equal_to(False))
    utils.check_that(is_invoice_with_promocode(promocode_id, invoice_id),
                     hamcrest.is_not(params['is_with_exception']))
    discount_pct = FIXED_DISCOUNT_VALUE if not params['is_with_exception'] else 0
    check_invoice_consumes_discount(discount_pct=discount_pct, invoice_id=invoice_id, precision=context.precision,
                                    qty_before=[QTY], adjust_quantity=True,
                                    apply_on_create=False, classname=promocode_class)


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH])
@pytest.mark.parametrize('params',
                         [
                             {'promocode_services': None,
                              'is_request_with_promo': True},

                             {'promocode_services': [],
                              'is_request_with_promo': False},

                             {'promocode_services': [AUTORU_RUB_CONTEXT],
                              'is_request_with_promo': False},

                             {'promocode_services': [AUTORU_RUB_CONTEXT, DIRECT_YANDEX_FIRM_FISH],
                              'is_request_with_promo': True},
                         ]
                         )
@pytest.mark.parametrize('promocode_class', [PromocodeClass.FIXED_DISCOUNT])
def test_promo_code_service_check_request(context, params, promocode_class):
    client_id = steps.ClientSteps.create()

    calc_params = fill_calc_params(calc_class_name=promocode_class, discount_pct=FIXED_DISCOUNT_VALUE,
                                   apply_on_create=False, adjust_quantity=True)
    if params['promocode_services'] is None:
        services_list = None
    else:
        services_list = [context_from_param.service.id for context_from_param in params['promocode_services']]
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=promocode_class,
                                                                client_id=None,
                                                                start_dt=DT_1_DAY_BEFORE,
                                                                end_dt=DT_1_DAY_AFTER,
                                                                firm_id=context.firm.id,
                                                                calc_params=calc_params,
                                                                service_ids=services_list)
    try:
        request_id, _ = create_request(context, client_id, promocode_code=promocode_code, qty=QTY)
    except Exception, exc:
        utils.check_that('Invalid promo code: ID_PC_NO_MATCHING_ROWS',
                         hamcrest.equal_to(steps.CommonSteps.get_exception_code(exc, 'contents')))
        utils.check_that(params['is_request_with_promo'], hamcrest.equal_to(False))
    else:
        utils.check_that(params['is_request_with_promo'], hamcrest.equal_to(True))
        utils.check_that(is_request_with_promocode(promocode_id, request_id), hamcrest.equal_to(True))


@pytest.mark.parametrize('params',
                         [
                             {'promocode_services': None, 'is_invoice_with_promo': True},

                             {'promocode_services': [AUTORU_RUB_CONTEXT], 'is_invoice_with_promo': False},

                             {'promocode_services': [DIRECT_YANDEX_FIRM_FISH], 'is_invoice_with_promo': True},

                             {'promocode_services': [GEO_YANDEX_FIRM_FISH, DIRECT_YANDEX_FIRM_FISH],
                              'is_invoice_with_promo': True},
                         ]
                         )
@pytest.mark.parametrize('promocode_class', [PromocodeClass.FIXED_DISCOUNT])
def test_promo_code_service_check_invoice_els(params, promocode_class):
    client_id = steps.ClientSteps.create(enable_single_account=True, single_account_activated=True)
    session, token = snout_steps.CartSteps.get_session_and_token(client_id)

    for context in [DIRECT_YANDEX_FIRM_FISH, GEO_YANDEX_FIRM_FISH]:
        service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
        steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                product_id=context.product.id, params={'AgencyID': None})

        add_res = snout_steps.CartSteps.post_item_add(session, context.service.id, service_order_id, 100, None, token)

    item_ids = [i['id'] for i in add_res.json()['data']['items']]
    snout_steps.CartSteps.post_create_request(session, _csrf=token, item_ids=item_ids)

    request_id = db.get_requests_by_client(client_id)[0]['id']
    print steps.RequestSteps.get_url(request_id)
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)

    calc_params = fill_calc_params(calc_class_name=promocode_class, discount_pct=FIXED_DISCOUNT_VALUE,
                                   apply_on_create=False, adjust_quantity=True)
    if params['promocode_services'] is None:
        services_list = None
    else:
        services_list = [context_from_param.service.id for context_from_param in params['promocode_services']]
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=promocode_class,
                                                                client_id=client_id,
                                                                start_dt=DT_1_DAY_BEFORE,
                                                                end_dt=DT_1_DAY_AFTER,
                                                                firm_id=context.firm.id,
                                                                calc_params=calc_params,
                                                                service_ids=services_list)

    invoice_id = create_payed_invoice(request_id, person_id, paysys_id=context.paysys.id)
    utils.check_that(is_invoice_with_promocode(promocode_id, invoice_id),
                     hamcrest.equal_to(params['is_invoice_with_promo']))
    discount_pct = FIXED_DISCOUNT_VALUE if params['is_invoice_with_promo'] else 0
    check_invoice_consumes_discount(discount_pct=discount_pct, invoice_id=invoice_id,
                                    precision=context.precision,
                                    qty_before=[QTY], adjust_quantity=True,
                                    apply_on_create=False, classname=promocode_class)


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH])
@pytest.mark.parametrize('params',
                         [
                             {'promocode_services': None, 'is_invoice_with_promo': True},

                             {'promocode_services': [AUTORU_RUB_CONTEXT], 'is_invoice_with_promo': False},

                             {'promocode_services': [AUTORU_RUB_CONTEXT, DIRECT_YANDEX_FIRM_FISH],
                              'is_invoice_with_promo': True},
                         ]
                         )
@pytest.mark.parametrize('promocode_class', [PromocodeClass.FIXED_DISCOUNT])
def test_promo_code_service_check_invoice(context, params, promocode_class):
    client_id = steps.ClientSteps.create()
    request_id, _ = create_request(context, client_id, qty=QTY)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    calc_params = fill_calc_params(calc_class_name=promocode_class, discount_pct=FIXED_DISCOUNT_VALUE,
                                   apply_on_create=False, adjust_quantity=True)
    if params['promocode_services'] is None:
        services_list = None
    else:
        services_list = [context_from_param.service.id for context_from_param in params['promocode_services']]
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=promocode_class,
                                                                client_id=client_id,
                                                                start_dt=DT_1_DAY_BEFORE,
                                                                end_dt=DT_1_DAY_AFTER,
                                                                firm_id=context.firm.id,
                                                                calc_params=calc_params,
                                                                service_ids=services_list)

    invoice_id = create_payed_invoice(request_id, person_id, paysys_id=context.paysys.id)
    utils.check_that(is_invoice_with_promocode(promocode_id, invoice_id),
                     hamcrest.equal_to(params['is_invoice_with_promo']))
    discount_pct = FIXED_DISCOUNT_VALUE if params['is_invoice_with_promo'] else 0
    check_invoice_consumes_discount(discount_pct=discount_pct, invoice_id=invoice_id,
                                    precision=context.precision,
                                    qty_before=[QTY], adjust_quantity=True,
                                    apply_on_create=False, classname=promocode_class)


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH])
@pytest.mark.parametrize('params',
                         [
                             {'dt': dt_11_month_ago, 'new_clients_only': 1, 'is_without_exception': False},
                             {'dt': dt_11_month_ago, 'new_clients_only': 0, 'is_without_exception': True},
                             {'dt': dt_13_month_ago, 'new_clients_only': 1, 'is_without_exception': True},

                         ])
@pytest.mark.parametrize('promocode_class', [PromocodeClass.FIXED_DISCOUNT])
def test_promo_code_is_new_clients_only_request(context, params, promocode_class):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    create_act(client_id, person_id, context, params['dt'], qty=QTY)
    calc_params = fill_calc_params(calc_class_name=promocode_class, discount_pct=FIXED_DISCOUNT_VALUE,
                                   apply_on_create=False, adjust_quantity=True)
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=promocode_class,
                                                                client_id=None,
                                                                start_dt=params['dt'],
                                                                end_dt=DT_1_DAY_AFTER,
                                                                firm_id=context.firm.id,
                                                                calc_params=calc_params,
                                                                service_ids=[context.service.id],
                                                                new_clients_only=params['new_clients_only'],
                                                                valid_until_paid=1)
    try:
        request_id, _ = create_request(context, client_id, qty=QTY, promocode_code=promocode_code)
        utils.check_that(params['is_without_exception'], hamcrest.equal_to(True))
        utils.check_that(is_request_with_promocode(promocode_id, request_id), hamcrest.equal_to(True))
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc, 'contents'),
                         hamcrest.equal_to('Invalid promo code: ID_PC_NOT_NEW_CLIENT'))
        utils.check_that(params['is_without_exception'], hamcrest.equal_to(False))


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH])
@pytest.mark.parametrize('params',
                         [
                             {'dt': dt_11_month_ago, 'new_clients_only': 1, 'is_without_exception': False},
                             {'dt': dt_11_month_ago, 'new_clients_only': 0, 'is_without_exception': True},
                             {'dt': dt_11_month_ago, 'new_clients_only': None, 'is_without_exception': True},
                             {'dt': dt_13_month_ago, 'new_clients_only': 1, 'is_without_exception': True}]
                         )
@pytest.mark.parametrize('promocode_class', [PromocodeClass.FIXED_DISCOUNT])
def test_promo_code_is_new_clients_only_invoice(context, params, promocode_class):
    client_id = steps.ClientSteps.create()
    calc_params = fill_calc_params(calc_class_name=promocode_class, discount_pct=FIXED_DISCOUNT_VALUE,
                                   apply_on_create=False, adjust_quantity=True)
    create_and_reserve_promocode(calc_class_name=promocode_class,
                                 client_id=client_id,
                                 start_dt=params['dt'],
                                 end_dt=DT_1_DAY_AFTER,
                                 firm_id=context.firm.id,
                                 calc_params=calc_params,
                                 service_ids=[context.service.id],
                                 new_clients_only=params['new_clients_only'],
                                 valid_until_paid=1)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    act_id, request_id = create_act(client_id, person_id, context, params['dt'], qty=QTY)
    invoice_id = create_payed_invoice(request_id, person_id, paysys_id=context.paysys.id)
    discount_pct = FIXED_DISCOUNT_VALUE if params['is_without_exception'] else 0
    check_invoice_consumes_discount(discount_pct=discount_pct, invoice_id=invoice_id, precision=context.precision,
                                    qty_before=[QTY], adjust_quantity=True,
                                    apply_on_create=False, classname=promocode_class)


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH])
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
@pytest.mark.parametrize('promocode_class', [PromocodeClass.FIXED_DISCOUNT])
def test_promo_code_is_valid_until_paid_request(context, params, promocode_class):
    client_id = steps.ClientSteps.create()
    client_for_promo = client_id
    if params['first_promo_on_another_client']:
        another_client = steps.ClientSteps.create()
        client_for_promo = another_client
    calc_params = fill_calc_params(calc_class_name=promocode_class, discount_pct=FIXED_DISCOUNT_VALUE,
                                   apply_on_create=False, adjust_quantity=True)
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=promocode_class,
                                                                client_id=None,
                                                                start_dt=DT_1_DAY_BEFORE,
                                                                end_dt=DT_1_DAY_AFTER,
                                                                firm_id=context.firm.id,
                                                                calc_params=calc_params,
                                                                service_ids=[context.service.id],
                                                                valid_until_paid=params['valid_until_paid'])
    request_id, orders_list = create_request(context, client_for_promo, qty=QTY, promocode_code=promocode_code)
    person_id = steps.PersonSteps.create(client_for_promo, context.person_type.code)
    create_payed_invoice(request_id=request_id, person_id=person_id, paysys_id=context.paysys.id)
    delete_reservation_for_client(client_for_promo)
    # reserve(client_id, promocode_id)
    try:
        request_id, orders_list = create_request(context, client_id, qty=QTY, promocode_code=promocode_code)
        utils.check_that(params['is_request_with_promo'], hamcrest.equal_to(True))
        utils.check_that(is_request_with_promocode(promocode_id, request_id), hamcrest.equal_to(True))
    except Exception, exc:
        utils.check_that('Invalid promo code: ID_PC_MANY_ISSUED_INVOICES',
                         hamcrest.equal_to(steps.CommonSteps.get_exception_code(exc, 'contents')))
        utils.check_that(params['is_request_with_promo'], hamcrest.equal_to(False))


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH])
@pytest.mark.parametrize('params', [{'need_unique_urls': 1, 'deny_promocode': 1, 'is_invoice_with_promo': False}
    , {'need_unique_urls': 1, 'deny_promocode': 0, 'is_invoice_with_promo': True}
    , {'need_unique_urls': 0, 'deny_promocode': 1, 'is_invoice_with_promo': True}
    , {'need_unique_urls': 0, 'deny_promocode': 0, 'is_invoice_with_promo': True}
    , {'need_unique_urls': 0, 'deny_promocode': 1, 'is_invoice_with_promo': True}
    , {'need_unique_urls': 0, 'deny_promocode': 0, 'is_invoice_with_promo': True}])
@pytest.mark.parametrize('promocode_class', [PromocodeClass.FIXED_DISCOUNT])
def test_unique_urls_and_deny_promocode_invoice(params, context, promocode_class):
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(client_id, context.person_type.code,
                                         {'email': 'test-balance-notify@yandex-team.ru'})
    calc_params = fill_calc_params(calc_class_name=promocode_class, discount_pct=FIXED_DISCOUNT_VALUE,
                                   apply_on_create=False, adjust_quantity=True)
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=promocode_class,
                                                                client_id=client_id,
                                                                start_dt=DT_1_DAY_BEFORE,
                                                                end_dt=DT_1_DAY_AFTER,
                                                                firm_id=context.firm.id,
                                                                calc_params=calc_params,
                                                                service_ids=[context.service.id],
                                                                need_unique_urls=params['need_unique_urls'])
    request_id, orders_list = create_request(context=context, client_id=client_id, qty=QTY,
                                             deny_promocode=params['deny_promocode'])
    invoice_id = create_payed_invoice(request_id=request_id, person_id=person_id, paysys_id=context.paysys.id)
    utils.check_that(steps.PromocodeSteps.is_invoice_with_promo(promocode_id, invoice_id),
                     hamcrest.equal_to(params['is_invoice_with_promo']))
    discount_pct = FIXED_DISCOUNT_VALUE if params['is_invoice_with_promo'] else 0
    check_invoice_consumes_discount(discount_pct=discount_pct, invoice_id=invoice_id, precision=context.precision,
                                    qty_before=[QTY], adjust_quantity=True,
                                    apply_on_create=False, classname=promocode_class)
