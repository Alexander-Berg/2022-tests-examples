import pytest

from billing.yandex_pay.yandex_pay.core.actions.user import GetUserSettingsAction, UpsertUserSettingsAction
from billing.yandex_pay.yandex_pay.core.entities.user import UserSettings


@pytest.mark.asyncio
async def test_should_create_if_not_exist(storage):
    result = await UpsertUserSettingsAction(uid=322, address_id='addr', contact_id='cont', card_id='card').run()

    created = await storage.user_settings.get(322)
    assert created.address_id == 'addr'
    assert created.contact_id == 'cont'
    assert created.card_id == 'card'
    assert result == created


@pytest.mark.asyncio
async def test_should_create_with_expected_rules(storage):
    result = await UpsertUserSettingsAction(
        uid=322,
        address_id='addr',
        contact_id='',
        card_id=None,
        is_checkout_onboarded=False
    ).run()

    created = await storage.user_settings.get(322)
    assert created.address_id == 'addr'
    assert created.contact_id is None
    assert created.card_id is None
    assert created.is_checkout_onboarded is False
    assert result == created


@pytest.mark.asyncio
async def test_should_update_with_expected_rules(storage):
    await storage.user_settings.create(UserSettings(
        uid=42,
        address_id='addr',
        contact_id='cont',
        card_id='card',
        is_checkout_onboarded=False,
    ))

    result = await UpsertUserSettingsAction(uid=42, address_id='new_addr', contact_id='', card_id=None).run()

    updated = await storage.user_settings.get(42)
    assert updated.address_id == 'new_addr'
    assert updated.contact_id is None
    assert updated.card_id == 'card'
    assert updated.is_checkout_onboarded is False
    assert result == updated


@pytest.mark.asyncio
async def test_can_get_user_settings(storage):
    created = await storage.user_settings.create(UserSettings(
        uid=322,
        address_id='addr',
        contact_id='cont',
        card_id='card',
        is_checkout_onboarded=False,
    ))

    result = await GetUserSettingsAction(uid=322).run()

    assert result == created


@pytest.mark.asyncio
async def test_should_return_empty_user_settings_if_not_exist(storage):
    result = await GetUserSettingsAction(uid=322).run()

    assert result == UserSettings(uid=322)
