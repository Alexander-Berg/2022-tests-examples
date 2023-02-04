from datetime import datetime

import pytest
from smb.common.testing_utils import Any

pytestmark = [pytest.mark.asyncio]


async def test_returns_nothing(dm, factory):
    session_id = await factory.create_log()

    got = await dm.submit_import_result(
        session_id=session_id,
        biz_id=123,
        import_result={
            "created_amount": 10,
            "updated_amount": 20,
        },
    )

    assert got is None


async def test_updates_data_in_db(dm, factory):
    session_id = await factory.create_log()

    await dm.submit_import_result(
        session_id=session_id,
        biz_id=123,
        import_result={
            "created_amount": 10,
            "updated_amount": 20,
        },
    )

    creation_log = await factory.find_creation_log(session_id)
    assert creation_log == {
        "_id": session_id,
        "biz_id": 123,
        "created_at": Any(datetime),
        "input_data": "Literally anything",
        "input_data_type": "TEXT",
        "import_result": {"created_amount": 10, "updated_amount": 20},  # new data
        "log_history": [
            {
                "created_at": Any(datetime),
                "status": "FINISHED",
                "step": "PARSING_DATA",
            },
            # log update
            {
                "created_at": Any(datetime),
                "status": "FINISHED",
                "step": "IMPORTING_CLIENTS",
            },
        ],
        "parsed_input": [
            ["Lorem", "ipsum dolor", "sit amet"],
            ["consectetur", "adipiscing", " elit.", "Nullam"],
            ["accumsan", "orci sit", "amet commodoconsectetur"],
        ],
    }
