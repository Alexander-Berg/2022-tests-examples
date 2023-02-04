from datetime import datetime

import pytest
from freezegun import freeze_time
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio, pytest.mark.freeze_time("2021-01-29 19:31:00")]


@pytest.fixture
def add_client(factory):
    async def _add_client(on_dt: datetime) -> int:
        with freeze_time(on_dt):
            return await factory.create_empty_client()

    return _add_client


async def test_returns_statistics(dm, add_client):
    await add_client(dt("2020-04-10 15:00:00"))
    await add_client(dt("2020-09-05 23:00:00"))

    got = await dm.segment_statistics(
        biz_id=123,
        segment_type=None,
        on_datetime=dt("2021-01-29 19:31:00"),
    )

    assert got == {
        dt("2020-01-31 00:00:00"): 0,
        dt("2020-02-29 00:00:00"): 0,
        dt("2020-03-31 00:00:00"): 0,
        dt("2020-04-30 00:00:00"): 1,
        dt("2020-05-31 00:00:00"): 1,
        dt("2020-06-30 00:00:00"): 1,
        dt("2020-07-31 00:00:00"): 1,
        dt("2020-08-31 00:00:00"): 1,
        dt("2020-09-30 00:00:00"): 2,
        dt("2020-10-31 00:00:00"): 2,
        dt("2020-11-30 00:00:00"): 2,
        dt("2020-12-31 00:00:00"): 2,
        dt("2021-01-31 00:00:00"): 2,
    }
