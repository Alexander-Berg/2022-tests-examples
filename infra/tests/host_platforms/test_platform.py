import pytest

import walle.host_platforms.platform as p
import walle.projects


@pytest.fixture
def platform(test):
    return p.Platform(test.mock_host({"inv": 0, "name": "hostname-f"}))


@pytest.fixture
def platform_with_ipxe_project(mp, test, platform):
    mp.function(walle.projects.ipxe_support_enabled, return_value=True)
    return platform


@pytest.fixture
def platform_without_ipxe_project(mp, test, platform):
    mp.function(walle.projects.ipxe_support_enabled, return_value=False)
    return platform


class TestPlatform:
    def test_dummy_platform(self, platform):
        assert platform.fqdn == "hostname-f"

    def test_str(self, platform):
        assert str(platform) == "Platform({}/{}: {})".format(platform.board, platform.system, platform.fqdn)

    def test_get_current_post_code(self, platform):
        assert platform.get_current_post_code(None) is None

    def test_get_post_problem_for(self, platform):
        assert platform.get_post_problem_for_code(0xBA) is None

    def test_provides_post_code(self, platform):
        assert platform.provides_post_code() is False

    def test_ipxe_supported_project_with_ipxe(self, platform_with_ipxe_project):
        assert platform_with_ipxe_project.ipxe_supported() is True

    def test_ipxe_supported_project_without_ipxe(self, platform_without_ipxe_project):
        assert platform_without_ipxe_project.ipxe_supported() is False
