from datetime import datetime
from decimal import Decimal
from typing import Optional

__all__ = ["make_event"]


def make_event(
    timestamp: datetime,
    campaign_id: int,
    device_id: str,
    event_name: str,
    cost: Optional[Decimal] = None,
) -> tuple:
    cost = Decimal("0") if cost is None else cost

    return (
        timestamp,
        campaign_id,
        "event_group_id",
        4,
        device_id,
        "app_platform",
        "app_version_name",
        100500,
        10.100500,
        20.200500,
        f"geoadv.bb.{event_name}",
        cost,
    )


def make_event_v2(
    timestamp: datetime,
    campaign_id: int,
    device_id: str,
    event_name: str,
    cost: Optional[Decimal] = None,
) -> tuple:
    cost = Decimal("0") if cost is None else cost

    return (timestamp, event_name, campaign_id, device_id, cost)


def make_raw_event(
    timestamp: datetime, campaign_id: int, device_id: str, event_name: str
) -> tuple:
    return (
        timestamp,
        "rt3.man--maps-adv-statistics--raw_metrika_log@1",
        100000,
        1,
        timestamp,
        campaign_id,
        "63da443a9125e54da803aaa9b8e8126f",
        30488,
        device_id,
        "android",
        "4.62.614",
        4626141,
        55.72059631347656,
        37.43385314941406,
        event_name,
    )
