import pytest

from maps_adv.export.lib.core.xml.helper import xml_namespace


@pytest.mark.parametrize(
    ["namespace", "name", "expected_name"],
    [
        ["ad", "name", "{http://maps.yandex.ru/advert/1.x}name"],
        ["biz", "name", "{http://maps.yandex.ru/business/1.x}name"],
        ["xml", "name", "{http://www.w3.org/XML/1998/namespace}name"],
    ],
)
def test_returns_name_with_namespace_as_expected(namespace, name, expected_name):
    result = xml_namespace("name", namespace)
    assert result == expected_name


def test_returns_name_with_ad_namespace_if_no_set_namespace_argument():
    result = xml_namespace("name")
    assert result == "{http://maps.yandex.ru/advert/1.x}name"
