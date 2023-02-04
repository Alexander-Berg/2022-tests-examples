import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.entities.enums import PayBackendType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import RoleType
from billing.yandex_pay_admin.yandex_pay_admin.tests.utils import is_datetime_with_tz, is_uuid

pytestmark = pytest.mark.usefixtures('mock_app_authentication', 'setup_interactions_tvm')


@pytest.fixture
def payload():
    return {'name': 'merchant_name'}


@pytest.fixture
def create_merchant(app, payload, partner, role):
    async def _inner(status: int = 200):
        r = await app.post(
            f'/api/web/v1/partners/{partner.partner_id}/merchants',
            json=payload,
        )
        assert_that(r.status, equal_to(status))
        return await r.json()

    return _inner


@pytest.fixture(autouse=True)
def mock_pay_backends(mock_pay_backend_put_merchant, mock_pay_plus_backend_put_merchant):
    mock_pay_backend_put_merchant(PayBackendType.PRODUCTION)
    mock_pay_backend_put_merchant(PayBackendType.SANDBOX)
    mock_pay_plus_backend_put_merchant(PayBackendType.PRODUCTION)
    mock_pay_plus_backend_put_merchant(PayBackendType.SANDBOX)


@pytest.mark.asyncio
async def test_returned(create_merchant):
    data = await create_merchant()

    assert_that(
        data,
        equal_to(
            {
                'status': 'success',
                'code': 200,
                'data': {
                    'merchant_id': is_uuid(),
                    'name': 'merchant_name',
                    'partner_id': is_uuid(),
                    'callback_url': None,
                    'url': None,
                    'delivery_integration_params': {'yandex_delivery': None, 'measurements': None},
                    'created': is_datetime_with_tz(),
                    'updated': is_datetime_with_tz(),
                },
            }
        ),
    )


@pytest.mark.asyncio
async def test_viewer_cannot_create_merchant(storage, role, create_merchant):
    role.role = RoleType.VIEWER
    await storage.role.save(role)

    data = await create_merchant(status=403)

    assert_that(
        data,
        equal_to(
            {
                'status': 'fail',
                'code': 403,
                'data': {
                    'message': 'FORBIDDEN',
                },
            }
        ),
    )


@pytest.mark.asyncio
async def test_cannot_create_merchant_if_role_missing(storage, role, create_merchant):
    await storage.role.delete(role)

    data = await create_merchant(status=403)

    assert_that(
        data,
        equal_to(
            {
                'status': 'fail',
                'code': 403,
                'data': {
                    'message': 'FORBIDDEN',
                },
            }
        ),
    )
