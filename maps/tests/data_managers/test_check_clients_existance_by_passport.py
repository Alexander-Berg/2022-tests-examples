import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_true_if_matched_by_passport(factory, dm, con):
    await factory.create_client(passport_uid=12345)

    got = await dm.check_clients_existence_by_passport(passport_uid=12345)

    assert got is True


async def test_returns_false_if_is_not_matched_by_passport(factory, dm, con):
    await factory.create_client(passport_uid=111)

    got = await dm.check_clients_existence_by_passport(passport_uid=222)

    assert got is False
