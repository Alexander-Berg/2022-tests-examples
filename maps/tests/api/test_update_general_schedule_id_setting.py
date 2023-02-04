import pytest
from smb.common.testing_utils import Any, dt

from maps_adv.geosmb.tuner.proto.errors_pb2 import Error
from maps_adv.geosmb.tuner.proto.settings_pb2 import (
    GeneralScheduleIdSetting,
    UpdateGeneralScheduleIdSetting,
)

pytestmark = [pytest.mark.asyncio]

URL = "/v1/update_settings/general_schedule_id/"


@pytest.mark.parametrize("value", (100500, None))
async def test_returns_updated_setting(value, api, factory):
    await factory.create_org_settings(biz_id=123)

    got = await api.post(
        URL,
        proto=UpdateGeneralScheduleIdSetting(biz_id=123, general_schedule_id=value)
        if value
        else UpdateGeneralScheduleIdSetting(biz_id=123),
        decode_as=GeneralScheduleIdSetting,
        expected_status=200,
    )

    assert (
        got == GeneralScheduleIdSetting(general_schedule_id=value)
        if value
        else GeneralScheduleIdSetting()
    )


@pytest.mark.parametrize("value", (100500, None))
async def test_updates_data_in_db(value, api, factory):
    await factory.create_org_settings(biz_id=123, created_at=dt("2020-11-03 11:00:00"))

    await api.post(
        URL,
        proto=UpdateGeneralScheduleIdSetting(biz_id=123, general_schedule_id=value)
        if value
        else UpdateGeneralScheduleIdSetting(biz_id=123),
        decode_as=GeneralScheduleIdSetting,
        expected_status=200,
    )

    result = await factory.fetch_org_settings(biz_id=123)

    assert result == dict(
        id=Any(int),
        biz_id=123,
        emails=["email1@ya.ru", "email2@ya.ru"],
        phone=900,
        contacts=dict(),
        notifications=dict(
            order_created=True,
            order_cancelled=False,
            order_changed=True,
            certificate_notifications=False,
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
        general_schedule_id=value,
        created_at=dt("2020-11-03 11:00:00"),
    )


async def test_errored_if_no_settings_for_biz_id(api, factory):
    result = await api.post(
        URL,
        proto=UpdateGeneralScheduleIdSetting(biz_id=123, general_schedule_id=100500),
        decode_as=Error,
        expected_status=400,
    )

    assert result == Error(
        code=Error.UNKNOWN_BIZ_ID, description="Settings for biz_id 123 not found."
    )


async def test_errored_on_incorrect_biz_id(api, factory):
    result = await api.post(
        URL,
        proto=UpdateGeneralScheduleIdSetting(biz_id=0, general_schedule_id=100500),
        decode_as=Error,
        expected_status=400,
    )

    assert result == Error(
        code=Error.VALIDATION_ERROR, description="biz_id: ['Must be at least 1.']"
    )


async def test_errored_on_incorrect_input(api, factory):
    result = await api.post(
        URL,
        proto=UpdateGeneralScheduleIdSetting(biz_id=10, general_schedule_id=0),
        decode_as=Error,
        expected_status=400,
    )

    assert result == Error(
        code=Error.VALIDATION_ERROR,
        description="general_schedule_id: ['Must be at least 1.']",
    )
