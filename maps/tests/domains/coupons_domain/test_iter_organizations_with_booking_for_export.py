import pytest

pytestmark = [pytest.mark.asyncio]


async def test_iterator_is_empty_if_there_are_no_data(domain, facade):
    facade.get_organizations_with_booking.seq = [[]]

    iter_counts = 0
    async for _ in domain.iter_organizations_with_booking_for_export():
        iter_counts += 1

    assert iter_counts == 0


async def test_returns_all_expected_columns(domain, facade):
    facade.get_organizations_with_booking.seq = [
        [dict(biz_id="biz_id_0", permalink="123", booking_url="url_0")]
    ]

    records = []
    async for orgs in domain.iter_organizations_with_booking_for_export():
        records.extend(orgs)

    assert records == [dict(permalink=123, booking_url="url_0")]


@pytest.mark.parametrize("iter_size", [None, 0, 1, 2, 100])
@pytest.mark.parametrize(
    "facade_data_seq",
    (
        # 1 sequences
        [
            [
                dict(
                    biz_id=f"biz_id_{idx}",
                    permalink=f"{idx}",
                    booking_url=f"url_{idx}",
                )
                for idx in range(3)
            ]
        ],
        # several sequences
        [
            [
                dict(biz_id="biz_id_0", permalink="0", booking_url="url_0"),
                dict(biz_id="biz_id_1", permalink="1", booking_url="url_1"),
            ],
            [dict(biz_id="biz_id_2", permalink="2", booking_url="url_2")],
        ],
    ),
)
async def test_returns_all_records_eventually(
    domain, facade, iter_size, facade_data_seq
):
    facade.get_organizations_with_booking.seq = facade_data_seq

    records = []
    async for orgs in domain.iter_organizations_with_booking_for_export(
        iter_size=iter_size
    ):
        records.extend(orgs)

    assert [record["permalink"] for record in records] == [idx for idx in range(3)]


@pytest.mark.parametrize(
    "facade_data_seq",
    (
        # 1 sequences
        [
            [
                dict(
                    biz_id=f"biz_id_{idx}",
                    permalink=f"{idx}",
                    booking_url=f"url_{idx}",
                )
                for idx in range(3)
            ]
        ],
        # several sequences
        [
            [
                dict(biz_id="biz_id_0", permalink="0", booking_url="url_0"),
                dict(biz_id="biz_id_1", permalink="1", booking_url="url_1"),
            ],
            [dict(biz_id="biz_id_2", permalink="2", booking_url="url_2")],
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
    facade.get_organizations_with_booking.seq = facade_data_seq

    chunk_sizes = []
    async for data_chunk in domain.iter_organizations_with_booking_for_export(
        iter_size=iter_size
    ):
        chunk_sizes.append(len(data_chunk))

    assert chunk_sizes == expected_chunk_sizes


async def test_raises_for_not_int_permalink(domain, facade):
    facade.get_organizations_with_booking.seq = [
        [dict(biz_id="biz_id_0", permalink="permalink_0", booking_url="url_0")]
    ]

    with pytest.raises(ValueError) as exc:
        async for _ in domain.iter_organizations_with_booking_for_export():
            pass

    assert exc.value.args == ("invalid literal for int() with base 10: 'permalink_0'",)
