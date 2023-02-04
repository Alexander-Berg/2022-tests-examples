from copy import deepcopy
from datetime import timedelta

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
        "photos": [],
        "contacts": {
            "phone": "+7 (495) 739-70-00",
            "phones": ["+7 (495) 739-70-00"],
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


@pytest.mark.parametrize("version", list(LandingVersion))
@pytest.mark.parametrize(
    ("phone", "phones", "expected_phone"),
    [
        ("+7 (495) 739-70-00", ["+7 (495) 739-70-00"], "+7 (495) 739-70-00"),
        ("+7 (495) 739-70-01", ["+7 (495) 739-70-00"], "+7 (495) 739-70-00"),
        ("+7 (495) 739-70-01", ["+7 (495) 739-70-00", "+7 (495) 739-70-01"], "+7 (495) 739-70-01"),
        ("+7 (495) 739-70-01", [], None)
    ]
)
async def test_saves_landing_data(factory, dm, landing_data, version, phone, phones, expected_phone):
    await factory.insert_biz_state(biz_id=15)
    landing_data = deepcopy(landing_data)
    landing_data["contacts"]["phone"] = phone
    landing_data["contacts"]["phones"] = phones

    await dm.save_landing_data_for_biz_id(
        biz_id=15, landing_data=landing_data, version=version
    )

    expected_contacts = {
        "phone": expected_phone,
        "phones": phones,
        "website": "http://cafe.ru",
        "vkontakte": "http://vk.com/cafe",
    }

    assert await factory.list_all_landing_data() == [
        {
            "id": Any(int),
            "name": "Кафе с едой",
            "categories": ["Общепит", "Ресторан"],
            "description": "Описание",
            "logo": "https://images.ru/logo/%s",
            "cover": "https://images.ru/cover/%s",
            "contacts": expected_contacts,
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


@pytest.mark.parametrize("version", list(LandingVersion))
async def test_saves_minimal_landing_data(factory, dm, version):
    await factory.insert_biz_state(biz_id=15)

    await dm.save_landing_data_for_biz_id(
        biz_id=15,
        landing_data={
            "name": "Кафе с едой",
            "categories": ["Общепит", "Ресторан"],
            "contacts": {},
            "preferences": {},
            "blocks_options": {},
        },
        version=version,
    )

    assert await factory.list_all_landing_data() == [
        {
            "id": Any(int),
            "name": "Кафе с едой",
            "categories": ["Общепит", "Ресторан"],
            "description": None,
            "logo": None,
            "cover": None,
            "contacts": {},
            "extras": {},
            "preferences": {},
            "blocks_options": {},
            "landing_type": "DEFAULT",
            "instagram": None,
            "schedule": None,
            "photos": None,
            "photo_settings": None,
            "chain_id": None,
            "is_updated_from_geosearch": False,
        }
    ]


@pytest.mark.parametrize(
    ("version", "version_field"),
    [
        (LandingVersion.STABLE, "stable_version"),
        (LandingVersion.UNSTABLE, "unstable_version"),
    ],
)
async def test_updates_biz_state(factory, dm, version, version_field):
    await factory.insert_biz_state(biz_id=15, slug="cafe", permalink="12345")

    await dm.save_landing_data_for_biz_id(
        biz_id=15,
        landing_data={
            "name": "Кафе с едой",
            "categories": ["Общепит", "Ресторан"],
            "contacts": {},
            "extras": {},
            "preferences": {},
            "blocks_options": {},
        },
        version=version,
    )

    data_id = (await factory.list_all_landing_data())[0]["id"]
    biz_state = await factory.fetch_biz_state(biz_id=15)
    assert biz_state[version_field] == data_id


@pytest.mark.parametrize(
    ("version", "version_field"),
    [
        (LandingVersion.STABLE, "stable_version"),
        (LandingVersion.UNSTABLE, "unstable_version"),
    ],
)
async def test_updates_biz_state_correctly_if_biz_state_exists(
    factory, dm, version, version_field
):
    pred_data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=15, slug="cafe", permalink="12345", **{version_field: pred_data_id}
    )

    await dm.save_landing_data_for_biz_id(
        biz_id=15,
        landing_data={
            "name": "Кафе с едой",
            "categories": ["Общепит", "Ресторан"],
            "contacts": {},
            "extras": {},
            "preferences": {},
            "blocks_options": {},
        },
        version=version,
    )

    data_id = (await factory.list_all_landing_data())[0]["id"]
    biz_state = await factory.fetch_biz_state(biz_id=15)
    assert biz_state[version_field] == data_id


@pytest.mark.parametrize(
    ("version", "version_field"),
    [
        (LandingVersion.STABLE, "unstable_version"),
        (LandingVersion.UNSTABLE, "stable_version"),
    ],
)
async def test_does_not_set_other_version_for_biz(factory, dm, version, version_field):
    await factory.insert_biz_state(biz_id=15, slug="cafe")

    await dm.save_landing_data_for_biz_id(
        biz_id=15,
        landing_data={
            "name": "Кафе с едой",
            "categories": ["Общепит", "Ресторан"],
            "contacts": {},
            "extras": {},
            "preferences": {},
            "blocks_options": {},
        },
        version=version,
    )

    biz_state = await factory.fetch_biz_state(biz_id=15)
    assert biz_state[version_field] is None


@pytest.mark.parametrize(
    ("version", "version_field"),
    [
        (LandingVersion.STABLE, "unstable_version"),
        (LandingVersion.UNSTABLE, "stable_version"),
    ],
)
async def test_does_not_replace_other_version_for_biz(
    factory, dm, version, version_field
):
    pred_data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=15, slug="cafe", **{version_field: pred_data_id}
    )

    await dm.save_landing_data_for_biz_id(
        biz_id=15,
        landing_data={
            "name": "Кафе с едой",
            "categories": ["Общепит", "Ресторан"],
            "contacts": {},
            "extras": {},
            "preferences": {},
            "blocks_options": {},
        },
        version=version,
    )

    biz_state = await factory.fetch_biz_state(biz_id=15)
    assert biz_state[version_field] == pred_data_id


@pytest.mark.parametrize("version", list(LandingVersion))
async def test_does_not_affect_another_biz_state(factory, dm, version):
    await factory.insert_biz_state(biz_id=15, permalink="54321", slug="cafe")
    await factory.insert_biz_state(biz_id=22, permalink="12345", slug="rest")

    await dm.save_landing_data_for_biz_id(
        biz_id=15,
        landing_data={
            "name": "Кафе с едой",
            "categories": ["Общепит", "Ресторан"],
            "contacts": {},
            "extras": {},
            "preferences": {},
            "blocks_options": {},
        },
        version=version,
    )

    biz_state = await factory.fetch_biz_state(biz_id=22)
    assert biz_state["stable_version"] is None
    assert biz_state["unstable_version"] is None


@pytest.mark.parametrize(
    ("version", "version_field"),
    [
        (LandingVersion.STABLE, "stable_version"),
        (LandingVersion.UNSTABLE, "unstable_version"),
    ],
)
async def test_removes_current_landing_data_for_version(
    factory, dm, version, version_field
):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=15, slug="cafe", permalink="54321", **{version_field: data_id}
    )

    await dm.save_landing_data_for_biz_id(
        biz_id=15,
        landing_data={
            "name": "Кафе с едой",
            "categories": ["Общепит", "Ресторан"],
            "contacts": {},
            "extras": {},
            "preferences": {},
            "blocks_options": {},
        },
        version=version,
    )

    assert await factory.list_all_landing_data() == [
        {
            "id": Any(int),
            "name": "Кафе с едой",
            "categories": ["Общепит", "Ресторан"],
            "description": None,
            "logo": None,
            "cover": None,
            "contacts": {},
            "extras": {},
            "preferences": {},
            "blocks_options": {},
            "landing_type": "DEFAULT",
            "instagram": None,
            "schedule": None,
            "photos": None,
            "photo_settings": None,
            "chain_id": None,
            "is_updated_from_geosearch": False,
        }
    ]


@pytest.mark.parametrize(
    ("version", "version_field"),
    [
        (LandingVersion.STABLE, "unstable_version"),
        (LandingVersion.UNSTABLE, "stable_version"),
    ],
)
async def test_does_not_remove_other_version_for_biz(
    factory, dm, version, version_field
):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(biz_id=15, slug="cafe", **{version_field: data_id})

    await dm.save_landing_data_for_biz_id(
        biz_id=15,
        landing_data={
            "name": "Кафе с едой",
            "categories": ["Общепит", "Ресторан"],
            "contacts": {},
            "extras": {},
            "preferences": {},
            "blocks_options": {},
        },
        version=version,
    )

    assert len(await factory.list_all_landing_data()) == 2


@pytest.mark.parametrize("version", list(LandingVersion))
async def test_does_not_remove_another_biz_versions(factory, dm, version):
    await factory.insert_biz_state(biz_id=15, slug="cafe")
    stable_data_id = await factory.insert_landing_data()
    unstable_data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22,
        slug="rest",
        stable_version=stable_data_id,
        unstable_version=unstable_data_id,
    )

    await dm.save_landing_data_for_biz_id(
        biz_id=15,
        landing_data={
            "name": "Кафе с едой",
            "categories": ["Общепит", "Ресторан"],
            "contacts": {},
            "extras": {},
            "preferences": {},
            "blocks_options": {},
        },
        version=version,
    )

    assert len(await factory.list_all_landing_data()) == 3


@pytest.mark.parametrize("version", list(LandingVersion))
@pytest.mark.parametrize("is_published", [False, True])
async def test_returns_new_landing_data(
    factory, dm, landing_data, version, is_published
):
    expected_landing_details = deepcopy(landing_data)
    expected_landing_details["version"] = version
    expected_landing_details["permalink"] = "12345"
    expected_landing_details["contacts"]["geo"] = dict(permalink="12345")
    expected_landing_details.update(landing_type="DEFAULT", instagram=None)

    await factory.insert_biz_state(
        biz_id=15, slug="cafe", permalink="12345", published=is_published
    )

    result = await dm.save_landing_data_for_biz_id(
        biz_id=15, landing_data=landing_data, version=version
    )

    assert result == {
        "slug": "cafe",
        "landing_details": expected_landing_details,
        "is_published": is_published,
    }


@pytest.mark.parametrize("version", list(LandingVersion))
async def test_raises_if_biz_id_is_unknown(factory, dm, landing_data, version):
    await factory.insert_biz_state(biz_id=22, slug="cafe")

    with pytest.raises(NoDataForBizId):
        await dm.save_landing_data_for_biz_id(
            biz_id=15, landing_data=landing_data, version=version
        )


@pytest.mark.parametrize("version", list(LandingVersion))
async def test_does_not_create_landing_data_if_biz_id_is_unknown(
    factory, dm, landing_data, version
):
    await factory.insert_biz_state(biz_id=22, slug="cafe")

    try:
        await dm.save_landing_data_for_biz_id(
            biz_id=15, landing_data=landing_data, version=version
        )
    except NoDataForBizId:
        pass

    assert await factory.list_all_landing_data() == []


@pytest.mark.parametrize("version", list(LandingVersion))
@pytest.mark.parametrize(("hidden_ids", "expected_photos"), [
    ([1], ['https://www.yandex.ru/pic2.png']),
    ([2], ['https://www.yandex.ru/pic1.jpeg'])
])
async def test_update_landing_data_and_store_old_extra(
    factory, dm, landing_data, version, hidden_ids, expected_photos
):
    slug = 'exp'
    photos = [{"id": 1, "url": 'https://www.yandex.ru/pic1.jpeg'}, {"id": 2, "url": 'https://www.yandex.ru/pic2.png'}]
    photo_settings = {"hidden_ids": hidden_ids}
    preferences = {"external_metrika_code": 60888865}
    stable_data_id = await factory.insert_landing_data(
        photos=photos, photo_settings=photo_settings, preferences=preferences, is_updated_from_geosearch=True
    )
    unstable_data_id = await factory.insert_landing_data(
        photos=photos, photo_settings=photo_settings, preferences=preferences, is_updated_from_geosearch=True
    )
    await factory.insert_biz_state(
        biz_id=15,
        slug=slug,
        stable_version=stable_data_id,
        unstable_version=unstable_data_id,
    )

    await dm.save_landing_data_for_biz_id(biz_id=15, landing_data=landing_data, version=version)

    result = await dm.fetch_landing_data_by_slug(slug=slug, version=version)
    landing_data_in_db = await factory.fetch_landing_data(biz_id=15, kind=version)
    assert result["photos"] == expected_photos
    assert result["preferences"]["external_metrika_code"] == preferences["external_metrika_code"]
    assert landing_data_in_db["is_updated_from_geosearch"] is True


@pytest.mark.parametrize("version", list(LandingVersion))
@pytest.mark.parametrize(("schedule", "expected_schedule"), [
    ({
        "schedule": [{"day": "EVERYDAY", "opens_at": 0, "closes_at": 86400}],
        "tz_offset": "3:00:00",
        "work_now_text": "Круглосуточно",
    }, {
        "schedule": [{"day": "EVERYDAY", "opens_at": 0, "closes_at": 86400}],
        "tz_offset": timedelta(seconds=10800),
        "work_now_text": "Круглосуточно",
    })
])
async def test_update_landing_data_and_store_schedule(
    factory, dm, landing_data, version, schedule, expected_schedule
):
    slug = 'exp'
    stable_data_id = await factory.insert_landing_data(
        is_updated_from_geosearch=True, schedule=schedule
    )
    unstable_data_id = await factory.insert_landing_data(
        is_updated_from_geosearch=True, schedule=schedule
    )
    await factory.insert_biz_state(
        biz_id=15,
        slug=slug,
        stable_version=stable_data_id,
        unstable_version=unstable_data_id,
    )

    await dm.save_landing_data_for_biz_id(biz_id=15, landing_data=landing_data, version=version)

    result = await dm.fetch_landing_data_by_slug(slug=slug, version=version)
    assert result["schedule"] == expected_schedule
