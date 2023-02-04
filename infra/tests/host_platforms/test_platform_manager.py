import pytest

from walle.host_platforms.errors import PlatformAlreadyExistsError
from walle.host_platforms.platform import Platform
from walle.host_platforms.platform_manager import PlatformRegistry, PlatformStore, create_platform_for_host


class EmptyPlatform:
    pass


class MockedPlatform(Platform):
    system = 'mocked_system'
    board = 'mocked_board'

    def __init__(self, host):
        super().__init__(host)


@pytest.fixture
def mocked_host(test):
    return test.mock_host(
        {
            "inv": 0,
            "platform": {"system": "mocked_system", "board": "mocked_board"},
            "name": "mocked-hostname.search.yandex.net",
        }
    )


@pytest.fixture
def mocked_registry(monkeypatch):
    monkeypatch.setattr(PlatformRegistry, "registered_platforms", PlatformStore())
    return PlatformRegistry


@pytest.fixture
def empty_platform_dict():
    return PlatformStore()


@pytest.fixture
def platform_dict():
    pd = PlatformStore()
    pd.add_platform("system", "board", EmptyPlatform)
    return pd


@pytest.fixture
def mocked_platform():
    return MockedPlatform


@pytest.fixture
def mocked_supported_host(test):
    return test.mock_host(
        {
            "inv": 0,
            "platform": {"system": "mocked_system", "board": "mocked_board"},
            "name": "mocked-hostname.search.yandex.net",
        }
    )


@pytest.fixture
def mocked_unsupported_host(test):
    return test.mock_host(
        {
            "inv": 1,
            "platform": {"system": "mocked_unsupported_system", "board": "mocked_unsupported_board"},
            "name": "unsupported-mocked-hostname.search.yandex.net",
        }
    )


@pytest.fixture
def mocked_registry_with_platform(monkeypatch):
    store = PlatformStore()
    store.add_platform(MockedPlatform.system, MockedPlatform.board, MockedPlatform)
    monkeypatch.setattr(PlatformRegistry, "registered_platforms", store)
    return PlatformRegistry


class TestPlatformFactory:
    def test_create_platform_for_supported_host(self, mocked_registry_with_platform, mocked_supported_host):
        platform = create_platform_for_host(mocked_supported_host)
        assert isinstance(platform, MockedPlatform)

    def test_create_platform_for_unsupported_host(self, mocked_registry, mocked_unsupported_host):
        platform = create_platform_for_host(mocked_unsupported_host)
        assert isinstance(platform, Platform)


class TestPlatformRegistry:
    def test_registry(self, mocked_registry, mocked_platform, mocked_host):
        platform = mocked_registry.register_platform(mocked_platform)
        assert mocked_registry.get_platform_for_host(mocked_host) == MockedPlatform
        assert platform == mocked_platform

    def test_register_platform_twice(self, mocked_registry, mocked_platform):
        mocked_registry.register_platform(mocked_platform)
        with pytest.raises(PlatformAlreadyExistsError):
            mocked_registry.register_platform(mocked_platform)


class TestPlatformStore:
    @pytest.mark.parametrize(
        ["system", "board", "value"],
        [
            ("filled_system", "filled_board", EmptyPlatform),
            (None, "filled_board", EmptyPlatform),
            ("filled_system", None, EmptyPlatform),
        ],
    )
    def test_add_value_correct(self, empty_platform_dict, system, board, value):
        empty_platform_dict.add_platform(system, board, value)
        assert empty_platform_dict.get_platform(system, board) == value

    @pytest.mark.parametrize(["system", "board"], [("system", "board"), (None, "board"), ("system", None)])
    def test_get_value(self, platform_dict, system, board):
        assert platform_dict.get_platform(system, board) == EmptyPlatform
