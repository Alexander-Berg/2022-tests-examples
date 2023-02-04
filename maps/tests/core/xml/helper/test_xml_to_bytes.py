import pytest

from maps_adv.export.lib.core.xml.base import XmlDoc
from maps_adv.export.lib.core.xml.helper import xml_to_bytes


def test_returns_expected_formatted_xml_bytes():
    doc = XmlDoc("root")
    doc.tag("child", name="attr", text="text value")
    doc.tag("childEmpty")
    with doc.tag("parent"):
        doc.tag("childEmpty")

    result = xml_to_bytes(doc.result)

    assert (
        result
        == b"""<root>
  <child name="attr">text value</child>
  <childEmpty/>
  <parent>
    <childEmpty/>
  </parent>
</root>\n"""
    )


@pytest.mark.parametrize(
    ["use_xml_declaration", "xml_declaration"],
    [[True, b"<?xml version='1.0' encoding='utf-8'?>\n"], [False, b""]],
)
def test_returns_expected_bytes_of_xml_with_or_without_declaration(
    use_xml_declaration, xml_declaration
):
    doc = XmlDoc("root")
    result = xml_to_bytes(doc.result, xml_declaration=use_xml_declaration)

    assert result == xml_declaration + b"<root/>\n"
