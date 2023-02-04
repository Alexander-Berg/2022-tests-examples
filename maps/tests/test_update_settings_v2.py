import pytest
from aiohttp.web import Response
from smb.common.http_client import UnknownResponse

from maps_adv.geosmb.tuner.client import (
    BadParams,
    BadRequest,
    BookingSettings,
    BusinessEmailNotificationSettings,
    BusinessSmsNotificationSettings,
    Contacts,
    OnlineBookingSettings,
    RequestsSettings,
    UnknownBizId,
)
from maps_adv.geosmb.tuner.proto import errors_pb2, settings_pb2

pytestmark = [pytest.mark.asyncio]


request_params = dict()


def make_response(**overrides) -> bytes:
    settings_params = dict(
        contacts=settings_pb2.Contacts(
            emails=["kek@yandex.ru"],
            phones=[900],
            telegram_logins=["test", "foo"],
        ),
        notifications=settings_pb2.EmailNotifications(
            order_created=True,
            order_cancelled=True,
            order_changed=True,
            certificate_notifications=True,
            request_created=True,
        ),
        sms_notifications=settings_pb2.SmsNotifications(
            request_created=True,
        ),
        booking=settings_pb2.Booking(
            enabled=True,
            slot_interval=30,
            online_settings=settings_pb2.OnlineBookingSettings(
                export_to_ya_services=True
            ),
        ),
        requests=settings_pb2.Requests(
            enabled=True,
            button_text="Отправить заявку",
        ),
    )
    settings_params.update(overrides)

    response_pb = settings_pb2.SettingsV2(**settings_params)

    return response_pb.SerializeToString()


@pytest.mark.parametrize(
    "booking_settings, expected_booking_pb, requests_settings, expected_requests_pb",
    [
        (
            BookingSettings(),
            dict(
                booking=settings_pb2.Booking(
                    enabled=True,
                    slot_interval=30,
                    online_settings=settings_pb2.OnlineBookingSettings(
                        export_to_ya_services=False
                    ),
                )
            ),
            RequestsSettings(),
            dict(
                requests=settings_pb2.Requests(
                    enabled=True,
                    button_text="Отправить заявку",
                ),
            ),
        ),
        (None, dict(), None, dict()),
    ],
)
async def test_sends_correct_request(
    booking_settings,
    expected_booking_pb,
    requests_settings,
    expected_requests_pb,
    client,
    mock_update_settings_v2,
):
    request_path = None
    request_body = None

    async def _handler(request):
        nonlocal request_path, request_body
        request_path = request.path
        request_body = await request.read()
        return Response(status=200, body=make_response())

    mock_update_settings_v2(_handler)

    await client.update_settings_v2(
        biz_id=123,
        contacts=Contacts(
            emails=["kek@yandex.ru"],
            phones=[900],
            telegram_logins=["test", "foo"],
        ),
        notification_settings=BusinessEmailNotificationSettings(),
        sms_notification_settings=BusinessSmsNotificationSettings(),
        booking_settings=booking_settings,
        requests_settings=requests_settings,
    )

    assert request_path == "/v2/update_settings/"
    request_pb = settings_pb2.UpdateSettingsV2.FromString(request_body)
    assert request_pb == settings_pb2.UpdateSettingsV2(
        biz_id=123,
        settings=settings_pb2.SettingsV2(
            contacts=settings_pb2.Contacts(
                emails=["kek@yandex.ru"],
                phones=[900],
                telegram_logins=["test", "foo"],
            ),
            notifications=settings_pb2.EmailNotifications(
                order_created=True,
                order_cancelled=True,
                order_changed=True,
                certificate_notifications=True,
                request_created=True,
            ),
            sms_notifications=settings_pb2.SmsNotifications(
                request_created=True,
            ),
            **expected_booking_pb,
            **expected_requests_pb,
        ),
    )


async def test_returns_updated_settings(client, mock_update_settings_v2):
    mock_update_settings_v2(lambda _: Response(status=200, body=make_response()))

    got = await client.update_settings_v2(
        biz_id=123,
        contacts=Contacts(
            emails=["kek@yandex.ru"],
            phones=[900],
            telegram_logins=["test", "foo"],
        ),
        notification_settings=BusinessEmailNotificationSettings(),
        sms_notification_settings=BusinessSmsNotificationSettings(),
        booking_settings=BookingSettings(),
        requests_settings=RequestsSettings(),
    )

    assert got == {
        "contacts": Contacts(
            emails=["kek@yandex.ru"],
            phones=[900],
            telegram_logins=["test", "foo"],
        ),
        "notifications": BusinessEmailNotificationSettings(
            order_created=True,
            order_cancelled=True,
            order_changed=True,
            certificate_notifications=True,
            request_created=True,
        ),
        "sms_notifications": BusinessSmsNotificationSettings(
            request_created=True,
        ),
        "booking": BookingSettings(
            enabled=True,
            slot_interval=30,
            online_settings=OnlineBookingSettings(export_to_ya_services=True),
        ),
        "requests": RequestsSettings(
            enabled=True,
            button_text="Отправить заявку",
        ),
    }


async def test_raises_if_no_contacts_set(client, mock_update_settings_v2):
    with pytest.raises(BadParams, match="Emails list can't be empty."):
        await client.update_settings_v2(
            biz_id=123,
            contacts=None,
            notification_settings=BusinessEmailNotificationSettings(),
            sms_notification_settings=BusinessSmsNotificationSettings(),
            booking_settings=BookingSettings(),
            requests_settings=RequestsSettings(),
        )


async def test_raises_if_no_emails_set(client, mock_update_settings_v2):
    with pytest.raises(BadParams, match="Emails list can't be empty."):
        await client.update_settings_v2(
            biz_id=123,
            contacts=Contacts(
                emails=[],
                phones=[900],
                telegram_logins=["test", "foo"],
            ),
            notification_settings=BusinessEmailNotificationSettings(),
            sms_notification_settings=BusinessSmsNotificationSettings(),
            booking_settings=BookingSettings(),
            requests_settings=RequestsSettings(),
        )


@pytest.mark.parametrize(
    "pb_error_code, expected_exception",
    [
        (errors_pb2.Error.UNKNOWN_BIZ_ID, UnknownBizId),
        (errors_pb2.Error.VALIDATION_ERROR, BadRequest),
    ],
)
async def test_raises_for_known_exceptions(
    pb_error_code, expected_exception, client, mock_update_settings_v2
):
    mock_update_settings_v2(
        lambda _: Response(
            status=400,
            body=errors_pb2.Error(
                code=pb_error_code, description="Something went wrong."
            ).SerializeToString(),
        )
    )

    with pytest.raises(expected_exception, match="Something went wrong."):
        await client.update_settings_v2(
            biz_id=123,
            contacts=Contacts(
                emails=["kek@yandex.ru"],
                phones=[900],
                telegram_logins=["test", "foo"],
            ),
            notification_settings=BusinessEmailNotificationSettings(),
            sms_notification_settings=BusinessSmsNotificationSettings(),
            booking_settings=BookingSettings(),
            requests_settings=RequestsSettings(),
        )


async def test_raises_for_unknown_exceptions(client, mock_fetch_settings_v2):
    mock_fetch_settings_v2(lambda _: Response(status=409))

    with pytest.raises(UnknownResponse):
        await client.fetch_settings_v2(biz_id=123)
