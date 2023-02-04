from maps_adv.common.helpers.enums import CampaignTypeEnum
from unittest.mock import call
import logging

import pytest

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.mock_dm,
    pytest.mark.mock_adv_store_client,
]

NORMALIZED_CAMPAIGNS = {
    10: {"action_make_route": 0, "billboard_show": 100},
    20: {"action_make_route": 0, "billboard_show": 100},
    30: {"action_make_route": 10, "billboard_show": 100},
    40: {"action_make_route": 100, "billboard_show": 100},
    50: {"action_make_route": 100, "billboard_show": 100},
    60: {"action_make_route": 100, "billboard_show": 100},
    70: {"action_make_route": 100, "billboard_show": 100},
    80: {"action_make_route": 0, "billboard_show": 100},
}

PROCESSED_CAMPAIGNS = {
    10: {"action_make_route": 0, "billboard_show": 70},
    20: {"action_make_route": 0, "billboard_show": 99},
    30: {"action_make_route": 9, "billboard_show": 70},
    40: {"action_make_route": 89, "billboard_show": 100},
    60: {"action_make_route": 0, "billboard_show": 0},
    70: {"action_make_route": 0, "billboard_show": 0},
    80: {"action_make_route": 0, "billboard_show": 80},
}

CAMPAIGN_TYPES = [
    {"campaign_id": 10, "campaign_type": CampaignTypeEnum.BILLBOARD},
    {"campaign_id": 20, "campaign_type": CampaignTypeEnum.BILLBOARD},
    {"campaign_id": 30, "campaign_type": CampaignTypeEnum.VIA_POINTS},
    {"campaign_id": 40, "campaign_type": CampaignTypeEnum.VIA_POINTS},
    {"campaign_id": 50, "campaign_type": CampaignTypeEnum.PIN_ON_ROUTE},
    {"campaign_id": 60, "campaign_type": CampaignTypeEnum.CATEGORY_SEARCH},
    {"campaign_id": 80, "campaign_type": CampaignTypeEnum.BILLBOARD},
]

OVERDRAFT_OK_CALLS = [
    call(
        description="",
        service="campaign_overdraft:PIN_ON_ROUTE",
        status="OK",
    ),
    call(
        description="",
        service="campaign_overdraft:BILLBOARD",
        status="OK",
    ),
    call(
        description="",
        service="campaign_overdraft:ZERO_SPEED_BANNER",
        status="OK",
    ),
    call(
        description="",
        service="campaign_overdraft:CATEGORY_SEARCH",
        status="OK",
    ),
    call(description="", service="campaign_overdraft:ROUTE_BANNER", status="OK"),
    call(
        description="",
        service="campaign_overdraft:VIA_POINTS",
        status="OK",
    ),
    call(
        description="",
        service="campaign_overdraft:OVERVIEW_BANNER",
        status="OK",
    ),
    call(description="", service="campaign_overdraft:PROMOCODE", status="OK"),
]

DOUBLE_SPENT_OK_CALLS = [
    call(
        description="",
        service="campaign_double_spent:PIN_ON_ROUTE",
        status="OK",
    ),
    call(
        description="",
        service="campaign_double_spent:BILLBOARD",
        status="OK",
    ),
    call(
        description="",
        service="campaign_double_spent:ZERO_SPEED_BANNER",
        status="OK",
    ),
    call(
        description="",
        service="campaign_double_spent:CATEGORY_SEARCH",
        status="OK",
    ),
    call(description="", service="campaign_double_spent:ROUTE_BANNER", status="OK"),
    call(
        description="",
        service="campaign_double_spent:VIA_POINTS",
        status="OK",
    ),
    call(
        description="",
        service="campaign_double_spent:OVERVIEW_BANNER",
        status="OK",
    ),
    call(description="", service="campaign_double_spent:PROMOCODE", status="OK"),
]


async def test_reports_overdraft(
    adv_store_client, dm, domain, juggler_client_mock, caplog
):
    adv_store_client.retrieve_campaign_data_for_monitorings.coro.return_value = (
        CAMPAIGN_TYPES
    )
    dm.get_aggregated_normalized_events_by_campaign.coro.return_value = (
        NORMALIZED_CAMPAIGNS
    )
    dm.get_aggregated_processed_events_by_campaign.coro.return_value = (
        PROCESSED_CAMPAIGNS
    )
    dm.get_aggregated_mapkit_events_by_campaign.coro.return_value = NORMALIZED_CAMPAIGNS

    await domain.check_overdraft()

    juggler_client_mock.__call__.coro.assert_has_calls(
        [
            call(
                description="Campaigns: 50(100.0%)",
                service="campaign_overdraft:PIN_ON_ROUTE",
                status="CRIT",
            ),
            call(
                description="Campaigns: 10(30.0%), 80(20.0%)",
                service="campaign_overdraft:BILLBOARD",
                status="CRIT",
            ),
            call(
                description="",
                service="campaign_overdraft:ZERO_SPEED_BANNER",
                status="OK",
            ),
            call(
                description="",
                service="campaign_overdraft:CATEGORY_SEARCH",
                status="OK",
            ),
            call(
                description="", service="campaign_overdraft:ROUTE_BANNER", status="OK"
            ),
            call(
                description="Campaigns: 40(11.0%)",
                service="campaign_overdraft:VIA_POINTS",
                status="CRIT",
            ),
            call(
                description="",
                service="campaign_overdraft:OVERVIEW_BANNER",
                status="OK",
            ),
            call(description="", service="campaign_overdraft:PROMOCODE", status="OK"),
            *DOUBLE_SPENT_OK_CALLS,
        ],
        any_order=True,
    )
    assert [
        record
        for record in caplog.record_tuples
        if record[0] == "dashboard.check_overdraft"
    ] == [
        (
            "dashboard.check_overdraft",
            logging.WARN,
            "Campaign overdraft for type PIN_ON_ROUTE, campaigns: 50(100.0%)",
        ),
        (
            "dashboard.check_overdraft",
            logging.WARN,
            "Campaign overdraft for type BILLBOARD, campaigns: 10(30.0%), 80(20.0%)",
        ),
        (
            "dashboard.check_overdraft",
            logging.WARN,
            "Campaign overdraft for type VIA_POINTS, campaigns: 40(11.0%)",
        ),
    ]


async def test_only_logs_if_not_enough_overdraft(
    adv_store_client, dm, domain, juggler_client_mock, caplog
):
    adv_store_client.retrieve_campaign_data_for_monitorings.coro.return_value = [
        {"campaign_id": 10, "campaign_type": CampaignTypeEnum.BILLBOARD},
        {"campaign_id": 20, "campaign_type": CampaignTypeEnum.BILLBOARD},
    ]
    dm.get_aggregated_normalized_events_by_campaign.coro.return_value = {
        10: {"action_make_route": 0, "billboard_show": 100},
        20: {"action_make_route": 0, "billboard_show": 10000},
    }
    dm.get_aggregated_processed_events_by_campaign.coro.return_value = {
        10: {"action_make_route": 0, "billboard_show": 70},
        20: {"action_make_route": 0, "billboard_show": 9999},
    }
    dm.get_aggregated_mapkit_events_by_campaign.coro.return_value = {
        10: {"action_make_route": 0, "billboard_show": 100},
        20: {"action_make_route": 0, "billboard_show": 10000},
    }

    await domain.check_overdraft()

    juggler_client_mock.__call__.coro.assert_has_calls(
        [*OVERDRAFT_OK_CALLS, *DOUBLE_SPENT_OK_CALLS],
        any_order=True,
    )
    assert [
        record
        for record in caplog.record_tuples
        if record[0] == "dashboard.check_overdraft"
    ] == [
        (
            "dashboard.check_overdraft",
            logging.WARN,
            "Campaign overdraft for type BILLBOARD, campaigns: 10(30.0%)",
        ),
    ]


async def test_reports_double_spent(
    adv_store_client, dm, domain, juggler_client_mock, caplog
):
    adv_store_client.retrieve_campaign_data_for_monitorings.coro.return_value = [
        {"campaign_id": 10, "campaign_type": CampaignTypeEnum.BILLBOARD},
        {"campaign_id": 20, "campaign_type": CampaignTypeEnum.BILLBOARD},
    ]
    dm.get_aggregated_mapkit_events_by_campaign.coro.return_value = {
        10: {"action_make_route": 0, "billboard_show": 100},
        20: {"action_make_route": 0, "billboard_show": 10000},
    }
    dm.get_aggregated_normalized_events_by_campaign.coro.return_value = {
        10: {"action_make_route": 0, "billboard_show": 101},
        20: {"action_make_route": 0, "billboard_show": 10000},
    }
    dm.get_aggregated_processed_events_by_campaign.coro.return_value = {
        10: {"action_make_route": 0, "billboard_show": 100},
        20: {"action_make_route": 0, "billboard_show": 10001},
    }

    await domain.check_overdraft()

    juggler_client_mock.__call__.coro.assert_has_calls(
        [
            *OVERDRAFT_OK_CALLS,
            call(
                description="",
                service="campaign_double_spent:PIN_ON_ROUTE",
                status="OK",
            ),
            call(
                description="Campaigns: 10(mapkit: 100, normalized: 101, processed: 100),"
                " 20(mapkit: 10000, normalized: 10000, processed: 10001)",
                service="campaign_double_spent:BILLBOARD",
                status="CRIT",
            ),
            call(
                description="",
                service="campaign_double_spent:ZERO_SPEED_BANNER",
                status="OK",
            ),
            call(
                description="",
                service="campaign_double_spent:CATEGORY_SEARCH",
                status="OK",
            ),
            call(
                description="",
                service="campaign_double_spent:ROUTE_BANNER",
                status="OK",
            ),
            call(
                description="",
                service="campaign_double_spent:VIA_POINTS",
                status="OK",
            ),
            call(
                description="",
                service="campaign_double_spent:OVERVIEW_BANNER",
                status="OK",
            ),
            call(
                description="", service="campaign_double_spent:PROMOCODE", status="OK"
            ),
        ],
        any_order=True,
    )
    assert [
        record
        for record in caplog.record_tuples
        if record[0] == "dashboard.check_double_spent"
    ] == [
        (
            "dashboard.check_double_spent",
            logging.WARN,
            "Campaign double spent for type BILLBOARD, campaigns: 10(mapkit: 100, normalized: 101, processed: 100),"
            " 20(mapkit: 10000, normalized: 10000, processed: 10001)",
        ),
    ]
