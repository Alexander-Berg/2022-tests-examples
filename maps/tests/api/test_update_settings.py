import pytest
from smb.common.testing_utils import Any, dt

from maps_adv.geosmb.tuner.proto.errors_pb2 import Error
from maps_adv.geosmb.tuner.proto.settings_pb2 import (
    Booking,
    EmailNotifications,
    OnlineBookingSettings,
    Requests,
    Settings,
    SmsNotifications,
    UpdateSettings,
)

pytestmark = [pytest.mark.asyncio]

URL = "/v1/update_settings/"


@pytest.mark.parametrize(
    "booking, expected_booking, requests, expected_requests, phone",
    [
        (
            Booking(
                enabled=True,
                slot_interval=20,
                online_settings=OnlineBookingSettings(export_to_ya_services=False),
            ),
            Booking(
                enabled=True,
                slot_interval=20,
                online_settings=OnlineBookingSettings(export_to_ya_services=False),
            ),
            Requests(enabled=True, button_text="Другая кнопка"),
            Requests(enabled=True, button_text="Другая кнопка"),
            800,
        ),
        (
            None,
            Booking(
                enabled=False,
                slot_interval=45,
                online_settings=OnlineBookingSettings(export_to_ya_services=True),
            ),
            None,
            Requests(enabled=False, button_text="Кнопка"),
            None,
        ),
    ],
)
async def test_returns_updated_org_settings(
    booking, expected_booking, requests, expected_requests, phone, api, factory
):
    await factory.create_org_settings(biz_id=123)

    result = await api.post(
        URL,
        proto=UpdateSettings(
            biz_id=123,
            settings=Settings(
                emails=["email3@ya.ru"],
                phone=phone,
                notifications=EmailNotifications(
                    order_created=False,
                    order_cancelled=True,
                    order_changed=False,
                    certificate_notifications=True,
                    request_created=False,
                ),
                sms_notifications=SmsNotifications(
                    request_created=True,
                ),
                booking=booking,
                requests=requests,
            ),
        ),
        decode_as=Settings,
        expected_status=200,
    )

    assert result == Settings(
        emails=["email3@ya.ru"],
        phone=phone,
        notifications=EmailNotifications(
            order_created=False,
            order_cancelled=True,
            order_changed=False,
            certificate_notifications=True,
            request_created=False,
        ),
        sms_notifications=SmsNotifications(
            request_created=True,
        ),
        booking=expected_booking,
        requests=expected_requests,
    )


@pytest.mark.parametrize("phone", [800, None])
async def test_updates_data_in_db(phone, api, factory):
    await factory.create_org_settings(biz_id=123, created_at=dt("2020-11-03 11:00:00"))

    await api.post(
        URL,
        proto=UpdateSettings(
            biz_id=123,
            settings=Settings(
                emails=["email3@ya.ru"],
                phone=phone,
                notifications=EmailNotifications(
                    order_created=False,
                    order_cancelled=True,
                    order_changed=False,
                    certificate_notifications=True,
                    request_created=False,
                ),
                sms_notifications=SmsNotifications(
                    request_created=True,
                ),
                booking=Booking(
                    enabled=True,
                    slot_interval=20,
                    online_settings=OnlineBookingSettings(export_to_ya_services=False),
                ),
                requests=Requests(
                    enabled=True,
                    button_text="Другая кнопка",
                ),
            ),
        ),
        decode_as=Settings,
        expected_status=200,
    )

    result = await factory.fetch_org_settings(biz_id=123)

    assert result == dict(
        id=Any(int),
        biz_id=123,
        emails=["email3@ya.ru"],
        phone=phone,
        contacts=dict(),
        notifications=dict(
            order_created=False,
            order_cancelled=True,
            order_changed=False,
            certificate_notifications=True,
            request_created=False,
        ),
        sms_notifications=dict(
            request_created=True,
        ),
        booking_enabled=True,
        booking_slot_interval=20,
        booking_export_to_ya_services=False,
        requests_enabled=True,
        requests_button_text="Другая кнопка",
        general_schedule_id=None,
        created_at=dt("2020-11-03 11:00:00"),
    )


async def test_does_not_update_optional_fields_if_not_passed(api, factory):
    await factory.create_org_settings(biz_id=123, created_at=dt("2020-11-03 11:00:00"))

    await api.post(
        URL,
        proto=UpdateSettings(
            biz_id=123,
            settings=Settings(
                emails=["email3@ya.ru"],
                phone=800,
                notifications=EmailNotifications(
                    order_created=False,
                    order_cancelled=True,
                    order_changed=False,
                    certificate_notifications=True,
                ),
            ),
        ),
        decode_as=Settings,
        expected_status=200,
    )

    result = await factory.fetch_org_settings(biz_id=123)

    assert result == dict(
        id=Any(int),
        biz_id=123,
        emails=["email3@ya.ru"],
        phone=800,
        contacts=dict(),
        notifications=dict(
            order_created=False,
            order_cancelled=True,
            order_changed=False,
            certificate_notifications=True,
            request_created=True,
        ),
        sms_notifications=dict(
            request_created=False,
        ),
        booking_enabled=False,
        booking_slot_interval=45,
        booking_export_to_ya_services=True,
        requests_enabled=False,
        requests_button_text="Кнопка",
        general_schedule_id=None,
        created_at=dt("2020-11-03 11:00:00"),
    )


async def test_does_not_update_general_schedule_id(api, factory):
    await factory.create_org_settings(
        biz_id=123, created_at=dt("2020-11-03 11:00:00"), general_schedule_id=100500
    )

    await api.post(
        URL,
        proto=UpdateSettings(
            biz_id=123,
            settings=Settings(
                emails=["email3@ya.ru"],
                phone=800,
                notifications=EmailNotifications(
                    order_created=False,
                    order_cancelled=True,
                    order_changed=False,
                    certificate_notifications=True,
                ),
            ),
        ),
        decode_as=Settings,
        expected_status=200,
    )

    result = await factory.fetch_org_settings(biz_id=123)

    assert result == dict(
        id=Any(int),
        biz_id=123,
        emails=["email3@ya.ru"],
        phone=800,
        contacts=dict(),
        notifications=dict(
            order_created=False,
            order_cancelled=True,
            order_changed=False,
            certificate_notifications=True,
            request_created=True,
        ),
        sms_notifications=dict(
            request_created=False,
        ),
        booking_enabled=False,
        booking_slot_interval=45,
        booking_export_to_ya_services=True,
        requests_enabled=False,
        requests_button_text="Кнопка",
        general_schedule_id=100500,
        created_at=dt("2020-11-03 11:00:00"),
    )


@pytest.mark.parametrize(
    "booking",
    [
        Booking(
            enabled=True,
            slot_interval=20,
            online_settings=OnlineBookingSettings(export_to_ya_services=False),
        ),
        None,
    ],
)
async def test_errored_if_no_settings_for_biz_id(booking, api, factory):
    result = await api.post(
        URL,
        proto=UpdateSettings(
            biz_id=123,
            settings=Settings(
                emails=["email3@ya.ru"],
                notifications=EmailNotifications(
                    order_created=False,
                    order_cancelled=True,
                    order_changed=False,
                    certificate_notifications=True,
                ),
                booking=booking,
            ),
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert result == Error(
        code=Error.UNKNOWN_BIZ_ID, description="Settings for biz_id 123 not found."
    )


async def test_errored_on_incorrect_biz_id(api, factory):
    result = await api.post(
        URL,
        proto=UpdateSettings(
            biz_id=0,
            settings=Settings(
                emails=["email3@ya.ru"],
                notifications=EmailNotifications(
                    order_created=False,
                    order_cancelled=True,
                    order_changed=False,
                    certificate_notifications=True,
                ),
                booking=Booking(
                    enabled=True,
                    slot_interval=20,
                    online_settings=OnlineBookingSettings(export_to_ya_services=False),
                ),
            ),
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert result == Error(
        code=Error.VALIDATION_ERROR, description="biz_id: ['Must be at least 1.']"
    )


@pytest.mark.parametrize(
    "extra_settings, error_description",
    [
        (
            {"emails": [""]},
            "settings: {'emails': {0: ['Shorter than minimum length 1.']}}",
        ),
        ({"phone": 0}, "settings: {'phone': ['Must be at least 1.']}"),
        (
            {
                "booking": Booking(
                    enabled=True,
                    slot_interval=0,
                    online_settings=OnlineBookingSettings(export_to_ya_services=False),
                )
            },
            "settings: {'booking': {'slot_interval': ['Must be at least 1.']}}",
        ),
    ],
)
async def test_errored_on_incorrect_input(
    extra_settings, error_description, api, factory
):
    settings = dict(
        emails=["email3@ya.ru"],
        phone=800,
        notifications=EmailNotifications(
            order_created=False,
            order_cancelled=True,
            order_changed=False,
            certificate_notifications=True,
        ),
        booking=Booking(
            enabled=True,
            slot_interval=20,
            online_settings=OnlineBookingSettings(export_to_ya_services=False),
        ),
    )
    settings.update(extra_settings)

    result = await api.post(
        URL,
        proto=UpdateSettings(biz_id=123, settings=Settings(**settings)),
        decode_as=Error,
        expected_status=400,
    )

    assert result == Error(
        code=Error.VALIDATION_ERROR,
        description=error_description,
    )
