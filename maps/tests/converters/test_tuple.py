import pytest

from maps_adv.common.config_loader import NonConvertibleValue, converters


@pytest.mark.parametrize(
    "input_, type_, expected",
    (
        ["first, second,third", str, ("first", "second", "third")],
        ["1, 2,3", str, ("1", "2", "3")],
        ["1, 2,3", int, (1, 2, 3)],
        ["1,0, true,false", converters.Bool(), (True, False, True, False)],
    ),
)
def test_converts_to_requested_type(input_, type_, expected):
    converter = converters.Tuple(type_, delimiter=",")

    got = converter(input_)

    assert got == expected


@pytest.mark.parametrize(
    "input_, delimiter",
    (
        ["first, second,third", ","],
        ["first; second;third", ";"],
        ["first: second:third", ":"],
    ),
)
def test_converts_with_passed_delimiter(input_, delimiter):
    converter = converters.Tuple(str, delimiter=delimiter)

    got = converter(input_)

    assert got == ("first", "second", "third")


def test_by_default_uses_str_delimited_by_comma():
    converter = converters.Tuple()

    got = converter("first, second,third")

    assert got == ("first", "second", "third")


@pytest.mark.parametrize(
    "input_, type_",
    (["first, second,third", int], ["1,0, kek,false", converters.Bool()]),
)
def test_raises_for_unexpected_input(input_, type_):
    converter = converters.Tuple(type_, delimiter=",")

    with pytest.raises(NonConvertibleValue, match=input_):
        converter(input_)
