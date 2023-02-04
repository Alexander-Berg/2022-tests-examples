import pytest
from smb.common.testing_utils import Any

from maps_adv.common import aiosup
from maps_adv.geosmb.telegraphist.server.lib.enums import NotificationType, Transport
from maps_adv.geosmb.telegraphist.server.lib.notification_router import (
    AddressNotAllowed,
    NoAddress,
    NoOrderLink,
    SendFailed,
)

pytestmark = [pytest.mark.asyncio]


async def test_uses_sup_client_to_send_push(
    notification_router, sup_client, notification_details
):
    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111, "device_id": "1234567"},
        transports=[Transport.PUSH],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    sup_client.send_push_notifications.assert_called_with(
        receiver=[Any(aiosup.Receiver)], notification=Any(aiosup.Notification)
    )


@pytest.mark.parametrize(
    "recipient_params, expected_push_receiver",
    [
        ({"device_id": "1234567"}, aiosup.Receiver(did="1234567")),
        ({"passport_uid": 1234567}, aiosup.Receiver(uid="1234567")),
        # device_id has priority over passport_uid
        (
            {"device_id": "111111", "passport_uid": 222222},
            aiosup.Receiver(did="111111"),
        ),
    ],
)
async def test_uses_provided_receiver_id(
    notification_router,
    sup_client,
    doorman,
    notification_details,
    recipient_params,
    expected_push_receiver,
):
    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111, **recipient_params},
        transports=[Transport.PUSH],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    assert sup_client.send_push_notifications.call_args[1]["receiver"] == [
        expected_push_receiver
    ]


async def test_uses_passport_uid_from_doorman_if_no_receiver_id_provided(
    notification_router, sup_client, doorman, notification_details
):
    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111},
        transports=[Transport.PUSH],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    assert sup_client.send_push_notifications.call_args[1]["receiver"] == [
        aiosup.Receiver(uid="456")
    ]


@pytest.mark.parametrize(
    ("notification_type", "expected_push_title", "expected_push_body"),
    [
        (
            NotificationType.ORDER_CREATED,
            "Вы записаны!",
            "«Кафе с едой» ждёт вас в 14:00, пн, 3 февр.",
        ),
        (
            NotificationType.ORDER_REMINDER,
            "Напоминаем о записи",
            "Вы записаны в «Кафе с едой» на 14:00, пн, 3 февр.",
        ),
        (
            NotificationType.ORDER_CANCELLED,
            "Запись отменена",
            "Ваша запись в «Кафе с едой» на 14:00, пн, 3 февр. отменена",
        ),
        (
            NotificationType.ORDER_CHANGED,
            "Запись изменена",
            "Время вашей записи в «Кафе с едой» было изменено",
        ),
    ],
)
async def test_uses_appropriate_push_template(
    notification_router,
    sup_client,
    notification_details,
    notification_type,
    expected_push_title,
    expected_push_body,
):
    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111, "device_id": "1234567"},
        transports=[Transport.PUSH],
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    assert (
        sup_client.send_push_notifications.call_args[1]["notification"].title
        == expected_push_title
    )
    assert (
        sup_client.send_push_notifications.call_args[1]["notification"].body
        == expected_push_body
    )


@pytest.mark.parametrize(
    "notification_type",
    [
        NotificationType.ORDER_CANCELLED,
        NotificationType.ORDER_CHANGED,
        NotificationType.ORDER_CREATED,
        NotificationType.ORDER_REMINDER,
    ],
)
async def test_uses_provided_link(
    notification_router, sup_client, notification_details, notification_type
):
    notification_details = notification_details[notification_type]
    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111, "device_id": "1234567"},
        transports=[Transport.PUSH],
        notification_type=notification_type,
        notification_details=notification_details,
    )

    assert sup_client.send_push_notifications.call_args[1][
        "notification"
    ].link == notification_details.get("details_link")


@pytest.mark.parametrize(
    "recipient_params, expected_receiver_id_returned",
    [
        ({}, {"passport_uid": 456}),  # from doorman
        ({"device_id": "1234567"}, {"device_id": "1234567"}),
        ({"passport_uid": 1234567}, {"passport_uid": 1234567}),
    ],
)
async def test_returns_receiver_id_if_push_sent(
    notification_router,
    notification_details,
    recipient_params,
    expected_receiver_id_returned,
    doorman,
):
    result = await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111, **recipient_params},
        transports=[Transport.PUSH],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    assert result == [{Transport.PUSH: expected_receiver_id_returned}]


async def test_does_not_send_push_if_no_receiver_id_available(
    notification_router, doorman, sup_client, notification_details
):
    del doorman.retrieve_client.coro.return_value["passport_uid"]

    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111},
        transports=[Transport.PUSH],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    sup_client.send_push_notifications.assert_not_called()


async def test_returns_error_if_no_receiver_ids_available(
    notification_router, doorman, notification_details
):
    del doorman.retrieve_client.coro.return_value["passport_uid"]

    result = await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111},
        transports=[Transport.PUSH],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    assert result == [{Transport.PUSH: Any(NoAddress)}]


async def test_returns_error_if_sup_client_failed(
    notification_router, sup_client, notification_details
):
    sup_client.send_push_notifications.coro.side_effect = Exception

    result = await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111, "device_id": "1234567"},
        transports=[Transport.PUSH],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    assert result == [{Transport.PUSH: Any(SendFailed)}]


@pytest.mark.config(LIMIT_RECIPIENTS=True)
@pytest.mark.parametrize(
    "recipient_id, whitelist_key",
    [
        ({"passport_uid": 111111}, "PASSPORT_UID_RECIPIENTS_WHITELIST"),
        ({"device_id": "111111"}, "DEVICE_ID_RECIPIENTS_WHITELIST"),
    ],
)
@pytest.mark.parametrize("whitelist", ["", "999999", "222222,999999", "1111119"])
async def test_returns_error_for_not_allowed_recipient(
    notification_router,
    yav_client,
    sup_client,
    notification_details,
    whitelist,
    recipient_id,
    whitelist_key,
):
    yav_client.retrieve_secret_head.coro.return_value["value"][
        whitelist_key
    ] = whitelist

    result = await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111, "passport_uid": 111111},
        transports=[Transport.PUSH],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    assert result == [{Transport.PUSH: Any(AddressNotAllowed)}]
    sup_client.send_push_notifications.assert_not_called()


@pytest.mark.config(LIMIT_RECIPIENTS=True)
@pytest.mark.parametrize(
    "recipient_id, whitelist_key",
    [
        ({"passport_uid": 111111}, "PASSPORT_UID_RECIPIENTS_WHITELIST"),
        ({"device_id": "111111"}, "DEVICE_ID_RECIPIENTS_WHITELIST"),
    ],
)
async def test_returns_error_if_whitelist_is_not_set(
    notification_router,
    yav_client,
    sup_client,
    notification_details,
    recipient_id,
    whitelist_key,
):
    del yav_client.retrieve_secret_head.coro.return_value["value"][whitelist_key]

    result = await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111, "passport_uid": 111111},
        transports=[Transport.PUSH],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    assert result == [{Transport.PUSH: Any(AddressNotAllowed)}]
    sup_client.send_push_notifications.assert_not_called()


@pytest.mark.config(LIMIT_RECIPIENTS=True)
async def test_ignores_passport_whitelist_if_is_pushed_by_device_id(
    notification_router, yav_client, sup_client, notification_details
):
    yav_client.retrieve_secret_head.coro.return_value = {
        "value": {
            "DEVICE_ID_RECIPIENTS_WHITELIST": "111111",
            "PASSPORT_UID_RECIPIENTS_WHITELIST": "222222",
        }
    }

    await notification_router.send_client_notification(
        recipient={
            "biz_id": 123,
            "client_id": 111,
            "device_id": "111111",
            "passport_uid": 111111,
        },
        transports=[Transport.PUSH],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    assert sup_client.send_push_notifications.call_args[1]["receiver"] == [
        aiosup.Receiver(did="111111")
    ]


@pytest.mark.config(LIMIT_RECIPIENTS=True)
@pytest.mark.parametrize(
    "whitelist",
    [
        # bad delimiter
        "111111;999999",
        # not int
        "abc",
    ],
)
async def test_raises_for_bad_whitelist_format(
    notification_router, sup_client, yav_client, notification_details, whitelist
):
    yav_client.retrieve_secret_head.coro.return_value["value"][
        "PASSPORT_UID_RECIPIENTS_WHITELIST"
    ] = whitelist

    with pytest.raises(ValueError):
        await notification_router.send_client_notification(
            recipient={"biz_id": 123, "client_id": 111, "passport_uid": 111111},
            transports=[Transport.PUSH],
            notification_type=NotificationType.ORDER_CREATED,
            notification_details=notification_details[NotificationType.ORDER_CREATED],
        )

    sup_client.send_push_notifications.assert_not_called()


@pytest.mark.config(LIMIT_RECIPIENTS=True)
@pytest.mark.parametrize(
    "recipient_id, whitelist_key, expected_receiver",
    [
        (
            {"passport_uid": 111111},
            "PASSPORT_UID_RECIPIENTS_WHITELIST",
            aiosup.Receiver(uid="111111"),
        ),
        (
            {"device_id": "111111"},
            "DEVICE_ID_RECIPIENTS_WHITELIST",
            aiosup.Receiver(did="111111"),
        ),
    ],
)
@pytest.mark.parametrize(
    "whitelist", ["111111", "111111,999999", "999999,111111", "  111111  , 999999"]
)
async def test_sends_push_for_allowed_recipient_if_recipients_limited(
    notification_router,
    yav_client,
    sup_client,
    notification_details,
    recipient_id,
    whitelist_key,
    expected_receiver,
    whitelist,
):
    yav_client.retrieve_secret_head.coro.return_value["value"][
        whitelist_key
    ] = whitelist

    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111, **recipient_id},
        transports=[Transport.PUSH],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    assert sup_client.send_push_notifications.call_args[1]["receiver"] == [
        expected_receiver
    ]


@pytest.mark.parametrize(
    "notification_type",
    [
        NotificationType.ORDER_CANCELLED,
        NotificationType.ORDER_CHANGED,
        NotificationType.ORDER_CREATED,
        NotificationType.ORDER_REMINDER,
    ],
)
async def test_raises_for_missed_order_link(
    notification_router, sup_client, yav_client, notification_details, notification_type
):
    order_details = notification_details[notification_type].copy()
    del order_details["details_link"]

    got = await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111, "passport_uid": 111111},
        transports=[Transport.PUSH],
        notification_type=notification_type,
        notification_details=order_details,
    )

    assert got == [{Transport.PUSH: Any(NoOrderLink)}]
