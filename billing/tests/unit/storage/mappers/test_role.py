import uuid

import psycopg2
import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.storage import RoleMapper
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import RoleType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.partner import Partner
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.role import Role
from billing.yandex_pay_admin.yandex_pay_admin.tests.unit.storage.mappers.base import BaseMapperTests


class TestRoleMapper(BaseMapperTests):
    @pytest.fixture
    def entity(self, partner: Partner) -> Role:
        assert partner.partner_id is not None

        return Role(
            partner_id=partner.partner_id,
            uid=4444,
            role=RoleType.VIEWER,
        )

    @pytest.fixture
    def mapper(self, storage) -> RoleMapper:
        return storage.role

    @pytest.mark.asyncio
    async def test_get(self, mapper, entity):
        created = await mapper.create(entity)

        assert_that(
            await mapper.get(created.partner_id, created.uid),
            equal_to(created),
        )

    @pytest.mark.asyncio
    async def test_get_not_found(self, mapper):
        with pytest.raises(Role.DoesNotExist):
            await mapper.get(uuid.uuid4(), 222)

    @pytest.mark.asyncio
    async def test_can_update(self, mapper, entity):
        created = await mapper.create(entity)
        created.role = RoleType.OWNER

        updated = await mapper.save(created)
        created.updated = updated.updated

        assert_that(
            updated,
            equal_to(created),
        )

    @pytest.mark.asyncio
    async def test_unique_for_partner_and_uid(self, mapper, entity):
        await mapper.create(entity)

        entity.uid = 42123
        await mapper.create(entity)

        with pytest.raises(Role.AlreadyExists):
            await mapper.create(entity)

    @pytest.mark.asyncio
    async def test_has_foreign_key_on_partner(self, mapper, entity):
        entity.partner_id = uuid.uuid4()

        with pytest.raises(psycopg2.errors.ForeignKeyViolation):
            await mapper.create(entity)

    @pytest.mark.asyncio
    async def test_should_allow_only_single_owner_for_partner(self, mapper, entity):
        entity.role = RoleType.OWNER
        await mapper.create(entity)

        entity.uid = 11122
        with pytest.raises(Role.AlreadyExists):
            await mapper.create(entity)

    @pytest.mark.asyncio
    async def test_should_allow_several_viewers_for_partner(self, mapper, entity):
        entity.role = RoleType.VIEWER
        await mapper.create(entity)

        entity.uid = 5522
        await mapper.create(entity)

    @pytest.mark.asyncio
    async def test_can_find_owner_by_partner_id(self, mapper, entity):
        await mapper.create(entity)
        entity.role = RoleType.OWNER
        entity.uid = 123
        owner = await mapper.create(entity)

        found_owner = await mapper.find_owner_by_partner_id(entity.partner_id)

        assert_that(
            found_owner,
            equal_to(owner),
        )

    @pytest.mark.asyncio
    async def test_should_raise_if_owner_not_found(self, mapper, entity):
        with pytest.raises(Role.DoesNotExist):
            await mapper.find_owner_by_partner_id(uuid.uuid4())
