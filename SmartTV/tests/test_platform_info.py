import pytest

from smarttv.droideka.utils import PlatformInfo, PlatformType


class TestPlatformInfo:
    platform_type = PlatformType.ANDROID

    @pytest.mark.parametrize('above_platform, below_platform', [
        (PlatformInfo(platform_type, '8', None, None, None), PlatformInfo(platform_type, '7.1.1', None, None, None)),
        (PlatformInfo(platform_type, '7.1.1', None, None, None), PlatformInfo(platform_type, '7.1.0', None, None, None)),
        (PlatformInfo(platform_type, '7.1.1', None, None, None), PlatformInfo(platform_type, '7.0.0', None, None, None)),
        (PlatformInfo(platform_type, '7.0.1', None, None, None), PlatformInfo(platform_type, '7.0.0', None, None, None)),
    ])
    def test_platform_info_strict_operators_platform_version_only(self, above_platform, below_platform):
        assert above_platform > below_platform
        assert below_platform < above_platform

    @pytest.mark.parametrize('above_platform, below_platform', [
        (PlatformInfo(platform_type, None, '3', None, None), PlatformInfo(platform_type, None, '1.2', None, None)),
        (PlatformInfo(platform_type, '1.2.680', None, None, None), PlatformInfo(platform_type, '1.2.679', None, None, None)),
        (PlatformInfo(platform_type, '1.2.6', None, None, None), PlatformInfo(platform_type, '1.2.0', None, None, None)),
        (PlatformInfo(platform_type, '1.2.6', None, None, None), PlatformInfo(platform_type, '1.0.0', None, None, None)),
        (PlatformInfo(platform_type, '1.3.7', None, None, None), PlatformInfo(platform_type, '1.2.8', None, None, None)),
    ])
    def test_platform_info_strict_operators_app_version_only(self, above_platform, below_platform):
        assert above_platform > below_platform
        assert below_platform < above_platform

    @pytest.mark.parametrize('client_platform, candidate_platform', [
        (PlatformInfo(platform_type, '8', None, None, None), PlatformInfo(platform_type, '8', None, None, None)),
        (PlatformInfo(platform_type, '7.1.1', None, None, None), PlatformInfo(platform_type, '7.1.1', None, None, None)),
        (PlatformInfo(platform_type, '7.0.0', None, None, None), PlatformInfo(platform_type, '7.0.0', None, None, None)),
        (PlatformInfo(platform_type, '7.0.1', None, None, None), PlatformInfo(platform_type, '7.0.1', None, None, None)),
    ])
    def test_platform_ge_operator_ge_and_equal_platform_versions_only(self, client_platform, candidate_platform):
        assert client_platform >= candidate_platform
        assert client_platform == candidate_platform
        assert client_platform <= candidate_platform

    @pytest.mark.parametrize('client_platform, candidate_platform', [
        (PlatformInfo(platform_type, None, '1', None, None), PlatformInfo(platform_type, None, '1', None, None)),
        (PlatformInfo(platform_type, None, '1.2', None, None), PlatformInfo(platform_type, None, '1.2', None, None)),
        (PlatformInfo(platform_type, None, '1.2.3', None, None), PlatformInfo(platform_type, None, '1.2.3', None, None)),
    ])
    def test_platform_ge_operator_ge_and_equal_app_versions_only(self, client_platform, candidate_platform):
        assert client_platform >= candidate_platform
        assert client_platform == candidate_platform
        assert client_platform <= candidate_platform

    @pytest.mark.parametrize('above_platform, below_platform', [
        (PlatformInfo(platform_type, '7.1.0', '3', None, None), PlatformInfo(platform_type, '7.0.0', '1.2', None, None)),
        (PlatformInfo(platform_type, '7.1.0', '3', None, None), PlatformInfo(platform_type, '7.1.0', '1.2', None, None)),
    ])
    def test_platform_info_strict_operators_mixed(self, above_platform, below_platform):
        assert above_platform > below_platform
        assert below_platform < above_platform

    @pytest.mark.parametrize('client_platform, candidate_platform', [
        (PlatformInfo(platform_type, None, '1', None, None), PlatformInfo(platform_type, '1', None, None, None)),
        (PlatformInfo(platform_type, None, '1', None, None), PlatformInfo(platform_type, None, None, None, None)),
        (PlatformInfo(platform_type, None, None, None, None), PlatformInfo(platform_type, None, None, None, None)),
    ])
    def test_platforms_not_comparable(self, client_platform, candidate_platform):
        with pytest.raises(PlatformInfo.NotComparable):
            client_platform > candidate_platform
        with pytest.raises(PlatformInfo.NotComparable):
            client_platform < candidate_platform
        with pytest.raises(PlatformInfo.NotComparable):
            candidate_platform < client_platform
        with pytest.raises(PlatformInfo.NotComparable):
            candidate_platform > client_platform


class TestPlatformInfoFeatures:

    @pytest.mark.parametrize('platform, expected_result', [
        ('yandexmodule_2', True),
        ('yandex_tv_hisi351_cvte', False),
        ('', False),
        (None, False),
    ])
    def test_is_module(self, platform, expected_result):
        p = PlatformInfo(quasar_platform=platform)
        assert p.is_module() is expected_result

    @pytest.mark.parametrize('version, expected_result', [
        ('2.95', False),
        ('2.96', True),  # у этих версий заголовки неправильные
        ('2.107', True),
        ('2.107.13', True),
        ('2.108', True),
        ('2.118', False),
        ('3.121', False),
        ('', False),
    ])
    def test_is_service_headers_supported(self, version, expected_result):
        p = PlatformInfo(app_version=version)
        assert p.is_service_headers_broken() is expected_result
