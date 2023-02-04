import json
import re

import pytest
import yarl

from sendr_pytest.matchers import convert_then_match

from hamcrest import assert_that, equal_to, match_equality

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.credentials.encrypt import (
    EncryptIntegrationCredentialsAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import IntegrationStatus, RoleType
from billing.yandex_pay_admin.yandex_pay_admin.tests.utils import is_datetime_with_tz, is_uuid

pytestmark = pytest.mark.usefixtures(
    'mock_app_authentication',
    'setup_interactions_tvm',
    'mock_pay_backend_put_merchant_both_environments',
    'mock_pay_plus_backend_put_merchant_both_environments',
)


@pytest.fixture
def create_merchant(app, partner, role):
    async def _inner(status: int = 200):
        r = await app.post(
            f'/api/web/v1/partners/{partner.partner_id}/merchants',
            json={'name': 'merchant_name'},
        )
        assert_that(r.status, equal_to(status))
        return await r.json()

    return _inner


@pytest.fixture(params=[False, True])
def for_testing(request):
    return request.param


@pytest.fixture(autouse=True)
def mock_plus_backend_upsert_integration(yandex_pay_admin_settings, for_testing, aioresponses_mocker):
    if for_testing:
        base_url = yandex_pay_admin_settings.YANDEX_PAY_PLUS_BACKEND_SANDBOX_URL
    else:
        base_url = yandex_pay_admin_settings.YANDEX_PAY_PLUS_BACKEND_PRODUCTION_URL

    upsert_url = yarl.URL(base_url) / 'api/internal/v1/integrations'
    return aioresponses_mocker.patch(re.compile(f'^{upsert_url}/.*$'), payload={})


class TestPaytureIntegration:
    @pytest.fixture
    def creds(self):
        return {'key': 'k3y', 'password': 'passw0rd', 'gateway_merchant_id': 'the-gwid'}

    @pytest.mark.asyncio
    async def test_returned__no_creds(self, app, create_merchant, mock_plus_backend_upsert_integration, for_testing):
        merchant_id = (await create_merchant())['data']['merchant_id']
        payload = {'psp_external_id': 'payture', 'for_testing': for_testing}

        r = await app.post(f'/api/web/v1/merchants/{merchant_id}/integrations', json=payload)

        assert_that(r.status, equal_to(200))
        assert_that(
            await r.json(),
            equal_to(
                {
                    'status': 'success',
                    'code': 200,
                    'data': {
                        'integration_id': is_uuid(),
                        'merchant_id': merchant_id,
                        'psp_id': '678de147-6ff0-4171-8bf8-5796c7b717c3',
                        'psp_external_id': 'payture',
                        'status': IntegrationStatus.AWAITING_CREDENTIALS.value,
                        'for_testing': for_testing,
                        'revision': 1,
                        'created': is_datetime_with_tz(),
                        'updated': is_datetime_with_tz(),
                    },
                }
            ),
        )
        mock_plus_backend_upsert_integration.assert_not_called()

    @pytest.mark.asyncio
    async def test_returned__creds_plain_text(
        self, app, create_merchant, mock_plus_backend_upsert_integration, for_testing, creds
    ):
        merchant_id = (await create_merchant())['data']['merchant_id']
        payload = {
            'psp_external_id': 'payture',
            'creds': json.dumps(creds),
            'for_testing': for_testing,
        }

        r = await app.post(f'/api/web/v1/merchants/{merchant_id}/integrations', json=payload)

        assert_that(r.status, equal_to(200))
        assert_that(
            await r.json(),
            equal_to(
                {
                    'status': 'success',
                    'code': 200,
                    'data': {
                        'integration_id': is_uuid(),
                        'merchant_id': merchant_id,
                        'psp_id': '678de147-6ff0-4171-8bf8-5796c7b717c3',
                        'psp_external_id': 'payture',
                        'status': IntegrationStatus.READY.value,
                        'for_testing': for_testing,
                        'revision': 1,
                        'created': is_datetime_with_tz(),
                        'updated': is_datetime_with_tz(),
                    },
                }
            ),
        )

        mock_plus_backend_upsert_integration.assert_called_once()
        call_kwargs = mock_plus_backend_upsert_integration.call_args.kwargs['json']
        assert_that(
            call_kwargs,
            equal_to(
                {
                    'merchant_id': merchant_id,
                    'psp_id': '678de147-6ff0-4171-8bf8-5796c7b717c3',
                    'status': IntegrationStatus.READY.value,
                    'creds': match_equality(
                        convert_then_match(
                            lambda creds: json.loads(
                                EncryptIntegrationCredentialsAction.get_crypter(for_testing).decrypt(creds),
                            ),
                            equal_to(creds),
                        )
                    ),
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_returned__creds_encrypted(
        self, app, create_merchant, mock_plus_backend_upsert_integration, for_testing, creds
    ):
        merchant_id = (await create_merchant())['data']['merchant_id']
        payload = {
            'psp_external_id': 'payture',
            'creds': 'secret',
            'encrypted': True,
            'for_testing': for_testing,
            'status': IntegrationStatus.DEPLOYED.value,
        }

        r = await app.post(f'/api/web/v1/merchants/{merchant_id}/integrations', json=payload)

        assert_that(r.status, equal_to(200))
        assert_that(
            await r.json(),
            equal_to(
                {
                    'status': 'success',
                    'code': 200,
                    'data': {
                        'integration_id': is_uuid(),
                        'merchant_id': merchant_id,
                        'psp_id': '678de147-6ff0-4171-8bf8-5796c7b717c3',
                        'psp_external_id': 'payture',
                        'status': IntegrationStatus.DEPLOYED.value,
                        'for_testing': for_testing,
                        'revision': 1,
                        'created': is_datetime_with_tz(),
                        'updated': is_datetime_with_tz(),
                    },
                }
            ),
        )

        mock_plus_backend_upsert_integration.assert_called_once()
        call_kwargs = mock_plus_backend_upsert_integration.call_args.kwargs['json']
        assert_that(
            call_kwargs,
            equal_to(
                {
                    'merchant_id': merchant_id,
                    'psp_id': '678de147-6ff0-4171-8bf8-5796c7b717c3',
                    'status': IntegrationStatus.DEPLOYED.value,
                    'creds': 'secret',
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_invalid_creds(self, app, create_merchant, mock_plus_backend_upsert_integration, for_testing, creds):
        creds.pop('gateway_merchant_id')
        merchant_id = (await create_merchant())['data']['merchant_id']
        payload = {
            'psp_external_id': 'payture',
            'creds': json.dumps(creds),
            'for_testing': for_testing,
        }

        r = await app.post(f'/api/web/v1/merchants/{merchant_id}/integrations', json=payload)

        assert_that(r.status, equal_to(400))
        assert_that(
            await r.json(),
            equal_to(
                {
                    'status': 'fail',
                    'code': 400,
                    'data': {
                        'message': 'INVALID_INTEGRATION_CREDENTIAL_VALUE',
                        'params': {'validation_errors': {'gateway_merchant_id': ['Missing data for required field.']}},
                    },
                }
            ),
        )
        mock_plus_backend_upsert_integration.assert_not_called()


@pytest.mark.asyncio
async def test_viewer_cannot_create_integration(app, storage, role, create_merchant):
    merchant_id = (await create_merchant())['data']['merchant_id']
    payload = {'psp_external_id': 'payture'}

    role.role = RoleType.VIEWER
    await storage.role.save(role)

    r = await app.post(f'/api/web/v1/merchants/{merchant_id}/integrations', json=payload)

    assert_that(r.status, equal_to(403))
    assert_that(
        await r.json(),
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
async def test_cannot_create_integration_if_role_missing(app, storage, role, create_merchant):
    merchant_id = (await create_merchant())['data']['merchant_id']
    payload = {'psp_external_id': 'payture'}

    await storage.role.delete(role)

    r = await app.post(f'/api/web/v1/merchants/{merchant_id}/integrations', json=payload)

    assert_that(r.status, equal_to(403))
    assert_that(
        await r.json(),
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
