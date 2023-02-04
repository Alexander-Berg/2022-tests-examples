import uuid

import pytest

from hamcrest import assert_that, equal_to, has_entry


@pytest.fixture
def api_url():
    return '/api/public/v1/button-cashback'


@pytest.fixture
def params():
    return {
        'merchant': {
            'id': str(uuid.uuid4()),
            'name': 'the-name',
            'url': 'https://url.test',
        },
    }


@pytest.fixture
def app(public_app, authenticate_client):
    authenticate_client(public_app)
    return public_app


@pytest.mark.asyncio
async def test_get_cashback(app, api_url, yandex_pay_plus_settings, params):
    r = await app.post(
        api_url,
        json=params,
    )
    data = await r.json()

    assert_that(r.status, equal_to(200), data)
    assert_that(
        data,
        has_entry(
            'data',
            has_entry(
                'category',
                '0.03',
            )
        )
    )
