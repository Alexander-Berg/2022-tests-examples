import json
from uuid import uuid4

import pytest

from sendr_utils import without_none

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.credentials.encrypt import (
    EncryptIntegrationCredentialsAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.integration.deploy import DeployIntegrationAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.integration.update import UpdateIntegrationAction
from billing.yandex_pay_admin.yandex_pay_admin.core.entities.enums import IntegrationPSPExternalID
from billing.yandex_pay_admin.yandex_pay_admin.interactions import (
    YandexPayPlusBackendClient,
    YandexPayPlusSandboxClient,
)
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import IntegrationStatus as Status
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.integration import Integration

CREDS_JSON = {'key': 'k3y', 'password': 'passw0rd', 'gateway_merchant_id': 'the-gwid'}
CREDS = json.dumps(CREDS_JSON)


@pytest.fixture
def for_testing():
    return True


@pytest.fixture
def mock_pay_client(mocker, for_testing):
    mock = mocker.AsyncMock()
    client_cls = YandexPayPlusSandboxClient if for_testing else YandexPayPlusBackendClient
    return mocker.patch.object(client_cls, 'upsert_integration', mock)


@pytest.fixture
async def integration(storage, merchant, start_status, for_testing) -> Integration:
    return await storage.integration.create(
        Integration(
            psp_id=uuid4(),
            psp_external_id='payture',
            merchant_id=merchant.merchant_id,
            status=start_status,
            for_testing=for_testing,
        )
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('start_status', [s for s in Status])
@pytest.mark.parametrize('encrypted', (False, True))
async def test_update_creds(user, mock_pay_client, mock_action, start_status, encrypted, integration):
    encrypt_mock = mock_action(EncryptIntegrationCredentialsAction, CREDS)
    deploy_mock = mock_action(DeployIntegrationAction, integration)
    action_kwargs = without_none(
        {
            'user': user,
            'integration_id': integration.integration_id,
            'encrypted': encrypted,
            'creds': CREDS,
        }
    )
    expected_status = start_status if start_status != Status.AWAITING_CREDENTIALS else Status.READY

    result = await UpdateIntegrationAction(**action_kwargs).run()

    if encrypted:
        encrypt_mock.assert_not_run()
    else:
        encrypt_mock.assert_run_once_with(
            psp_external_id=IntegrationPSPExternalID.PAYTURE,
            creds=CREDS,
            for_testing=integration.for_testing,
        )
    mock_pay_client.assert_called_once()
    deploy_mock.assert_not_called()

    integration.status = expected_status
    integration.revision = 1 if start_status == expected_status else 2
    integration.updated = result.updated
    assert_that(result, equal_to(integration))


@pytest.mark.asyncio
@pytest.mark.parametrize('start_status', [s for s in Status])
async def test_not_update(user, mock_pay_client, mock_action, start_status, integration):
    encrypt_mock = mock_action(EncryptIntegrationCredentialsAction, CREDS)
    deploy_mock = mock_action(DeployIntegrationAction, integration)
    action_kwargs = without_none(
        {
            'user': user,
            'integration_id': integration.integration_id,
        }
    )

    result = await UpdateIntegrationAction(**action_kwargs).run()

    encrypt_mock.assert_not_run()
    mock_pay_client.assert_not_awaited()
    deploy_mock.assert_not_called()

    integration.status = start_status
    integration.revision = 1
    integration.updated = result.updated
    assert_that(result, equal_to(integration))
