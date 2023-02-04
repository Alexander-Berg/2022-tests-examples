import asyncio
from datetime import datetime

import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.harmonist.proto.pipeline_pb2 import Error, ImportClientsInput
from maps_adv.geosmb.harmonist.server.lib.enums import PipelineStep, StepStatus

pytestmark = [pytest.mark.asyncio]

URL = "/v1/import_clients/"


@pytest.mark.parametrize(
    "invalid_field, expected_description",
    [
        (dict(biz_id=0), "biz_id: ['Must be at least 1.']"),
        (dict(session_id=""), "session_id: ['Shorter than minimum length 1.']"),
    ],
)
async def test_errored_on_incorrect_input(invalid_field, expected_description, api):
    input_params = dict(biz_id=123, session_id="60228287f083be1269dade4c")
    input_params.update(**invalid_field)

    got = await api.post(
        URL,
        proto=ImportClientsInput(**input_params),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(code=Error.VALIDATION_ERROR, description=expected_description)


async def test_returns_nothing(api, factory):
    session_id = await factory.create_log(valid_clients=[{"email": "some@yandex.ru"}])

    got = await api.post(
        URL,
        proto=ImportClientsInput(session_id=session_id, biz_id=123),
        expected_status=200,
    )

    assert got == b""


async def test_errored_for_unknown_session_id(api):
    got = await api.post(
        URL,
        proto=ImportClientsInput(session_id="602510b81ef43c79625128ba", biz_id=123),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.INVALID_SESSION_ID,
        description="Session not found: "
        "session_id=602510b81ef43c79625128ba, biz_id=123",
    )


async def test_errored_if_session_id_does_not_match_biz_id(api, factory):
    other_biz_session_id = await factory.create_log(biz_id=123)
    await factory.create_log(biz_id=321)

    got = await api.post(
        URL,
        proto=ImportClientsInput(session_id=other_biz_session_id, biz_id=321),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.INVALID_SESSION_ID,
        description=f"Session not found: session_id={other_biz_session_id}, biz_id=321",
    )


async def test_errored_if_import_already_has_been_done(api, factory):
    session_id = await factory.create_log(
        valid_clients=[{"email": "some@yandex.ru"}],
        import_result={"created_amount": 10, "updated_amount": 20},
    )

    got = await api.post(
        URL,
        proto=ImportClientsInput(session_id=session_id, biz_id=123),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.CLIENTS_ALREADY_WERE_IMPORTED,
        description=f"Clients for session_id {session_id} already imported.",
    )


async def test_errored_if_no_finished_validation_step(api, factory):
    session_id = await factory.create_log()
    await factory.add_history_record(
        session_id=session_id,
        step=PipelineStep.VALIDATING_DATA,
        status=StepStatus.IN_PROGRESS,
    )

    got = await api.post(
        URL,
        proto=ImportClientsInput(session_id=session_id, biz_id=123),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.VALIDATION_NOT_FINISHED,
        description=f"Validation for session_id {session_id} hasn't finished yet.",
    )


async def test_saves_empty_result_if_no_clients_for_import(api, factory):
    session_id = await factory.create_log(
        valid_clients=[],
    )

    await api.post(
        URL,
        proto=ImportClientsInput(session_id=session_id, biz_id=123),
        expected_status=200,
    )

    creation_log = await factory.find_creation_log(session_id)
    assert creation_log["import_result"] == {"created_amount": 0, "updated_amount": 0}


async def test_does_not_save_import_result_if_import_fails(api, factory, doorman):
    session_id = await factory.create_log(
        valid_clients=[{"email": "some@yandex.ru"}],
    )
    doorman.create_clients.coro.side_effect = Exception("Boom!")

    await api.post(
        URL,
        proto=ImportClientsInput(session_id=session_id, biz_id=123),
        expected_status=200,
    )

    await asyncio.sleep(0.1)

    creation_log = await factory.find_creation_log(session_id)
    assert "import_result" not in creation_log


async def test_saves_import_result_for_successful_import(api, factory, doorman):
    session_id = await factory.create_log(
        valid_clients=[{"email": "some@yandex.ru"}],
    )
    doorman.create_clients.coro.return_value = (10, 20)

    await api.post(
        URL,
        proto=ImportClientsInput(session_id=session_id, biz_id=123),
        expected_status=200,
    )

    await asyncio.sleep(0.1)

    creation_log = await factory.find_creation_log(session_id)
    assert creation_log["import_result"] == {
        "new_clients_amount": 10,
        "updated_clients_amount": 20,
    }


async def test_marks_creation_log_failed_if_import_fails(api, factory, doorman):
    session_id = await factory.create_log(
        valid_clients=[{"email": "some@yandex.ru"}],
    )
    doorman.create_clients.coro.side_effect = Exception("Boom!")

    await api.post(
        URL,
        proto=ImportClientsInput(session_id=session_id, biz_id=123),
        expected_status=200,
    )

    await asyncio.sleep(0.1)

    creation_log = await factory.find_creation_log(session_id)
    assert creation_log["log_history"][-1] == {
        "step": "IMPORTING_CLIENTS",
        "status": "FAILED",
        "created_at": Any(datetime),
        "failed_reason": "Failed to import clients",
    }


async def test_marks_creation_log_finished_after_successful_import(
    api, factory, doorman
):
    session_id = await factory.create_log(
        valid_clients=[{"email": "some@yandex.ru"}],
    )
    doorman.create_clients.coro.return_value = (10, 20)

    await api.post(
        URL,
        proto=ImportClientsInput(session_id=session_id, biz_id=123),
        expected_status=200,
    )

    await asyncio.sleep(0.1)

    creation_log = await factory.find_creation_log(session_id)
    assert creation_log["log_history"][-1] == {
        "step": "IMPORTING_CLIENTS",
        "status": "FINISHED",
        "created_at": Any(datetime),
    }
