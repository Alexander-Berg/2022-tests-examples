from base64 import urlsafe_b64decode

import pytest

from sendr_pytest.matchers import equal_to

from hamcrest import assert_that, has_properties, has_property, match_equality, not_none

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant.create_key import CreateMerchantKeyAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import CoreKeyLimitExceededError
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import Merchant


@pytest.fixture
async def merchant(storage):
    return await storage.merchant.create(Merchant(name='1'))


@pytest.mark.asyncio
async def test_create_key(merchant, storage, mocker):
    mocker.patch('random.randbytes', mocker.Mock(return_value=urlsafe_b64decode('7Xh-3ZlciwG3UpHHW9zYNmBAz7lPb_e2')))
    key = await CreateMerchantKeyAction(merchant.merchant_id).run()

    assert_that(
        key,
        equal_to(dict(
            key_id=match_equality(has_property('version', 4)),
            value=f'{merchant.merchant_id.hex}.7Xh-3ZlciwG3UpHHW9zYNmBAz7lPb_e2',
            created=match_equality(has_property('tzinfo', not_none())),
            updated=match_equality(has_property('tzinfo', not_none())),
        )),
    )
    assert_that(
        await storage.merchant_key.get(key['key_id']),
        has_properties(
            created=key['created'],
            updated=key['updated'],
        )
    )


@pytest.mark.asyncio
async def test_too_many_keys(merchant, storage, yandex_pay_plus_settings):
    yandex_pay_plus_settings.MERCHANT_API_MAX_KEYS = 0

    with pytest.raises(CoreKeyLimitExceededError):
        await CreateMerchantKeyAction(merchant.merchant_id).run()
