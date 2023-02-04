import pytest

from maps_adv.common.shared_mock import SharedMock

pytest_plugins = ["maps_adv.common.shared_mock.pytest.plugin"]


@pytest.fixture
def shared_mock():
    return SharedMock()
