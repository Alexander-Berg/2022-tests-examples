import pytest

from maps_adv.common.helpers import AsyncIterator

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("data", [[], [1, 2, 3], [[1, 2], [3, 4]]])
async def test_iterates_over_all_data(data):
    iterator = AsyncIterator(data)

    iterated_data = []
    async for item in iterator():
        iterated_data.append(item)

    assert iterated_data == data


async def test_raises_on_exception_data_item():
    iterator = AsyncIterator([1, Exception("Data exception"), 2])

    with pytest.raises(Exception, match="Data exception"):
        iterated_data = []
        async for item in iterator():
            iterated_data.append(item)

    assert iterated_data == [1]
