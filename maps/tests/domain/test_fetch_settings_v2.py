import pytest

from maps_adv.geosmb.tuner.server.lib.exceptions import UnknownBizId

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_returns_dm_data(domain, dm):
    dm.fetch_settings_v2.coro.return_value = dict(
        contacts=dict(emails=["email@ya.ru"], phones=[900], telegram_logins=['login']),
        notifications=dict(notify_kek=True, notify_meow=False),
        sms_notifications=dict(notify_kek=False),
        booking=dict(book_meow=True, book_wow=dict(meow=45)),
        requests=dict(a=1),
        general_schedule_id=100500,
    )

    got = await domain.fetch_settings_v2(biz_id=123)

    dm.create_default_settings_v2.assert_not_called()
    assert got == dict(
        contacts=dict(emails=["email@ya.ru"], phones=[900], telegram_logins=['login']),
        notifications=dict(notify_kek=True, notify_meow=False),
        sms_notifications=dict(notify_kek=False),
        booking=dict(book_meow=True, book_wow=dict(meow=45)),
        requests=dict(a=1),
        general_schedule_id=100500,
    )


async def test_does_not_create_default_settings_if_there_are_any(domain, dm):
    dm.fetch_settings_v2.coro.return_value = dict(
        contacts=dict(emails=["email@ya.ru"], phones=[900], telegram_logins=['login']),
        notifications=dict(notify_kek=True, notify_meow=False),
        sms_notifications=dict(notify_kek=False),
        booking=dict(book_meow=True, book_wow=dict(meow=45)),
        requests=dict(a=1),
        general_schedule_id=100500,
    )

    await domain.fetch_settings_v2(biz_id=123)

    dm.create_default_settings_v2.assert_not_called()


async def test_returns_default_settings_if_were_not_any(domain, dm):
    dm.fetch_settings_v2.coro.side_effect = UnknownBizId()
    dm.create_default_settings_v2.coro.return_value = dict(
        contacts=dict(emails=["email@ya.ru"], phones=[900], telegram_logins=['login']),
        notifications=dict(notify_cheburek=True, notify_lol=False),
        sms_notifications=dict(notify_kek=False),
        booking=dict(book_meow=True, book_wow=dict(meow=45)),
        requests=dict(a=1),
        general_schedule_id=None,
    )

    got = await domain.fetch_settings_v2(biz_id=123)

    assert got == dict(
        contacts=dict(emails=["email@ya.ru"], phones=[900], telegram_logins=['login']),
        notifications=dict(notify_cheburek=True, notify_lol=False),
        sms_notifications=dict(notify_kek=False),
        booking=dict(book_meow=True, book_wow=dict(meow=45)),
        requests=dict(a=1),
        general_schedule_id=None,
    )


async def test_saves_default_settings_if_were_not_any(domain, dm):
    dm.fetch_settings_v2.coro.side_effect = UnknownBizId()

    await domain.fetch_settings_v2(biz_id=123)

    dm.create_default_settings_v2.coro.assert_called_with(
        biz_id=123, contacts=dict()
    )
