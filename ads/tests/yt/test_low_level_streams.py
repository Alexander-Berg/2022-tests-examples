import pytest
import brotli
from ads_pytorch.yt.low_level_streams import (
    StringJoinStream,
    BrotliDecompressorStream,
    unravel_stream,
)
from ads_pytorch.cpp_lib import libcpp_lib


async def _rfgecnf():
    yield b'1'
    yield b'2'
    yield b'3'


@pytest.mark.parametrize(
    'input_stream',
    [
        b'123',
        libcpp_lib.StringHandle(b'123'),
        [b'1', b'2', b'3'],
        (b'1', b'2', b'3'),
        iter([b'1', b'2', b'3']),
        _rfgecnf(),
        _rfgecnf().__aiter__()
    ],
    ids=[
        'bytes',
        'StringHandle',
        'list',
        'generator',
        'list_iterator',
        'async_generator',
        'async_iterator'
    ]
)
@pytest.mark.asyncio
async def test_unravel_stream(input_stream):
    string_stream = StringJoinStream()
    await unravel_stream(input_stream, string_stream)
    res = await string_stream.flush()
    assert bytes(res) == b'123'


@pytest.fixture(params=[StringJoinStream, BrotliDecompressorStream])
def target_stream(request):
    cls = request.param
    return cls()


@pytest.mark.parametrize('input_stream', [b'', []])
@pytest.mark.asyncio
async def test_empty_stream(input_stream, target_stream):
    await unravel_stream(input_stream, target_stream)
    res = await target_stream.flush()
    assert bytes(res) == b''


@pytest.mark.asyncio
async def test_string_joiner_empty_strings():
    target_stream = StringJoinStream()
    await unravel_stream([b'', b'123', b'', b'456', b''], target_stream)
    res = await target_stream.flush()
    assert bytes(res) == b'123456'


@pytest.mark.asyncio
async def test_decompress_stream():
    string_stream = BrotliDecompressorStream()
    base_string = b'qjoidjoiewfju908243u9r03ujfopeij2hq87fy92q8f4j0972h3c'
    string = brotli.compress(b'qjoidjoiewfju908243u9r03ujfopeij2hq87fy92q8f4j0972h3c')
    data = [string[:len(string) // 3], string[len(string) // 3: 2 * (len(string) // 3)], string[2 * (len(string) // 3):]]
    await unravel_stream(data, string_stream)
    s = await string_stream.flush()
    assert bytes(s) == base_string
