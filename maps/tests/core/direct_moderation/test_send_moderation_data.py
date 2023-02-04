import json
from decimal import Decimal

import pytest

from maps_adv.adv_store.v2.lib.core.logbroker import LogbrokerWrapper
from maps_adv.adv_store.api.schemas.enums import (
    ActionTypeEnum,
    CampaignDirectModerationStatusEnum,
    CampaignDirectModerationWorkflowEnum,
    CampaignStatusEnum,
    PlatformEnum,
    PublicationEnvEnum,
    ResolveUriTargetEnum,
)
from maps_adv.common.helpers import coro_mock, dt
from maps_adv.common.helpers.enums import CampaignTypeEnum

pytestmark = [pytest.mark.asyncio]

SUPPORTED_CAMPAIGN_TYPES = [CampaignTypeEnum.ZERO_SPEED_BANNER]
UNSUPPORTED_CAMPAIGN_TYPES = list(set(CampaignTypeEnum) - set(SUPPORTED_CAMPAIGN_TYPES))


@pytest.fixture
async def topic_writer():
    class MockTopicWriter(LogbrokerWrapper):
        start = coro_mock()
        stop = coro_mock()
        write_one = coro_mock()

    return MockTopicWriter()


@pytest.fixture
async def logbroker_client(topic_writer):
    class MockLogbrokerClient(LogbrokerWrapper):
        start = coro_mock()
        stop = coro_mock()

        def create_writer(self, topic: str):
            return topic_writer

    return MockLogbrokerClient()


@pytest.mark.parametrize("campaign_type", SUPPORTED_CAMPAIGN_TYPES)
async def test_client_sends_direct_moderation_data_for_zero_speed_banner(
    config, direct_moderation_client, topic_writer, campaign_type
):
    campaign = {
        "id": 1,
        "author_id": 123,
        "created_datetime": dt("2020-01-01 00:00:00"),
        "name": "campaign0",
        "timezone": "UTC",
        "comment": "",
        "user_display_limit": None,
        "user_daily_display_limit": None,
        "start_datetime": dt("2020-01-01 00:00:00"),
        "end_datetime": dt("2020-01-01 00:00:00"),
        "targeting": {},
        "publication_envs": [PublicationEnvEnum.DATA_TESTING],
        "campaign_type": campaign_type,
        "platforms": [PlatformEnum.NAVI],
        "rubric": None,
        "order_size": None,
        "order_id": 10,
        "manul_order_id": None,
        "billing": {
            "cpm": {
                "cost": Decimal("12.3456"),
                "budget": Decimal("1000"),
                "daily_budget": Decimal("5000"),
                "auto_daily_budget": False,
            }
        },
        "actions": [
            {"type_": "open_site", "title": "Перейти", "url": "ya.ru"},
            {"type_": "phone_call", "title": "Позвонить", "phone": "322-223"},
            {
                "type_": "search",
                "title": "Поискать",
                "organizations": [1, 2, 4],
                "history_text": "Поискал",
            },
        ],
        "creatives": [
            {
                "type_": "banner",
                "images": [
                    {
                        "type": "source_image",
                        "group_id": "1111",
                        "image_name": "name1",
                        "alias_template": "banner_320x64_x4",
                    },
                    {
                        "type": "banner",
                        "group_id": "2222",
                        "image_name": "name2",
                        "alias_template": "banner_320x64_{size}",
                    },
                ],
                "disclaimer": "Дисклеймер",
                "show_ads_label": True,
                "description": "Описание",
                "title": "Заголовок",
                "terms": "",
            }
        ],
        "placing": {"organizations": {"permalinks": [123, 345]}},
        "week_schedule": [],
        "status": CampaignStatusEnum.DRAFT,
        "discounts": [],
        "datatesting_expires_at": None,
        "moderation_verdicts": None,
    }

    moderation = {
        "id": 1,
        "created_at": dt("2020-01-01 00:00:00"),
        "campaign_id": 1,
        "reviewer_uid": None,
        "status": CampaignDirectModerationStatusEnum.NEW,
        "workflow": CampaignDirectModerationWorkflowEnum.COMMON,
        "verdicts": [],
    }

    async with direct_moderation_client:
        await direct_moderation_client.send_campaign_moderation(campaign, moderation, 1)

    message = {
        "meta": {"campaign_id": 1, "version_id": 1},
        "type": "ZERO_SPEED_BANNER",
        "workflow": "COMMON",
        "country_geo_id": 1,
        "verdictor_uid": None,
        "data": {
            "image": "https://avatars.mds.yandex.net/get-geoadv-ext/1111/name1/banner_320x64_x4",  # noqa: E501
            "disclaimer": "Дисклеймер",
            "title": "Заголовок",
            "description": "Описание",
            "terms": "",
            "actions": [
                {"title": "Перейти", "url": "ya.ru", "type": "open_site"},
                {"title": "Позвонить", "phone": "322-223", "type": "phone_call"},
                {
                    "title": "Поискать",
                    "organizations": [1, 2, 4],
                    "history_text": "Поискал",
                    "type": "search",
                },
            ],
        },
    }

    jmessage = json.dumps(message, default=lambda x: x.__dict__)
    topic_writer.write_one.assert_called_with(str.encode(jmessage))


@pytest.mark.parametrize("campaign_type", UNSUPPORTED_CAMPAIGN_TYPES)
async def test_client_does_not_send_direct_moderation_data_for_other_campaigns(
    config, direct_moderation_client, topic_writer, campaign_type
):
    campaign = {
        "id": 1,
        "author_id": 123,
        "created_datetime": dt("2020-01-01 00:00:00"),
        "name": "campaign0",
        "timezone": "UTC",
        "comment": "",
        "user_display_limit": None,
        "user_daily_display_limit": None,
        "start_datetime": dt("2020-01-01 00:00:00"),
        "end_datetime": dt("2020-01-01 00:00:00"),
        "targeting": {},
        "publication_envs": [PublicationEnvEnum.DATA_TESTING],
        "campaign_type": campaign_type,
        "platforms": [PlatformEnum.NAVI],
        "rubric": None,
        "order_size": None,
        "order_id": 10,
        "manul_order_id": None,
        "billing": {
            "cpm": {
                "cost": Decimal("12.3456"),
                "budget": Decimal("1000"),
                "daily_budget": Decimal("5000"),
                "auto_daily_budget": False,
            }
        },
        "actions": [
            {"type_": "open_site", "title": "Перейти", "url": "ya.ru"},
            {"type_": "phone_call", "title": "Позвонить", "phone": "322-223"},
            {
                "type_": "search",
                "title": "Поискать",
                "organizations": [1, 2, 4],
                "history_text": "Поискал",
            },
        ],
        "creatives": [
            {
                "type_": "banner",
                "images": [{"file1": "filename1"}, {"file2": "filename2"}],
                "disclaimer": "Дисклеймер",
                "show_ads_label": True,
                "description": "Описание",
                "title": "Заголовок",
                "terms": "",
            }
        ],
        "placing": {"organizations": {"permalinks": [123, 345]}},
        "week_schedule": [],
        "status": CampaignStatusEnum.DRAFT,
        "discounts": [],
        "datatesting_expires_at": None,
        "moderation_verdicts": None,
    }

    moderation = {
        "id": 1,
        "created_at": dt("2020-01-01 00:00:00"),
        "campaign_id": 1,
        "reviewer_uid": None,
        "status": CampaignDirectModerationStatusEnum.NEW,
        "workflow": CampaignDirectModerationWorkflowEnum.COMMON,
        "verdicts": [],
    }

    async with direct_moderation_client:
        await direct_moderation_client.send_campaign_moderation(campaign, moderation, 1)

    topic_writer.write_one.assert_not_called()


async def test_client_does_not_send_direct_moderation_data_if_resolve_uri_action_present(
    direct_moderation_client, topic_writer
):
    campaign = {
        "id": 1,
        "author_id": 123,
        "created_datetime": dt("2020-01-01 00:00:00"),
        "name": "campaign0",
        "timezone": "UTC",
        "comment": "",
        "user_display_limit": None,
        "user_daily_display_limit": None,
        "start_datetime": dt("2020-01-01 00:00:00"),
        "end_datetime": dt("2020-01-01 00:00:00"),
        "targeting": {},
        "publication_envs": [PublicationEnvEnum.DATA_TESTING],
        "campaign_type": CampaignTypeEnum.ZERO_SPEED_BANNER,
        "platforms": [PlatformEnum.NAVI],
        "rubric": None,
        "order_size": None,
        "order_id": 10,
        "manul_order_id": None,
        "billing": {
            "cpm": {
                "cost": Decimal("12.3456"),
                "budget": Decimal("1000"),
                "daily_budget": Decimal("5000"),
                "auto_daily_budget": False,
            }
        },
        "actions": [
            {"type_": "open_site", "title": "Перейти", "url": "ya.ru"},
            {"type_": "phone_call", "title": "Позвонить", "phone": "322-223"},
            {
                "type_": "search",
                "title": "Поискать",
                "organizations": [1, 2, 4],
                "history_text": "Поискал",
            },
            {
                "type_": "resolve_uri",
                "uri": "magic://url",
                "action_type": ActionTypeEnum.OPEN_SITE,
                "target": ResolveUriTargetEnum.WEB_VIEW,
                "dialog": None,
            },
        ],
        "creatives": [
            {
                "type_": "banner",
                "images": [
                    {
                        "type": "source_image",
                        "group_id": "1111",
                        "image_name": "name1",
                        "alias_template": "banner_320x64_x4",
                    },
                    {
                        "type": "banner",
                        "group_id": "2222",
                        "image_name": "name2",
                        "alias_template": "banner_320x64_{size}",
                    },
                ],
                "disclaimer": "Дисклеймер",
                "show_ads_label": True,
                "description": "Описание",
                "title": "Заголовок",
                "terms": "",
            }
        ],
        "placing": {"organizations": {"permalinks": [123, 345]}},
        "week_schedule": [],
        "status": CampaignStatusEnum.DRAFT,
        "discounts": [],
        "datatesting_expires_at": None,
        "moderation_verdicts": None,
    }

    moderation = {
        "id": 1,
        "created_at": dt("2020-01-01 00:00:00"),
        "campaign_id": 1,
        "reviewer_uid": None,
        "status": CampaignDirectModerationStatusEnum.NEW,
        "workflow": CampaignDirectModerationWorkflowEnum.COMMON,
        "verdicts": [],
    }

    async with direct_moderation_client:
        await direct_moderation_client.send_campaign_moderation(campaign, moderation, 1)

    topic_writer.write_one.assert_not_called()
