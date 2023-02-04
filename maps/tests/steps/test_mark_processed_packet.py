from decimal import Decimal

import pytest

from maps_adv.common.helpers import dt
from maps_adv.statistics.beekeeper.lib.steps import MarkProcessedPacket

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def mark_processed_packet(ch_config):
    return MarkProcessedPacket(ch_client_params=ch_config, build_revision=132)


async def test_adds_nothing_if_mark_event_exist(factory, mark_processed_packet):
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:20:00"),
                "campaign_id": 0,
                "device_id": "kek",
                "event_name": "TECHNICAL_PROCESSED_TO",
                "event_group_id": "",
            }
        ]
    )

    await mark_processed_packet.run(
        {
            "packet_start": dt("2000-02-02 01:10:00"),
            "packet_end": dt("2000-02-02 01:20:00"),
            "orders": [],
        }
    )

    assert factory.get_all_processed(0, 1, 4) == [
        (dt("2000-02-02 01:20:00"), "TECHNICAL_PROCESSED_TO", "kek")
    ]


@pytest.mark.parametrize(
    ["field_name", "field_number", "expected_value"],
    [
        ("receive_timestamp", 0, dt("2000-02-02 01:20:00")),
        ("event_name", 1, "TECHNICAL_PROCESSED_TO"),
        ("event_group_id", 3, ""),
        ("device_id", 4, ""),
        ("application", 5, "BEEKEEPER"),
        ("app_platform", 6, "LINUX"),
        ("app_version_name", 7, "r132"),
        ("app_build_number", 8, 132),
        ("user_latitude", 9, Decimal("0")),
        ("user_longitude", 10, Decimal("0")),
        ("place_id", 11, None),
        ("_normalization_metadata", 14, "{}"),
    ],
)
async def test_adds_mark_event_expected(
    field_name, field_number, expected_value, factory, mark_processed_packet
):
    await mark_processed_packet.run(
        {
            "packet_start": dt("2000-02-02 01:10:00"),
            "packet_end": dt("2000-02-02 01:20:00"),
            "orders": [],
        }
    )

    assert factory.get_all_processed(field_number)[0] == expected_value
