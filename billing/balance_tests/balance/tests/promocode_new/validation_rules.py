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

dt = datetime.datetime.now()
order_dt = dt

PROMOCODE_BONUS = 20


def generate_dates_for_test_validation_rules_while_create_request():
    today = datetime.datetime.now()
    day_ago = today - datetime.timedelta(days=1)
    two_days_ago = day_ago - datetime.timedelta(days=1)
    day_after = today + datetime.timedelta(days=1)
    two_days_after = day_after + datetime.timedelta(days=1)

    return [
        (two_days_ago, day_ago),
        (day_ago, day_after),
        (day_after, two_days_after)
    ]


def create_promocode(firm_id=1, is_global_unique=0, start_dt=None, middle_dt=None, end_dt=None):
    if middle_dt is None:
        middle_dt = start_dt + datetime.timedelta(seconds=1)
    code = steps.PromocodeSteps.generate_code()
    calc_params = steps.PromocodeSteps.fill_calc_params(promocode_type=PromocodeClass.LEGACY_PROMO, middle_dt=middle_dt,
                                                        bonus1=PROMOCODE_BONUS, bonus2=PROMOCODE_BONUS, minimal_qty=100,
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
    return promocode_id, promocode_code


DIRECT_YANDEX_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1)


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH])
@pytest.mark.parametrize('promocode_start_dt, promocode_end_dt',
                         generate_dates_for_test_validation_rules_while_create_request()
                         )
def test_validation_rules_while_create_request(context, promocode_start_dt, promocode_end_dt):
    client_id = steps.ClientSteps.create()
    promocode_id, promocode_code = create_promocode(start_dt=promocode_start_dt, end_dt=promocode_end_dt)
    steps.PromocodeSteps.make_reservation(client_id, promocode_id, begin_dt=dt - datetime.timedelta(days=1))
    steps.PersonSteps.create(client_id, context.person_type.code)
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 500, 'BeginDT': dt}
    ]
    try:
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                               additional_params=dict(PromoCode=promocode_code))
        utils.check_that(steps.PromocodeSteps.is_request_with_promocode_new(promocode_id, request_id),
                         hamcrest.equal_to(True))
    except Exception, exc:
        reporter.log(exc)
        utils.check_that(
            'Invalid promo code: ID_PC_INVALID_PERIOD' == steps.CommonSteps.get_exception_code(exc, 'contents'),
            hamcrest.equal_to(True))
        utils.check_that(datetime.datetime.now() not in (promocode_start_dt, promocode_end_dt), hamcrest.equal_to(True))


def generate_dates_for_test_check_validation_rules_while_create_invoice():
    today = datetime.datetime.now()
    day_ago = today - datetime.timedelta(days=1)
    two_days_ago = day_ago - datetime.timedelta(days=1)
    day_after = today + datetime.timedelta(days=1)
    two_days_after = day_after + datetime.timedelta(days=1)

    return [
        (day_ago, day_after, today),
        (day_ago, day_after, two_days_ago),
        (day_ago, day_after, two_days_after)
    ]


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH])
@pytest.mark.parametrize('promocode_start_dt, promocode_end_dt, invoice_dt',
                         generate_dates_for_test_check_validation_rules_while_create_invoice()
                         )
def test_check_validation_rules_while_create_invoice(context, promocode_start_dt, promocode_end_dt, invoice_dt):
    client_id = steps.ClientSteps.create()
    promocode_id, promocode_code = create_promocode(start_dt=promocode_start_dt, end_dt=promocode_end_dt)
    steps.PromocodeSteps.make_reservation(client_id, promocode_id, begin_dt=promocode_start_dt)
    steps.PersonSteps.create(client_id, context.person_type.code)
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 500, 'BeginDT': order_dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(PromoCode=promocode_code, InvoiceDesireDT=invoice_dt))
    utils.check_that(steps.PromocodeSteps.is_request_with_promocode_new(promocode_id, request_id),
                     hamcrest.equal_to(True))
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=context.paysys.id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    try:
        steps.InvoiceSteps.pay(invoice_id)
        utils.check_that(steps.PromocodeSteps.is_invoice_with_promo(promocode_id, invoice_id), hamcrest.equal_to(True))
        steps.PromocodeSteps.check_invoice_is_with_discount(invoice_id, bonus=PROMOCODE_BONUS,
                                                            is_with_discount=True, qty=500)
    except Exception, exc:
        reporter.log(exc)
        contents = 'Invalid parameter for function: current date {0} not inpromo code {1}, period: [{2}, {3}]'.format(
            utils.Date.nullify_microseconds_of_date(invoice_dt)
            , promocode_code
            , utils.Date.nullify_microseconds_of_date(promocode_start_dt)
            , utils.Date.nullify_microseconds_of_date(promocode_end_dt)
        )
        utils.check_that(contents, hamcrest.equal_to(steps.CommonSteps.get_exception_code(exc, 'contents')))
        utils.check_that(steps.PromocodeSteps.is_invoice_with_promo(promocode_id, invoice_id), hamcrest.equal_to(False))


if __name__ == "__main__":
    pytest.main("test_validation_rules.py -v")
