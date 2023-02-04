import pytest


def test_calls_args_is_last_call_args(shared_mock):
    shared_mock(1, 2, b=3)
    shared_mock(11, 22, c=33)

    assert shared_mock.call_args == ((11, 22), {"c": 33})


def test_calls_args_is_none_if_mock_was_not_called(shared_mock):
    assert shared_mock.call_args is None


def test_call_args_list_is_all_call_args(shared_mock):
    shared_mock(1, 2, b=3)
    shared_mock(11, 22, c=33)
    shared_mock(1, 2, b=3)
    shared_mock(1, 2, b=3)

    assert shared_mock.call_args_list == [
        ((1, 2), {"b": 3}),
        ((11, 22), {"c": 33}),
        ((1, 2), {"b": 3}),
        ((1, 2), {"b": 3}),
    ]


def test_call_args_list_is_empty_list_if_mock_was_not_called(shared_mock):
    assert shared_mock.call_args_list == []


def test_call_count_is_how_many_times_mock_was_called(shared_mock):
    shared_mock(1, 2)
    shared_mock(2, 3, c=4)
    shared_mock(1, 2)
    shared_mock(1, 2)

    assert shared_mock.call_count == 4


def test_call_count_zero_if_mock_was_not_called(shared_mock):
    assert shared_mock.call_count == 0


@pytest.mark.parametrize("times_called", range(1, 4))
def test_called_is_true_if_mock_was_called(shared_mock, times_called):
    for i in range(times_called):
        shared_mock(i)

    assert shared_mock.called is True


def test_called_is_false_if_mock_was_not_called(shared_mock):
    assert shared_mock.called is False
