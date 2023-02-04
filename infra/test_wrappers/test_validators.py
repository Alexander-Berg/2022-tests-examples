# coding: utf-8
import pytest

from awacs.wrappers.errors import ValidationError
from awacs.wrappers.util import validate_request_line, timedelta_to_ms, validate_timedelta_range, validate_timedelta


def test_validate_request_line():
    with pytest.raises(ValidationError) as e:
        validate_request_line(r'GET /ping HTTP/0.9\nHost: beta.mobsearch.yandex.ru\r\n\r\n')
    e.match('http version "HTTP/0.9" is not valid')

    with pytest.raises(ValidationError) as e:
        validate_request_line(r'GET /ping HTTP\nHost: beta.mobsearch.yandex.ru\r\n\r\n')
    e.match(r'is not valid: Bad request version \(\'HTTP\'\)')

    with pytest.raises(ValidationError) as e:
        validate_request_line(r'123')
    e.match(r'must end with "\\n\\n" or "\\r\\n\\r\\n"')

    with pytest.raises(ValidationError) as e:
        validate_request_line(r'123\n\n')
    e.match(r'is not valid: Bad request syntax \(\'123\'\)')

    with pytest.raises(ValidationError) as e:
        validate_request_line(r'GE /ping HTTP/1.1\nHost: beta.mobsearch.yandex.ru\r\n\r\n')
    e.match('command "GE" is not allowed')

    with pytest.raises(ValidationError) as e:
        validate_request_line(r'GET HTTP/1.1\nHost: beta.mobsearch.yandex.ru\r\n\r\n')
    e.match('http version is missing')

    with pytest.raises(ValidationError) as e:
        validate_request_line(r'GET HTTP/1.1\nHost: beta.mobsearch.yandex.ru\r\n\r\n')
    e.match('http version is missing')

    with pytest.raises(ValidationError) as e:
        validate_request_line(r'Host: beta.mobsearch.yandex.ru\r\n\r\n')
    e.match(r'is not valid: Bad HTTP/0.9 request type \(\'Host:\'\)')

    with pytest.raises(ValidationError) as e:
        validate_request_line(r'GET /ping HTTP/1.1\r\n\r\n')
    e.match('header "Host" must be set for HTTP/1.1')

    validate_request_line(r'GET /ping HTTP/1.1\nHost: beta.mobsearch.yandex.ru\r\n\r\n')
    validate_request_line(r'GET /ping HTTP/1.1\nhost: beta.mobsearch.yandex.ru\n\n')


def test_timedelta_to_ms():
    assert timedelta_to_ms('100ms') == 100
    assert timedelta_to_ms('2ms') == 2
    assert timedelta_to_ms('2s') == 2000
    assert timedelta_to_ms('0002000ms') == 2000
    assert timedelta_to_ms('0000ms') == 0
    assert timedelta_to_ms('0000m') == 0
    assert timedelta_to_ms('0.000m') == 0
    assert timedelta_to_ms('0.5m') == 30000
    assert timedelta_to_ms('0.1s') == 100
    with pytest.raises(ValueError):
        timedelta_to_ms('')
    with pytest.raises(ValueError):
        timedelta_to_ms('ms')
    with pytest.raises(ValueError):
        timedelta_to_ms('-1ms')


def test_validate_timedelta():
    validate_timedelta('100ms')
    validate_timedelta('0.1ms')
    validate_timedelta('0.1s')
    validate_timedelta('1m')
    with pytest.raises(ValidationError):
        validate_timedelta('1h')


def test_validate_timedelta_range():
    validate_timedelta_range('1ms', min_='0ms', max_='30s')
    validate_timedelta_range('0ms', min_='0ms', max_='30s')
    validate_timedelta_range('30s', min_='0ms', max_='30s')
    validate_timedelta_range('30s', min_='30s', max_='30s')
    validate_timedelta_range('0.5m', min_='30s', max_='30s')
    for value in ('0ms', '30s', '31s', '31001ms', '10.5m'):
        with pytest.raises(ValidationError):
            validate_timedelta_range(value, min_='0ms', max_='30s', exclusive_min=True, exclusive_max=True)

    with pytest.raises(ValidationError) as e:
        validate_timedelta_range('0ms', min_='1ms', max_='30s', exclusive_min=True)
    assert e.match('must be greater than 1ms')

    with pytest.raises(ValidationError) as e:
        validate_timedelta_range('1ms', min_='1ms', max_='30s', exclusive_min=True)
    assert e.match('must be greater than 1ms')

    with pytest.raises(ValidationError) as e:
        validate_timedelta_range('0ms', min_='1ms', max_='30s')
    assert e.match('must be greater or equal to 1ms')

    with pytest.raises(ValidationError) as e:
        validate_timedelta_range('30001ms', min_='1ms', max_='30s')
    assert e.match('must be less or equal to 30s')

    with pytest.raises(ValidationError) as e:
        validate_timedelta_range('30001ms', min_='1ms', max_='30s')
    assert e.match('must be less or equal to 30s')
