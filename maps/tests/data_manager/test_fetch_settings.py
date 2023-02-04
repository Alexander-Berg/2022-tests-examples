import pytest

from maps_adv.geosmb.tuner.server.lib.exceptions import UnknownBizId

pytestmark = [pytest.mark.asyncio]


async def test_returns_org_settings(dm, factory):
    await factory.create_org_settings(biz_id=123, general_schedule_id=100500)

    result = await dm.fetch_settings(biz_id=123)

    assert result == dict(
        emails=["email1@ya.ru", "email2@ya.ru"],
        phone=900,
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
        booking=dict(
            enabled=False,
            slot_interval=45,
            online_settings=dict(export_to_ya_services=True),
        ),
        requests=dict(
            enabled=False,
            button_text="Кнопка",
        ),
        general_schedule_id=100500,
    )


async def test_raises_if_no_settings_for_requested_biz_id(dm, factory):
    with pytest.raises(UnknownBizId, match="Settings for biz_id 123 not found."):
        await dm.fetch_settings(biz_id=123)


async def test_does_not_return_other_business_settings(dm, factory):
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
        general_schedule_id=100500,
    )
    await factory.create_org_settings(biz_id=222)

    result = await dm.fetch_settings(biz_id=123)

    assert result == dict(
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
        booking=dict(
            enabled=False,
            slot_interval=45,
            online_settings=dict(export_to_ya_services=True),
        ),
        requests=dict(
            enabled=False,
            button_text="Кнопка",
        ),
        general_schedule_id=100500,
    )
