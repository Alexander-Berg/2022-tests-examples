from datetime import datetime

import pytest
from smb.common.testing_utils import Any

pytestmark = [pytest.mark.asyncio]


async def test_returns_nothing(dm, factory):
    session_id = await factory.create_log()

    got = await dm.submit_error_file(
        session_id=session_id, biz_id=123, validation_errors_file_link="http://any_link"
    )

    assert got is None


async def test_updates_data_in_db(dm, factory):
    # creates PARSING_DATA.FINISHED record in log history
    session_id = await factory.create_log()

    await dm.submit_error_file(
        session_id=session_id, biz_id=123, validation_errors_file_link="http://any_link"
    )

    creation_log = await factory.find_creation_log(session_id)
    assert creation_log == {
        "_id": session_id,
        "biz_id": 123,
        "created_at": Any(datetime),
        "input_data": "Literally anything",
        "input_data_type": "TEXT",
        "validation_errors_file_link": "http://any_link",  # new data
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
                "step": "VALIDATING_DATA",
            },
        ],
        "parsed_input": [
            ["Lorem", "ipsum dolor", "sit amet"],
            ["consectetur", "adipiscing", " elit.", "Nullam"],
            ["accumsan", "orci sit", "amet commodoconsectetur"],
        ],
    }
