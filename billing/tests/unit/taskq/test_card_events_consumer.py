import datetime
import json

import pytest

from billing.yandex_pay.yandex_pay.taskq.card_events_consumer import (
    BindingEventSchema, EventKind, TrustBindingsEventsWorker
)


def message(*, kind: str, event_dt: datetime.datetime) -> str:
    return json.dumps(
        {
            "uid": 123456,
            "card_id": str("some-card-id"),
            "kind": kind,
            "event_dt": event_dt.isoformat(),
        }
    )


@pytest.fixture
def logbroker_mock(mocker):
    return mocker.MagicMock()


@pytest.fixture
def logbroker_consumer_mock(mocker):
    def _consumer(data=None):
        commit = mocker.AsyncMock()
        mocker.patch('sendr_logbroker.logbroker.Consumer.messages', mocker.AsyncMock(return_value=iter(data or [])))
        mocker.patch('sendr_logbroker.logbroker.Consumer._create', mocker.AsyncMock())
        mocker.patch('sendr_logbroker.logbroker.Consumer.commit', commit)
        return commit
    return _consumer


@pytest.mark.asyncio
async def test_basic(
    mocker,
    mocked_logger,
    logbroker_mock,
    logbroker_consumer_mock,
    yandex_pay_settings,
):
    yandex_pay_settings.CARD_EVENTS_UID_WHITELIST_ENABLED = False
    worker = TrustBindingsEventsWorker(logger=mocked_logger)
    now = datetime.datetime.utcnow()
    worker.logbroker = logbroker_mock()
    logbroker_consumer_mock(data=[
        message(kind='binding_update', event_dt=now)
    ])
    worker.process_message_count = 1
    worker.sync_user = mocker.AsyncMock()

    await worker.process_task()
    data = worker.sync_user.call_args.args[0]

    assert worker.sync_user.call_count == 1
    assert data.uid == 123456
    assert data.card_id == "some-card-id"
    assert data.kind == EventKind.BINDING_UPDATE
    assert data.event_dt == now


@pytest.mark.asyncio
async def test_dont_commit_when_sync_user_error(
    mocker,
    mocked_logger,
    logbroker_mock,
    logbroker_consumer_mock,
    yandex_pay_settings,
):
    yandex_pay_settings.CARD_EVENTS_UID_WHITELIST_ENABLED = False
    worker = TrustBindingsEventsWorker(logger=mocked_logger)
    now = datetime.datetime.utcnow()
    worker.logbroker = logbroker_mock()
    commit = logbroker_consumer_mock(data=[
        message(kind='binding_update', event_dt=now)
    ])
    worker.process_message_count = 1
    worker.sync_user = mocker.AsyncMock(side_effect=Exception())

    with pytest.raises(Exception):
        await worker.process_task()

    assert commit.call_count == 0


@pytest.mark.asyncio
async def test_read_many_messages(
    mocker,
    mocked_logger,
    logbroker_mock,
    logbroker_consumer_mock,
    yandex_pay_settings,
):
    yandex_pay_settings.CARD_EVENTS_UID_WHITELIST_ENABLED = False
    worker = TrustBindingsEventsWorker(logger=mocked_logger)
    now = datetime.datetime.utcnow()
    data = [
        message(kind='binding_update', event_dt=now),
        message(kind='unbind', event_dt=now),
    ]
    commit = logbroker_consumer_mock(data=data)
    worker.logbroker = logbroker_mock()
    worker.process_message_count = 2
    worker.sync_user = mocker.AsyncMock()
    await worker.process_task()

    data = worker.sync_user.call_args.args[0]

    assert worker.sync_user.call_count == 2
    assert commit.call_count == 2
    assert data.uid == 123456
    assert data.card_id == "some-card-id"
    assert data.kind == EventKind.UNBIND
    assert data.event_dt == now


def test_schema():
    schema = BindingEventSchema()
    now = datetime.datetime.utcnow()
    raw = message(kind='binding_update', event_dt=now)
    data = schema.loads(raw).data

    assert data.uid == 123456
    assert data.card_id == "some-card-id"
    assert data.kind == EventKind.BINDING_UPDATE
    assert data.event_dt == now
