from uuid import uuid4

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import (
    ClassicOrderStatus,
    OrderEventSource,
    OrderEventStatus,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order_status_event import OrderStatusEvent


def make_event(**kwargs):
    kwargs = dict(
        event_id=uuid4(),
        message_id='1:msgid',
        order_status=ClassicOrderStatus.HOLD,
        event_time=utcnow(),
        event_status=OrderEventStatus.PENDING,
        event_source=OrderEventSource.PSP,
    ) | kwargs
    return OrderStatusEvent(**kwargs)


@pytest.mark.asyncio
async def test_create(storage):
    run_at = utcnow()
    event = make_event(run_at=run_at)

    created = await storage.order_status_event.create(event)

    event.event_id = created.event_id
    event.created = created.created
    event.updated = created.updated
    assert_that(created, equal_to(event))


@pytest.mark.parametrize('event_status', list(OrderEventStatus))
@pytest.mark.asyncio
async def test_event_status_creatable(storage, event_status):
    run_at = utcnow()
    event = make_event(run_at=run_at, event_status=event_status)

    created = await storage.order_status_event.create(event)

    assert_that(created.event_status, equal_to(event_status))


@pytest.mark.parametrize('event_source', list(OrderEventSource))
@pytest.mark.asyncio
async def test_event_source_creatable(storage, event_source):
    run_at = utcnow()
    event = make_event(run_at=run_at, event_source=event_source)

    created = await storage.order_status_event.create(event)

    assert_that(created.event_source, equal_to(event_source))


@pytest.mark.asyncio
async def test_get(storage):
    event = make_event()

    created = await storage.order_status_event.create(event)
    loaded = await storage.order_status_event.get(created.event_id)

    assert_that(loaded, equal_to(created))


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(OrderStatusEvent.DoesNotExist):
        await storage.order_status_event.get(uuid4())


@pytest.mark.asyncio
async def test_save(storage):
    event = make_event()

    created = await storage.order_status_event.create(event)
    created.message_id = '2:msgid'
    created.params['amount'] = '10'
    created.order_status = ClassicOrderStatus.REFUND
    created.event_status = OrderEventStatus.APPLIED

    saved = await storage.order_status_event.save(created)

    created.updated = saved.updated
    assert_that(saved, equal_to(created))


@pytest.mark.asyncio
async def test_unique_message_id_and_event_time(storage):
    event_time = utcnow()
    await storage.order_status_event.create(
        make_event(
            message_id='msg-id',
            event_time=event_time,
            order_status=ClassicOrderStatus.HOLD,
        )
    )

    with pytest.raises(OrderStatusEvent.EventDuplicateKey):
        await storage.order_status_event.create(
            make_event(
                message_id='msg-id',
                event_time=event_time,
                order_status=ClassicOrderStatus.SUCCESS,
            )
        )


class TestFindByMessageId:
    @pytest.mark.asyncio
    async def test_find_one(self, storage):
        event = await storage.order_status_event.create(make_event())

        assert_that(
            await storage.order_status_event.find_by_message_id(event.message_id),
            equal_to([event]),
        )

    @pytest.mark.asyncio
    async def test_find_multiple(self, storage):
        event1 = await storage.order_status_event.create(make_event())
        event2 = await storage.order_status_event.create(
            make_event(order_status=ClassicOrderStatus.SUCCESS)
        )

        assert_that(
            await storage.order_status_event.find_by_message_id(event1.message_id),
            equal_to([event1, event2]),
        )

    @pytest.mark.asyncio
    async def test_find_empty(self, storage):
        assert_that(
            await storage.order_status_event.find_by_message_id('missing'),
            equal_to([]),
        )


class TestGetByMessageIdAndEventTime:
    @pytest.mark.asyncio
    async def test_success(self, storage):
        event = await storage.order_status_event.create(make_event())

        assert_that(
            await storage.order_status_event.get_by_message_id_and_event_time(event.message_id, event.event_time),
            equal_to(event),
        )

    @pytest.mark.asyncio
    async def test_not_found(self, storage):
        event = make_event()

        with pytest.raises(OrderStatusEvent.DoesNotExist):
            await storage.order_status_event.get_by_message_id_and_event_time(event.message_id, event.event_time)
