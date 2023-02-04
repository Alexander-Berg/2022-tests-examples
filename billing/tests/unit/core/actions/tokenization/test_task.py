from uuid import UUID

import pytest

from hamcrest import assert_that, has_properties

from billing.yandex_pay.yandex_pay.core.actions.tokenization.task import (
    ProcessTokenizationTaskAction, TokenizationStatusDeletedError, TokenizationStatusExpiredError,
    TokenizationStatusInactiveError, TokenizationStatusSuspendedError
)
from billing.yandex_pay.yandex_pay.core.entities.enums import TSPTokenStatus
from billing.yandex_pay.yandex_pay.tests.utils import patch_storage_in_action


@pytest.fixture
def card_id():
    return UUID(int=0)


@pytest.fixture
def tsp_token_status(request):
    return getattr(request, 'param', TSPTokenStatus.ACTIVE)


@pytest.fixture
def mock_tokenization_action_cls(mocker, tsp_token_status):
    action_cls = mocker.Mock()
    run_async_mock = mocker.AsyncMock()
    run_async_mock.return_value.tsp_token_status = tsp_token_status
    action_cls.return_value.run = run_async_mock
    return action_cls


@pytest.mark.asyncio
async def test_tokenization_action(mock_tokenization_action_cls, card_id):
    task_action = ProcessTokenizationTaskAction(
        mock_tokenization_action_cls, str(card_id)
    )
    expected_properties = {
        'card_id': card_id,
        'tokenization_action_cls': mock_tokenization_action_cls,
        'merchant_id': None,
    }
    assert_that(task_action, has_properties(expected_properties))

    await task_action.run()
    mock_tokenization_action_cls.assert_called_once_with(card_id=card_id)
    mock_tokenization_action_cls.return_value.run.assert_awaited_once_with()


@pytest.mark.asyncio
async def test_tokenization_skipped_if_token_already_active(
    storage, mock_tokenization_action_cls, card_id, mocker
):
    task_action = ProcessTokenizationTaskAction(mock_tokenization_action_cls, card_id)

    mock_enrollment = mocker.AsyncMock()
    mock_enrollment.return_value.tsp_token_status = TSPTokenStatus.ACTIVE
    mocker.patch.object(storage.enrollment, 'get_by_card_id_and_merchant_id', mock_enrollment)
    patch_storage_in_action(task_action, storage, mocker)

    await task_action.run()

    mock_enrollment.assert_awaited_once_with(card_id=card_id, merchant_id=None)
    mock_tokenization_action_cls.assert_not_called()


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'tsp_token_status,expected_exception_class',
    [
        (TSPTokenStatus.INACTIVE, TokenizationStatusInactiveError),
        (TSPTokenStatus.SUSPENDED, TokenizationStatusSuspendedError),
        (TSPTokenStatus.DELETED, TokenizationStatusDeletedError),
        (TSPTokenStatus.EXPIRED, TokenizationStatusExpiredError),
    ],
    indirect=['tsp_token_status'],
)
async def test_tokenization_error_for_token_status(
    mock_tokenization_action_cls, card_id, expected_exception_class
):
    task_action = ProcessTokenizationTaskAction(mock_tokenization_action_cls, card_id)

    with pytest.raises(expected_exception_class):
        await task_action.run()

    mock_tokenization_action_cls.assert_called_once_with(card_id=card_id)
    mock_tokenization_action_cls.return_value.run.assert_awaited_once_with()
