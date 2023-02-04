import pytest

from maps_adv.common.helpers import dt
from maps_adv.common.helpers.enums import CampaignTypeEnum
from maps_adv.common.proto.campaign_pb2 import CampaignType
from maps_adv.statistics.dashboard.proto import campaign_stat_pb2

pytestmark = [pytest.mark.asyncio]


@pytest.mark.mock_dm
async def test_returns_expected(dm, api_provider):
    dm.calculate_campaigns_events_for_period.coro.return_value = {
        10: 10,
        20: 12,
        30: 13,
    }
    pb_input = campaign_stat_pb2.CampaignEventsForPeriodInput(
        campaigns=[
            campaign_stat_pb2.CampaignEventsInputPart(
                campaign_id=10, campaign_type=CampaignType.PIN_ON_ROUTE
            ),
            campaign_stat_pb2.CampaignEventsInputPart(
                campaign_id=20, campaign_type=CampaignType.PIN_ON_ROUTE
            ),
            campaign_stat_pb2.CampaignEventsInputPart(
                campaign_id=30, campaign_type=CampaignType.PIN_ON_ROUTE
            ),
        ],
        period_from=dt("2020-12-01 00:00:00", as_proto=True),
        period_to=dt("2020-12-01 12:00:00", as_proto=True),
    )

    raw_got = await api_provider.calculate_campaigns_events_for_period(
        pb_input.SerializeToString()
    )

    dm.calculate_campaigns_events_for_period.assert_called_with(
        events_query=[
            (10, CampaignTypeEnum.PIN_ON_ROUTE),
            (20, CampaignTypeEnum.PIN_ON_ROUTE),
            (30, CampaignTypeEnum.PIN_ON_ROUTE),
        ],
        period_from=dt("2020-12-01 00:00:00"),
        period_to=dt("2020-12-01 12:00:00"),
    )

    got = campaign_stat_pb2.CampaignEventsForPeriodOutput.FromString(raw_got)

    assert got == campaign_stat_pb2.CampaignEventsForPeriodOutput(
        campaigns_events=[
            campaign_stat_pb2.CampaignEvents(campaign_id=10, events=10),
            campaign_stat_pb2.CampaignEvents(campaign_id=20, events=12),
            campaign_stat_pb2.CampaignEvents(campaign_id=30, events=13),
        ]
    )
