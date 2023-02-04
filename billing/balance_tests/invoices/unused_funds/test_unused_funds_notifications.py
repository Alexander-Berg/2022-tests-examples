# -*- coding: utf-8 -*-
from functools import partial

import pytest
from mock import mock

from balance.actions.invoice_refunds import InvoiceRefundManager
from balance.constants import (
    TransferMode,
    ServiceId,
    NotifyOpcode, DIRECT_PRODUCT_ID,
)
from balance.mapper import ClientUnusedFunds
from tests.balance_tests.invoice_refunds.common import create_bank_cpf
from tests.balance_tests.invoices.invoice_common import create_refund
from tests.balance_tests.invoices.unused_funds.common import (
    create_invoice,
    create_order,
    get_unused_funds_cache,
)

DIRECT_RUB_ORDER_FUNC = (partial(create_order, service_id=ServiceId.DIRECT), 20)
DIRECT_FISH_ORDER_FUNC = (partial(create_order, product_id=DIRECT_PRODUCT_ID, service_id=ServiceId.DIRECT), 20)
MKB_ORDER_FUNC = (partial(create_order, service_id=ServiceId.MKB), 20)

DIRECT_ORDER_NOTIFICATION = pytest.mark.parametrize(
    ["notify_services", "invoice_with_unused_funds"],
    [
        (
            [ServiceId.DIRECT],
            [DIRECT_RUB_ORDER_FUNC],
        ),
        (
            [ServiceId.DIRECT],
            [DIRECT_FISH_ORDER_FUNC],
        ),
    ],
    indirect=True,
)


def create_cache_records(
    session,
    invoice,
    data,  # type: list[dict[str, Any]]
):  # type: (...) -> None
    session.add_all(
        [
            ClientUnusedFunds(
                client_id=invoice.client.id,
                invoice_id=invoice.id,
                currency=record["currency"],
                unused_funds=record["unused_funds"],
            )
            for record in data
        ]
    )
    session.flush()


@pytest.fixture()
def unused_funds(request):
    funds_left = getattr(request, "param", 100)
    assert isinstance(funds_left, int)
    return funds_left


@pytest.fixture()
def notify_services(session, request):
    assert isinstance(request.param, list), "Expected param to be list"
    session.config.__dict__["NOTIFY_UNUSED_FUNDS_SERVICES"] = request.param
    return request.param


@pytest.fixture()
def pre_test_invoice_hook(request):
    def identity(*args, **kwargs):
        pass

    prehook = getattr(request, "param", identity)
    return prehook if prehook else identity


@pytest.fixture()
def invoice_with_unused_funds(
    session,
    request,
    client,
    unused_funds,
    pre_test_invoice_hook,
):  # type: (...) -> Invoice
    order_transfer_data = {
        order_func(client): spent for (order_func, spent) in request.param
    }  # type: dict[Order, int]

    invoice = create_invoice(
        session,
        client,
        order_transfer_data.keys(),
        quantity=666,
        overdraft=0,
    )

    spent = sum(order_transfer_data.values())
    # Disable notification function so that no objects are created in fixture
    with mock.patch(
        "balance.mapper.invoices.Invoice.notify_unused_funds",
        return_value=True,
    ):
        invoice.create_receipt(unused_funds + spent)
        for order, transferred_amount in order_transfer_data.items():
            invoice.transfer(
                order, TransferMode.src, transferred_amount, skip_check=True
            )
        assert invoice.unused_funds == unused_funds

    pre_test_invoice_hook(session, invoice)
    return invoice


@pytest.fixture()
def pure_direct_invoice(
    session,
    client,
    unused_funds,
):
    invoice = create_invoice(
        session,
        client,
        [create_order(client, service_id=ServiceId.DIRECT)],
        quantity=666,
        overdraft=0,
    )
    return invoice


def assert_direct_notified_if_able(
    session, invoice, services_to_notify, expects_cache_item=True
):
    cache = get_unused_funds_cache(session, invoice.id)
    if expects_cache_item:
        assert len(cache) == 1
        cache_obj = cache[0]
        validate_cache_obj(cache_obj, invoice)
    else:
        assert len(cache) == 0

    assert set(get_notification_object_ids(session)) == {
        ClientUnusedFunds.calculate_notification_id(
            client_id=invoice.client.id, invoice_id=invoice.id, service_id=service_id
        )
        for service_id in set(services_to_notify).intersection({ServiceId.DIRECT})
    }


def get_notification_object_ids(session):  # type: (...) -> list[int]
    return [
        row[0]
        for row in session.execute(
            """select object_id from t_object_notification where opcode = :opcode""",
            {"opcode": NotifyOpcode.INVOICE_UNUSED_FUNDS},
        ).fetchall()
    ]


def clear_notifications(session):  # type: (...) -> None
    session.execute(
        """delete from t_object_notification where opcode = :opcode""",
        {"opcode": NotifyOpcode.INVOICE_UNUSED_FUNDS},
    )


def validate_cache_obj(
    cache_obj,  # type: ClientUnusedFunds
    invoice,  # Invoice
):  # type: (...) -> None
    assert cache_obj.invoice == invoice
    assert cache_obj.client == invoice.client
    assert cache_obj.unused_funds == invoice.unused_funds_invoice_currency
    assert cache_obj.currency == invoice.currency


@pytest.mark.parametrize(
    ["notify_services", "invoice_with_unused_funds"],
    [
        pytest.param(
            [ServiceId.DIRECT],
            [DIRECT_RUB_ORDER_FUNC],
            id="single_service",
        ),
        pytest.param(
            [ServiceId.MKB, ServiceId.DIRECT],
            [MKB_ORDER_FUNC, DIRECT_RUB_ORDER_FUNC],
            id="multiple_services",
        ),
        pytest.param(
            [ServiceId.DIRECT],
            [MKB_ORDER_FUNC, DIRECT_RUB_ORDER_FUNC],
            id="some_services",
        ),
    ],
    indirect=["notify_services", "invoice_with_unused_funds"],
)
@pytest.mark.parametrize(
    "unused_funds",
    [100, 0],
    indirect=True,
)
@pytest.mark.parametrize(
    "pre_test_invoice_hook",
    [
        pytest.param(
            partial(create_cache_records, data=[]),
            id="empty_cache",
        ),
        pytest.param(
            partial(
                create_cache_records,
                data=[
                    {
                        "currency": "RUR",
                        "unused_funds": 666,
                    },
                ],
            ),
            id="populated_cache",
        ),
    ],
    indirect=True,
)
def test_notify_services(
    session,
    invoice_with_unused_funds,
    client,
    notify_services,
    unused_funds,
):
    invoice_with_unused_funds.notify_unused_funds()

    cache = get_unused_funds_cache(session, invoice_with_unused_funds.id)

    if unused_funds == 0:
        assert (
            len(cache) == 0
        ), "Did not expect to fill cache when unused_funds are absent"
    else:
        assert len(cache) == 1
        cache_obj = cache[0]
        validate_cache_obj(cache_obj, invoice_with_unused_funds)

        assert (
            ClientUnusedFunds.from_notification_id(
                session,
                cache_obj.notification_id(notify_services[0]),
            )
            == cache_obj
        )

    assert set(get_notification_object_ids(session)) == {
        ClientUnusedFunds.calculate_notification_id(
            client_id=client.id,
            invoice_id=invoice_with_unused_funds.id,
            service_id=service_id,
        )
        for service_id in notify_services
    }


@pytest.mark.parametrize(
    ["notify_services", "invoice_with_unused_funds"],
    [
        (
            [ServiceId.MARKET],
            [DIRECT_RUB_ORDER_FUNC],
        ),
    ],
    indirect=True,
)
def test_ignore_absent_services(
    session, client, invoice_with_unused_funds, notify_services
):
    invoice_with_unused_funds.notify_unused_funds()

    # Do not send any notifications
    assert len(get_notification_object_ids(session)) == 0

    # But still populate cache
    cache = get_unused_funds_cache(session, invoice_with_unused_funds.id)
    assert len(cache) == 1
    validate_cache_obj(cache[0], invoice_with_unused_funds)


@DIRECT_ORDER_NOTIFICATION
@pytest.mark.parametrize(
    "spare_funds",
    [0, 25],
)
def test_notify_on_existent_transfer(
    session,
    invoice_with_unused_funds,
    notify_services,
    unused_funds,
    spare_funds,
):
    order = invoice_with_unused_funds.invoice_orders[0].order
    invoice_with_unused_funds.transfer(
        order,
        TransferMode.src,
        unused_funds - spare_funds,
    )

    assert invoice_with_unused_funds.unused_funds == spare_funds
    assert_direct_notified_if_able(
        session,
        invoice_with_unused_funds,
        notify_services,
        expects_cache_item=spare_funds != 0,
    )


@pytest.mark.parametrize(
    "notify_services",
    [
        [ServiceId.DIRECT],
        [],
    ],
    indirect=True,
)
@pytest.mark.parametrize(
    "spare_funds",
    [0, 25],
)
def test_notify_on_new_transfer(
    session,
    pure_direct_invoice,
    unused_funds,
    spare_funds,
    notify_services,
):
    with mock.patch(
        "balance.mapper.invoices.Invoice.notify_unused_funds",
        return_value=True,
    ):
        pure_direct_invoice.create_receipt(unused_funds)
    order = pure_direct_invoice.invoice_orders[0].order
    pure_direct_invoice.transfer(
        order,
        TransferMode.src,
        unused_funds - spare_funds,
    )
    assert_direct_notified_if_able(
        session,
        pure_direct_invoice,
        notify_services,
        expects_cache_item=spare_funds != 0
    )


@pytest.mark.parametrize(
    "notify_services",
    [
        [ServiceId.DIRECT],
        [],
    ],
    indirect=True,
)
def test_notify_on_receipt(
    session,
    pure_direct_invoice,
    notify_services,
    unused_funds,
):
    pure_direct_invoice.create_receipt(unused_funds)

    assert_direct_notified_if_able(session, pure_direct_invoice, notify_services)


@pytest.mark.parametrize(
    "notify_services",
    [
        [ServiceId.DIRECT],
        [],
    ],
    indirect=True,
)
@pytest.mark.parametrize(
    "invoice_with_unused_funds",
    [
        [DIRECT_RUB_ORDER_FUNC, ],
        [DIRECT_FISH_ORDER_FUNC, ],
    ],
    indirect=True,
)
def test_update_cache_with_locked_funds(
    session,
    invoice_with_unused_funds,
    unused_funds,
    notify_services,
):
    additional_funds = 50
    invoice_with_unused_funds.create_receipt(additional_funds)

    cache = get_unused_funds_cache(session, invoice_with_unused_funds.id)
    assert len(cache) > 0
    validate_cache_obj(cache[0], invoice_with_unused_funds)
    clear_notifications(session)

    invoice_with_unused_funds.unused_funds_lock = True
    invoice_with_unused_funds.create_receipt(invoice_with_unused_funds.unused_funds)

    cache = get_unused_funds_cache(session, invoice_with_unused_funds.id)
    assert len(cache) == 0
    assert len(get_notification_object_ids(session)) == len(notify_services)
    assert invoice_with_unused_funds.unused_funds > 0


@pytest.mark.parametrize(
    "notify_services",
    [
        [ServiceId.DIRECT],
        [],
    ],
    indirect=True,
)
@pytest.mark.parametrize(
    "invoice_with_unused_funds",
    [
        [DIRECT_RUB_ORDER_FUNC, ],
    ],
    indirect=True,
)
@pytest.mark.parametrize(
    "spare_funds",
    [0, 25],
)
def test_update_cache_on_client_has_expired_invoice(
    session,
    invoice_with_unused_funds,
    unused_funds,
    notify_services,
    spare_funds,
):
    order = invoice_with_unused_funds.invoice_orders[0].order
    with mock.patch('balance.mapper.clients.Client.has_expired_invoice', return_value=True):
        invoice_with_unused_funds.transfer(
            order,
            TransferMode.src,
            unused_funds - spare_funds,
        )
    # Was able to transfer funds using Invoice.transfer
    assert invoice_with_unused_funds.unused_funds == spare_funds

    cache = get_unused_funds_cache(session, invoice_with_unused_funds.id)
    if spare_funds:
        assert len(cache) == 1
        validate_cache_obj(cache[0], invoice_with_unused_funds)
    else:
        assert len(cache) == 0

    assert len(get_notification_object_ids(session)) == len(notify_services)


def refund_manager_refund(invoice, refund_amount):
    cpf = create_bank_cpf(invoice, invoice.receipt_sum)
    refund_manager = InvoiceRefundManager(cpf, None)
    return refund_manager.create(refund_amount)


def object_builder_refund(invoice, refund_amount):
    return create_refund(invoice, refund_amount)


@DIRECT_ORDER_NOTIFICATION
@pytest.mark.parametrize(
    'refund_amount',
    [30, 70]
)
@pytest.mark.parametrize(
    'refund_func',
    [
        refund_manager_refund,
        object_builder_refund,
    ]
)
def test_update_cache_on_refund(
    session,
    invoice_with_unused_funds,
    notify_services,
    unused_funds,
    refund_amount,
    refund_func,
):
    invoice = invoice_with_unused_funds
    invoice.notify_unused_funds()
    assert get_unused_funds_cache(session, invoice.id)[0].unused_funds == unused_funds

    assert 0 < refund_amount < unused_funds
    leftover_amount = unused_funds - refund_amount

    refund_func(invoice, refund_amount)

    assert invoice.unused_funds == leftover_amount
    assert get_unused_funds_cache(session, invoice.id)[0].unused_funds == leftover_amount
