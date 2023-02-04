# coding: utf-8
import pytest

from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import Http2, SslSni
from awacs.wrappers.errors import ValidationError


def test_http2():
    ssl_sni = SslSni(modules_pb2.SslSniModule())
    pb = modules_pb2.Http2Module()

    http2 = Http2(pb)
    http2.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        http2.validate(chained_modules=True)
    e.match('must be preceded by ssl_sni module')

    http2.validate(chained_modules=True, preceding_modules=[ssl_sni])

    config = http2.to_config()
    assert config.table['goaway_debug_data_enabled'] is False
    assert config.table['debug_log_enabled'] is False

    pb.debug_log_enabled = True
    http2.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        http2.validate(chained_modules=True, preceding_modules=[ssl_sni])
    e.match('debug_log_name.*is required')

    pb.debug_log_name = 'log'
    http2.update_pb(pb)

    http2.validate(chained_modules=True, preceding_modules=[ssl_sni])
