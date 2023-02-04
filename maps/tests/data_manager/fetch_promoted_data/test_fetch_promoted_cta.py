import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_promoted_cta_button(dm, factory):
    await factory.create_promoted_cta(biz_id=123)

    got = await dm.fetch_promoted_cta(biz_id=123)

    assert got == {
        "custom": "Перейти на сайт",
        "value": "http://promoted.cta//link",
    }


async def test_returns_none_if_nothing_found(dm):
    got = await dm.fetch_promoted_cta(biz_id=123)

    assert got is None


async def test_does_not_return_other_org_cta(dm, factory):
    await factory.create_promoted_cta(biz_id=222)

    got = await dm.fetch_promoted_cta(biz_id=123)

    assert got is None
