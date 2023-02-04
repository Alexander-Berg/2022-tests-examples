import logging
from decimal import Decimal
from operator import attrgetter

import pytest

from maps_adv.common.helpers import dt
from maps_adv.statistics.dashboard.proto import campaign_stat_pb2, error_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("fill_ch")]

url = "/statistics/campaigns/charged_sum/"


async def test_returns_nothing_if_no_campaigns_found(api):
    pb_input = campaign_stat_pb2.CampaignChargedSumInput(
        campaign_ids=[110, 120, 130], on_timestamp=1546603800
    )

    await api.post(url, proto=pb_input, expected_status=204)


async def test_returns_200(api):
    pb_input = campaign_stat_pb2.CampaignChargedSumInput(
        campaign_ids=[10, 20], on_timestamp=1546603800
    )

    await api.post(url, proto=pb_input, expected_status=200)


async def test_returns_200_without_timestamp_field(api):
    pb_input = campaign_stat_pb2.CampaignChargedSumInput(campaign_ids=[10, 20])

    await api.post(url, proto=pb_input, expected_status=200)


@pytest.mark.parametrize(
    "campaign_ids, expected",
    (
        [
            [10],
            [campaign_stat_pb2.CampaignChargedSum(campaign_id=10, charged_sum="1.1")],
        ],
        [[20], [campaign_stat_pb2.CampaignChargedSum(campaign_id=20, charged_sum="5")]],
        [
            [10, 20],
            [
                campaign_stat_pb2.CampaignChargedSum(campaign_id=10, charged_sum="1.1"),
                campaign_stat_pb2.CampaignChargedSum(campaign_id=20, charged_sum="5"),
            ],
        ],
    ),
)
async def test_returns_statistic_for_one_campaign(campaign_ids, expected, api):
    sort_key = attrgetter("campaign_id")
    pb_input = campaign_stat_pb2.CampaignChargedSumInput(
        campaign_ids=campaign_ids, on_timestamp=1546603800
    )

    got = await api.post(
        url,
        proto=pb_input,
        decode_as=campaign_stat_pb2.CampaignChargedSumOutput,
        expected_status=200,
    )
    got.campaigns_charged_sums.sort(key=sort_key)

    assert got == campaign_stat_pb2.CampaignChargedSumOutput(
        campaigns_charged_sums=sorted(expected, key=sort_key)
    )


@pytest.mark.parametrize("data", (b"", None))
async def test_errored_for_wrong_payload(data, api):
    got = await api.post(url, data=data, decode_as=error_pb2.Error, expected_status=400)

    assert got == error_pb2.Error(code=error_pb2.Error.NO_CAMPAIGNS_PASSED)


async def test_round_cost_to_two_decimal_places(api):
    pb_input = campaign_stat_pb2.CampaignChargedSumInput(
        campaign_ids=[30], on_timestamp=1546603800
    )

    got = await api.post(
        url,
        proto=pb_input,
        decode_as=campaign_stat_pb2.CampaignChargedSumOutput,
        expected_status=200,
    )

    assert got == campaign_stat_pb2.CampaignChargedSumOutput(
        campaigns_charged_sums=[
            campaign_stat_pb2.CampaignChargedSum(campaign_id=30, charged_sum="0.37")
        ]
    )


@pytest.mark.xfail
@pytest.mark.mock_dm
async def test_serialization_errors_are_logged(dm, api, caplog):
    dm.calculate_campaigns_charged_sum.coro.return_value = [
        {"campaign_id": 10, "charged_sum": Decimal("0.7777777")},
        {"campaign_id": 20, "charged_sum": Decimal("1.1")},
    ]
    pb_input = campaign_stat_pb2.CampaignChargedSumInput(
        campaign_ids=[10, 20], on_timestamp=1546603800
    )

    await api.post(url, proto=pb_input, expected_status=400)

    assert (
        "maps_adv.statistics.dashboard.server.lib.api",
        logging.ERROR,
        "Serialization error: {'campaigns_charged_sums': {0: {'charged_sum': "
        '["Value has 7 fraction digits, but only 2 can\'t be serialized"]}}}',
    ) in caplog.record_tuples


@pytest.mark.parametrize(
    ["on_timestamp", "expected_sum"],
    [
        (int(dt("2019-01-01 12:20:00").timestamp()), "0.37"),
        (int(dt("2019-01-01 12:20:00").timestamp()) - 1, "0.24"),
    ],
)
async def test_limits_events_by_receive_timestamp(on_timestamp, expected_sum, api):
    pb_input = campaign_stat_pb2.CampaignChargedSumInput(
        campaign_ids=[30], on_timestamp=on_timestamp
    )

    got = await api.post(
        url,
        proto=pb_input,
        decode_as=campaign_stat_pb2.CampaignChargedSumOutput,
        expected_status=200,
    )

    assert got == campaign_stat_pb2.CampaignChargedSumOutput(
        campaigns_charged_sums=[
            campaign_stat_pb2.CampaignChargedSum(
                campaign_id=30, charged_sum=expected_sum
            )
        ]
    )
