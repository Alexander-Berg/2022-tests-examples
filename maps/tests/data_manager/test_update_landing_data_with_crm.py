from copy import deepcopy

import pytest

from maps_adv.geosmb.landlord.server.lib.enums import LandingVersion
from maps_adv.geosmb.landlord.server.lib.exceptions import NoDataForBizId
from smb.common.testing_utils import Any

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def landing_data():
    return {
        "name": "Кафе с едой",
        "categories": ["Общепит", "Ресторан"],
        "logo": "https://images.ru/logo/%s",
        "cover": "https://images.ru/cover/%s",
        "description": "Описание",
        "contacts": {
            "phone": "+7 (495) 739-70-00",
            "website": "http://cafe.ru",
            "vkontakte": "http://vk.com/cafe",
        },
        "extras": {
            "plain_extras": ["Wi-fi", "Оплата картой"],
            "extended_description": "Описание особенностей",
        },
        "preferences": {
            "personal_metrika_code": "888",
            "color_theme": {"theme": "LIGHT", "preset": "RED"},
            "cta_button": {
                "predefined": "BOOK_TABLE",
                "value": "https://maps.yandex.ru",
            },
        },
        "blocks_options": {
            "show_cover": True,
            "show_logo": False,
            "show_schedule": True,
            "show_photos": True,
            "show_map_and_address": False,
            "show_services": True,
            "show_reviews": True,
            "show_extras": True,
        },
    }


@pytest.mark.skip('not ready')
@pytest.mark.parametrize(
    ("version", "version_field"),
    [
        (LandingVersion.STABLE, "stable_version"),
        (LandingVersion.UNSTABLE, "unstable_version"),
    ],
)
async def test_saves_landing_data(factory, dm, version, version_field):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(biz_id=15, **{version_field: data_id})

    await dm.update_landing_data_with_crm(
        biz_id=15,
        version=version,
        name="Кафе с едой",
        categories=["Общепит", "Ресторан"],
        description="Описание",
        logo="https://images.ru/logo/%s",
        cover="https://images.ru/cover/%s",
        extras={
            "plain_extras": ["Wi-fi", "Оплата картой"],
            "extended_description": "Описание особенностей",
        },
        preferences={
            "personal_metrika_code": "888",
            "color_theme": {"theme": "LIGHT", "preset": "RED"},
            "cta_button": {
                "predefined": "BOOK_TABLE",
                "value": "https://maps.yandex.ru",
            },
        },
        blocks_options={
            "show_cover": True,
            "show_logo": False,
            "show_schedule": True,
            "show_photos": True,
            "show_map_and_address": False,
            "show_services": True,
            "show_reviews": True,
            "show_extras": True,
        },
    )

    assert await factory.list_all_landing_data() == [
        {
            "id": Any(int),
            "name": "Кафе с едой",
            "categories": ["Общепит", "Ресторан"],
            "description": "Описание",
            "logo": "https://images.ru/logo/%s",
            "cover": "https://images.ru/cover/%s",
            "contacts": {
                "phone": "+7 (495) 739-70-00",
                "website": "http://cafe.ru",
                "vkontakte": "http://vk.com/cafe",
            },
            "extras": {
                "plain_extras": ["Wi-fi", "Оплата картой"],
                "extended_description": "Описание особенностей",
            },
            "preferences": {
                "personal_metrika_code": "888",
                "color_theme": {"theme": "LIGHT", "preset": "RED"},
                "cta_button": {
                    "predefined": "BOOK_TABLE",
                    "value": "https://maps.yandex.ru",
                },
            },
            "blocks_options": {
                "show_cover": True,
                "show_logo": False,
                "show_schedule": True,
                "show_photos": True,
                "show_map_and_address": False,
                "show_services": True,
                "show_reviews": True,
                "show_extras": True,
            },
            "landing_type": "DEFAULT",
            "instagram": None,
            "schedule": None,
            "photos": None,
            "photo_settings": None,
            "chain_id": None,
            "is_updated_from_geosearch": False,
        }
    ]


@pytest.mark.skip('not ready')
@pytest.mark.parametrize("version", list(LandingVersion))
@pytest.mark.parametrize("is_published", [False, True])
async def test_returns_new_landing_data(factory, dm, landing_data, version, is_published):
    expected_landing_details = deepcopy(landing_data)
    expected_landing_details["version"] = version
    expected_landing_details["permalink"] = "12345"
    expected_landing_details["contacts"]["geo"] = dict(permalink="12345")
    expected_landing_details.update(landing_type="DEFAULT", instagram=None)

    await factory.insert_biz_state(biz_id=15, slug="cafe", permalink="12345", published=is_published)

    result = await dm.update_landing_data_with_crm(
        biz_id=15,
        version=version,
        name="Кафе с едой",
        categories=["Общепит", "Ресторан"],
        description="Описание",
        logo="https://images.ru/logo/%s",
        cover="https://images.ru/cover/%s",
        extras={
            "plain_extras": ["Wi-fi", "Оплата картой"],
            "extended_description": "Описание особенностей",
        },
        preferences={
            "personal_metrika_code": "888",
            "color_theme": {"theme": "LIGHT", "preset": "RED"},
            "cta_button": {
                "predefined": "BOOK_TABLE",
                "value": "https://maps.yandex.ru",
            },
        },
        blocks_options={
            "show_cover": True,
            "show_logo": False,
            "show_schedule": True,
            "show_photos": True,
            "show_map_and_address": False,
            "show_services": True,
            "show_reviews": True,
            "show_extras": True,
        },
    )

    assert result == {
        "slug": "cafe",
        "landing_details": expected_landing_details,
        "is_published": is_published,
    }


@pytest.mark.skip('not ready')
@pytest.mark.parametrize("version", list(LandingVersion))
async def test_raises_if_biz_id_is_unknown(factory, dm, landing_data, version):
    await factory.insert_biz_state(biz_id=22, slug="cafe")

    with pytest.raises(NoDataForBizId):
        await dm.update_landing_data_with_crm(
            biz_id=15,
            version=version,
            name="Кафе с едой",
            categories=["Общепит", "Ресторан"],
            description="Описание",
            logo="https://images.ru/logo/%s",
            cover="https://images.ru/cover/%s",
            extras=None,
            preferences=None,
            blocks_options=None,
        )
