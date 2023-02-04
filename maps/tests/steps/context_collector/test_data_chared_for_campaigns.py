from decimal import Decimal

import pytest

from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio]


async def test_sums_events_into_charged(factory, context_collector_step):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "event_name": "BILLBOARD_SHOW",
                "receive_timestamp": dt("2000-02-02 01:15:00"),
            }
        ]
    )
    factory.insert_into_processed(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:01:00"),
                "event_name": "BILLBOARD_SHOW",
                "cost": Decimal("1.2"),
            },
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:02:00"),
                "event_name": "BILLBOARD_SHOW",
                "cost": Decimal("1.2"),
            },
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:03:00"),
                "event_name": "BILLBOARD_SHOW",
                "cost": Decimal("0.2"),
            },
        ]
    )

    result = await context_collector_step.run()

    paid_events_count = result["orders"][0]["campaigns"][0]["charged"]
    assert paid_events_count == Decimal("2.6")


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
async def test_sums_not_billboard_show_into_charged(
    factory, context_collector_step, event_name
):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "event_name": "BILLBOARD_SHOW",
                "receive_timestamp": dt("2000-02-02 01:15:00"),
            }
        ]
    )
    factory.insert_into_processed(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:03:00"),
                "event_name": event_name,
                "cost": Decimal("1.1"),
            }
        ]
    )

    result = await context_collector_step.run()

    paid_events_count = result["orders"][0]["campaigns"][0]["charged"]
    assert paid_events_count == Decimal("1.1")


async def test_not_sums_events_for_other_campaigns_into_charged(
    factory, context_collector_step
):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "event_name": "BILLBOARD_SHOW",
                "receive_timestamp": dt("2000-02-02 01:15:00"),
            }
        ]
    )
    factory.insert_into_processed(
        [
            {
                "campaign_id": 500,
                "receive_timestamp": dt("2000-02-02 01:03:00"),
                "event_name": "BILLBOARD_SHOW",
                "cost": Decimal("1.1"),
            },
            {
                "campaign_id": 501,
                "receive_timestamp": dt("2000-02-02 01:04:00"),
                "event_name": "BILLBOARD_SHOW",
                "cost": Decimal("2.1"),
            },
        ]
    )

    result = await context_collector_step.run()

    paid_events_count = result["orders"][0]["campaigns"][0]["charged"]
    assert paid_events_count == Decimal("0")
