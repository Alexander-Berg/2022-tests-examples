from unittest.mock import Mock

import pytest

from maps_adv.common.shared_mock import SHARED_MOCK_DEFAULT, SharedMock


class CustomException(BaseException):
    pass


def test_returns_self_by_default(shared_mock):
    mock = SharedMock()

    retval = mock()

    assert retval is mock


def test_uses_return_value():
    return_value = object()
    mock = SharedMock(return_value=return_value)

    retval = mock()

    assert retval is return_value


def test_uses_side_effect_callable():
    side_effect_return_value = object()
    side_effect_mock = Mock(return_value=side_effect_return_value)
    mock = SharedMock(side_effect=side_effect_mock)

    retval = mock(1, b=2)

    side_effect_mock.assert_called_with(1, b=2)
    assert retval is side_effect_return_value


def test_raises_side_effect_if_is_exception_object():
    exception = CustomException()
    mock = SharedMock(side_effect=exception)

    with pytest.raises(CustomException) as exc:
        mock()

    assert exc.value is exception


def test_raises_side_effect_if_is_exception_class():
    mock = SharedMock(side_effect=CustomException)

    with pytest.raises(CustomException):
        mock()


def tests_prefers_side_effect_to_return_value():
    return_value, side_effect_return_value = object(), object()
    side_effect_mock = Mock(return_value=side_effect_return_value)
    mock = SharedMock(side_effect=side_effect_mock)

    retval = mock()

    side_effect_mock.assert_called()
    assert retval is side_effect_return_value


def test_uses_return_value_if_side_effect_returns_default():
    return_value = object()
    side_effect_mock = Mock(return_value=SHARED_MOCK_DEFAULT)
    mock = SharedMock(side_effect=side_effect_mock, return_value=return_value)

    retval = mock()

    side_effect_mock.assert_called()
    assert retval is return_value


def test_raises_type_error_for_invalid_side_effect_when_called():
    mock = SharedMock(side_effect=4)

    with pytest.raises(TypeError, match="Bad side effect"):
        mock()


def test_side_effect_can_be_reset():
    return_value = object()
    side_effect_mock = Mock()
    mock = SharedMock(side_effect=side_effect_mock, return_value=return_value)

    mock.side_effect = None
    retval = mock()

    side_effect_mock.assert_not_called()
    assert retval is return_value


def test_side_effect_can_be_sequence():
    obj1, obj2, obj3 = object(), object(), []
    func = lambda: 3
    exception = CustomException("info")
    mock = SharedMock(
        side_effect=[obj1, func, CustomException, None, exception, obj2, obj3],
        return_value=4,
    )

    assert mock() is obj1
    assert mock() is func
    with pytest.raises(CustomException):
        mock()
    assert mock() is None
    with pytest.raises(CustomException, match="info") as exc:
        mock()
    assert exc.value is exception
    assert mock() is obj2
    assert mock() is obj3
    with pytest.raises(StopIteration):
        mock()
