from decimal import Decimal
from typing import List

import pytest

from maps_adv.adv_store.v2.lib.core.direct_moderation.schema import (
    DirectModerationDataZeroSpeedBanner,
    DirectModerationMeta,
    DirectModerationOutgoing,
)
from maps_adv.adv_store.api.schemas.enums import (
    CampaignDirectModerationStatusEnum,
    CampaignDirectModerationWorkflowEnum,
    CampaignStatusEnum,
    PlatformEnum,
    PublicationEnvEnum,
)
from maps_adv.common.helpers import Any, dt
from maps_adv.common.helpers.enums import CampaignTypeEnum

pytestmark = [pytest.mark.asyncio]


async def test_client_returns_direct_moderation_message_for_zero_speed_banner(
    direct_moderation_client,
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

    data = direct_moderation_client.create_moderation_message_for_zero_speed_banner(
        campaign, moderation
    )

    assert data == DirectModerationOutgoing(
        meta=DirectModerationMeta(1, 1),
        type=CampaignTypeEnum.ZERO_SPEED_BANNER,
        workflow=CampaignDirectModerationWorkflowEnum.COMMON,
        country_geo_id=225,
        verdictor_uid=None,
        data=DirectModerationDataZeroSpeedBanner(
            image="https://avatars.mds.yandex.net/get-geoadv-ext/1111/name1/banner_320x64_x4",  # noqa: E501
            disclaimer="Дисклеймер",
            title="Заголовок",
            description="Описание",
            terms="",
            actions=[
                {"title": "Перейти", "url": "ya.ru", "type": "open_site"},
                {"title": "Позвонить", "phone": "322-223", "type": "phone_call"},
                {
                    "title": "Поискать",
                    "organizations": [1, 2, 4],
                    "history_text": "Поискал",
                    "type": "search",
                },
            ],
        ),
    )


async def test_client_takes_only_some_orgs_if_there_are_many(direct_moderation_client):
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
            {
                "type_": "search",
                "title": "Поискать",
                "organizations": [x for x in range(100)],
                "history_text": "Поискал",
            }
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
        "placing": {"organizations": {"permalinks": [x for x in range(100)]}},
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

    data = direct_moderation_client.create_moderation_message_for_zero_speed_banner(
        campaign, moderation
    )

    assert data == DirectModerationOutgoing(
        meta=DirectModerationMeta(1, 1),
        type=CampaignTypeEnum.ZERO_SPEED_BANNER,
        workflow=CampaignDirectModerationWorkflowEnum.COMMON,
        country_geo_id=225,
        verdictor_uid=None,
        data=DirectModerationDataZeroSpeedBanner(
            image="https://avatars.mds.yandex.net/get-geoadv-ext/1111/name1/banner_320x64_x4",  # noqa: E501
            disclaimer="Дисклеймер",
            title="Заголовок",
            description="Описание",
            terms="",
            actions=[
                {
                    "title": "Поискать",
                    "organizations": Any(List),
                    "history_text": "Поискал",
                    "type": "search",
                }
            ],
        ),
    )

    assert len(data.data.actions[0]["organizations"]) == 5
