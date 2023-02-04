import datetime

import hamcrest
import pytest

import balance.balance_db as db
import btestlib.utils as utils
from balance import balance_steps as steps

dt = datetime.datetime.now()
order_dt = dt
today = datetime.datetime.now()
day_ago = today - datetime.timedelta(days=1)
day_after = today + datetime.timedelta(days=1)
promocode_start_dt = day_ago
promocode_end_dt = day_after
invoice_dt = today


def generate_dates_for_test_check_validation_rules_for_invoice_depending_on_multicurrency_bonuses():
    return [{'currency': 'RUB', 'multicurrency_bonuses': None},
    {'currency': 'RUB', 'multicurrency_bonuses': [{'currency': 'RUB', 'bonus1': 20, 'bonus2': 20, 'minimal_qty': 10}]},
    {'currency': 'RUB', 'multicurrency_bonuses': [{'currency': 'TRY', 'bonus1': 20, 'bonus2': 20, 'minimal_qty': 10}]}
            ]

@pytest.mark.parametrize('currency_params_and_bonuses',
                         generate_dates_for_test_check_validation_rules_for_invoice_depending_on_multicurrency_bonuses()
                         )
def test_check_validation_rules_for_invoice_depending_on_multicurrency_bonuses(currency_params_and_bonuses):
    currency = currency_params_and_bonuses['currency']
    bonus = currency_params_and_bonuses['multicurrency_bonuses']
    PERSON_TYPE = 'ur'
    SERVICE_ID = 7
    PRODUCT_ID = 503162
    PAYSYS_ID = 1003
    client_id = steps.ClientSteps.create_multicurrency(currency=currency)
    promocode_id = steps.PromocodeSteps.create(start_dt=promocode_start_dt, end_dt=promocode_end_dt, bonus1=20, bonus2=20, minimal_qty=100)
    if bonus:
        steps.PromocodeSteps.set_multicurrency_bonuses(promocode_id, bonus)
    promocode_code = db.get_promocode_by_id(promocode_id)[0]['code']
    steps.PromocodeSteps.make_reservation(client_id, promocode_id, begin_dt=promocode_start_dt)
    steps.PersonSteps.create(client_id, PERSON_TYPE)
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID, service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 500, 'BeginDT': order_dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(PromoCode=promocode_code, InvoiceDesireDT=invoice_dt))
    utils.check_that(steps.PromocodeSteps.is_request_with_promo(promocode_id, request_id), hamcrest.equal_to(True))
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    try:
        utils.check_that(steps.PromocodeSteps.is_invoice_with_promo(promocode_id, invoice_id), hamcrest.equal_to(True))
        utils.check_that(steps.PromocodeSteps.is_applied(promocode_id, invoice_id), hamcrest.equal_to(True))
    except AssertionError:
        utils.check_that(currency, hamcrest.contains(True))


if __name__ == "__main__":
    pytest.main("test_validation_rules_invoice_currency.py -v")
