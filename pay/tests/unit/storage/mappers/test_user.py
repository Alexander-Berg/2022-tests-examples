import pytest

from hamcrest import assert_that, equal_to

from pay.bill_payments.bill_payments.storage import Storage, UserMapper
from pay.bill_payments.bill_payments.storage.entities.user import User
from pay.bill_payments.bill_payments.tests.unit.storage.mappers.base import BaseMapperTests


class TestUserMapper(BaseMapperTests):
    @pytest.fixture
    def entity(self) -> User:
        return User(
            uid=42,
        )

    @pytest.fixture
    def mapper(self, storage: Storage) -> UserMapper:
        return storage.user

    @pytest.mark.asyncio
    async def test_should_create_with_expected_defaults(self, mapper, entity):
        created = await mapper.create(entity)

        assert created.revision == 1
        assert created.synced_revision == 0
        assert created.syncing_revision is None

    @pytest.mark.asyncio
    async def test_get(self, mapper, entity):
        created = await mapper.create(entity)

        assert_that(
            await mapper.get(created.uid),
            equal_to(created),
        )

    @pytest.mark.asyncio
    async def test_get_not_found(self, mapper):
        with pytest.raises(User.DoesNotExist):
            await mapper.get(-1)

    @pytest.mark.asyncio
    async def test_can_update(self, mapper, entity):
        created = await mapper.create(entity)
        created.revision = 2
        created.synced_revision = 2
        created.syncing_revision = 2
        created.subscription_id = 'some_sub'

        updated = await mapper.save(created)
        created.updated = updated.updated

        assert_that(
            updated,
            equal_to(created),
        )

    @pytest.mark.asyncio
    async def test_unique_for_subscription_id(self, mapper, entity):
        entity.subscription_id = 'subscription'
        await mapper.create(entity)

        entity.uid = 1222
        with pytest.raises(User.AlreadyExists):
            await mapper.create(entity)

    @pytest.mark.asyncio
    async def test_delete(self, mapper, entity):
        created = await mapper.create(entity)

        await mapper.delete(created)

        with pytest.raises(User.DoesNotExist):
            await mapper.get(entity.uid)

    @pytest.mark.asyncio
    async def test_increment_revision(self, mapper, entity):
        await mapper.create(entity)

        revision = await mapper.increment_revision(entity.uid)

        assert_that(revision, equal_to(entity.revision + 1))

    @pytest.mark.asyncio
    async def test_update_synced_revision(self, mapper, entity):
        entity.syncing_revision = 1
        await mapper.create(entity)

        await mapper.update_synced_revision(entity.uid, 1)

        updated = await mapper.get(entity.uid)
        assert updated.syncing_revision is None
        assert_that(updated.synced_revision, equal_to(1))

    @pytest.mark.asyncio
    async def test_update_synced_revision_consistency_check(self, mapper, entity):
        await mapper.create(entity)

        with pytest.raises(User.DoesNotExist):
            await mapper.update_synced_revision(entity.uid, 1)
