import pytest

from hamcrest import assert_that, equal_to


@pytest.mark.usefixtures('mock_app_authentication')
@pytest.mark.asyncio
async def test_bills_state_getting_works(app, user):
    resp = await app.get('/api/v1/bills')
    json_body = await resp.json()

    assert_that(resp.status, equal_to(200))
    assert_that(
        json_body,
        equal_to({'status': 'success', 'code': 200, 'data': {'state': 'syncing', 'bills': []}}),
    )
