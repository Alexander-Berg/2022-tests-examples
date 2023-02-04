import datetime
from decimal import Decimal as D

import hamcrest
import pytest

import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features
from btestlib.constants import PromocodeClass

pytestmark = [pytest.mark.priority('mid'),
              reporter.feature(Features.PROMOCODE, Features.REQUEST, Features.INVOICE)]

dt = datetime.datetime.now()
order_dt = dt

today = datetime.datetime.now()
day_ago = today - datetime.timedelta(days=1)
two_days_ago = day_ago - datetime.timedelta(days=1)
day_after = today + datetime.timedelta(days=1)
two_days_after = day_after + datetime.timedelta(days=1)


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


def create_promo_code(start_dt, end_dt):
    promo_resp, = steps.PromocodeSteps.create_new(
        calc_class_name=PromocodeClass.LEGACY_PROMO,
        calc_params=steps.PromocodeSteps.fill_calc_params(
            promocode_type=PromocodeClass.LEGACY_PROMO,
            middle_dt=start_dt + datetime.timedelta(seconds=1),
            bonus1=20,
            bonus2=20,
            minimal_qty=100,
            discount_pct=0
        ),
        promocodes=[steps.PromocodeSteps.generate_code()],
        start_dt=start_dt,
        end_dt=end_dt,
    )
    promocode_id = promo_resp['id']
    promocode_code = promo_resp['code']
    return promocode_id, promocode_code


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('promocode_start_dt, promocode_end_dt',
                         generate_dates_for_test_validation_rules_while_create_request()
                         )
def test_validation_rules_while_create_request(promocode_start_dt, promocode_end_dt):
    PERSON_TYPE = 'ur'
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    client_id = steps.ClientSteps.create()

    promocode_id, promocode_code = create_promo_code(promocode_start_dt, promocode_end_dt)

    steps.PersonSteps.create(client_id, PERSON_TYPE)
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                            service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 500, 'BeginDT': dt}
    ]
    try:
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                               additional_params=dict(PromoCode=promocode_code))
        utils.check_that(steps.PromocodeSteps.is_request_with_promo(promocode_id, request_id), hamcrest.equal_to(True))
    except Exception, exc:
        reporter.log(exc)
        utils.check_that(
            'Invalid promo code: ID_PC_INVALID_PERIOD' == steps.CommonSteps.get_exception_code(exc, 'contents'),
            hamcrest.equal_to(True))
        utils.check_that(datetime.datetime.now() not in (promocode_start_dt, promocode_end_dt), hamcrest.equal_to(True))


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('promocode_start_dt, promocode_end_dt, invoice_dt, expected',
                         [(day_ago, day_after, today, True),
                          (day_ago, day_after, two_days_ago, False),
                          (day_ago, day_after, two_days_after, False)]
                         )
def test_check_validation_rules_while_create_invoice(promocode_start_dt, promocode_end_dt, invoice_dt, expected):
    PERSON_TYPE = 'ur'
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    PAYSYS_ID = 1003
    client_id = steps.ClientSteps.create()

    promocode_id, _ = create_promo_code(promocode_start_dt, promocode_end_dt)

    steps.PromocodeSteps.make_reservation(client_id, promocode_id, begin_dt=promocode_start_dt)
    steps.PersonSteps.create(client_id, PERSON_TYPE)
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                            service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 500, 'BeginDT': order_dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params={'InvoiceDesireDT': invoice_dt})
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id)
    utils.check_that(steps.PromocodeSteps.is_invoice_with_promo(promocode_id, invoice_id), hamcrest.equal_to(expected))


if __name__ == "__main__":
    pytest.main("test_validation_rules.py -v")
