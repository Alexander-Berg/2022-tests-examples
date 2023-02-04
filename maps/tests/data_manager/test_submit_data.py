from datetime import datetime

import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.harmonist.server.lib.enums import InputDataType

pytestmark = [pytest.mark.asyncio]


async def test_saves_data_as_expected(dm, factory):
    session_id = await dm.submit_data(
        biz_id=123,
        input_data="Literally any text",
        input_data_type=InputDataType.TEXT,
        parsed_input=[("Literally", "any", "text")],
    )

    assert await factory.find_creation_log(session_id) == {
        "_id": session_id,
        "biz_id": 123,
        "input_data": "Literally any text",
        "input_data_type": "TEXT",
        "parsed_input": [["Literally", "any", "text"]],
        "created_at": Any(datetime),
        "log_history": [
            {
                "step": "PARSING_DATA",
                "status": "FINISHED",
                "created_at": Any(datetime),
            },
        ],
    }


async def test_returns_session_id(dm):
    session_id = await dm.submit_data(
        biz_id=123,
        input_data="Literally any text",
        input_data_type=InputDataType.TEXT,
        parsed_input=[("Literally", "any", "text")],
    )

    assert session_id == Any(str)
