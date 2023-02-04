import pytest

from maps_adv.export.lib.core.utils import printable


@pytest.mark.parametrize(
    "source, target",
    (
        ([1, 2, 3], "1, 2, 3"),
        ({1, 2, 3}, "1, 2, 3"),
        ({1: "a", 2: "b", 3: "c"}, "1, 2, 3"),
        ([], ""),
        ({}, ""),
    ),
)
def test_printable(source, target):
    got = printable(source)
    assert got == target
