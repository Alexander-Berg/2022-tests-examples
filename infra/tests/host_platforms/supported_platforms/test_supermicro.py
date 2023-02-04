import pytest

from walle import projects
from walle.host_platforms.supported_platforms.supermicro import (
    PlatformX8DTIF,
    PlatformX9DRWIF,
    PlatformX9DRW3F,
    PlatformX10DRWI,
)


def mock_project_supports_ipxe(mp, is_enabled):
    mp.function(projects.ipxe_support_enabled, return_value=is_enabled)


class TestPlatformX8DTIF:
    @pytest.fixture
    def platform(self, test):
        host = test.mock_host({"name": "mocked-supermicro-hostname", "platform": {"system": "X8DTI-F", "board": None}})

        return PlatformX8DTIF(host)

    def test_supports_ipxe(self, platform):
        assert not platform.ipxe_supported()


class TestPlatformX9DRWIF:
    @pytest.fixture
    def platform(self, test):
        host = test.mock_host(
            {
                "name": "mocked-supermicro-hostname",
                "project": "mock-project",
                "platform": {"system": "X9DRW-IF", "board": None},
            }
        )

        return PlatformX9DRWIF(host)

    @pytest.mark.parametrize("enabled_in_project", [True, False])
    def test_supports_ipxe(self, mp, platform, enabled_in_project):
        mock_project_supports_ipxe(mp, enabled_in_project)
        assert platform.ipxe_supported() == enabled_in_project


class TestPlatformX9DRW3F:
    @pytest.fixture
    def platform(self, test):
        host = test.mock_host(
            {
                "name": "mocked-supermicro-hostname",
                "project": "mock-project",
                "platform": {"system": "X9DRW-3F", "board": None},
            }
        )

        return PlatformX9DRW3F(host)

    @pytest.mark.parametrize("enabled_in_project", [True, False])
    def test_supports_ipxe(self, mp, platform, enabled_in_project):
        mock_project_supports_ipxe(mp, enabled_in_project)
        assert platform.ipxe_supported() == enabled_in_project


class TestPlatformX10DRWI:
    @pytest.fixture
    def platform(self, test):
        host = test.mock_host(
            {
                "name": "mocked-supermicro-hostname",
                "project": "mock-project",
                "platform": {"system": "X10DRW-I", "board": None},
            }
        )

        return PlatformX10DRWI(host)

    @pytest.mark.parametrize("enabled_in_project", [True, False])
    def test_supports_ipxe(self, mp, platform, enabled_in_project):
        mock_project_supports_ipxe(mp, enabled_in_project)
        assert platform.ipxe_supported() == enabled_in_project
