import re
import pytest

from smarttv.droideka.proxy.models import PlatformModel


@pytest.mark.parametrize('string, is_matched', [
    ('', True),
    ('7.1', True),
    ('7.1.1', True),
    ('7.1.11', True),
    ('7.12.11', True),
    ('72.12.11', True),
    ('72.1.11', True),
    ('72.1.1', True),
    ('7.1.1.1', False),
    ('7', False),
    ('7.', False),
    ('7.1.', False),
    ('7.1.1.', False),
])
def test_platform_version(string, is_matched):
    assert bool(re.compile(PlatformModel.PLATFORM_VERSION_PATTERN).match(string)) == is_matched


@pytest.mark.parametrize('string, is_matched', [
    ('', True),
    ('7.1', True),
    ('0.1', True),
    ('1.0', True),
    ('1.10', True),
    ('21.10', True),
    ('7.1.1', False),
    ('7.1.11', False),
    ('7.12.11', False),
    ('72.12.11', False),
    ('72.1.11', False),
    ('72.1.1', False),
    ('7.1.1.1', False),
    ('7', False),
    ('7.', False),
    ('7.1.', False),
    ('7.1.1.', False),
])
def test_app_version(string, is_matched):
    assert bool(re.compile(PlatformModel.APP_VERSION_PATTERN).match(string)) == is_matched
