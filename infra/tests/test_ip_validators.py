import pytest

from awacs.lib import validators


@pytest.mark.parametrize('ip, result, descr', [
    (u'2a02:6b8:c08:a40c:0:696:7ef7', False, 'Too few components'),
    (u'0::5', True, 'Should be valid'),
    (u'2a02:6b8:c08:a40c:0:696:7ef7:1', True, 'Enough components'),
    (u'2a02:6b8:c08:a40c:0:696:7ef7:1:9', False, 'Too many components'),
    (u'2g::01', False, 'One of the components is not a valid hex number'),
    (u'0:1', False, 'Invalid format'),
    (u'::1', True, 'Correct format with skipped zeros')
])
def test_v6(ip, result, descr):
    assert validators.ipv6(ip) == result, descr


@pytest.mark.parametrize('ip, result, descr', [
    (u'127.0.0.1', True, 'Should be valid'),
    (u'127.0.0', False, 'Too short'),
    (u'127.0.0.0.1', False, 'Too long'),
    (u'300.0.0.1', False, 'One octet >  255'),
    (u'7f.0.0.0.1', False, 'One octet is not a decimal int'),

])
def test_v4(ip, result, descr):
    assert validators.ipv4(ip) == result, descr
