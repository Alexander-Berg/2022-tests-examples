import pytest
from aiohttp.web import Response

from maps_adv.export.lib.core.enum import CampaignType
from maps_adv.export.lib.pipeline.exceptions import StepException
from maps_adv.export.lib.pipeline.steps import AddStatisticsStep
from maps_adv.statistics.dashboard.proto.campaign_stat_pb2 import (
    CampaignEvents,
    CampaignEventsForPeriodOutput,
)

dashboard_proto_message = CampaignEventsForPeriodOutput(
    campaigns_events=[
        CampaignEvents(campaign_id=1, events=100),
        CampaignEvents(campaign_id=2, events=2000),
        CampaignEvents(campaign_id=100, events=0),
    ]
).SerializeToString()

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
async def step(config, experimental_options, request):
    marker = request.node.get_closest_marker("experiments")
    with experimental_options(marker.args[0] if marker is not None else {}):
        yield AddStatisticsStep(config)


@pytest.mark.experiments({"EXPERIMENT_QUARTER_HOUR_DISPLAYS_CAMPAIGNS": [1]})
async def test_returns_campaign_data_with_displays(step, mock_dashboard):

    campaigns = [
        {
            "id": 1,
            "timezone": "Europe/Moscow",
            "campaign_type": CampaignType.PIN_ON_ROUTE,
        },
        {"id": 2, "timezone": "UTC", "campaign_type": CampaignType.PIN_ON_ROUTE},
        {"id": 100, "campaign_type": CampaignType.PIN_ON_ROUTE},
    ]
    mock_dashboard(Response(body=dashboard_proto_message, status=200))
    mock_dashboard(Response(body=dashboard_proto_message, status=200))
    mock_dashboard(Response(body=dashboard_proto_message, status=200))

    await step(campaigns)

    assert campaigns == [
        {
            "id": 1,
            "timezone": "Europe/Moscow",
            "total_displays": 100,
            "campaign_type": CampaignType.PIN_ON_ROUTE,
        },
        {
            "id": 2,
            "timezone": "UTC",
            "total_displays": 2000,
            "campaign_type": CampaignType.PIN_ON_ROUTE,
        },
        {"id": 100, "total_displays": 0, "campaign_type": CampaignType.PIN_ON_ROUTE},
    ]


async def test_raises_for_unknown_error(step, mock_dashboard):
    mock_dashboard(Response(body=b"", status=404))
    mock_dashboard(Response(body=dashboard_proto_message, status=200))

    with pytest.raises(StepException) as exc:
        await step(
            [
                {
                    "id": 1,
                    "timezone": "Europe/Moscow",
                    "campaign_type": CampaignType.PIN_ON_ROUTE,
                },
                {
                    "id": 2,
                    "timezone": "UTC",
                    "campaign_type": CampaignType.PIN_ON_ROUTE,
                },
            ]
        )

    assert exc.type == StepException
    assert exc.value.args[0] == [1]
    assert exc.value.args[1] == [2]
