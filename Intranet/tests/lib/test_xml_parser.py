import pytest
import yt.wrapper

from intranet.trip.src.lib.xml_parser import RegistryParser


pytestmark = pytest.mark.asyncio


@yt.wrapper.yt_dataclass
class SomeRow:
    SOME_STR: str = None
    SOME_INT: int = None
    SOME_FLOAT: float = None


raw_xml_data = """
<?xml version="1.0" encoding="utf-8"?>
<Document>
<StrDoc><SOME_STR>Hello</SOME_STR><SOME_INT>3</SOME_INT><some_float>3.14</some_float></StrDoc>
<StrDoc><some_str>World</some_str><some_int>1</some_int></StrDoc>
<StrDoc><some_str>!</some_str><SOME_INT>-7</SOME_INT></StrDoc>
</Document>
"""


async def test_xml_parser_api():
    parser = RegistryParser(raw_xml=raw_xml_data.strip())
    _received_data = [
        row
        async for row in RegistryParser.rows_iterator(
            xml_parsers=[await parser.parser()],
            type_row=SomeRow,
        )
    ]

    assert len(_received_data) == 3
    # in xml
    assert SomeRow(SOME_STR='Hello', SOME_INT=3, SOME_FLOAT=3.14) in _received_data
    assert SomeRow(SOME_STR='World', SOME_INT=1) in _received_data
    assert SomeRow(SOME_STR='!', SOME_INT=-7) in _received_data
    # not in xml
    assert SomeRow(SOME_STR='Hello', SOME_INT=1) not in _received_data
    assert SomeRow(SOME_STR='Hello', SOME_INT=3) not in _received_data
    assert SomeRow(SOME_STR='Hello', SOME_INT=3, SOME_FLOAT='3.14') not in _received_data
