# flake8: noqa
from datetime import datetime

import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.harmonist.proto.pipeline_pb2 import (
    CreateClientsInput,
    CreateClientsOutput,
    Error,
    File,
)

pytestmark = [pytest.mark.asyncio]

URL = "/v1/submit_data/"


async def test_returns_session_id(api):
    got = await api.post(
        URL,
        proto=CreateClientsInput(biz_id=123, text="Literally, any, separated, text"),
        decode_as=CreateClientsOutput,
        expected_status=200,
    )

    assert got == CreateClientsOutput(session_id=got.session_id)


async def test_saves_data_in_db(api, factory):
    got = await api.post(
        URL,
        proto=CreateClientsInput(biz_id=123, text="Literally; any; separated; text"),
        decode_as=CreateClientsOutput,
        expected_status=200,
    )

    assert await factory.find_creation_log(got.session_id) == {
        "_id": got.session_id,
        "biz_id": 123,
        "input_data": "Literally; any; separated; text",
        "input_data_type": "TEXT",
        "parsed_input": [["Literally", "any", "separated", "text"]],
        "created_at": Any(datetime),
        "log_history": [
            {
                "step": "PARSING_DATA",
                "status": "FINISHED",
                "created_at": Any(datetime),
            },
        ],
    }


@pytest.mark.parametrize(
    "input_data, expected_parsed_input",
    [
        ("; any; ; text", [["", "any", "", "text"]]),
        (
            "      Literally, any;mixed,separated; text        ",
            [["Literally, any", "mixed,separated", "text"]],
        ),
        (
            """Literally;any; separated; text   


            Repeated; in several; rows



            """,
            [
                ["Literally", "any", "separated", "text"],
                ["Repeated", "in several", "rows"],
            ],
        ),
    ],
)
async def test_parses_data_as_expected(input_data, expected_parsed_input, api, factory):
    got = await api.post(
        URL,
        proto=CreateClientsInput(biz_id=123, text=input_data),
        decode_as=CreateClientsOutput,
        expected_status=200,
    )

    log = await factory.find_creation_log(got.session_id)
    assert log["parsed_input"] == expected_parsed_input
