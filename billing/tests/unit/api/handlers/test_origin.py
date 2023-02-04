from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.delete import DeleteOriginAction

PARTNER_ID = uuid4()
ORIGIN_ID = uuid4()


@pytest.mark.asyncio
async def test_success(
    app,
    disable_tvm_checking,
):
    response = await app.delete(f'/api/web/v1/partners/{PARTNER_ID}/origins/{ORIGIN_ID}')
    data = await response.json()

    assert_that(response.status, equal_to(200))
    assert_that(data, equal_to({'status': 'success', 'code': 200, 'data': {}}))


@pytest.mark.asyncio
async def test_calls_delete_origin(
    app,
    disable_tvm_checking,
    mock_delete_origin,
):
    await app.delete(f'/api/web/v1/partners/{PARTNER_ID}/origins/{ORIGIN_ID}')

    mock_delete_origin.assert_run_once_with(
        partner_id=PARTNER_ID,
        origin_id=ORIGIN_ID,
    )


@pytest.fixture(autouse=True)
def mock_delete_origin(mock_action):
    return mock_action(DeleteOriginAction)
