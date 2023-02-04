from datetime import datetime

import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.harmonist.server.lib.enums import (
    ColumnType,
    PipelineStep,
    StepStatus,
)

pytestmark = [pytest.mark.asyncio]


async def test_returns_creation_log_if_found(dm, factory):
    session_id = await factory.create_log()

    got = await dm.fetch_clients_creation_log(session_id=session_id, biz_id=123)

    assert got == {
        "_id": session_id,
        "biz_id": 123,
        "created_at": Any(datetime),
        "input_data_type": "TEXT",
        "log_history": [
            {
                "created_at": Any(datetime),
                "status": StepStatus.FINISHED,
                "step": PipelineStep.PARSING_DATA,
            }
        ],
        "parsed_input": [
            ["Lorem", "ipsum dolor", "sit amet"],
            ["consectetur", "adipiscing", " elit.", "Nullam"],
            ["accumsan", "orci sit", "amet commodoconsectetur"],
        ],
    }


@pytest.mark.parametrize("step", PipelineStep)
@pytest.mark.parametrize("step_status", StepStatus)
async def test_converts_log_records_correctly(step_status, step, dm, factory):
    session_id = await factory.create_log()
    await factory.add_history_record(
        session_id=session_id, step=step, status=step_status
    )

    got = await dm.fetch_clients_creation_log(session_id=session_id, biz_id=123)

    assert got["log_history"] == [
        {
            "created_at": Any(datetime),
            "status": StepStatus.FINISHED,
            "step": PipelineStep.PARSING_DATA,
        },
        {
            "created_at": Any(datetime),
            "status": step_status,
            "step": step,
        },
    ]


async def test_markup_correctly(dm, factory):
    session_id = await factory.create_log(
        markup={
            "ignore_first_line": True,
            "column_type_map": [
                {"column_type": ColumnType.FIRST_NAME, "column_number": 5},
                {"column_type": ColumnType.LAST_NAME, "column_number": 7},
                {"column_type": ColumnType.PHONE, "column_number": 8},
                {"column_type": ColumnType.COMMENT, "column_number": 9},
                {"column_type": ColumnType.EMAIL, "column_number": 10},
                {"column_type": ColumnType.DO_NOT_IMPORT, "column_number": 11},
            ],
        }
    )

    got = await dm.fetch_clients_creation_log(session_id=session_id, biz_id=123)

    assert got["markup"] == {
        "ignore_first_line": True,
        "column_type_map": [
            {"column_type": ColumnType.FIRST_NAME, "column_number": 5},
            {"column_type": ColumnType.LAST_NAME, "column_number": 7},
            {"column_type": ColumnType.PHONE, "column_number": 8},
            {"column_type": ColumnType.COMMENT, "column_number": 9},
            {"column_type": ColumnType.EMAIL, "column_number": 10},
            {"column_type": ColumnType.DO_NOT_IMPORT, "column_number": 11},
        ],
    }


@pytest.mark.parametrize(
    "session_id", ["8b3b4257-c615-4184-b6a2-0203e59f76a4", "abc", ""]
)
async def test_returns_none_if_nothing_exists(dm, factory, session_id):
    got = await dm.fetch_clients_creation_log(session_id=session_id, biz_id=123)

    assert got is None


async def test_returns_none_if_session_id_do_not_match_biz_id(dm, factory):
    other_biz_session_id = await factory.create_log(biz_id=123)
    await factory.create_log(biz_id=321)

    got = await dm.fetch_clients_creation_log(
        session_id=other_biz_session_id, biz_id=321
    )

    assert got is None
