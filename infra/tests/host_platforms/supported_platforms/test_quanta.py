import pytest

from walle.host_platforms.supported_platforms.quanta import PlatformQuantaR210MB2MS


@pytest.fixture
def quanta_platform(test):
    host = test.mock_host(
        {"name": "mocked-quanta-hostname", "platform": {"system": "R210-MB2MS", "board": "R210-MB2MS"}}
    )

    return PlatformQuantaR210MB2MS(host)


class TestPlatformQuanta:
    def test_supports_ipxe(self, quanta_platform):
        assert quanta_platform.ipxe_supported() is False
