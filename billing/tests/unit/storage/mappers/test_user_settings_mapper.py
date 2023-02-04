import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.entities.user import UserSettings


@pytest.fixture
def user_settings_entity() -> UserSettings:
    return UserSettings(uid=1111, address_id='addr', contact_id='cont', card_id='card')


@pytest.mark.asyncio
async def test_create(storage, user_settings_entity):
    created = await storage.user_settings.create(user_settings_entity)

    user_settings_entity.created = created.created
    user_settings_entity.updated = created.updated
    assert_that(
        created,
        equal_to(user_settings_entity),
    )


@pytest.mark.asyncio
async def test_get(storage, user_settings_entity):
    created = await storage.user_settings.create(user_settings_entity)

    assert_that(
        await storage.user_settings.get(user_settings_entity.uid),
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(UserSettings.DoesNotExist):
        await storage.user_settings.get(-1)


@pytest.mark.asyncio
async def test_save(storage, user_settings_entity):
    created: UserSettings = await storage.user_settings.create(user_settings_entity)
    created.address_id = 'new_addr'
    created.contact_id = ''
    created.card_id = None

    saved = await storage.user_settings.save(created)
    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )
