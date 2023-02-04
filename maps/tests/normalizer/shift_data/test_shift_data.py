from datetime import datetime

import pytest

from maps_adv.stat_tasks_starter.lib.normalizer.shift_data import DataShifter
from maps_adv.stat_tasks_starter.tests.tools import dt

pytestmark = [pytest.mark.asyncio]

expected_sample = [
    (
        datetime.utcfromtimestamp(1554894569),
        7726,
        "37b36bd03bf84fcfe8f95ab43191653c",
        4,
        "4CE92B30-6A33-457D-A7D4-1B8CBAD54597",
        "iOS",
        "1112",
        11476,
        55.718732876522175,
        37.40151579701865,
        "geoadv.bb.action.makeRoute",
    ),
    (
        datetime.utcfromtimestamp(1554894569),
        7728,
        "525ef1ea2bd9042d64ac32d3ea4adbcb",
        4,
        "4CE92B30-6A33-457D-A7D4-1B8CBAD54597",
        "iOS",
        "1112",
        11476,
        55.72769056007629,
        37.470415387480365,
        "geoadv.bb.action.makeRoute",
    ),
]


async def test_selected_data_looks_as_expected(ch_client):
    source = DataShifter(
        database="stat",
        table_from="source_sample",
        table_to="normalized_sample",
        host="localhost",
        port=9001,
    )
    await source(dt("2019-04-10 11:05:55"), dt("2019-04-10 11:11:55"))
    existing_data_in_db = ch_client.execute("SELECT * FROM stat.normalized_sample")

    assert set(existing_data_in_db) == set(expected_sample)
