from uuid import UUID

import pytest

from sendr_pytest.mocks import mock_action

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.threeds.method_result import ThreeDSMethodResultAction

_dummy_use_fixture = [mock_action]
TRANSACTION_ID = '1d94f624-1e41-4e05-9653-d944c347a672'


@pytest.mark.asyncio
async def test_returned(public_app):
    res = await public_app.post(f'/3ds/method-result/{TRANSACTION_ID}')

    assert_that(res.status, equal_to(200))
    assert_that(
        await res.text(),
        equal_to('the-html'),
    )


@pytest.mark.asyncio
async def test_calls_action(public_app, mock_tds_method_result_action):
    await public_app.post(f'/3ds/method-result/{TRANSACTION_ID}', raise_for_status=True)

    mock_tds_method_result_action.assert_run_once_with(
        transaction_id=UUID(TRANSACTION_ID),
    )


@pytest.fixture(autouse=True)
def mock_tds_method_result_action(mock_action):
    return mock_action(ThreeDSMethodResultAction, return_value='the-html')
