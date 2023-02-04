from dataclasses import replace

import pytest
from sendr_auth.entities import User

from hamcrest import assert_that, contains_inanyorder, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.partner.list import ListPartnersAction
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import RoleType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.role import Role


@pytest.fixture
async def viewer_partner(storage, uid, partner):
    entity = replace(partner, partner_id=None)
    v_partner = await storage.partner.create(entity)
    role = await storage.role.create(Role(partner_id=v_partner.partner_id, uid=uid, role=RoleType.VIEWER))
    v_partner.role = role
    return v_partner


@pytest.mark.asyncio
async def test_list_partners(partner, viewer_partner, role, user):
    partner.role = role

    partners = await ListPartnersAction(user=user).run()

    assert_that(partners, contains_inanyorder(partner, viewer_partner))


@pytest.mark.asyncio
async def test_filter_partners_by_role(partner, viewer_partner, role, user):
    partner.role = role

    owner_partners = await ListPartnersAction(user=user, user_role=RoleType.OWNER).run()
    viewer_partners = await ListPartnersAction(user=user, user_role=RoleType.VIEWER).run()

    assert_that(owner_partners, equal_to([partner]))
    assert_that(viewer_partners, equal_to([viewer_partner]))


@pytest.mark.asyncio
async def test_does_not_expose_partners_to_unrelated_uid(viewer_partner, role, user):
    another_user = User(uid=user.uid + 1)
    partners = await ListPartnersAction(user=another_user).run()

    assert_that(partners, equal_to([]))
