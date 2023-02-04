import logging

import pytest

from hamcrest import assert_that, equal_to, has_entries, has_item, has_properties, is_

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.partner.create import (
    CreatePartnerAction,
    CreatePartnerByAgentAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import PartnerAlreadyExistsError
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import RoleType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.partner import Contact, RegistrationData


@pytest.fixture
def registration_data():
    return RegistrationData(
        contact=Contact(
            email='email@test',
            phone='+1(000)555-0100',
            first_name='John',
            last_name='Doe',
            middle_name='Татьянович',
        ),
        tax_ref_number='0123 АБ',
    )


def test_should_run_in_transaction():
    assert_that(CreatePartnerAction.transact, is_(True))


@pytest.mark.asyncio
async def test_creates_entities(storage, user, registration_data):
    partner = await CreatePartnerAction(
        user=user,
        name='test name',
        registration_data=registration_data,
    ).run()

    loaded_partner = await storage.partner.get(partner.partner_id, uid=user.uid)
    loaded_role = await storage.role.get(partner.partner_id, user.uid)

    assert_that(loaded_partner, equal_to(partner))
    assert_that(loaded_partner, has_properties(role=loaded_role, name='test name', revision=1))
    assert_that(loaded_role.role, equal_to(RoleType.OWNER))


@pytest.mark.asyncio
async def test_call_logged(user, registration_data, dummy_logs):
    partner = await CreatePartnerAction(
        user=user,
        name='test name',
        registration_data=registration_data,
    ).run()

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='PARTNER_CREATED',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=user.uid,
                    partner_id=partner.partner_id,
                ),
            )
        ),
    )


@pytest.mark.asyncio
async def test_only_one_partner_owned_per_uid(user, registration_data):
    await CreatePartnerAction(
        user=user,
        name='test name',
        registration_data=registration_data,
    ).run()

    with pytest.raises(PartnerAlreadyExistsError):
        await CreatePartnerAction(
            user=user,
            name='other test name',
            registration_data=registration_data,
        ).run()


@pytest.mark.asyncio
async def test_create_partner_by_agent_success(agent):
    agent_partner_id = 'some-agent-id'
    reg_data = RegistrationData(tax_ref_number='123456')
    name = 'some name'

    created_partner = await CreatePartnerByAgentAction(
        agent_id=agent.agent_id,
        agent_partner_id=agent_partner_id,
        registration_data=reg_data,
        name=name,
    ).run()

    assert created_partner.agent_id == agent.agent_id
    assert created_partner.agent_partner_id == agent_partner_id
    assert created_partner.name == name
    assert created_partner.registration_data == reg_data


@pytest.mark.asyncio
async def test_create_partner_by_agent_already_exists(agent):
    agent_partner_id = 'some-agent-id'
    reg_data = RegistrationData(tax_ref_number='123456')
    name = 'some name'

    a = await CreatePartnerByAgentAction(
        agent_id=agent.agent_id,
        agent_partner_id=agent_partner_id,
        registration_data=reg_data,
        name=name,
    ).run()

    b = await CreatePartnerByAgentAction(
        agent_id=agent.agent_id,
        agent_partner_id=agent_partner_id,
        registration_data=reg_data,
        name=name,
    ).run()

    assert a == b
