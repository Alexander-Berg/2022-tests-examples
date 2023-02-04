import pytest

from maps_adv.common.helpers import Converter

converter = Converter(((1, "1"), (2, "2"), (3, "3")))


@pytest.mark.parametrize("current, expected", ((1, "1"), (2, "2"), (3, "3")))
def test_forward(current, expected):
    assert converter.forward(current) == expected


@pytest.mark.parametrize("current, expected", (("1", 1), ("2", 2), ("3", 3)))
def test_reversed(current, expected):
    assert converter.reversed(current) == expected


@pytest.mark.parametrize("call", (converter.forward, converter.reversed))
def test_raises_for_unexistent_choice(call):
    with pytest.raises(KeyError):
        call(123)
