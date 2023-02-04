import pytest

from contextlib import contextmanager


@contextmanager
def assert_not_raises(exception=None):
    exception = exception or Exception
    try:
        yield
    except exception as exc:
        pytest.fail('Unexpected exception: %s' % repr(exc))


def assert_history_exists(obj, event, **kwargs):
    history = obj.history.filter(event=event, **kwargs)
    assert history.exists()
