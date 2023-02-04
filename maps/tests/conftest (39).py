import pytest

pytest_plugins = ["aiohttp.pytest_plugin"]


@pytest.fixture
def loop(event_loop):
    return event_loop
