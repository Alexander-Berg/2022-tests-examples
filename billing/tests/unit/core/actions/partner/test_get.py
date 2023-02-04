from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.partner.get import GetPartnerAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.role import AuthorizeRoleAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import PartnerNotFoundError
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import RoleType


@pytest.mark.asyncio
async def test_get_partner(partner, role, user):
    partner.role = role

    loaded = await GetPartnerAction(user=user, partner_id=partner.partner_id).run()

    assert_that(loaded, equal_to(partner))


@pytest.mark.asyncio
async def test_calls_authorize_action(partner, role, user, mock_action):
    mock = mock_action(AuthorizeRoleAction)

    await GetPartnerAction(user=user, partner_id=partner.partner_id).run()

    mock.assert_run_once_with(partner_id=partner.partner_id, user=user, minimum_role_required=RoleType.VIEWER)


@pytest.mark.asyncio
async def test_partner_not_found_if_role_is_missing(storage, partner, role, user):
    await storage.role.delete(role)

    with pytest.raises(PartnerNotFoundError):
        await GetPartnerAction(user=user, partner_id=partner.partner_id).run()


@pytest.mark.asyncio
async def test_get_invalid_partner_id(partner, user):
    with pytest.raises(PartnerNotFoundError):
        await GetPartnerAction(user=user, partner_id=uuid4()).run()
