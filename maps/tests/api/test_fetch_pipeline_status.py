import pytest

from maps_adv.geosmb.harmonist.proto.pipeline_pb2 import (
    ColumnTypeMap,
    Error,
    FetchPipelineStatus,
    ImportClientsResult,
    InvalidClientsReport,
    MarkUp,
    MarkUpValidationResult,
    PipelineStatus,
    PipelineStep,
    PipelineStepStatus,
    PreviewState,
    Row,
)
from maps_adv.geosmb.harmonist.server.lib.enums import (
    ColumnType,
    PipelineStep as PipelineStepEnum,
    StepStatus,
)

pytestmark = [pytest.mark.asyncio]

URL = "/v1/fetch_pipeline_status/"


async def test_returns_preview_if_parsing_data_finished(factory, api):
    session_id = await factory.create_log(parsed_input=[["lorem", "ipsum"]])

    got = await api.post(
        URL,
        proto=FetchPipelineStatus(biz_id=123, session_id=session_id),
        decode_as=PipelineStatus,
        expected_status=200,
    )

    assert got == PipelineStatus(
        step=PipelineStep.PARSING_DATA,
        step_status=PipelineStepStatus.FINISHED,
        preview=PreviewState(rows=[Row(cells=["lorem", "ipsum"])]),
    )


async def test_returns_no_more_than_10_rows_in_preview(factory, api):
    session_id = await factory.create_log(
        parsed_input=[[f"lorem{idx}", f"ipsum{idx}"] for idx in range(11)]
    )

    got = await api.post(
        URL,
        proto=FetchPipelineStatus(biz_id=123, session_id=session_id),
        decode_as=PipelineStatus,
        expected_status=200,
    )

    assert got == PipelineStatus(
        step=PipelineStep.PARSING_DATA,
        step_status=PipelineStepStatus.FINISHED,
        preview=PreviewState(
            rows=[Row(cells=[f"lorem{idx}", f"ipsum{idx}"]) for idx in range(10)]
        ),
    )


@pytest.mark.parametrize("ignore_first_line", [True, False])
@pytest.mark.parametrize("segment", [dict(), dict(segment="Import from 03.02.2021")])
async def test_returns_preview_with_markup_while_validating_data(
    ignore_first_line, segment, api, factory
):
    session_id = await factory.create_log(
        parsed_input=[["lorem", "ipsum"]],
        # updates log_history according to real pipeline
        markup=dict(
            ignore_first_line=ignore_first_line,
            column_type_map=[
                dict(column_type=ColumnType.FIRST_NAME, column_number=1),
                dict(column_type=ColumnType.LAST_NAME, column_number=2),
                dict(column_type=ColumnType.PHONE, column_number=4),
                dict(column_type=ColumnType.EMAIL, column_number=5),
                dict(column_type=ColumnType.DO_NOT_IMPORT, column_number=7),
                dict(column_type=ColumnType.COMMENT, column_number=8),
            ],
            **segment,
        ),
    )

    got = await api.post(
        URL,
        proto=FetchPipelineStatus(biz_id=123, session_id=session_id),
        decode_as=PipelineStatus,
        expected_status=200,
    )

    assert got == PipelineStatus(
        step=PipelineStep.VALIDATING_DATA,
        step_status=PipelineStepStatus.IN_PROGRESS,
        preview=PreviewState(
            rows=[Row(cells=["lorem", "ipsum"])],
            markup=MarkUp(
                ignore_first_line=ignore_first_line,
                column_type_map=[
                    ColumnTypeMap(
                        column_type=ColumnTypeMap.ColumnType.FIRST_NAME, column_number=1
                    ),
                    ColumnTypeMap(
                        column_type=ColumnTypeMap.ColumnType.LAST_NAME, column_number=2
                    ),
                    ColumnTypeMap(
                        column_type=ColumnTypeMap.ColumnType.PHONE, column_number=4
                    ),
                    ColumnTypeMap(
                        column_type=ColumnTypeMap.ColumnType.EMAIL, column_number=5
                    ),
                    ColumnTypeMap(
                        column_type=ColumnTypeMap.ColumnType.DO_NOT_IMPORT,
                        column_number=7,
                    ),
                    ColumnTypeMap(
                        column_type=ColumnTypeMap.ColumnType.COMMENT, column_number=8
                    ),
                ],
                **segment,
            ),
        ),
    )


async def test_returns_validation_result_if_validation_is_ready(api, factory):
    session_id = await factory.create_log(
        # updates log_history according to real pipeline
        valid_clients=[dict(email="chebu_@rek.ru") for _ in range(3)],
        invalid_clients=[dict(email="meow_@rek.ru") for _ in range(5)],
        validation_errors_file_link="http://click.me",
    )

    got = await api.post(
        URL,
        proto=FetchPipelineStatus(biz_id=123, session_id=session_id),
        decode_as=PipelineStatus,
        expected_status=200,
    )

    assert got == PipelineStatus(
        step=PipelineStep.VALIDATING_DATA,
        step_status=PipelineStepStatus.FINISHED,
        validation_result=MarkUpValidationResult(
            valid_clients_amount=3,
            invalid_clients=InvalidClientsReport(
                invalid_clients_amount=5, report_link="http://click.me"
            ),
        ),
    )


async def test_returns_failed_reason_if_validation_failed(api, factory):
    session_id = await factory.create_log(
        # updates log_history according to real pipeline
        markup=dict(
            ignore_first_line=True,
            column_type_map=[
                dict(column_type=ColumnType.FIRST_NAME, column_number=1),
                dict(column_type=ColumnType.PHONE, column_number=4),
            ],
        ),
    )
    await factory.add_history_record(
        session_id=session_id,
        step=PipelineStepEnum.VALIDATING_DATA,
        status=StepStatus.FAILED,
        failed_reason="because of kek.",
    )

    got = await api.post(
        URL,
        proto=FetchPipelineStatus(biz_id=123, session_id=session_id),
        decode_as=PipelineStatus,
        expected_status=200,
    )

    assert got == PipelineStatus(
        step=PipelineStep.VALIDATING_DATA,
        step_status=PipelineStepStatus.FAILED,
        failed_reason="because of kek.",
    )


async def test_does_not_return_invalid_clients_info_if_no_invalid_clients(api, factory):
    session_id = await factory.create_log(
        # updates log_history according to real pipeline
        valid_clients=[dict(email="chebu_@rek.ru") for _ in range(3)],
        invalid_clients=[],
    )

    got = await api.post(
        URL,
        proto=FetchPipelineStatus(biz_id=123, session_id=session_id),
        decode_as=PipelineStatus,
        expected_status=200,
    )

    assert got == PipelineStatus(
        step=PipelineStep.VALIDATING_DATA,
        step_status=PipelineStepStatus.FINISHED,
        validation_result=MarkUpValidationResult(valid_clients_amount=3),
    )


async def test_returns_validation_result_if_importing_clients_in_progress(api, factory):
    session_id = await factory.create_log(
        # updates log_history according to real pipeline
        valid_clients=[dict(email="chebu_@rek.ru") for _ in range(3)],
        invalid_clients=[dict(email="meow_@rek.ru") for _ in range(5)],
        validation_errors_file_link="http://click.me",
    )
    await factory.add_history_record(
        session_id=session_id,
        step=PipelineStepEnum.IMPORTING_CLIENTS,
        status=StepStatus.IN_PROGRESS,
    )

    got = await api.post(
        URL,
        proto=FetchPipelineStatus(biz_id=123, session_id=session_id),
        decode_as=PipelineStatus,
        expected_status=200,
    )

    assert got == PipelineStatus(
        step=PipelineStep.IMPORTING_CLIENTS,
        step_status=PipelineStepStatus.IN_PROGRESS,
        validation_result=MarkUpValidationResult(
            valid_clients_amount=3,
            invalid_clients=InvalidClientsReport(
                invalid_clients_amount=5, report_link="http://click.me"
            ),
        ),
    )


async def test_returns_error_if_importing_clients_failed(api, factory):
    session_id = await factory.create_log(
        # updates log_history according to real pipeline
        valid_clients=[dict(email="chebu_@rek.ru") for _ in range(3)],
        invalid_clients=[],
    )
    await factory.add_history_record(
        session_id=session_id,
        step=PipelineStepEnum.IMPORTING_CLIENTS,
        status=StepStatus.FAILED,
        failed_reason="because of kek.",
    )

    got = await api.post(
        URL,
        proto=FetchPipelineStatus(biz_id=123, session_id=session_id),
        decode_as=PipelineStatus,
        expected_status=200,
    )

    assert got == PipelineStatus(
        step=PipelineStep.IMPORTING_CLIENTS,
        step_status=PipelineStepStatus.FAILED,
        failed_reason="because of kek.",
    )


async def test_returns_import_result_if_import_finished(api, factory):
    session_id = await factory.create_log(
        # updates log_history according to real pipeline
        import_result={"new_clients_amount": 10, "updated_clients_amount": 15},
    )

    got = await api.post(
        URL,
        proto=FetchPipelineStatus(biz_id=123, session_id=session_id),
        decode_as=PipelineStatus,
        expected_status=200,
    )

    assert got == PipelineStatus(
        step=PipelineStep.IMPORTING_CLIENTS,
        step_status=PipelineStepStatus.FINISHED,
        import_result=ImportClientsResult(
            new_clients_amount=10, updated_clients_amount=15
        ),
    )


async def test_errored_if_session_id_not_found(api, factory):
    got = await api.post(
        URL,
        proto=FetchPipelineStatus(biz_id=123, session_id="8b3b4257"),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.INVALID_SESSION_ID,
        description="Session not found: session_id=8b3b4257, biz_id=123",
    )


async def test_errored_if_session_id_do_not_match_biz_id(api, factory):
    other_biz_session_id = await factory.create_log(biz_id=123)
    await factory.create_log(biz_id=321)

    got = await api.post(
        URL,
        proto=FetchPipelineStatus(biz_id=321, session_id=other_biz_session_id),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.INVALID_SESSION_ID,
        description=f"Session not found: session_id={other_biz_session_id}, biz_id=321",
    )


@pytest.mark.parametrize(
    "invalid_field, expected_description",
    [
        (dict(biz_id=0), "biz_id: ['Must be at least 1.']"),
        (dict(session_id=""), "session_id: ['Shorter than minimum length 1.']"),
    ],
)
async def test_errored_on_incorrect_input(invalid_field, expected_description, api):
    input_params = dict(biz_id=123, session_id="8b3b4257-c615-4184-b6a2-0203e59f76a4")
    input_params.update(**invalid_field)

    got = await api.post(
        URL,
        proto=FetchPipelineStatus(**input_params),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(code=Error.VALIDATION_ERROR, description=expected_description)
