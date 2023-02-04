import datetime

import hamcrest
import pytest

import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features
from temp.igogor.balance_objects import Contexts, Products, Firms, Currencies, Regions

pytestmark = [pytest.mark.priority('mid'),
              reporter.feature(Features.PROMOCODE, Features.REQUEST, Features.INVOICE)]

dt = datetime.datetime.now()
dt_1_day_before = dt - datetime.timedelta(days=1)
dt_1_day_after = dt + datetime.timedelta(days=1)

PROMOCODE_BONUS = 20
PROMOCODE_CURRENCY_BONUS = 2400
QTY = 10

DIRECT_FISH_RUB_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, product=Products.DIRECT_FISH,
                                                              region=Regions.RU, currency=Currencies.RUB)


def create_and_reserve_promocode(client_id, firm_id=1, is_global_unique=0, start_dt=None, end_dt=None,
                                 new_clients_only=0,
                                 valid_until_paid=1, currency=None, minimal_qty_currency=None, minimal_qty=None,
                                 services=None,
                                 code=None, middle_dt=None, bonus1=None, bonus2=None, event=None):
    if not start_dt:
        start_dt = dt_1_day_before
    if not end_dt:
        end_dt = dt_1_day_after
    if not bonus1:
        bonus1 = PROMOCODE_BONUS
    if not bonus2:
        bonus2 = PROMOCODE_BONUS
    if code:
        exist_promo = db.get_promocode_by_code(code)
        if exist_promo:
            id = exist_promo[0]['id']
            steps.PromocodeSteps.clean_up(id)
            steps.PromocodeSteps.delete_promocode(id)
    promo_code_id = steps.PromocodeSteps.create(start_dt=start_dt, end_dt=end_dt, bonus1=bonus1, bonus2=bonus2,
                                                minimal_qty=minimal_qty, reservation_days=None,
                                                firm_id=firm_id, is_global_unique=is_global_unique,
                                                new_clients_only=new_clients_only, valid_until_paid=valid_until_paid,
                                                code=code, middle_dt=middle_dt)
    if currency:
        steps.PromocodeSteps.set_multicurrency_bonuses(promo_code_id, [
            {'currency': currency, 'bonus1': PROMOCODE_CURRENCY_BONUS, 'bonus2': PROMOCODE_CURRENCY_BONUS,
             'minimal_qty': minimal_qty_currency}
        ])
    promo_code_code = db.get_promocode_by_id(promo_code_id)[0]['code']
    return promo_code_id, promo_code_code


def create_request(service_id, client_id, product_id, promocode_code=None, invoice_dt=None, agency_id=None, qty=None):
    if qty is None:
        qty = QTY
    service_order_id = steps.OrderSteps.next_id(service_id=service_id)
    steps.OrderSteps.create(client_id=client_id, product_id=product_id, service_id=service_id,
                            service_order_id=service_order_id, params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': dt}]
    request_id = steps.RequestSteps.create(client_id=agency_id if agency_id else client_id, orders_list=orders_list,
                                           additional_params={'InvoiceDesireDT': invoice_dt})
    return request_id, orders_list


@pytest.mark.smoke
@pytest.mark.parametrize('context, params', [
    (DIRECT_FISH_RUB_CONTEXT, {'minimal_qty_currency': None, 'is_with_discount': True}),

])
def test_promo_code_minimal_qty_currency(context, params):
    client_id = steps.ClientSteps.create()
    steps.PersonSteps.create(client_id, 'ur')
    promocode_id, promocode_code = create_and_reserve_promocode(client_id=client_id,
                                                                firm_id=context.firm.id,
                                                                currency=context.currency.iso_code,
                                                                minimal_qty=10,
                                                                minimal_qty_currency=params['minimal_qty_currency'],
                                                                bonus1=10, bonus2=10)

    request_id, _ = create_request(context.service.id, client_id, context.product.id)
    print steps.PromocodeSteps.generate_code()
