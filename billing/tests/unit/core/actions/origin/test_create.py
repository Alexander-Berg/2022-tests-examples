from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.create import CreateOriginAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.load import GetOriginWithLazyFieldsAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.revision import BumpOriginRevisionAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.pay_backend.put_merchant import PayBackendPutMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.role import AuthorizeRoleAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import (
    MerchantNotFoundError,
    OriginAlreadyExistsError,
    PartnerNotFoundError,
)
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin import Origin


@pytest.mark.asyncio
async def test_returned(params, mock_get_origin):
    returned = await CreateOriginAction(**params).run()

    assert_that(returned, equal_to(mock_get_origin.action_return_value))


@pytest.mark.asyncio
async def test_creates_origin(storage, params, merchant):
    await CreateOriginAction(**params).run()

    origin = (await storage.origin.find_by_merchant_id(merchant.merchant_id))[0]

    assert_that(
        origin,
        equal_to(
            Origin(
                merchant_id=merchant.merchant_id,
                revision=0,
                origin_id=origin.origin_id,
                origin='https://a.test',
                created=origin.created,
                updated=origin.created,
            )
        ),
    )


@pytest.mark.asyncio
async def test_calls_put_merchant(params, mock_put_merchant, merchant):
    await CreateOriginAction(**params).run()

    mock_put_merchant.assert_run_once_with(merchant=merchant, force_add_origin=False)


@pytest.mark.asyncio
async def test_calls_authorize_role(params, user, mock_authorize_role, merchant):
    await CreateOriginAction(**params).run()

    mock_authorize_role.assert_run_once_with(partner_id=merchant.partner_id, user=user)


@pytest.mark.asyncio
async def test_calls_bump_origin_revision(storage, params, mock_bump_origin_revision, merchant):
    await CreateOriginAction(**params).run()

    origin = (await storage.origin.find_by_merchant_id(merchant.merchant_id))[0]

    mock_bump_origin_revision.assert_run_once_with(origin_id=origin.origin_id)


@pytest.mark.asyncio
async def test_partner_not_found(storage, params):
    with pytest.raises(PartnerNotFoundError):
        params['partner_id'] = uuid4()
        await CreateOriginAction(**params).run()


@pytest.mark.asyncio
async def test_merchant_not_found(storage, params):
    with pytest.raises(MerchantNotFoundError):
        params['merchant_id'] = uuid4()
        await CreateOriginAction(**params).run()


@pytest.mark.asyncio
async def test_origin_already_exists(storage, params):
    params['origin'] = 'https://b.test'
    await CreateOriginAction(**params).run()

    with pytest.raises(OriginAlreadyExistsError):
        params['origin'] = 'https://b.test'
        await CreateOriginAction(**params).run()


@pytest.fixture(autouse=True)
def mock_get_origin(mock_action):
    return mock_action(GetOriginWithLazyFieldsAction)


@pytest.fixture(autouse=True)
def mock_put_merchant(mock_action):
    return mock_action(PayBackendPutMerchantAction)


@pytest.fixture(autouse=True)
def mock_authorize_role(mock_action):
    return mock_action(AuthorizeRoleAction)


@pytest.fixture(autouse=True)
def mock_bump_origin_revision(mock_action):
    return mock_action(BumpOriginRevisionAction)


@pytest.fixture
def params(partner, user, merchant):
    return {
        'user': user,
        'partner_id': partner.partner_id,
        'merchant_id': merchant.merchant_id,
        'origin': 'https://a.test',
    }
