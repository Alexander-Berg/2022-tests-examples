import copy

import pytest
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


def make_promotion_data(advert_id: int = 111) -> dict:
    return dict(
        advert_id=advert_id,
        coupon_id=11100,
        biz_id=1110011,
        name="Какая-то акция",
        date_from=dt("2020-03-16 18:00:00"),
        date_to=dt("2020-04-17 18:00:00"),
        description="Какое-то описание",
        announcement="Какой-то анонс",
        image_url="http://image.url/promo1",
        coupon_url="http://promotion.url/promo1",
    )


async def test_iterator_is_empty_if_there_are_no_data(domain, facade):
    facade.fetch_coupon_promotions.seq = [[]]

    iter_counts = 0
    async for _ in domain.iter_coupon_promotions_for_export():
        iter_counts += 1

    assert iter_counts == 0


async def test_returns_all_expected_columns(domain, facade):
    records = []
    async for orgs in domain.iter_coupon_promotions_for_export():
        records.extend(orgs)

    assert records == [
        {
            "advert_id": 111,
            "announcement": "Какой-то анонс",
            "biz_id": 1110011,
            "coupon_id": 11100,
            "coupon_url": "http://promotion.url/promo1",
            "date_from": "2020-03-16",
            "date_to": "2020-04-17",
            "description": "Какое-то описание",
            "image_url": "http://image.url/promo1",
            "name": "Какая-то акция",
        },
        {
            "advert_id": 222,
            "announcement": "Какой-то другой анонс",
            "coupon_id": 22200,
            "coupon_url": "http://promotion.url/promo2",
            "date_from": "2020-02-16",
            "date_to": "2020-03-17",
            "description": "Какое-то другое описание",
            "image_url": "http://image.url/promo2",
            "name": "Какая-то другая акция",
        },
    ]


@pytest.mark.parametrize("iter_size", [None, 0, 1, 2, 100])
@pytest.mark.parametrize(
    "facade_data_seq",
    (
        # 1 sequences
        [[make_promotion_data(advert_id=idx) for idx in range(1, 4)]],
        # several sequences
        [
            [make_promotion_data(advert_id=1), make_promotion_data(advert_id=2)],
            [make_promotion_data(advert_id=3)],
        ],
    ),
)
async def test_returns_all_records_eventually(
    domain, facade, iter_size, facade_data_seq
):
    facade.fetch_coupon_promotions.seq = copy.deepcopy(facade_data_seq)

    records = []
    async for orgs in domain.iter_coupon_promotions_for_export(iter_size=iter_size):
        records.extend(orgs)

    assert [record["advert_id"] for record in records] == [idx for idx in range(1, 4)]


@pytest.mark.parametrize(
    "facade_data_seq",
    (
        # 1 sequences
        [[make_promotion_data(advert_id=idx) for idx in range(1, 4)]],
        # several sequences
        [
            [make_promotion_data(advert_id=1), make_promotion_data(advert_id=2)],
            [make_promotion_data(advert_id=3)],
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
    facade.fetch_coupon_promotions.seq = copy.deepcopy(facade_data_seq)

    chunk_sizes = []
    async for data_chunk in domain.iter_coupon_promotions_for_export(
        iter_size=iter_size
    ):
        chunk_sizes.append(len(data_chunk))

    assert chunk_sizes == expected_chunk_sizes
