from uuid import UUID, uuid4

import pytest

from sendr_pytest.helpers import ensure_all_fields
from sendr_utils import json_value, utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.integration.deploy import DeployIntegrationAction
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import IntegrationStatus
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.integration import Integration


@pytest.fixture
def merchant_id():
    return uuid4()


@pytest.fixture
def now():
    return utcnow()


@pytest.fixture
def url(integration):
    return f'/api/web/v1/integrations/{integration.integration_id}/deploy'


@pytest.fixture
def integration(merchant_id, now):
    return ensure_all_fields(
        Integration,
        integration_id=uuid4(),
        merchant_id=merchant_id,
        psp_id=UUID('678de147-6ff0-4171-8bf8-5796c7b717c3'),
        psp_external_id='payture',
        status=IntegrationStatus.AWAITING_CREDENTIALS,
        for_testing=True,
        revision=1,
        created=now,
        updated=now,
    )


class TestDeployIntegration:
    @pytest.fixture(autouse=True)
    def mock_deploy_integration_action(self, mock_action, integration):
        return mock_action(DeployIntegrationAction, integration)

    @pytest.mark.asyncio
    async def test_returned(self, app, url, integration, now):
        r = await app.post(url)

        assert_that(r.status, equal_to(200))
        now_iso = now.isoformat()
        expected = {**json_value(integration), 'created': now_iso, 'updated': now_iso}
        assert_that(
            await r.json(),
            equal_to(
                {
                    'status': 'success',
                    'code': 200,
                    'data': expected,
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_action_called(self, app, url, mock_deploy_integration_action, integration, user):
        await app.post(url)

        mock_deploy_integration_action.assert_run_once_with(
            user=user,
            integration_id=integration.integration_id,
        )
