import logging
from dataclasses import replace
from datetime import timedelta
from decimal import Decimal
from uuid import uuid4

import pytest

from sendr_pytest.matchers import convert_then_match
from sendr_utils import utcnow

from hamcrest import (
    assert_that,
    contains_string,
    equal_to,
    has_entries,
    has_items,
    has_length,
    has_properties,
    instance_of,
    match_equality,
)

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.customer import CreateCustomerAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.order.send_to_payments_history import (
    SendToPaymentsHistoryAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.order.status_event.process import (
    ProcessOrderStatusEventsAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.order.update_status import UpdateOrderStatusAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.transaction.sync import TriggerTransactionStatusSyncAction
from billing.yandex_pay_plus.yandex_pay_plus.core.order_state_machine import MachineState
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import (
    ClassicOrderStatus,
    OrderEventSource,
    OrderEventStatus,
    PaymentMethodType,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order import Order
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order_status_event import OrderStatusEvent

ORDER_AMOUNT = Decimal('1000')
CASHBACK_AMOUNT = Decimal('30')
CASHBACK_CATEGORY = Decimal('0.03')
ORDER_STATUS_SEQ = (ClassicOrderStatus.HOLD, ClassicOrderStatus.SUCCESS, ClassicOrderStatus.REFUND)


@pytest.fixture
async def customer():
    return await CreateCustomerAction(uid=456).run()


@pytest.fixture
async def trust_card_id(rands):
    return f'card-x{rands()}'


@pytest.fixture(autouse=True)
def mock_xts_default_cashback(yandex_pay_plus_settings):
    yandex_pay_plus_settings.CASHBACK_USER_SHEET_SPENDING_LIMIT['XTS'] = 1000
    yandex_pay_plus_settings.CASHBACK_CARD_SHEET_SPENDING_LIMIT['XTS'] = 3000


@pytest.fixture
async def order(storage, customer, rands, trust_card_id):
    return await storage.order.create(
        Order(
            uid=customer.uid,
            message_id=rands(),
            currency='XTS',
            amount=ORDER_AMOUNT,
            cashback=CASHBACK_AMOUNT,
            cashback_category=CASHBACK_CATEGORY,
            psp_id=uuid4(),
            merchant_id=uuid4(),
            status=ClassicOrderStatus.NEW,
            trust_card_id=trust_card_id,
            payment_method_type=PaymentMethodType.CARD,
        )
    )


@pytest.fixture(params=[ORDER_STATUS_SEQ])
async def events(storage, order: Order, request):
    events = []
    event_time = utcnow() - timedelta(hours=1)

    for idx, status in enumerate(request.param):
        event = await storage.order_status_event.create(
            OrderStatusEvent(
                event_id=uuid4(),
                message_id=order.message_id,
                order_status=status,
                event_time=event_time + timedelta(seconds=idx),
                event_status=OrderEventStatus.PENDING,
                run_at=event_time,
                event_source=OrderEventSource.PSP,
            )
        )
        events.append(event)

    return events


@pytest.fixture(autouse=True)
def mock_send_to_payments_history_action(mock_action):
    mock_action(SendToPaymentsHistoryAction)


@pytest.mark.asyncio
async def test_no_available_events_do_not_break_action(mocker):
    mock = mocker.patch.object(ProcessOrderStatusEventsAction, 'apply_events_to_order')

    await ProcessOrderStatusEventsAction().run()

    mock.assert_not_called()


class TestSuccessfulPass:
    @pytest.mark.asyncio
    async def test_events_applied_in_order(self, order, events, storage, mocker):
        spy = mocker.spy(UpdateOrderStatusAction, '__init__')

        await ProcessOrderStatusEventsAction().run()

        assert_that(spy.call_count, equal_to(len(events)))

        for idx, event in enumerate(events):
            loaded = await storage.order_status_event.get(event.event_id)
            assert_that(loaded.event_status, equal_to(OrderEventStatus.APPLIED))

            _, call_kwargs = spy.call_args_list[idx]
            assert_that(call_kwargs, has_entries(status=event.order_status))

        loaded_order = await storage.order.get(order.uid, order.order_id)
        assert_that(
            loaded_order,
            has_properties(
                status=equal_to(ClassicOrderStatus.REFUND),
                amount=ORDER_AMOUNT,
            )
        )

    @pytest.mark.asyncio
    async def test_machine_state_stored_in_event(self, order, events, storage):
        await ProcessOrderStatusEventsAction().run()

        expected_states = (
            (MachineState.CAPTURED, order.amount),
            (MachineState.CHARGED, order.amount),
            (MachineState.CANCELLED, Decimal(0)),
        )

        for event, expected in zip(events, expected_states):
            loaded = await storage.order_status_event.get(event.event_id)
            expected_state, expected_amount = expected
            assert_that(
                loaded,
                has_properties(
                    event_status=OrderEventStatus.APPLIED,
                    details=has_entries(
                        machine={
                            'state': match_equality(
                                convert_then_match(MachineState, expected_state)
                            ),
                            'amount': match_equality(
                                convert_then_match(Decimal, expected_amount)
                            ),
                        }
                    )
                )
            )

    @pytest.mark.asyncio
    async def test_multiple_passes(self, order, events, storage):
        # first pass
        await ProcessOrderStatusEventsAction().run()

        for event in events:
            loaded = await storage.order_status_event.get(event.event_id)
            assert_that(loaded.event_status, equal_to(OrderEventStatus.APPLIED))

        # new event arrived
        chargeback = await storage.order_status_event.create(
            OrderStatusEvent(
                event_id=uuid4(),
                message_id=order.message_id,
                order_status=ClassicOrderStatus.CHARGEBACK,
                event_time=utcnow() - timedelta(minutes=1),
                event_status=OrderEventStatus.PENDING,
                run_at=utcnow() - timedelta(minutes=1),
                event_source=OrderEventSource.PSP,
            )
        )

        # second pass
        await ProcessOrderStatusEventsAction().run()

        for event in events:
            loaded = await storage.order_status_event.get(event.event_id)
            assert_that(loaded.event_status, equal_to(OrderEventStatus.APPLIED))

        chargeback = await storage.order_status_event.get(chargeback.event_id)
        assert_that(chargeback.event_status, equal_to(OrderEventStatus.APPLIED))

    @pytest.mark.asyncio
    async def test_event_adjusts_order_amount(self, order, events, storage):
        event = events[1]
        event.params['amount'] = '10'
        await storage.order_status_event.save(event)

        await ProcessOrderStatusEventsAction().run()

        loaded_order = await storage.order.get(order.uid, order.order_id)
        assert_that(
            loaded_order,
            has_properties(
                status=equal_to(ClassicOrderStatus.REFUND),
                amount=Decimal(10),
            )
        )

    @pytest.mark.asyncio
    async def test_successful_pass_is_logged(self, order, events, caplog, dummy_logger):
        caplog.set_level(logging.INFO, logger=dummy_logger.logger.name)

        await ProcessOrderStatusEventsAction().run()

        logs = [r for r in caplog.records if r.name == dummy_logger.logger.name]
        assert_that(
            logs,
            has_items(
                has_properties(
                    message='Order events applied successfully',
                    levelno=logging.INFO,
                    _context=has_entries(
                        uid=order.uid,
                        applied_events_log=has_length(len(events)),
                        order=has_properties(message_id=order.message_id),
                    )
                )
            )
        )

    @pytest.mark.asyncio
    async def test_broken_fast_forward_still_passes(self, order, events, storage):
        await ProcessOrderStatusEventsAction().run()

        for event in events:
            event = await storage.order_status_event.get(event.event_id)
            assert_that(event, has_properties(event_status=OrderEventStatus.APPLIED))

        success = await storage.order_status_event.get(events[1].event_id)
        assert_that(
            success, has_properties(details=has_entries(machine=instance_of(dict)))
        )
        success.details.pop('machine')
        success.event_status = OrderEventStatus.PENDING
        await storage.order_status_event.save(success)

        await ProcessOrderStatusEventsAction().run()

        for event in events:
            event = await storage.order_status_event.get(event.event_id)
            assert_that(
                event,
                has_properties(
                    event_status=OrderEventStatus.APPLIED,
                    details=has_entries(machine=instance_of(dict)),
                )
            )


class TestFailingPass:
    @pytest.mark.asyncio
    @pytest.mark.parametrize('events', [ORDER_STATUS_SEQ + (ClassicOrderStatus.SUCCESS,)], indirect=True)
    async def test_invalid_order_state_is_skipped(self, events, storage, order):
        await ProcessOrderStatusEventsAction().run()

        *applied_events, last = events
        for event in applied_events:
            loaded = await storage.order_status_event.get(event.event_id)
            assert_that(loaded.event_status, equal_to(OrderEventStatus.APPLIED))

        last = await storage.order_status_event.get(events[-1].event_id)
        assert_that(
            last,
            has_properties(
                event_status=OrderEventStatus.SKIPPED,
                tries=1,
                details=has_entries(exception=contains_string('MachineError'))
            )
        )

        order = await storage.order.get(order.uid, order.order_id)
        assert_that(order.status, equal_to(ClassicOrderStatus.REFUND))

    @pytest.mark.asyncio
    async def test_skip_middle_event(self, events, storage, order):
        event = events[1]
        event.params['amount'] = '-1'
        await storage.order_status_event.save(event)

        await ProcessOrderStatusEventsAction().run()

        first, *rest = events
        loaded = await storage.order_status_event.get(first.event_id)
        assert_that(loaded.event_status, equal_to(OrderEventStatus.APPLIED))

        for event in rest:
            loaded = await storage.order_status_event.get(event.event_id)
            assert_that(loaded.event_status, equal_to(OrderEventStatus.SKIPPED))

        order = await storage.order.get(order.uid, order.order_id)
        assert_that(order.status, equal_to(ClassicOrderStatus.HOLD))

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'exception_cls,expected_event_status',
        [
            *(
                (ex, OrderEventStatus.SKIPPED)
                for ex in ProcessOrderStatusEventsAction.instantly_skip_exceptions
            ),
            # don't skip instantly, maybe retry will succeed
            (Exception, OrderEventStatus.PENDING),
        ]
    )
    async def test_first_invalid_event_skips_the_rest_of_the_pass(
        self, events, storage, order, mocker, exception_cls, expected_event_status
    ):
        mock = mocker.patch.object(
            ProcessOrderStatusEventsAction,
            '_apply_single_event_to_order',
            side_effect=exception_cls('test'),
        )

        await ProcessOrderStatusEventsAction().run()

        mock.assert_called_once()

        for event in events:
            loaded = await storage.order_status_event.get(event.event_id)
            assert_that(
                loaded,
                has_properties(
                    event_status=equal_to(expected_event_status),
                    tries=equal_to(1),
                )
            )

        order = await storage.order.get(order.uid, order.order_id)
        assert_that(order.status, equal_to(ClassicOrderStatus.NEW))

    @pytest.mark.asyncio
    async def test_exhausted_max_tries_triggers_event_skipping(
        self, events, storage, order, mocker
    ):
        mock = mocker.patch.object(
            ProcessOrderStatusEventsAction,
            '_apply_single_event_to_order',
            # without max_tries exhausted exception should have been retried
            side_effect=Exception,
        )
        for event in events:
            event.tries = ProcessOrderStatusEventsAction.max_tries
            await storage.order_status_event.save(event)

        await ProcessOrderStatusEventsAction().run()

        mock.assert_called_once()

        for event in events:
            loaded = await storage.order_status_event.get(event.event_id)
            assert_that(
                loaded,
                has_properties(
                    event_status=equal_to(OrderEventStatus.SKIPPED),
                    tries=equal_to(ProcessOrderStatusEventsAction.max_tries + 1),
                )
            )

        order = await storage.order.get(order.uid, order.order_id)
        assert_that(order.status, equal_to(ClassicOrderStatus.NEW))

    @pytest.mark.asyncio
    async def test_unsuccessful_pass_is_logged(
        self, order, events, caplog, dummy_logger, mocker
    ):
        msg = 'You shall not pass!'
        mocker.patch.object(
            ProcessOrderStatusEventsAction,
            '_apply_single_event_to_order',
            side_effect=Exception(msg),
        )
        caplog.set_level(logging.INFO, logger=dummy_logger.logger.name)

        await ProcessOrderStatusEventsAction().run()

        logs = [r for r in caplog.records if r.name == dummy_logger.logger.name]
        assert_that(
            logs,
            has_items(
                has_properties(
                    message='Order events applied with exception',
                    levelno=logging.ERROR,
                    _context=has_entries(
                        uid=order.uid,
                        applied_events_log=has_length(0),
                        failed_event_index=0,
                        failed_event=has_properties(event_id=events[0].event_id),
                    )
                )
            )
        )


class TestCheckoutOrder:
    @pytest.fixture(autouse=True)
    def mock_trigger_transaction_sync(self, mock_action):
        return mock_action(TriggerTransactionStatusSyncAction)

    @pytest.fixture
    async def transaction(self, storage, stored_transaction, order):
        return await storage.transaction.save(
            replace(stored_transaction, message_id=order.message_id)
        )

    @pytest.mark.asyncio
    async def test_psp_events_are_ignored(self, order, events, storage, mocker, transaction):
        await ProcessOrderStatusEventsAction().run()

        for idx, event in enumerate(events):
            loaded = await storage.order_status_event.get(event.event_id)
            assert_that(loaded.event_status, equal_to(OrderEventStatus.IGNORED))

    @pytest.mark.asyncio
    async def test_psp_event_triggers_transaction_sync(
        self, order, events, storage, mocker, transaction, mock_trigger_transaction_sync
    ):
        await ProcessOrderStatusEventsAction().run()

        mock_trigger_transaction_sync.assert_called_once_with(transaction=transaction)

    @pytest.mark.asyncio
    async def test_checkout_event_is_applied(self, order, storage, mocker, transaction):
        spy = mocker.spy(UpdateOrderStatusAction, '__init__')
        event_time = utcnow() - timedelta(minutes=1)
        checkout_event = await storage.order_status_event.create(
            OrderStatusEvent(
                event_id=uuid4(),
                message_id=order.message_id,
                order_status=ClassicOrderStatus.HOLD,
                event_time=event_time,
                event_status=OrderEventStatus.PENDING,
                run_at=event_time,
                event_source=OrderEventSource.CHECKOUT,
            )
        )

        await ProcessOrderStatusEventsAction().run()

        assert_that(spy.call_count, equal_to(1))
        checkout_event = await storage.order_status_event.get(checkout_event.event_id)
        assert_that(checkout_event.event_status, equal_to(OrderEventStatus.APPLIED))

    @pytest.mark.asyncio
    async def test_ignored_events_are_skipped(self, order, storage, mocker):
        spy = mocker.spy(UpdateOrderStatusAction, '__init__')
        event_time = utcnow() - timedelta(minutes=1)
        await storage.order_status_event.create(
            OrderStatusEvent(
                event_id=uuid4(),
                message_id=order.message_id,
                order_status=ClassicOrderStatus.HOLD,
                event_time=event_time,
                event_status=OrderEventStatus.IGNORED,
                run_at=event_time,
                event_source=OrderEventSource.CHECKOUT,
            )
        )

        await ProcessOrderStatusEventsAction().run()

        assert_that(spy.call_count, equal_to(0))
