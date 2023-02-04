from datetime import datetime, timezone

import pytest
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


async def test_iterator_is_empty_if_there_are_no_data(domain, facade):
    facade.get_loyalty_items_list_for_snapshot.seq = [[]]

    iter_counts = 0
    async for _ in domain.iter_loyalty_items_for_export():
        iter_counts += 1

    assert iter_counts == 0


async def test_returns_all_expected_columns(domain, facade):
    facade.get_loyalty_items_list_for_snapshot.seq = [
        [
            dict(
                client_id=111,
                issued_at=datetime(2020, 1, 1, 11, 22, 33, 123456, tzinfo=timezone.utc),
                id=1,
                type="COUPON",
                data={"key1": "value1"},
            )
        ]
    ]

    records = []
    async for orgs in domain.iter_loyalty_items_for_export():
        records.extend(orgs)

    assert records == [
        dict(
            client_id=111,
            issued_at=1577877753123456,
            id=1,
            type="COUPON",
            data={"key1": "value1"},
        )
    ]


@pytest.mark.parametrize("iter_size", [None, 0, 1, 2, 100])
@pytest.mark.parametrize(
    "facade_data_seq",
    (
        # 1 sequences
        [
            [
                dict(
                    client_id=idx,
                    issued_at=dt("2020-01-01 00:00:00"),
                    id=1,
                    type="COUPON",
                    data={"key1": "value1"},
                )
                for idx in range(1, 4)
            ]
        ],
        # several sequences
        [
            [
                dict(
                    client_id=1,
                    issued_at=dt("2020-01-01 00:00:00"),
                    id=1,
                    type="COUPON",
                    data={"key1": "value1"},
                ),
                dict(
                    client_id=2,
                    issued_at=dt("2020-01-01 00:00:00"),
                    id=1,
                    type="COUPON",
                    data={"key1": "value1"},
                ),
            ],
            [
                dict(
                    client_id=3,
                    issued_at=dt("2020-01-01 00:00:00"),
                    id=1,
                    type="COUPON",
                    data={"key1": "value1"},
                )
            ],
        ],
    ),
)
async def test_returns_all_records_eventually(
    domain, facade, iter_size, facade_data_seq
):
    facade.get_loyalty_items_list_for_snapshot.seq = facade_data_seq.copy()

    records = []
    async for orgs in domain.iter_loyalty_items_for_export(iter_size=iter_size):
        records.extend(orgs)

    assert [record["client_id"] for record in records] == [idx for idx in range(1, 4)]


@pytest.mark.parametrize(
    "facade_data_seq",
    (
        # 1 sequences
        [
            [
                dict(
                    client_id=idx,
                    issued_at=dt("2020-01-01 00:00:00"),
                    id=1,
                    type="COUPON",
                    data={"key1": "value1"},
                )
                for idx in range(1, 4)
            ]
        ],
        # several sequences
        [
            [
                dict(
                    client_id=1,
                    issued_at=dt("2020-01-01 00:00:00"),
                    id=1,
                    type="COUPON",
                    data={"key1": "value1"},
                ),
                dict(
                    client_id=2,
                    issued_at=dt("2020-01-01 00:00:00"),
                    id=1,
                    type="COUPON",
                    data={"key1": "value1"},
                ),
            ],
            [
                dict(
                    client_id=3,
                    issued_at=dt("2020-01-01 00:00:00"),
                    id=1,
                    type="COUPON",
                    data={"key1": "value1"},
                )
            ],
        ],
    ),
)
@pytest.mark.parametrize(
    "iter_size, expected_chunk_sizes",
    [(None, [3]), (0, [3]), (1, [1, 1, 1]), (2, [2, 1]), (100, [3])],
)
async def test_chunks_data_by_requested_size(
    domain, facade, facade_data_seq, iter_size, expected_chunk_sizes
):
    facade.get_loyalty_items_list_for_snapshot.seq = facade_data_seq

    chunk_sizes = []
    async for data_chunk in domain.iter_loyalty_items_for_export(iter_size=iter_size):
        chunk_sizes.append(len(data_chunk))

    assert chunk_sizes == expected_chunk_sizes
