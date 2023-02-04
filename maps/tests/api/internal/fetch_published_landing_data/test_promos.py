import pytest
from google.protobuf import timestamp_pb2

from maps_adv.geosmb.landlord.proto import (
    common_pb2,
    organization_details_pb2,
    promo_pb2,
)

pytestmark = [pytest.mark.asyncio]

URL = "/v1/fetch_landing_data/"


async def test_returns_promos_as_expected(api, factory):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )
    await factory.create_promotion(biz_id=22, promotion_id=100)
    await factory.create_promotion(
        biz_id=22,
        promotion_id=101,
        announcement="Купи чизбургер - получи бочка в подарок!",
        date_from="2020-11-13",
        date_to="2020-12-25",
        description="Будешь самый жирненький.",
        banner_img=None,
        link=None,
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

    assert got.promos == promo_pb2.Promos(
        promotion=[
            promo_pb2.Promotion(
                announcement="Купи чизбургер - получи бочка в подарок!",
                description="Будешь самый жирненький.",
                date_from=timestamp_pb2.Timestamp(seconds=1605225600),
                date_to=timestamp_pb2.Timestamp(seconds=1608940799),
            ),
            promo_pb2.Promotion(
                announcement="Купи 1 кружку кофе и вторую тоже купи",
                description="Самый лучший кофе в городе",
                date_from=timestamp_pb2.Timestamp(seconds=1586649600),
                date_to=timestamp_pb2.Timestamp(seconds=1589241599),
                details_url="http://promotion.link",
                banner_img=common_pb2.ImageTemplate(
                    template_url="https://avatars.mds.yandex.net/2a0000016a0c63891/banner"  # noqa
                ),
            ),
        ]
    )


async def test_returns_promos_as_expected_if_no_promos(api, factory):
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

    assert got.promos == promo_pb2.Promos(promotion=[])
