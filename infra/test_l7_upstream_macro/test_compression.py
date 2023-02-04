import pytest

from awacs.wrappers.l7upstreammacro import L7UpstreamMacro
from awtest.wrappers import get_exception_msg
from infra.awacs.proto import modules_pb2


@pytest.mark.parametrize('codecs', [
    [],
    ['x-deflate', 'br'],
])
def test_compression(zk_storage, cache, codecs):
    pb = modules_pb2.L7UpstreamMacro()
    pb.version = u'0.1.1'
    pb.id = u'default'
    pb.matcher.any = True
    pb.compression.codecs.extend(codecs)
    pb.static_response.status = 421
    pb.static_response.content = 'Bad Request'

    m = L7UpstreamMacro(pb)
    expanded = m.expand()
    compressor_pb = expanded[1].compressor
    assert compressor_pb.enable_compression.value
    assert codecs == compressor_pb.compression_codecs


@pytest.mark.parametrize('codecs, expected_error', [
    ([], None),
    (
        ['x-deflate', 'x-deflate', 'br'],
        'compression -> codecs: duplicate item "x-deflate"'
    ),
    (
        ['unknown_codec', 'x-deflate', 'br'],
        'compression -> codecs: unknown compression codec names: [unknown_codec]'
    ),
])
def test_compression_negative(codecs, expected_error):
    pb = modules_pb2.L7UpstreamMacro()
    pb.version = u'0.1.1'
    pb.id = u'default'
    pb.matcher.any = True
    pb.compression.codecs.extend(codecs)
    pb.static_response.status = 421
    pb.static_response.content = 'Bad Request'

    m = L7UpstreamMacro(pb)
    assert get_exception_msg(m.validate) == expected_error


def test_compression_not_set():
    pb = modules_pb2.L7UpstreamMacro()
    pb.version = u'0.1.1'
    pb.id = u'default'
    pb.matcher.any = True
    pb.static_response.status = 421
    pb.static_response.content = 'Bad Request'

    m = L7UpstreamMacro(pb)
    holder_pbs = m.expand()
    assert 2 == len(holder_pbs)
    for h_pb in holder_pbs:
        assert 'compressor' != h_pb.WhichOneof('module')
