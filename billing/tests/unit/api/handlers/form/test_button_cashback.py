from decimal import Decimal

import pytest

from hamcrest import assert_that, equal_to, has_entries

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.category import GetCashbackCategoryAction


@pytest.fixture
def api_url():
    return '/api/public/v1/button-cashback'


@pytest.fixture
def action(mock_action):
    return mock_action(GetCashbackCategoryAction)


@pytest.fixture
def body_params():
    return {
        'merchant': {
            'id': '933c5784-9685-4db0-832a-90cb61b9646a',
            'name': 'name',
            'url': 'https://url.test',
        }
    }


@pytest.fixture
def app(public_app):
    return public_app


@pytest.mark.asyncio
async def test_returned_cashback(app, api_url, body_params, mock_action):
    mock_action(
        GetCashbackCategoryAction,
        Decimal('0.05'),
    )

    r = await app.post(api_url, json=body_params)
    data = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(
        data,
        has_entries({
            'status': 'success',
            'code': 200,
            'data': {
                'category': '0.05',
            }
        })
    )
