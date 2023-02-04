from datetime import timedelta
from decimal import Decimal

import pytest

from maps_adv.geosmb.clients.bvm import BvmNotFound
from maps_adv.geosmb.doorman.client import NotFound
from maps_adv.geosmb.telegraphist.proto.errors_pb2 import Error
from maps_adv.geosmb.telegraphist.proto.v2.common_pb2 import (
    Cost,
    NotificationResult,
    NotificationTransportResult,
    Transport,
)
from maps_adv.geosmb.telegraphist.proto.v2.notifications_pb2 import (
    Notification,
    OrderCancelled,
    OrderChanged,
    OrderCreated,
    OrderReminder,
    Recipient,
)
from maps_adv.geosmb.telegraphist.server.lib.enums import (
    NotificationType,
    Transport as TransportEnum,
)
from maps_adv.geosmb.telegraphist.server.lib.notification_router import (
    NoAddress,
    UnsupportedTransport,
)
from maps_adv.geosmb.telegraphist.server.tests.helpers import make_order

pytestmark = [pytest.mark.asyncio]

url = "/api/v2/send-notification/"


def notification_proto(**updates) -> Notification:
    kwargs = dict(
        recipient=Recipient(
            client_id=100500,
            biz_id=15,
            phone=71234567890,
            email="example@yandex.ru",
            device_id="abc123",
            passport_uid=111111,
        ),
        transports=[Transport.SMS],
    )
    kwargs.update(updates)
    return Notification(**kwargs)


@pytest.mark.parametrize(
    ("notification_router_return", "expected_pb"),
    [
        (
            [{TransportEnum.SMS: {"phone": 322223}}],
            NotificationResult(
                results=[
                    NotificationTransportResult(transport=Transport.SMS, phone=322223)
                ]
            ),
        ),
        (
            [{TransportEnum.EMAIL: NoAddress()}],
            NotificationResult(
                results=[
                    NotificationTransportResult(
                        transport=Transport.EMAIL, error="No address to send to"
                    )
                ]
            ),
        ),
        (
            [
                {TransportEnum.SMS: {"phone": 322223}},
                {TransportEnum.EMAIL: NoAddress()},
            ],
            NotificationResult(
                results=[
                    NotificationTransportResult(transport=Transport.SMS, phone=322223),
                    NotificationTransportResult(
                        transport=Transport.EMAIL, error="No address to send to"
                    ),
                ]
            ),
        ),
    ],
)
async def test_return_data(
    api, notification_router, notification_router_return, expected_pb
):
    notification_router.send_client_notification.coro.return_value = (
        notification_router_return
    )

    got = await api.post(
        url,
        proto=notification_proto(
            order_created=OrderCreated(order=make_order(as_proto=True))
        ),
        decode_as=NotificationResult,
        expected_status=200,
    )

    assert got == expected_pb


@pytest.mark.parametrize(
    "pb_transport, expected_transport",
    [
        (Transport.SMS, TransportEnum.SMS),
        (Transport.EMAIL, TransportEnum.EMAIL),
        (Transport.PUSH, TransportEnum.PUSH),
    ],
)
@pytest.mark.parametrize(
    "pb_order_field, pb_order_cls, expected_notification_type",
    [
        ("order_created", OrderCreated, NotificationType.ORDER_CREATED),
        ("order_cancelled", OrderCancelled, NotificationType.ORDER_CANCELLED),
        ("order_reminder", OrderReminder, NotificationType.ORDER_REMINDER),
        ("order_changed", OrderChanged, NotificationType.ORDER_CHANGED),
    ],
)
@pytest.mark.parametrize(
    "pb_total_cost, call_total_cost",
    [
        (
            dict(total_cost=Cost(final_cost="100.50")),
            dict(total_cost=dict(final_cost=Decimal("100.50"))),
        ),
        (
            dict(total_cost=Cost(minimal_cost="100.50")),
            dict(total_cost=dict(minimal_cost=Decimal("100.50"))),
        ),
        (
            dict(total_cost=Cost(final_cost="0")),
            dict(total_cost=dict(final_cost=Decimal("0"))),
        ),
        (
            dict(total_cost=Cost(minimal_cost="0")),
            dict(total_cost=dict(minimal_cost=Decimal("0"))),
        ),
    ],
)
async def test_will_notify(
    notification_router,
    api,
    pb_transport,
    expected_transport,
    pb_order_field,
    pb_order_cls,
    expected_notification_type,
    pb_total_cost,
    call_total_cost,
):
    await api.post(
        url,
        proto=notification_proto(
            transports=[pb_transport],
            **{
                pb_order_field: pb_order_cls(
                    order=make_order(as_proto=True, **pb_total_cost),
                    details_link="http://order-link.ru",
                )
            },
        ),
    )

    notification_router.send_client_notification.assert_called_with(
        recipient=dict(
            client_id=100500,
            biz_id=15,
            phone=71234567890,
            email="example@yandex.ru",
            device_id="abc123",
            passport_uid=111111,
        ),
        transports=[expected_transport],
        notification_type=expected_notification_type,
        notification_details=dict(
            org=dict(
                permalink="54321",
                name="Кафе с едой",
                phone="+7 (495) 739-70-00",
                formatted_address="Город, Улица, 1",
                url="http://cafe.ru",
                categories=["Общепит", "Ресторан"],
                tz_offset=timedelta(seconds=10800),
            ),
            order=make_order(**call_total_cost),
            details_link="http://order-link.ru",
        ),
    )


async def test_errored_if_client_not_found(api, notification_router):
    notification_router.send_client_notification.coro.side_effect = NotFound(
        "Some client not found in Doorman"
    )

    got = await api.post(
        url,
        proto=notification_proto(
            order_created=OrderCreated(order=make_order(as_proto=True))
        ),
        decode_as=Error,
    )

    assert got == Error(
        code=Error.UNKNOWN_CLIENT, description="Some client not found in Doorman"
    )


@pytest.mark.parametrize(
    "recipient_param, expected",
    (
        [
            Recipient(client_id=0, biz_id=15),
            "recipient: {'client_id': ['Must be at least 1.']}",
        ],
        [
            Recipient(client_id=100500, biz_id=0),
            "recipient: {'biz_id': ['Must be at least 1.']}",
        ],
        [
            Recipient(client_id=100500, biz_id=15, phone=0),
            "recipient: {'phone': ['Must be at least 1.']}",
        ],
        [
            Recipient(client_id=100500, biz_id=15, email=""),
            "recipient: {'email': ['Shorter than minimum length 1.']}",
        ],
        [
            Recipient(client_id=100500, biz_id=15, passport_uid=0),
            "recipient: {'passport_uid': ['Must be at least 1.']}",
        ],
        [
            Recipient(client_id=100500, biz_id=15, device_id=""),
            "recipient: {'device_id': ['Shorter than minimum length 1.']}",
        ],
    ),
)
async def test_errored_on_wrong_recipient_input(recipient_param, expected, api):
    got = await api.post(
        url,
        proto=notification_proto(
            recipient=recipient_param,
            order_created=OrderCreated(order=make_order(as_proto=True)),
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(code=Error.VALIDATION_ERROR, description=expected)


@pytest.mark.parametrize(
    "order_input, expected_error",
    [
        (
            dict(booking_code=""),
            "order_changed: {'order': "
            "{'booking_code': ['Shorter than minimum length 1.']}}",
        ),
        (
            dict(total_cost=Cost(final_cost="azaza")),
            "order_changed: {'order': {'total_cost': "
            "{'final_cost': [\"'azaza' value can't be converted to Decimal\"], "
            "'_schema': ['At least one of final_cost or minimal_cost must be specified.']}}}",  # noqa
        ),
        (
            dict(total_cost=Cost(final_cost="")),
            "order_changed: {'order': {'total_cost': "
            "{'final_cost': [\"'' value can't be converted to Decimal\"], "
            "'_schema': ['At least one of final_cost or minimal_cost must be specified.']}}}",  # noqa
        ),
        (
            dict(total_cost=Cost(minimal_cost="azaza")),
            "order_changed: {'order': {'total_cost': "
            "{'minimal_cost': [\"'azaza' value can't be converted to Decimal\"], "
            "'_schema': ['At least one of final_cost or minimal_cost must be specified.']}}}",  # noqa
        ),
        (
            dict(total_cost=Cost(minimal_cost="")),
            "order_changed: {'order': {'total_cost': "
            "{'minimal_cost': [\"'' value can't be converted to Decimal\"], "
            "'_schema': ['At least one of final_cost or minimal_cost must be specified.']}}}",  # noqa
        ),
        (
            dict(total_cost=Cost(final_cost=None)),
            "order_changed: {'order': {'total_cost': {'_schema': "
            "['At least one of final_cost or minimal_cost must be specified.']}}}",
        ),
        (
            dict(total_cost=Cost(minimal_cost=None)),
            "order_changed: {'order': {'total_cost': {'_schema': "
            "['At least one of final_cost or minimal_cost must be specified.']}}}",
        ),
        (
            dict(items=[]),
            "order_changed: {'order': {'items': ['Shorter than minimum length 1.']}}",
        ),
    ],
)
async def test_errored_on_wrong_order_changed_input(api, order_input, expected_error):
    got = await api.post(
        url,
        proto=notification_proto(
            order_changed=OrderChanged(order=make_order(**order_input, as_proto=True))
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(code=Error.VALIDATION_ERROR, description=expected_error)


@pytest.mark.parametrize(
    "order_input, expected_error",
    [
        (
            dict(booking_code=""),
            "order_cancelled: {'order': "
            "{'booking_code': ['Shorter than minimum length 1.']}}",
        ),
        (
            dict(total_cost=Cost(final_cost="azaza")),
            "order_cancelled: {'order': {'total_cost': "
            "{'final_cost': [\"'azaza' value can't be converted to Decimal\"], "
            "'_schema': ['At least one of final_cost or minimal_cost must be specified.']}}}",  # noqa
        ),
        (
            dict(total_cost=Cost(final_cost="")),
            "order_cancelled: {'order': {'total_cost': "
            "{'final_cost': [\"'' value can't be converted to Decimal\"], "
            "'_schema': ['At least one of final_cost or minimal_cost must be specified.']}}}",  # noqa
        ),
        (
            dict(total_cost=Cost(minimal_cost="azaza")),
            "order_cancelled: {'order': {'total_cost': "
            "{'minimal_cost': [\"'azaza' value can't be converted to Decimal\"], "
            "'_schema': ['At least one of final_cost or minimal_cost must be specified.']}}}",  # noqa
        ),
        (
            dict(total_cost=Cost(minimal_cost="")),
            "order_cancelled: {'order': {'total_cost': "
            "{'minimal_cost': [\"'' value can't be converted to Decimal\"], "
            "'_schema': ['At least one of final_cost or minimal_cost must be specified.']}}}",  # noqa
        ),
        (
            dict(total_cost=Cost(final_cost=None)),
            "order_cancelled: {'order': {'total_cost': {'_schema': "
            "['At least one of final_cost or minimal_cost must be specified.']}}}",
        ),
        (
            dict(total_cost=Cost(minimal_cost=None)),
            "order_cancelled: {'order': {'total_cost': {'_schema': "
            "['At least one of final_cost or minimal_cost must be specified.']}}}",
        ),
        (
            dict(items=[]),
            "order_cancelled: {'order': {'items': ['Shorter than minimum length 1.']}}",
        ),
    ],
)
async def test_errored_on_wrong_order_cancelled_input(api, order_input, expected_error):
    got = await api.post(
        url,
        proto=notification_proto(
            order_cancelled=OrderCancelled(
                order=make_order(**order_input, as_proto=True)
            )
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(code=Error.VALIDATION_ERROR, description=expected_error)


@pytest.mark.parametrize(
    "order_input, expected_error",
    [
        (
            dict(booking_code=""),
            "order_reminder: {'order': "
            "{'booking_code': ['Shorter than minimum length 1.']}}",
        ),
        (
            dict(total_cost=Cost(final_cost="azaza")),
            "order_reminder: {'order': {'total_cost': "
            "{'final_cost': [\"'azaza' value can't be converted to Decimal\"], "
            "'_schema': ['At least one of final_cost or minimal_cost must be specified.']}}}",  # noqa
        ),
        (
            dict(total_cost=Cost(final_cost="")),
            "order_reminder: {'order': {'total_cost': "
            "{'final_cost': [\"'' value can't be converted to Decimal\"], "
            "'_schema': ['At least one of final_cost or minimal_cost must be specified.']}}}",  # noqa
        ),
        (
            dict(total_cost=Cost(minimal_cost="azaza")),
            "order_reminder: {'order': {'total_cost': "
            "{'minimal_cost': [\"'azaza' value can't be converted to Decimal\"], "
            "'_schema': ['At least one of final_cost or minimal_cost must be specified.']}}}",  # noqa
        ),
        (
            dict(total_cost=Cost(minimal_cost="")),
            "order_reminder: {'order': {'total_cost': "
            "{'minimal_cost': [\"'' value can't be converted to Decimal\"], "
            "'_schema': ['At least one of final_cost or minimal_cost must be specified.']}}}",  # noqa
        ),
        (
            dict(total_cost=Cost(final_cost=None)),
            "order_reminder: {'order': {'total_cost': {'_schema': "
            "['At least one of final_cost or minimal_cost must be specified.']}}}",
        ),
        (
            dict(total_cost=Cost(minimal_cost=None)),
            "order_reminder: {'order': {'total_cost': {'_schema': "
            "['At least one of final_cost or minimal_cost must be specified.']}}}",
        ),
        (
            dict(items=[]),
            "order_reminder: {'order': {'items': ['Shorter than minimum length 1.']}}",
        ),
    ],
)
async def test_errored_on_wrong_order_reminder_input(api, order_input, expected_error):
    got = await api.post(
        url,
        proto=notification_proto(
            order_reminder=OrderReminder(order=make_order(**order_input, as_proto=True))
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(code=Error.VALIDATION_ERROR, description=expected_error)


@pytest.mark.parametrize(
    "order_input, expected_error",
    [
        (
            dict(booking_code=""),
            "order_created: {'order': "
            "{'booking_code': ['Shorter than minimum length 1.']}}",
        ),
        (
            dict(total_cost=Cost(final_cost="azaza")),
            "order_created: {'order': {'total_cost': "
            "{'final_cost': [\"'azaza' value can't be converted to Decimal\"], "
            "'_schema': ['At least one of final_cost or minimal_cost must be specified.']}}}",  # noqa
        ),
        (
            dict(total_cost=Cost(final_cost="")),
            "order_created: {'order': {'total_cost': "
            "{'final_cost': [\"'' value can't be converted to Decimal\"], "
            "'_schema': ['At least one of final_cost or minimal_cost must be specified.']}}}",  # noqa
        ),
        (
            dict(total_cost=Cost(minimal_cost="azaza")),
            "order_created: {'order': {'total_cost': "
            "{'minimal_cost': [\"'azaza' value can't be converted to Decimal\"], "
            "'_schema': ['At least one of final_cost or minimal_cost must be specified.']}}}",  # noqa
        ),
        (
            dict(total_cost=Cost(minimal_cost="")),
            "order_created: {'order': {'total_cost': "
            "{'minimal_cost': [\"'' value can't be converted to Decimal\"], "
            "'_schema': ['At least one of final_cost or minimal_cost must be specified.']}}}",  # noqa
        ),
        (
            dict(total_cost=Cost(final_cost=None)),
            "order_created: {'order': {'total_cost': {'_schema': "
            "['At least one of final_cost or minimal_cost must be specified.']}}}",
        ),
        (
            dict(total_cost=Cost(minimal_cost=None)),
            "order_created: {'order': {'total_cost': {'_schema': "
            "['At least one of final_cost or minimal_cost must be specified.']}}}",
        ),
        (
            dict(items=[]),
            "order_created: {'order': {'items': ['Shorter than minimum length 1.']}}",
        ),
    ],
)
async def test_errored_on_wrong_order_created_input(api, order_input, expected_error):
    got = await api.post(
        url,
        proto=notification_proto(
            order_created=OrderCreated(order=make_order(**order_input, as_proto=True))
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(code=Error.VALIDATION_ERROR, description=expected_error)


@pytest.mark.parametrize(
    ("transports", "expected_description"),
    [
        (
            [TransportEnum.SMS],
            "notification_type=order_changed, unsupported_transports=SMS",
        ),
        (
            [TransportEnum.EMAIL],
            "notification_type=order_changed, unsupported_transports=EMAIL",
        ),
        (
            [TransportEnum.PUSH],
            "notification_type=order_changed, unsupported_transports=PUSH",
        ),
        (
            [TransportEnum.PUSH, TransportEnum.SMS, TransportEnum.EMAIL],
            "notification_type=order_changed, unsupported_transports=PUSH,SMS,EMAIL",
        ),
    ],
)
async def test_errored_on_unsupported_transport(
    api, notification_router, transports, expected_description
):
    notification_router.send_client_notification.coro.side_effect = (
        UnsupportedTransport(NotificationType.ORDER_CHANGED, transports)
    )

    got = await api.post(
        url,
        proto=notification_proto(
            order_created=OrderCreated(order=make_order(as_proto=True))
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.UNSUPPORTED_TRANSPORT, description=expected_description
    )


async def test_errored_on_unknown_biz_id(api, bvm):
    bvm.fetch_permalinks_by_biz_id.coro.side_effect = BvmNotFound(
        "Some BVM error description"
    )

    got = await api.post(
        url,
        proto=notification_proto(
            order_created=OrderCreated(order=make_order(as_proto=True))
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.UNKNOWN_BIZ_ID, description="Some BVM error description"
    )


async def test_errored_on_business_without_orgs(api, bvm):
    bvm.fetch_permalinks_by_biz_id.coro.return_value = []

    got = await api.post(
        url,
        proto=notification_proto(
            order_created=OrderCreated(order=make_order(as_proto=True))
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.NO_ORGS_FOR_BIZ_ID, description="No permalinks for biz_id 15 in BVM."
    )


async def test_errored_on_org_without_info(api, geosearch):
    geosearch.resolve_org.coro.return_value = None

    got = await api.post(
        url,
        proto=notification_proto(
            order_created=OrderCreated(order=make_order(as_proto=True))
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.NO_ORG_INFO,
        description="Can't resolve org with permalink 54321 in Geosearch.",
    )
