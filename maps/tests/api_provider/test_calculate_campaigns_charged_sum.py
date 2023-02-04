from decimal import Decimal

import pytest

from maps_adv.statistics.dashboard.proto import campaign_stat_pb2
from maps_adv.statistics.dashboard.server.lib.api_provider import NoCampaignsPassed

pytestmark = [pytest.mark.asyncio]


@pytest.mark.mock_dm
async def test_returns_expected(dm, api_provider):
    dm.calculate_campaigns_charged_sum.coro.return_value = [
        {"campaign_id": 10, "charged_sum": Decimal("0.7")},
        {"campaign_id": 20, "charged_sum": Decimal("3.71")},
        {"campaign_id": 30, "charged_sum": Decimal("0.01")},
    ]
    pb_input = campaign_stat_pb2.CampaignChargedSumInput(campaign_ids=[10, 20, 30])

    raw_got = await api_provider.calculate_campaigns_charged_sum(
        pb_input.SerializeToString()
    )

    dm.calculate_campaigns_charged_sum.assert_called_with(campaign_ids=[10, 20, 30])

    got = campaign_stat_pb2.CampaignChargedSumOutput.FromString(raw_got)
    assert got == campaign_stat_pb2.CampaignChargedSumOutput(
        campaigns_charged_sums=[
            campaign_stat_pb2.CampaignChargedSum(campaign_id=10, charged_sum="0.7"),
            campaign_stat_pb2.CampaignChargedSum(campaign_id=20, charged_sum="3.71"),
            campaign_stat_pb2.CampaignChargedSum(campaign_id=30, charged_sum="0.01"),
        ]
    )


@pytest.mark.mock_dm
async def test_calls_with_expected_arguments(dm, api_provider):
    dm.calculate_campaigns_charged_sum.coro.return_value = [
        {"campaign_id": 10, "charged_sum": Decimal("0.7")},
        {"campaign_id": 20, "charged_sum": Decimal("3.71")},
        {"campaign_id": 30, "charged_sum": Decimal("0.01")},
    ]
    pb_input = campaign_stat_pb2.CampaignChargedSumInput(
        campaign_ids=[10, 20, 30], on_timestamp=123
    )

    await api_provider.calculate_campaigns_charged_sum(pb_input.SerializeToString())

    dm.calculate_campaigns_charged_sum.assert_called_with(
        campaign_ids=[10, 20, 30], on_timestamp=123
    )


@pytest.mark.mock_dm
async def test_raises_for_empty_campaign_ids(dm, api_provider):
    pb_input = campaign_stat_pb2.CampaignChargedSumInput(
        campaign_ids=[], on_timestamp=1546603800
    )

    with pytest.raises(NoCampaignsPassed):
        await api_provider.calculate_campaigns_charged_sum(pb_input.SerializeToString())
