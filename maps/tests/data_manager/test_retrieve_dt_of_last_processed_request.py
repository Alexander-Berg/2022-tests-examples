import pytest
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_dt_of_last_processed_request(factory, dm):
    await factory.create_request(
        passport_uid=999,
        processed_at=dt("2020-01-01 11:00:00"),
        created_at=dt("2020-01-01 00:00:00"),
    )
    await factory.create_request(
        passport_uid=999,
        processed_at=dt("2020-02-02 12:00:00"),
        created_at=dt("2020-02-02 00:00:00"),
    )
    await factory.create_request(
        passport_uid=999, processed_at=None, created_at=dt("2020-03-03 00:00:00")
    )

    got = await dm.retrieve_dt_of_last_processed_request(passport_uid=999)

    assert got == dt("2020-02-02 12:00:00")


async def test_returns_nothing_if_no_processed_requests(factory, dm):
    await factory.create_request(passport_uid=999, processed_at=None)

    got = await dm.retrieve_dt_of_last_processed_request(passport_uid=999)

    assert got is None


async def test_returns_nothing_if_no_matched_requests(factory, dm):
    await factory.create_request(
        passport_uid=111, processed_at=dt("2020-01-01 11:00:00")
    )

    got = await dm.retrieve_dt_of_last_processed_request(passport_uid=999)

    assert got is None
