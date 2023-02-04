import uuid

import pytest

from hamcrest import assert_that, equal_to, has_properties, instance_of, none

from billing.yandex_pay_admin.yandex_pay_admin.storage import PartnerMapper
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.agent import Agent
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import RoleType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.partner import Contact, Partner
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.role import Role
from billing.yandex_pay_admin.yandex_pay_admin.tests.unit.storage.mappers.base import BaseMapperTests


class TestPartnerMapper(BaseMapperTests):
    @pytest.fixture
    def entity(self, partner_entity) -> Partner:
        partner_entity.partner_id = uuid.uuid4()
        partner_entity.balance_client_id = 'client_id'
        partner_entity.balance_person_id = 'person_id'
        partner_entity.balance_contract_id = 'contract_id'
        partner_entity.balance_ext_contract_id = 'ext_contract_id'
        return partner_entity

    @pytest.fixture
    def role_entity(self, entity, uid):
        return Role(
            partner_id=entity.partner_id,
            uid=uid,
            role=RoleType.OWNER,
        )

    @pytest.fixture
    def mapper(self, storage) -> PartnerMapper:
        return storage.partner

    @pytest.mark.asyncio
    async def test_should_generate_partner_id_if_not_passed(self, mapper, entity):
        entity.partner_id = None
        created = await mapper.create(entity)

        assert_that(created.partner_id, instance_of(uuid.UUID))

    @pytest.mark.asyncio
    async def test_data_mapper_when_optionals_are_missing(self, mapper, entity):
        entity.registration_data.contact = Contact()
        entity.registration_data.tax_ref_number = None
        created = await mapper.create(entity)

        assert_that(
            await mapper.get(created.partner_id),
            has_properties(
                registration_data=has_properties(
                    contact=Contact(),
                    tax_ref_number=None,
                )
            ),
        )
        assert_that(created.role, none())

    @pytest.mark.asyncio
    async def test_get(self, mapper, entity):
        created = await mapper.create(entity)

        assert_that(
            await mapper.get(created.partner_id),
            equal_to(created),
        )
        assert_that(created.role, none())

    @pytest.mark.asyncio
    async def test_get_not_found(self, mapper):
        with pytest.raises(Partner.DoesNotExist):
            await mapper.get(uuid.uuid4())

    @pytest.mark.asyncio
    async def test_can_update(self, mapper, entity):
        created = await mapper.create(entity)
        created.name = 'changed name'

        updated = await mapper.save(created)
        created.updated = updated.updated

        assert_that(
            updated,
            equal_to(created),
        )

    @pytest.mark.asyncio
    async def test_primary_key(self, mapper, entity):
        await mapper.create(entity)

        with pytest.raises(Partner.AlreadyExists):
            await mapper.create(entity)

    @pytest.mark.asyncio
    async def test_get_with_valid_user_role(self, storage, mapper, entity, uid, role_entity):
        await mapper.create(entity)
        role = await storage.role.create(role_entity)

        loaded = await mapper.get(entity.partner_id, uid=uid)

        assert_that(loaded.role, equal_to(role))

    @pytest.mark.asyncio
    async def test_get_with_invalid_user_role(self, mapper, entity, uid):
        await mapper.create(entity)
        await mapper.get(entity.partner_id)  # partner does exist

        with pytest.raises(Partner.DoesNotExist):
            # but it cannot be retrieved with the wrong uid
            await mapper.get(entity.partner_id, uid=uid)

    @pytest.mark.asyncio
    async def test_update_bumps_revision(self, mapper, entity):
        created = await mapper.create(entity)
        assert_that(created.revision, equal_to(1))

        updated = await mapper.save(created)
        assert_that(updated.revision, equal_to(2))

        created.updated = updated.updated
        assert_that(updated, equal_to(created))

    @pytest.mark.asyncio
    async def test_find_by_uid(self, storage, mapper, entity, uid, role_entity):
        created = await mapper.create(entity)
        role = await storage.role.create(role_entity)
        created.role = role

        assert_that(await mapper.find_by_uid(uid), equal_to([created]))

    @pytest.mark.asyncio
    async def test_find_by_uid_does_not_return_unrelated_partners(self, mapper, entity, uid):
        await mapper.create(entity)

        # returns empty list as use role for the partner is missing
        assert_that(await mapper.find_by_uid(uid), equal_to([]))

    @pytest.mark.asyncio
    async def test_set_agent_id(self, storage, mapper, entity):
        agent = await storage.agent.create(Agent(name='agent'))
        partner = await mapper.create(entity)
        partner.agent_id = agent.agent_id

        saved = await mapper.save(partner)

        assert_that(saved.agent_id, equal_to(agent.agent_id))
