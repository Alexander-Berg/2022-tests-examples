import pytest
from smb.common.multiruntime.lib.basics import is_arcadia_python

from maps_adv.geosmb.harmonist.server.lib.domain import XLSXParser
from maps_adv.geosmb.harmonist.server.lib.exceptions import BadFileContent
from maps_adv.geosmb.harmonist.server.tests import load_xlsx_fixture_file

pytestmark = [pytest.mark.asyncio]
if is_arcadia_python:
    pytestmark.append(pytest.mark.usefixtures("setup_xlsx_fixture_files"))


@pytest.fixture
def parser():
    return XLSXParser()


async def test_parses_data_as_expected(parser):
    got = parser.parse(load_xlsx_fixture_file("common.xlsx"))

    assert got == [["текст1", "текст2"], ["текст3", "текст4"]]


async def test_parses_empty_cells_as_empty_strings(parser):
    got = parser.parse(load_xlsx_fixture_file("emptycell.xlsx"))

    assert got == [
        ["ячейка1", "", "ячейка3"],
        ["", "ячейка5", "ячейка6"],
    ]


async def test_different_line_lengths(parser):
    got = parser.parse(load_xlsx_fixture_file("different_line_length.xlsx"))

    assert got == [
        ["ячейка1", "ячейка2", ""],
        ["ячейка3", "ячейка4", "ячейка5"],
        ["", "ячейка7", ""],
    ]


async def test_processes_formulas(parser):
    got = parser.parse(load_xlsx_fixture_file("formulas.xlsx"))

    assert got == [
        ["3", "8", "11"],
        ["", "2", "4"],
    ]


async def test_uses_active_sheet(parser):
    got = parser.parse(load_xlsx_fixture_file("twosheets.xlsx"))

    assert got == [
        ["1текст", "2текст"],
        ["3текст", "4текст"],
    ]


async def test_raises_if_failed_to_parse_data_as_xlsx(parser):
    with pytest.raises(
        BadFileContent, match="Content of file can't be parsed as XLSX file"
    ):
        parser.parse(b"\x80")
