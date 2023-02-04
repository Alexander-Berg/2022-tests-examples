import pytest

from billing.yandex_pay.yandex_pay.core.actions.tokenization.bulk_tokenization import (
    BulkTokenizationAction, extract_uid_from_string
)
from billing.yandex_pay.yandex_pay.core.entities.enums import SyncUserCardsTaskType


@pytest.fixture
def operation_key() -> str:
    return "test_key"


@pytest.mark.parametrize('line,expected_uid',
                         (('{"_id":-4.003404817e+09}', None),
                          ('{"_id":1}', 1),
                          ('{"_id":11178}', 11178),
                          ('{"_id":{"$numberLong":"1130000050456951"}}', 1130000050456951),
                          ('{"_id":{"$oid":"600039ead61f8bfd14492b79"}}', None),
                          ('"42"', 42),
                          ('322', 322),
                          ))
def test_extract_uid_from_string(line, expected_uid):
    assert extract_uid_from_string(line) == expected_uid


@pytest.mark.asyncio
async def test_should_create_state_and_update_last_processed(operation_key, storage):
    lines = ['228']
    await BulkTokenizationAction(operation_key=operation_key, lines=lines, restart=False).run()

    created_state = await storage.bulk_tokenization_state.get(operation_key)
    assert created_state.last_processed_uid == 228
    assert created_state.has_finished is True


@pytest.mark.asyncio
async def test_should_correctly_restart_if_last_processed_uid_is_none(operation_key, storage):
    lines = ['nothing']
    await BulkTokenizationAction(operation_key=operation_key, lines=lines, restart=False).run()

    new_lines = ['1']
    await BulkTokenizationAction(operation_key=operation_key, lines=new_lines, restart=False).run()

    state = await storage.bulk_tokenization_state.get(operation_key)
    assert state.last_processed_uid == 1


@pytest.mark.asyncio
async def test_can_resume(operation_key, storage):
    lines = ['1']
    await BulkTokenizationAction(operation_key=operation_key, lines=lines, restart=False).run()

    first_event = await storage.sync_user_cards_task.get(1)
    assert first_event.event_count == 1

    new_lines = ['1', '2']
    await BulkTokenizationAction(operation_key=operation_key, lines=new_lines, restart=False).run()

    first_event = await storage.sync_user_cards_task.get(1)
    second_event = await storage.sync_user_cards_task.get(2)
    assert first_event.event_count == 1
    assert second_event.event_count == 1


@pytest.mark.asyncio
async def test_can_restart(operation_key, storage):
    lines = ['1']
    await BulkTokenizationAction(operation_key=operation_key, lines=lines, restart=False).run()

    first_event = await storage.sync_user_cards_task.get(1)
    assert first_event.event_count == 1

    new_lines = ['1', '2']
    await BulkTokenizationAction(operation_key=operation_key, lines=new_lines, restart=True).run()

    first_event = await storage.sync_user_cards_task.get(1)
    second_event = await storage.sync_user_cards_task.get(2)
    assert first_event.event_count == 2
    assert second_event.event_count == 1


@pytest.mark.asyncio
async def test_should_create_event_with_bulk_type(operation_key, storage):
    lines = ['1']
    await BulkTokenizationAction(operation_key=operation_key, lines=lines, restart=False).run()

    event = await storage.sync_user_cards_task.get(1)
    assert event.task_type == SyncUserCardsTaskType.BULK
