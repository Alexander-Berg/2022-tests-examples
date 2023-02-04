from datetime import timedelta
from decimal import Decimal

import pytest

from maps_adv.geosmb.doorman.client import NotFound
from maps_adv.geosmb.telegraphist.proto.errors_pb2 import Error
from maps_adv.geosmb.telegraphist.proto.v2.common_pb2 import Cost, Transport
from maps_adv.geosmb.telegraphist.proto.v2.notifications_for_business_pb2 import (
    Client,
    Notification,
    OrderCancelled,
    OrderChanged,
    OrderCreated,
    Recipient,
)
from maps_adv.geosmb.telegraphist.server.lib.enums import (
    NotificationType,
    Transport as TransportEnum,
)
from maps_adv.geosmb.telegraphist.server.tests.helpers import (
    make_order_for_business_notification,
)

pytestmark = [pytest.mark.asyncio]

url = "/api/v2/send-notification-for-business/"


def notification_proto(**updates) -> Notification:
    kwargs = dict(
        recipient=Recipient(
            biz_id=15,
            cabinet_link="http://cabinet.link",
            company_link="http://company.link",
        ),
        transports=[Transport.EMAIL],
    )
    kwargs.update(updates)

    return Notification(**kwargs)


@pytest.mark.parametrize(
    "pb_order_field, pb_order_cls, expected_notification_type",
    [
        (
            "order_created",
            OrderCreated,
            NotificationType.ORDER_CREATED_FOR_BUSINESS,
        ),
        (
            "order_cancelled",
            OrderCancelled,
            NotificationType.ORDER_CANCELLED_FOR_BUSINESS,
        ),
        (
            "order_changed",
            OrderChanged,
            NotificationType.ORDER_CHANGED_FOR_BUSINESS,
        ),
    ],
)
@pytest.mark.parametrize("comment", [dict(), dict(comment="some_comment")])
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
async def test_will_notify_about_order_events(
    notification_router,
    api,
    comment,
    pb_order_field,
    pb_order_cls,
    expected_notification_type,
    pb_total_cost,
    call_total_cost,
):
    await api.post(
        url,
        proto=notification_proto(
            **{
                pb_order_field: pb_order_cls(
                    client=Client(client_id=160),
                    order=make_order_for_business_notification(
                        as_proto=True, **comment, **pb_total_cost
                    ),
                    details_link="http://order-link.ru",
                ),
            },
        ),
    )

    notification_router.send_business_notification.assert_called_with(
        recipient=dict(biz_id=15),
        transports=[TransportEnum.EMAIL],
        notification_type=expected_notification_type,
        notification_details=dict(
            client=dict(
                name="client_first_name client_last_name",
                phone=1234567890123,
            ),
            org=dict(
                formatted_address="Город, Улица, 1",
                cabinet_link="http://cabinet.link",
                company_link="http://company.link",
                tz_offset=timedelta(seconds=10800),
            ),
            order=make_order_for_business_notification(**comment, **call_total_cost),
            details_link="http://order-link.ru",
        ),
    )


async def test_errored_on_wrong_client_input(api):
    got = await api.post(
        url,
        proto=notification_proto(
            order_created=OrderCreated(
                client=Client(client_id=0),
                order=make_order_for_business_notification(as_proto=True),
            ),
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.VALIDATION_ERROR,
        description="order_created: {'client': {'client_id': ['Must be at least 1.']}}",
    )


@pytest.mark.parametrize(
    "order_input, expected_error",
    [
        (
            dict(booking_code=""),
            "{'booking_code': ['Shorter than minimum length 1.']}",
        ),
        (
            dict(comment=""),
            "{'comment': ['Shorter than minimum length 1.']}",
        ),
        (
            dict(source=""),
            "{'source': ['Shorter than minimum length 1.']}",
        ),
        (
            dict(total_cost=Cost(final_cost="azaza")),
            "{'total_cost': {'final_cost': [\"'azaza' value can't be converted to Decimal\"], "  # noqa
            "'_schema': ['At least one of final_cost or minimal_cost must be specified.']}}",  # noqa
        ),
        (
            dict(total_cost=Cost(final_cost="")),
            "{'total_cost': {'final_cost': [\"'' value can't be converted to Decimal\"], "  # noqa
            "'_schema': ['At least one of final_cost or minimal_cost must be specified.']}}",  # noqa
        ),
        (
            dict(total_cost=Cost(minimal_cost="azaza")),
            "{'total_cost': {'minimal_cost': [\"'azaza' value can't be converted to Decimal\"], "  # noqa
            "'_schema': ['At least one of final_cost or minimal_cost must be specified.']}}",  # noqa
        ),
        (
            dict(total_cost=Cost(final_cost=None)),
            "{'total_cost': {'_schema': ['At least one of final_cost or minimal_cost must be specified.']}}",  # noqa
        ),
        (
            dict(total_cost=Cost(minimal_cost=None)),
            "{'total_cost': {'_schema': ['At least one of final_cost or minimal_cost must be specified.']}}",  # noqa
        ),
        (
            dict(total_cost=Cost(minimal_cost="")),
            "{'total_cost': {'minimal_cost': [\"'' value can't be converted to Decimal\"], "  # noqa
            "'_schema': ['At least one of final_cost or minimal_cost must be specified.']}}",  # noqa
        ),
        (
            dict(items=[]),
            "{'items': ['Shorter than minimum length 1.']}",
        ),
    ],
)
@pytest.mark.parametrize(
    "pb_order_field, pb_order_cls",
    [
        ("order_changed", OrderChanged),
        ("order_created", OrderCreated),
        ("order_cancelled", OrderCancelled),
    ],
)
async def test_errored_on_wrong_order_changed_input(
    api, order_input, expected_error, pb_order_field, pb_order_cls
):
    got = await api.post(
        url,
        proto=notification_proto(
            **{
                pb_order_field: pb_order_cls(
                    client=Client(client_id=160),
                    order=make_order_for_business_notification(
                        **order_input, as_proto=True
                    ),
                )
            }
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.VALIDATION_ERROR,
        description=pb_order_field + ": {'order': " + expected_error + "}",
    )


async def test_errored_if_client_not_found(api, notification_router, doorman):
    doorman.retrieve_client.coro.side_effect = NotFound(
        "Some client not found in Doorman"
    )

    got = await api.post(
        url,
        proto=notification_proto(
            order_created=OrderCreated(
                client=Client(client_id=160),
                order=make_order_for_business_notification(as_proto=True),
            ),
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.UNKNOWN_CLIENT, description="Some client not found in Doorman"
    )
