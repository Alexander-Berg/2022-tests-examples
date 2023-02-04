import pytest

from maps_adv.geosmb.harmonist.proto.pipeline_pb2 import (
    ColumnTypeMap,
    Error,
    MarkUp,
    PreviewState,
    Row,
    ShowPreviewInput,
)
from maps_adv.geosmb.harmonist.server.lib.enums import ColumnType

pytestmark = [pytest.mark.asyncio]

URL = "/v1/show_preview/"


@pytest.mark.parametrize("ignore_first_line", [True, False])
@pytest.mark.parametrize("segment", [dict(), dict(segment="Import from 03.02.2021")])
async def test_returns_preview_as_expected(ignore_first_line, segment, api, factory):
    session_id = await factory.create_log(
        biz_id=123,
        parsed_input=[["Line", "One"], ["Second", "line"]],
        markup={
            "ignore_first_line": ignore_first_line,
            "column_type_map": [
                {"column_type": ColumnType.FIRST_NAME, "column_number": 5},
                {"column_type": ColumnType.LAST_NAME, "column_number": 7},
                {"column_type": ColumnType.PHONE, "column_number": 8},
                {"column_type": ColumnType.COMMENT, "column_number": 9},
                {"column_type": ColumnType.EMAIL, "column_number": 10},
                {"column_type": ColumnType.DO_NOT_IMPORT, "column_number": 11},
            ],
            **segment,
        },
    )

    got = await api.post(
        URL,
        proto=ShowPreviewInput(biz_id=123, session_id=session_id),
        decode_as=PreviewState,
        expected_status=200,
    )

    assert got == PreviewState(
        rows=[Row(cells=["Line", "One"]), Row(cells=["Second", "line"])],
        markup=MarkUp(
            ignore_first_line=ignore_first_line,
            column_type_map=[
                ColumnTypeMap(
                    column_type=ColumnTypeMap.ColumnType.FIRST_NAME, column_number=5
                ),
                ColumnTypeMap(
                    column_type=ColumnTypeMap.ColumnType.LAST_NAME, column_number=7
                ),
                ColumnTypeMap(
                    column_type=ColumnTypeMap.ColumnType.PHONE, column_number=8
                ),
                ColumnTypeMap(
                    column_type=ColumnTypeMap.ColumnType.COMMENT, column_number=9
                ),
                ColumnTypeMap(
                    column_type=ColumnTypeMap.ColumnType.EMAIL, column_number=10
                ),
                ColumnTypeMap(
                    column_type=ColumnTypeMap.ColumnType.DO_NOT_IMPORT, column_number=11
                ),
            ],
            **segment,
        ),
    )


async def test_does_not_return_markup_if_do_not_have_one(api, factory):
    session_id = await factory.create_log(
        biz_id=123, parsed_input=[["Line", "One"], ["Second", "line"]]
    )

    got = await api.post(
        URL,
        proto=ShowPreviewInput(biz_id=123, session_id=session_id),
        decode_as=PreviewState,
        expected_status=200,
    )

    assert got == PreviewState(
        rows=[Row(cells=["Line", "One"]), Row(cells=["Second", "line"])]
    )


@pytest.mark.parametrize("lines_number", [10, 20])
async def test_returns_no_more_than_10_lines_in_preview(lines_number, api, factory):
    session_id = await factory.create_log(
        biz_id=123,
        parsed_input=[
            [f"Some{idx}", f"Line{idx}", f"Content{idx}"] for idx in range(lines_number)
        ],
    )

    got = await api.post(
        URL,
        proto=ShowPreviewInput(biz_id=123, session_id=session_id),
        decode_as=PreviewState,
        expected_status=200,
    )

    assert got == PreviewState(
        rows=[
            Row(cells=[f"Some{idx}", f"Line{idx}", f"Content{idx}"])
            for idx in range(10)
        ]
    )


async def test_errored_if_session_id_not_found(api, factory):
    got = await api.post(
        URL,
        proto=ShowPreviewInput(
            biz_id=123, session_id="8b3b4257-c615-4184-b6a2-0203e59f76a4"
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.INVALID_SESSION_ID,
        description="Session not found: session_id=8b3b4257-c615-4184-b6a2-0203e59f76a4, biz_id=123",  # noqa E501
    )


async def test_errored_if_session_id_do_not_match_biz_id(api, factory):
    other_biz_session_id = await factory.create_log(biz_id=123)
    await factory.create_log(biz_id=321)

    got = await api.post(
        URL,
        proto=ShowPreviewInput(biz_id=321, session_id=other_biz_session_id),
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
        proto=ShowPreviewInput(**input_params),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(code=Error.VALIDATION_ERROR, description=expected_description)
