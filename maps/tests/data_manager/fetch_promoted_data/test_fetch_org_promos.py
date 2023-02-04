import pytest
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_promos_for_permalink(dm, factory):
    await factory.create_promotion(biz_id=123, promotion_id=100)
    await factory.create_promotion(
        biz_id=123,
        promotion_id=101,
        announcement="Купи чизбургер - получи бочка в подарок!",
        date_from="2020-11-13",
        date_to="2020-12-25",
        description="Будешь самый жирненький.",
        banner_img=None,
        link=None,
    )

    got = await dm.fetch_org_promos(biz_id=123)

    assert got == dict(
        promotion=[
            {
                "announcement": "Купи чизбургер - получи бочка в подарок!",
                "description": "Будешь самый жирненький.",
                "date_from": dt("2020-11-13 00:00:00"),
                "date_to": dt("2020-12-25 23:59:59"),
                "banner_img": None,
                "details_url": None,
            },
            {
                "announcement": "Купи 1 кружку кофе и вторую тоже купи",
                "description": "Самый лучший кофе в городе",
                "date_from": dt("2020-04-12 00:00:00"),
                "date_to": dt("2020-05-11 23:59:59"),
                "details_url": "http://promotion.link",
                "banner_img": "https://avatars.mds.yandex.net/2a0000016a0c63891/banner",
            },
        ]
    )


async def test_returns_empty_list_for_each_promo_type_if_nothing_found(dm):
    got = await dm.fetch_org_promos(biz_id=123)

    assert got == dict(promotion=[])


async def test_does_not_return_other_org_promos(dm, factory):
    await factory.create_promotion(biz_id=222)

    got = await dm.fetch_org_promos(biz_id=123)

    assert got == {"promotion": []}
