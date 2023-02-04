from uuid import UUID, uuid4

import pytest

from sendr_pytest.helpers import ensure_all_fields
from sendr_utils import json_value, utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.integration.create import CreateIntegrationAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.integration.list import ListIntegrationsAction
from billing.yandex_pay_admin.yandex_pay_admin.core.entities.enums import IntegrationPSPExternalID
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import IntegrationStatus
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.integration import Integration


@pytest.fixture
def merchant_id():
    return uuid4()


@pytest.fixture
def now():
    return utcnow()


@pytest.fixture
def url(merchant_id):
    return f'/api/web/v1/merchants/{merchant_id}/integrations'


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


class TestCreateIntegration:
    @pytest.fixture
    def payload(self):
        return {
            'psp_external_id': 'payture',
        }

    @pytest.fixture(autouse=True)
    def mock_create_integration_action(self, mock_action, integration):
        return mock_action(CreateIntegrationAction, integration)

    @pytest.mark.asyncio
    async def test_post(self, payload, app, url, integration, now):
        r = await app.post(url, json=payload)

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
    async def test_action_called(self, payload, app, url, merchant_id, mock_create_integration_action, user):
        await app.post(url, json=payload)

        mock_create_integration_action.assert_run_once_with(
            user=user,
            merchant_id=merchant_id,
            psp_external_id=IntegrationPSPExternalID.PAYTURE,
            encrypted=False,
            for_testing=True,
        )


class TestListIntegrations:
    @pytest.mark.asyncio
    async def test_result(self, app, url, now, mock_action, integration):
        mock_action(ListIntegrationsAction, [integration])

        r = await app.get(url, raise_for_status=True)
        data = await r.json()

        now_iso = now.isoformat()
        assert_that(
            data['data']['integrations'],
            equal_to([{**json_value(integration), 'created': now_iso, 'updated': now_iso}]),
        )

    @pytest.mark.asyncio
    async def test_action_called(self, app, url, mock_action, merchant_id, user):
        mock = mock_action(ListIntegrationsAction, [])
        await app.get(url)

        mock.assert_run_once_with(user=user, merchant_id=merchant_id)
