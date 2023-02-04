from datetime import datetime

import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.harmonist.server.lib.enums import PipelineStep, StepStatus

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("pipeline_step", PipelineStep)
@pytest.mark.parametrize("status", StepStatus)
@pytest.mark.parametrize("failed_reason", [None, "some kek"])
async def test_returns_nothing(failed_reason, pipeline_step, status, dm, factory):
    session_id = await factory.create_log()

    got = await dm.update_log_status(
        session_id=session_id,
        biz_id=123,
        pipeline_step=pipeline_step,
        status=status,
        failed_reason=failed_reason,
    )

    assert got is None


@pytest.mark.parametrize("pipeline_step", PipelineStep)
@pytest.mark.parametrize("status", StepStatus)
@pytest.mark.parametrize("failed_reason", [dict(), dict(failed_reason="some kek")])
async def test_updates_data_in_db(failed_reason, pipeline_step, status, dm, factory):
    # creates PARSING_DATA.FINISHED record in log history
    session_id = await factory.create_log()

    await dm.update_log_status(
        session_id=session_id,
        biz_id=123,
        pipeline_step=pipeline_step,
        status=status,
        **failed_reason,
    )

    creation_log = await factory.find_creation_log(session_id)
    assert creation_log == {
        "_id": session_id,
        "biz_id": 123,
        "created_at": Any(datetime),
        "input_data": "Literally anything",
        "input_data_type": "TEXT",
        "log_history": [
            {
                "created_at": Any(datetime),
                "status": "FINISHED",
                "step": "PARSING_DATA",
            },
            # new inserted data
            {
                "created_at": Any(datetime),
                "status": status.name,
                "step": pipeline_step.name,
                **failed_reason,
            },
        ],
        "parsed_input": [
            ["Lorem", "ipsum dolor", "sit amet"],
            ["consectetur", "adipiscing", " elit.", "Nullam"],
            ["accumsan", "orci sit", "amet commodoconsectetur"],
        ],
    }
