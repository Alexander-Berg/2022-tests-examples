import asyncio
from unittest.mock import Mock

import pytest
from smb.common.testing_utils import dt

from maps_adv.common.helpers import AsyncContextManagerMock
from maps_adv.geosmb.doorman.client import Source
from maps_adv.geosmb.harmonist.server.lib.enums import PipelineStep, StepStatus
from maps_adv.geosmb.harmonist.server.lib.exceptions import (
    ClientsAlreadyWereImported,
    InvalidSessionId,
    ValidationNotFinished,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_raises_for_unknown_session_id(domain, dm):
    dm.fetch_clients_creation_log.coro.return_value = None

    with pytest.raises(
        InvalidSessionId,
        match="Session not found: session_id=f8d049c53e04, biz_id=123",
    ):
        await domain.import_clients(session_id="f8d049c53e04", biz_id=123)


async def test_raises_if_import_already_has_been_done(domain, dm):
    dm.fetch_clients_creation_log.coro.return_value = {
        "_id": "f8d049c53e04",
        "import_result": {"created": 1, "merged": 2},
    }

    with pytest.raises(
        ClientsAlreadyWereImported,
        match="Clients for session_id f8d049c53e04 already imported.",
    ):
        await domain.import_clients(session_id="f8d049c53e04", biz_id=123)


@pytest.mark.parametrize(
    "step", [s for s in PipelineStep if s != PipelineStep.VALIDATING_DATA]
)
async def test_raises_if_no_validating_step(domain, dm, step):
    dm.fetch_clients_creation_log.coro.return_value = {
        "log_history": [
            {
                "step": step,
                "status": StepStatus.FINISHED,
                "created_at": dt("2020-01-01 00:00:00"),
            },
        ],
    }

    with pytest.raises(
        ValidationNotFinished,
        match="Validation for session_id f8d049c53e04 hasn't finished yet.",
    ):
        await domain.import_clients(session_id="f8d049c53e04", biz_id=123)


@pytest.mark.parametrize(
    "validation_status", [s for s in StepStatus if s != StepStatus.FINISHED]
)
async def test_raises_if_no_finished_validation_step(domain, dm, validation_status):
    dm.fetch_clients_creation_log.coro.return_value = {
        "log_history": [
            {
                "step": "VALIDATING_DATA",
                "status": validation_status,
                "created_at": dt("2020-01-01 00:00:00"),
            },
        ],
    }

    with pytest.raises(
        ValidationNotFinished,
        match="Validation for session_id f8d049c53e04 hasn't finished yet.",
    ):
        await domain.import_clients(session_id="f8d049c53e04", biz_id=123)


async def test_saves_empty_result_if_no_clients_for_import(domain, dm):
    dm.fetch_clients_creation_log.coro.return_value = {
        "valid_clients": [],
        "log_history": [
            {
                "step": PipelineStep.VALIDATING_DATA,
                "status": StepStatus.FINISHED,
                "created_at": dt("2020-01-01 00:00:00"),
            },
        ],
    }

    await domain.import_clients(session_id="f8d049c53e04", biz_id=123)

    dm.submit_import_result.assert_called_with(
        session_id="f8d049c53e04",
        biz_id=123,
        import_result={"created_amount": 0, "updated_amount": 0},
    )


async def test_marks_creation_log_in_progress_before_import_starts(domain, dm):
    dm.fetch_clients_creation_log.coro.return_value = {
        "valid_clients": [{"email": "some@yandex.ru"}],
        "log_history": [
            {
                "step": PipelineStep.VALIDATING_DATA,
                "status": StepStatus.FINISHED,
                "created_at": dt("2020-01-01 00:00:00"),
            },
        ],
    }

    await domain.import_clients(session_id="f8d049c53e04", biz_id=123)

    dm.update_log_status.assert_any_call(
        session_id="f8d049c53e04",
        biz_id=123,
        pipeline_step=PipelineStep.IMPORTING_CLIENTS,
        status=StepStatus.IN_PROGRESS,
    )


async def test_locks_creation_log(mocker, domain, dm, lock_manager):
    mocker.patch.object(
        lock_manager,
        "try_lock_creation_entry",
        Mock(return_value=AsyncContextManagerMock()),
    )

    dm.fetch_clients_creation_log.coro.return_value = {
        "valid_clients": [{"email": "some@yandex.ru"}],
        "log_history": [
            {
                "step": PipelineStep.VALIDATING_DATA,
                "status": StepStatus.FINISHED,
                "created_at": dt("2020-01-01 00:00:00"),
            },
        ],
    }
    await domain.import_clients(session_id="f8d049c53e04", biz_id=123)
    await asyncio.sleep(0.1)

    lock_manager.try_lock_creation_entry.assert_called_with("f8d049c53e04")


async def test_does_not_call_doorman_if_no_clients_for_import(domain, dm, doorman):
    dm.fetch_clients_creation_log.coro.return_value = {
        "valid_clients": [],
        "log_history": [
            {
                "step": PipelineStep.VALIDATING_DATA,
                "status": StepStatus.FINISHED,
                "created_at": dt("2020-01-01 00:00:00"),
            },
        ],
    }

    await domain.import_clients(session_id="f8d049c53e04", biz_id=123)

    await asyncio.sleep(0.1)

    doorman.create_clients.assert_not_called()


async def test_calls_doorman_for_import(domain, dm, doorman):
    dm.fetch_clients_creation_log.coro.return_value = {
        "valid_clients": [{"email": "some@yandex.ru"}],
        "markup": {"segment": "orange"},
        "log_history": [
            {
                "step": PipelineStep.VALIDATING_DATA,
                "status": StepStatus.FINISHED,
                "created_at": dt("2020-01-01 00:00:00"),
            },
        ],
    }

    await domain.import_clients(session_id="f8d049c53e04", biz_id=123)

    await asyncio.sleep(0.1)

    doorman.create_clients.assert_called_with(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="orange",
        clients=[{"email": "some@yandex.ru"}],
    )


async def test_logs_doorman_exceptions(domain, dm, doorman, caplog):
    dm.fetch_clients_creation_log.coro.return_value = {
        "valid_clients": [{"email": "some@yandex.ru"}],
        "log_history": [
            {
                "step": PipelineStep.VALIDATING_DATA,
                "status": StepStatus.FINISHED,
                "created_at": dt("2020-01-01 00:00:00"),
            },
        ],
    }
    doorman.create_clients.coro.side_effect = Exception("Boom!")

    await domain.import_clients(session_id="f8d049c53e04", biz_id=123)

    await asyncio.sleep(0.1)

    errors = [r for r in caplog.records if r.levelname == "ERROR"]
    assert {error.message for error in errors} == {
        "Failed to import clients for session_id f8d049c53e04"
    }


async def test_marks_creation_log_failed_if_doorman_exception(domain, dm, doorman):
    dm.fetch_clients_creation_log.coro.return_value = {
        "valid_clients": [{"email": "some@yandex.ru"}],
        "log_history": [
            {
                "step": PipelineStep.VALIDATING_DATA,
                "status": StepStatus.FINISHED,
                "created_at": dt("2020-01-01 00:00:00"),
            },
        ],
    }
    doorman.create_clients.coro.side_effect = Exception("Boom!")

    await domain.import_clients(session_id="f8d049c53e04", biz_id=123)

    await asyncio.sleep(0.1)

    dm.update_log_status.assert_called_with(
        session_id="f8d049c53e04",
        biz_id=123,
        pipeline_step=PipelineStep.IMPORTING_CLIENTS,
        status=StepStatus.FAILED,
        failed_reason="Failed to import clients",
    )


async def test_saves_import_result_from_doorman(domain, dm, doorman):
    dm.fetch_clients_creation_log.coro.return_value = {
        "valid_clients": [{"email": "some@yandex.ru"}],
        "log_history": [
            {
                "step": PipelineStep.VALIDATING_DATA,
                "status": StepStatus.FINISHED,
                "created_at": dt("2020-01-01 00:00:00"),
            },
        ],
    }
    doorman.create_clients.coro.return_value = (10, 20)

    await domain.import_clients(session_id="f8d049c53e04", biz_id=123)

    await asyncio.sleep(0.1)

    dm.submit_import_result.assert_called_with(
        session_id="f8d049c53e04",
        biz_id=123,
        import_result={"new_clients_amount": 10, "updated_clients_amount": 20},
    )
