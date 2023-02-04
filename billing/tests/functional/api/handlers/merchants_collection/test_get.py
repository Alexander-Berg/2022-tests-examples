import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import RoleType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.merchant import Merchant
from billing.yandex_pay_admin.yandex_pay_admin.tests.utils import is_datetime_with_tz

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
def url(partner):
    return f'/api/web/v1/partners/{partner.partner_id}/merchants'


@pytest.mark.asyncio
@pytest.mark.parametrize('role_type', list(RoleType))
async def test_returned(storage, merchant, url, app, role, role_type):
    role.role = role_type
    await storage.role.save(role)

    r = await app.get(url)

    assert_that(r.status, equal_to(200))
    data = await r.json()
    assert_that(
        data,
        equal_to(
            {
                'status': 'success',
                'code': 200,
                'data': {
                    'merchants': [
                        {
                            'merchant_id': str(merchant.merchant_id),
                            'name': 'merchant name',
                            'partner_id': str(merchant.partner_id),
                            'callback_url': None,
                            'url': None,
                            'delivery_integration_params': {'yandex_delivery': None, 'measurements': None},
                            'created': is_datetime_with_tz(),
                            'updated': is_datetime_with_tz(),
                        }
                    ]
                },
            }
        ),
    )


@pytest.mark.asyncio
async def test_cannot_list_merchants_if_role_missing(storage, role, merchant, url, app):
    await storage.role.delete(role)

    r = await app.get(url)

    assert_that(r.status, equal_to(403))
    data = await r.json()
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
