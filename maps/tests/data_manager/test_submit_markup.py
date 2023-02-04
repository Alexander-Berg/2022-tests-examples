from datetime import datetime

import pytest

from maps_adv.common.helpers import Any
from maps_adv.geosmb.harmonist.server.lib.enums import ColumnType

pytestmark = [pytest.mark.asyncio]


async def test_returns_nothing(dm, factory):
    session_id = await factory.create_log()

    got = await dm.submit_markup(
        session_id=session_id,
        biz_id=123,
        markup={
            "ignore_first_line": True,
            "column_type_map": [
                {"column_type": ColumnType.FIRST_NAME, "column_number": 5},
                {"column_type": ColumnType.LAST_NAME, "column_number": 7},
            ],
        },
    )

    assert got is None


async def test_updates_data_in_db(dm, factory):
    session_id = await factory.create_log()

    await dm.submit_markup(
        session_id=session_id,
        biz_id=123,
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
        },
    )

    creation_log = await factory.find_creation_log(session_id)
    assert creation_log["markup"] == {
        "ignore_first_line": True,
        "column_type_map": [
            {"column_type": "FIRST_NAME", "column_number": 5},
            {"column_type": "LAST_NAME", "column_number": 7},
            {"column_type": "PHONE", "column_number": 8},
            {"column_type": "COMMENT", "column_number": 9},
            {"column_type": "EMAIL", "column_number": 10},
            {"column_type": "DO_NOT_IMPORT", "column_number": 11},
        ],
    }
    assert creation_log["log_history"] == [
        {"step": "PARSING_DATA", "status": "FINISHED", "created_at": Any(datetime)},
        {
            "step": "VALIDATING_DATA",
            "status": "IN_PROGRESS",
            "created_at": Any(datetime),
        },
    ]
