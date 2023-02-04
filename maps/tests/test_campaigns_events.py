import pytest
from aiohttp.web import Response

from maps_adv.common.helpers import dt
from maps_adv.common.helpers.enums import CampaignTypeEnum
from maps_adv.common.proto import campaign_pb2
from maps_adv.statistics.dashboard.client import (
    Client,
    NoStatistics,
    UnknownResponse,
)
from maps_adv.common.client.lib.client import REQUEST_MAX_ATTEMPTS
from maps_adv.common.client.lib.exceptions import (
    BadGateway,
    ServiceUnavailable,
)
from maps_adv.statistics.dashboard.proto.campaign_stat_pb2 import (
    CampaignEvents,
    CampaignEventsForPeriodInput,
    CampaignEventsInputPart,
    CampaignEventsForPeriodOutput,
)

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "now, period_from, period_to, campaign_ids, expected_request_body",
    [
        (
            dt("2020-02-20 20:20:20"),
            None,
            None,
            [1, 2, 3],
            CampaignEventsForPeriodInput(
                campaigns=[
                    CampaignEventsInputPart(
                        campaign_id=1,
                        campaign_type=campaign_pb2.CampaignType.PIN_ON_ROUTE,
                    ),
                    CampaignEventsInputPart(
                        campaign_id=2,
                        campaign_type=campaign_pb2.CampaignType.PIN_ON_ROUTE,
                    ),
                    CampaignEventsInputPart(
                        campaign_id=3,
                        campaign_type=campaign_pb2.CampaignType.PIN_ON_ROUTE,
                    ),
                ]
            ),
        ),
        (
            dt("2020-02-20 20:20:20"),
            dt("2020-02-20 20:20:00"),
            dt("2020-02-20 20:40:00"),
            [1],
            CampaignEventsForPeriodInput(
                campaigns=[
                    CampaignEventsInputPart(
                        campaign_id=1,
                        campaign_type=campaign_pb2.CampaignType.PIN_ON_ROUTE,
                    ),
                ],
                period_from=dt("2020-02-20 20:20:00", as_proto=True),
                period_to=dt("2020-02-20 20:40:00", as_proto=True),
            ),
        ),
    ],
)
async def test_requests_data_correctly(
    now,
    period_from,
    period_to,
    campaign_ids,
    expected_request_body,
    mock_displays_api,
    freezer,
):
    freezer.move_to(now)

    async def _handler(request):
        assert (
            CampaignEventsForPeriodInput.FromString(await request.read())
            == expected_request_body
        )
        return Response(
            body=CampaignEventsForPeriodOutput().SerializeToString(), status=200
        )

    mock_displays_api(_handler)

    async with Client("http://dashboard.server") as client:
        await client.campaigns_events(
            map(
                lambda id: dict(id=id, type=CampaignTypeEnum.PIN_ON_ROUTE), campaign_ids
            ),
            period_from=period_from,
            period_to=period_to,
        )


@pytest.mark.parametrize(
    "campaigns_pbs, expected",
    (
        [[CampaignEvents(campaign_id=10, events=4)], {10: 4}],
        [[CampaignEvents(campaign_id=20, events=50)], {20: 50}],
        [
            [
                CampaignEvents(campaign_id=10, events=4),
                CampaignEvents(campaign_id=20, events=50),
            ],
            {10: 4, 20: 50},
        ],
    ),
)
async def test_parses_response_correctly(campaigns_pbs, expected, mock_displays_api):
    mock_displays_api(
        Response(
            body=CampaignEventsForPeriodOutput(
                campaigns_events=campaigns_pbs
            ).SerializeToString(),
            status=200,
        )
    )

    async with Client("http://dashboard.server") as client:
        got = await client.campaigns_events(
            [
                dict(id=1, type=CampaignTypeEnum.PIN_ON_ROUTE),
                dict(id=2, type=CampaignTypeEnum.PIN_ON_ROUTE),
                dict(id=3, type=CampaignTypeEnum.PIN_ON_ROUTE),
            ]
        )

    assert got == expected


async def test_raises_if_server_returns_nothing(mock_displays_api):
    mock_displays_api(Response(status=204))

    async with Client("http://dashboard.server") as client:

        with pytest.raises(NoStatistics):
            await client.campaigns_events(
                [
                    dict(id=1, type=CampaignTypeEnum.PIN_ON_ROUTE),
                    dict(id=2, type=CampaignTypeEnum.PIN_ON_ROUTE),
                    dict(id=3, type=CampaignTypeEnum.PIN_ON_ROUTE),
                ]
            )


async def test_empty_if_no_campaigns_passed():
    async with Client("http://dashboard.server") as client:
        got = await client.campaigns_events([])
    assert got == {}


async def test_raises_for_unexpected_status(mock_displays_api):
    mock_displays_api(Response(status=409))

    async with Client("http://dashboard.server") as client:

        with pytest.raises(UnknownResponse):
            await client.campaigns_events(
                [
                    dict(id=1, type=CampaignTypeEnum.PIN_ON_ROUTE),
                    dict(id=2, type=CampaignTypeEnum.PIN_ON_ROUTE),
                    dict(id=3, type=CampaignTypeEnum.PIN_ON_ROUTE),
                ]
            )


@pytest.mark.parametrize(
    "status, expected_exc", ([502, BadGateway], [503, ServiceUnavailable])
)
async def test_raises_for_expected_statuses_if_retrying_fails(
    status, expected_exc, mock_displays_api
):
    for _ in range(REQUEST_MAX_ATTEMPTS):
        mock_displays_api(Response(status=status))

    async with Client("http://dashboard.server") as client:

        with pytest.raises(expected_exc):
            await client.campaigns_events(
                [
                    dict(id=1, type=CampaignTypeEnum.PIN_ON_ROUTE),
                    dict(id=2, type=CampaignTypeEnum.PIN_ON_ROUTE),
                    dict(id=3, type=CampaignTypeEnum.PIN_ON_ROUTE),
                ]
            )


@pytest.mark.parametrize("status", (502, 503))
async def test_returns_result_if_retries_successfully(status, mock_displays_api):
    for _ in range(REQUEST_MAX_ATTEMPTS - 1):
        mock_displays_api(Response(status=status))
    campaign_pb = CampaignEvents(campaign_id=10, events=4)
    mock_displays_api(
        Response(
            body=CampaignEventsForPeriodOutput(
                campaigns_events=[campaign_pb]
            ).SerializeToString(),
            status=200,
        )
    )

    async with Client("http://dashboard.server") as client:
        got = await client.campaigns_events(
            [
                dict(id=10, type=CampaignTypeEnum.PIN_ON_ROUTE),
            ]
        )

    assert got == {10: 4}
