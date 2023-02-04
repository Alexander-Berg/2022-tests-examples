from copy import deepcopy

import pytest

from maps_adv.geosmb.doorman.client import NotFound
from maps_adv.geosmb.telegraphist.server.lib.enums import NotificationType, Transport
from maps_adv.geosmb.telegraphist.server.lib.notification_router import (
    NotificationRouter,
    SendFailed,
    UnsupportedNotificationType,
    UnsupportedTransport,
    supported_notifications,
)

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "notification_type",
    [
        NotificationType.ORDER_CHANGED,
        NotificationType.ORDER_CREATED,
        NotificationType.ORDER_CANCELLED,
        NotificationType.ORDER_REMINDER,
    ],
)
async def test_uses_clients_for_all_provided_transports_for_supported_notification_type(
    notification_type,
    notification_router,
    notification_details,
    yasms,
    email_client,
    sup_client,
):
    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111},
        transports=[Transport.SMS, Transport.EMAIL, Transport.PUSH],
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    yasms.send_sms.assert_called()
    email_client.send_message.assert_called()
    sup_client.send_push_notifications.assert_called()


@pytest.mark.parametrize(
    "notification_type",
    [
        NotificationType.ORDER_CHANGED,
        NotificationType.ORDER_CREATED,
        NotificationType.ORDER_CANCELLED,
        NotificationType.ORDER_REMINDER,
    ],
)
async def test_retrieves_doorman_client_data(
    notification_type,
    notification_router,
    notification_details,
    doorman,
):
    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111},
        transports=[Transport.SMS, Transport.EMAIL, Transport.PUSH],
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    doorman.retrieve_client.assert_called_with(biz_id=123, client_id=111)


@pytest.mark.parametrize(
    "notification_type",
    [
        NotificationType.ORDER_CHANGED,
        NotificationType.ORDER_CREATED,
        NotificationType.ORDER_CANCELLED,
        NotificationType.ORDER_REMINDER,
    ],
)
async def test_raises_if_client_not_found_in_doorman(
    notification_router, notification_details, notification_type, doorman
):
    doorman.retrieve_client.coro.side_effect = NotFound

    with pytest.raises(NotFound):
        await notification_router.send_client_notification(
            recipient={"biz_id": 123, "client_id": 111},
            transports=[Transport.SMS, Transport.EMAIL, Transport.PUSH],
            notification_type=notification_type,
            notification_details=notification_details[notification_type],
        )


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
    ],
)
async def test_errored_for_unsupported_notifications(
    notification_type,
    notification_router,
    notification_details,
    yasms,
    email_client,
    sup_client,
):
    with pytest.raises(UnsupportedNotificationType) as exc:
        await notification_router.send_client_notification(
            recipient={"biz_id": 123, "client_id": 111},
            transports=[Transport.SMS, Transport.EMAIL, Transport.PUSH],
            notification_type=notification_type,
            notification_details=notification_details[notification_type],
        )

    assert exc.value.notification_type == notification_type
    yasms.send_sms.assert_not_called()
    email_client.send_message.assert_not_called()
    sup_client.send_push_notifications.assert_not_called()


async def test_returns_sending_results_as_expected(
    notification_router,
    notification_details,
    yasms,
    email_client,
    sup_client,
):
    got = await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111},
        transports=[Transport.SMS, Transport.EMAIL, Transport.PUSH],
        notification_type=NotificationType.ORDER_CHANGED,
        notification_details=notification_details[NotificationType.ORDER_CHANGED],
    )

    assert got == [
        {Transport.SMS: {"phone": 1234567890123}},
        {Transport.EMAIL: {"email": "email@yandex.ru"}},
        {Transport.PUSH: {"passport_uid": 456}},
    ]


async def test_uses_other_transports_if_one_fails(
    notification_router, notification_details, yasms, email_client
):
    yasms.send_sms.coro.side_effect = SendFailed
    email_client.send_message.coro.side_effect = SendFailed

    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111},
        transports=[Transport.SMS, Transport.EMAIL],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    yasms.send_sms.assert_called()
    email_client.send_message.assert_called()


@pytest.mark.parametrize(
    "transports",
    [
        [Transport.PUSH],
        [Transport.SMS, Transport.PUSH],
        [Transport.PUSH, Transport.SMS],
    ],
)
async def test_raises_for_unsupported_transport(
    mocker,
    doorman,
    email_client,
    yasms,
    yav_client,
    sup_client,
    notification_details,
    email_template_codes,
    tuner_client,
    transports,
):
    supported_clone = deepcopy(supported_notifications)
    del supported_clone["client"][NotificationType.ORDER_CREATED][Transport.PUSH]
    mocker.patch(
        "maps_adv.geosmb.telegraphist.server.lib.notification_router.supported_notifications",  # noqa
        new=supported_clone,
    )

    notification_router = NotificationRouter(
        doorman_client=doorman,
        email_client=email_client,
        yasms=yasms,
        yav_client=yav_client,
        tuner_client=tuner_client,
        sup_client=sup_client,
        email_template_codes=email_template_codes,
        yav_secret_id="kek-id",
    )

    with pytest.raises(UnsupportedTransport) as exc:
        await notification_router.send_client_notification(
            recipient={"biz_id": 123, "client_id": 111},
            transports=transports,
            notification_type=NotificationType.ORDER_CREATED,
            notification_details=notification_details[NotificationType.ORDER_CREATED],
        )
    assert exc.value.notification_type == NotificationType.ORDER_CREATED
    assert exc.value.transports == [Transport.PUSH]
    yasms.send_sms.assert_not_called()
