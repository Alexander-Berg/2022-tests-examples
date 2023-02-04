from operator import attrgetter

import pytest

from maps_adv.common.helpers import dt
from maps_adv.common.proto import campaign_pb2
from maps_adv.statistics.dashboard.proto import campaign_stat_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("fill_ch")]

url = "/statistics/campaigns/events/"


async def test_returns_200(api):
    pb_input = campaign_stat_pb2.CampaignEventsForPeriodInput(
        campaigns=[
            campaign_stat_pb2.CampaignEventsInputPart(
                campaign_id=10, campaign_type=campaign_pb2.CampaignType.PIN_ON_ROUTE
            ),
            campaign_stat_pb2.CampaignEventsInputPart(
                campaign_id=20, campaign_type=campaign_pb2.CampaignType.PIN_ON_ROUTE
            ),
        ],
        period_from=dt("2020-12-01 00:00:00", as_proto=True),
        period_to=dt("2020-12-01 12:00:00", as_proto=True),
    )

    await api.post(url, proto=pb_input, expected_status=200)


@pytest.mark.parametrize(
    "campaign_ids, expected",
    (
        [[10], [campaign_stat_pb2.CampaignEvents(campaign_id=10, events=4)]],
        [[20], [campaign_stat_pb2.CampaignEvents(campaign_id=20, events=3)]],
        [
            [10, 20],
            [
                campaign_stat_pb2.CampaignEvents(campaign_id=10, events=4),
                campaign_stat_pb2.CampaignEvents(campaign_id=20, events=3),
            ],
        ],
        [
            [10, 101],
            [
                campaign_stat_pb2.CampaignEvents(campaign_id=10, events=4),
                campaign_stat_pb2.CampaignEvents(campaign_id=101, events=0),
            ],
        ],
    ),
)
async def test_returns_statistic_for_one_campaign(campaign_ids, expected, api):
    sort_key = attrgetter("campaign_id")
    pb_input = campaign_stat_pb2.CampaignEventsForPeriodInput(
        campaigns=list(
            map(
                lambda id: campaign_stat_pb2.CampaignEventsInputPart(
                    campaign_id=id, campaign_type=campaign_pb2.CampaignType.PIN_ON_ROUTE
                ),
                campaign_ids,
            )
        ),
        period_from=dt("2019-01-01 00:00:00", as_proto=True),
        period_to=dt("2019-01-01 18:00:00", as_proto=True),
    )

    got = await api.post(
        url,
        proto=pb_input,
        decode_as=campaign_stat_pb2.CampaignEventsForPeriodOutput,
        expected_status=200,
    )

    got.campaigns_events.sort(key=sort_key)

    assert got == campaign_stat_pb2.CampaignEventsForPeriodOutput(
        campaigns_events=sorted(expected, key=sort_key)
    )


@pytest.mark.parametrize("data", (b"", None))
async def test_empty_payload(data, api):
    got = await api.post(
        url,
        data=data,
        decode_as=campaign_stat_pb2.CampaignEventsForPeriodOutput,
        expected_status=200,
    )

    assert got == campaign_stat_pb2.CampaignEventsForPeriodOutput(campaigns_events=[])


@pytest.mark.parametrize(
    ["period_from", "period_to", "events"],
    [
        (None, None, 2),
        (None, dt("2019-01-01 12:20:00", as_proto=True), 3),
        (dt("2019-01-01 12:01:00", as_proto=True), None, 1),
    ],
)
async def test_limits_events_by_receive_timestamp(
    period_from, period_to, events, api, freezer
):
    freezer.move_to(dt("2019-01-01 12:10:00"))

    pb_input = campaign_stat_pb2.CampaignEventsForPeriodInput(
        campaigns=[
            campaign_stat_pb2.CampaignEventsInputPart(
                campaign_id=30, campaign_type=campaign_pb2.CampaignType.PIN_ON_ROUTE
            )
        ],
        period_from=period_from,
        period_to=period_to,
    )

    got = await api.post(
        url,
        proto=pb_input,
        decode_as=campaign_stat_pb2.CampaignEventsForPeriodOutput,
        expected_status=200,
    )

    assert got == campaign_stat_pb2.CampaignEventsForPeriodOutput(
        campaigns_events=[
            campaign_stat_pb2.CampaignEvents(campaign_id=30, events=events)
        ]
    )
