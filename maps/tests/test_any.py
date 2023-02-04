import pytest

from maps_adv.common.helpers import Any


@pytest.mark.parametrize("value, cls", (["text", str], [100500, int], [100.500, float]))
def test_equal_true_if_isinstance(value, cls):
    assert Any(cls) == value


@pytest.mark.parametrize("value, cls", (["text", int], [100500, float], [100.500, str]))
def test_not_equal_if_not_isinstance(value, cls):
    assert Any(cls) != value


def test_type_and_len_equal():
    assert Any(list).of_len(2) == [1, 2]


def test_type_equal_len_not():
    assert Any(list).of_len(2) != [1, 2, 3]


def test_len_equal_type_not():
    assert Any(list).of_len(2) != {1, 2}
