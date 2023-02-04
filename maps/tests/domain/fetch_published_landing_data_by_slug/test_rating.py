from datetime import datetime, timezone
from decimal import Decimal

import pytest

from maps_adv.geosmb.landlord.server.lib.enums import LandingVersion

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


# rating is fully tested in rating provider
async def test_returns_org_rating(domain):
    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["rating"] == {
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


# rating is fully tested in rating provider
async def test_does_not_return_rating_if_no_rating_for_org(domain, ugcdb_client):
    ugcdb_client.fetch_org_reviews.coro.return_value = None

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert "rating" not in result
