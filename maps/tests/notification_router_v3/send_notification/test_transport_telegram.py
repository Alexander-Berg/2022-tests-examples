import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.telegraphist.server.lib.enums import NotificationType, Transport
from maps_adv.geosmb.telegraphist.server.lib.exceptions import (
    AddressNotAllowed,
    NoAddress,
    SendFailed,
)

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
async def test_uses_notify_me_to_send_message(
    notification_router_v3, notify_me, notification_details, telegram_transport, notification_type
):
    await notification_router_v3.send_notification(
        transport=telegram_transport,
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    notify_me.send_message.assert_called_with(telegram_uid=Any(int), text=Any(str))


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
async def test_uses_appropriate_message_template(
    notification_router_v3, notify_me, notification_details, telegram_transport, notification_type
):
    await notification_router_v3.send_notification(
        transport=telegram_transport,
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    assert notify_me.send_message.call_args[1]["text"] == (
        "Вам пришла новая заявка или заказ — свяжитесь с клиентом, чтобы уточнить детали. " +
        "Будет здорово, если получится сделать это в ближайшее время.\n\n" +
        "Посмотреть подробности можно по ссылке: http://details.link"
    )


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
async def test_returns_effective_telegram_uid_if_message_sent(
    notification_router_v3, notification_details, telegram_transport, notification_type
):
    result = await notification_router_v3.send_notification(
        transport=telegram_transport,
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    assert result == [
        {Transport.TELEGRAM: {"telegram_uid": 496329590}},
        {Transport.TELEGRAM: {"telegram_uid": 496329591}},
    ]


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
async def test_does_not_try_to_send_message_if_no_telegra_uid_available(
    notification_router_v3, notify_me, notification_details, notification_type
):

    await notification_router_v3.send_notification(
        transport=dict(type=Transport.TELEGRAM),
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    notify_me.send_message.assert_not_called()


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
async def test_returns_error_if_no_telegram_uid_available(
    notification_router_v3, notification_details, notification_type
):
    result = await notification_router_v3.send_notification(
        transport=dict(type=Transport.TELEGRAM),
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    assert result == [{Transport.TELEGRAM: Any(NoAddress)}]


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
async def test_returns_error_if_notify_me_client_failed(
    notification_router_v3, notify_me, notification_details, telegram_transport, notification_type
):
    notify_me.send_message.coro.side_effect = Exception

    result = await notification_router_v3.send_notification(
        transport=telegram_transport,
        notification_type=notification_type,
        notification_details=notification_details[notification_type]
    )

    assert result == [{Transport.TELEGRAM: Any(SendFailed)}, {Transport.TELEGRAM: Any(SendFailed)}]


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
@pytest.mark.config(LIMIT_RECIPIENTS=True)
@pytest.mark.parametrize(
    "secret",
    [
        {"value": {}},
        {"value": {"TELEGRAM_UIDS_RECIPIENTS_WHITELIST": ""}},
        {"value": {"TELEGRAM_UIDS_RECIPIENTS_WHITELIST": "9999999"}},
        {"value": {"TELEGRAM_UIDS_RECIPIENTS_WHITELIST": "2222222,9999999"}},
        {"value": {"TELEGRAM_UIDS_RECIPIENTS_WHITELIST": "1111119"}},
    ],
)
async def test_returns_error_for_not_allowed_recipient_if_recipients_limited(
    notification_router_v3, notify_me, yav_client, notification_details, secret, telegram_transport, notification_type
):
    yav_client.retrieve_secret_head.coro.return_value = secret

    result = await notification_router_v3.send_notification(
        transport=telegram_transport,
        notification_type=notification_type,
        notification_details=notification_details[notification_type]
    )

    assert result == [{Transport.TELEGRAM: Any(AddressNotAllowed)}, {Transport.TELEGRAM: Any(AddressNotAllowed)}]
    notify_me.send_message.assert_not_called()


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
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
    notification_router_v3, notify_me, yav_client, notification_details, whitelist, telegram_transport, notification_type
):
    yav_client.retrieve_secret_head.coro.return_value = {
        "value": {"TELEGRAM_UIDS_RECIPIENTS_WHITELIST": whitelist}
    }

    result = await notification_router_v3.send_notification(
        transport=telegram_transport,
        notification_type=notification_type,
        notification_details=notification_details[notification_type]
    )

    assert result == [{Transport.TELEGRAM: Any(ValueError)}, {Transport.TELEGRAM: Any(ValueError)}]

    notify_me.send_message.assert_not_called()


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
@pytest.mark.config(LIMIT_RECIPIENTS=True)
@pytest.mark.parametrize(
    "whitelist", ["496329590", "496329590,999999", "999999,496329590", "  496329590  , 999999"]
)
async def test_sends_message_for_allowed_recipient_if_recipients_limited(
    notification_router_v3, notify_me, yav_client, notification_details, whitelist, telegram_transport, notification_type
):
    yav_client.retrieve_secret_head.coro.return_value = {
        "value": {"TELEGRAM_UIDS_RECIPIENTS_WHITELIST": whitelist}
    }

    await notification_router_v3.send_notification(
        transport=telegram_transport,
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    assert notify_me.send_message.call_args[1]["telegram_uid"] == 496329590
