import pytest

from maps_adv.adv_store.api.schemas.enums import (
    CampaignDirectModerationStatusEnum,
    CampaignDirectModerationWorkflowEnum,
    CampaignStatusEnum,
)
from maps_adv.adv_store.v2.lib.domains.moderation import ModerationDomain
from maps_adv.billing_proxy.client.lib.enums import Currency
from maps_adv.common.helpers import dt
from maps_adv.common.helpers.enums import CampaignTypeEnum

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.mock_dm,
    pytest.mark.mock_direct_moderation_client,
]

SUPPORTED_CAMPAIGN_TYPES = [CampaignTypeEnum.ZERO_SPEED_BANNER]
UNSUPPORTED_CAMPAIGN_TYPES = list(set(CampaignTypeEnum) - set(SUPPORTED_CAMPAIGN_TYPES))


@pytest.mark.parametrize("campaign_type", SUPPORTED_CAMPAIGN_TYPES)
async def test_calls_direct_moderation_client_and_update_moderation_status(
    moderation_domain,
    moderation_dm,
    campaigns_dm,
    direct_moderation_client,
    billing_proxy_client,
    campaign_type,
):

    moderation = {
        "id": 1,
        "created_at": dt("2020-01-01 00:00:00"),
        "campaign_id": 2,
        "reviewer_uid": None,
        "status": CampaignDirectModerationStatusEnum.NEW,
        "workflow": CampaignDirectModerationWorkflowEnum.COMMON,
        "verdicts": [],
    }

    moderation_dm.retrieve_direct_moderations_by_status.coro.return_value = [moderation]
    campaign = {
        "id": 2,
        "order_id": 5,
        "status": CampaignStatusEnum.REVIEW,
        "campaign_type": campaign_type,
        "creatives": [
            {
                "type_": "banner",
                "images": [
                    {
                        "type": "source_image",
                        "group_id": "65726",
                        "image_name": "2a000001732535aa60da41b677edf6652a5a",
                        "alias_template": "banner_x4",
                    },
                    {
                        "type": "banner",
                        "group_id": "65726",
                        "image_name": "2a000001732535ac2e4592fa1166b3ab6f1d",
                        "alias_template": "banner_{size}",
                    },
                ],
                "disclaimer": "Дисклеймер",
                "show_ads_label": True,
                "description": "Описание",
                "title": "Заголовок",
                "terms": "",
            }
        ],
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
        "comment": "",
    }
    campaigns_dm.retrieve_campaign.coro.return_value = campaign
    billing_proxy_client.fetch_order.coro.return_value = {
        "id": 5,
        "currency": Currency.RUB,
    }

    await moderation_domain.process_new_moderations()

    direct_moderation_client.send_campaign_moderation.assert_called_with(
        campaign, moderation, 225
    )

    moderation_dm.update_direct_moderation.assert_called_with(
        1, CampaignDirectModerationStatusEnum.PROCESSING
    )


@pytest.mark.parametrize(
    ["currency", "geo_id"],
    [
        (Currency.RUB, 225),
        (Currency.BYN, 149),
        (Currency.KZT, 159),
        (Currency.TRY, 983),
        (Currency.EUR, 111),
        (Currency.USD, 84),
        (None, 318),
    ],
)
async def test_correctly_determines_geo_id(
    moderation_domain,
    moderation_dm,
    campaigns_dm,
    direct_moderation_client,
    billing_proxy_client,
    currency,
    geo_id,
):

    moderation = {
        "id": 1,
        "created_at": dt("2020-01-01 00:00:00"),
        "campaign_id": 2,
        "reviewer_uid": None,
        "status": CampaignDirectModerationStatusEnum.NEW,
        "workflow": CampaignDirectModerationWorkflowEnum.COMMON,
        "verdicts": [],
    }

    moderation_dm.retrieve_direct_moderations_by_status.coro.return_value = [moderation]
    campaign = {
        "id": 2,
        "order_id": 5,
        "status": CampaignStatusEnum.REVIEW,
        "campaign_type": CampaignTypeEnum.ZERO_SPEED_BANNER,
        "creatives": [
            {
                "type_": "banner",
                "images": [
                    {
                        "type": "source_image",
                        "group_id": "65726",
                        "image_name": "2a000001732535aa60da41b677edf6652a5a",
                        "alias_template": "banner_x4",
                    },
                    {
                        "type": "banner",
                        "group_id": "65726",
                        "image_name": "2a000001732535ac2e4592fa1166b3ab6f1d",
                        "alias_template": "banner_{size}",
                    },
                ],
                "disclaimer": "Дисклеймер",
                "show_ads_label": True,
                "description": "Описание",
                "title": "Заголовок",
                "terms": "",
            }
        ],
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
        "comment": "",
    }
    campaigns_dm.retrieve_campaign.coro.return_value = campaign
    billing_proxy_client.fetch_order.coro.return_value = {"id": 5, "currency": currency}

    await moderation_domain.process_new_moderations()

    direct_moderation_client.send_campaign_moderation.assert_called_with(
        campaign, moderation, geo_id
    )

    moderation_dm.update_direct_moderation.assert_called_with(
        1, CampaignDirectModerationStatusEnum.PROCESSING
    )


@pytest.mark.parametrize("campaign_type", UNSUPPORTED_CAMPAIGN_TYPES)
async def test_does_not_call_direct_moderation_client_but_update_moderation_status(
    moderation_domain,
    moderation_dm,
    campaigns_dm,
    direct_moderation_client,
    campaign_type,
):
    moderation = {
        "id": 1,
        "created_at": dt("2020-01-01 00:00:00"),
        "campaign_id": 2,
        "reviewer_uid": None,
        "status": CampaignDirectModerationStatusEnum.NEW,
        "workflow": CampaignDirectModerationWorkflowEnum.COMMON,
        "verdicts": [],
    }

    moderation_dm.retrieve_direct_moderations_by_status.coro.return_value = [moderation]
    campaign = {
        "id": 2,
        "order_id": 1,
        "status": CampaignStatusEnum.REVIEW,
        "campaign_type": campaign_type,
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
        "comment": "",
    }
    campaigns_dm.retrieve_campaign.coro.return_value = campaign

    await moderation_domain.process_new_moderations()

    direct_moderation_client.send_campaign_moderation.assert_not_called()

    moderation_dm.update_direct_moderation.assert_called_with(
        1, CampaignDirectModerationStatusEnum.PROCESSING
    )


async def test_logging_error_if_no_exists_direct_client(
    moderation_dm, campaigns_dm, caplog
):
    await ModerationDomain(
        moderation_dm, campaigns_dm, None, None
    ).process_new_moderations()

    assert "Direct client not found" in caplog.messages


@pytest.mark.parametrize("campaign_type", SUPPORTED_CAMPAIGN_TYPES)
async def test_does_not_call_direct_moderation_client_for_manual_orders(
    moderation_domain,
    moderation_dm,
    campaigns_dm,
    direct_moderation_client,
    campaign_type,
):
    moderation = {
        "id": 1,
        "created_at": dt("2020-01-01 00:00:00"),
        "campaign_id": 2,
        "reviewer_uid": None,
        "status": CampaignDirectModerationStatusEnum.NEW,
        "workflow": CampaignDirectModerationWorkflowEnum.COMMON,
        "verdicts": [],
    }

    moderation_dm.retrieve_direct_moderations_by_status.coro.return_value = [moderation]
    campaign = {
        "id": 2,
        "manul_order_id": 1,
        "status": CampaignStatusEnum.REVIEW,
        "campaign_type": campaign_type,
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
        "comment": "",
    }
    campaigns_dm.retrieve_campaign.coro.return_value = campaign

    await moderation_domain.process_new_moderations()

    direct_moderation_client.send_campaign_moderation.assert_not_called()

    moderation_dm.update_direct_moderation.assert_called_with(
        1, CampaignDirectModerationStatusEnum.PROCESSING
    )


@pytest.mark.parametrize("campaign_type", SUPPORTED_CAMPAIGN_TYPES)
async def test_does_not_call_direct_moderation_client_if_comment_present(
    moderation_domain,
    moderation_dm,
    campaigns_dm,
    direct_moderation_client,
    campaign_type,
):
    moderation = {
        "id": 1,
        "created_at": dt("2020-01-01 00:00:00"),
        "campaign_id": 2,
        "reviewer_uid": None,
        "status": CampaignDirectModerationStatusEnum.NEW,
        "workflow": CampaignDirectModerationWorkflowEnum.COMMON,
        "verdicts": [],
    }

    moderation_dm.retrieve_direct_moderations_by_status.coro.return_value = [moderation]
    campaign = {
        "id": 2,
        "manul_order_id": 1,
        "status": CampaignStatusEnum.REVIEW,
        "campaign_type": campaign_type,
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
        "comment": "DO NOT START THIS CAMPAIGN",
    }
    campaigns_dm.retrieve_campaign.coro.return_value = campaign

    await moderation_domain.process_new_moderations()

    direct_moderation_client.send_campaign_moderation.assert_not_called()

    moderation_dm.update_direct_moderation.assert_called_with(
        1, CampaignDirectModerationStatusEnum.PROCESSING
    )
