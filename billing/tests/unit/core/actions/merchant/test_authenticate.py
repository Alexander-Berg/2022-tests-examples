from uuid import UUID, uuid4

import pytest
import yenv

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant.authenticate import AuthenticateMerchantAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant.create_or_update import CreateOrUpdateMerchantAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import MerchantAuthenticationError
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant_key import MerchantKey


@pytest.fixture
async def merchant():
    return await CreateOrUpdateMerchantAction(merchant_id=uuid4(), name='some_name', origins=[]).run()


@pytest.fixture
async def merchant_key(merchant, storage):
    return await storage.merchant_key.create(MerchantKey.create(merchant_id=merchant.merchant_id, key='key'))


@pytest.fixture
def yenv_type_sandbox():
    _type = yenv.type
    yenv.type = 'sandbox'
    yield
    yenv.type = _type


@pytest.mark.asyncio
async def test_correct_key(merchant_key):
    header = f'Api-Key {merchant_key.merchant_id.hex}.key'
    assert_that(
        await AuthenticateMerchantAction(authorization_header=header).run(),
        equal_to(merchant_key.merchant_id)
    )


@pytest.mark.asyncio
async def test_sandbox_correct_key(yenv_type_sandbox):
    header = 'Api-Key 00000000ffff44449999cccccccccccc'
    assert_that(
        await AuthenticateMerchantAction(authorization_header=header).run(),
        equal_to(UUID('00000000ffff44449999cccccccccccc'))
    )


@pytest.mark.asyncio
async def test_sandbox_malformed_key(yenv_type_sandbox, merchant_key):
    header = f'Api-Key {merchant_key.merchant_id.hex}.key'

    with pytest.raises(MerchantAuthenticationError) as exc_info:
        await AuthenticateMerchantAction(authorization_header=header).run()

    assert_that(
        exc_info.value.description,
        equal_to('Malformed API key. Are you trying to use a production key in sandbox?'),
    )


@pytest.mark.asyncio
async def test_key_does_not_exists(merchant_key):
    header = f'Api-Key {merchant_key.merchant_id.hex}.missing'

    with pytest.raises(MerchantAuthenticationError) as exc_info:
        await AuthenticateMerchantAction(authorization_header=header).run()

    assert_that(exc_info.value.description, equal_to('Invalid API key'))


@pytest.mark.asyncio
async def test_deleted_key(storage, merchant_key):
    merchant_key.deleted = True
    await storage.merchant_key.save(merchant_key)
    header = f'Api-Key {merchant_key.merchant_id.hex}.key'

    with pytest.raises(MerchantAuthenticationError) as exc_info:
        await AuthenticateMerchantAction(authorization_header=header).run()

    assert_that(exc_info.value.description, equal_to('Invalid API key'))


@pytest.mark.asyncio
async def test_no_authorization_header():
    with pytest.raises(MerchantAuthenticationError) as exc_info:
        await AuthenticateMerchantAction(authorization_header=None).run()

    assert_that(exc_info.value.description, equal_to('Authorization header is missing'))


@pytest.mark.asyncio
async def test_malformed_authorization_header():
    with pytest.raises(MerchantAuthenticationError) as exc_info:
        await AuthenticateMerchantAction(authorization_header='token').run()

    assert_that(exc_info.value.description, equal_to('Authorization header is malformed'))


@pytest.mark.asyncio
async def test_incorrect_realm():
    with pytest.raises(MerchantAuthenticationError) as exc_info:
        await AuthenticateMerchantAction(authorization_header='OAuth token').run()

    assert_that(exc_info.value.description, equal_to('Auth kind "OAuth" is not supported'))


@pytest.mark.asyncio
async def test_malformed_merchant_id():
    with pytest.raises(MerchantAuthenticationError) as exc_info:
        await AuthenticateMerchantAction(authorization_header='Api-Key token.key').run()

    assert_that(exc_info.value.description, equal_to('Malformed API key'))


@pytest.mark.asyncio
async def test_sandbox_key_in_production():
    header = 'Api-Key 00000000ffff44449999cccccccccccc'

    with pytest.raises(MerchantAuthenticationError) as exc_info:
        await AuthenticateMerchantAction(authorization_header=header).run()

    assert_that(
        exc_info.value.description,
        equal_to('Malformed API key. Are you trying to use a sandbox key in production?'),
    )
