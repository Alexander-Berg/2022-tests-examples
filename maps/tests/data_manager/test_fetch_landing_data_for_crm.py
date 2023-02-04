import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.landlord.server.lib.enums import LandingVersion
from maps_adv.geosmb.landlord.server.lib.exceptions import (
    NoDataForBizId,
    VersionDoesNotExist,
)

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    ("requested_version", "existing_version"),
    [
        (LandingVersion.STABLE, "stable_version"),
        (LandingVersion.UNSTABLE, "unstable_version"),
    ],
)
async def test_return_data(factory, dm, requested_version, existing_version):
    data_id = await factory.insert_landing_data(
        name="Кафе здесь",
        categories=["Кафе", "Ресторан"],
        description="Описание",
        logo="https://images.com/logo",
        cover="https://images.com/cover",
        contacts={
            "phone": "+7 (495) 739-70-00",
            "phones": ["+7 (495) 739-70-00"],
            "website": "http://cafe.ru",
            "vkontakte": "http://vk.com/cafe",
            "facebook": "http://facebook.com/cafe",
            "instagram": "http://instagram.com/cafe",
            "twitter": "http://twitter.com/cafe",
            "telegram": "https://t.me/cafe",
            "viber": "https://viber.click/cafe",
            "whatsapp": "https://wa.me/cafe",
        },
        extras={
            "plain_extras": ["Wi-fi", "Оплата картой"],
            "extended_description": "Описание особенностей",
        },
        preferences={
            "personal_metrika_code": "metrika_code",
            "color_theme": {"theme": "LIGHT", "preset": "RED"},
            "cta_button": {
                "predefined": "BOOK_TABLE",
                "value": "https://maps.yandex.ru",
            },
        },
        blocks_options={
            "show_cover": True,
            "show_logo": True,
            "show_schedule": False,
            "show_photos": True,
            "show_map_and_address": False,
            "show_services": True,
            "show_reviews": True,
            "show_extras": True,
        },
    )
    await factory.insert_biz_state(
        biz_id=15, slug="cafe", permalink="54321", **{existing_version: data_id}
    )

    result = await dm.fetch_landing_data_for_crm(biz_id=15, version=requested_version)

    assert result == {
        "slug": "cafe",
        "landing_details": {
            "name": "Кафе здесь",
            "categories": ["Кафе", "Ресторан"],
            "description": "Описание",
            "logo": "https://images.com/logo",
            "cover": "https://images.com/cover",
            "contacts": {
                "geo": {"permalink": "54321"},
                "phone": "+7 (495) 739-70-00",
                "phones": ["+7 (495) 739-70-00"],
                "website": "http://cafe.ru",
                "vkontakte": "http://vk.com/cafe",
                "facebook": "http://facebook.com/cafe",
                "instagram": "http://instagram.com/cafe",
                "twitter": "http://twitter.com/cafe",
                "telegram": "https://t.me/cafe",
                "viber": "https://viber.click/cafe",
                "whatsapp": "https://wa.me/cafe",
            },
            "extras": {
                "plain_extras": ["Wi-fi", "Оплата картой"],
                "extended_description": "Описание особенностей",
            },
            "preferences": {
                "personal_metrika_code": "metrika_code",
                "color_theme": {"theme": "LIGHT", "preset": "RED"},
                "cta_button": {
                    "predefined": "BOOK_TABLE",
                    "value": "https://maps.yandex.ru",
                },
            },
            "blocks_options": {
                "show_cover": True,
                "show_logo": True,
                "show_schedule": False,
                "show_photos": True,
                "show_map_and_address": False,
                "show_services": True,
                "show_reviews": True,
                "show_extras": True,
            },
            "permalink": "54321",
            "photos": [],
            "version": requested_version,
            "blocked": False,
            "blocking_data": None,
            "landing_type": "DEFAULT",
            "instagram": None,
            "is_updated_from_geosearch": False,
        },
        "is_published": Any(bool),
    }


@pytest.mark.parametrize(
    ("requested_version", "expected_name"),
    [
        (LandingVersion.STABLE, "Кафе стабильное"),
        (LandingVersion.UNSTABLE, "Кафе нестабильное"),
    ],
)
async def test_returns_requested_version(factory, dm, requested_version, expected_name):
    stable_data_id = await factory.insert_landing_data(name="Кафе стабильное")
    unstable_data_id = await factory.insert_landing_data(name="Кафе нестабильное")
    await factory.insert_biz_state(
        biz_id=15, stable_version=stable_data_id, unstable_version=unstable_data_id
    )

    result = await dm.fetch_landing_data_for_crm(biz_id=15, version=requested_version)

    assert result["landing_details"]["name"] == expected_name


@pytest.mark.parametrize(
    ("requested_version", "existing_version"),
    [
        (LandingVersion.STABLE, "stable_version"),
        (LandingVersion.UNSTABLE, "unstable_version"),
    ],
)
@pytest.mark.parametrize("is_published", [False, True])
async def test_returns_valid_published_status(
    factory, dm, requested_version, existing_version, is_published
):
    data_id = await factory.insert_landing_data(name="Кафе стабильное")
    await factory.insert_biz_state(
        biz_id=15, published=is_published, **{existing_version: data_id}
    )

    result = await dm.fetch_landing_data_for_crm(biz_id=15, version=requested_version)

    assert result["is_published"] == is_published


@pytest.mark.parametrize(
    ("requested_version", "existing_version"),
    [
        (LandingVersion.STABLE, "stable_version"),
        (LandingVersion.UNSTABLE, "unstable_version"),
    ],
)
@pytest.mark.parametrize(
    "blocked, blocking_data",
    [
        (
            False,
            None,
        ),
        (
            True,
            {
                "blocker_uid": 1234567,
                "blocking_description": "Test",
                "ticket_id": "TICKET1",
            },
        ),
    ],
)
async def test_returns_blocked(
    factory, dm, requested_version, existing_version, blocked, blocking_data
):
    data_id = await factory.insert_landing_data(name="Кафе стабильное")
    await factory.insert_biz_state(
        biz_id=15,
        blocked=blocked,
        blocking_data=blocking_data,
        **{existing_version: data_id},
    )

    result = await dm.fetch_landing_data_for_crm(biz_id=15, version=requested_version)

    landing_details = result["landing_details"]
    assert landing_details["blocked"] == blocked
    assert landing_details["blocking_data"] == blocking_data


@pytest.mark.parametrize(
    ("requested_version", "existing_version"),
    [
        (LandingVersion.STABLE, "stable_version"),
        (LandingVersion.UNSTABLE, "unstable_version"),
    ],
)
async def test_raises_if_biz_state_does_not_exist(
    factory, dm, requested_version, existing_version
):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=25, slug="cafe", **{existing_version: data_id}
    )

    with pytest.raises(NoDataForBizId):
        await dm.fetch_landing_data_for_crm(biz_id=15, version=requested_version)


@pytest.mark.parametrize(
    ("requested_version", "existing_version"),
    [
        (LandingVersion.STABLE, "unstable_version"),
        (LandingVersion.UNSTABLE, "stable_version"),
    ],
)
async def test_raises_if_version_does_not_exist(
    factory, dm, requested_version, existing_version
):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=15, slug="cafe", **{existing_version: data_id}
    )

    with pytest.raises(VersionDoesNotExist):
        await dm.fetch_landing_data_for_crm(biz_id=15, version=requested_version)
