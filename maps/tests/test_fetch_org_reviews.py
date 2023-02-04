import json
import re
from datetime import datetime, timezone
from decimal import Decimal

import pytest

from maps_adv.common.ugcdb_client import OrderBy

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def response():
    return {
        "Params": {
            "Offset": 0,
            "Limit": 10,
            "Count": 1109,
            "FilterKeyPhrase": "",
            "FilterMyReview": False,
        },
        "TotalCount": 1167,
        "Reviews": [
            {
                "Type": "YANDEX",
                "ReviewId": "hh4UX2ILpbErROa6zLpOWjdGKISytPUq6",
                "Author": {
                    "Name": "Вася.Пупкин",
                    "ProfileUrl": "https://reviews.yandex.ru/user/t8kxw3agk2nxgzby4dxwh8hp00",
                    "PublicId": "t8kxw3agk2nxgzby4dxwh8hp00",
                    "AvatarUrl": "https://avatars.mds.yandex.net/get-yapic/43473/06aL2MjxoTMf2FvYN4V5jqHY8-1/{size}",
                    "ProfessionLevel": "Знаток города 8 уровня",
                },
                "IsAnonymous": False,
                "Text": "Очень вкусная старорусская кухня",
                "Snippet": "",
                "Rating": 5,
                "LikeCount": 0,
                "DislikeCount": 0,
                "SkipCount": 0,
                "UserReaction": "NONE",
                "Photos": [],
                "OrgId": "1018907821",
                "KeyPhrases": [
                    {
                        "KeyPhrase": "национальная кухня",
                        "Fragment": [
                            {"Position": 74, "Size": 7, "Text": "блюдо"},
                            {"Position": 664, "Size": 13, "Text": "горячие блюда"},
                        ],
                    },
                    {
                        "KeyPhrase": "дорого",
                        "Fragment": [{"Position": 63, "Size": 6, "Text": "дорого"}],
                    },
                ],
                "CommentCount": 0,
                "UpdatedTime": "2020-09-27T11:03:12.238Z",
            },
            {
                "Type": "YANDEX",
                "ReviewId": "3H-6R14PDtcoFHgChIFopdS_xKMEmWCp",
                "Author": {
                    "Name": "Vasily P.",
                    "ProfileUrl": "https://reviews.yandex.ru/user/9et7ftckdf0kp39ptbp07hdhr8",
                    "PublicId": "9et7ftckdf0kp39ptbp07hdhr8",
                    "AvatarUrl": "https://avatars.mds.yandex.net/get-yapic/61207/QmSDVrMsGTKGjBJV8wWMHheBNHA-1/{size}",
                    "ProfessionLevel": "Знаток города 4 уровня",
                },
                "IsAnonymous": False,
                "Text": "Очень красивый интерьер зала Библиотека.",
                "Snippet": "",
                "Rating": 4,
                "LikeCount": 0,
                "DislikeCount": 0,
                "SkipCount": 0,
                "UserReaction": "NONE",
                "Photos": [],
                "OrgId": "1018907821",
                "KeyPhrases": [
                    {
                        "KeyPhrase": "национальная кухня",
                        "Fragment": [{"Position": 341, "Size": 5, "Text": "блюдо"}],
                    },
                    {
                        "KeyPhrase": "интерьер",
                        "Fragment": [
                            {"Position": 6, "Size": 17, "Text": "красивый интерьер"}
                        ],
                    },
                ],
                "CommentCount": 0,
                "UpdatedTime": "2020-09-22T17:10:01.691Z",
            },
        ],
        "Rating": {"Value": 4.6, "Count": 3420},
        "MyReview": {
            "Type": "YANDEX",
            "ReviewId": "",
            "IsAnonymous": False,
            "Text": "",
            "Snippet": "",
            "Rating": 0,
            "LikeCount": 0,
            "DislikeCount": 0,
            "SkipCount": 0,
            "UserReaction": "NONE",
            "Photos": [],
            "OrgId": "",
            "KeyPhrases": [],
            "CommentCount": 0,
        },
        "KeyPhrases": [],
    }


@pytest.mark.parametrize("limit", [10, 15])
@pytest.mark.parametrize(
    ("order_by", "expected_ranking"),
    [
        (OrderBy.BY_TIME, "by_time"),
        (OrderBy.BY_RELEVANCE_ORG, "by_relevance_org"),
        (OrderBy.BY_LIKES_COUNT_DESC, "by_likes_count_desc"),
        (OrderBy.BY_RATING_ASC, "by_rating_asc"),
        (OrderBy.BY_RATING_DESC, "by_rating_desc"),
    ],
)
async def test_makes_valid_request(
    client, aresponses, response, limit, order_by, expected_ranking
):
    path, query = None, None

    async def handler(request):
        nonlocal path, query
        path = request.path
        query = dict(request.query)
        return aresponses.Response(
            body=json.dumps(response), content_type="application/json"
        )

    aresponses.add(
        "ugcdb.test", re.compile(r"/v1/orgs/\d+/get-reviews"), "GET", handler
    )

    await client.fetch_org_reviews(permalink=12345, limit=limit, order_by=order_by)

    assert path == "/v1/orgs/12345/get-reviews"
    assert query == {
        "limit": str(limit),
        "ranking": expected_ranking,
    }


async def test_default_params(client, aresponses, response):
    path, query = None, None

    async def handler(request):
        nonlocal path, query
        path = request.path
        query = dict(request.query)
        return aresponses.Response(
            body=json.dumps(response), content_type="application/json"
        )

    aresponses.add(
        "ugcdb.test", re.compile(r"/v1/orgs/\d+/get-reviews"), "GET", handler
    )

    await client.fetch_org_reviews(permalink=12345)

    assert query == {
        "limit": "10",
        "ranking": "by_time",
    }


async def test_return_data(client, aresponses, response):
    aresponses.add(
        "ugcdb.test",
        re.compile(r"/v1/orgs/\d+/get-reviews"),
        "GET",
        aresponses.Response(body=json.dumps(response), content_type="application/json"),
    )

    result = await client.fetch_org_reviews(
        permalink=12345, limit=15, order_by=OrderBy.BY_RATING_ASC
    )

    assert result == {
        "aggregated_rating": Decimal("4.6"),
        "review_count": 1167,
        "reviews": [
            {
                "username": "Вася.Пупкин",
                "avatar": "https://avatars.mds.yandex.net/get-yapic/43473/06aL2MjxoTMf2FvYN4V5jqHY8-1/%s",
                "updated_at": datetime(
                    2020, 9, 27, 11, 3, 12, 238000, tzinfo=timezone.utc
                ),
                "rating": 5,
                "text": "Очень вкусная старорусская кухня",
            },
            {
                "username": "Vasily P.",
                "avatar": "https://avatars.mds.yandex.net/get-yapic/61207/QmSDVrMsGTKGjBJV8wWMHheBNHA-1/%s",
                "updated_at": datetime(
                    2020, 9, 22, 17, 10, 1, 691000, tzinfo=timezone.utc
                ),
                "rating": 4,
                "text": "Очень красивый интерьер зала Библиотека.",
            },
        ],
    }


async def test_does_not_return_user_fields_for_anonymous_reviews(
    client, aresponses, response
):
    response["Reviews"] = [
        {
            "Type": "YANDEX",
            "ReviewId": "hh4UX2ILpbErROa6zLpOWjdGKISytPUq6",
            "Author": {},
            "IsAnonymous": True,
            "Text": "Очень вкусная старорусская кухня",
            "Snippet": "",
            "Rating": 5,
            "LikeCount": 0,
            "DislikeCount": 0,
            "SkipCount": 0,
            "UserReaction": "NONE",
            "Photos": [],
            "OrgId": "1018907821",
            "KeyPhrases": [
                {
                    "KeyPhrase": "национальная кухня",
                    "Fragment": [
                        {"Position": 74, "Size": 7, "Text": "блюдо"},
                        {"Position": 664, "Size": 13, "Text": "горячие блюда"},
                    ],
                },
                {
                    "KeyPhrase": "дорого",
                    "Fragment": [{"Position": 63, "Size": 6, "Text": "дорого"}],
                },
            ],
            "CommentCount": 0,
            "UpdatedTime": "2020-09-27T11:03:12.238Z",
        },
    ]
    aresponses.add(
        "ugcdb.test",
        re.compile(r"/v1/orgs/\d+/get-reviews"),
        "GET",
        aresponses.Response(body=json.dumps(response), content_type="application/json"),
    )

    result = await client.fetch_org_reviews(permalink=12345)

    assert result == {
        "aggregated_rating": Decimal("4.6"),
        "review_count": 1167,
        "reviews": [
            {
                "updated_at": datetime(
                    2020, 9, 27, 11, 3, 12, 238000, tzinfo=timezone.utc
                ),
                "rating": 5,
                "text": "Очень вкусная старорусская кухня",
            },
        ],
    }


async def test_does_not_return_avatar_if_avatar_is_empty(client, aresponses, response):
    response["Reviews"] = [
        {
            "Type": "YANDEX",
            "ReviewId": "hh4UX2ILpbErROa6zLpOWjdGKISytPUq6",
            "Author": {
                "Name": "Вася.Пупкин",
                "ProfileUrl": "https://reviews.yandex.ru/user/t8kxw3agk2nxgzby4dxwh8hp00",
                "PublicId": "t8kxw3agk2nxgzby4dxwh8hp00",
                "AvatarUrl": "",
                "ProfessionLevel": "Знаток города 8 уровня",
            },
            "IsAnonymous": False,
            "Text": "Очень вкусная старорусская кухня",
            "Snippet": "",
            "Rating": 5,
            "LikeCount": 0,
            "DislikeCount": 0,
            "SkipCount": 0,
            "UserReaction": "NONE",
            "Photos": [],
            "OrgId": "1018907821",
            "KeyPhrases": [
                {
                    "KeyPhrase": "национальная кухня",
                    "Fragment": [
                        {"Position": 74, "Size": 7, "Text": "блюдо"},
                        {"Position": 664, "Size": 13, "Text": "горячие блюда"},
                    ],
                },
                {
                    "KeyPhrase": "дорого",
                    "Fragment": [{"Position": 63, "Size": 6, "Text": "дорого"}],
                },
            ],
            "CommentCount": 0,
            "UpdatedTime": "2020-09-27T11:03:12.238Z",
        },
    ]
    aresponses.add(
        "ugcdb.test",
        re.compile(r"/v1/orgs/\d+/get-reviews"),
        "GET",
        aresponses.Response(body=json.dumps(response), content_type="application/json"),
    )

    result = await client.fetch_org_reviews(permalink=12345)

    assert result == {
        "aggregated_rating": Decimal("4.6"),
        "review_count": 1167,
        "reviews": [
            {
                "username": "Вася.Пупкин",
                "updated_at": datetime(
                    2020, 9, 27, 11, 3, 12, 238000, tzinfo=timezone.utc
                ),
                "rating": 5,
                "text": "Очень вкусная старорусская кухня",
            },
        ],
    }


async def test_returns_none_if_no_reviews(client, aresponses):
    aresponses.add(
        "ugcdb.test",
        re.compile(r"/v1/orgs/\d+/get-reviews"),
        "GET",
        aresponses.Response(
            body=json.dumps(
                {
                    "Params": {
                        "Offset": 0,
                        "Limit": 10,
                        "Count": 0,
                        "FilterKeyPhrase": "",
                        "FilterMyReview": False,
                    },
                    "TotalCount": 0,
                    "Reviews": [],
                    "KeyPhrases": [],
                }
            ),
            content_type="application/json",
        ),
    )

    result = await client.fetch_org_reviews(
        permalink=12345, limit=15, order_by=OrderBy.BY_RATING_ASC
    )

    assert result is None
