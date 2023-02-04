from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.layout.delete import DeleteLayoutAction

PARTNER_ID = uuid4()
LAYOUT_ID = uuid4()
FILE_CONTENT = b'1' * 1000


@pytest.mark.asyncio
async def test_success(
    app,
    disable_tvm_checking,
):
    response = await app.delete(f'/api/web/v1/partners/{PARTNER_ID}/layouts/{LAYOUT_ID}')

    assert_that(response.status, equal_to(200))
    data = await response.json()
    assert_that(
        data,
        equal_to(
            {
                'status': 'success',
                'code': 200,
                'data': {},
            }
        ),
    )


@pytest.mark.asyncio
async def test_calls_delete_layout(app, disable_tvm_checking, mock_delete_layout):
    await app.delete(f'/api/web/v1/partners/{PARTNER_ID}/layouts/{LAYOUT_ID}')

    mock_delete_layout.assert_run_once_with(
        partner_id=PARTNER_ID,
        layout_id=LAYOUT_ID,
    )


@pytest.fixture(autouse=True)
def mock_delete_layout(mock_action):
    return mock_action(DeleteLayoutAction)
