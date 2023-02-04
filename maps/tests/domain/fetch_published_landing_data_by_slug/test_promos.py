import pytest

from maps_adv.common.helpers import dt
from maps_adv.geosmb.landlord.server.lib.enums import LandingVersion

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.parametrize(
    "promos",
    [
        [],
        [
            {
                "announcement": "Купи 1 кружку кофе и вторую тоже купи",
                "description": "Самый лучший кофе в городе",
                "date_from": dt("2020-04-12 00:00:00"),
                "date_to": dt("2020-05-11 23:59:59"),
                "details_url": "http://promotion.link",
                "banner_img": "https://avatars.mds.yandex.net/2a0000016a0c63891/banner",
            },
            {
                "announcement": "Купи чизбургер - получи бочка в подарок!",
                "description": "Будешь самый жирненький.",
                "date_from": dt("2020-11-13 00:00:00"),
                "date_to": dt("2020-12-25 23:59:59"),
                "details_url": None,
                "banner_img": None,
            },
        ],
    ],
)
async def test_returns_promos_as_expected(promos, domain, dm):
    dm.fetch_org_promos.coro.return_value = {"promotion": promos}

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["promos"] == dict(promotion=promos)
