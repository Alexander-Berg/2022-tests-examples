from datetime import datetime

import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.tuner.proto.errors_pb2 import Error
from maps_adv.geosmb.tuner.proto.settings_pb2 import (
    Booking,
    EmailNotifications,
    FetchSettings,
    OnlineBookingSettings,
    Requests,
    Settings,
    SmsNotifications,
)

pytestmark = [pytest.mark.asyncio]

URL = "/v1/fetch_settings/"


async def test_returns_org_settings(api, factory):
    await factory.create_org_settings(biz_id=123)

    result = await api.post(
        URL,
        proto=FetchSettings(biz_id=123),
        decode_as=Settings,
        expected_status=200,
    )

    assert result == Settings(
        emails=["email1@ya.ru", "email2@ya.ru"],
        phone=900,
        notifications=EmailNotifications(
            order_created=True,
            order_cancelled=False,
            order_changed=True,
            certificate_notifications=False,
            request_created=True,
        ),
        sms_notifications=SmsNotifications(
            request_created=False,
        ),
        booking=Booking(
            enabled=False,
            slot_interval=45,
            online_settings=OnlineBookingSettings(export_to_ya_services=True),
        ),
        requests=Requests(
            enabled=False,
            button_text="Кнопка",
        ),
    )


async def test_returns_default_settings_if_no_data(api, factory):
    result = await api.post(
        URL,
        proto=FetchSettings(biz_id=123),
        decode_as=Settings,
        expected_status=200,
    )

    assert result == Settings(
        emails=[],
        phone=None,
        notifications=EmailNotifications(
            order_created=True,
            order_cancelled=True,
            order_changed=True,
            certificate_notifications=True,
            request_created=True,
        ),
        sms_notifications=SmsNotifications(
            request_created=True,
        ),
        booking=Booking(
            enabled=True,
            slot_interval=30,
            online_settings=OnlineBookingSettings(export_to_ya_services=False),
        ),
        requests=Requests(
            enabled=True,
            button_text="Отправить заявку",
        ),
    )


async def test_saves_default_settings_in_db(api, factory):
    await api.post(
        URL,
        proto=FetchSettings(biz_id=123),
        decode_as=Settings,
        expected_status=200,
    )

    row = await factory.fetch_org_settings(biz_id=123)
    assert row == dict(
        id=Any(int),
        biz_id=123,
        emails=[],
        phone=None,
        contacts=dict(),
        notifications=dict(
            order_created=True,
            order_cancelled=True,
            order_changed=True,
            certificate_notifications=True,
            request_created=True,
        ),
        sms_notifications=dict(
            request_created=True,
        ),
        booking_enabled=True,
        booking_slot_interval=30,
        booking_export_to_ya_services=False,
        requests_enabled=True,
        requests_button_text="Отправить заявку",
        general_schedule_id=None,
        created_at=Any(datetime),
    )


async def test_does_not_return_other_business_settings(api, factory):
    await factory.create_org_settings(
        biz_id=123,
        emails=["email@ya.ru"],
        phone=800,
        notifications=dict(
            order_created=False,
            order_cancelled=False,
            order_changed=False,
            certificate_notifications=False,
            request_created=False,
        ),
        sms_notifications=dict(
            request_created=False,
        ),
        booking_enabled=True,
        booking_slot_interval=50,
        booking_export_to_ya_services=True,
        requests_enabled=True,
        requests_button_text="Другая кнопка",
    )
    await factory.create_org_settings(biz_id=222)

    result = await api.post(
        URL,
        proto=FetchSettings(biz_id=123),
        decode_as=Settings,
        expected_status=200,
    )

    assert result == Settings(
        emails=["email@ya.ru"],
        phone=800,
        notifications=EmailNotifications(
            order_created=False,
            order_cancelled=False,
            order_changed=False,
            certificate_notifications=False,
            request_created=False,
        ),
        sms_notifications=SmsNotifications(
            request_created=False,
        ),
        booking=Booking(
            enabled=True,
            slot_interval=50,
            online_settings=OnlineBookingSettings(export_to_ya_services=True),
        ),
        requests=Requests(
            enabled=True,
            button_text="Другая кнопка",
        ),
    )


async def test_errored_on_incorrect_biz_id(api, factory):
    result = await api.post(
        URL, proto=FetchSettings(biz_id=0), decode_as=Error, expected_status=400
    )

    assert result == Error(
        code=Error.VALIDATION_ERROR, description="biz_id: ['Must be at least 1.']"
    )
