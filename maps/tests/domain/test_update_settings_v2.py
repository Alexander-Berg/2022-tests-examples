import pytest

from maps_adv.geosmb.tuner.server.lib.exceptions import UnknownBizId

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_returns_updated_data(domain, dm):
    dm.check_settings_exist.coro.return_value = True
    dm.update_settings_v2.coro.return_value = dict(
        contacts=dict(emails=["email3@ya.ru"], phones=[900], telegram_logins=['login']),
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

    got = await domain.update_settings_v2(
        biz_id=123,
        contacts=dict(emails=["email3@ya.ru"]),
    )

    assert got == dict(
        contacts=dict(emails=["email3@ya.ru"], phones=[900], telegram_logins=['login']),
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


async def test_requests_dm_correctly(domain, dm):
    dm.check_settings_exist.coro.return_value = True

    await domain.update_settings_v2(
        biz_id=123,
        contacts=dict(emails=["email3@ya.ru"], phones=[900], telegram_logins=['login']),
        booking=dict(
            enabled=False,
            slot_interval=45,
            online_settings=dict(export_to_ya_services=True),
        ),
    )

    dm.check_settings_exist.assert_called_with(biz_id=123)
    dm.update_settings_v2.assert_called_with(
        biz_id=123,
        contacts=dict(emails=["email3@ya.ru"], phones=[900], telegram_logins=['login']),
        booking=dict(
            enabled=False,
            slot_interval=45,
            online_settings=dict(export_to_ya_services=True),
        ),
    )


async def test_raises_if_biz_id_not_found(domain, dm):
    dm.check_settings_exist.coro.return_value = False

    with pytest.raises(UnknownBizId, match="Settings for biz_id 123 not found."):
        await domain.update_settings_v2(biz_id=123, contacts=dict(emails=["email3@ya.ru"]))

    dm.update_settings_v2.assert_not_called()
