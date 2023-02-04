import uuid

import pytest
from pay.lib.entities.cart import Measurements

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant.create import CreateMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant.list import ListMerchantsAction
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.merchant import (
    DeliveryIntegrationParams,
    Merchant,
    YandexDeliveryParams,
)
from billing.yandex_pay_admin.yandex_pay_admin.tests.utils import check_error, replace_payload


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
def url(partner):
    return f'/api/web/v1/partners/{partner.partner_id}/merchants'


class TestCreateMerchant:
    @pytest.fixture
    def payload(self, merchant_entity):
        return {'name': merchant_entity.name}

    @pytest.mark.asyncio
    async def test_post(self, url, payload, app, mock_action, merchant_entity, user, partner, utc_now):
        action_mock = mock_action(CreateMerchantAction, merchant_entity)

        r = await app.post(url, json=payload)

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
                            'measurements': {
                                'length': 1.0,
                                'height': 2.0,
                                'width': 3.0,
                                'weight': 4.0,
                            },
                        },
                        'created': utc_now.isoformat(),
                        'updated': utc_now.isoformat(),
                    },
                }
            ),
        )

        action_mock.assert_run_once_with(
            user=user, name=payload['name'], partner_id=partner.partner_id, callback_url=None, url=None
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'full_path, value, expected_error',
        (
            pytest.param('name', None, 'Field may not be null.', id='name_not_null'),
            pytest.param('name', 123, 'Not a valid string.', id='name_string_required'),
        ),
    )
    async def test_post_validate_payload(self, app, mock_action, url, payload, full_path, value, expected_error):
        payload = replace_payload(payload, full_path, value)
        action_mock = mock_action(CreateMerchantAction)

        r = await app.post(url, json=payload)

        await check_error(r, full_path, expected_error)
        action_mock.assert_not_run()


class TestListMerchants:
    @pytest.mark.asyncio
    async def test_get(self, url, app, mock_action, merchant_entity, partner, user, utc_now):
        action_mock = mock_action(ListMerchantsAction, [merchant_entity])

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
                                'merchant_id': str(merchant_entity.merchant_id),
                                'name': merchant_entity.name,
                                'partner_id': str(partner.partner_id),
                                'callback_url': None,
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
                                'updated': utc_now.isoformat(),
                            }
                        ]
                    },
                }
            ),
        )

        action_mock.assert_run_once_with(user=user, partner_id=partner.partner_id)

    @pytest.mark.asyncio
    async def test_get_no_merchants(self, url, app, mock_action, partner, user):
        action_mock = mock_action(ListMerchantsAction, [])

        r = await app.get(url)

        assert_that(r.status, equal_to(200))
        data = await r.json()

        assert_that(
            data,
            equal_to(
                {
                    'status': 'success',
                    'code': 200,
                    'data': {'merchants': []},
                }
            ),
        )

        action_mock.assert_run_once_with(user=user, partner_id=partner.partner_id)
