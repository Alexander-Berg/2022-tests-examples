from util import get_all_valid_tariffs_descriptions

import yatest.common
from xml.etree import ElementTree
import pytest


def get_all_categories():
    categories_xml = ElementTree.parse(yatest.common.build_path('maps/wikimap/mapspro/cfg/editor/categories.xml'))
    return frozenset(category.attrib.get('id') for category in categories_xml.getroot())


ALL_CATEGORIES = get_all_categories()


@pytest.mark.parametrize('tariffs_description', get_all_valid_tariffs_descriptions('maps/tasks_tariffs/cartographic/edits'))
def test_edit_tariffs_should_contain_all_categories(tariffs_description):
    for tariff in tariffs_description['tariffs']:
        non_used_categories = set(ALL_CATEGORIES)
        for category in tariff['tasks']:
            non_used_categories.discard(category)
        assert len(non_used_categories) == 0,\
            f"Categories {non_used_categories} are absent in tariff '{tariff['tariff_name']}'."
