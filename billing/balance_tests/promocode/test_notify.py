# -*- coding: utf-8 -*-
import datetime
from decimal import Decimal as D

import pytest
import hamcrest as hm

from balance import (
    constants as cst,
    mapper,
    exc,
    muzzle_util as ut,
)
from balance.actions.consumption import reverse_consume
from balance.actions.process_completions import ProcessCompletions
from balance.actions.process_completions.log_tariff import ProcessLogTariff
from balance.actions.process_completions.shipment import FAIR_PROC_SQL
from balance.actions import promocodes as pca

from notifier import data_objects as notifier_objects
import tests.object_builder as ob
from tests.balance_tests.promocode.common import (
    create_order,
    create_invoice,
    create_promocode,
    create_multicurrency_client
)
from tests.balance_tests.process_completions.common import create_order_task

pytestmark = [
    pytest.mark.promo_code,
]

NOTIFICATION_SQL = """select * from bo.t_object_notification
                      where opcode = :opcode and object_id = :order_id"""
NOTIFICATION_DEL_SQL = """delete bo.t_object_notification
                          where opcode = :opcode and object_id = :order_id"""
QTY = D('10')


@pytest.fixture
def promocode(session):
    return create_promocode(session, {'valid_until_paid': True, 'calc_params': {u"discount_pct": 42}})


@pytest.fixture
def order(session):
    return create_order(session, product_id=cst.DIRECT_PRODUCT_ID)


@pytest.fixture
def currency_order(session):
    client = create_multicurrency_client(session)
    return create_order(session, client=client, product_id=cst.DIRECT_PRODUCT_RUB_ID)


@pytest.fixture
def currency_invoice(session, currency_order, promocode):
    return _create_invoice(session, currency_order, promocode)


@pytest.fixture
def invoice(session, order, promocode):
    return _create_invoice(session, order, promocode)


def _create_invoice(session, order, promocode):
    invoice = ob.InvoiceBuilder(
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                rows=[ob.BasketItemBuilder(order=order, quantity=QTY)])
        ),
        firm=ob.Getter(mapper.Firm, cst.FirmId.YANDEX_OOO),
    ).build(session).obj
    pca.reserve_promo_code(invoice.client, promocode)
    session.flush()
    return invoice


@pytest.fixture(autouse=True)
def mock_config(session):
    session.config.__dict__['NOTIFY_PROMOCODE_OFF'] = 0
    session.config.__dict__['NOTIFY_PROMOCODE_SERVICES'] = [cst.ServiceId.DIRECT]


def complete(order, qty):
    order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: qty})
    order.session.flush()


def get_notification_kw(order):
    return {'opcode': cst.NOTIFY_PROMOCODE_OPCODE, 'order_id': order.id}


def _delete_notification(session, order):
    notification_kw = get_notification_kw(order)
    # удаляем нотификацию, чтобы прошла следующая проверка
    session.execute(NOTIFICATION_DEL_SQL, notification_kw)


def _assert_notification(session, order):
    notification_kw = get_notification_kw(order)
    notification_sql_len = len(session.execute(NOTIFICATION_SQL, notification_kw).fetchall())
    assert notification_sql_len == 1, 'Required 1 notification for order with promocode'


def _assert_no_notification(session, order):
    notification_kw = get_notification_kw(order)
    notification_sql_len = len(session.execute(NOTIFICATION_SQL, notification_kw).fetchall())
    assert notification_sql_len == 0, 'Required 0 notification for order with promocode'


def _create_consume(session, invoice):
    invoice.turn_on_rows(apply_promocode=True)
    session.flush()


def _create_consume_without_notification(session, invoice, order):
    _create_consume(session, invoice)
    _delete_notification(session, order)


def test_create_consume(session, invoice, order):
    assert session.execute(NOTIFICATION_SQL, get_notification_kw(order)).first() is None
    _create_consume(session, invoice)
    _assert_notification(session, order)


class LogTariffPrCompl(object):

    def __init__(self, order):
        self.order = order

    def do(self, total_completion_qty, prev_completion_qty):
        delta_qty = total_completion_qty - prev_completion_qty
        task = create_order_task(self.order, delta_qty)
        process_log_tariff = ProcessLogTariff(self.order, task.task_id, obj_lock=True, shared_lock=True)
        return process_log_tariff.do()


class ClassicPrCompl(object):

    def __init__(self, order):
        self.order = order

    def do(self, total_completion_qty, *args, **kwargs):
        process_completions = ProcessCompletions(self.order)
        return process_completions.process_completions(qty=total_completion_qty)


@pytest.mark.parametrize('process_completion_class', [
    ClassicPrCompl,
    LogTariffPrCompl
])
def test_process_completions_python(session, currency_order, currency_invoice, process_completion_class):
    # Полная открутка через python => создается нотификация --------------------------------------------------------
    _create_consume_without_notification(session, currency_invoice, currency_order)
    consume = currency_order.consumes[0]
    qty = consume.current_qty

    process_completion = process_completion_class(currency_order)
    process_completion.do(qty, consume.completion_qty)

    _assert_notification(session, currency_order)
    assert consume.completion_qty == consume.current_qty


@pytest.mark.parametrize('process_completion_class', [
    ClassicPrCompl,
    LogTariffPrCompl
])
def test_process_completions_python_reverse(session, currency_order, currency_invoice, process_completion_class):
    # Reverse полностью открученного consume через python => создается нотификация ----------------------------------
    _create_consume_without_notification(session, currency_invoice, currency_order)
    consume = currency_order.consumes[0]
    qty = consume.current_qty
    new_qty = currency_order.round_qty(qty / 2)

    consume.completion_qty = qty
    consume.completion_sum = consume.current_sum
    session.flush()

    process_completion = process_completion_class(currency_order)
    process_completion.do(new_qty, consume.completion_qty)

    _assert_notification(session, currency_order)
    assert consume.completion_qty == new_qty


def test_process_completion_python_without_notify(session, order, invoice):
    # Неполная открутка через python => нотификация не создается ---------------------------------------------------
    _create_consume_without_notification(session, invoice, order)
    consume = order.consumes[0]
    qty = order.round_qty(consume.current_qty / 2)

    pc = ProcessCompletions(order)
    pc.process_completions(qty=qty)

    _assert_no_notification(session, order)
    assert order.completion_qty == qty


def test_process_completions_sql(session, order, invoice):
    # Неполная открутка через sql => нотификация не создается ------------------------------------------------------
    _create_consume_without_notification(session, invoice, order)
    consume = order.consumes[0]
    co_qty = order.round_qty(consume.current_qty / 2)
    order.active_consume_id = consume.id
    order.shipment.update(
        datetime.datetime.now(),
        {order.shipment_type: co_qty}
    )
    order.session.flush()
    assert order.need_processing == False

    order.session.execute(FAIR_PROC_SQL, {
        'service_id': order.service_id,
        'service_order_id': order.service_order_id,
        'skip_deny_shipment': 0
    })
    order.session.expire_all()

    _assert_no_notification(session, order)
    assert order.need_processing == 0
    assert order.completion_qty == co_qty


def test_process_completions_sql_raise(session, order, invoice):
    # Полная открутка через sql => нотификация не создается, need_processing устанавливается в True ----------------
    _create_consume_without_notification(session, invoice, order)
    consume = order.consumes[0]
    co_qty = consume.current_qty

    order.active_consume_id = consume.id
    order.shipment.update(
        datetime.datetime.now(),
        {order.shipment_type: co_qty}
    )
    order.session.flush()

    order.session.execute(FAIR_PROC_SQL, {
        'service_id': order.service_id,
        'service_order_id': order.service_order_id,
        'skip_deny_shipment': 0
    })
    order.session.expire_all()

    _assert_no_notification(session, order)
    assert order.need_processing == 1


def test_process_completion_raises_python(session, order, invoice):
    # Полная открутка через sql => обработка передается в python и создается нотификация ---------------------------
    _create_consume_without_notification(session, invoice, order)
    consume = order.consumes[0]
    co_qty = consume.current_qty

    pc = ProcessCompletions(order)
    pc.calculate_consumption_fair({order.shipment_type: co_qty})

    _assert_notification(session, order)
    assert consume.completion_qty == consume.current_qty


def test_reverse_consume(session, order, invoice):
    _create_consume_without_notification(session, invoice, order)
    reverse_consume(order.consumes[0], None, 1)
    _assert_notification(session, order)


def test_order_unused_promocode_qty(session, order, invoice, promocode):
    # Проверка расчетов ----------------------------------------------------------------------
    _create_consume(session, invoice)
    assert len(order.consumes) == 1

    consume = order.consumes[0]
    qty = order.round_qty(consume.current_qty / 2)  # откручиваем половину средств на заказе

    complete(order, qty)
    session.flush()

    current_promocode_qty = order.round_qty(
        consume.current_qty
        - ut.add_percent(consume.current_qty, -consume.promo_code_discount_pct)
    )
    unused_promocode_qty = order.round_qty(current_promocode_qty / 2)

    assert ut.round(order.unused_promocode_qty, precision=2) == ut.round(unused_promocode_qty, precision=2)
    assert ut.round(order.available_promocode_qty, precision=2) == ut.round(unused_promocode_qty, precision=2)


def test_order_promocode_notify(session, order, invoice, promocode):
    # Тест параметров отправляемой нотификации ---------------------------------------------------------------------
    _create_consume(session, invoice)

    pc = ProcessCompletions(order)
    pc.process_completions(qty=QTY / 2)
    session.flush()

    notify_info = notifier_objects.BaseInfo.get_notification_info(
        session,
        cst.NOTIFY_PROMOCODE_OPCODE,
        order.id
    )[1]
    notify_args = notify_info['args'][0]
    assert notify_args['ServiceID'] == order.service_id
    assert notify_args['ServiceOrderID'] == order.service_order_id

    promocodes_ids = [promocode.id]
    notify_promocodes_ids = sorted([item.get('PromocodeID', None) for item in notify_args['Promocodes']])
    assert promocodes_ids == notify_promocodes_ids

    notification_promos = notify_args['Promocodes']
    assert len(notification_promos) == 1
    assert notification_promos[0]['UnusedPromocodeQty'] == str(D(order.unused_promocode_qty))
    assert notification_promos[0]['AvailablePromocodeQty'] == str(D(order.available_promocode_qty))


def test_wrong_service(session, promocode):
    order = create_order(session, service_id=cst.ServiceId.MARKET)
    invoice = create_invoice(session, qty=QTY, orders=[order])
    pca.reserve_promo_code(invoice.client, promocode)
    session.flush()
    notification_kw = {'opcode': cst.NOTIFY_PROMOCODE_OPCODE, 'order_id': order.id}
    assert session.execute(NOTIFICATION_SQL, notification_kw).first() is None
    _create_consume(session, invoice)
    _assert_no_notification(session, order)


def test_all_funds_to_overact(session, promocode):
    """
    invoice1
    order1
    - w_pc 10/5 10/5 20/10
    order2
    - w_pc 10/5 5/2.5 0/0
    Overact: 5
    На переакт уходят все свободные средства, нотификация уходит пустой
    """
    order = create_order(session)
    invoice = create_invoice(session, qty=10, orders=[order])
    invoice.create_receipt(invoice.effective_sum)
    invoice.promo_code = promocode
    session.flush()

    invoice.transfer(order, discount_obj=mapper.DiscountObj(0, 50, invoice.promo_code))
    complete(order, order.consume_qty)
    invoice.generate_act(force=1, backdate=datetime.datetime.now())
    complete(order, 10)

    order_alt = create_order(session, order.client, mapper.DIRECT_PRODUCT_RUB_ID)
    order.transfer(order_alt)
    complete(order_alt, 5)
    session.flush()

    with pytest.raises(exc.CANT_TEAR_PC_OFF_NO_FREE_CONSUMES) as exc_info:
        pca.tear_promocode_off(session, invoice)
    assert exc_info.value.msg == "Can't tear promocode off: PC_TEAR_OFF_NO_FREE_CONSUMES"

    for o in [order, order_alt]:
        status, notify_info = notifier_objects.BaseInfo.get_notification_info(
            session,
            cst.NOTIFY_PROMOCODE_OPCODE,
            o.id
        )
        if o is order:
            promocodes_match = hm.empty()
        else:
            promocodes_match = hm.contains(
                hm.has_entries({
                    'PromocodeID': promocode.id,
                    'AvailablePromocodeQty': '0.0000',
                    'UnusedPromocodeQty': '2.5000',
                }),
            )
        hm.assert_that(
            notify_info.get('args', []),
            hm.contains(
                hm.has_entries({
                    'ServiceID': o.service_id,
                    'ServiceOrderID': o.service_order_id,
                    'Promocodes': promocodes_match,
                }),
            ),
        )
