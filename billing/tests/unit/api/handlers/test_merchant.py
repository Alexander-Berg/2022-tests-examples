import uuid
from dataclasses import replace

import pytest
from pay.lib.entities.cart import Measurements

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant.get import GetMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant.update import UpdateMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import MerchantNotFoundError
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.merchant import (
    DeliveryIntegrationParams,
    Merchant,
    YandexDeliveryParams,
)
from billing.yandex_pay_admin.yandex_pay_admin.tests.utils import check_error


@pytest.fixture
def utc_now():
    return utcnow()


@pytest.fixture
def merchant_entity(partner, role, utc_now):
    merchant_id = uuid.uuid4()
    return Merchant(
        merchant_id=merchant_id,
        partner_id=partner.partner_id,
        name='merchant_name',
        created=utc_now,
        updated=utc_now,
        delivery_integration_params=DeliveryIntegrationParams(
            yandex_delivery=YandexDeliveryParams(
                autoaccept=False,
                oauth_token='token',
            ),
            measurements=Measurements(length=1, height=2, width=3, weight=4),
        ),
    )


@pytest.fixture
def url(partner, merchant_entity):
    return f'/api/web/v1/partners/{partner.partner_id}/merchants/{merchant_entity.merchant_id}'


class TestUpdateMerchant:
    @pytest.mark.asyncio
    async def test_returned(self, url, merchant_entity, user, app, mock_action, utc_now):
        now = utcnow()
        updated = replace(merchant_entity, name='new name', callback_url='https://call.back', updated=now)
        mock_action(UpdateMerchantAction, updated)

        r = await app.patch(url, json={'name': 'new name'})

        assert_that(r.status, equal_to(200))
        data = await r.json()
        assert_that(
            data,
            equal_to(
                {
                    'status': 'success',
                    'code': 200,
                    'data': {
                        'merchant_id': str(merchant_entity.merchant_id),
                        'name': 'new name',
                        'partner_id': str(merchant_entity.partner_id),
                        'callback_url': 'https://call.back',
                        'url': None,
                        'delivery_integration_params': {
                            'yandex_delivery': {'autoaccept': False},
                            'measurements': {
                                'length': 1.0,
                                'height': 2.0,
                                'width': 3.0,
                                'weight': 4.0,
                            },
                        },
                        'created': utc_now.isoformat(),
                        'updated': now.isoformat(),
                    },
                }
            ),
        )

    @pytest.mark.parametrize(
        'params, expected',
        [
            (
                {'name': 'n', 'callback_url': 'https://some.ru/cb', 'url': 'https://some.ru'},
                {'name': 'n', 'callback_url': 'https://some.ru/cb', 'url': 'https://some.ru'},
            ),
            (
                {'delivery_integration_params': {'yandex_delivery': None, 'measurements': None}},
                {'delivery_integration_params': DeliveryIntegrationParams(yandex_delivery=None, measurements=None)},
            ),
            (
                {'delivery_integration_params': {'yandex_delivery': {}, 'measurements': None}},
                {
                    'delivery_integration_params': DeliveryIntegrationParams(
                        yandex_delivery=YandexDeliveryParams(),
                        measurements=None,
                    ),
                },
            ),
            (
                {
                    'delivery_integration_params': {
                        'yandex_delivery': {'autoaccept': True, 'warehouses': [], 'oauth_token': 'token'},
                        'measurements': {'length': 1.0, 'height': 2.0, 'width': 3.0, 'weight': 4.0},
                    },
                },
                {
                    'delivery_integration_params': DeliveryIntegrationParams(
                        yandex_delivery=YandexDeliveryParams(autoaccept=True, warehouses=[], oauth_token='token'),
                        measurements=Measurements(length=1, height=2, width=3, weight=4),
                    ),
                },
            ),
        ],
    )
    @pytest.mark.asyncio
    async def test_calls_action(self, url, merchant_entity, user, app, mock_action, params, expected):
        action_mock = mock_action(UpdateMerchantAction, merchant_entity)

        await app.patch(url, json=params, raise_for_status=True)

        call_kwargs = action_mock.call_args.kwargs
        if yd := call_kwargs.get('delivery_integration_params', DeliveryIntegrationParams()).yandex_delivery:
            yd.oauth_token = yd.get_oauth_token()

        expected_call = {
            'user': user,
            'partner_id': merchant_entity.partner_id,
            'merchant_id': merchant_entity.merchant_id,
            'callback_url': None,
            'url': None,
            **expected,
        }
        assert_that(call_kwargs, equal_to(expected_call))

    @pytest.mark.parametrize(
        'full_path, value, expected_error',
        (
            pytest.param('name', None, 'Field may not be null.', id='name_not_null'),
            pytest.param('name', 123, 'Not a valid string.', id='name_string_required'),
            pytest.param('delivery_integration_params', None, 'Field may not be null.', id='dip_not_null'),
        ),
    )
    @pytest.mark.asyncio
    async def test_patch_validate_payload(self, url, app, mock_action, full_path, value, expected_error):
        action_mock = mock_action(UpdateMerchantAction)

        r = await app.patch(url, json={full_path: value})

        await check_error(r, full_path, expected_error)

        action_mock.assert_not_run()

    @pytest.mark.asyncio
    async def test_merchant_not_found(self, url, app, mock_action):
        action_mock = mock_action(UpdateMerchantAction, side_effect=MerchantNotFoundError)

        r = await app.patch(url, json={'name': 'any'})

        assert_that(r.status, equal_to(404))
        data = await r.json()
        assert_that(
            data,
            equal_to(
                {
                    'status': 'fail',
                    'code': 404,
                    'data': {
                        'message': 'MERCHANT_NOT_FOUND',
                    },
                }
            ),
        )

        action_mock.assert_run_once()


class TestGetMerchant:
    @pytest.mark.asyncio
    async def test_get(self, url, user, merchant_entity, app, mock_action, partner, utc_now):
        action_mock = mock_action(GetMerchantAction, merchant_entity)

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
                        'merchant_id': str(merchant_entity.merchant_id),
                        'name': merchant_entity.name,
                        'partner_id': str(partner.partner_id),
                        'callback_url': None,
                        'url': None,
                        'delivery_integration_params': {
                            'yandex_delivery': {'autoaccept': False},
                            'measurements': {'length': 1.0, 'height': 2.0, 'width': 3.0, 'weight': 4.0},
                        },
                        'created': utc_now.isoformat(),
                        'updated': utc_now.isoformat(),
                    },
                }
            ),
        )

        action_mock.assert_run_once_with(
            user=user,
            partner_id=partner.partner_id,
            merchant_id=merchant_entity.merchant_id,
        )

    @pytest.mark.asyncio
    async def test_get_merchant_not_found(self, url, merchant_entity, app, mock_action):
        action_mock = mock_action(GetMerchantAction, side_effect=MerchantNotFoundError)

        r = await app.get(url)

        assert_that(r.status, equal_to(404))
        data = await r.json()
        assert_that(
            data,
            equal_to(
                {
                    'status': 'fail',
                    'code': 404,
                    'data': {
                        'message': 'MERCHANT_NOT_FOUND',
                    },
                }
            ),
        )

        action_mock.assert_run_once()
