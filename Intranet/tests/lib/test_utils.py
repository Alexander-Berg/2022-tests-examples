import pytest

from copy import deepcopy
from intranet.trip.src.lib.utils import make_hash, mask_value


def test_make_hash_return_same_results():
    service_1_dict = {
        'type': 'hotel',
        'name': {
            'ru': 'Some hotel',
        },
        'location': {
            'name': {
                'ru': 'Some city',
                'en': 'Some city (en)',
            },
        },
        'start_at': '2021-06-07',
        'end_at': '2021-06-10',
    }
    service_2_dict = {
        'name': {
            'ru': 'Some hotel',
        },
        'type': 'hotel',
        'start_at': '2021-06-07',
        'end_at': '2021-06-10',
        'location': {
            'name': {
                'en': 'Some city (en)',
                'ru': 'Some city',
            },
        },
    }
    assert make_hash(service_1_dict) == make_hash(service_2_dict)


def test_make_hash_return_different_results():
    service_dict = {
        'type': 'hotel',
        'name': {
            'ru': 'Some hotel',
        },
        'location': {
            'name': {
                'ru': 'Some city',
            },
        },
        'start_at': '2021-06-07',
        'end_at': '2021-06-10',
    }
    modified_service_dict = deepcopy(service_dict)
    modified_service_dict['location']['name']['ru'] = 'Some other city'
    assert make_hash(service_dict) != make_hash(modified_service_dict)


@pytest.mark.parametrize(
    'value, prefix_len, suffix_len, placeholder, expected_result', (
        ('12387592123', 3, 0, '*', '123********'),
        ('12387592123', 0, 3, '*', '********123'),
        ('12387592123', 0, 0, '*', '***********'),
        ('12387592123', 3, 3, '*', '123*****123'),
        ('12387592123', 5, 6, '*', '12387592123'),
        ('12387592123', 0, 0, '@', '@@@@@@@@@@@'),
        (None, 0, 0, '*', ''),
    )
)
def test_mask_value(
    value,
    prefix_len,
    suffix_len,
    placeholder,
    expected_result,
):
    result = mask_value(
        value,
        placeholder=placeholder,
        visible_prefix_len=prefix_len,
        visible_suffix_len=suffix_len,
    )
    assert result == expected_result


@pytest.mark.parametrize('prefix_len, suffix_len', (
    (-1, 0),
    (0, -2),
))
def test_mask_value_bad_input_params(prefix_len, suffix_len):
    with pytest.raises(ValueError):
        mask_value(
            'some value',
            visible_prefix_len=prefix_len,
            visible_suffix_len=suffix_len,
        )
