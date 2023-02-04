import pytest


@pytest.mark.parametrize("times_called", range(1, 4))
def test_assert_called_not_raises_if_mock_was_called(shared_mock, times_called):
    for i in range(times_called):
        shared_mock(i)

    try:
        shared_mock.assert_called()
    except AssertionError:
        pytest.fail("Should not raise AssertionError")


def test_assert_called_raises_if_mock_was_not_called(shared_mock):
    with pytest.raises(
        AssertionError, match="Expected 'shared_mock' to have been called."
    ):
        shared_mock.assert_called()


def test_assert_not_called_not_raises_if_mock_was_not_called(shared_mock):
    try:
        shared_mock.assert_not_called()
    except AssertionError:
        pytest.fail("Should not raise AssertionError")


@pytest.mark.parametrize("times_called", range(1, 4))
def test_assert_not_called_raises_if_mock_was_called(shared_mock, times_called):
    for i in range(times_called):
        shared_mock(i)

    with pytest.raises(
        AssertionError,
        match=f"Expected 'shared_mock' to not have been called. Called {times_called} times.",
    ):
        shared_mock.assert_not_called()


def test_assert_called_with_not_raises_if_last_called_with_provided_args(shared_mock):
    shared_mock(1, 2, b=3)
    shared_mock(11, 22, c=33)

    try:
        shared_mock.assert_called_with(11, 22, c=33)
    except AssertionError:
        pytest.fail("Should not raise AssertionError")


@pytest.mark.parametrize(
    ("assert_args", "assert_kwargs"),
    [(tuple(), dict()), ((1, 2), dict()), (tuple(), {"c": 3}), ((1, 2), {"c": 3})],
)
def test_assert_called_with_raises_if_mock_was_not_called(
    shared_mock, assert_args, assert_kwargs
):
    with pytest.raises(AssertionError, match="Not called"):
        shared_mock.assert_called_with(*assert_args, **assert_kwargs)


@pytest.mark.parametrize(
    ("assert_args", "assert_kwargs"),
    [
        (tuple(), dict()),
        ((1, 2), dict()),
        (tuple(), {"c": 3}),
        ((2, 3), {"c": 3}),
        ((1, 2), {"c": 4}),
        ((1, 2), {"d": 3}),
    ],
)
def test_assert_called_with_raises_if_mock_was_called_with_other_arguments(
    shared_mock, assert_args, assert_kwargs
):
    shared_mock(1, 2, c=3)

    with pytest.raises(AssertionError, match="Last called with other arguments"):
        shared_mock.assert_called_with(*assert_args, **assert_kwargs)


def test_assert_called_with_raises_if_mock_call_with_these_arguments_is_not_last(
    shared_mock
):
    shared_mock(1, 2, c=3)
    shared_mock(12, 22, c=33)

    with pytest.raises(AssertionError, match="Last called with other arguments"):
        shared_mock.assert_called_with(1, 2, c=3)


def test_assert_any_call_not_raises_if_mock_called_last_with_provided_args(shared_mock):
    shared_mock(1, 2, c=3)
    shared_mock(11, 22, c=33)

    try:
        shared_mock.assert_any_call(11, 22, c=33)
    except AssertionError:
        pytest.fail("Should not raise AssertionError")


def test_assert_any_call_not_raises_if_mock_called_not_last_with_provided_args(
    shared_mock
):
    shared_mock(1, 2, c=3)
    shared_mock(11, 22, c=33)
    shared_mock(111, 222, c=333)

    try:
        shared_mock.assert_any_call(11, 22, c=33)
    except AssertionError:
        pytest.fail("Should not raise AssertionError")


def test_assert_any_call_raises_if_mock_was_not_called(shared_mock):
    with pytest.raises(AssertionError, match="Not called"):
        shared_mock.assert_any_call(11, 22, c=33)


@pytest.mark.parametrize(
    ("assert_args", "assert_kwargs"),
    [
        (tuple(), dict()),
        ((1, 2), dict()),
        (tuple(), {"c": 3}),
        ((2, 3), {"c": 3}),
        ((1, 2), {"c": 4}),
        ((1, 2), {"d": 3}),
    ],
)
def test_assert_any_call_raises_if_mock_was_called_with_other_arguments(
    shared_mock, assert_args, assert_kwargs
):
    shared_mock(1, 2, c=3)

    with pytest.raises(AssertionError, match="Last called with other arguments"):
        shared_mock.assert_any_call(*assert_args, **assert_kwargs)


def test_assert_enter_called_not_raises_if_mock_was_used_as_context_manager(
    shared_mock
):
    with shared_mock:
        pass

    try:
        shared_mock.assert_enter_called()
    except AssertionError:
        pytest.fail("Should not raise AssertionError")


def test_assert_enter_called_raises_if_mock_was_not_used_as_context_manager(
    shared_mock
):
    with pytest.raises(AssertionError, match="Not called"):
        shared_mock.assert_enter_called()


def test_assert_exit_called_not_raises_if_mock_was_used_as_context_manager(shared_mock):
    with shared_mock:
        pass

    try:
        shared_mock.assert_exit_called()
    except AssertionError:
        pytest.fail("Should not raise AssertionError")


def test_assert_exit_called_raises_if_mock_was_not_used_as_context_manager(shared_mock):
    with pytest.raises(AssertionError, match="Not called"):
        shared_mock.assert_exit_called()


def test_assert_exit_called_without_exception_not_raises_if_no_exception_raised_in_cm_block(
    shared_mock
):
    with shared_mock:
        pass

    try:
        shared_mock.assert_exit_called_without_exception()
    except AssertionError:
        pytest.fail("Should not raise AssertionError")


def test_assert_exit_called_without_exception_raises_if_exception_raised_in_cm_block(
    shared_mock
):
    try:
        with shared_mock:
            raise Exception
    except Exception:
        pass

    with pytest.raises(AssertionError, match="Called with exception"):
        shared_mock.assert_exit_called_without_exception()


def test_assert_exit_called_without_exception_raises_if_mock_was_not_used_as_context_manager(
    shared_mock
):
    shared_mock()

    with pytest.raises(AssertionError, match="Not called"):
        shared_mock.assert_exit_called_without_exception()


def test_assert_exit_called_with_exception_raises_if_no_exception_raised_in_cm_block(
    shared_mock
):
    with shared_mock:
        pass

    with pytest.raises(AssertionError):
        shared_mock.assert_exit_called_with_exception()


def test_assert_exit_called_with_exception_not_raises_if_exception_raised_in_cm_block(
    shared_mock
):
    try:
        with shared_mock:
            raise Exception
    except Exception:
        pass

    try:
        shared_mock.assert_exit_called_with_exception()
    except AssertionError:
        pytest.fail("Should not raise AssertionError")


def test_assert_exit_called_with_exception_raises_if_mock_was_not_used_as_context_manager(
    shared_mock
):
    shared_mock()

    with pytest.raises(AssertionError, match="Not called"):
        shared_mock.assert_exit_called_with_exception()
