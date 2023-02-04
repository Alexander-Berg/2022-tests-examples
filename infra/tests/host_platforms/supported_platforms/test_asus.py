import pytest

from walle.host_platforms.supported_platforms.asus import (
    PlatformASUSZ9PGD16,
    PlatformASUSZ9PGD16G2,
    PlatformASUSKGPED16,
    PlatformASUSESC4000,
)


@pytest.fixture
def asus_platform_fdr(test):
    host = test.mock_host(
        {"name": "mocked-asus-hostname", "platform": {"system": "ESC4000-FDR G2S", "board": "Z9PG-D16"}}
    )

    return PlatformASUSZ9PGD16(host)


@pytest.fixture
def asus_platform_g2(test):
    host = test.mock_host(
        {"name": "mocked-asus-hostname", "platform": {"system": "ESC4000 G2", "board": "Z9PG-D16 Series"}}
    )

    return PlatformASUSZ9PGD16G2(host)


@pytest.fixture
def asus_platform_kgpe(test):
    host = test.mock_host({"name": "mocked-asus-hostname", "platform": {"system": "KGPE-D16", "board": None}})

    return PlatformASUSKGPED16(host)


@pytest.fixture
def asus_platform_esc4000(test):
    host = test.mock_host({"name": "mocked-asus-hostname", "platform": {"system": "ESC4000", "board": None}})

    return PlatformASUSESC4000(host)


class TestPlatformASUSFDR:
    def test_supports_ipxe(self, asus_platform_fdr):
        assert asus_platform_fdr.ipxe_supported() is False


class TestPlatformASUSG2:
    def test_supports_ipxe(self, asus_platform_g2):
        assert asus_platform_g2.ipxe_supported() is False


class TestPlatformASUSKGPE:
    def test_supports_ipxe(self, asus_platform_kgpe):
        assert not asus_platform_kgpe.ipxe_supported()


class TestPlatformASUSESC4000:
    def test_supports_ipxe(self, asus_platform_esc4000):
        assert not asus_platform_esc4000.ipxe_supported()
