import re
from uuid import uuid4

import pytest
from aioresponses import CallbackResult

from sendr_utils import MISSING

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.base import BaseAction
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import IntegrationStatus
from billing.yandex_pay_plus.yandex_pay_plus.tests.common_conftest import *  # noqa
from billing.yandex_pay_plus.yandex_pay_plus.tests.db import *  # noqa


@pytest.fixture
def db_engine(raw_db_engine):
    return raw_db_engine


@pytest.fixture(autouse=True)
def action_context_setup(dummy_logger, app, db_engine, request_id):
    # 'app' fixture is required to guarantee the execution order
    # since BaseInteractionClient.CONNECTOR is set by web.Application
    BaseAction.setup_context(
        logger=dummy_logger,
        request_id=request_id,
        db_engine=db_engine,
    )
    assert BaseAction.context.storage is None


@pytest.fixture
def mock_internal_tvm(aioresponses_mocker, yandex_pay_plus_settings):
    aioresponses_mocker.get(
        re.compile(f'.*{yandex_pay_plus_settings.TVM_URL}/tvm/checksrv.*'),
        payload={
            'src': yandex_pay_plus_settings.TVM_ALLOWED_SRC[0],
            'dst': yandex_pay_plus_settings.TVM_ALLOWED_SRC[0],
        },
    )


@pytest.fixture
def authenticate_client(mock_sessionid_auth):
    def _authenticate_client(test_client, method='sessionid'):
        if method == 'sessionid':
            test_client.session.cookie_jar.update_cookies({'Session_id': 'UNITTEST_SESSION_ID'})
            return
        raise ValueError('Authentication method not supported')

    return _authenticate_client


@pytest.fixture
def mock_sessionid_auth(aioresponses_mocker, yandex_pay_plus_settings, entity_auth_user):
    def blackbox_callback(url, *, params, **kwargs):
        if params.get('sessionid') == 'UNITTEST_SESSION_ID':
            return CallbackResult(
                status=200,
                payload={
                    'status': {'value': 'VALID'},
                    'error': 'OK',
                    'uid': {
                        'value': entity_auth_user.uid,
                    },
                    'login_id': 'loginid:unittest',
                },
            )
        return CallbackResult(status=400)

    base_url = yandex_pay_plus_settings.BLACKBOX_API_URL.rstrip('/')
    return aioresponses_mocker.get(
        re.compile(fr'{base_url}\?.*method=sessionid.*'),
        callback=blackbox_callback,
        repeat=True,
    )


@pytest.fixture(autouse=True)
def mock_csrf_checker(mocker):
    return mocker.patch('sendr_auth.CsrfChecker.check', mocker.Mock())


@pytest.fixture
def mock_merchant_order_create(aioresponses_zora_mocker, stored_merchant):
    return aioresponses_zora_mocker.post(
        f'{stored_merchant.callback_url}/v1/order/create',
        payload={'status': 'success', 'data': {'orderId': 'real-order-id'}}
    )


@pytest.fixture
def mock_merchant_webhook(aioresponses_zora_mocker, stored_merchant):
    return aioresponses_zora_mocker.post(
        f'{stored_merchant.callback_url}/webhook', payload={'status': 'success'}
    )


@pytest.fixture
def mock_pay_card_info(aioresponses_zora_mocker, yandex_pay_plus_settings):
    return aioresponses_zora_mocker.get(
        re.compile(f'{yandex_pay_plus_settings.YANDEX_PAY_API_URL}/api/internal/v1/user/cards/.*'),
        payload={
            'status': 'success',
            'data': {
                'card_id': '00000000-0000-0000-0000-000000000000',
                'last4': '0000',
                'trust_card_id': 'card-x0000',
                'card_network': 'MIR',
                'issuer_bank': 'BANKROSSIYA',
                'expiration_date': {
                    'month': 1,
                    'year': 3000,
                }
            },
        },
    )


@pytest.fixture
def mock_split_create_order(aioresponses_zora_mocker, yandex_pay_plus_settings):
    return aioresponses_zora_mocker.post(
        re.compile(f'{yandex_pay_plus_settings.SPLIT_API_URL}/order/create(\\?.*)?'),
        payload={
            'order_id': 'split-order-id',
            'checkout_url': 'https://split-checkout-url.test',
        },
    )


@pytest.fixture
def create_order(
    public_app,
    stored_merchant,
    mock_billing_contact,
    mock_shipping_contact,
    mock_shipping_address,
    mock_merchant_order_create,
    mock_merchant_webhook,
    mock_pay_card_info,
    mock_split_create_order,
):
    async def _create_order(**kwargs):
        order = {
            'merchant_id': str(stored_merchant.merchant_id),
            'currency_code': 'XTS',
            'cart': {
                'items': [
                    {
                        'product_id': 'pid1',
                        'title': 'product title',
                        'total': '10',
                        'quantity': {'count': '1'},
                        'receipt': {
                            'tax': 1
                        }
                    }
                ],
                'total': {'amount': '10.0'},
            },
            'payment_method': {
                'method_type': 'CARD',
            },
            'order_amount': '10.00',
            'shipping_address_id': 'ship-a-id',
            'shipping_contact_id': 'ship-c-id',
            'billing_contact_id': 'bill-c-id',
        }
        order.update(kwargs)
        r = await public_app.post(
            '/api/public/v1/orders/create',
            headers={'x-pay-session-id': 'sessid-123'},
            json=order,
            raise_for_status=True,
        )
        return (await r.json())['data']['order']
    return _create_order


@pytest.fixture
def mock_shipping_address(aioresponses_mocker, yandex_pay_plus_settings):
    return aioresponses_mocker.get(
        re.compile(rf'{yandex_pay_plus_settings.PASSPORT_ADDRESSES_URL}/address/get\?id=ship-a-id&.*'),
        payload={
            'id': 'ship-a-id',
            'country': 'passp-country',
            'locality': 'passp-locality',
            'street': 'passp-street',
            'building': 'passp-building',
        }
    )


@pytest.fixture
def mock_shipping_contact(aioresponses_mocker, yandex_pay_plus_settings):
    return aioresponses_mocker.get(
        re.compile(rf'{yandex_pay_plus_settings.PASSPORT_ADDRESSES_URL}/contact/get\?id=ship-c-id&.*'),
        payload={
            'id': 'ship-c-id',
            'phone_number': '+70001112233',
            'email': 'address@email.test',
            'first_name': 'fname',
            'second_name': 'sname',
            'last_name': 'lname',
        }
    )


@pytest.fixture
def mock_billing_contact(aioresponses_mocker, yandex_pay_plus_settings):
    return aioresponses_mocker.get(
        re.compile(rf'{yandex_pay_plus_settings.PASSPORT_ADDRESSES_URL}/contact/get\?id=bill-c-id&.*'),
        payload={
            'id': 'bill-c-id',
            'phone_number': '+70001112233',
            'email': 'address@email.test',
            'first_name': 'fname',
            'second_name': 'sname',
            'last_name': 'lname',
        }
    )


@pytest.fixture
def create_transaction(public_app):
    async def _create_transaction(checkout_order_id, **kwargs):
        r = await public_app.post(
            f'/api/public/v1/orders/{checkout_order_id}/transactions',
            headers={'x-pay-session-id': 'sessid-123', 'x-forwarded-for-y': '192.0.2.1'},
            json={
                'card_id': 'card-x1234',
                'browser_data': {
                    'java_enabled': True,
                    'language': 'ru',
                    'screen_color_depth': 24,
                    'screen_height': 1080,
                    'screen_width': 1960,
                    'timezone': -180,
                    'window_height': 1080,
                    'window_width': 1960
                },
                'challenge_return_path': 'https://challenge.test.ya.ru',
            },
        )
        data = await r.json()
        return data['data']['transaction']
    return _create_transaction


@pytest.fixture
def create_integration(app, mock_internal_tvm, stored_merchant, stored_unittest_psp):
    async def _create_integration(
        merchant_id=stored_merchant.merchant_id,
        psp_id=stored_unittest_psp.psp_id,
        integration_id=MISSING,
        creds='',
        status=IntegrationStatus.DEPLOYED,
    ):
        if integration_id is MISSING:
            integration_id = uuid4()
        r = await app.patch(
            f'/api/internal/v1/integrations/{integration_id}',
            json={
                'merchant_id': str(merchant_id),
                'psp_id': str(psp_id),
                'status': status.value,
                'creds': creds,
            },
        )
        data = await r.json()
        return data['data']['integration']
    return _create_integration
