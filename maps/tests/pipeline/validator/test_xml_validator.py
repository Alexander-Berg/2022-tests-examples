import pytest
from lxml.etree import DocumentInvalid, fromstring as xml_bytes_parse

from maps_adv.export.lib.core.xml.base import XmlSchema
from maps_adv.export.lib.pipeline.validator import ValidateXmlSchema


def test_success_validation_example_pins_xml(xml_example_data: bytes, config):
    validate = ValidateXmlSchema.from_config(config)
    xml = xml_bytes_parse(xml_example_data)

    validate(xml)


def test_raises_invalid_xml(config):
    validate = ValidateXmlSchema.from_config(config)
    xml = xml_bytes_parse(
        b"""<?xml version='1.0' encoding='utf-8'?>
<ImposibleRootTag xmlns="http://maps.yandex.ru/advert/1.x" />
"""
    )

    with pytest.raises(DocumentInvalid):
        validate(xml)


def test_fail_validator_without_throw_exception(xml_example_data: bytes, config):
    validate = ValidateXmlSchema(XmlSchema.from_config(config), raises=False)
    xml = xml_bytes_parse(
        b"""<?xml version='1.0' encoding='utf-8'?>
<ImposibleRootTag xmlns="http://maps.yandex.ru/advert/1.x" />
"""
    )

    result = validate(xml)

    assert result == False  # noqa: E712
