import pytest
from google.protobuf import timestamp_pb2

from maps_adv.geosmb.landlord.proto import (
    common_pb2,
    organization_details_pb2,
    rating_pb2,
)

pytestmark = [pytest.mark.asyncio]

URL = "/external/fetch_landing_data/"


async def test_returns_org_rating_as_expected(api, factory):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.rating == rating_pb2.Rating(
        aggregated_rating=common_pb2.Decimal(value="4.6"),
        review_count=1167,
        reviews=[
            rating_pb2.UserReview(
                username="Вася.Пупкин",
                userpic=common_pb2.ImageTemplate(
                    template_url="https://avatars.mds.yandex.net/get-yapic/43473/06aL2MjxoTMf2FvYN4V5jqHY8-1/%s"  # noqa
                ),
                created_at=timestamp_pb2.Timestamp(seconds=1601204592, nanos=238000000),
                rating=common_pb2.Decimal(value="5"),
                text="Очень вкусная старорусская кухня",
            ),
            rating_pb2.UserReview(
                username="Vasily P.",
                userpic=common_pb2.ImageTemplate(
                    template_url="https://avatars.mds.yandex.net/get-yapic/61207/QmSDVrMsGTKGjBJV8wWMHheBNHA-1/%s"  # noqa
                ),
                created_at=timestamp_pb2.Timestamp(seconds=1600794601, nanos=691000000),
                rating=common_pb2.Decimal(value="4"),
                text="Очень красивый интерьер зала Библиотека.",
            ),
        ],
    )


# rating is fully tested in rating provider
async def test_does_not_return_rating_if_no_rating_for_org(api, factory, ugcdb_client):
    ugcdb_client.fetch_org_reviews.coro.return_value = None

    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert not got.HasField("rating")
