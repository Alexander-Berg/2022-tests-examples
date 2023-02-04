from datetime import datetime

import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.harmonist.proto.pipeline_pb2 import (
    ColumnTypeMap,
    Error,
    MarkUp,
    SubmitMarkUp,
)

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.usefixtures("wait_for_background_tasks_to_finish"),
]

URL = "/v1/submit_markup/"


def make_input_pb(*, session_id: str, biz_id: int = 123, **markup_overrides):
    markup_kw = dict(
        ignore_first_line=True,
        segment=None,
        column_type_map=[
            ColumnTypeMap(
                column_type=ColumnTypeMap.ColumnType.FIRST_NAME, column_number=2
            ),
            ColumnTypeMap(column_type=ColumnTypeMap.ColumnType.PHONE, column_number=3),
        ],
    )
    markup_kw.update(markup_overrides)

    return SubmitMarkUp(
        session_id=session_id, biz_id=biz_id, markup=MarkUp(**markup_kw)
    )


async def test_returns_nothing(api, factory):
    session_id = await factory.create_log()

    got = await api.post(
        URL,
        proto=make_input_pb(session_id=session_id),
        expected_status=200,
    )

    assert got == b""


@pytest.mark.parametrize("ignore_first_line", [True, False])
@pytest.mark.parametrize("segment", [dict(), dict(segment="Import from 03.02.2021")])
async def test_updates_data_in_db(ignore_first_line, segment, api, factory):
    session_id = await factory.create_log()

    await api.post(
        URL,
        proto=make_input_pb(
            session_id=session_id,
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
        expected_status=200,
    )

    creation_log = await factory.find_creation_log(session_id)
    assert creation_log["markup"] == {
        "ignore_first_line": ignore_first_line,
        "column_type_map": [
            {"column_number": 1, "column_type": "FIRST_NAME"},
            {"column_number": 2, "column_type": "LAST_NAME"},
            {"column_number": 4, "column_type": "PHONE"},
            {"column_number": 5, "column_type": "EMAIL"},
            {"column_number": 7, "column_type": "DO_NOT_IMPORT"},
            {"column_number": 8, "column_type": "COMMENT"},
        ],
        **segment,
    }
    assert {
        "step": "VALIDATING_DATA",
        "status": "IN_PROGRESS",
        "created_at": Any(datetime),
    } in creation_log["log_history"]


@pytest.mark.parametrize(
    "pb_column_type, db_column_type",
    [
        (ColumnTypeMap.ColumnType.PHONE, "PHONE"),
        (ColumnTypeMap.ColumnType.EMAIL, "EMAIL"),
    ],
)
async def test_must_have_at_least_one_identity_field_to_import(
    pb_column_type, db_column_type, api, factory
):
    session_id = await factory.create_log()

    await api.post(
        URL,
        proto=make_input_pb(
            session_id=session_id,
            column_type_map=[
                ColumnTypeMap(column_type=pb_column_type, column_number=2),
                ColumnTypeMap(
                    column_type=ColumnTypeMap.ColumnType.DO_NOT_IMPORT, column_number=5
                ),
                ColumnTypeMap(
                    column_type=ColumnTypeMap.ColumnType.DO_NOT_IMPORT, column_number=7
                ),
            ],
        ),
        expected_status=200,
    )

    creation_log = await factory.find_creation_log(session_id)
    assert creation_log["markup"]["column_type_map"] == [
        {"column_number": 2, "column_type": db_column_type},
        {"column_number": 5, "column_type": "DO_NOT_IMPORT"},
        {"column_number": 7, "column_type": "DO_NOT_IMPORT"},
    ]


async def test_errored_if_markup_already_submitted(api, factory):
    session_id = await factory.create_log(
        markup={
            "ignore_first_line": True,
            "column_type_map": [{"column_number": 4, "column_type": "PHONE"}],
        }
    )

    got = await api.post(
        URL,
        proto=make_input_pb(session_id=session_id),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.MARKUP_ALREADY_SUBMITTED,
        description=f"Markup for session_id {session_id} already submitted.",
    )


async def test_errored_if_session_id_does_not_match_biz_id(api, factory):
    other_biz_session_id = await factory.create_log(biz_id=123)
    await factory.create_log(biz_id=321)

    got = await api.post(
        URL,
        proto=make_input_pb(session_id=other_biz_session_id, biz_id=321),
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
        (dict(segment=""), "markup: {'segment': ['Shorter than minimum length 1.']}"),
        (
            dict(column_type_map=[]),
            "markup: {'column_type_map': ['Shorter than minimum length 1.']}",
        ),
    ],
)
async def test_errored_on_incorrect_input(invalid_field, expected_description, api):
    input_kw = dict(session_id="8b3b4257-c615-4184-b6a2-0203e59f76a4")
    input_kw.update(invalid_field)

    got = await api.post(
        URL,
        proto=make_input_pb(**input_kw),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(code=Error.VALIDATION_ERROR, description=expected_description)


@pytest.mark.parametrize(
    "invalid_column_map, expected_description",
    [
        (
            [
                ColumnTypeMap(
                    column_type=ColumnTypeMap.ColumnType.EMAIL, column_number=2
                ),
                ColumnTypeMap(
                    column_type=ColumnTypeMap.ColumnType.EMAIL, column_number=3
                ),
            ],
            "markup: {'_schema': ['Column types must be unique.']}",
        ),
        (
            [
                ColumnTypeMap(
                    column_type=ColumnTypeMap.ColumnType.EMAIL, column_number=2
                ),
                ColumnTypeMap(
                    column_type=ColumnTypeMap.ColumnType.LAST_NAME, column_number=2
                ),
            ],
            "markup: {'_schema': ['Column numbers must be unique.']}",
        ),
        (
            [
                ColumnTypeMap(
                    column_type=ColumnTypeMap.ColumnType.DO_NOT_IMPORT, column_number=5
                ),
                ColumnTypeMap(
                    column_type=ColumnTypeMap.ColumnType.DO_NOT_IMPORT, column_number=7
                ),
            ],
            "markup: {'_schema': ['Specify at least one column type.']}",
        ),
        (
            [
                ColumnTypeMap(
                    column_type=ColumnTypeMap.ColumnType.FIRST_NAME, column_number=5
                ),
                ColumnTypeMap(
                    column_type=ColumnTypeMap.ColumnType.LAST_NAME, column_number=6
                ),
                ColumnTypeMap(
                    column_type=ColumnTypeMap.ColumnType.COMMENT, column_number=7
                ),
                ColumnTypeMap(
                    column_type=ColumnTypeMap.ColumnType.DO_NOT_IMPORT, column_number=8
                ),
            ],
            "markup: {'_schema': ['Email or phone must be set.']}",
        ),
    ],
)
async def test_errored_on_invalid_column_mapping(
    invalid_column_map, expected_description, api, factory
):
    session_id = await factory.create_log()

    got = await api.post(
        URL,
        proto=make_input_pb(session_id=session_id, column_type_map=invalid_column_map),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(code=Error.VALIDATION_ERROR, description=expected_description)
