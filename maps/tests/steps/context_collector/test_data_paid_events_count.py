import pytest

from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio]


async def test_counts_billboard_show_into_paid_events_count(
    factory, context_collector_step
):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:10:00"),
                "event_name": "BILLBOARD_SHOW",
            },
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "event_name": "BILLBOARD_SHOW",
            },
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:20:00"),
                "event_name": "BILLBOARD_SHOW",
            },
        ]
    )

    result = await context_collector_step.run()

    paid_events_count = result["orders"][0]["campaigns"][0]["paid_events_count"]
    assert paid_events_count == 3


@pytest.mark.parametrize(
    "event_name",
    [
        "BILLBOARD_TAP",
        "ACTION_CALL",
        "ACTION_MAKE_ROUTE",
        "ACTION_SEARCH",
        "ACTION_OPEN_SITE",
        "ACTION_OPEN_APP",
        "ACTION_SAVE_OFFER",
    ],
)
async def test_not_counts_not_billboard_show_into_paid_events_count(
    factory, context_collector_step, event_name
):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:11:00"),
                "event_name": event_name,
            }
        ]
    )

    result = await context_collector_step.run()

    paid_events_count = result["orders"][0]["campaigns"][0]["paid_events_count"]
    assert paid_events_count == 0


async def test_not_counts_events_outside_of_packet_into_paid_events_count(
    factory, context_collector_step
):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 00:05:00"),
                "event_name": "BILLBOARD_SHOW",
            },
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:25:00"),
                "event_name": "BILLBOARD_SHOW",
            },
        ]
    )

    result = await context_collector_step.run()

    paid_events_count = result["orders"][0]["campaigns"][0]["paid_events_count"]
    assert paid_events_count == 0


async def test_not_counts_events_for_other_campaigns_into_paid_events_count(
    factory, context_collector_step
):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 500,
                "receive_timestamp": dt("2000-02-02 01:15:00"),
                "event_name": "BILLBOARD_SHOW",
            },
            {
                "campaign_id": 501,
                "receive_timestamp": dt("2000-02-02 01:16:00"),
                "event_name": "BILLBOARD_SHOW",
            },
        ]
    )

    result = await context_collector_step.run()

    paid_events_count = result["orders"][0]["campaigns"][0]["paid_events_count"]
    assert paid_events_count == 0
