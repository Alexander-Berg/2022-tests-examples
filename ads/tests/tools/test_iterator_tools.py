import pytest
from ads_pytorch.tools.iterator_tools import (
    async_batch_iterator,
    chunk_iterator,
    samples_pack_with_tail_hack_iterator
)


async def async_range(count: int):
    for i in range(count):
        yield i


@pytest.mark.asyncio
@pytest.mark.parametrize('batch_size', [1, 10])
async def test_empty_iterator(batch_size):
    res = []
    async for x in async_batch_iterator(async_range(0), batch_size=batch_size):
        res.append(x)
    assert len(res) == 0


@pytest.mark.asyncio
async def test_batch_size_one():
    res = []
    async for x in async_batch_iterator(async_range(100), batch_size=1):
        res.append(x)
    assert res == [[i] for i in range(100)]


@pytest.mark.asyncio
async def test_even_iterator():
    batch_size = 3
    res = []
    async for x in async_batch_iterator(async_range(9), batch_size=batch_size):
        res.append(x)
    assert res == [[0, 1, 2], [3, 4, 5], [6, 7, 8]]


@pytest.mark.asyncio
async def test_odd_iterator():
    batch_size = 3
    res = []
    async for x in async_batch_iterator(async_range(10), batch_size=batch_size):
        res.append(x)
    assert res == [[0, 1, 2], [3, 4, 5], [6, 7, 8], [9]]


@pytest.mark.asyncio
async def test_batch_size_bigger_than_sequence():
    res = []
    async for x in async_batch_iterator(async_range(10), batch_size=20):
        res.append(x)
    assert res == [list(range(10))]


@pytest.mark.parametrize('chunk_size', [-1, 0], ids=['Negative', 'Zero'])
def test_chunk_iterator_negative_chunk(chunk_size):
    with pytest.raises(ValueError):
        list(chunk_iterator(start_offset=1, end_offset=2, chunk_size=chunk_size))


def test_chunk_iterator_end_less_start():
    with pytest.raises(ValueError):
        list(chunk_iterator(start_offset=1, end_offset=0, chunk_size=1))


def test_chunk_iterator_end_is_start():
    assert list(chunk_iterator(start_offset=1, end_offset=1, chunk_size=1)) == []


def test_chunk_iterator():
    res = list(chunk_iterator(start_offset=0, end_offset=8, chunk_size=3))
    assert res == [(0, 3), (3, 6), (6, 8)]


def test_chunk_iterator_big():
    res = list(chunk_iterator(start_offset=0, end_offset=8, chunk_size=300000))
    assert res == [(0, 8)]


@pytest.mark.asyncio
async def test_tail_one():
    yielded = [x async for x in samples_pack_with_tail_hack_iterator(async_range(5), pack_size=1)]
    assert yielded == list(range(5))


@pytest.mark.asyncio
async def test_tail_even():
    yielded = [x async for x in samples_pack_with_tail_hack_iterator(async_range(15), pack_size=3)]
    assert yielded == list(range(15))


@pytest.mark.asyncio
async def test_tail_odd_one():
    yielded = [x async for x in samples_pack_with_tail_hack_iterator(async_range(16), pack_size=3)]
    assert yielded == list(range(15)) + [15, 15, 15]


@pytest.mark.asyncio
async def test_tail_odd_two():
    yielded = [x async for x in samples_pack_with_tail_hack_iterator(async_range(17), pack_size=3)]
    assert yielded == list(range(15)) + [15, 16, 15]


@pytest.mark.asyncio
async def test_tail_many_ranges():
    for range_size in range(1, 25):
        for pack_size in range(1, 17):
            lst = [x async for x in samples_pack_with_tail_hack_iterator(async_range(range_size), pack_size=pack_size)]
            assert len(lst) % pack_size == 0
