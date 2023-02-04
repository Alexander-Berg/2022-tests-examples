import pytest

from maps.garden.sdk.core.version import simplify


def _check_equality(a, b):
    assert a == b
    assert str(a) == str(b)


def test_simple():
    _check_equality(simplify([]), ("list", ()))
    _check_equality(simplify(()), ("tuple", ()))

    complex_structure = ({1: [2, 3], 4: 5}, 6)

    with pytest.raises(TypeError):
        hash(complex_structure)

    hash(simplify(complex_structure))
    _check_equality(
        simplify(complex_structure),
        ("tuple", (
            ("dict", (
                (1, (
                    "list",
                    (2, 3)
                    )),
                (4, 5)
                )),
            6)))
