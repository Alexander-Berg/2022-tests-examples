from datetime import datetime, timezone
from uuid import UUID

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.cms.merchant.create import CreateMerchantForCMSAction
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.merchant import Merchant
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin import OriginBackbone
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.partner import Contact, RegistrationData


@pytest.mark.asyncio
async def test_success(app, disable_tvm_checking, params):
    response = await app.post('/api/web/v1/cms/merchants', json=params)

    assert_that(response.status, equal_to(200))
    data = await response.json()
    assert_that(
        data,
        equal_to(
            {
                'status': 'success',
                'code': 200,
                'data': {
                    'merchant': {
                        'merchant_id': '8c3edc6f-b25f-420d-9126-6fdbbd719ce9',
                        'partner_id': 'b4ffedc8-092d-45c0-8f91-a8adf1b7ae35',
                        'created': '2020-12-30T23:59:59+00:00',
                        'updated': '2020-12-30T23:59:59+00:00',
                        'name': 'merchant-name',
                        'origins': [
                            {'origin': 'https://a.test'},
                        ],
                        'callback_url': None,
                        'url': None,
                        'delivery_integration_params': {
                            'yandex_delivery': None,
                            'measurements': None,
                        },
                    }
                },
            }
        ),
    )


@pytest.mark.asyncio
async def test_calls_create_merchant(app, disable_tvm_checking, mock_create_merchant, params, user):
    await app.post('/api/web/v1/cms/merchants', json=params)

    mock_create_merchant.assert_run_once_with(
        user=user,
        partner_name='partner',
        origins=[OriginBackbone(origin='https://origin.test')],
        partner_registration_data=RegistrationData(
            contact=Contact(
                email='email@test',
                phone='+1(234)567890',
                first_name='first-name',
                last_name='last-name',
                middle_name='middle-name',
            ),
            tax_ref_number='100000',
        ),
        callback_url=None,
    )


@pytest.mark.asyncio
async def test_calls_create_merchant__when_optionals_are_omitted(
    app, disable_tvm_checking, mock_create_merchant, params, user
):
    del params['partner_registration_data']

    await app.post('/api/web/v1/cms/merchants', json=params)

    mock_create_merchant.assert_run_once_with(
        user=user,
        partner_name='partner',
        origins=[OriginBackbone(origin='https://origin.test')],
        callback_url=None,
    )


@pytest.mark.parametrize(
    'required, error',
    (
        ('partner_name', {'partner_name': ['Missing data for required field.']}),
        ('origins', {'origins': {'0': {'origin': ['Missing data for required field.']}}}),
    ),
)
@pytest.mark.asyncio
async def test_validates_required(app, disable_tvm_checking, mock_create_merchant, params, required, error):
    del params[required]

    response = await app.post('/api/web/v1/cms/merchants', json=params)

    assert_that(
        await response.json(),
        equal_to(
            {
                'status': 'fail',
                'code': 400,
                'data': {
                    'message': 'SCHEMA_VALIDATION_ERROR',
                    'params': error,
                },
            }
        ),
    )


@pytest.fixture
def params():
    return {
        'partner_name': 'partner',
        'origins': [{'origin': 'https://origin.test'}],
        'partner_registration_data': {
            'contact': {
                'email': 'email@test',
                'phone': '+1(234)567890',
                'first_name': 'first-name',
                'last_name': 'last-name',
                'middle_name': 'middle-name',
            },
            'tax_ref_number': '100000',
        },
    }


@pytest.fixture(autouse=True)
def mock_create_merchant(mock_action):
    return mock_action(
        CreateMerchantForCMSAction,
        Merchant(
            merchant_id=UUID('8c3edc6f-b25f-420d-9126-6fdbbd719ce9'),
            created=datetime(2020, 12, 30, 23, 59, 59, tzinfo=timezone.utc),
            updated=datetime(2020, 12, 30, 23, 59, 59, tzinfo=timezone.utc),
            name='merchant-name',
            partner_id=UUID('b4ffedc8-092d-45c0-8f91-a8adf1b7ae35'),
            origins=[OriginBackbone(origin='https://a.test')],
        ),
    )
