# flake8: noqa
import pytest

from maps_adv.geosmb.harmonist.server.lib.domain import CsvParser
from maps_adv.geosmb.harmonist.server.lib.exceptions import BadFileContent

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def parser():
    return CsvParser()


@pytest.mark.parametrize(
    "data, expected_parsed",
    [
        # text
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
        # bytes
        (b"; any; ; text", [["", "any", "", "text"]]),
        (
            b"      Literally, any;mixed,separated; text        ",
            [["Literally, any", "mixed,separated", "text"]],
        ),
        (
            b"""Literally;any; separated; text   


            Repeated; in several; rows



            """,
            [
                ["Literally", "any", "separated", "text"],
                ["Repeated", "in several", "rows"],
            ],
        ),
    ],
)
async def test_parses_data_as_expected(parser, data, expected_parsed):
    got = parser.parse(data)

    assert got == expected_parsed


async def test_raises_if_failed_decode_to_utf8(parser):
    with pytest.raises(
        BadFileContent, match="Content of file can't be decoded as utf-8"
    ):
        parser.parse(b"\x80")
