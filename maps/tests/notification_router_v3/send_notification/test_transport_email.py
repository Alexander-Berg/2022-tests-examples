from copy import deepcopy
from unittest import mock

import pytest

from maps_adv.common.helpers import Any
from maps_adv.geosmb.telegraphist.server.lib.enums import NotificationType, Transport
from maps_adv.geosmb.telegraphist.server.lib.exceptions import (
    AddressNotAllowed,
    NoAddress,
    SendFailed,
)
from maps_adv.geosmb.telegraphist.server.lib.notification_router_v3 import (
    NotificationRouterV3,
    supported_notifications,
)
from maps_adv.geosmb.telegraphist.server.tests.helpers import UncopiedMock

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
async def test_sends_emails(
    notification_router_v3, email_client, notification_details, email_transport, notification_type
):
    await notification_router_v3.send_notification(
        transport=email_transport,
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    email_client.send_message.assert_has_calls(
        [
            mock.call(
                args=Any(dict),
                template_code=Any(str),
                to_email="foo@yandex-team.ru",
                subject=Any(str),
                from_email=Any(str),
                from_name=Any(str),
            ),
            mock.call(
                args=Any(dict),
                template_code=Any(str),
                to_email="kek@yandex.ru",
                subject=Any(str),
                from_email=Any(str),
                from_name=Any(str),
            ),
        ],
        any_order=True,
    )


@pytest.mark.parametrize(
    ("notification_type", "expected_email_template"),
    [
        (
            NotificationType.REQUEST_CREATED_FOR_BUSINESS,
            "request_created_email_for_business",
        ),
        (
            NotificationType.CART_ORDER_CREATED,
            "cart_order_created",
        )
    ],
)
async def test_uses_appropriate_email_template_for_business_emails(
    notification_router_v3,
    email_client,
    notification_details,
    notification_type,
    expected_email_template,
    email_transport,
):
    await notification_router_v3.send_notification(
        transport=email_transport,
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    assert (
        email_client.send_message.call_args.kwargs["template_code"]
        == expected_email_template
    )


@pytest.mark.parametrize(
    'notification_type, mock_data', [
        (NotificationType.REQUEST_CREATED_FOR_BUSINESS, {"org": {"name": "Мистер Отправитель"}}),
        (NotificationType.CART_ORDER_CREATED, {"landing": {"domain": "landing.site"}})
    ]
)
async def test_uses_context_processor(
    mocker,
    email_client,
    yasms,
    yav_client,
    notify_me,
    email_template_codes,
    notification_details,
    email_transport,
    notification_type,
    mock_data
):
    orig = deepcopy(supported_notifications)
    orig[notification_type][Transport.EMAIL][
        "context_processor"
    ] = UncopiedMock(return_value=mock_data)
    mock = mocker.patch(
        "maps_adv.geosmb.telegraphist.server.lib.notification_router_v3.supported_notifications",  # noqa
        orig,
    )

    notification_router_v3 = NotificationRouterV3(
        email_client=email_client,
        yasms=yasms,
        email_template_codes=email_template_codes,
        yav_client=yav_client,
        telegram_client=notify_me,
        yav_secret_id="kek-id",
        limit_recipients=True,
    )

    await notification_router_v3.send_notification(
        transport=email_transport,
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    mock[notification_type][Transport.EMAIL][
        "context_processor"
    ].assert_called()
    assert email_client.send_message.call_args.kwargs["args"] == mock_data


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
async def test_uses_subject_generator(
    mocker,
    email_client,
    yasms,
    notify_me,
    yav_client,
    email_template_codes,
    notification_details,
    email_transport,
    notification_type
):

    orig = deepcopy(supported_notifications)
    orig[notification_type][Transport.EMAIL][
        "subject_generator"
    ] = UncopiedMock(return_value="Тема письма")
    mock = mocker.patch(
        "maps_adv.geosmb.telegraphist.server.lib.notification_router_v3.supported_notifications",  # noqa
        orig,
    )

    notification_router_v3 = NotificationRouterV3(
        email_client=email_client,
        yasms=yasms,
        telegram_client=notify_me,
        email_template_codes=email_template_codes,
        yav_client=yav_client,
        yav_secret_id="kek-id",
        limit_recipients=True,
    )

    await notification_router_v3.send_notification(
        transport=email_transport,
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    mock[notification_type][Transport.EMAIL][
        "subject_generator"
    ].assert_called()
    assert email_client.send_message.call_args.kwargs["subject"] == "Тема письма"


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
async def test_uses_from_generator(
    mocker,
    email_client,
    yasms,
    notify_me,
    yav_client,
    email_template_codes,
    notification_router_v3,
    notification_details,
    email_transport,
    notification_type
):
    orig = deepcopy(supported_notifications)
    orig[notification_type][Transport.EMAIL][
        "from_generator"
    ] = UncopiedMock(return_value=("Мистер Отправитель", "mister_otpravitel@yandex.ru"))
    mock = mocker.patch(
        "maps_adv.geosmb.telegraphist.server.lib.notification_router_v3.supported_notifications",  # noqa
        orig,
    )

    notification_router_v3 = NotificationRouterV3(
        email_client=email_client,
        yasms=yasms,
        telegram_client=notify_me,
        email_template_codes=email_template_codes,
        yav_client=yav_client,
        yav_secret_id="kek-id",
        limit_recipients=True,
    )

    await notification_router_v3.send_notification(
        transport=email_transport,
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    mock[notification_type][Transport.EMAIL][
        "from_generator"
    ].assert_called()
    assert (
        email_client.send_message.call_args.kwargs["from_email"]
        == "mister_otpravitel@yandex.ru"
    )
    assert (
        email_client.send_message.call_args.kwargs["from_name"] == "Мистер Отправитель"
    )


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
async def test_returns_effective_email_if_message_sent(
    notification_router_v3, notification_details, email_transport, notification_type
):
    result = await notification_router_v3.send_notification(
        transport=email_transport,
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    assert result == [
        {Transport.EMAIL: {"email": "foo@yandex-team.ru"}},
        {Transport.EMAIL: {"email": "kek@yandex.ru"}},
    ]


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
async def test_does_not_try_to_send_message_if_no_email_available(
    notification_router_v3, email_client, notification_details, notification_type
):
    await notification_router_v3.send_notification(
        transport=dict(type=Transport.EMAIL),
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    email_client.send_message.assert_not_called()


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
async def test_returns_error_if_no_email_available(
    notification_router_v3, notification_details, notification_type
):
    result = await notification_router_v3.send_notification(
        transport=dict(type=Transport.EMAIL),
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    assert result == [{Transport.EMAIL: Any(NoAddress)}]


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
async def test_returns_error_if_email_client_failed(
    notification_router_v3, email_client, notification_details, email_transport, notification_type
):
    email_client.send_message.coro.side_effect = Exception

    result = await notification_router_v3.send_notification(
        transport=email_transport,
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    assert result == [
        {Transport.EMAIL: Any(SendFailed)},
        {Transport.EMAIL: Any(SendFailed)},
    ]


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
async def test_returns_error_if_recipients_limited(
    email_client,
    yasms,
    notify_me,
    yav_client,
    notification_details,
    email_template_codes,
    email_transport,
    notification_type
):
    notification_router_v3 = NotificationRouterV3(
        email_client=email_client,
        yasms=yasms,
        telegram_client=notify_me,
        email_template_codes=email_template_codes,
        yav_client=yav_client,
        yav_secret_id="kek-id",
        limit_recipients=True,
    )

    result = await notification_router_v3.send_notification(
        transport=email_transport,
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    assert result == [
        {Transport.EMAIL: {"email": "foo@yandex-team.ru"}},
        {Transport.EMAIL: Any(AddressNotAllowed)},
    ]
    assert email_client.send_message.call_count == 1


@pytest.mark.parametrize(
    'notification_type', [
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
        NotificationType.CART_ORDER_CREATED
    ]
)
async def test_sends_ok_to_yandex_team_if_recipients_limited(
    email_client,
    yasms,
    notify_me,
    yav_client,
    notification_details,
    email_template_codes,
    notification_type
):
    notification_router_v3 = NotificationRouterV3(
        email_client=email_client,
        yasms=yasms,
        telegram_client=notify_me,
        email_template_codes=email_template_codes,
        yav_client=yav_client,
        yav_secret_id="kek-id",
        limit_recipients=True,
    )

    result = await notification_router_v3.send_notification(
        transport=dict(
            type=Transport.EMAIL,
            emails=["email@yandex-team.ru"]
        ),
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    assert result == [{Transport.EMAIL: {"email": "email@yandex-team.ru"}}]
    assert (
        email_client.send_message.call_args.kwargs["to_email"] == "email@yandex-team.ru"
    )
