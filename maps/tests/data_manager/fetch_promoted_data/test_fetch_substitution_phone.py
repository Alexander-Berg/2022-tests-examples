import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_substitution_phone(dm, factory):
    await factory.create_substitution_phone(biz_id=123)

    got = await dm.fetch_substitution_phone(biz_id=123)

    assert got == "+7 (800) 200-06-00"


async def test_returns_none_if_nothing_found(dm):
    got = await dm.fetch_substitution_phone(biz_id=123)

    assert got is None


async def test_does_not_return_other_org_sub_phone(dm, factory):
    await factory.create_substitution_phone(biz_id=222)

    got = await dm.fetch_substitution_phone(biz_id=123)

    assert got is None
