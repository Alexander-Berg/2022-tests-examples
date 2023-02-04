import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_data(factory, dm):
    await factory.create_goods_data(
        54321,
        {"categories": [{"name": "QQQ 1"}, {"name": "QQQ 2"}], "source_name": "SSS 1"},
    )

    result = await dm.fetch_goods_data_for_permalink(54321)

    assert result == {
        "categories": [{"name": "QQQ 1"}, {"name": "QQQ 2"}],
        "goods_available": True,
        "source_name": "SSS 1",
    }


async def test_returns_empty_if_no_row(dm):
    result = await dm.fetch_goods_data_for_permalink(78901)

    assert result == {"goods_available": False}
