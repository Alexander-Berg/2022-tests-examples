from uuid import UUID, uuid4

import pytest

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


@pytest.fixture
async def merchant_id(app, partner, role):
    r = await app.post(
        f'/api/web/v1/partners/{partner.partner_id}/merchants', json={'name': 'merchant_name'}, raise_for_status=True
    )
    return UUID((await r.json())['data']['merchant_id'])


@pytest.fixture
async def integrations(storage, merchant_id):
    return [
        await storage.integration.create(
            Integration(
                psp_id=uuid4(),
                psp_external_id='payture',
                merchant_id=merchant_id,
                status=IntegrationStatus.READY,
            )
        )
        for _ in range(3)
    ]


@pytest.mark.asyncio
async def test_returned(app, merchant_id, integrations):
    r = await app.get(f'/api/web/v1/merchants/{merchant_id}/integrations', raise_for_status=True)

    assert_that(
        await r.json(),
        equal_to(
            {
                'status': 'success',
                'code': 200,
                'data': {
                    'integrations': [
                        {
                            'integration_id': str(integration.integration_id),
                            'for_testing': True,
                            'created': is_datetime_with_tz(),
                            'updated': is_datetime_with_tz(),
                            'revision': 1,
                            'status': 'ready',
                            'psp_id': str(integration.psp_id),
                            'psp_external_id': 'payture',
                            'merchant_id': str(merchant_id),
                        }
                        for integration in integrations
                    ],
                },
            }
        ),
    )
