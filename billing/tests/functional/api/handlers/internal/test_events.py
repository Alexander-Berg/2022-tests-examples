from datetime import datetime

import pytest

from hamcrest import assert_that, equal_to

pytestmark = pytest.mark.usefixtures('mock_app_authentication', 'setup_interactions_tvm')


@pytest.mark.asyncio
async def test_success(
    partner,
    merchant,
    app,
    storage,
):
    assert partner.trial_started_at is None

    event_time = datetime.utcnow()
    response = await app.post(
        '/internal/v1/events',
        json={
            'event_time': event_time.isoformat(),
            'data': {
                'event_type': 'FIRST_TRANSACTION',
                'merchant_id': str(merchant.merchant_id),
                'partner_id': str(partner.partner_id),
            },
        },
        headers={'x-ya-service-ticket': 'dummy-service-ticket'},
    )
    data = await response.json()
    partner_updated = await storage.partner.get(partner.partner_id)

    assert_that(response.status, equal_to(200))
    assert partner_updated.trial_started_at.replace(tzinfo=None) == event_time
    assert data is not None


@pytest.mark.asyncio
async def test_unknown_event(
    app,
):

    event_time = datetime.utcnow()
    response = await app.post(
        '/internal/v1/events',
        json={
            'event_time': event_time.isoformat(),
            'data': {
                'event_type': 'SOME_EVENT',
            },
        },
        headers={'x-ya-service-ticket': 'dummy-service-ticket'},
    )
    data = await response.json()

    assert_that(response.status, equal_to(400))
    assert data['status'] == 'fail'
    assert data['data']['message'] == 'SCHEMA_VALIDATION_ERROR'
    assert data['data']['params']['data']['event_type'][0] == 'Unsupported value: SOME_EVENT'
