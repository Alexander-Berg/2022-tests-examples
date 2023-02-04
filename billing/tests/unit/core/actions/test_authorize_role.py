import logging

import pytest

from hamcrest import assert_that, has_entries, has_item, has_properties

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.role import AuthorizeRoleAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import RolePermissionError
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import RoleType


@pytest.mark.asyncio
@pytest.mark.parametrize('minimum_role_required', list(RoleType))
async def test_authorization_succeeds(partner, role, user, dummy_logs, minimum_role_required):
    await AuthorizeRoleAction(
        partner_id=partner.partner_id,
        user=user,
        minimum_role_required=minimum_role_required,
    ).run()

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='AUTH_ROLE_SUCCESS',
                levelno=logging.INFO,
                _context=has_entries(
                    partner_id=partner.partner_id,
                    uid=user.uid,
                    minimum_role_required=minimum_role_required,
                ),
            )
        ),
    )


@pytest.mark.asyncio
async def test_role_missing(partner, user, dummy_logs):
    with pytest.raises(RolePermissionError):
        await AuthorizeRoleAction(partner_id=partner.partner_id, user=user).run()

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='AUTH_ROLE_NOT_FOUND',
                levelno=logging.WARNING,
                _context=has_entries(
                    partner_id=partner.partner_id,
                    uid=user.uid,
                    minimum_role_required=RoleType.OWNER,
                ),
            )
        ),
    )


@pytest.mark.asyncio
async def test_role_has_insufficient_permissions(storage, partner, user, role, dummy_logs):
    role.role = RoleType.VIEWER
    await storage.role.save(role)

    with pytest.raises(RolePermissionError):
        await AuthorizeRoleAction(partner_id=partner.partner_id, user=user).run()

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='AUTH_ROLE_INSUFFICIENT_PERMISSIONS',
                levelno=logging.WARNING,
                _context=has_entries(
                    partner_id=partner.partner_id,
                    uid=user.uid,
                    minimum_role_required=RoleType.OWNER,
                    user_role=RoleType.VIEWER,
                ),
            )
        ),
    )
