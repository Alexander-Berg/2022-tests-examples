from decimal import Decimal

import pytest

from maps_adv.adv_store.api.schemas.enums import (
    ActionTypeEnum,
    CampaignStatusEnum,
    FixTimeIntervalEnum,
    OrderSizeEnum,
    PlatformEnum,
    PublicationEnvEnum,
    ResolveUriTargetEnum,
)
from maps_adv.common.helpers import Any, dt
from maps_adv.common.helpers.enums import CampaignTypeEnum

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    ["billing_type", "billing", "expected_result"],
    [
        (
            "cpm",
            {
                "cost": Decimal("0.9999"),
                "budget": Decimal("1000"),
                "daily_budget": Decimal("5000"),
                "auto_daily_budget": False,
            },
            {
                "id": Any(int),
                "cost": "0.9999",
                "budget": "1000.0000",
                "daily_budget": "5000.0000",
                "auto_daily_budget": False,
            },
        ),
        (
            "cpa",
            {
                "cost": Decimal("12.3456"),
                "budget": Decimal("1000"),
                "daily_budget": Decimal("5000"),
                "auto_daily_budget": False,
            },
            {
                "id": Any(int),
                "cost": "12.3456",
                "budget": "1000.0000",
                "daily_budget": "5000.0000",
                "auto_daily_budget": False,
            },
        ),
        (
            "fix",
            {"time_interval": FixTimeIntervalEnum.MONTHLY, "cost": Decimal("12.3456")},
            {"id": Any(int), "time_interval": "MONTHLY", "cost": "12.3456"},
        ),
    ],
)
async def test_returns_billing_data_as_expected(
    billing_type, billing, expected_result, factory
):
    campaign_id = (await factory.create_campaign(**{billing_type: billing}))["id"]

    result = await factory.retrieve_campaign_full_state_view_entry(campaign_id)

    assert result["billing"][billing_type] == expected_result


@pytest.mark.parametrize(
    ["campaign", "expected_result"],
    [
        (
            {
                "author_id": 123,
                "campaign_type": CampaignTypeEnum.ZERO_SPEED_BANNER,
                "comment": "",
                "datatesting_expires_at": None,
                "display_probability": None,
                "display_probability_auto": None,
                "end_datetime": dt("2019-02-01 00:00:00"),
                "manul_order_id": None,
                "name": "campaign0",
                "order_id": 10,
                "order_size": None,
                "platforms": [PlatformEnum.NAVI],
                "publication_envs": [PublicationEnvEnum.DATA_TESTING],
                "rubric": None,
                "start_datetime": dt("2019-01-01 00:00:00"),
                "targeting": {},
                "timezone": "UTC",
                "user_daily_display_limit": None,
                "user_display_limit": None,
                "settings": {},
                "paid_till": None,
            },
            {
                "id": Any(int),
                "author_id": 123,
                "campaign_type": "ZERO_SPEED_BANNER",
                "changed_datetime": Any(str),
                "comment": "",
                "datatesting_expires_at": None,
                "display_probability": None,
                "display_probability_auto": None,
                "end_datetime": "2019-02-01T00:00:00+00:00",
                "manul_order_id": None,
                "direct_moderation_id": None,
                "name": "campaign0",
                "order_id": 10,
                "order_size": None,
                "platforms": ["NAVI"],
                "publication_envs": ["DATA_TESTING"],
                "rubric": None,
                "start_datetime": "2019-01-01T00:00:00+00:00",
                "targeting": {},
                "timezone": "UTC",
                "user_daily_display_limit": None,
                "user_display_limit": None,
                "settings": {},
                "paid_till": None,
            },
        ),
        (
            {
                "author_id": 123,
                "campaign_type": CampaignTypeEnum.BILLBOARD,
                "comment": "1234",
                "datatesting_expires_at": dt("2019-01-01 00:00:00"),
                "display_probability": Decimal("0.123456"),
                "display_probability_auto": Decimal("0.123456"),
                "end_datetime": dt("2019-02-01 00:00:00"),
                "manul_order_id": None,
                "name": "campaign0",
                "order_id": 10,
                "order_size": OrderSizeEnum.BIG,
                "platforms": [PlatformEnum.MAPS, PlatformEnum.METRO],
                "publication_envs": [
                    PublicationEnvEnum.DATA_TESTING,
                    PublicationEnvEnum.PRODUCTION,
                ],
                "rubric": None,
                "start_datetime": dt("2019-01-01 00:00:00"),
                "targeting": {"key": "value"},
                "timezone": "UTC",
                "user_daily_display_limit": 1,
                "user_display_limit": 2,
                "settings": {"custom_page_id": "abc"},
                "paid_till": None,
            },
            {
                "id": Any(int),
                "author_id": 123,
                "campaign_type": "BILLBOARD",
                "changed_datetime": Any(str),
                "comment": "1234",
                "datatesting_expires_at": "2019-01-01T00:00:00+00:00",
                "display_probability": 0.123456,
                "display_probability_auto": 0.123456,
                "end_datetime": "2019-02-01T00:00:00+00:00",
                "manul_order_id": None,
                "direct_moderation_id": None,
                "name": "campaign0",
                "order_id": 10,
                "order_size": "BIG",
                "platforms": ["MAPS", "METRO"],
                "publication_envs": ["DATA_TESTING", "PRODUCTION"],
                "rubric": None,
                "start_datetime": "2019-01-01T00:00:00+00:00",
                "targeting": {"key": "value"},
                "timezone": "UTC",
                "user_daily_display_limit": 1,
                "user_display_limit": 2,
                "settings": {"custom_page_id": "abc"},
                "paid_till": None,
            },
        ),
    ],
)
async def test_returns_campaign_data_as_expected(campaign, expected_result, factory):
    campaign_id = (await factory.create_campaign(**campaign))["id"]

    result = await factory.retrieve_campaign_full_state_view_entry(campaign_id)

    assert result["campaign"] == expected_result


@pytest.mark.parametrize(
    ["placing", "expected_result"],
    [
        (
            {"organizations": {"permalinks": [1, 2, 3]}},
            {"organizations": {"permalinks": [1, 2, 3]}, "area": None},
        ),
        (
            {"area": {"version": 1, "areas": []}},
            {"organizations": None, "area": {"version": 1, "areas": []}},
        ),
        (
            {
                "organizations": {"permalinks": [1, 2, 3]},
                "area": {"version": 1, "areas": [{"key": "value"}]},
            },
            {
                "organizations": {"permalinks": [1, 2, 3]},
                "area": {"version": 1, "areas": [{"key": "value"}]},
            },
        ),
    ],
)
async def test_returns_placing_data_as_expected(placing, expected_result, factory):
    campaign_id = (await factory.create_campaign(**placing))["id"]

    result = await factory.retrieve_campaign_full_state_view_entry(campaign_id)

    assert result["placing"] == expected_result


@pytest.mark.parametrize(
    ["week_schedule_periods", "expected_result"],
    [
        ([], None),
        ([{"start": 1, "end": 2}], [{"start": 1, "end": 2}]),
        (
            [{"start": 2, "end": 3}, {"start": 1, "end": 2}],
            [{"start": 1, "end": 2}, {"start": 2, "end": 3}],
        ),
    ],
)
async def test_returns_week_schedule_data_as_expected(
    week_schedule_periods, expected_result, factory
):
    campaign_id = (await factory.create_campaign(week_schedule=week_schedule_periods))[
        "id"
    ]

    result = await factory.retrieve_campaign_full_state_view_entry(campaign_id)

    assert result["week_schedule"] == expected_result


@pytest.mark.parametrize(
    ["discounts", "expected_result"],
    [
        ([], None),
        (
            [
                {
                    "start_datetime": dt("2019-01-01 00:00:00"),
                    "end_datetime": dt("2019-01-01 00:00:01"),
                    "cost_multiplier": Decimal("1.1"),
                }
            ],
            [
                {
                    "id": Any(int),
                    "start_datetime": "2019-01-01T00:00:00+00:00",
                    "end_datetime": "2019-01-01T00:00:01+00:00",
                    "cost_multiplier": 1.1,
                }
            ],
        ),
        (
            [
                {
                    "start_datetime": dt("2019-01-01 00:00:02"),
                    "end_datetime": dt("2019-01-01 00:00:03"),
                    "cost_multiplier": Decimal("1.1"),
                },
                {
                    "start_datetime": dt("2019-01-01 00:00:00"),
                    "end_datetime": dt("2019-01-01 00:00:01"),
                    "cost_multiplier": Decimal("0"),
                },
            ],
            [
                {
                    "id": Any(int),
                    "start_datetime": "2019-01-01T00:00:00+00:00",
                    "end_datetime": "2019-01-01T00:00:01+00:00",
                    "cost_multiplier": 0,
                },
                {
                    "id": Any(int),
                    "start_datetime": "2019-01-01T00:00:02+00:00",
                    "end_datetime": "2019-01-01T00:00:03+00:00",
                    "cost_multiplier": 1.1,
                },
            ],
        ),
    ],
)
async def test_returns_discount_data_as_expected(discounts, expected_result, factory):
    campaign_id = (await factory.create_campaign(discounts=discounts))["id"]

    result = await factory.retrieve_campaign_full_state_view_entry(campaign_id)

    assert result["discounts"] == expected_result


@pytest.mark.parametrize(
    ["status_history", "expected_result"],
    [
        (
            [
                {
                    "author_id": 0,
                    "status": CampaignStatusEnum.DRAFT,
                    "metadata": {},
                    # "changed_datetime": dt("2019-01-01 00:00:00"),
                }
            ],
            {
                "author_id": 0,
                "status": "DRAFT",
                "metadata": {},
                "changed_datetime": Any(str),
            },
        ),
        (
            [
                {"author_id": 0, "status": CampaignStatusEnum.DRAFT, "metadata": {}},
                {
                    "author_id": 123,
                    "status": CampaignStatusEnum.ACTIVE,
                    "metadata": {"key": "value"},
                },
            ],
            {
                "author_id": 123,
                "status": "ACTIVE",
                "metadata": {"key": "value"},
                "changed_datetime": Any(str),
            },
        ),
    ],
)
async def test_returns_current_status_history_data_as_expected(
    status_history, expected_result, factory
):
    campaign_id = (await factory.create_campaign())["id"]
    for status in status_history:
        await factory.set_status(campaign_id=campaign_id, **status)

    result = await factory.retrieve_campaign_full_state_view_entry(campaign_id)

    assert result["current_status_history"] == expected_result


@pytest.mark.parametrize(
    ["actions", "expected_result"],
    [
        (
            [],
            {
                "download_app": None,
                "open_site": None,
                "phone_call": None,
                "promocode": None,
                "search": None,
                "resolve_uri": None,
                "add_point_to_route": None,
            },
        ),
        (
            [
                {
                    "type_": "download_app",
                    "title": "title download_app",
                    "google_play_id": "google://play/id",
                    "app_store_id": "app://store/id",
                    "url": "http://download/url",
                    "main": False,
                }
            ],
            {
                "download_app": [
                    {
                        "title": "title download_app",
                        "google_play_id": "google://play/id",
                        "app_store_id": "app://store/id",
                        "url": "http://download/url",
                        "main": False,
                    }
                ],
                "open_site": None,
                "phone_call": None,
                "promocode": None,
                "search": None,
                "resolve_uri": None,
                "add_point_to_route": None,
            },
        ),
        (
            [
                {
                    "type_": "open_site",
                    "title": "title open_site",
                    "url": "http://site/url",
                    "main": False,
                }
            ],
            {
                "download_app": None,
                "open_site": [
                    {
                        "title": "title open_site",
                        "url": "http://site/url",
                        "main": False,
                    }
                ],
                "phone_call": None,
                "promocode": None,
                "search": None,
                "resolve_uri": None,
                "add_point_to_route": None,
            },
        ),
        (
            [
                {
                    "type_": "phone_call",
                    "title": "title phone_call",
                    "phone": "+00000000000000",
                    "main": False,
                }
            ],
            {
                "download_app": None,
                "open_site": None,
                "phone_call": [
                    {
                        "title": "title phone_call",
                        "phone": "+00000000000000",
                        "main": False,
                    }
                ],
                "promocode": None,
                "search": None,
                "resolve_uri": None,
                "add_point_to_route": None,
            },
        ),
        (
            [{"type_": "promocode", "promocode": "promocode number", "main": False}],
            {
                "download_app": None,
                "open_site": None,
                "phone_call": None,
                "promocode": [{"promocode": "promocode number", "main": False}],
                "search": None,
                "resolve_uri": None,
                "add_point_to_route": None,
            },
        ),
        (
            [
                {
                    "type_": "search",
                    "title": "title search",
                    "organizations": [1, 2, 3],
                    "history_text": "history search text",
                    "main": False,
                }
            ],
            {
                "download_app": None,
                "open_site": None,
                "phone_call": None,
                "promocode": None,
                "search": [
                    {
                        "title": "title search",
                        "organizations": [1, 2, 3],
                        "history_text": "history search text",
                        "main": False,
                    }
                ],
                "resolve_uri": None,
                "add_point_to_route": None,
            },
        ),
        (
            [
                {
                    "type_": "resolve_uri",
                    "uri": "magic://site/url",
                    "action_type": ActionTypeEnum.OPEN_SITE,
                    "target": ResolveUriTargetEnum.WEB_VIEW,
                    "dialog": None,
                    "main": False,
                }
            ],
            {
                "download_app": None,
                "open_site": None,
                "phone_call": None,
                "promocode": None,
                "search": None,
                "resolve_uri": [
                    {
                        "uri": "magic://site/url",
                        "action_type": "OPEN_SITE",
                        "target": "WEB_VIEW",
                        "dialog": None,
                        "main": False,
                    }
                ],
                "add_point_to_route": None,
            },
        ),
        (
            [
                {
                    "type_": "add_point_to_route",
                    "latitude": 1.2,
                    "longitude": 3.4,
                    "main": False,
                }
            ],
            {
                "download_app": None,
                "open_site": None,
                "phone_call": None,
                "promocode": None,
                "search": None,
                "resolve_uri": None,
                "add_point_to_route": [
                    {"latitude": 1.2, "longitude": 3.4, "main": False}
                ],
            },
        ),
        (
            [
                {
                    "type_": "download_app",
                    "title": "title download_app",
                    "google_play_id": "google://play/id",
                    "app_store_id": "app://store/id",
                    "url": "http://download/url",
                    "main": False,
                },
                {
                    "type_": "open_site",
                    "title": "title open_site",
                    "url": "http://site/url",
                    "main": False,
                },
                {
                    "type_": "phone_call",
                    "title": "title phone_call",
                    "phone": "+00000000000000",
                    "main": True,
                },
                {"type_": "promocode", "promocode": "promocode number", "main": False},
                {
                    "type_": "search",
                    "title": "title search",
                    "organizations": [1, 2, 3],
                    "history_text": "history search text",
                    "main": False,
                },
                {
                    "type_": "resolve_uri",
                    "uri": "magic://site/url",
                    "action_type": ActionTypeEnum.OPEN_SITE,
                    "target": ResolveUriTargetEnum.WEB_VIEW,
                    "dialog": None,
                    "main": False,
                },
                {
                    "type_": "add_point_to_route",
                    "latitude": 1.2,
                    "longitude": 3.4,
                    "main": False,
                },
            ],
            {
                "download_app": [
                    {
                        "title": "title download_app",
                        "google_play_id": "google://play/id",
                        "app_store_id": "app://store/id",
                        "url": "http://download/url",
                        "main": False,
                    }
                ],
                "open_site": [
                    {
                        "title": "title open_site",
                        "url": "http://site/url",
                        "main": False,
                    }
                ],
                "phone_call": [
                    {
                        "title": "title phone_call",
                        "phone": "+00000000000000",
                        "main": True,
                    }
                ],
                "promocode": [{"promocode": "promocode number", "main": False}],
                "search": [
                    {
                        "title": "title search",
                        "organizations": [1, 2, 3],
                        "history_text": "history search text",
                        "main": False,
                    }
                ],
                "resolve_uri": [
                    {
                        "uri": "magic://site/url",
                        "action_type": "OPEN_SITE",
                        "target": "WEB_VIEW",
                        "dialog": None,
                        "main": False,
                    }
                ],
                "add_point_to_route": [
                    {"latitude": 1.2, "longitude": 3.4, "main": False}
                ],
            },
        ),
    ],
)
async def test_returns_action_data_as_expected(actions, expected_result, factory):
    campaign_id = (await factory.create_campaign(actions=actions))["id"]

    result = await factory.retrieve_campaign_full_state_view_entry(campaign_id)

    assert result["action"] == expected_result


@pytest.mark.parametrize(
    ["creatives", "expected_result"],
    [
        (  # no creatives
            [],
            {
                "pin": None,
                "billboard": None,
                "icon": None,
                "pin_search": None,
                "logo_and_text": None,
                "text": None,
                "via_point": None,
                "banner": None,
                "audio_banner": None,
            },
        ),
        (  # only banner type of creative
            [
                {
                    "type_": "banner",
                    "images": [{}],
                    "disclaimer": "banner disclaimer",
                    "show_ads_label": True,
                    "description": "banner description",
                    "title": "banner title",
                    "terms": "banner terms",
                }
            ],
            {
                "banner": [
                    {
                        "images": [{}],
                        "disclaimer": "banner disclaimer",
                        "show_ads_label": True,
                        "description": "banner description",
                        "title": "banner title",
                        "terms": "banner terms",
                    }
                ],
                "pin": None,
                "billboard": None,
                "icon": None,
                "pin_search": None,
                "logo_and_text": None,
                "text": None,
                "via_point": None,
                "audio_banner": None,
            },
        ),
        (  # only billboard type of creative
            [
                {
                    "type_": "billboard",
                    "images": [{}],
                    "images_v2": [],
                    "title": "billboard title",
                    "description": "billboard description",
                }
            ],
            {
                "billboard": [
                    {
                        "images": [{}],
                        "images_v2": [],
                        "title": "billboard title",
                        "description": "billboard description",
                    }
                ],
                "pin": None,
                "icon": None,
                "pin_search": None,
                "logo_and_text": None,
                "text": None,
                "via_point": None,
                "banner": None,
                "audio_banner": None,
            },
        ),
        (
            [
                {
                    "type_": "icon",
                    "images": [{}],
                    "position": 100500,
                    "title": "icon title",
                }
            ],
            {
                "icon": [{"images": [{}], "position": 100500, "title": "icon title"}],
                "pin": None,
                "billboard": None,
                "pin_search": None,
                "logo_and_text": None,
                "text": None,
                "via_point": None,
                "banner": None,
                "audio_banner": None,
            },
        ),
        (  # only logo_and_text type of creative
            [{"type_": "logo_and_text", "images": [{}], "text": "logo_and_text text"}],
            {
                "pin": None,
                "billboard": None,
                "icon": None,
                "pin_search": None,
                "logo_and_text": [{"images": [{}], "text": "logo_and_text text"}],
                "text": None,
                "via_point": None,
                "banner": None,
                "audio_banner": None,
            },
        ),
        (  # only pin type of creative
            [
                {
                    "type_": "pin",
                    "images": [{}],
                    "title": "pin title",
                    "subtitle": "pin subtitle",
                }
            ],
            {
                "pin": [
                    {"images": [{}], "title": "pin title", "subtitle": "pin subtitle"}
                ],
                "billboard": None,
                "icon": None,
                "pin_search": None,
                "logo_and_text": None,
                "text": None,
                "via_point": None,
                "banner": None,
                "audio_banner": None,
            },
        ),
        (  # only pin_search type of creative
            [
                {
                    "type_": "pin_search",
                    "images": [{}],
                    "title": "pin_search title",
                    "organizations": [1, 2, 3],
                }
            ],
            {
                "pin": None,
                "billboard": None,
                "icon": None,
                "pin_search": [
                    {
                        "images": [{}],
                        "title": "pin_search title",
                        "organizations": [1, 2, 3],
                    }
                ],
                "logo_and_text": None,
                "text": None,
                "via_point": None,
                "banner": None,
                "audio_banner": None,
            },
        ),
        (  # only text type of creative
            [{"type_": "text", "text": "text text", "disclaimer": "disclaimer text"}],
            {
                "pin": None,
                "billboard": None,
                "icon": None,
                "pin_search": None,
                "logo_and_text": None,
                "text": [{"text": "text text", "disclaimer": "disclaimer text"}],
                "via_point": None,
                "banner": None,
                "audio_banner": None,
            },
        ),
        (  # only via_point type of creative
            [
                {
                    "type_": "via_point",
                    "images": [{}],
                    "button_text_active": "via_point active text",
                    "button_text_inactive": "via_point inactive text",
                    "description": "via_point description",
                }
            ],
            {
                "pin": None,
                "billboard": None,
                "icon": None,
                "pin_search": None,
                "logo_and_text": None,
                "text": None,
                "via_point": [
                    {
                        "images": [{}],
                        "button_text_active": "via_point active text",
                        "button_text_inactive": "via_point inactive text",
                        "description": "via_point description",
                    }
                ],
                "banner": None,
                "audio_banner": None,
            },
        ),
        (  # only audio banner type of creative
            [
                {
                    "type_": "audio_banner",
                    "images": [{}],
                    "left_anchor": 0.5,
                    "audio_file_url": "http://someurl.com",
                }
            ],
            {
                "pin": None,
                "billboard": None,
                "icon": None,
                "pin_search": None,
                "logo_and_text": None,
                "text": None,
                "via_point": None,
                "banner": None,
                "audio_banner": [
                    {
                        "images": [{}],
                        "left_anchor": 0.5,
                        "audio_file_url": "http://someurl.com",
                    }
                ],
            },
        ),
        (  # all types of creatives in one campaign
            [
                {
                    "type_": "banner",
                    "images": [{}],
                    "disclaimer": "banner disclaimer",
                    "show_ads_label": True,
                    "description": "banner description",
                    "title": "banner title",
                    "terms": "banner terms",
                },
                {
                    "type_": "billboard",
                    "images": [{}],
                    "images_v2": [],
                    "title": "billboard title",
                    "description": "billboard description",
                },
                {
                    "type_": "icon",
                    "images": [{}],
                    "position": 100500,
                    "title": "icon title",
                },
                {
                    "type_": "logo_and_text",
                    "images": [{}],
                    "text": "logo_and_text text",
                },
                {
                    "type_": "pin",
                    "images": [{}],
                    "title": "pin title",
                    "subtitle": "pin subtitle",
                },
                {
                    "type_": "pin_search",
                    "images": [{}],
                    "title": "pin_search title",
                    "organizations": [1, 2, 3],
                },
                {"type_": "text", "text": "text text", "disclaimer": "disclaimer text"},
                {
                    "type_": "via_point",
                    "images": [{}],
                    "button_text_active": "via_point active text",
                    "button_text_inactive": "via_point inactive text",
                    "description": "via_point description",
                },
                {
                    "type_": "audio_banner",
                    "images": [{}],
                    "left_anchor": 0.5,
                    "audio_file_url": "http://someurl.com",
                },
            ],
            {
                "banner": [
                    {
                        "images": [{}],
                        "disclaimer": "banner disclaimer",
                        "show_ads_label": True,
                        "description": "banner description",
                        "title": "banner title",
                        "terms": "banner terms",
                    }
                ],
                "billboard": [
                    {
                        "images": [{}],
                        "images_v2": [],
                        "title": "billboard title",
                        "description": "billboard description",
                    }
                ],
                "icon": [{"images": [{}], "position": 100500, "title": "icon title"}],
                "logo_and_text": [{"images": [{}], "text": "logo_and_text text"}],
                "pin": [
                    {"images": [{}], "title": "pin title", "subtitle": "pin subtitle"}
                ],
                "pin_search": [
                    {
                        "images": [{}],
                        "title": "pin_search title",
                        "organizations": [1, 2, 3],
                    }
                ],
                "text": [{"text": "text text", "disclaimer": "disclaimer text"}],
                "via_point": [
                    {
                        "images": [{}],
                        "button_text_active": "via_point active text",
                        "button_text_inactive": "via_point inactive text",
                        "description": "via_point description",
                    }
                ],
                "audio_banner": [
                    {
                        "images": [{}],
                        "left_anchor": 0.5,
                        "audio_file_url": "http://someurl.com",
                    }
                ],
            },
        ),
    ],
)
async def test_returns_creative_data_as_expected(creatives, expected_result, factory):
    campaign_id = (await factory.create_campaign(creatives=creatives))["id"]

    result = await factory.retrieve_campaign_full_state_view_entry(campaign_id)

    assert result["creative"] == expected_result
