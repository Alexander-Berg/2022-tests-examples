import pytest
import yarl

from hamcrest import assert_that, equal_to, has_entries

from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import RoleType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.merchant import Merchant

pytestmark = pytest.mark.usefixtures('mock_app_authentication', 'setup_interactions_tvm')


@pytest.fixture
async def merchant(storage, partner, role):
    return await storage.merchant.create(
        Merchant(
            name='merchant name',
            partner_id=partner.partner_id,
        )
    )


@pytest.fixture
def url(merchant):
    return f'/api/web/v1/merchants/{merchant.merchant_id}/keys'


@pytest.fixture
def plus_url(merchant, yandex_pay_admin_settings):
    url = yarl.URL(yandex_pay_admin_settings.YANDEX_PAY_PLUS_BACKEND_PRODUCTION_URL)
    return url / f'api/internal/v1/merchants/{merchant.merchant_id}/keys'


@pytest.mark.asyncio
@pytest.mark.parametrize('role_type', list(RoleType))
async def test_get_keys(storage, aioresponses_mocker, url, plus_url, app, role, role_type):
    role.role = role_type
    await storage.role.save(role)

    aioresponses_mocker.get(
        plus_url,
        payload={
            'data': {
                'keys': [{'key_id': '9c818d51-97d8-49c3-9063-1d55f9b44cd2', 'created': '2022-02-22T20:22:00+00:00'}]
            }
        },
    )

    r = await app.get(url)
    response = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(
        response['data'],
        equal_to(
            {
                'keys': [
                    {
                        'key_id': '9c818d51-97d8-49c3-9063-1d55f9b44cd2',
                        'created': '2022-02-22T20:22:00+00:00',
                        'value': None,
                    }
                ]
            }
        ),
    )


@pytest.mark.asyncio
async def test_create_key(storage, aioresponses_mocker, url, plus_url, app, role):
    aioresponses_mocker.post(
        plus_url,
        payload={
            'data': {
                'key': {
                    'key_id': '9c818d51-97d8-49c3-9063-1d55f9b44cd2',
                    'created': '2022-02-22T20:22:00+00:00',
                    'value': '1',
                }
            }
        },
    )

    r = await app.post(url)
    response = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(
        response['data'],
        equal_to(
            {
                'key': {
                    'key_id': '9c818d51-97d8-49c3-9063-1d55f9b44cd2',
                    'created': '2022-02-22T20:22:00+00:00',
                    'value': '1',
                }
            }
        ),
    )


@pytest.mark.asyncio
async def test_create_key_error(storage, aioresponses_mocker, url, plus_url, app, role):
    aioresponses_mocker.post(
        plus_url,
        status=400,
        payload={
            'code': 400,
            'data': {'message': 'TOO_MANY_API_KEYS'},
            'status': 'fail',
        },
    )

    r = await app.post(url)
    response = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(
        response,
        equal_to({'data': {'message': 'TOO_MANY_API_KEYS'}, 'code': 400, 'status': 'fail'}),
    )


@pytest.mark.asyncio
async def test_delete_key(storage, aioresponses_mocker, url, plus_url, app, role):
    aioresponses_mocker.delete(plus_url, payload={})

    r = await app.delete(url, json={'key_id': '9c818d51-97d8-49c3-9063-1d55f9b44cd2'})

    assert_that(r.status, equal_to(200))
    assert_that(
        aioresponses_mocker.requests[('DELETE', plus_url)][0].kwargs,
        has_entries(
            {
                'json': {'key_id': '9c818d51-97d8-49c3-9063-1d55f9b44cd2'},
            }
        ),
    )
