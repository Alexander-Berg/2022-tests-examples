from uuid import uuid4

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant.create_key import CreateMerchantKeyAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant.delete_key import DeleteMerchantKeyAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant.get_keys import GetMerchantKeysAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import CoreKeyLimitExceededError
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant_key import MerchantKey


@pytest.fixture
def merchant_id():
    return uuid4()


@pytest.fixture
def url(merchant_id):
    return f'/api/internal/v1/merchants/{merchant_id}/keys'


class TestCreate:
    @pytest.mark.asyncio
    async def test_success(self, app, url, merchant_id, mock_action):
        key = {
            'key_id': uuid4(),
            'value': 'x',
            'created': utcnow(),
            'updated': utcnow(),
        }
        mock = mock_action(CreateMerchantKeyAction, key)
        r = await app.post(url, raise_for_status=True)
        data = await r.json()

        mock.assert_called_once_with(merchant_id=merchant_id)
        assert_that(data['data']['key'], equal_to({
            'key_id': str(key['key_id']),
            'value': key['value'],
            'created': key['created'].isoformat(),
            'updated': key['updated'].isoformat(),
        }))

    @pytest.mark.asyncio
    async def test_error(self, app, url, merchant_id, mock_action):
        mock_action(CreateMerchantKeyAction, CoreKeyLimitExceededError)
        r = await app.post(url)
        data = await r.json()

        assert_that(r.status, equal_to(400))
        assert_that(data, equal_to({'status': 'fail', 'data': {'message': 'TOO_MANY_API_KEYS'}, 'code': 400}))


class TestGetKeys:
    @pytest.mark.asyncio
    async def test_success(self, app, url, merchant_id, mock_action):
        key = MerchantKey(merchant_id=merchant_id, key_hash='x', key_id=uuid4(), created=utcnow(), updated=utcnow())
        mock = mock_action(GetMerchantKeysAction, [key])
        r = await app.get(url, raise_for_status=True)
        data = await r.json()

        mock.assert_called_once_with(merchant_id=merchant_id)
        assert_that(data['data']['keys'], equal_to([
            {'key_id': str(key.key_id), 'created': key.created.isoformat(), 'updated': key.updated.isoformat()}
        ]))


class TestDeleteKey:
    @pytest.mark.asyncio
    async def test_success(self, app, url, merchant_id, mock_action):
        key_id = uuid4()
        mock = mock_action(DeleteMerchantKeyAction)
        await app.delete(
            url,
            json={'key_id': str(key_id)},
            raise_for_status=True,
        )
        mock.assert_called_once_with(merchant_id=merchant_id, key_id=key_id)
