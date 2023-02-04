import re
from uuid import UUID, uuid4

import pytest
import yarl

from sendr_utils import without_none

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import IntegrationStatus
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.integration import Integration
from billing.yandex_pay_admin.yandex_pay_admin.tests.utils import is_datetime_with_tz

pytestmark = pytest.mark.usefixtures(
    'mock_app_authentication',
    'setup_interactions_tvm',
    'mock_pay_backend_put_merchant_both_environments',
    'mock_pay_plus_backend_put_merchant_both_environments',
)

URL = '/api/web/v1/integrations'


@pytest.fixture
def creds():
    return {'key': 'k3y', 'password': 'passw0rd', 'gateway_merchant_id': 'the-gwid'}


@pytest.fixture
async def merchant_id(app, partner, role):
    r = await app.post(
        f'/api/web/v1/partners/{partner.partner_id}/merchants', json={'name': 'merchant_name'}, raise_for_status=True
    )
    return UUID((await r.json())['data']['merchant_id'])


@pytest.fixture
def for_testing():
    return True


@pytest.fixture
def upsert():
    return True


@pytest.fixture(autouse=True)
def mock_plus_backend_integration(yandex_pay_admin_settings, for_testing, aioresponses_mocker, upsert):
    if for_testing:
        base_url = yandex_pay_admin_settings.YANDEX_PAY_PLUS_BACKEND_SANDBOX_URL
    else:
        base_url = yandex_pay_admin_settings.YANDEX_PAY_PLUS_BACKEND_PRODUCTION_URL

    upsert_url = yarl.URL(base_url) / 'api/internal/v1/integrations'
    if upsert:
        return aioresponses_mocker.patch(re.compile(f'^{upsert_url}/.*$'), payload={})
    else:
        return aioresponses_mocker.delete(re.compile(f'^{upsert_url}/.*$'), payload={})


@pytest.fixture
def create_integration(storage, merchant_id, for_testing):
    async def _inner(status: IntegrationStatus = IntegrationStatus.READY):
        return await storage.integration.create(
            Integration(
                psp_id=uuid4(),
                psp_external_id='payture',
                merchant_id=merchant_id,
                status=status,
                for_testing=for_testing,
            )
        )

    return _inner


class TestDeploy:
    @pytest.mark.asyncio
    async def test_deploy_simple(self, app, mock_plus_backend_integration, for_testing, create_integration):
        integration = await create_integration()

        r = await app.post(f'{URL}/{integration.integration_id}/deploy')

        assert_that(r.status, equal_to(200))
        assert_that(
            await r.json(),
            equal_to(
                {
                    'status': 'success',
                    'code': 200,
                    'data': {
                        'integration_id': str(integration.integration_id),
                        'merchant_id': str(integration.merchant_id),
                        'psp_id': str(integration.psp_id),
                        'psp_external_id': integration.psp_external_id,
                        'status': IntegrationStatus.DEPLOYED.value,
                        'for_testing': for_testing,
                        'revision': 2,
                        'created': is_datetime_with_tz(),
                        'updated': is_datetime_with_tz(),
                    },
                }
            ),
        )
        mock_plus_backend_integration.assert_called_once()

        call_kwargs = mock_plus_backend_integration.call_args.kwargs['json']
        assert_that(
            call_kwargs,
            equal_to(
                {
                    'merchant_id': str(integration.merchant_id),
                    'psp_id': str(integration.psp_id),
                    'status': IntegrationStatus.DEPLOYED.value,
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_deploy_toggle(
        self, app, storage, merchant_id, mock_plus_backend_integration, for_testing, create_integration
    ):
        integration = await create_integration()
        already_deployed = await create_integration(IntegrationStatus.DEPLOYED)

        r = await app.post(f'{URL}/{integration.integration_id}/deploy')

        assert_that(r.status, equal_to(200))
        assert_that(
            await r.json(),
            equal_to(
                {
                    'status': 'success',
                    'code': 200,
                    'data': {
                        'integration_id': str(integration.integration_id),
                        'merchant_id': str(merchant_id),
                        'psp_id': str(integration.psp_id),
                        'psp_external_id': integration.psp_external_id,
                        'status': IntegrationStatus.DEPLOYED.value,
                        'for_testing': for_testing,
                        'revision': 2,
                        'created': is_datetime_with_tz(),
                        'updated': is_datetime_with_tz(),
                    },
                }
            ),
        )
        was_deployed = await storage.integration.get(already_deployed.integration_id)
        assert_that(was_deployed.status, equal_to(IntegrationStatus.READY))


class TestDelete:
    @pytest.fixture
    def upsert(self):
        return False

    @pytest.mark.asyncio
    async def test_delete_simple(
        self, app, merchant_id, mock_plus_backend_integration, for_testing, create_integration
    ):
        integration = await create_integration()
        payload = {'integration_id': str(integration.integration_id)}

        r = await app.delete(f'{URL}/{integration.integration_id}', json=payload)

        assert_that(r.status, equal_to(200))
        mock_plus_backend_integration.assert_called_once()


class TestUpdate:
    @pytest.mark.asyncio
    @pytest.mark.parametrize('start_status', [s for s in IntegrationStatus])
    async def test_update_creeds(
        self,
        app,
        merchant_id,
        mock_plus_backend_integration,
        for_testing,
        create_integration,
        start_status,
    ):
        integration = await create_integration(start_status)
        if start_status == IntegrationStatus.AWAITING_CREDENTIALS:
            expected_status = IntegrationStatus.READY
        else:
            expected_status = start_status

        payload = without_none(
            {
                'integration_id': str(integration.integration_id),
                'creds': 'secret',
                'encrypted': True,
            }
        )

        r = await app.patch(f'{URL}/{integration.integration_id}', json=payload)

        assert_that(r.status, equal_to(200))
        assert_that(
            await r.json(),
            equal_to(
                {
                    'status': 'success',
                    'code': 200,
                    'data': {
                        'integration_id': str(integration.integration_id),
                        'merchant_id': str(merchant_id),
                        'psp_id': str(integration.psp_id),
                        'psp_external_id': integration.psp_external_id,
                        'status': expected_status.value,
                        'for_testing': for_testing,
                        'revision': 1 if start_status == expected_status else 2,
                        'created': is_datetime_with_tz(),
                        'updated': is_datetime_with_tz(),
                    },
                }
            ),
        )
        mock_plus_backend_integration.assert_called_once()

        call_kwargs = mock_plus_backend_integration.call_args.kwargs['json']
        assert_that(
            call_kwargs,
            equal_to(
                {
                    'merchant_id': str(merchant_id),
                    'psp_id': str(integration.psp_id),
                    'creds': 'secret',
                    'status': expected_status.value,
                }
            ),
        )
