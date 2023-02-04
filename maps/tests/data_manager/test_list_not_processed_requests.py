import pytest
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_not_processed_requests(factory, dm):
    record_id = await factory.create_request(passport_uid=111)
    await factory.create_request(
        passport_uid=222, processed_at=dt("2020-01-01 00:00:00")
    )

    got = await dm.list_not_processed_requests()

    assert got == [{"id": record_id, "passport_uid": 111}]


async def test_returns_nothing_if_no_data(factory, dm):
    await factory.create_request(processed_at=dt("2020-01-01 00:00:00"))

    got = await dm.list_not_processed_requests()

    assert got == []


async def test_returns_records_ordered_by_created_at(factory, dm):
    id_1 = await factory.create_request(created_at=dt("2021-01-01 00:00:00"))
    id_2 = await factory.create_request(created_at=dt("2020-01-01 00:00:00"))
    id_3 = await factory.create_request(created_at=dt("2023-01-01 00:00:00"))

    got = await dm.list_not_processed_requests()

    assert [record["id"] for record in got] == [id_2, id_1, id_3]
