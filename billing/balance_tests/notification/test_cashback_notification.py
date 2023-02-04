# -*- coding: utf-8 -*-


import pytest
import datetime
import mock
from decimal import Decimal

from cluster_tools.notify_cashback import get_query, put_notifications_in_queue
from balance import constants as cst, mapper
from tests import object_builder as ob

NOW = datetime.datetime.now()
HALF_DAY_AGO = NOW - datetime.timedelta(hours=12)
TWO_DAYS_AGO = NOW - datetime.timedelta(days=2)
YESTERDAY = NOW - datetime.timedelta(days=1)


@pytest.fixture(autouse=True)
def notification_config(session):
    session.config.__dict__['ALLOWED_CASHBACK_NOTIFICATIONS'] = [cst.ServiceId.DIRECT]


@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder.construct(session)


@pytest.fixture(name='cashback')
def create_cashback(session, client, **kw):
    return ob.ClientCashbackBuilder.construct(
        session,
        client=client,
        **kw
    )


def do_completion(order, qty):
    order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: qty})
    order.session.flush()


@pytest.fixture(name='order')
def create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID):
    return ob.OrderBuilder.construct(session, client=client, product_id=product_id)


@pytest.fixture(name='invoice')
def create_invoice(session, client, person=None, request_=None, **kwargs):
    request_ = request_ or create_request(session, client,
                                          [(create_order(session, client), Decimal('100'))])
    return ob.InvoiceBuilder.construct(
        session,
        request=request_,
        person=person,
        **kwargs
    )


def create_request(session, client, orders):
    return ob.RequestBuilder.construct(
        session,
        basket=ob.BasketBuilder(
            client=client,
            rows=[
                ob.BasketItemBuilder(order=o, quantity=qty)
                for o, qty in orders
            ],
        ),
    )


@pytest.mark.parametrize('order_shipment_update_dt', [
    None,
    TWO_DAYS_AGO,
    HALF_DAY_AGO,
    NOW
])
@pytest.mark.parametrize('cashback_usage_dt', [
    TWO_DAYS_AGO,
    HALF_DAY_AGO,
    NOW
])
@pytest.mark.parametrize('last_notification_dt', [
    None,
    YESTERDAY
])
def test_get_query(session, cashback, client, order_shipment_update_dt, cashback_usage_dt,
                   last_notification_dt):
    # нотифицируем кешбек, если было изменение откруток после даты
    # последней нотификации и после последнего взятия кешбека
    # в первый раз пропускаем условие про дату последней нотификации
    order = create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID)
    request = create_request(session, client, orders=[(order, 100)])
    invoice = create_invoice(session, client, request_=request)
    invoice.turn_on_rows()
    order.shipment_update_dt = order_shipment_update_dt
    order.consumes[0].cashback_usage.dt = cashback_usage_dt
    session.flush()
    query = get_query(session, last_notification_dt)
    cashbacks = query.all()
    if (order_shipment_update_dt and (order_shipment_update_dt >= YESTERDAY or not last_notification_dt)
        and cashback_usage_dt < order_shipment_update_dt):

        assert len(cashbacks) == 1
        assert cashbacks[0][0] == cashback.id
        assert cashbacks[0][1] == order.service_id
    else:
        assert len(cashbacks) == 0


def return_true(*args, **kwargs):
    return True


def set_complete(*args, **kwargs):
    return


def create_cashback_usage(session, client, cashback_usage_dt=YESTERDAY,
                          order_shipment_update_dt=HALF_DAY_AGO):
    order = create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID)
    request = create_request(session, client, orders=[(order, 100)])
    invoice = create_invoice(session, client, request_=request)
    invoice.turn_on_rows()
    order.consumes[0].cashback_usage.dt = cashback_usage_dt
    order.shipment_update_dt = order_shipment_update_dt


@mock.patch('balance.mapper.jobs.Job.need_run', return_true)
@mock.patch('balance.mapper.jobs.Job.set_complete', set_complete)
def test_put_notifications_in_queue(session):
    session.config.__dict__['LAST_CASHBACK_NOTIFICATION_DT'] = YESTERDAY
    # prev = session.query(mapper.Config).get('LAST_CASHBACK_NOTIFICATION_DT')
    for _ in range(2):
        client = create_client(session)
        create_cashback(session, client)
        create_cashback_usage(session, client)

    session.flush()
    put_notifications_in_queue(session, batch_size=1, delay=1)
    assert session.query(mapper.Config).get('LAST_CASHBACK_NOTIFICATION_DT').value_dt > YESTERDAY


@mock.patch('balance.mapper.jobs.Job.need_run', return_true)
@mock.patch('balance.mapper.jobs.Job.set_complete', set_complete)
def test_put_notifications_in_queue_call_count(session):
    for _ in range(4):
        client = create_client(session)
        create_cashback(session, client)
        create_cashback_usage(session, client)
    session.flush()
    with mock.patch('cluster_tools.notify_cashback.set_last_notification_dt') as patch_set_dt:
        put_notifications_in_queue(session, batch_size=2, delay=1)
    # два раза на пачку + последний
    assert patch_set_dt.call_count == 3
