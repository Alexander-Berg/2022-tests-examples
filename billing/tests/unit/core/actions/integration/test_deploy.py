from uuid import uuid4

import pytest

from sendr_utils import without_none

from hamcrest import assert_that, contains_inanyorder, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.integration.deploy import DeployIntegrationAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import InvalidIntegrationStatusError
from billing.yandex_pay_admin.yandex_pay_admin.interactions import (
    YandexPayPlusBackendClient,
    YandexPayPlusSandboxClient,
)
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import IntegrationStatus
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.integration import Integration


async def _integration(storage, merchant, start_status, for_testing) -> Integration:
    return await storage.integration.create(
        Integration(
            psp_id=uuid4(),
            psp_external_id='payture',
            merchant_id=merchant.merchant_id,
            status=start_status,
            for_testing=for_testing,
        )
    )


@pytest.fixture
async def integration(storage, merchant, start_status, for_testing) -> Integration:
    return await _integration(storage, merchant, start_status, for_testing)


@pytest.fixture
def for_testing():
    return True


@pytest.fixture
def start_status():
    return IntegrationStatus.READY


@pytest.fixture
def mock_pay_client(mocker, for_testing):
    mock = mocker.AsyncMock()
    client_cls = YandexPayPlusSandboxClient if for_testing else YandexPayPlusBackendClient
    return mocker.patch.object(client_cls, 'upsert_integration', mock)


@pytest.mark.asyncio
async def test_deploy(user, mock_pay_client, integration):
    action_kwargs = without_none(
        {
            'user': user,
            'integration_id': integration.integration_id,
        }
    )
    deployed = await DeployIntegrationAction(**action_kwargs).run()

    mock_pay_client.assert_awaited_once()
    integration.status = IntegrationStatus.DEPLOYED
    integration.revision = 2
    assert_that(deployed, equal_to(integration))


@pytest.mark.asyncio
async def test_already_deployed(storage, user, mock_pay_client, merchant, for_testing):
    integration = await _integration(storage, merchant, IntegrationStatus.DEPLOYED, for_testing)
    action_kwargs = without_none(
        {
            'user': user,
            'integration_id': integration.integration_id,
        }
    )
    deployed = await DeployIntegrationAction(**action_kwargs).run()

    mock_pay_client.assert_not_awaited()
    assert_that(deployed, equal_to(integration))


@pytest.mark.asyncio
async def test_toggle_other(storage, user, mock_pay_client, merchant, for_testing):
    statuses = (IntegrationStatus.READY, IntegrationStatus.READY, IntegrationStatus.DEPLOYED)
    integrations = [await _integration(storage, merchant, status, for_testing) for status in statuses]
    integration = integrations[0]
    action_kwargs = without_none(
        {
            'user': user,
            'integration_id': integration.integration_id,
        }
    )
    deployed = await DeployIntegrationAction(**action_kwargs).run()

    assert_that(mock_pay_client.call_count, 2)

    integration.status = IntegrationStatus.DEPLOYED
    integration.revision = 2
    integrations[2].status = IntegrationStatus.READY
    integrations[2].revision = 2

    assert_that(deployed, equal_to(integration))

    all_integrations = await storage.integration.list_by_merchant_id(merchant.merchant_id)
    assert_that(len(all_integrations), equal_to(3))
    assert_that(all_integrations, contains_inanyorder(*integrations))


@pytest.mark.asyncio
async def test_deploy_without_creds(storage, user, mock_pay_client, merchant, for_testing):
    integration = await _integration(storage, merchant, IntegrationStatus.AWAITING_CREDENTIALS, for_testing)
    action_kwargs = without_none(
        {
            'user': user,
            'integration_id': integration.integration_id,
        }
    )
    with pytest.raises(InvalidIntegrationStatusError):
        await DeployIntegrationAction(**action_kwargs).run()
