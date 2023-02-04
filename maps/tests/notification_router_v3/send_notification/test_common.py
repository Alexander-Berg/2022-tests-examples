import pytest

from maps_adv.geosmb.telegraphist.server.lib.enums import NotificationType, Transport
from maps_adv.geosmb.telegraphist.server.lib.exceptions import (
    UnsupportedNotificationType,
    UnsupportedTransport,
)

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "notification_type",
    [
        NotificationType.CERTIFICATE_CONNECT_PAYMENT,
    ],
)
async def test_errored_for_unsupported_notifications(
    notification_type,
    notification_router_v3,
    notification_details,
    yasms,
    email_client,
):
    with pytest.raises(UnsupportedNotificationType) as exc:
        await notification_router_v3.send_notification(
            transport=dict(
                type=Transport.PUSH,
            ),
            notification_type=notification_type,
            notification_details=notification_details[notification_type],
        )

    assert exc.value.notification_type == notification_type

    email_client.send_message.assert_not_called()
    yasms.send_sms.assert_not_called()


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
async def test_returns_sending_results_as_expected(
    notification_router_v3, notification_details, email_transport, notification_type
):
    got = await notification_router_v3.send_notification(
        transport=email_transport,
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    assert got == [
        {Transport.EMAIL: {"email": "foo@yandex-team.ru"}},
        {Transport.EMAIL: {"email": "kek@yandex.ru"}},
    ]


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
async def test_raises_for_unsupported_transport(
    notification_details,
    notification_router_v3,
    notification_type
):
    with pytest.raises(UnsupportedTransport) as exc:
        await notification_router_v3.send_notification(
            transport=dict(
                type=Transport.PUSH,
            ),
            notification_type=notification_type,
            notification_details=notification_details[notification_type],
        )

    assert exc.value.notification_type == notification_type
    assert exc.value.transports == [Transport.PUSH]
