import datetime
from decimal import Decimal as D

import hamcrest
import pytest

import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features
from btestlib.constants import Services, Products, PromocodeClass, Firms
from temp.igogor.balance_objects import Contexts

pytestmark = [pytest.mark.priority('mid'),
              reporter.feature(Features.PROMOCODE, Features.REQUEST, Features.INVOICE)]

DIRECT_YANDEX_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1)

dt = datetime.datetime.now()
day_ago = dt - datetime.timedelta(days=1)
two_days_ago = day_ago - datetime.timedelta(days=1)

PROMOCODE_BONUS = 20
QTY = 500


def create_and_reserve_promocode(client_id, firm_id=1, is_global_unique=0, start_dt=day_ago, middle_dt=None):
    if middle_dt is None:
        middle_dt = start_dt + datetime.timedelta(seconds=1)
    code = steps.PromocodeSteps.generate_code()
    calc_params = steps.PromocodeSteps.fill_calc_params(promocode_type=PromocodeClass.LEGACY_PROMO, middle_dt=middle_dt,
                                                        bonus1=PROMOCODE_BONUS, bonus2=PROMOCODE_BONUS, minimal_qty=0,
                                                        currency=None, multicurrency_bonus1=PROMOCODE_BONUS,
                                                        multicurrency_bonus2=PROMOCODE_BONUS,
                                                        multicurrency_minimal_qty=0,
                                                        discount_pct=0
                                                        )
    promo = steps.PromocodeSteps.create_new(start_dt=start_dt, end_dt=dt + datetime.timedelta(days=1),
                                            calc_params=calc_params,
                                            firm_id=firm_id, is_global_unique=is_global_unique,
                                            reservation_days=None, service_ids=None, promocodes=[code])[0]
    promocode_id, promocode_code = promo['id'], promo['code']
    promocode_code = db.get_promocode2_by_id(promocode_id)[0]['code']
    steps.PromocodeSteps.reserve_new(client_id, promocode_id)
    return promocode_id, promocode_code


def create_request(context, client_id, promocode_code=None):
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id)
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': dt}]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params={'PromoCode': promocode_code})
    return request_id, orders_list


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH])
@pytest.mark.parametrize('params', [
    {'reservation_is_active': True, 'reservation_is_on_another_client': True,
     'expected_error': 'Invalid promo code: ID_PC_RESERVED_ON_ANOTHER_CLIENT'},
    {'reservation_is_active': False, 'reservation_is_on_another_client': True, 'expected_error': None},
    {'reservation_is_active': False, 'reservation_is_on_another_client': False, 'expected_error': None},
    {'reservation_is_active': True, 'reservation_is_on_another_client': False, 'expected_error': None},
])
def test_promo_code_check_request(context, params):
    client_id = steps.ClientSteps.create()
    if params['reservation_is_on_another_client']:
        client_with_promo = steps.ClientSteps.create()
    else:
        client_with_promo = client_id
    promocode_id, promocode_code = create_and_reserve_promocode(client_id=client_with_promo)
    if not params['reservation_is_active']:
        steps.PromocodeSteps.make_reservation(client_with_promo, promocode_id, two_days_ago, day_ago)
    try:
        request_id, _ = create_request(context, client_id, promocode_code)
        utils.check_that(steps.PromocodeSteps.is_request_with_promocode_new(promocode_id, request_id), hamcrest.equal_to(True))
        utils.check_that(params['expected_error'], hamcrest.equal_to(None))
    except Exception, exc:
        utils.check_that(params['expected_error'],
                         hamcrest.equal_to(steps.CommonSteps.get_exception_code(exc, 'contents')))


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH])
@pytest.mark.parametrize('params', [
    {'reservation_is_active': True, 'reservation_is_on_another_client': True, 'is_invoice_with_promo': False},
    {'reservation_is_active': False, 'reservation_is_on_another_client': True, 'is_invoice_with_promo': False},
    {'reservation_is_active': False, 'reservation_is_on_another_client': False, 'is_invoice_with_promo': False},
    {'reservation_is_active': True, 'reservation_is_on_another_client': False, 'is_invoice_with_promo': True},
])
def test_promo_code_check_invoice(context, params):
    client_id = steps.ClientSteps.create()
    promocode_id, promocode_code = create_and_reserve_promocode(client_id=client_id)
    request_id, _ = create_request(context, client_id, promocode_code)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    if params['reservation_is_on_another_client']:
        client_with_promo = steps.ClientSteps.create()
    else:
        client_with_promo = client_id

    if not params['reservation_is_active']:
        steps.PromocodeSteps.make_reservation(client_with_promo, promocode_id, two_days_ago, day_ago)
    else:
        steps.PromocodeSteps.reserve_new(client_with_promo, promocode_id)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=context.paysys.id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    utils.check_that(steps.PromocodeSteps.is_invoice_with_promo(promocode_id, invoice_id),
                     hamcrest.equal_to(params['is_invoice_with_promo']))
    steps.PromocodeSteps.check_invoice_is_with_discount(invoice_id, bonus=PROMOCODE_BONUS,
                                                        is_with_discount=params['is_invoice_with_promo'], qty=QTY)
