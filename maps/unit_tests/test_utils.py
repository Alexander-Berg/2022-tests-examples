import pytest

from maps.b2bgeo.pipedrive_gate.lib.logic.util import normalize_field, reverse_dict


def test_normalize_field():
    assert normalize_field('') == ''
    assert normalize_field(None) is None
    assert normalize_field('    ') == ''
    assert normalize_field('  1   2   3   ') == '1 2 3'
    assert normalize_field('  1 \t  2 \n  3   ') == '1 2 3'
    assert normalize_field(1) == 1


def test_reverse_dict():
    assert reverse_dict({4: {"42", "40"}, 3: {"37"}}) == {"42": 4, "40": 4, "37": 3}
    assert reverse_dict({5: {"BY", "KZ", "UZ"}}) == {"BY": 5, "UZ": 5, "KZ": 5}
    assert reverse_dict({5: {}}) == {}
    assert reverse_dict({}) == {}
    with pytest.raises(RuntimeError):
        reverse_dict({5: {"BY", "KZ", "UZ"}, 2: {"GB", "BY"}})
