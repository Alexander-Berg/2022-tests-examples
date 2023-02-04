from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to, has_entries, matches_regexp

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant_key import MerchantKey
from billing.yandex_pay_plus.yandex_pay_plus.tests.utils import is_datetime_with_tz, is_uuid4


@pytest.fixture
def merchant_id():
    return uuid4()


@pytest.fixture
def url(merchant_id):
    return f'/api/internal/v1/merchants/{merchant_id}/keys'


@pytest.fixture(autouse=True)
async def merchant(app, merchant_id):
    await app.put(
        f'/api/internal/v1/merchants/{merchant_id}',
        headers={'x-ya-service-ticket': 'dummy-service-ticket'},
        json={
            'name': 'merchant name',
            'origins': [{'origin': 'https://origin.test'}],
        },
        raise_for_status=True,
    )


@pytest.fixture
async def merchant_keys(storage, merchant_id):
    return [
        await storage.merchant_key.create(MerchantKey.create(merchant_id, '1')),
        await storage.merchant_key.create(MerchantKey.create(merchant_id, '2')),
    ]


@pytest.mark.asyncio
async def test_create_key(app, url):
    r = await app.post(url, raise_for_status=True)
    data = await r.json()

    assert_that(
        data['data']['key'],
        has_entries(
            key_id=is_uuid4,
            value=matches_regexp(r'[a-f0-9]{32}\.[a-zA-Z0-9_\-]{32}'),
            created=is_datetime_with_tz,
            updated=is_datetime_with_tz,
        ),
    )


@pytest.mark.asyncio
async def test_list_keys(app, merchant_keys, url):
    r = await app.get(url, raise_for_status=True)
    data = await r.json()

    assert_that(
        data['data']['keys'],
        equal_to(
            [
                {'key_id': str(k.key_id), 'created': is_datetime_with_tz, 'updated': is_datetime_with_tz}
                for k in merchant_keys
            ]
        ),
    )


@pytest.mark.asyncio
async def test_delete_key(app, storage, merchant_keys, url):
    await app.delete(
        url,
        json={'key_id': str(merchant_keys[0].key_id)},
        raise_for_status=True,
    )
    key = await storage.merchant_key.get(merchant_keys[0].key_id)
    assert key.deleted is True

    r = await app.get(url, raise_for_status=True)
    data = await r.json()

    assert_that(
        data['data']['keys'],
        equal_to(
            [
                {'key_id': str(k.key_id), 'created': is_datetime_with_tz, 'updated': is_datetime_with_tz}
                for k in merchant_keys[1:]
            ]
        ),
    )
