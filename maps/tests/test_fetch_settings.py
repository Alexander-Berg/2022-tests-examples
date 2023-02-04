import pytest
from aiohttp.web import Response
from smb.common.http_client import UnknownResponse

from maps_adv.geosmb.tuner.client import (
    BadRequest,
    BookingSettings,
    BusinessEmailNotificationSettings,
    BusinessSmsNotificationSettings,
    OnlineBookingSettings,
    RequestsSettings,
    UnknownBizId,
)
from maps_adv.geosmb.tuner.proto import errors_pb2, settings_pb2

pytestmark = [pytest.mark.asyncio]


def make_response(**overrides) -> bytes:
    settings_params = dict(
        emails=["kek@yandex.ru", "foo@gmail.com"],
        phone=900,
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

    response_pb = settings_pb2.Settings(**settings_params)

    return response_pb.SerializeToString()


async def test_sends_correct_request(client, mock_fetch_settings):
    request_path = None
    request_body = None

    async def _handler(request):
        nonlocal request_path, request_body
        request_path = request.path
        request_body = await request.read()
        return Response(status=200, body=make_response())

    mock_fetch_settings(_handler)

    await client.fetch_settings(biz_id=123)

    assert request_path == "/v1/fetch_settings/"
    assert request_body == settings_pb2.FetchSettings(biz_id=123).SerializeToString()


async def test_returns_settings(client, mock_fetch_settings):
    mock_fetch_settings(lambda _: Response(status=200, body=make_response()))

    got = await client.fetch_settings(biz_id=123)

    assert got == {
        "emails": ["kek@yandex.ru", "foo@gmail.com"],
        "phone": 900,
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


@pytest.mark.parametrize(
    "pb_error_code, expected_exception",
    [
        (errors_pb2.Error.UNKNOWN_BIZ_ID, UnknownBizId),
        (errors_pb2.Error.VALIDATION_ERROR, BadRequest),
    ],
)
async def test_raises_for_known_exceptions(
    pb_error_code, expected_exception, client, mock_fetch_settings
):
    mock_fetch_settings(
        lambda _: Response(
            status=400,
            body=errors_pb2.Error(
                code=pb_error_code, description="Something went wrong."
            ).SerializeToString(),
        )
    )

    with pytest.raises(expected_exception, match="Something went wrong."):
        await client.fetch_settings(biz_id=123)


async def test_raises_for_unknown_exceptions(client, mock_fetch_settings):
    mock_fetch_settings(lambda _: Response(status=409))

    with pytest.raises(UnknownResponse):
        await client.fetch_settings(biz_id=123)
