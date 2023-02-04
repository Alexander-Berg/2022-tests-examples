from uuid import uuid4

import pytest

from sendr_utils import without_none

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.integration.delete import DeleteIntegrationAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import IntegrationNotFoundError
from billing.yandex_pay_admin.yandex_pay_admin.interactions import (
    YandexPayPlusBackendClient,
    YandexPayPlusSandboxClient,
)
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import IntegrationStatus
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.integration import Integration


@pytest.fixture
async def integrations(storage, merchant, for_testing) -> list[Integration]:
    return [
        await storage.integration.create(
            Integration(
                psp_id=uuid4(),
                psp_external_id='payture',
                merchant_id=merchant.merchant_id,
                status=IntegrationStatus.READY,
                for_testing=for_testing,
            )
        )
        for _ in range(3)
    ]


@pytest.fixture
def for_testing():
    return True


@pytest.fixture
def mock_pay_client(mocker, for_testing):
    mock = mocker.AsyncMock()
    client_cls = YandexPayPlusSandboxClient if for_testing else YandexPayPlusBackendClient
    return mocker.patch.object(client_cls, 'delete_integration', mock)


@pytest.mark.asyncio
@pytest.mark.parametrize('for_testing', (False, True))
async def test_delete(storage, user, mock_pay_client, integrations, for_testing):
    integration = integrations.pop()
    action_kwargs = without_none(
        {
            'user': user,
            'integration_id': integration.integration_id,
        }
    )
    await DeleteIntegrationAction(**action_kwargs).run()
    current_integrations = await storage.integration.list_by_merchant_id(integration.merchant_id)

    mock_pay_client.assert_awaited_once()
    assert_that(integrations, equal_to(current_integrations))


@pytest.mark.asyncio
async def test_delete_not_exists(user, mock_pay_client):
    action_kwargs = without_none(
        {
            'user': user,
            'integration_id': uuid4(),
        }
    )
    with pytest.raises(IntegrationNotFoundError):
        await DeleteIntegrationAction(**action_kwargs).run()

    mock_pay_client.assert_not_awaited()
