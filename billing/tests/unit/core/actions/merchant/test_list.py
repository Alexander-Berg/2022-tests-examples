import pytest

from hamcrest import assert_that, contains_inanyorder, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant.list import ListMerchantsAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.role import AuthorizeRoleAction
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import RoleType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.merchant import Merchant


@pytest.fixture
def create_merchant(storage, partner, role):
    async def _inner(name):
        return await storage.merchant.create(
            Merchant(
                name=name,
                partner_id=partner.partner_id,
            )
        )

    return _inner


@pytest.fixture
async def merchant(create_merchant):
    return await create_merchant('merchant name')


@pytest.mark.asyncio
async def test_list_merchants(create_merchant, merchant, partner, user):
    another_merchant = await create_merchant('another name')

    merchants = await ListMerchantsAction(user=user, partner_id=partner.partner_id).run()

    assert_that(merchants, contains_inanyorder(merchant, another_merchant))


@pytest.mark.asyncio
@pytest.mark.usefixtures('role')
async def test_list_no_merchants(partner, user):
    merchants = await ListMerchantsAction(user=user, partner_id=partner.partner_id).run()

    assert_that(merchants, equal_to([]))


@pytest.mark.asyncio
async def test_calls_authorize_action(partner, user, mock_action):
    mock = mock_action(AuthorizeRoleAction)

    await ListMerchantsAction(user=user, partner_id=partner.partner_id).run()

    mock.assert_run_once_with(
        partner_id=partner.partner_id,
        user=user,
        minimum_role_required=RoleType.VIEWER,
    )
