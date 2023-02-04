import logging
from uuid import uuid4

import pytest
import yenv

from sendr_interactions.exceptions import InteractionResponseError

from hamcrest import assert_that, equal_to, has_entries, has_item, has_properties

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.pay_backend.put_merchant import (
    PayBackendPutMerchantAction,
    PutMerchantToEnvironmentAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.core.entities.enums import PayBackendType
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import PutMerchantDataError
from billing.yandex_pay_admin.yandex_pay_admin.interactions import (
    YandexPayBackendClient,
    YandexPayPlusBackendClient,
    YandexPayPlusSandboxClient,
    YandexPaySandboxClient,
)
from billing.yandex_pay_admin.yandex_pay_admin.interactions.pay_backend import PayBackendClientResponseError
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.merchant import (
    DeliveryIntegrationParams,
    YandexDeliveryParams,
)
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin import Origin
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin_moderation import OriginModeration


@pytest.fixture(autouse=True)
async def origin(storage, merchant):
    return await storage.origin.create(
        Origin(
            merchant_id=merchant.merchant_id,
            origin_id=uuid4(),
            origin='a.test',
            revision=777,
        )
    )


@pytest.fixture
async def moderation(storage, origin):
    return await storage.origin_moderation.create(
        OriginModeration(
            origin_moderation_id=uuid4(),
            origin_id=origin.origin_id,
            revision=origin.revision,
            ticket='TICKET-1',
            resolved=True,
            approved=True,
        )
    )


@pytest.fixture(params=('PAY', 'PLUS'))
def pay_client_kind(request):
    return request.param


@pytest.fixture
async def mock_put_merchant(mocker, pay_client_kind):
    pay_client_mocker = mocker.patch.object(YandexPayBackendClient, 'put_merchant', mocker.AsyncMock())
    pay_plus_client_mocker = mocker.patch.object(YandexPayPlusBackendClient, 'put_merchant', mocker.AsyncMock())
    if pay_client_kind == 'PAY':
        return pay_client_mocker
    return pay_plus_client_mocker


@pytest.fixture
async def mock_put_merchant_sandbox(mocker, pay_client_kind):
    pay_client_mocker = mocker.patch.object(YandexPaySandboxClient, 'put_merchant', mocker.AsyncMock())
    pay_plus_client_mocker = mocker.patch.object(YandexPayPlusSandboxClient, 'put_merchant', mocker.AsyncMock())
    if pay_client_kind == 'PAY':
        return pay_client_mocker
    return pay_plus_client_mocker


@pytest.fixture(params=('testing', 'production'))
def env_type(request):
    return request.param


@pytest.fixture
def yenv_type(env_type):
    _type = yenv.type
    yenv.type = env_type
    yield
    yenv.type = _type


@pytest.mark.asyncio
async def test_put_merchant(mocker, mock_action, yenv_type, env_type):
    merchant = mocker.Mock()
    mock = mock_action(PutMerchantToEnvironmentAction)

    await PayBackendPutMerchantAction(merchant).run()

    expected_calls = [mocker.call(merchant=merchant, backend_type=PayBackendType.PRODUCTION, force_add_origin=False)]
    if env_type == 'production':
        expected_calls.append(
            mocker.call(merchant=merchant, backend_type=PayBackendType.SANDBOX, force_add_origin=False)
        )
    assert_that(mock.call_count, equal_to(len(expected_calls)))
    mock.assert_has_calls(expected_calls[::-1])


class TestPutMerchantToEnvSucceeds:
    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'callback_url, delivery_token',
        [
            (None, None),
            ('https://merchant.test', 'token'),
        ],
    )
    async def test_put_merchant_sandbox(
        self, merchant, origin, mock_put_merchant_sandbox, pay_client_kind, callback_url, delivery_token
    ):
        merchant.callback_url = callback_url
        merchant.delivery_integration_params = DeliveryIntegrationParams(
            YandexDeliveryParams(oauth_token=delivery_token, autoaccept=True)
        )
        origins = await PutMerchantToEnvironmentAction(merchant=merchant).run()

        assert_that(origins, equal_to([origin]))

        expected = {
            'merchant_id': merchant.merchant_id,
            'name': merchant.name,
            'origins': [origin.origin],
            'callback_url': callback_url,
        }
        if pay_client_kind == 'PLUS':
            expected['partner_id'] = merchant.partner_id
            expected['delivery_integration_params'] = merchant.delivery_integration_params if delivery_token else None

        mock_put_merchant_sandbox.assert_awaited_once_with(**expected)

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'callback_url, delivery_token',
        [
            (None, None),
            ('https://merchant.test', 'token'),
        ],
    )
    @pytest.mark.usefixtures('moderation')
    async def test_put_merchant_production(
        self,
        merchant,
        origin,
        mock_put_merchant,
        aioresponses_mocker,
        pay_client_kind,
        callback_url,
        delivery_token,
    ):
        merchant.callback_url = callback_url
        merchant.delivery_integration_params = DeliveryIntegrationParams(
            YandexDeliveryParams(oauth_token=delivery_token, autoaccept=True)
        )
        origins = await PutMerchantToEnvironmentAction(
            merchant=merchant,
            backend_type=PayBackendType.PRODUCTION,
        ).run()

        assert_that(origins, equal_to([origin]))

        expected = {
            'merchant_id': merchant.merchant_id,
            'name': merchant.name,
            'origins': [origin.origin],
            'callback_url': callback_url,
        }
        if pay_client_kind == 'PLUS':
            expected['partner_id'] = merchant.partner_id
            expected['delivery_integration_params'] = merchant.delivery_integration_params if delivery_token else None

        mock_put_merchant.assert_awaited_once_with(**expected)

    @pytest.mark.asyncio
    async def test_put_merchant_production__skip_unmoderated_origins(
        self,
        merchant,
        origin,
        mock_put_merchant,
        aioresponses_mocker,
        storage,
        pay_client_kind,
    ):
        assert_that(
            await storage.origin_moderation.find_by_origin_id(origin.origin_id),
            equal_to([]),
        )

        origins = await PutMerchantToEnvironmentAction(
            merchant=merchant,
            backend_type=PayBackendType.PRODUCTION,
        ).run()

        assert_that(origins, equal_to([]))

        expected = {
            'merchant_id': merchant.merchant_id,
            'name': merchant.name,
            'origins': [],
            'callback_url': None,
        }
        if pay_client_kind == 'PLUS':
            expected['partner_id'] = merchant.partner_id
            expected['delivery_integration_params'] = None

        mock_put_merchant.assert_awaited_once_with(**expected)

    @pytest.mark.asyncio
    async def test_put_merchant_production__force_add_origin(
        self, merchant, mock_put_merchant, moderation, origin, storage, pay_client_kind
    ):
        moderation.approved = False
        await storage.origin_moderation.save(moderation)

        origins = await PutMerchantToEnvironmentAction(
            merchant=merchant,
            backend_type=PayBackendType.PRODUCTION,
            force_add_origin=True,
        ).run()

        assert_that(origins, equal_to([origin]))

        expected = {
            'merchant_id': merchant.merchant_id,
            'name': merchant.name,
            'origins': [origin.origin],
            'callback_url': None,
        }
        if pay_client_kind == 'PLUS':
            expected['partner_id'] = merchant.partner_id
            expected['delivery_integration_params'] = None

        mock_put_merchant.assert_awaited_once_with(**expected)

    @pytest.mark.asyncio
    async def test_put_merchant_production__skip_origin_if_moderation_not_approved(
        self, merchant, mock_put_merchant, moderation, storage, pay_client_kind
    ):
        moderation.approved = False
        await storage.origin_moderation.save(moderation)

        origins = await PutMerchantToEnvironmentAction(
            merchant=merchant,
            backend_type=PayBackendType.PRODUCTION,
        ).run()

        assert_that(origins, equal_to([]))

        expected = {
            'merchant_id': merchant.merchant_id,
            'name': merchant.name,
            'origins': [],
            'callback_url': None,
        }
        if pay_client_kind == 'PLUS':
            expected['partner_id'] = merchant.partner_id
            expected['delivery_integration_params'] = None

        mock_put_merchant.assert_awaited_once_with(**expected)

    @pytest.mark.asyncio
    async def test_put_merchant_logged(self, merchant, mock_put_merchant_sandbox, origin, dummy_logs):
        origins = await PutMerchantToEnvironmentAction(merchant=merchant).run()

        assert_that(origins, equal_to([origin]))
        assert_that(
            dummy_logs(),
            has_item(
                has_properties(
                    levelno=logging.INFO,
                    message='MERCHANT_PROPAGATED',
                    _context=has_entries(
                        pay_backend_type=PayBackendType.SANDBOX,
                        origin_urls=[origin.origin],
                        merchant_id=merchant.merchant_id,
                        merchant_name=merchant.name,
                    ),
                )
            ),
        )


class TestPutMerchantToEnvFails:
    @pytest.mark.asyncio
    async def test_invalid_origin(self, merchant, mock_put_merchant_sandbox):
        mock_put_merchant_sandbox.side_effect = PayBackendClientResponseError(
            status_code=400,
            method='put',
            service=YandexPayBackendClient.SERVICE,
            message='INVALID_MERCHANT_ORIGIN',
            params={
                'origin': 'a.test',
                'secret_attribute': 'should not be exposed',
            },
        )

        with pytest.raises(PutMerchantDataError) as exc_info:
            await PutMerchantToEnvironmentAction(merchant=merchant).run()

        assert_that(
            exc_info.value,
            has_properties(
                params={'origin': 'a.test'},
                message='INVALID_MERCHANT_ORIGIN',
            ),
        )

    @pytest.mark.asyncio
    async def test_insecure_origin(self, merchant, mock_put_merchant_sandbox):
        mock_put_merchant_sandbox.side_effect = PayBackendClientResponseError(
            status_code=400,
            method='put',
            service=YandexPayBackendClient.SERVICE,
            message='INSECURE_MERCHANT_ORIGIN',
            params={
                'description': 'Insecure origin schema: HTTPS is expected.',
                'origin': 'a.test',
            },
        )

        with pytest.raises(PutMerchantDataError) as exc_info:
            await PutMerchantToEnvironmentAction(merchant=merchant).run()

        assert_that(
            exc_info.value,
            has_properties(
                params={
                    'description': 'Insecure origin schema: HTTPS is expected.',
                    'origin': 'a.test',
                },
                message='INSECURE_MERCHANT_ORIGIN',
            ),
        )

    @pytest.mark.asyncio
    async def test_generic_origin_error(self, merchant, mock_put_merchant_sandbox):
        mock_put_merchant_sandbox.side_effect = PayBackendClientResponseError(
            status_code=400,
            method='put',
            service=YandexPayBackendClient.SERVICE,
            message='BAD_REQUEST',
            params={'origins': {'0': {'origin': ['Not a valid URL.']}}},
        )

        with pytest.raises(PutMerchantDataError) as exc_info:
            await PutMerchantToEnvironmentAction(merchant=merchant).run()

        assert_that(
            exc_info.value,
            has_properties(
                params={'origins': {'0': {'origin': ['Not a valid URL.']}}},
                message='BAD_REQUEST',
            ),
        )

    @pytest.mark.asyncio
    async def test_non_json_4xx_error(self, merchant, mock_put_merchant_sandbox):
        mock_put_merchant_sandbox.side_effect = InteractionResponseError(
            status_code=400,
            method='put',
            service=YandexPayBackendClient.SERVICE,
            params={'error': 'Fake text error'},
        )

        with pytest.raises(PutMerchantDataError) as exc_info:
            await PutMerchantToEnvironmentAction(merchant=merchant).run()

        assert_that(
            exc_info.value,
            has_properties(
                params={},
                message='BAD_REQUEST',
            ),
        )
