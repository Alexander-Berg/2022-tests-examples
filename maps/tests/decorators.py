import logging
import time
import pytest

from maps.garden.sdk.utils.decorators import time_measurer, log_method

logger = logging.getLogger("garden.core.tests")


def test_time_measurer(caplog):
    @time_measurer(logger)
    def foo():
        time.sleep(0.001)

    foo()

    assert caplog.messages[-1].startswith("foo took")


def test_time_measurer_raise(caplog):
    @time_measurer(logger)
    def foo_raises():
        raise RuntimeError("Aha")

    with pytest.raises(RuntimeError):
        foo_raises()

    assert caplog.messages[-1].startswith("foo_raises took")


def test_log_method(caplog):
    class Foo:
        @log_method(logger)
        def bar(self):
            time.sleep(0.001)

    Foo().bar()

    assert caplog.messages[-2].startswith("Foo::bar")
    assert caplog.messages[-1] == "Foo::bar ends"


def test_log_method_raises(caplog):
    class Foo:
        @log_method(logger)
        def bar_raises(self):
            raise RuntimeError("Aha")

    with pytest.raises(RuntimeError):
        Foo().bar_raises()

    assert caplog.messages[-2].startswith("Foo::bar_raises")
    assert caplog.messages[-1] == "Foo::bar_raises ends"
