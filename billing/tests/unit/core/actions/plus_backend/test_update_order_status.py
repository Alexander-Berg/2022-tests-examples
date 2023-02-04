import logging
from decimal import Decimal

import pytest

from sendr_taskqueue.worker.storage.db.entities import TaskState
from sendr_utils import alist, utcnow

from hamcrest import assert_that, has_entries, has_properties

from billing.yandex_pay.yandex_pay.core.actions.plus_backend.update_order_status import (
    YandexPayPlusUpdateOrderStatusAction
)
from billing.yandex_pay.yandex_pay.core.entities.task import Task
from billing.yandex_pay.yandex_pay.core.exceptions import CoreEventAlreadyExistsError
from billing.yandex_pay.yandex_pay.interactions import YandexPayPlusClient
from billing.yandex_pay.yandex_pay.interactions.plus_backend import OrderStatus
from billing.yandex_pay.yandex_pay.interactions.plus_backend.exceptions import OrderEventAlreadyExistsError
from billing.yandex_pay.yandex_pay.utils.stats import pay_plus_update_order_failures


@pytest.fixture
def message_id(rands):
    return rands()


@pytest.fixture
def event_time():
    return utcnow()


@pytest.fixture(autouse=True)
def mocked_update_order_status_request(mocker):
    return mocker.patch.object(YandexPayPlusClient, 'update_order_status', mocker.AsyncMock())


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'amount,serialized_amount', [(Decimal('1.00'), '1.00'), (None, None)]
)
async def test_serialize_kwargs(message_id, event_time, storage, amount, serialized_amount):
    await YandexPayPlusUpdateOrderStatusAction(
        message_id=message_id,
        status=OrderStatus.SUCCESS,
        event_time=event_time,
        amount=amount,
    ).run_async()

    filters = {'action_name': YandexPayPlusUpdateOrderStatusAction.action_name}
    [task] = await alist(storage.task.find(filters=filters))
    assert_that(
        task,
        has_properties(
            state=TaskState.PENDING,
            params=has_entries(
                max_retries=10,
                action_kwargs=dict(
                    message_id=message_id,
                    status='success',
                    event_time=event_time.isoformat(sep=' '),
                    amount=serialized_amount,
                )
            )
        )
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('amount', [Decimal('1.00'), None])
async def test_deserialize_kwargs(message_id, event_time, storage, amount):
    status = OrderStatus.FAIL
    await YandexPayPlusUpdateOrderStatusAction(
        message_id=message_id,
        status=status,
        event_time=event_time,
        amount=amount,
    ).run_async()

    filters = {'action_name': YandexPayPlusUpdateOrderStatusAction.action_name}
    [task] = await alist(storage.task.find(filters=filters))

    loaded = YandexPayPlusUpdateOrderStatusAction(
        **YandexPayPlusUpdateOrderStatusAction.deserialize_kwargs(task.params['action_kwargs'])
    )
    assert_that(
        loaded,
        has_properties(
            message_id=message_id,
            status=status,
            event_time=event_time,
            amount=amount,
        )
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('status', list(OrderStatus))
async def test_update_order_status(
    message_id, event_time, status, mocked_update_order_status_request
):
    amount = Decimal('1.00')

    await YandexPayPlusUpdateOrderStatusAction(
        message_id=message_id,
        status=status,
        event_time=event_time,
        amount=amount,
        payment_id='x',
        recurring=True,
    ).run()

    mocked_update_order_status_request.assert_awaited_once_with(
        message_id=message_id,
        status=status,
        event_time=event_time,
        amount=amount,
        payment_id='x',
        recurring=True,
    )


@pytest.mark.asyncio
async def test_action_call_logged(
    message_id, event_time, caplog, dummy_logger
):
    caplog.set_level(logging.INFO, logger=dummy_logger.logger.name)
    amount = Decimal('1.00')
    status = OrderStatus.HOLD

    await YandexPayPlusUpdateOrderStatusAction(
        message_id=message_id,
        status=status,
        event_time=event_time,
        amount=amount,
    ).run()

    [log] = [r for r in caplog.records if r.name == dummy_logger.logger.name]
    assert_that(
        log,
        has_properties(
            message='Order status updated',
            levelno=logging.INFO,
            _context=has_entries(
                message_id=message_id,
                status=status,
                event_time=event_time,
                amount=amount,
            ),
        )
    )


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'exception,log_message,levelno,core_exception',
    [
        (
            OrderEventAlreadyExistsError(
                message='test',
                status_code=409,
                method='patch',
                service=YandexPayPlusClient.SERVICE,
            ),
            'Order event already exists',
            logging.WARNING,
            CoreEventAlreadyExistsError,
        ),
    ]
)
async def test_errors_logged(
    message_id,
    event_time,
    exception,
    log_message,
    levelno,
    core_exception,
    mocked_update_order_status_request,
    caplog,
    dummy_logger,
):
    caplog.set_level(logging.INFO, logger=dummy_logger.logger.name)
    mocked_update_order_status_request.side_effect = exception
    status = OrderStatus.REFUND
    amount = None

    with pytest.raises(core_exception):
        await YandexPayPlusUpdateOrderStatusAction(
            message_id=message_id,
            status=status,
            event_time=event_time,
            amount=amount,
        ).run()

    [log] = [r for r in caplog.records if r.name == dummy_logger.logger.name]
    assert_that(
        log,
        has_properties(
            message=log_message,
            levelno=levelno,
            _context=has_entries(
                message_id=message_id,
                status=status,
                event_time=event_time,
                amount=amount,
            ),
        )
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('reason', [None, 'fake_reason'])
async def test_report_task_failure(caplog, dummy_logger, mocker, reason):
    caplog.set_level(logging.INFO, logger=dummy_logger.logger.name)

    mock_counter_inc = mocker.patch.object(pay_plus_update_order_failures, 'inc')
    task = mocker.Mock(spec=Task)
    await YandexPayPlusUpdateOrderStatusAction.report_task_failure(
        task=task, reason=reason
    )

    mock_counter_inc.assert_called_once_with()
    [log] = [r for r in caplog.records if r.name == dummy_logger.logger.name]
    assert_that(
        log,
        has_properties(
            message='Failed to update plus cashback order status',
            levelno=logging.ERROR,
            _context=has_entries(
                task=task,
                reason=reason
            ),
        )
    )
