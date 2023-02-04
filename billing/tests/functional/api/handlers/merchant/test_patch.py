import pytest
from pay.lib.entities.cart import Measurements
from pay.lib.entities.contact import Contact
from pay.lib.entities.shipping import Address, ShippingWarehouse

from sendr_pytest.matchers import convert_then_match, equal_to

from hamcrest import assert_that, has_properties

from billing.yandex_pay_admin.yandex_pay_admin.core.entities.enums import PayBackendType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import RoleType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.merchant import Merchant, YandexDeliveryParams
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
def mock_plus_prod(mock_pay_plus_backend_put_merchant):
    return mock_pay_plus_backend_put_merchant(PayBackendType.PRODUCTION)


@pytest.fixture
def mock_plus_sandbox(mock_pay_plus_backend_put_merchant):
    return mock_pay_plus_backend_put_merchant(PayBackendType.SANDBOX)


@pytest.fixture(autouse=True)
def mock_pay_backends(mock_pay_backend_put_merchant, mock_plus_prod, mock_plus_sandbox):
    mock_pay_backend_put_merchant(PayBackendType.PRODUCTION)
    mock_pay_backend_put_merchant(PayBackendType.SANDBOX)


@pytest.fixture
def url(partner, merchant):
    return f'/api/web/v1/partners/{partner.partner_id}/merchants/{merchant.merchant_id}'


@pytest.mark.asyncio
async def test_success(merchant, url, app, storage, mock_plus_prod):
    delivery_params = {
        'yandex_delivery': {
            'oauth_token': 'token',
            'autoaccept': True,
            'warehouses': [
                {
                    'address': {'country': 'Russia', 'locality': 'Moscow', 'building': '1'},
                    'contact': {'email': 'email'},
                    'emergency_contact': {'phone': 'phone'},
                }
            ],
        },
        'measurements': {'length': 1.0, 'height': 2.0, 'width': 3.0, 'weight': 4.0},
    }

    r = await app.patch(
        url,
        json={
            'name': 'new name',
            'callback_url': 'https://call.back',
            'delivery_integration_params': delivery_params,
        },
        raise_for_status=True,
    )

    stored = await storage.merchant.get(merchant.merchant_id)
    assert_that(
        stored,
        has_properties(
            name='new name',
            delivery_integration_params=has_properties(
                yandex_delivery=has_properties(
                    autoaccept=True,
                    warehouses=[
                        ShippingWarehouse(
                            address=Address(country='Russia', locality='Moscow', building='1'),
                            contact=Contact(email='email'),
                            emergency_contact=Contact(phone='phone'),
                        ),
                    ],
                    oauth_token=convert_then_match(
                        YandexDeliveryParams.oauth_token_crypter.decrypt,
                        'token',
                    ),
                ),
                measurements=Measurements(length=1, height=2, width=3, weight=4),
            ),
        ),
    )

    del delivery_params['yandex_delivery']['oauth_token']
    assert_that(
        await r.json(),
        equal_to(
            {
                'status': 'success',
                'code': 200,
                'data': {
                    'merchant_id': str(merchant.merchant_id),
                    'name': 'new name',
                    'partner_id': str(merchant.partner_id),
                    'callback_url': 'https://call.back',
                    'url': None,
                    'delivery_integration_params': delivery_params,
                    'created': is_datetime_with_tz(),
                    'updated': is_datetime_with_tz(),
                },
            }
        ),
    )

    delivery_params['yandex_delivery'][
        'encrypted_oauth_token'
    ] = stored.delivery_integration_params.yandex_delivery.oauth_token

    assert_that(
        mock_plus_prod.call_args.kwargs['json'],
        equal_to(
            {
                'name': 'new name',
                'origins': [],
                'partner_id': str(merchant.partner_id),
                'callback_url': 'https://call.back',
                'delivery_integration_params': delivery_params,
            }
        ),
    )


@pytest.mark.asyncio
async def test_viewer_cannot_update_merchant(storage, role, merchant, app, url):
    role.role = RoleType.VIEWER
    await storage.role.save(role)

    r = await app.patch(url, json={'name': 'new name'})

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


@pytest.mark.asyncio
async def test_cannot_update_merchant_if_role_missing(storage, role, merchant, url, app):
    await storage.role.delete(role)

    r = await app.patch(url, json={'name': 'new name'})

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
