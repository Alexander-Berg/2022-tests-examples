from unittest.mock import Mock

import pytest

from maps_adv.common.helpers import AsyncContextManagerMock
from maps_adv.geosmb.doorman.client import Source
from maps_adv.geosmb.harmonist.server.lib.enums import PipelineStep, StepStatus

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture(autouse=True)
def common_mocks(dm):
    dm.list_unimported_creation_entries.coro.return_value = [
        dict(
            session_id="session_id_1",
            biz_id=123,
            segment="orange",
            clients=[{"email": "some1@yandex.ru"}, {"phone": 111}],
        ),
        dict(
            session_id="session_id_2",
            biz_id=321,
            segment="green",
            clients=[{"email": "some2@yandex.ru"}, {"phone": 222}],
        ),
    ]


async def test_fetches_unimported_list(domain, dm):
    await domain.process_unimported()

    dm.list_unimported_creation_entries.assert_called_once()


async def test_locks_creation_log(mocker, domain, dm, lock_manager):
    mocker.patch.object(
        lock_manager,
        "try_lock_creation_entry",
        Mock(return_value=AsyncContextManagerMock()),
    )

    await domain.process_unimported()

    lock_manager.try_lock_creation_entry.assert_any_call("session_id_1")
    lock_manager.try_lock_creation_entry.assert_any_call("session_id_2")


async def test_calls_doorman_for_import(domain, doorman):
    await domain.process_unimported()

    doorman.create_clients.assert_any_call(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="orange",
        clients=[{"email": "some1@yandex.ru"}, {"phone": 111}],
    )
    doorman.create_clients.assert_any_call(
        biz_id=321,
        source=Source.CRM_INTERFACE,
        label="green",
        clients=[{"email": "some2@yandex.ru"}, {"phone": 222}],
    )


async def test_marks_creation_log_failed_if_doorman_exception(domain, dm, doorman):
    doorman.create_clients.coro.side_effect = Exception("Boom!")

    await domain.process_unimported()

    dm.update_log_status.assert_any_call(
        session_id="session_id_1",
        biz_id=123,
        pipeline_step=PipelineStep.IMPORTING_CLIENTS,
        status=StepStatus.FAILED,
        failed_reason="Failed to import clients",
    )
    dm.update_log_status.assert_any_call(
        session_id="session_id_2",
        biz_id=321,
        pipeline_step=PipelineStep.IMPORTING_CLIENTS,
        status=StepStatus.FAILED,
        failed_reason="Failed to import clients",
    )
