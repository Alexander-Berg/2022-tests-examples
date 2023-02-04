from copy import deepcopy
from unittest import mock

import pytest

from maps_adv.common.helpers import Any
from maps_adv.geosmb.telegraphist.server.lib.enums import NotificationType, Transport
from maps_adv.geosmb.telegraphist.server.lib.notification_router import (
    AddressNotAllowed,
    NoAddress,
    NotificationDisabledInSettings,
    NotificationRouter,
    SendFailed,
    supported_notifications,
)
from maps_adv.geosmb.telegraphist.server.tests.helpers import UncopiedMock
from maps_adv.geosmb.tuner.client import BusinessEmailNotificationSettings

pytestmark = [pytest.mark.asyncio]


async def test_sends_emails_to_all_business_emails(
    notification_router, email_client, notification_details
):
    await notification_router.send_business_notification(
        recipient={"biz_id": 123},
        transports=[Transport.EMAIL],
        notification_type=NotificationType.CERTIFICATE_CREATED,
        notification_details=notification_details[NotificationType.CERTIFICATE_CREATED],
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
            NotificationType.ORDER_CREATED_FOR_BUSINESS,
            "order_created_email_for_business",
        ),
        (
            NotificationType.ORDER_CHANGED_FOR_BUSINESS,
            "order_changed_email_for_business",
        ),
        (
            NotificationType.ORDER_CANCELLED_FOR_BUSINESS,
            "order_cancelled_email_for_business",
        ),
        (NotificationType.CERTIFICATE_EXPIRING, "certificate_expiring"),
        (NotificationType.CERTIFICATE_EXPIRED, "certificate_expired"),
        (NotificationType.CERTIFICATE_REJECTED, "certificate_rejected"),
        (NotificationType.CERTIFICATE_CONNECT_PAYMENT, "certificate_connect_payment"),
        (NotificationType.FIRST_CERTIFICATE_APPROVED, "first_certificate_approved"),
        (
            NotificationType.SUBSEQUENT_CERTIFICATE_APPROVED,
            "subsequent_certificate_approved",
        ),
        (NotificationType.CERTIFICATE_PURCHASED, "certificate_purchased"),
        (NotificationType.CERTIFICATE_CREATED, "certificate_created"),
        (
            NotificationType.REQUEST_CREATED_FOR_BUSINESS,
            "request_created_email_for_business",
        ),
    ],
)
async def test_uses_appropriate_email_template_for_business_emails(
    notification_router,
    email_client,
    notification_details,
    notification_type,
    expected_email_template,
):
    await notification_router.send_business_notification(
        recipient={"biz_id": 123},
        transports=[Transport.EMAIL],
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    assert (
        email_client.send_message.call_args.kwargs["template_code"]
        == expected_email_template
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
        NotificationType.REQUEST_CREATED_FOR_BUSINESS,
    ],
)
async def test_does_not_send_email_if_notification_disabled_in_settings(
    notification_router,
    email_client,
    notification_details,
    notification_type,
    tuner_client,
):
    tuner_client.fetch_settings.coro.return_value[
        "notifications"
    ] = BusinessEmailNotificationSettings(
        order_created=False,
        order_cancelled=False,
        order_changed=False,
        certificate_notifications=False,
        request_created=False,
    )

    result = await notification_router.send_business_notification(
        recipient={"biz_id": 123},
        transports=[Transport.EMAIL],
        notification_type=notification_type,
        notification_details=notification_details[notification_type],
    )

    assert result == [{Transport.EMAIL: Any(NotificationDisabledInSettings)}]


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
    orig["business"][NotificationType.CERTIFICATE_CREATED][Transport.EMAIL][
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

    await notification_router.send_business_notification(
        recipient={"biz_id": 123},
        transports=[Transport.EMAIL],
        notification_type=NotificationType.CERTIFICATE_CREATED,
        notification_details=notification_details[NotificationType.CERTIFICATE_CREATED],
    )

    mock["business"][NotificationType.CERTIFICATE_CREATED][Transport.EMAIL][
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
    orig["business"][NotificationType.CERTIFICATE_CREATED][Transport.EMAIL][
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

    await notification_router.send_business_notification(
        recipient={"biz_id": 123},
        transports=[Transport.EMAIL],
        notification_type=NotificationType.CERTIFICATE_CREATED,
        notification_details=notification_details[NotificationType.CERTIFICATE_CREATED],
    )

    mock["business"][NotificationType.CERTIFICATE_CREATED][Transport.EMAIL][
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
):
    orig = deepcopy(supported_notifications)
    orig["business"][NotificationType.CERTIFICATE_CREATED][Transport.EMAIL][
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

    await notification_router.send_business_notification(
        recipient={"biz_id": 123},
        transports=[Transport.EMAIL],
        notification_type=NotificationType.CERTIFICATE_CREATED,
        notification_details=notification_details[NotificationType.CERTIFICATE_CREATED],
    )

    mock["business"][NotificationType.CERTIFICATE_CREATED][Transport.EMAIL][
        "from_generator"
    ].assert_called()
    assert (
        email_client.send_message.call_args.kwargs["from_email"]
        == "mister_otpravitel@yandex.ru"
    )
    assert (
        email_client.send_message.call_args.kwargs["from_name"] == "Мистер Отправитель"
    )


async def test_returns_effective_email_if_message_sent(
    notification_router, notification_details
):
    result = await notification_router.send_business_notification(
        recipient={"biz_id": 123},
        transports=[Transport.EMAIL],
        notification_type=NotificationType.CERTIFICATE_CREATED,
        notification_details=notification_details[NotificationType.CERTIFICATE_CREATED],
    )

    assert result == [
        {Transport.EMAIL: {"email": "kek@yandex.ru"}},
        {Transport.EMAIL: {"email": "foo@yandex-team.ru"}},
    ]


async def test_does_not_try_to_send_message_if_no_email_available(
    notification_router, email_client, tuner_client, notification_details
):
    tuner_client.fetch_settings.coro.return_value["emails"] = []

    await notification_router.send_business_notification(
        recipient={"biz_id": 123},
        transports=[Transport.EMAIL],
        notification_type=NotificationType.CERTIFICATE_CREATED,
        notification_details=notification_details[NotificationType.CERTIFICATE_CREATED],
    )

    email_client.send_message.assert_not_called()


async def test_returns_error_if_no_email_available(
    notification_router, tuner_client, notification_details
):
    tuner_client.fetch_settings.coro.return_value["emails"] = []

    result = await notification_router.send_business_notification(
        recipient={"biz_id": 123},
        transports=[Transport.EMAIL],
        notification_type=NotificationType.CERTIFICATE_CREATED,
        notification_details=notification_details[NotificationType.CERTIFICATE_CREATED],
    )

    assert result == [{Transport.EMAIL: Any(NoAddress)}]


async def test_returns_error_if_email_client_failed(
    notification_router, email_client, notification_details
):
    email_client.send_message.coro.side_effect = Exception

    result = await notification_router.send_business_notification(
        recipient={"biz_id": 123},
        transports=[Transport.EMAIL],
        notification_type=NotificationType.CERTIFICATE_CREATED,
        notification_details=notification_details[NotificationType.CERTIFICATE_CREATED],
    )

    assert result == [
        {Transport.EMAIL: Any(SendFailed)},
        {Transport.EMAIL: Any(SendFailed)},
    ]


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

    result = await notification_router.send_business_notification(
        recipient={"biz_id": 123},
        transports=[Transport.EMAIL],
        notification_type=NotificationType.CERTIFICATE_CREATED,
        notification_details=notification_details[NotificationType.CERTIFICATE_CREATED],
    )

    assert result == [
        {Transport.EMAIL: Any(AddressNotAllowed)},
        {Transport.EMAIL: {"email": "foo@yandex-team.ru"}},
    ]
    assert email_client.send_message.call_count == 1


async def test_sends_ok_to_yandex_team_if_recipients_limited(
    doorman,
    email_client,
    yasms,
    yav_client,
    sup_client,
    notification_details,
    email_template_codes,
    tuner_client,
):
    tuner_client.fetch_settings.coro.return_value["emails"] = ["email@yandex-team.ru"]

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

    result = await notification_router.send_business_notification(
        recipient={"biz_id": 123},
        transports=[Transport.EMAIL],
        notification_type=NotificationType.CERTIFICATE_CREATED,
        notification_details=notification_details[NotificationType.CERTIFICATE_CREATED],
    )

    assert result == [{Transport.EMAIL: {"email": "email@yandex-team.ru"}}]
    assert (
        email_client.send_message.call_args.kwargs["to_email"] == "email@yandex-team.ru"
    )
