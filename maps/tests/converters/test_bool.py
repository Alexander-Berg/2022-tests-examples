import pytest

from maps_adv.common.config_loader import NonConvertibleValue, converters


@pytest.mark.parametrize(
    "input_, expected",
    (["true", True], ["1", True], ["false", False], ["0", False], ["null", False]),
)
def test_returns_expected_result(input_, expected):
    converter = converters.Bool()

    got = converter(input_)

    assert got == expected


@pytest.mark.parametrize("input_", (True, False, 1, 0, "kek", ""))
def test_raises_for_unexpected_result(input_):
    converter = converters.Bool()

    with pytest.raises(NonConvertibleValue, match=str(input_)):
        converter(input_)
