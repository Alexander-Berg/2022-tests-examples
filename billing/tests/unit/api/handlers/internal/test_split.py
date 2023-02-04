import pytest
from pay.lib.interactions.split.entities import YandexSplitOrderStatus

from billing.yandex_pay_plus.yandex_pay_plus.api.handlers.internal.base import BaseInternalHandler
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.split.process_callback import ProcessSplitCallbackAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.enums import SplitCallbackEventType

URL = '/api/internal/v2/split/callback'


@pytest.fixture(autouse=True)
def mock_internal_tvm(mocker):
    return mocker.patch.object(
        BaseInternalHandler.TICKET_CHECKER,
        'check_tvm_service_ticket',
        mocker.AsyncMock(),
    )


@pytest.fixture(autouse=True)
def action(mock_action):
    return mock_action(ProcessSplitCallbackAction)


@pytest.fixture
def data():
    return {
        'order_id': '123',
        'external_id': '456',
        'status': 'approved',
        'merchant_id': 'yapay',
    }


@pytest.mark.asyncio
@pytest.mark.parametrize('status', list(YandexSplitOrderStatus))
@pytest.mark.parametrize('event_type', list(SplitCallbackEventType))
async def test_success(app, data, status, event_type, action, mock_internal_tvm):
    data['status'] = status.value
    await app.post(
        URL,
        json=data,
        params={
            'event_type': event_type.value
        },
        raise_for_status=True,
    )

    action.assert_called_once_with(
        order_id='123',
        external_id='456',
        status=status,
        split_merchant_id='yapay',
        event_type=event_type,
    )
    mock_internal_tvm.assert_called_once()
