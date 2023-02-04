import pytest

from irt.utils import suppress


def test_suppress():
    with suppress(ValueError) as enter_result:
        assert enter_result is None

    with suppress(ValueError):
        assert pow(2, 5) == 32

    with suppress(TypeError):
        len(5)

    with suppress(LookupError):
        'Hello'[50]

    with pytest.raises(ZeroDivisionError):
        with suppress(TypeError):
            1 / 0

    with pytest.raises(ZeroDivisionError):
        with suppress():
            1 / 0

    with suppress(ZeroDivisionError, TypeError):
        1 / 0
    with suppress(ZeroDivisionError, TypeError):
        len(5)

    ignore_exceptions = suppress(Exception)
    with ignore_exceptions:
        pass
    with ignore_exceptions:
        len(5)
    with ignore_exceptions:
        1 / 0
        with ignore_exceptions:
            len(5)
