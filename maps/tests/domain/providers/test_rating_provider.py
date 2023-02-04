from datetime import datetime, timezone
from decimal import Decimal

import pytest
from smb.common.http_client import BaseHttpClientException

from maps_adv.common.ugcdb_client import OrderBy
from maps_adv.geosmb.landlord.server.lib.domain.rating_provider import RatingProvider

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def rating_provider(ugcdb_client):
    return RatingProvider(permalink=54321, ugcdb_client=ugcdb_client)


@pytest.mark.parametrize(
    ("reviews_min_rating", "expected_order_by"),
    [
        (None, OrderBy.BY_TIME),
        (4, OrderBy.BY_RATING_DESC),
    ],
)
async def test_uses_ugcbd_client(
    rating_provider, ugcdb_client, reviews_min_rating, expected_order_by
):
    await rating_provider.fetch_rating(reviews_min_rating=reviews_min_rating)

    ugcdb_client.fetch_org_reviews.assert_called_with(
        permalink=54321, limit=10, order_by=expected_order_by
    )


async def test_returns_rating(rating_provider):
    result = await rating_provider.fetch_rating()

    assert result == {
        "aggregated_rating": Decimal("4.6"),
        "review_count": 1167,
        "reviews": [
            {
                "username": "Вася.Пупкин",
                "userpic": "https://avatars.mds.yandex.net/get-yapic/43473/06aL2MjxoTMf2FvYN4V5jqHY8-1/%s",  # noqa
                "created_at": datetime(
                    2020, 9, 27, 11, 3, 12, 238000, tzinfo=timezone.utc
                ),
                "rating": Decimal(5),
                "text": "Очень вкусная старорусская кухня",
            },
            {
                "username": "Vasily P.",
                "userpic": "https://avatars.mds.yandex.net/get-yapic/61207/QmSDVrMsGTKGjBJV8wWMHheBNHA-1/%s",  # noqa
                "created_at": datetime(
                    2020, 9, 22, 17, 10, 1, 691000, tzinfo=timezone.utc
                ),
                "rating": Decimal(4),
                "text": "Очень красивый интерьер зала Библиотека.",
            },
        ],
    }


@pytest.mark.parametrize(
    ("reviews_min_rating", "expected_review_texts"),
    [
        (5, ["1", "2", "3"]),
        (4, ["1", "2", "3", "4", "5"]),
        (3, ["1", "2", "3", "4", "5", "6"]),
        (2, ["1", "2", "3", "4", "5", "6", "7"]),
        (1, ["1", "2", "3", "4", "5", "6", "7", "8"]),
        (None, ["1", "2", "3", "4", "5", "6", "7", "8", "9"]),
    ],
)
async def test_returns_only_reviews_with_requested_rating_or_higher(
    ugcdb_client, reviews_min_rating, expected_review_texts
):
    reviews = [
        {"rating": 5, "text": "1"},
        {"rating": 5, "text": "2"},
        {"rating": 5, "text": "3"},
        {"rating": 4, "text": "4"},
        {"rating": 4, "text": "5"},
        {"rating": 3, "text": "6"},
        {"rating": 2, "text": "7"},
        {"rating": 1, "text": "8"},
        {"rating": 0, "text": "9"},
    ]
    for review in reviews:
        review.update(
            {
                "username": "Вася.Пупкин",
                "avatar": "https://avatars.mds.yandex.net/get-yapic/43473/06aL2MjxoTMf2FvYN4V5jqHY8-1/%s",  # noqa
                "updated_at": datetime(
                    2020, 9, 27, 11, 3, 12, 238000, tzinfo=timezone.utc
                ),
            }
        )

    ugcdb_client.fetch_org_reviews.coro.return_value["reviews"] = reviews
    rating_provider = RatingProvider(permalink=54321, ugcdb_client=ugcdb_client)

    result = await rating_provider.fetch_rating(reviews_min_rating=reviews_min_rating)

    review_texts = list(review["text"] for review in result["reviews"])
    assert review_texts == expected_review_texts


@pytest.mark.parametrize("reviews_min_rating", [None, 4])
async def test_returns_none_if_no_reviews_found(
    rating_provider, ugcdb_client, reviews_min_rating
):
    ugcdb_client.fetch_org_reviews.coro.return_value = None

    result = await rating_provider.fetch_rating(reviews_min_rating=reviews_min_rating)

    assert result is None


async def test_ignores_anonymous_reviews(rating_provider, ugcdb_client):
    ugcdb_client.fetch_org_reviews.coro.return_value["reviews"] = [
        {
            "username": "Вася.Пупкин",
            "avatar": "https://avatars.mds.yandex.net/get-yapic/43473/06aL2MjxoTMf2FvYN4V5jqHY8-1/%s",  # noqa
            "updated_at": datetime(2020, 9, 27, 11, 3, 12, 238000, tzinfo=timezone.utc),
            "rating": 5,
            "text": "Очень вкусная старорусская кухня",
        },
        {
            "updated_at": datetime(2020, 9, 27, 11, 3, 12, 238000, tzinfo=timezone.utc),
            "rating": 5,
            "text": "Очень вкусная старорусская кухня",
        },
        {
            "username": "Vasily P.",
            "avatar": "https://avatars.mds.yandex.net/get-yapic/61207/QmSDVrMsGTKGjBJV8wWMHheBNHA-1/%s",  # noqa
            "updated_at": datetime(2020, 9, 22, 17, 10, 1, 691000, tzinfo=timezone.utc),
            "rating": 4,
            "text": "Очень красивый интерьер зала Библиотека.",
        },
    ]

    result = await rating_provider.fetch_rating()

    assert result["reviews"] == [
        {
            "username": "Вася.Пупкин",
            "userpic": "https://avatars.mds.yandex.net/get-yapic/43473/06aL2MjxoTMf2FvYN4V5jqHY8-1/%s",  # noqa
            "created_at": datetime(2020, 9, 27, 11, 3, 12, 238000, tzinfo=timezone.utc),
            "rating": Decimal(5),
            "text": "Очень вкусная старорусская кухня",
        },
        {
            "username": "Vasily P.",
            "userpic": "https://avatars.mds.yandex.net/get-yapic/61207/QmSDVrMsGTKGjBJV8wWMHheBNHA-1/%s",  # noqa
            "created_at": datetime(2020, 9, 22, 17, 10, 1, 691000, tzinfo=timezone.utc),
            "rating": Decimal(4),
            "text": "Очень красивый интерьер зала Библиотека.",
        },
    ]


async def test_does_not_return_userpic_if_no_avatar_for_author(
    rating_provider, ugcdb_client
):
    ugcdb_client.fetch_org_reviews.coro.return_value["reviews"] = [
        {
            "username": "Вася.Пупкин",
            "updated_at": datetime(2020, 9, 27, 11, 3, 12, 238000, tzinfo=timezone.utc),
            "rating": 5,
            "text": "Очень вкусная старорусская кухня",
        },
    ]

    result = await rating_provider.fetch_rating()

    assert result["reviews"] == [
        {
            "username": "Вася.Пупкин",
            "created_at": datetime(2020, 9, 27, 11, 3, 12, 238000, tzinfo=timezone.utc),
            "rating": Decimal(5),
            "text": "Очень вкусная старорусская кухня",
        },
    ]


async def test_returns_none_if_clients_returns_error(rating_provider, ugcdb_client):
    ugcdb_client.fetch_org_reviews.coro.side_effect = BaseHttpClientException

    try:
        result = await rating_provider.fetch_rating()
    except Exception:
        pytest.fail("Should not propagate exception from ugcdb_client client")

    assert result is None
