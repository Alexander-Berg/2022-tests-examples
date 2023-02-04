from uuid import UUID, uuid4

import pytest
from pay.lib.entities.cart import Measurements
from pay.lib.entities.contact import Contact
from pay.lib.entities.shipping import Address, ShippingWarehouse

from sendr_pytest.matchers import convert_then_match
from sendr_pytest.mocks import explain_call_asserts  # noqa
from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_entries, match_equality

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant.create_or_update import CreateOrUpdateMerchantAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant.get import GetMerchantAction
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import (
    DeliveryIntegrationParams,
    MerchantWithRelated,
    YandexDeliveryParams,
    create_oauth_token_crypter,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant_origin import MerchantOrigin

WAREHOUSE = ShippingWarehouse(
    address=Address(country='Russia', locality='Moscow', building='1'),
    contact=Contact(email='email'),
    emergency_contact=Contact(phone='phone'),
)
WAREHOUSE_SERIALIZED = {
    'address': {'country': 'Russia', 'locality': 'Moscow', 'building': '1'},
    'contact': {'email': 'email'},
    'emergency_contact': {'phone': 'phone'},
}


def match_encrypted_token(token):
    crypter = create_oauth_token_crypter()
    return match_equality(
        convert_then_match(crypter.decrypt, equal_to(token)),
    )


@pytest.mark.asyncio
async def test_get(
    internal_app,
    action_result,
    mock_action,
    mock_internal_tvm,
    expected_handler_result,
):
    mock_action(GetMerchantAction, action_result)

    r = await internal_app.get(f'api/internal/v1/merchants/{uuid4()}')
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to(expected_handler_result))


@pytest.mark.asyncio
async def test_put(
    internal_app,
    mock_action,
    mock_internal_tvm,
    merchant_put_data,
    action_result,
    expected_handler_result,
):
    mock_action(CreateOrUpdateMerchantAction, action_result)

    r = await internal_app.put(
        f'api/internal/v1/merchants/{uuid4()}',
        json=merchant_put_data,
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to(expected_handler_result))


@pytest.mark.asyncio
async def test_put_no_oauth_token(internal_app, mock_action, mock_internal_tvm, merchant_put_data):
    mock_action(CreateOrUpdateMerchantAction, action_result)
    del merchant_put_data['delivery_integration_params']['yandex_delivery']['oauth_token']

    r = await internal_app.put(
        f'api/internal/v1/merchants/{uuid4()}',
        json=merchant_put_data,
    )
    json = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(
        json['data']['params'],
        has_entries({
            'delivery_integration_params': has_entries({
                'yandex_delivery': has_entries({
                    '_schema': ["The 'oauth_token' or 'encrypted_oauth_token' must be specified"]
                })
            })
        })
    )


@pytest.mark.asyncio
async def test_put_no_name(internal_app, mock_action, mock_internal_tvm, merchant_put_data):
    mock_action(CreateOrUpdateMerchantAction, action_result)
    del merchant_put_data['name']

    r = await internal_app.put(
        f'api/internal/v1/merchants/{uuid4()}',
        json=merchant_put_data,
    )
    json = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(
        json['data']['params'],
        has_entries({
            'name': ['Missing data for required field.'],
        })
    )


@pytest.mark.asyncio
async def test_put_no_origins(internal_app, mock_action, mock_internal_tvm, merchant_put_data):
    mock_action(CreateOrUpdateMerchantAction, action_result)
    del merchant_put_data['origins']

    r = await internal_app.put(
        f'api/internal/v1/merchants/{uuid4()}',
        json=merchant_put_data,
    )
    json = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(
        json['data']['params'],
        has_entries({
            'origins': has_entries({
                '0': has_entries({
                    'origin': ['Missing data for required field.']
                })
            })
        })
    )


@pytest.mark.asyncio
async def test_put_empty_origin(internal_app, mock_action, mock_internal_tvm, merchant_put_data):
    mock_action(CreateOrUpdateMerchantAction, action_result)
    merchant_put_data['origins'] = [{}]

    r = await internal_app.put(
        f'api/internal/v1/merchants/{uuid4()}',
        json=merchant_put_data,
    )
    json = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(
        json['data']['params'],
        has_entries({
            'origins': has_entries({
                '0': has_entries({
                    'origin': ['Missing data for required field.']
                })
            })
        })
    )


@pytest.mark.asyncio
async def test_put_no_callback_url(internal_app, mock_action, mock_internal_tvm, merchant_put_data):
    mock_action(CreateOrUpdateMerchantAction, action_result)
    del merchant_put_data['callback_url']

    r = await internal_app.put(
        f'api/internal/v1/merchants/{uuid4()}',
        json=merchant_put_data,
    )

    assert_that(r.status, equal_to(200))


@pytest.mark.parametrize('delivery_params, expected_delivery_params', (
    (None, None),
    ({'yandex_delivery': None}, DeliveryIntegrationParams()),
    (
        {'yandex_delivery': {'oauth_token': 'secret', 'autoaccept': True}},
        DeliveryIntegrationParams(YandexDeliveryParams(oauth_token=match_encrypted_token('secret'), autoaccept=True)),
    ),
    (
        {'yandex_delivery': {'encrypted_oauth_token': 'secret', 'warehouses': [WAREHOUSE_SERIALIZED]}},
        DeliveryIntegrationParams(YandexDeliveryParams(oauth_token='secret', autoaccept=False, warehouses=[WAREHOUSE])),
    ),
))
@pytest.mark.asyncio
async def test_should_call_action_with_expected_args(
    internal_app,
    mock_action,
    mock_internal_tvm,
    merchant_put_data,
    delivery_params,
    expected_delivery_params,
):
    mocked_action = mock_action(CreateOrUpdateMerchantAction, action_result)
    merchant_id = uuid4()
    merchant_put_data['delivery_integration_params'] = delivery_params

    await internal_app.put(
        f'api/internal/v1/merchants/{merchant_id}',
        json=merchant_put_data,
    )

    mocked_action.assert_called_once_with(
        origins=[{'origin': 'https://a.test'}],
        name='name',
        callback_url='https://test.back',
        merchant_id=merchant_id,
        partner_id=UUID(merchant_put_data['partner_id']),
        delivery_integration_params=expected_delivery_params,
    )


@pytest.fixture
def action_result(rands, entity_warehouse):
    return MerchantWithRelated(
        merchant_id=uuid4(),
        partner_id=uuid4(),
        name=rands(),
        created=utcnow(),
        updated=utcnow(),
        origins=[
            MerchantOrigin(
                merchant_id=uuid4(),
                origin='https://origin.test:443',
                created=utcnow(),
            )
        ],
        callback_url='https://callback.test',
        delivery_integration_params=DeliveryIntegrationParams(
            yandex_delivery=YandexDeliveryParams(
                oauth_token='secret',
                autoaccept=True,
                warehouses=[WAREHOUSE],
            ),
            measurements=Measurements(length=1, height=2, width=3, weight=4),
        )
    )


@pytest.fixture
def expected_handler_result(action_result):
    return {
        'status': 'success',
        'code': 200,
        'data': {
            'merchant': {
                'merchant_id': str(action_result.merchant_id),
                'name': action_result.name,
                'created': action_result.created.isoformat(),
                'origins': [
                    {'origin': origin.origin, 'created': origin.created.isoformat(), 'is_blocked': False}
                    for origin in action_result.origins
                ],
                'is_blocked': False,
                'callback_url': action_result.callback_url,
                'partner_id': str(action_result.partner_id),
                'delivery_integration_params': {
                    'yandex_delivery': {
                        'autoaccept': True,
                        'warehouses': [WAREHOUSE_SERIALIZED],
                    },
                    'measurements': {
                        'length': 1.,
                        'height': 2.,
                        'width': 3.,
                        'weight': 4.,
                    },
                },
            }
        }
    }


@pytest.fixture
def merchant_put_data():
    return {
        'name': 'name',
        'origins': [{'origin': 'https://a.test'}],
        'callback_url': 'https://test.back',
        'partner_id': str(uuid4()),
        'delivery_integration_params': {
            'yandex_delivery': {
                'oauth_token': 'secret',
                'autoaccept': True,
                'warehouses': [WAREHOUSE_SERIALIZED],
            },
            'measurements': {
                'length': 1.,
                'height': 2.,
                'width': 3.,
                'weight': 4.,
            },
        }
    }
