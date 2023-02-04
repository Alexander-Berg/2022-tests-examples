__author__ = 'aikawa'

import calendar
import datetime

import pytest
from hamcrest import has_length

import balance.balance_db as db
import balance.balance_steps as steps
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance.features import Features
from btestlib.matchers import contains_dicts_with_entries
import btestlib.config as balance_config

pytestmark = [
    pytest.mark.priority('mid'),
    reporter.feature(Features.ACT)
]

current_dt = datetime.datetime.now()
_, last_day = calendar.monthrange(datetime.date.today().year, datetime.date.today().month)
# the last day of month is special dt usually
previous_month_not_the_last_day = (current_dt.replace(day=1) - datetime.timedelta(days=4))
current_month_the_last_day = current_dt.replace(day=last_day, hour=0, minute=0, second=0, microsecond=0)
current_dt_midnight = datetime.datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
previous_month_the_last_day = (current_dt.replace(day=1) - datetime.timedelta(days=1)).replace(hour=0, minute=0,
                                                                                               second=0, microsecond=0)

SERVICE_ID = 7
PRODUCT_ID = 1475
PAYSYS_ID = 1003
PERSON_TYPE = 'ur'
QTY = 100


@pytest.fixture
def client_id():
    return steps.ClientSteps.create()


@pytest.fixture
def person_id(client_id):
    return steps.PersonSteps.create(client_id, PERSON_TYPE)


def create_act(client_id, person_id, shipment_dt, invoice_dt, act_dt, act_force, shipment_qty):
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID,
                            service_id=SERVICE_ID)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': current_dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=invoice_dt))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': shipment_qty}, 0, shipment_dt)

    steps.ActsSteps.generate(client_id, force=act_force, date=act_dt)

    return invoice_id


@pytest.mark.parametrize('shipment_dt', [
    previous_month_not_the_last_day
    , current_dt
]
    , ids=[
        'sh:previous_month_not_the_last_day'
        , 'sh:current_dt'
    ])
@pytest.mark.parametrize('invoice_dt', [
    previous_month_not_the_last_day,
     current_dt
]
    , ids=[
        'inv:previous_month_not_the_last_day',
        'inv:current_dt',
    ])
@pytest.mark.parametrize('act_dt', [
    previous_month_not_the_last_day
    , current_dt
]
    , ids=[
        'act:previous_month_not_the_last_day'
        , 'act:current_dt'
    ])
@pytest.mark.parametrize('act_force', [
    1,
    0
]
    , ids=[
        'act_type:monthly',
        'act_type:daily'
    ])
@pytest.mark.parametrize('shipment_qty', [
    QTY
    , QTY / 2
]
    , ids=[
        'equal_cons_qty',
        'half_of_cons_qty'
    ])
def test_act_dt_on_prepaid_invoice_depend_on_shipment_dt(client_id, person_id, shipment_dt, invoice_dt, act_dt,
                                                         act_force, shipment_qty):
    invoice_id = create_act(client_id, person_id, shipment_dt, invoice_dt, act_dt, act_force, shipment_qty)
    if act_force:
        if act_dt == previous_month_not_the_last_day:
            if (shipment_dt == previous_month_not_the_last_day) and \
                    (invoice_dt == previous_month_not_the_last_day or balance_config.ENABLE_SINGLE_ACCOUNT):
                expected_act_dt = [{'dt': previous_month_the_last_day}]
            else:
                expected_act_dt = []
        else:
            expected_act_dt = [{'dt': current_month_the_last_day}]
    else:
        if balance_config.ENABLE_SINGLE_ACCOUNT:
            expected_act_dt = []
        else:
            if shipment_qty == QTY / 2:
                expected_act_dt = []
            else:
                expected_act_dt = [{'dt': current_dt_midnight}]

    if balance_config.ENABLE_SINGLE_ACCOUNT:
        invoice_id = db.get_invoice_by_charge_note_id(invoice_id)[0]['id']
    acts = db.get_acts_by_invoice(invoice_id)
    utils.check_that(acts, has_length(len(expected_act_dt)))
    utils.check_that(acts, contains_dicts_with_entries(expected_act_dt))


if __name__ == "__main__":
    pytest.main("test_act_at_month_before_invoice.py -v")
