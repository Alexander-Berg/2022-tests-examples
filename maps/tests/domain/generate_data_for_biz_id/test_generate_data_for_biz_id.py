from unittest import mock

import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.landlord.server.lib.enums import LandingVersion
from maps_adv.geosmb.landlord.server.lib.exceptions import NoOrginfo, NoOrgsForBizId

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.mock_dm,
    pytest.mark.usefixtures("logging_warning"),
]


async def test_uses_clients_to_fetch_data_if_biz_id_passed(domain, bvm, geosearch):
    await domain.generate_data_for_biz_id(biz_id=15)

    bvm.fetch_permalinks_by_biz_id.assert_called_with(biz_id=15)
    geosearch.resolve_org.assert_called_with(permalink=54321)


async def test_raises_if_no_permalinks_found_for_biz_id(domain, bvm):
    bvm.fetch_permalinks_by_biz_id.coro.return_value = []

    with pytest.raises(NoOrgsForBizId):
        await domain.generate_data_for_biz_id(biz_id=15)


async def test_uses_clients_to_fetch_data_if_permalink_passed(domain, bvm, geosearch):
    await domain.generate_data_for_biz_id(permalink=54321)

    bvm.fetch_biz_id_by_permalink.assert_called_with(permalink=54321)
    geosearch.resolve_org.assert_called_with(permalink=54321)


async def test_raises_if_no_parameters_passed(domain):
    with pytest.raises(ValueError):
        await domain.generate_data_for_biz_id()


async def test_raises_if_both_parameters_passed(domain):
    with pytest.raises(ValueError):
        await domain.generate_data_for_biz_id(biz_id=15, permalink=54321)


async def test_raises_if_no_org_data_got_from_geosearch(domain, geosearch):
    geosearch.resolve_org.coro.return_value = None

    with pytest.raises(NoOrginfo):
        await domain.generate_data_for_biz_id(biz_id=15)


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_uses_dm_to_create_landing_data(domain, dm, params):
    await domain.generate_data_for_biz_id(**params)

    dm.fetch_biz_state.assert_any_call(biz_id=15)
    dm.create_biz_state.assert_called_with(
        biz_id=15, permalink="54321", slug="kafe-s-edoj"
    )
    dm.save_landing_data_for_biz_id.assert_has_calls(
        [
            mock.call(
                biz_id=15,
                landing_data={
                    "name": "Кафе с едой",
                    "categories": ["Общепит", "Ресторан"],
                    "logo": Any(str),
                    "cover": Any(str),
                    "contacts": {
                        "phone": "+7 (495) 739-70-00",
                        "website": "http://cafe.ru",
                        "phones": ["+7 (495) 739-70-00", "+7 (495) 739-70-22"],
                        "instagram": None,
                        "facebook": None,
                        "vkontakte": "http://vk.com/cafe",
                        "twitter": None,
                        "telegram": None,
                        "viber": None,
                        "whatsapp": None,
                        "email": "cafe@gmail.com",
                    },
                    "extras": Any(dict),
                    "preferences": {
                        "color_theme": {"theme": "LIGHT", "preset": "YELLOW"},
                        "cta_button": {
                            "predefined": "CALL",
                            "value": "+7 (495) 739-70-00",
                        },
                    },
                    "blocks_options": {
                        "show_cover": True,
                        "show_logo": True,
                        "show_schedule": True,
                        "show_photos": True,
                        "show_map_and_address": True,
                        "show_services": True,
                        "show_reviews": True,
                        "show_extras": True,
                    },
                },
                version=LandingVersion.STABLE,
            ),
            mock.call(
                biz_id=15,
                landing_data={
                    "name": "Кафе с едой",
                    "categories": ["Общепит", "Ресторан"],
                    "logo": Any(str),
                    "cover": Any(str),
                    "contacts": {
                        "phone": "+7 (495) 739-70-00",
                        "website": "http://cafe.ru",
                        "phones": ["+7 (495) 739-70-00", "+7 (495) 739-70-22"],
                        "instagram": None,
                        "facebook": None,
                        "vkontakte": "http://vk.com/cafe",
                        "twitter": None,
                        "telegram": None,
                        "viber": None,
                        "whatsapp": None,
                        "email": "cafe@gmail.com",
                    },
                    "extras": Any(dict),
                    "preferences": {
                        "color_theme": {"theme": "LIGHT", "preset": "YELLOW"},
                        "cta_button": {
                            "predefined": "CALL",
                            "value": "+7 (495) 739-70-00",
                        },
                    },
                    "blocks_options": {
                        "show_cover": True,
                        "show_logo": True,
                        "show_schedule": True,
                        "show_photos": True,
                        "show_map_and_address": True,
                        "show_services": True,
                        "show_reviews": True,
                        "show_extras": True,
                    },
                },
                version=LandingVersion.UNSTABLE,
            ),
        ],
        any_order=True,
    )


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_creates_biz_state_with_head_permalink(domain, dm, params, geosearch):
    geosearch.resolve_org.coro.return_value.permalink = "98765"

    await domain.generate_data_for_biz_id(**params)

    dm.create_biz_state.assert_called_with(
        biz_id=15, permalink="98765", slug="kafe-s-edoj"
    )
    dm.update_biz_state_permalink.assert_not_called()


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_updates_biz_state_head_permalink_if_has_new(
    domain, dm, params, geosearch
):
    dm.fetch_biz_state.coro.return_value = {
        "biz_id": 15,
        "slug": "kafe-s-edoj",
        "permalink": "35687",
        "stable_version": None,
        "unstable_version": None,
    }
    geosearch.resolve_org.coro.return_value.permalink = "98765"

    await domain.generate_data_for_biz_id(**params)

    dm.create_biz_state.assert_not_called()
    dm.update_biz_state_permalink.assert_called_with(biz_id=15, permalink="98765")


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_sets_landing_published_if_requested(domain, dm, params):
    await domain.generate_data_for_biz_id(publish=True, **params)

    dm.set_landing_publicity.assert_called_with(biz_id=15, is_published=True)


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_does_not_set_landing_published_if_not_requested(domain, dm, params):
    await domain.generate_data_for_biz_id(publish=False, **params)

    dm.set_landing_publicity.assert_not_called()


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_does_not_set_landing_published_by_default(domain, dm, params):
    await domain.generate_data_for_biz_id(**params)

    dm.set_landing_publicity.assert_not_called()


@pytest.mark.parametrize(
    ("features", "expected_extras"),
    [
        ([{"id": "wifi", "name": "Wi-fi", "value": True}], ["Wi-fi"]),
        # Capitalize name
        ([{"id": "wifi", "name": "wi-fi", "value": True}], ["Wi-fi"]),
        (
            # False value ignored
            [{"id": "wifi", "name": "Wi-fi", "value": False}],
            [],
        ),
        (
            # Text value ignored
            [{"id": "wifi", "name": "Wi-fi", "value": ["2.4G"]}],
            [],
        ),
        (
            # Enum value ignored
            [
                {
                    "id": "wifi",
                    "name": "Wi-fi",
                    "value": {"id": "enum_value1", "name": "Значение 1"},
                }
            ],
            [],
        ),
        (
            # Nameless feature ignored
            [{"id": "wifi", "value": True}],
            [],
        ),
        (
            # Combined case
            [
                {"id": "wifi", "name": "Wi-fi", "value": True},
                {"id": "wifi", "name": "Wi-fi", "value": ["2.4G"]},
                {"id": "card", "name": "Оплата картой", "value": True},
            ],
            ["Wi-fi", "Оплата картой"],
        ),
    ],
)
@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_selects_extras_from_features(
    domain, dm, geosearch, features, expected_extras, params
):
    geosearch.resolve_org.coro.return_value.features = features

    await domain.generate_data_for_biz_id(**params)

    landing_data = dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]
    assert landing_data["extras"]["plain_extras"] == expected_extras


@pytest.mark.parametrize(
    ("social_links", "expected_social_in_contacts"),
    [
        ({"vkontakte": "http://vk.com/cafe"}, {"vkontakte": "http://vk.com/cafe"}),
        (
            {"facebook": "http://facebook.com/cafe"},
            {"facebook": "http://facebook.com/cafe"},
        ),
        (
            {"twitter": "http://twitter.com/cafe"},
            {"twitter": "http://twitter.com/cafe"},
        ),
        (
            {"instagram": "http://instagram.com/cafe"},
            {"instagram": "http://instagram.com/cafe"},
        ),
        (
            {"telegram": "https://t.me/cafe"},
            {"telegram": "https://t.me/cafe"},
        ),
        (
            {"viber": "https://viber.click/cafe"},
            {"viber": "https://viber.click/cafe"},
        ),
        (
            {"whatsapp": "https://wa.me/cafe"},
            {"whatsapp": "https://wa.me/cafe"},
        ),
        ({"lj": "http://cafe.livejournal.com"}, {}),
        (
            {
                "vkontakte": "http://vk.com/cafe",
                "lj": "http://cafe.livejournal.com",
                "twitter": "http://twitter.com/cafe",
            },
            {"vkontakte": "http://vk.com/cafe", "twitter": "http://twitter.com/cafe"},
        ),
    ],
)
@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_selects_social_links(
    domain, dm, geosearch, social_links, expected_social_in_contacts, params
):
    expected_contacts = {
        "phone": "+7 (495) 739-70-00",
        "phones": ["+7 (495) 739-70-00", "+7 (495) 739-70-22"],
        "website": "http://cafe.ru",
        "facebook": None,
        "instagram": None,
        "telegram": None,
        "viber": None,
        "whatsapp": None,
        "twitter": None,
        "vkontakte": None,
        "email": "cafe@gmail.com",
    }
    expected_contacts.update(expected_social_in_contacts)
    geosearch.resolve_org.coro.return_value.social_links = social_links

    await domain.generate_data_for_biz_id(**params)

    assert (
        dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]["contacts"]
        == expected_contacts
    )


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_returns_created_slug(domain, params):
    assert await domain.generate_data_for_biz_id(**params) == "kafe-s-edoj"


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_creates_data_for_biz_state_without_stable_data(domain, dm, params):
    dm.fetch_biz_state.coro.return_value = {
        "biz_id": 15,
        "slug": "cafe",
        "permalink": "235345",
        "stable_version": None,
        "unstable_version": None,
        "published": False,
    }

    await domain.generate_data_for_biz_id(**params)

    dm.save_landing_data_for_biz_id.assert_called()


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_return_slug_if_biz_state_exists(domain, dm, params):
    dm.fetch_biz_state.coro.return_value = {
        "biz_id": 15,
        "slug": "cafe",
        "permalink": "235345",
        "stable_version": 88,
        "unstable_version": 991,
        "published": False,
    }

    await domain.generate_data_for_biz_id(**params)

    dm.save_landing_data_for_biz_id.assert_not_called()
