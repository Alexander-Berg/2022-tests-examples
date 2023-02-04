from copy import deepcopy

import pytest

from maps_adv.common.helpers import Any
from maps_adv.geosmb.telegraphist.server.lib.enums import NotificationType, Transport
from maps_adv.geosmb.telegraphist.server.lib.notification_router import (
    AddressNotAllowed,
    NoAddress,
    NotificationRouter,
    SendFailed,
    supported_notifications,
)
from maps_adv.geosmb.telegraphist.server.tests.helpers import UncopiedMock

pytestmark = [pytest.mark.asyncio]


async def test_uses_email_client_to_send_message(
    notification_router, email_client, notification_details
):
    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111},
        transports=[Transport.EMAIL],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    email_client.send_message.assert_called_with(
        args=Any(dict),
        template_code=Any(str),
        to_email=Any(str),
        subject=Any(str),
        from_email=Any(str),
        from_name=Any(str),
    )


async def test_uses_provided_email(
    notification_router, email_client, doorman, notification_details
):
    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111, "email": "provided@yandex.ru"},
        transports=[Transport.EMAIL],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    assert (
        email_client.send_message.call_args.kwargs["to_email"] == "provided@yandex.ru"
    )


async def test_uses_email_from_doorman_if_not_provided(
    notification_router, email_client, doorman, notification_details
):
    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111},
        transports=[Transport.EMAIL],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    assert email_client.send_message.call_args.kwargs["to_email"] == "email@yandex.ru"


@pytest.mark.parametrize(
    ("notification_type", "expected_email_template"),
    [
        (NotificationType.ORDER_CREATED, "order_created_email"),
        (NotificationType.ORDER_REMINDER, "order_reminder_email"),
        (NotificationType.ORDER_CHANGED, "order_changed_email"),
        (NotificationType.ORDER_CANCELLED, "order_cancelled_email"),
    ],
)
async def test_uses_appropriate_email_template(
    notification_router,
    email_client,
    notification_details,
    notification_type,
    expected_email_template,
):
    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111},
        transports=[Transport.EMAIL],
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    assert (
        email_client.send_message.call_args.kwargs["template_code"]
        == expected_email_template
    )


async def test_uses_context_processor(
    mocker,
    doorman,
    email_client,
    yasms,
    yav_client,
    sup_client,
    email_template_codes,
    tuner_client,
    notification_details,
):
    orig = deepcopy(supported_notifications)
    orig["client"][NotificationType.ORDER_CHANGED][Transport.EMAIL][
        "context_processor"
    ] = UncopiedMock(return_value={"org": {"name": "Мистер Отправитель"}})
    mock = mocker.patch(
        "maps_adv.geosmb.telegraphist.server.lib.notification_router.supported_notifications",  # noqa
        orig,
    )

    notification_router = NotificationRouter(
        doorman_client=doorman,
        email_client=email_client,
        yasms=yasms,
        yav_client=yav_client,
        sup_client=sup_client,
        tuner_client=tuner_client,
        email_template_codes=email_template_codes,
        yav_secret_id="kek-id",
    )

    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111},
        transports=[Transport.EMAIL],
        notification_type=NotificationType.ORDER_CHANGED,
        notification_details=notification_details[NotificationType.ORDER_CHANGED],
    )

    mock["client"][NotificationType.ORDER_CHANGED][Transport.EMAIL][
        "context_processor"
    ].assert_called()
    assert email_client.send_message.call_args.kwargs["args"] == {
        "org": {"name": "Мистер Отправитель"}
    }


async def test_uses_subject_generator(
    mocker,
    doorman,
    email_client,
    yasms,
    yav_client,
    tuner_client,
    sup_client,
    email_template_codes,
    notification_details,
    notification_router,
):
    orig = deepcopy(supported_notifications)
    orig["client"][NotificationType.ORDER_CHANGED][Transport.EMAIL][
        "subject_generator"
    ] = UncopiedMock(return_value="Тема письма")
    mock = mocker.patch(
        "maps_adv.geosmb.telegraphist.server.lib.notification_router.supported_notifications",  # noqa
        orig,
    )

    notification_router = NotificationRouter(
        doorman_client=doorman,
        email_client=email_client,
        yasms=yasms,
        yav_client=yav_client,
        sup_client=sup_client,
        tuner_client=tuner_client,
        email_template_codes=email_template_codes,
        yav_secret_id="kek-id",
    )

    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111},
        transports=[Transport.EMAIL],
        notification_type=NotificationType.ORDER_CHANGED,
        notification_details=notification_details[NotificationType.ORDER_CHANGED],
    )

    mock["client"][NotificationType.ORDER_CHANGED][Transport.EMAIL][
        "subject_generator"
    ].assert_called()
    assert email_client.send_message.call_args.kwargs["subject"] == "Тема письма"


async def test_uses_from_generator(
    mocker,
    doorman,
    email_client,
    yasms,
    yav_client,
    sup_client,
    email_template_codes,
    tuner_client,
    notification_details,
    notification_router,
):
    orig = deepcopy(supported_notifications)
    orig["client"][NotificationType.ORDER_CHANGED][Transport.EMAIL][
        "from_generator"
    ] = UncopiedMock(return_value=("Мистер Отправитель", "mister_otpravitel@yandex.ru"))
    mock = mocker.patch(
        "maps_adv.geosmb.telegraphist.server.lib.notification_router.supported_notifications",  # noqa
        orig,
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

    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111},
        transports=[Transport.EMAIL],
        notification_type=NotificationType.ORDER_CHANGED,
        notification_details=notification_details[NotificationType.ORDER_CHANGED],
    )

    mock["client"][NotificationType.ORDER_CHANGED][Transport.EMAIL][
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
    ("explicit_email", "expected_email"),
    [(None, "email@yandex.ru"), ("provided@yandex.ru", "provided@yandex.ru")],
)
async def test_returns_effective_email_if_message_sent(
    notification_router, notification_details, explicit_email, expected_email
):
    recipient = {"biz_id": 123, "client_id": 111}
    if explicit_email is not None:
        recipient["email"] = explicit_email

    result = await notification_router.send_client_notification(
        recipient=recipient,
        transports=[Transport.EMAIL],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    assert result == [{Transport.EMAIL: {"email": expected_email}}]


async def test_does_not_try_to_send_message_if_no_email_available(
    notification_router, email_client, doorman, notification_details
):
    del doorman.retrieve_client.coro.return_value["email"]

    await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111},
        transports=[Transport.EMAIL],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    email_client.send_message.assert_not_called()


async def test_returns_error_if_no_email_available(
    notification_router, doorman, notification_details
):
    del doorman.retrieve_client.coro.return_value["email"]

    result = await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111},
        transports=[Transport.EMAIL],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    assert result == [{Transport.EMAIL: Any(NoAddress)}]


async def test_returns_error_if_email_client_failed(
    notification_router, email_client, notification_details
):
    email_client.send_message.coro.side_effect = Exception

    result = await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111, "email": "provided@yandex.ru"},
        transports=[Transport.EMAIL],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    assert result == [{Transport.EMAIL: Any(SendFailed)}]


async def test_returns_error_if_recipients_limited(
    doorman,
    email_client,
    yasms,
    yav_client,
    sup_client,
    notification_details,
    email_template_codes,
    tuner_client,
):
    notification_router = NotificationRouter(
        doorman_client=doorman,
        email_client=email_client,
        yasms=yasms,
        yav_client=yav_client,
        sup_client=sup_client,
        tuner_client=tuner_client,
        email_template_codes=email_template_codes,
        limit_recipients=True,
        yav_secret_id="kek-id",
    )

    result = await notification_router.send_client_notification(
        recipient={"biz_id": 123, "client_id": 111, "email": "provided@yandex.ru"},
        transports=[Transport.EMAIL],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    assert result == [{Transport.EMAIL: Any(AddressNotAllowed)}]
    email_client.send_message.assert_not_called()


@pytest.mark.parametrize(
    ("explicit_email", "expected_email"),
    [
        (None, "email@yandex-team.ru"),
        ("another_email@yandex-team.ru", "another_email@yandex-team.ru"),
    ],
)
async def test_sends_ok_to_yandex_team_if_recipients_limited(
    doorman,
    email_client,
    yasms,
    yav_client,
    sup_client,
    notification_details,
    email_template_codes,
    tuner_client,
    explicit_email,
    expected_email,
):
    doorman.retrieve_client.coro.return_value["email"] = "email@yandex-team.ru"

    notification_router = NotificationRouter(
        doorman_client=doorman,
        email_client=email_client,
        yasms=yasms,
        yav_client=yav_client,
        sup_client=sup_client,
        email_template_codes=email_template_codes,
        tuner_client=tuner_client,
        limit_recipients=True,
        yav_secret_id="kek-id",
    )
    recipient = {"biz_id": 123, "client_id": 111}
    if explicit_email is not None:
        recipient["email"] = explicit_email

    result = await notification_router.send_client_notification(
        recipient=recipient,
        transports=[Transport.EMAIL],
        notification_type=NotificationType.ORDER_CREATED,
        notification_details=notification_details[NotificationType.ORDER_CREATED],
    )

    assert result == [{Transport.EMAIL: {"email": expected_email}}]
    assert email_client.send_message.call_args.kwargs["to_email"] == expected_email
