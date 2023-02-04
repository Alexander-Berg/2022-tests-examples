from datetime import datetime

import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.harmonist.server.lib.enums import StepStatus

pytestmark = [pytest.mark.asyncio]


async def test_returns_nothing(dm, factory):
    session_id = await factory.create_log()

    got = await dm.submit_validated_clients(
        session_id=session_id,
        biz_id=123,
        valid_clients=[dict(email="some@email.ru")],
        invalid_clients=[dict(row=["some", "values"], reason="because")],
        validation_step_status=StepStatus.IN_PROGRESS,
    )

    assert got is None


@pytest.mark.parametrize("step_status", StepStatus)
async def test_updates_data_in_db(step_status, dm, factory):
    # creates PARSING_DATA.FINISHED record in log history
    session_id = await factory.create_log()

    await dm.submit_validated_clients(
        session_id=session_id,
        biz_id=123,
        valid_clients=[dict(email="some@email.ru")],
        invalid_clients=[dict(row=["some", "values"], reason="because")],
        validation_step_status=step_status,
    )

    creation_log = await factory.find_creation_log(session_id)
    assert creation_log == {
        "_id": session_id,
        "biz_id": 123,
        "created_at": Any(datetime),
        "input_data": "Literally anything",
        "input_data_type": "TEXT",
        # new data
        "invalid_clients": [{"reason": "because", "row": ["some", "values"]}],
        "valid_clients": [{"email": "some@email.ru"}],
        "log_history": [
            {
                "created_at": Any(datetime),
                "status": "FINISHED",
                "step": "PARSING_DATA",
            },
            # log update
            {
                "created_at": Any(datetime),
                "status": step_status.name,
                "step": "VALIDATING_DATA",
            },
        ],
        "parsed_input": [
            ["Lorem", "ipsum dolor", "sit amet"],
            ["consectetur", "adipiscing", " elit.", "Nullam"],
            ["accumsan", "orci sit", "amet commodoconsectetur"],
        ],
    }
