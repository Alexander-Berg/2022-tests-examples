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
async def test_uses_sms_client_to_send_sms(
    notification_router_v3, yasms, notification_details, sms_transport, notification_type
):
    await notification_router_v3.send_notification(
        transport=sms_transport,
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    yasms.send_sms.assert_called_with(phone=Any(int), text=Any(str))


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
async def test_uses_appropriate_sms_template(
    notification_router_v3, yasms, notification_details, sms_transport, notification_type
):
    await notification_router_v3.send_notification(
        transport=sms_transport,
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    assert yasms.send_sms.call_args[1]["text"] == (
        "Новая заявка в Яндекс.Бизнесе http://details.link"
    )


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
async def test_returns_effective_phone_if_sms_sent(
    notification_router_v3, notification_details, sms_transport, notification_type
):
    result = await notification_router_v3.send_notification(
        transport=sms_transport,
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    assert result == [
        {Transport.SMS: {"phone": 123456789}},
        {Transport.SMS: {"phone": 987654321}},
    ]


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
async def test_does_not_try_to_send_sms_if_no_phone_available(
    notification_router_v3, yasms, notification_details, notification_type
):

    await notification_router_v3.send_notification(
        transport=dict(type=Transport.SMS),
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    yasms.send_sms.assert_not_called()


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
async def test_returns_error_if_no_phone_available(
    notification_router_v3, notification_details, notification_type
):
    result = await notification_router_v3.send_notification(
        transport=dict(type=Transport.SMS),
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    assert result == [{Transport.SMS: Any(NoAddress)}]


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
async def test_returns_error_if_yasms_client_failed(
    notification_router_v3, yasms, notification_details, sms_transport, notification_type
):
    yasms.send_sms.coro.side_effect = Exception

    result = await notification_router_v3.send_notification(
        transport=sms_transport,
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    assert result == [{Transport.SMS: Any(SendFailed)}, {Transport.SMS: Any(SendFailed)}]


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
        {"value": {"PHONE_RECIPIENTS_WHITELIST": ""}},
        {"value": {"PHONE_RECIPIENTS_WHITELIST": "999999"}},
        {"value": {"PHONE_RECIPIENTS_WHITELIST": "222222,999999"}},
        {"value": {"PHONE_RECIPIENTS_WHITELIST": "1111119"}},
    ],
)
async def test_returns_error_for_not_allowed_recipient_if_recipients_limited(
    notification_router_v3, yasms, yav_client, notification_details, secret, sms_transport, notification_type
):
    yav_client.retrieve_secret_head.coro.return_value = secret

    result = await notification_router_v3.send_notification(
        transport=sms_transport,
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    assert result == [{Transport.SMS: Any(AddressNotAllowed)}, {Transport.SMS: Any(AddressNotAllowed)}]
    yasms.send_sms.assert_not_called()


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
    notification_router_v3, yasms, yav_client, notification_details, whitelist, sms_transport, notification_type
):
    yav_client.retrieve_secret_head.coro.return_value = {
        "value": {"PHONE_RECIPIENTS_WHITELIST": whitelist}
    }

    result = await notification_router_v3.send_notification(
        transport=sms_transport,
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    assert result == [{Transport.SMS: Any(ValueError)}, {Transport.SMS: Any(ValueError)}]

    yasms.send_sms.assert_not_called()


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
@pytest.mark.config(LIMIT_RECIPIENTS=True)
@pytest.mark.parametrize(
    "whitelist", ["123456789", "123456789,999999", "999999,123456789", "  123456789  , 999999"]
)
async def test_sends_sms_for_allowed_recipient_if_recipients_limited(
    notification_router_v3, yasms, yav_client, notification_details, whitelist, sms_transport, notification_type
):
    yav_client.retrieve_secret_head.coro.return_value = {
        "value": {"PHONE_RECIPIENTS_WHITELIST": whitelist}
    }

    await notification_router_v3.send_notification(
        transport=sms_transport,
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    assert yasms.send_sms.call_args[1]["phone"] == 123456789
