import pytest

from intranet.femida.src.candidates.filters.base import FilterCtl
from constance.test import override_config


pytestmark = pytest.mark.django_db
city_filter = 'CityFilter'


def test_filters_as_meta_dict():
    data = FilterCtl.as_meta_dict()

    assert 'structure' in data
    assert 'conditions' in data

    conditions = data['conditions']
    for condition_name, condition_data in conditions.items():
        assert 'condition' in condition_data
        assert 'choices' in condition_data['condition']
        choices = condition_data['condition']['choices']

        assert 'forms' in condition_data
        assert len(condition_data['forms']) == len(choices)


@pytest.mark.parametrize('disabled_filters', (f'["{city_filter}"]', '[]'))
def test_filter_form_with_disabled_filters(disabled_filters):
    is_city_filter_expected_in_choices = city_filter not in disabled_filters
    city_choice = {'label': city_filter, 'value': city_filter}

    with override_config(DISABLED_FILTERS=disabled_filters):
        data = FilterCtl.as_meta_dict()

    structure = data['structure']
    choices = structure['filters']['structure']['value']['field']['choices']
    assert (city_choice in choices) == is_city_filter_expected_in_choices
