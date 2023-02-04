import logging
from copy import copy

import pytest

from hamcrest import assert_that, equal_to, has_entries, has_item, has_properties, is_

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.partner.update import UpdatePartnerAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.partner.update_data import UpdatePartnerDataAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.role import AuthorizeRoleAction
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import RoleType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.partner import RegistrationData


def test_should_run_in_transaction():
    assert_that(UpdatePartnerAction.transact, is_(True))


@pytest.mark.asyncio
async def test_update_partner(partner, role, user):
    name = 'new name'

    updated_partner = await UpdatePartnerAction(user=user, partner_id=partner.partner_id, name=name).run()

    partner.role = role
    partner.name = name
    partner.revision += 1
    partner.updated = updated_partner.updated

    assert_that(updated_partner, equal_to(partner))


@pytest.mark.asyncio
async def test_update_registration_data(partner, user, role, mock_action):
    mock = mock_action(UpdatePartnerDataAction, copy(partner))
    reg_data = RegistrationData()

    await UpdatePartnerAction(
        user=user, partner_id=partner.partner_id, name=partner.name, registration_data=reg_data
    ).run()

    partner.role = role
    mock.assert_run_once_with(
        user=user,
        partner=partner,
        registration_data=reg_data,
        save=False,
    )


@pytest.mark.asyncio
async def test_update_skipped_if_name_unchanged(partner, role, user, dummy_logs):
    updated_partner = await UpdatePartnerAction(user=user, partner_id=partner.partner_id, name=partner.name).run()

    partner.role = role

    assert_that(updated_partner, equal_to(partner))

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='PARTNER_UPDATE_SKIPPED',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=user.uid,
                    partner_id=partner.partner_id,
                ),
            )
        ),
    )


@pytest.mark.asyncio
async def test_calls_authorize_action(partner, role, user, mock_action):
    mock = mock_action(AuthorizeRoleAction)

    await UpdatePartnerAction(user=user, partner_id=partner.partner_id, name=partner.name).run()

    mock.assert_run_once_with(
        partner_id=partner.partner_id,
        user=user,
        minimum_role_required=RoleType.OWNER,
    )
