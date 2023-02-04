# coding: utf-8
import pytest

from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import Http
from awacs.wrappers.errors import ValidationError


def test_http():
    pb = modules_pb2.HttpModule()

    http = Http(pb)
    http.validate(chained_modules=True)

    config = http.to_config()
    assert config.table['maxlen'] == Http.DEFAULT_MAXLEN
    assert config.table['maxreq'] == Http.DEFAULT_MAXREQ
    assert config.table['keepalive']

    pb.keepalive.value = False
    http.validate(chained_modules=True)
    config = http.to_config()
    assert config.table['maxlen'] == Http.DEFAULT_MAXLEN
    assert config.table['maxreq'] == Http.DEFAULT_MAXREQ
    assert not config.table['keepalive']

    pb.keepalive_timeout = '0s'
    http.update_pb(pb)
    http.validate(chained_modules=True)

    pb.keepalive_timeout = '1000ms'
    http.update_pb(pb)
    with pytest.raises(ValidationError) as e:
        http.validate(chained_modules=True)
    e.match('keepalive_timeout: must be 0 if "keepalive" set to false')

    pb.keepalive.value = True

    pb.keepalive_drop_probability.value = 2
    http.update_pb(pb)
    with pytest.raises(ValidationError) as e:
        http.validate(chained_modules=True)
    e.match('keepalive_drop_probability: must be between or equal to 0 or 1')

    pb.keepalive_drop_probability.value = .5
    pb.keepalive_requests.value = -10
    with pytest.raises(ValidationError) as e:
        http.validate(chained_modules=True)
    e.match('keepalive_requests: must be non-negative')

    pb.keepalive_requests.value = 10
    http.update_pb(pb)
    http.validate(chained_modules=True)

    config = http.to_config()
    assert config.table['maxlen'] == Http.DEFAULT_MAXLEN
    assert config.table['maxreq'] == Http.DEFAULT_MAXREQ
    assert config.table['keepalive'] is True
    assert config.table['keepalive_timeout'] == '1000ms'
    assert config.table['keepalive_drop_probability'] == .5
    assert config.table['keepalive_requests'] == 10

    pb = modules_pb2.HttpModule()
    pb.keepalive_timeout = '0s'
    http = Http(pb)
    http.validate(chained_modules=True)
    config = http.to_config()
    assert config.table['keepalive'] is True
    assert config.table['keepalive_timeout'] == '0s'
