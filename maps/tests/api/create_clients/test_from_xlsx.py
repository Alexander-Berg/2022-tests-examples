from datetime import datetime

import pytest
from smb.common.multiruntime.lib.basics import is_arcadia_python
from smb.common.testing_utils import Any

from maps_adv.geosmb.harmonist.proto.pipeline_pb2 import (
    CreateClientsInput,
    CreateClientsOutput,
    Error,
    File,
)
from maps_adv.geosmb.harmonist.server.tests import load_xlsx_fixture_file

pytestmark = [pytest.mark.asyncio]
if is_arcadia_python:
    pytestmark.append(pytest.mark.usefixtures("setup_xlsx_fixture_files"))

URL = "/v1/submit_data/"


async def test_parses_data_as_expected(api, factory):
    data = load_xlsx_fixture_file("common.xlsx")

    got = await api.post(
        URL,
        proto=CreateClientsInput(
            biz_id=123,
            file=File(content=data, extension=File.Extension.XLSX),
        ),
        decode_as=CreateClientsOutput,
        expected_status=200,
    )

    assert await factory.find_creation_log(got.session_id) == {
        "_id": got.session_id,
        "biz_id": 123,
        "input_data": data,
        "input_data_type": "XLSX_FILE",
        "parsed_input": [["текст1", "текст2"], ["текст3", "текст4"]],
        "created_at": Any(datetime),
        "log_history": [
            {
                "step": "PARSING_DATA",
                "status": "FINISHED",
                "created_at": Any(datetime),
            },
        ],
    }


async def test_parses_empty_cells_as_empty_strings(api, factory):
    data = load_xlsx_fixture_file("emptycell.xlsx")

    got = await api.post(
        URL,
        proto=CreateClientsInput(
            biz_id=123,
            file=File(content=data, extension=File.Extension.XLSX),
        ),
        decode_as=CreateClientsOutput,
        expected_status=200,
    )

    log = await factory.find_creation_log(got.session_id)
    assert log["parsed_input"] == [
        ["ячейка1", "", "ячейка3"],
        ["", "ячейка5", "ячейка6"],
    ]


async def test_different_line_lengths(api, factory):
    data = load_xlsx_fixture_file("different_line_length.xlsx")

    got = await api.post(
        URL,
        proto=CreateClientsInput(
            biz_id=123,
            file=File(content=data, extension=File.Extension.XLSX),
        ),
        decode_as=CreateClientsOutput,
        expected_status=200,
    )

    log = await factory.find_creation_log(got.session_id)
    assert log["parsed_input"] == [
        ["ячейка1", "ячейка2", ""],
        ["ячейка3", "ячейка4", "ячейка5"],
        ["", "ячейка7", ""],
    ]


async def test_processes_formulas(api, factory):
    data = load_xlsx_fixture_file("formulas.xlsx")

    got = await api.post(
        URL,
        proto=CreateClientsInput(
            biz_id=123,
            file=File(content=data, extension=File.Extension.XLSX),
        ),
        decode_as=CreateClientsOutput,
        expected_status=200,
    )

    log = await factory.find_creation_log(got.session_id)
    assert log["parsed_input"] == [
        ["3", "8", "11"],
        ["", "2", "4"],
    ]


async def test_uses_active_sheet(api, factory):
    data = load_xlsx_fixture_file("twosheets.xlsx")

    got = await api.post(
        URL,
        proto=CreateClientsInput(
            biz_id=123,
            file=File(content=data, extension=File.Extension.XLSX),
        ),
        decode_as=CreateClientsOutput,
        expected_status=200,
    )

    log = await factory.find_creation_log(got.session_id)
    assert log["parsed_input"] == [
        ["1текст", "2текст"],
        ["3текст", "4текст"],
    ]


async def test_returns_error_if_failed_to_parse_data_as_xlsx(api, factory):
    got = await api.post(
        URL,
        proto=CreateClientsInput(
            biz_id=123,
            file=File(content=b"\x80", extension=File.Extension.XLSX),
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.BAD_FILE_CONTENT,
        description="Content of file can't be parsed as XLSX file",
    )
