from datetime import datetime

import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.tuner.proto.errors_pb2 import Error
from maps_adv.geosmb.tuner.proto.settings_pb2 import (
    FetchSettings,
    GeneralScheduleIdSetting,
)

pytestmark = [pytest.mark.asyncio]

URL = "/v1/fetch_settings/general_schedule_id/"


@pytest.mark.parametrize("value", (100500, None))
async def test_returns_org_setting(value, api, factory):
    await factory.create_org_settings(biz_id=123, general_schedule_id=value)

    got = await api.post(
        URL,
        proto=FetchSettings(biz_id=123),
        decode_as=GeneralScheduleIdSetting,
        expected_status=200,
    )

    assert (
        got == GeneralScheduleIdSetting(general_schedule_id=value)
        if value
        else GeneralScheduleIdSetting()
    )


async def test_returns_default_setting_if_no_data(api, factory):
    got = await api.post(
        URL,
        proto=FetchSettings(biz_id=123),
        decode_as=GeneralScheduleIdSetting,
        expected_status=200,
    )

    assert got == GeneralScheduleIdSetting()


async def test_saves_default_settings_in_db(api, factory):
    await api.post(
        URL,
        proto=FetchSettings(biz_id=123),
        decode_as=GeneralScheduleIdSetting,
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
        general_schedule_id=100500,
    )
    await factory.create_org_settings(biz_id=222)

    got = await api.post(
        URL,
        proto=FetchSettings(biz_id=123),
        decode_as=GeneralScheduleIdSetting,
        expected_status=200,
    )

    assert got == GeneralScheduleIdSetting(general_schedule_id=100500)


async def test_errored_on_incorrect_biz_id(api, factory):
    got = await api.post(
        URL, proto=FetchSettings(biz_id=0), decode_as=Error, expected_status=400
    )

    assert got == Error(
        code=Error.VALIDATION_ERROR, description="biz_id: ['Must be at least 1.']"
    )
