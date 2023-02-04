"""Tests in-memory cache."""

import pytest

from walle.network import NoNetworkOnSwitch
from walle.util.cache import cached_with_exceptions


def test_cached():
    state = {"counter": 0}
    current_time = 0

    @cached_with_exceptions(value_ttl=10, error_ttl=5, timer=lambda: current_time)
    def func():
        state["counter"] += 1
        if state["counter"] == 3:
            raise NoNetworkOnSwitch("one", "two")
        return state["counter"]

    assert func() == 1
    assert func() == 1

    current_time += 20

    assert func() == 2
    assert func() == 2

    current_time += 20

    with pytest.raises(NoNetworkOnSwitch, match="one switch have no L3 networks for two VLAN."):
        func()

    with pytest.raises(NoNetworkOnSwitch, match="one switch have no L3 networks for two VLAN."):
        func()

    current_time += 6

    assert func() == 4
    assert func() == 4
