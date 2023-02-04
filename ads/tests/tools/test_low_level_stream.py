import pytest
from ads_pytorch.tools.low_level_stream import LowLevelStreamReader


class BufferedStream(LowLevelStreamReader):
    def __init__(self):
        super(BufferedStream, self).__init__()
        self.strings = []
        self._called_feed_eof = False

    async def _feed_data_impl(self, data):
        self.strings.append(data)

    def _feed_eof_impl(self):
        self._called_feed_eof = True

    async def _flush_impl(self):
        return "".join(self.strings)


@pytest.mark.asyncio
async def test_simple():
    stream = BufferedStream()
    await stream.feed_data("1")
    await stream.feed_data("2")
    stream.feed_eof()
    assert stream._called_feed_eof
    flushed = await stream.flush()
    assert flushed == "12"


@pytest.mark.asyncio
async def test_exception():
    stream = BufferedStream()
    await stream.feed_data("1")
    stream.set_exception(LookupError)
    with pytest.raises(LookupError):
        await stream.feed_data("2")
    with pytest.raises(LookupError):
        stream.feed_eof()
    with pytest.raises(LookupError):
        await stream.flush()


@pytest.mark.asyncio
async def test_double_flush():
    stream = BufferedStream()
    await stream.feed_data("1")
    await stream.feed_data("2")
    stream.feed_eof()
    flushed = await stream.flush()
    assert flushed == "12"
    with pytest.raises(RuntimeError):
        await stream.flush()


@pytest.mark.asyncio
async def test_double_feed_eof():
    stream = BufferedStream()
    await stream.feed_data("1")
    await stream.feed_data("2")
    stream.feed_eof()
    with pytest.raises(RuntimeError):
        stream.feed_eof()


@pytest.mark.asyncio
async def test_feed_data_after_feed_eof():
    stream = BufferedStream()
    await stream.feed_data("1")
    await stream.feed_data("2")
    stream.feed_eof()
    with pytest.raises(RuntimeError):
        await stream.feed_data("2")


@pytest.mark.asyncio
async def test_feed_after_flush():
    stream = BufferedStream()
    await stream.feed_data("1")
    await stream.feed_data("2")
    stream.feed_eof()
    flushed = await stream.flush()
    assert flushed == "12"
    with pytest.raises(RuntimeError):
        await stream.feed_data("2")
    with pytest.raises(RuntimeError):
        await stream.feed_eof()


# Error failing tests


class BufferedStreamFeedFailed(BufferedStream):
    def __init__(self):
        super(BufferedStreamFeedFailed, self).__init__()

    async def _feed_data_impl(self, data):
        raise LookupError


@pytest.mark.asyncio
async def test_feed_data_failed():
    stream = BufferedStreamFeedFailed()
    with pytest.raises(LookupError):
        await stream.feed_data("1")
    with pytest.raises(LookupError):
        stream.feed_eof()
    with pytest.raises(LookupError):
        await stream.flush()
    stream.set_exception(RuntimeError)
    with pytest.raises(RuntimeError):
        await stream.feed_data("1")


class BufferedStreamFeedEofFailed(BufferedStream):
    def __init__(self):
        super(BufferedStreamFeedEofFailed, self).__init__()

    def _feed_eof_impl(self):
        raise LookupError


@pytest.mark.asyncio
async def test_feed_eof_failed():
    stream = BufferedStreamFeedEofFailed()
    await stream.feed_data("1")
    with pytest.raises(LookupError):
        stream.feed_eof()
    with pytest.raises(LookupError):
        await stream.feed_data("1")
    with pytest.raises(LookupError):
        await stream.flush()
    stream.set_exception(RuntimeError)
    with pytest.raises(RuntimeError):
        await stream.feed_data("1")


class BufferedStreamFlushFailed(BufferedStream):
    def __init__(self):
        super(BufferedStreamFlushFailed, self).__init__()

    async def _flush_impl(self):
        raise LookupError


@pytest.mark.asyncio
async def test_flush_failed():
    stream = BufferedStreamFlushFailed()
    await stream.feed_data("1")
    stream.feed_eof()
    with pytest.raises(LookupError):
        await stream.flush()
    with pytest.raises(LookupError):
        await stream.flush()
