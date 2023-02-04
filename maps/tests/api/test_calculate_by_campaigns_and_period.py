import logging
from datetime import date
from decimal import Decimal

import pytest

from maps_adv.common.helpers import dt
from maps_adv.statistics.dashboard.proto import campaigns_stat_pb2, error_pb2
from maps_adv.statistics.dashboard.server.tests import make_event

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("fill_ch")]

url = "/statistics/campaigns/"


async def test_returns_nothing_if_no_campaigns_found(api):
    pb_input = campaigns_stat_pb2.CampaignsStatInput(
        campaign_ids=[110, 120, 130],
        period_from=dt("2019-01-01 00:00:00", as_proto=True),
        period_to=dt("2019-01-01 23:59:59", as_proto=True),
    )

    await api.post(url, proto=pb_input, expected_status=204)


async def test_returns_200(api):
    pb_input = campaigns_stat_pb2.CampaignsStatInput(
        campaign_ids=[10, 20],
        period_from=dt("2019-01-01 00:00:00", as_proto=True),
        period_to=dt("2019-01-01 23:59:59", as_proto=True),
    )

    await api.post(url, proto=pb_input, expected_status=200)


async def test_returns_statistic_for_one_campaign(api):
    pb_input = campaigns_stat_pb2.CampaignsStatInput(
        campaign_ids=[10],
        period_from=dt("2019-01-01 00:00:00", as_proto=True),
        period_to=dt("2019-01-01 23:59:59", as_proto=True),
    )

    got = await api.post(
        url,
        proto=pb_input,
        decode_as=campaigns_stat_pb2.CampaignsStatOutput,
        expected_status=200,
    )

    assert got == campaigns_stat_pb2.CampaignsStatOutput(
        by_dates=[
            campaigns_stat_pb2.CampaignsStatOnDate(
                date="2019-01-01",
                details=campaigns_stat_pb2.CampaignsStatDetails(
                    call=1,
                    makeRoute=1,
                    openSite=0,
                    saveOffer=0,
                    search=2,
                    show=7,
                    tap=4,
                    ctr=0.5714,
                    clicks_to_routes=0.25,
                    charged_sum="0.7",
                    show_unique=3,
                ),
            )
        ],
        total=campaigns_stat_pb2.CampaignsStatDetails(
            call=1,
            makeRoute=1,
            openSite=0,
            saveOffer=0,
            search=2,
            show=7,
            tap=4,
            ctr=0.5714,
            clicks_to_routes=0.25,
            charged_sum="0.7",
            show_unique=3,
        ),
    )


async def test_returns_statistic_for_many_campaigns(api):
    pb_input = campaigns_stat_pb2.CampaignsStatInput(
        campaign_ids=[10, 20],
        period_from=dt("2019-01-01 00:00:00", as_proto=True),
        period_to=dt("2019-01-01 23:59:59", as_proto=True),
    )

    got = await api.post(
        url,
        proto=pb_input,
        decode_as=campaigns_stat_pb2.CampaignsStatOutput,
        expected_status=200,
    )

    assert got == campaigns_stat_pb2.CampaignsStatOutput(
        by_dates=[
            campaigns_stat_pb2.CampaignsStatOnDate(
                date="2019-01-01",
                details=campaigns_stat_pb2.CampaignsStatDetails(
                    call=1,
                    makeRoute=1,
                    openSite=2,
                    saveOffer=1,
                    search=2,
                    show=12,
                    tap=7,
                    ctr=0.5833,
                    clicks_to_routes=0.1428,
                    charged_sum="5.7",
                    show_unique=6,
                ),
            )
        ],
        total=campaigns_stat_pb2.CampaignsStatDetails(
            call=1,
            makeRoute=1,
            openSite=2,
            saveOffer=1,
            search=2,
            show=12,
            tap=7,
            ctr=0.5833,
            clicks_to_routes=0.1428,
            charged_sum="5.7",
            show_unique=6,
        ),
    )


async def test_returns_statistic_for_many_days(api):
    pb_input = campaigns_stat_pb2.CampaignsStatInput(
        campaign_ids=[10],
        period_from=dt("2019-01-01 00:00:00", as_proto=True),
        period_to=dt("2019-01-31 23:59:59", as_proto=True),
    )

    got = await api.post(
        url,
        proto=pb_input,
        decode_as=campaigns_stat_pb2.CampaignsStatOutput,
        expected_status=200,
    )

    assert got == campaigns_stat_pb2.CampaignsStatOutput(
        by_dates=[
            campaigns_stat_pb2.CampaignsStatOnDate(
                date="2019-01-02",
                details=campaigns_stat_pb2.CampaignsStatDetails(
                    call=1,
                    makeRoute=0,
                    openSite=0,
                    saveOffer=0,
                    search=1,
                    show=4,
                    tap=2,
                    ctr=0.5,
                    clicks_to_routes=0,
                    charged_sum="0.4",
                    show_unique=3,
                ),
            ),
            campaigns_stat_pb2.CampaignsStatOnDate(
                date="2019-01-01",
                details=campaigns_stat_pb2.CampaignsStatDetails(
                    call=1,
                    makeRoute=1,
                    openSite=0,
                    saveOffer=0,
                    search=2,
                    show=7,
                    tap=4,
                    ctr=0.5714,
                    clicks_to_routes=0.25,
                    charged_sum="0.7",
                    show_unique=3,
                ),
            ),
        ],
        total=campaigns_stat_pb2.CampaignsStatDetails(
            call=2,
            makeRoute=1,
            openSite=0,
            saveOffer=0,
            search=3,
            show=11,
            tap=6,
            ctr=0.5454,
            clicks_to_routes=0.1666,
            charged_sum="1.1",
            show_unique=4,
        ),
    )


async def test_errored_if_no_campaigns_passed(api):
    pb_input = campaigns_stat_pb2.CampaignsStatInput(
        campaign_ids=[],
        period_from=dt("2019-01-01 00:00:00", as_proto=True),
        period_to=dt("2019-01-01 23:59:59", as_proto=True),
    )

    got = await api.post(
        url, proto=pb_input, decode_as=error_pb2.Error, expected_status=400
    )

    assert got == error_pb2.Error(code=error_pb2.Error.NO_CAMPAIGNS_PASSED)


@pytest.mark.parametrize("data", (b"", None))
async def test_errored_for_wrong_payload(data, api):
    got = await api.post(url, data=data, decode_as=error_pb2.Error, expected_status=400)

    assert got == error_pb2.Error(
        code=error_pb2.Error.VALIDATION_ERROR,
        description=(
            '["Failed to decode all proto data as valid maps_adv.statistics.'
            'dashboard.proto.campaigns_stat.CampaignsStatInput"]'
        ),
    )


async def test_round_cost_to_two_decimal_places(api):
    pb_input = campaigns_stat_pb2.CampaignsStatInput(
        campaign_ids=[30],
        period_from=dt("2019-01-01 00:00:00", as_proto=True),
        period_to=dt("2019-01-01 23:59:59", as_proto=True),
    )

    got = await api.post(
        url,
        proto=pb_input,
        decode_as=campaigns_stat_pb2.CampaignsStatOutput,
        expected_status=200,
    )

    assert got == campaigns_stat_pb2.CampaignsStatOutput(
        by_dates=[
            campaigns_stat_pb2.CampaignsStatOnDate(
                date="2019-01-01",
                details=campaigns_stat_pb2.CampaignsStatDetails(
                    call=0,
                    makeRoute=0,
                    openSite=0,
                    saveOffer=0,
                    search=0,
                    show=3,
                    tap=0,
                    ctr=0.0,
                    clicks_to_routes=0,
                    charged_sum="0.37",
                    show_unique=1,
                ),
            )
        ],
        total=campaigns_stat_pb2.CampaignsStatDetails(
            call=0,
            makeRoute=0,
            openSite=0,
            saveOffer=0,
            search=0,
            show=3,
            tap=0,
            ctr=0.0,
            clicks_to_routes=0,
            charged_sum="0.37",
            show_unique=1,
        ),
    )


async def test_clicks_to_routes_is_0_if_there_are_no_taps(ch, api):
    event = make_event(dt("2019-01-01 12:20:00"), 40, "di0", "action.makeRoute")
    ch.execute("INSERT INTO stat.accepted_sample VALUES", [event])
    pb_input = campaigns_stat_pb2.CampaignsStatInput(
        campaign_ids=[40],
        period_from=dt("2019-01-01 00:00:00", as_proto=True),
        period_to=dt("2019-01-01 23:59:59", as_proto=True),
    )

    got = await api.post(
        url,
        proto=pb_input,
        decode_as=campaigns_stat_pb2.CampaignsStatOutput,
        expected_status=200,
    )

    assert got == campaigns_stat_pb2.CampaignsStatOutput(
        by_dates=[
            campaigns_stat_pb2.CampaignsStatOnDate(
                date="2019-01-01",
                details=campaigns_stat_pb2.CampaignsStatDetails(
                    call=0,
                    makeRoute=1,
                    openSite=0,
                    saveOffer=0,
                    search=0,
                    show=0,
                    tap=0,
                    ctr=0.0,
                    clicks_to_routes=0,
                    charged_sum="0",
                    show_unique=0,
                ),
            )
        ],
        total=campaigns_stat_pb2.CampaignsStatDetails(
            call=0,
            makeRoute=1,
            openSite=0,
            saveOffer=0,
            search=0,
            show=0,
            tap=0,
            ctr=0.0,
            clicks_to_routes=0,
            charged_sum="0",
            show_unique=0,
        ),
    )


async def test_ctr_is_0_if_there_are_no_shows(ch, api):
    event = make_event(dt("2019-01-01 12:20:00"), 40, "di0", "pin.tap")
    ch.execute("INSERT INTO stat.accepted_sample VALUES", [event])
    pb_input = campaigns_stat_pb2.CampaignsStatInput(
        campaign_ids=[40],
        period_from=dt("2019-01-01 00:00:00", as_proto=True),
        period_to=dt("2019-01-01 23:59:59", as_proto=True),
    )

    got = await api.post(
        url,
        proto=pb_input,
        decode_as=campaigns_stat_pb2.CampaignsStatOutput,
        expected_status=200,
    )

    assert got == campaigns_stat_pb2.CampaignsStatOutput(
        by_dates=[
            campaigns_stat_pb2.CampaignsStatOnDate(
                date="2019-01-01",
                details=campaigns_stat_pb2.CampaignsStatDetails(
                    call=0,
                    makeRoute=0,
                    openSite=0,
                    saveOffer=0,
                    search=0,
                    show=0,
                    tap=1,
                    ctr=0.0,
                    clicks_to_routes=0,
                    charged_sum="0",
                    show_unique=0,
                ),
            )
        ],
        total=campaigns_stat_pb2.CampaignsStatDetails(
            call=0,
            makeRoute=0,
            openSite=0,
            saveOffer=0,
            search=0,
            show=0,
            tap=1,
            ctr=0.0,
            clicks_to_routes=0,
            charged_sum="0",
            show_unique=0,
        ),
    )


@pytest.mark.xfail
@pytest.mark.mock_dm
async def test_serialization_errors_are_logged(dm, api, caplog):
    dm.calculate_by_campaigns_and_period.coro.return_value = [
        {
            "date": date(2019, 1, 1),
            "call": 1,
            "makeRoute": 1,
            "openSite": 0,
            "saveOffer": 0,
            "search": 2,
            "show": 7,
            "tap": 4,
            "ctr": 0.5799,
            "clicks_to_routes": 0.25,
            "charged_sum": Decimal("0.7777777"),
            "show_unique": 3,
        },
        {
            "call": 2,
            "makeRoute": 1,
            "openSite": 0,
            "saveOffer": 0,
            "search": 3,
            "show": 11,
            "tap": 6,
            "ctr": 0.5499,
            "clicks_to_routes": 0.1666,
            "charged_sum": Decimal("1.1"),
            "show_unique": 4,
        },
    ]
    pb_input = campaigns_stat_pb2.CampaignsStatInput(
        campaign_ids=[10],
        period_from=dt("2019-01-01 00:00:00", as_proto=True),
        period_to=dt("2019-01-31 23:59:59", as_proto=True),
    )

    await api.post(url, proto=pb_input, expected_status=400)

    assert (
        "maps_adv.statistics.dashboard.server.lib.api",
        logging.ERROR,
        "Serialization error: {'by_dates': {0: {'details': {'charged_sum': "
        '["Value has 7 fraction digits, but only 2 can\'t be serialized"]}}}}',
    ) in caplog.record_tuples
