import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_biz_ids(factory, dm):
    await factory.create_business(biz_id=123)
    await factory.create_business(biz_id=345)
    await factory.create_business(biz_id=567)

    result = await dm.list_biz_ids()

    assert result == [123, 345, 567]


async def test_returns_empty_list_if_no_data(dm):
    result = await dm.list_biz_ids()

    assert result == []
