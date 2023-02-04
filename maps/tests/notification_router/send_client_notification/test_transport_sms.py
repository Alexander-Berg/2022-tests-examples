import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.telegraphist.server.lib.enums import NotificationType, Transport
from maps_adv.geosmb.telegraphist.server.lib.notification_router import (
    AddressNotAllowed,
    NoAddress,
    SendFailed,
)

pytestmark = [pytest.mark.asyncio]


async def test_uses_sms_client_to_send_sms(
    notification_router, yasms, notification_details
):
    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111},
        transports=[Transport.SMS],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    yasms.send_sms.assert_called_with(phone=Any(int), text=Any(str))


async def test_uses_provided_phone(
    notification_router, yasms, doorman, notification_details
):
    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111, "phone": 322223},
        transports=[Transport.SMS],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    assert yasms.send_sms.call_args[1]["phone"] == 322223


async def test_uses_phone_from_doorman_if_not_provided(
    notification_router, yasms, doorman, notification_details
):
    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111},
        transports=[Transport.SMS],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    assert yasms.send_sms.call_args[1]["phone"] == 1234567890123


@pytest.mark.parametrize(
    ("notification_type", "expected_sms_text"),
    [
        (
            NotificationType.ORDER_CREATED,
            "Вы записались в «Кафе с едой» на 14:00, пн, 3 февр. "
            "Отменить или перенести: localhost/?order_id=123",
        ),
        (
            NotificationType.ORDER_REMINDER,
            "Напоминаем, что вы записаны в «Кафе с едой» на 14:00, пн, 3 февр."
            " Отменить или перенести: localhost/?order_id=123",
        ),
        (
            NotificationType.ORDER_CHANGED,
            "Время вашей записи в «Кафе с едой» было изменено."
            " Ждем вас в 14:00, пн, 3 февр."
            " Отменить или перенести: localhost/?order_id=123",
        ),
        (
            NotificationType.ORDER_CANCELLED,
            "Ваша запись в «Кафе с едой» на 14:00, пн, 3 февр. отменена",
        ),
    ],
)
async def test_uses_appropriate_sms_template(
    notification_router,
    yasms,
    notification_details,
    notification_type,
    expected_sms_text,
):
    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111},
        transports=[Transport.SMS],
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    assert yasms.send_sms.call_args[1]["text"] == expected_sms_text


@pytest.mark.parametrize(
    ("explicit_phone", "expected_phone"), [(None, 1234567890123), (322223, 322223)]
)
async def test_returns_effective_phone_if_sms_sent(
    notification_router, notification_details, explicit_phone, expected_phone
):
    recipient = {"biz_id": 123, "client_id": 111}
    if explicit_phone is not None:
        recipient["phone"] = explicit_phone

    result = await notification_router.send_client_notification(
        recipient=recipient,
        transports=[Transport.SMS],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    assert result == [{Transport.SMS: {"phone": expected_phone}}]


async def test_does_not_try_to_send_sms_if_no_phone_available(
    notification_router, yasms, doorman, notification_details
):
    del doorman.retrieve_client.coro.return_value["phone"]

    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111},
        transports=[Transport.SMS],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    yasms.send_sms.assert_not_called()


async def test_returns_error_if_no_phone_available(
    notification_router, doorman, notification_details
):
    del doorman.retrieve_client.coro.return_value["phone"]

    result = await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111},
        transports=[Transport.SMS],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    assert result == [{Transport.SMS: Any(NoAddress)}]


async def test_returns_error_if_yasms_client_failed(
    notification_router, yasms, notification_details
):
    yasms.send_sms.coro.side_effect = Exception

    result = await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111, "phone": 322223},
        transports=[Transport.SMS],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    assert result == [{Transport.SMS: Any(SendFailed)}]


@pytest.mark.config(LIMIT_RECIPIENTS=True)
@pytest.mark.parametrize(
    "secret",
    [
        {"value": {}},
        {"value": {"PHONE_RECIPIENTS_WHITELIST": ""}},
        {"value": {"PHONE_RECIPIENTS_WHITELIST": "999999"}},
        {"value": {"PHONE_RECIPIENTS_WHITELIST": "222222,999999"}},
        {"value": {"PHONE_RECIPIENTS_WHITELIST": "1111119"}},
    ],
)
async def test_returns_error_for_not_allowed_recipient_if_recipients_limited(
    notification_router, yasms, yav_client, notification_details, secret
):
    yav_client.retrieve_secret_head.coro.return_value = secret

    result = await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111, "phone": 111111},
        transports=[Transport.SMS],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    assert result == [{Transport.SMS: Any(AddressNotAllowed)}]
    yasms.send_sms.assert_not_called()


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
async def test_raises_for_bad_whitelist_format_if_recipients_limited(
    notification_router, yasms, yav_client, notification_details, whitelist
):
    yav_client.retrieve_secret_head.coro.return_value = {
        "value": {"PHONE_RECIPIENTS_WHITELIST": whitelist}
    }

    with pytest.raises(ValueError):
        await notification_router.send_client_notification(
            recipient={"biz_id": 123, "client_id": 111, "phone": 111111},
            transports=[Transport.SMS],
            notification_type=NotificationType.ORDER_CREATED,
            notification_details=notification_details[NotificationType.ORDER_CREATED],
        )

    yasms.send_sms.assert_not_called()


@pytest.mark.config(LIMIT_RECIPIENTS=True)
@pytest.mark.parametrize(
    "whitelist", ["111111", "111111,999999", "999999,111111", "  111111  , 999999"]
)
async def test_sends_sms_for_allowed_recipient_if_recipients_limited(
    notification_router, yasms, yav_client, notification_details, whitelist
):
    yav_client.retrieve_secret_head.coro.return_value = {
        "value": {"PHONE_RECIPIENTS_WHITELIST": whitelist}
    }

    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111, "phone": 111111},
        transports=[Transport.SMS],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    assert yasms.send_sms.call_args[1]["phone"] == 111111
