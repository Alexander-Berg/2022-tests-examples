import pytest
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("processed_at", [None, dt("2020-01-01 00:00:00")])
async def test_returns_request_data(factory, dm, processed_at):
    request_id = await factory.create_request(
        passport_uid=999, processed_at=processed_at
    )

    got = await dm.retrieve_last_request(passport_uid=999)

    assert got == {"id": request_id, "processed_at": processed_at}


async def test_returns_last_matched_request(factory, dm):
    await factory.create_request(passport_uid=999, created_at=dt("2020-01-01 00:00:00"))
    id_2 = await factory.create_request(
        passport_uid=999, created_at=dt("2022-01-01 00:00:00")
    )
    await factory.create_request(passport_uid=999, created_at=dt("2021-01-01 00:00:00"))

    got = await dm.retrieve_last_request(passport_uid=999)

    assert got["id"] == id_2


async def test_returns_nothing_if_no_matches(factory, dm):
    await factory.create_request(passport_uid=111)

    got = await dm.retrieve_last_request(passport_uid=999)

    assert got is None
