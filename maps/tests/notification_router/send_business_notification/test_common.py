import pytest

from maps_adv.geosmb.telegraphist.server.lib.enums import NotificationType, Transport
from maps_adv.geosmb.telegraphist.server.lib.notification_router import (
    UnsupportedNotificationType,
    UnsupportedTransport,
)
from maps_adv.geosmb.tuner.client import UnknownBizId

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "notification_type",
    [
        NotificationType.ORDER_CREATED_FOR_BUSINESS,
        NotificationType.ORDER_CHANGED_FOR_BUSINESS,
        NotificationType.ORDER_CANCELLED_FOR_BUSINESS,
        NotificationType.CERTIFICATE_EXPIRING,
        NotificationType.CERTIFICATE_EXPIRED,
        NotificationType.CERTIFICATE_REJECTED,
        NotificationType.CERTIFICATE_CONNECT_PAYMENT,
        NotificationType.FIRST_CERTIFICATE_APPROVED,
        NotificationType.SUBSEQUENT_CERTIFICATE_APPROVED,
        NotificationType.CERTIFICATE_PURCHASED,
        NotificationType.CERTIFICATE_CREATED,
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
    ],
)
async def test_uses_clients_for_provided_transport_for_supported_notifications(
    notification_type, notification_router, notification_details, email_client
):
    await notification_router.send_business_notification(
        recipient={"biz_id": 123},
        transports=[Transport.EMAIL],
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    email_client.send_message.assert_called()


@pytest.mark.parametrize(
    "notification_type",
    [
        NotificationType.ORDER_CREATED_FOR_BUSINESS,
        NotificationType.ORDER_CHANGED_FOR_BUSINESS,
        NotificationType.ORDER_CANCELLED_FOR_BUSINESS,
        NotificationType.CERTIFICATE_EXPIRING,
        NotificationType.CERTIFICATE_EXPIRED,
        NotificationType.CERTIFICATE_REJECTED,
        NotificationType.CERTIFICATE_CONNECT_PAYMENT,
        NotificationType.FIRST_CERTIFICATE_APPROVED,
        NotificationType.SUBSEQUENT_CERTIFICATE_APPROVED,
        NotificationType.CERTIFICATE_PURCHASED,
        NotificationType.CERTIFICATE_CREATED,
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
    ],
)
async def test_fetches_business_settings(
    notification_type,
    notification_router,
    email_client,
    tuner_client,
    notification_details,
):
    await notification_router.send_business_notification(
        recipient={"biz_id": 123},
        transports=[Transport.EMAIL],
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    tuner_client.fetch_settings.assert_called_with(biz_id=123)


@pytest.mark.parametrize(
    "notification_type",
    [
        NotificationType.ORDER_CREATED_FOR_BUSINESS,
        NotificationType.ORDER_CHANGED_FOR_BUSINESS,
        NotificationType.ORDER_CANCELLED_FOR_BUSINESS,
        NotificationType.CERTIFICATE_EXPIRING,
        NotificationType.CERTIFICATE_EXPIRED,
        NotificationType.CERTIFICATE_REJECTED,
        NotificationType.CERTIFICATE_CONNECT_PAYMENT,
        NotificationType.FIRST_CERTIFICATE_APPROVED,
        NotificationType.SUBSEQUENT_CERTIFICATE_APPROVED,
        NotificationType.CERTIFICATE_PURCHASED,
        NotificationType.CERTIFICATE_CREATED,
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
    ],
)
async def test_raises_on_tuner_exceptions(
    notification_type,
    notification_router,
    email_client,
    tuner_client,
    notification_details,
):
    tuner_client.fetch_settings.coro.side_effect = UnknownBizId("Unknown biz_id.")

    with pytest.raises(UnknownBizId, match="Unknown biz_id."):
        await notification_router.send_business_notification(
            recipient={"biz_id": 123},
            transports=[Transport.EMAIL],
            notification_type=notification_type,
            notification_details=notification_details[notification_type],
        )


@pytest.mark.parametrize(
    "notification_type",
    [
        NotificationType.ORDER_CHANGED,
        NotificationType.ORDER_CREATED,
        NotificationType.ORDER_CANCELLED,
        NotificationType.ORDER_REMINDER,
    ],
)
async def test_errored_for_unsupported_notifications(
    notification_type,
    notification_router,
    notification_details,
    yasms,
    sup_client,
    email_client,
):
    with pytest.raises(UnsupportedNotificationType) as exc:
        await notification_router.send_business_notification(
            recipient={"biz_id": 123},
            transports=[Transport.SMS, Transport.EMAIL, Transport.PUSH],
            notification_type=notification_type,
            notification_details=notification_details[notification_type],
        )

    assert exc.value.notification_type == notification_type

    email_client.send_message.assert_not_called()
    sup_client.send_push_notifications.assert_not_called()
    yasms.send_sms.assert_not_called()


async def test_returns_sending_results_as_expected(
    notification_router, notification_details, email_client
):
    got = await notification_router.send_business_notification(
        recipient={"biz_id": 123},
        transports=[Transport.EMAIL],
        notification_type=NotificationType.CERTIFICATE_CREATED,
        notification_details=notification_details[NotificationType.CERTIFICATE_CREATED],
    )

    assert got == [
        {Transport.EMAIL: {"email": "kek@yandex.ru"}},
        {Transport.EMAIL: {"email": "foo@yandex-team.ru"}},
    ]


@pytest.mark.parametrize(
    "transports, expected_unsupported_transport",
    [
        ([Transport.PUSH], [Transport.PUSH]),
        ([Transport.PUSH, Transport.EMAIL], [Transport.PUSH]),
        (
            [Transport.PUSH, Transport.SMS, Transport.EMAIL],
            [Transport.PUSH, Transport.SMS],
        ),
    ],
)
async def test_raises_for_unsupported_transport(
    email_client,
    yasms,
    sup_client,
    notification_details,
    transports,
    notification_router,
    expected_unsupported_transport,
):
    with pytest.raises(UnsupportedTransport) as exc:
        await notification_router.send_business_notification(
            recipient={"biz_id": 123},
            transports=transports,
            notification_type=NotificationType.ORDER_CREATED_FOR_BUSINESS,
            notification_details=notification_details[
                NotificationType.ORDER_CREATED_FOR_BUSINESS
            ],
        )
    assert exc.value.notification_type == NotificationType.ORDER_CREATED_FOR_BUSINESS
    assert exc.value.transports == expected_unsupported_transport
    yasms.send_sms.assert_not_called()
    sup_client.send_push_notifications.assert_not_called()
