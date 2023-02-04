import logging
from uuid import uuid4

import pytest
from pay.lib.interactions.merchant.entities import MerchantResponse, TransactionStatus, UpdateTransactionStatusRequest

from sendr_interactions.exceptions import InteractionResponseError
from sendr_taskqueue.worker.storage.db.entities import TaskState
from sendr_utils import alist, utcnow

from hamcrest import assert_that, contains, equal_to, has_entries, has_item, has_properties

from billing.yandex_pay.yandex_pay.core.actions.merchant.update_order import UpdateMerchantOrderAction
from billing.yandex_pay.yandex_pay.core.exceptions import MerchantClientRateLimitError, MerchantOrderUpdateError
from billing.yandex_pay.yandex_pay.interactions.merchant import MerchantClient
from billing.yandex_pay.yandex_pay.utils.stats import merchant_order_update_failures


@pytest.fixture
def merchant_id():
    return uuid4()


@pytest.fixture
def external_id(rands):
    return rands()


@pytest.fixture
def event_time():
    return utcnow()


@pytest.fixture
def run_action(merchant_id, external_id, event_time):
    async def _inner(**kwargs):
        kwargs = {
            'merchant_id': merchant_id,
            'external_id': external_id,
            'status': TransactionStatus.SUCCESS,
            'event_time': event_time,
        } | kwargs
        await UpdateMerchantOrderAction(**kwargs).run()
    return _inner


@pytest.fixture(autouse=True)
def mock_update_transaction_status(mocker):
    mock = mocker.AsyncMock(return_value=MerchantResponse(code=200))
    return mocker.patch.object(MerchantClient, 'update_transaction_status', mock)


@pytest.mark.asyncio
async def test_serialize_kwargs(merchant_id, external_id, event_time, storage):
    await UpdateMerchantOrderAction(
        merchant_id=merchant_id,
        external_id=external_id,
        status=TransactionStatus.SUCCESS,
        event_time=event_time,
    ).run_async()

    filters = {'action_name': UpdateMerchantOrderAction.action_name}
    [task] = await alist(storage.task.find(filters=filters))
    assert_that(
        task,
        has_properties(
            state=TaskState.PENDING,
            params=has_entries(
                max_retries=UpdateMerchantOrderAction.max_retries,
                action_kwargs={
                    'merchant_id': str(merchant_id),
                    'external_id': external_id,
                    'status': TransactionStatus.SUCCESS.value,
                    'event_time': event_time.isoformat(sep=' '),
                }
            )
        )
    )


@pytest.mark.asyncio
async def test_deserialize_kwargs(merchant_id, external_id, event_time, storage):
    await UpdateMerchantOrderAction(
        merchant_id=merchant_id,
        external_id=external_id,
        status=TransactionStatus.PARTIAL_REFUND,
        event_time=event_time,
    ).run_async()

    filters = {'action_name': UpdateMerchantOrderAction.action_name}
    [task] = await alist(storage.task.find(filters=filters))

    action_kwargs = task.params['action_kwargs']
    loaded = UpdateMerchantOrderAction(
        **UpdateMerchantOrderAction.deserialize_kwargs(action_kwargs)
    )

    assert_that(
        loaded,
        has_properties(
            merchant_id=merchant_id,
            external_id=external_id,
            status=TransactionStatus.PARTIAL_REFUND,
            event_time=event_time,
        )
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('status', list(TransactionStatus))
async def test_update_order(
    merchant_id,
    external_id,
    status,
    event_time,
    run_action,
    mock_update_transaction_status,
    yandex_pay_settings,
):
    await run_action(status=status)

    mock_update_transaction_status.assert_called_once_with(
        base_url=yandex_pay_settings.SPLIT_MERCHANT_URL,
        merchant_id=merchant_id,
        data=UpdateTransactionStatusRequest(
            order_id=external_id,
            status=status,
            event_time=event_time,
        ),
    )


@pytest.mark.asyncio
async def test_call_logged(
    merchant_id,
    external_id,
    event_time,
    run_action,
    dummy_logs,
):
    await run_action()

    logs = dummy_logs()
    assert_that(
        logs,
        contains(
            *[
                has_properties(
                    message=msg,
                    levelno=logging.INFO,
                    _context=has_entries(
                        merchant_id=merchant_id,
                        external_id=external_id,
                        status=TransactionStatus.SUCCESS,
                        event_time=event_time,
                    ),
                )
                for msg in ('MERCHANT_ORDER_UPDATE_REQUESTED', 'MERCHANT_ORDER_UPDATED')
            ]
        )
    )


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'error_code,exc_cls,metric_inc',
    [
        (429, MerchantClientRateLimitError, 0),
        (400, MerchantOrderUpdateError, 1),
        (500, InteractionResponseError, 1),
    ],
)
async def test_merchant_failure(
    run_action,
    mock_update_transaction_status,
    error_code,
    exc_cls,
    metric_inc,
    dummy_logs,
):
    mock_update_transaction_status.side_effect = InteractionResponseError(
        status_code=error_code,
        method='post',
        service='test',
    )
    before = merchant_order_update_failures.get()

    with pytest.raises(exc_cls):
        await run_action()

    after = merchant_order_update_failures.get()
    assert_that(after[0][1] - before[0][1], equal_to(metric_inc))

    if metric_inc:
        assert_that(
            dummy_logs(),
            has_item(
                has_properties(
                    message='MERCHANT_ORDER_UPDATE_ERROR',
                    levelno=logging.ERROR,
                )
            )
        )
