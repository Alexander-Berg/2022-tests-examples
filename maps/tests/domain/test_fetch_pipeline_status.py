import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.harmonist.server.lib.enums import PipelineStep, StepStatus
from maps_adv.geosmb.harmonist.server.lib.exceptions import InvalidSessionId

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_raises_if_creation_log_not_found(domain, dm):
    dm.fetch_clients_creation_log.coro.return_value = None

    with pytest.raises(
        InvalidSessionId, match="Session not found: session_id=f8d049c53e04, biz_id=123"
    ):
        await domain.fetch_pipeline_status(session_id="f8d049c53e04", biz_id=123)


async def test_returns_preview_if_parsing_data_finished(domain, dm):
    dm.fetch_clients_creation_log.coro.return_value = {
        "log_history": [
            {
                "step": PipelineStep.PARSING_DATA,
                "status": StepStatus.FINISHED,
                "created_at": dt("2020-02-20 18:00:00"),
            }
        ],
        "parsed_input": [["lorem", "ipsum"]],
    }

    got = await domain.fetch_pipeline_status(session_id="f8d049c53e04", biz_id=123)

    assert got == dict(
        step=PipelineStep.PARSING_DATA,
        step_status=StepStatus.FINISHED,
        preview=dict(rows=[["lorem", "ipsum"]]),
    )


async def test_returns_no_more_than_10_preview_rows(domain, dm):
    dm.fetch_clients_creation_log.coro.return_value = {
        "log_history": [
            {
                "step": PipelineStep.PARSING_DATA,
                "status": StepStatus.FINISHED,
                "created_at": dt("2020-02-20 18:00:00"),
            }
        ],
        "parsed_input": [[f"lorem_{idx}", f"ipsum_{idx}"] for idx in range(15)],
    }

    got = await domain.fetch_pipeline_status(session_id="f8d049c53e04", biz_id=123)

    assert got["preview"]["rows"] == [
        [f"lorem_{idx}", f"ipsum_{idx}"] for idx in range(10)
    ]


async def test_returns_preview_with_markup_while_validating_data(domain, dm):
    dm.fetch_clients_creation_log.coro.return_value = {
        "log_history": [
            {
                "step": PipelineStep.PARSING_DATA,
                "status": StepStatus.FINISHED,
                "created_at": dt("2020-02-20 18:00:00"),
            },
            {
                "step": PipelineStep.VALIDATING_DATA,
                "status": StepStatus.IN_PROGRESS,
                "created_at": dt("2020-02-20 18:01:00"),
            },
        ],
        "parsed_input": [["lorem", "ipsum"]],
        "markup": {"some_markup": "some_params"},
    }

    got = await domain.fetch_pipeline_status(session_id="f8d049c53e04", biz_id=123)

    assert got == dict(
        step=PipelineStep.VALIDATING_DATA,
        step_status=StepStatus.IN_PROGRESS,
        preview=dict(rows=[["lorem", "ipsum"]], markup={"some_markup": "some_params"}),
    )


async def test_returns_validation_result_if_validation_is_ready(domain, dm):
    dm.fetch_clients_creation_log.coro.return_value = {
        "log_history": [
            {
                "step": PipelineStep.PARSING_DATA,
                "status": StepStatus.FINISHED,
                "created_at": dt("2020-02-20 18:00:00"),
            },
            {
                "step": PipelineStep.VALIDATING_DATA,
                "status": StepStatus.FINISHED,
                "created_at": dt("2020-02-20 18:01:00"),
            },
        ],
        "valid_clients": [dict(email="chebu_@rek.ru") for _ in range(3)],
        "invalid_clients": [dict(email="meow_@rek.ru") for _ in range(5)],
        "validation_errors_file_link": "http://click.me",
    }

    got = await domain.fetch_pipeline_status(session_id="f8d049c53e04", biz_id=123)

    assert got == dict(
        step=PipelineStep.VALIDATING_DATA,
        step_status=StepStatus.FINISHED,
        validation_result=dict(
            valid_clients_amount=3,
            invalid_clients=dict(
                invalid_clients_amount=5, report_link="http://click.me"
            ),
        ),
    )


async def test_does_not_return_invalid_clients_info_if_no_invalid_clients(domain, dm):
    dm.fetch_clients_creation_log.coro.return_value = {
        "log_history": [
            {
                "step": PipelineStep.PARSING_DATA,
                "status": StepStatus.FINISHED,
                "created_at": dt("2020-02-20 18:00:00"),
            },
            {
                "step": PipelineStep.VALIDATING_DATA,
                "status": StepStatus.FINISHED,
                "created_at": dt("2020-02-20 18:01:00"),
            },
        ],
        "valid_clients": [dict(email="chebu_@rek.ru") for _ in range(3)],
        "invalid_clients": [],
    }

    got = await domain.fetch_pipeline_status(session_id="f8d049c53e04", biz_id=123)

    assert got == dict(
        step=PipelineStep.VALIDATING_DATA,
        step_status=StepStatus.FINISHED,
        validation_result=dict(valid_clients_amount=3),
    )


async def test_returns_validation_result_if_importing_clients_in_progress(domain, dm):
    dm.fetch_clients_creation_log.coro.return_value = {
        "log_history": [
            {
                "step": PipelineStep.PARSING_DATA,
                "status": StepStatus.FINISHED,
                "created_at": dt("2020-02-20 18:00:00"),
            },
            {
                "step": PipelineStep.VALIDATING_DATA,
                "status": StepStatus.FINISHED,
                "created_at": dt("2020-02-20 18:01:00"),
            },
            {
                "step": PipelineStep.IMPORTING_CLIENTS,
                "status": StepStatus.IN_PROGRESS,
                "created_at": dt("2020-02-20 18:02:00"),
            },
        ],
        "valid_clients": [dict(email="chebu_@rek.ru") for _ in range(3)],
        "invalid_clients": [dict(email="meow_@rek.ru") for _ in range(5)],
        "validation_errors_file_link": "http://click.me",
    }

    got = await domain.fetch_pipeline_status(session_id="f8d049c53e04", biz_id=123)

    assert got == dict(
        step=PipelineStep.IMPORTING_CLIENTS,
        step_status=StepStatus.IN_PROGRESS,
        validation_result=dict(
            valid_clients_amount=3,
            invalid_clients=dict(
                invalid_clients_amount=5, report_link="http://click.me"
            ),
        ),
    )


async def test_returns_import_result_if_import_finished(domain, dm):
    dm.fetch_clients_creation_log.coro.return_value = {
        "log_history": [
            {
                "step": PipelineStep.PARSING_DATA,
                "status": StepStatus.FINISHED,
                "created_at": dt("2020-02-20 18:00:00"),
            },
            {
                "step": PipelineStep.VALIDATING_DATA,
                "status": StepStatus.FINISHED,
                "created_at": dt("2020-02-20 18:01:00"),
            },
            {
                "step": PipelineStep.IMPORTING_CLIENTS,
                "status": StepStatus.FINISHED,
                "created_at": dt("2020-02-20 18:02:00"),
            },
        ],
        "import_result": {"new_clients_amount": 15, "updated_clients_amount": 20},
    }

    got = await domain.fetch_pipeline_status(session_id="f8d049c53e04", biz_id=123)

    assert got == dict(
        step=PipelineStep.IMPORTING_CLIENTS,
        step_status=StepStatus.FINISHED,
        import_result=dict(new_clients_amount=15, updated_clients_amount=20),
    )


@pytest.mark.parametrize(
    "pipeline_step", [PipelineStep.VALIDATING_DATA, PipelineStep.IMPORTING_CLIENTS]
)
async def test_returns_failed_reason_if_step_was_failed(pipeline_step, domain, dm):
    dm.fetch_clients_creation_log.coro.return_value = {
        "log_history": [
            {
                "step": PipelineStep.PARSING_DATA,
                "status": StepStatus.FINISHED,
                "created_at": dt("2020-02-20 18:00:00"),
            },
            {
                "step": pipeline_step,
                "status": StepStatus.FAILED,
                "created_at": dt("2020-02-20 18:01:00"),
                "failed_reason": "because of kek",
            },
        ]
    }

    got = await domain.fetch_pipeline_status(session_id="f8d049c53e04", biz_id=123)

    assert got == dict(
        step=pipeline_step,
        step_status=StepStatus.FAILED,
        failed_reason="because of kek",
    )
