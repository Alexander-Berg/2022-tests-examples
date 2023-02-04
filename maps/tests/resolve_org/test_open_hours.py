import pytest
from yandex.maps.proto.search import hours_pb2

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "pb_open_hours, expected_open_hours",
    [
        (
            [
                hours_pb2.Hours(
                    day=[hours_pb2.DayOfWeek.Value("MONDAY")],
                    time_range=[
                        hours_pb2.TimeRange(**{"from": 9 * 60 * 60, "to": 18 * 60 * 60})
                    ],
                )
            ],
            [(32400, 64800)],
        ),
        (
            [
                hours_pb2.Hours(
                    day=[hours_pb2.DayOfWeek.Value("TUESDAY")],
                    time_range=[
                        hours_pb2.TimeRange(**{"from": 9 * 60 * 60, "to": 18 * 60 * 60})
                    ],
                )
            ],
            [(118800, 151200)],
        ),
        (
            [
                hours_pb2.Hours(
                    day=[hours_pb2.DayOfWeek.Value("TUESDAY")],
                    time_range=[
                        hours_pb2.TimeRange(
                            **{"from": 9 * 60 * 60, "to": 11 * 60 * 60}
                        ),
                        hours_pb2.TimeRange(
                            **{"from": 15 * 60 * 60, "to": 17 * 60 * 60}
                        ),
                    ],
                )
            ],
            [(118800, 126000), (140400, 147600)],
        ),
        (
            [
                hours_pb2.Hours(
                    day=[
                        hours_pb2.DayOfWeek.Value("TUESDAY"),
                        hours_pb2.DayOfWeek.Value("FRIDAY"),
                    ],
                    time_range=[
                        hours_pb2.TimeRange(**{"from": 9 * 60 * 60, "to": 18 * 60 * 60})
                    ],
                )
            ],
            [(118800, 151200), (378000, 410400)],
        ),
        (
            [
                hours_pb2.Hours(
                    day=[hours_pb2.DayOfWeek.Value("TUESDAY")],
                    time_range=[
                        hours_pb2.TimeRange(**{"from": 9 * 60 * 60, "to": 18 * 60 * 60})
                    ],
                ),
                hours_pb2.Hours(
                    day=[hours_pb2.DayOfWeek.Value("FRIDAY")],
                    time_range=[
                        hours_pb2.TimeRange(
                            **{"from": 10 * 60 * 60, "to": 16 * 60 * 60}
                        )
                    ],
                ),
            ],
            [(118800, 151200), (381600, 403200)],
        ),
        (
            [
                hours_pb2.Hours(
                    day=[hours_pb2.DayOfWeek.Value("FRIDAY")],
                    time_range=[
                        hours_pb2.TimeRange(
                            **{"from": 10 * 60 * 60, "to": 16 * 60 * 60}
                        )
                    ],
                ),
                hours_pb2.Hours(
                    day=[hours_pb2.DayOfWeek.Value("TUESDAY")],
                    time_range=[
                        hours_pb2.TimeRange(**{"from": 9 * 60 * 60, "to": 18 * 60 * 60})
                    ],
                ),
            ],
            [(118800, 151200), (381600, 403200)],
        ),
        (
            [
                hours_pb2.Hours(
                    day=[hours_pb2.DayOfWeek.Value("EVERYDAY")],
                    time_range=[hours_pb2.TimeRange(**{"from": 9 * 60 * 60, "to": 0})],
                )
            ],
            [
                (32400, 86400),
                (118800, 172800),
                (205200, 259200),
                (291600, 345600),
                (378000, 432000),
                (464400, 518400),
                (550800, 604800),
            ],
        ),
        (
            [
                hours_pb2.Hours(
                    day=[hours_pb2.DayOfWeek.Value("EVERYDAY")],
                    time_range=[
                        hours_pb2.TimeRange(**{"from": 9 * 60 * 60, "to": 18 * 60 * 60})
                    ],
                )
            ],
            [
                (32400, 64800),
                (118800, 151200),
                (205200, 237600),
                (291600, 324000),
                (378000, 410400),
                (464400, 496800),
                (550800, 583200),
            ],
        ),
        (
            [
                hours_pb2.Hours(
                    day=[hours_pb2.DayOfWeek.Value("TUESDAY")],
                    time_range=[
                        hours_pb2.TimeRange(**{"from": 21 * 60 * 60, "to": 2 * 60 * 60})
                    ],
                ),
                hours_pb2.Hours(
                    day=[hours_pb2.DayOfWeek.Value("WEDNESDAY")],
                    time_range=[
                        hours_pb2.TimeRange(**{"from": 7 * 60 * 60, "to": 10 * 60 * 60})
                    ],
                ),
            ],
            [(162000, 180000), (198000, 208800)],
        ),
        (
            [
                hours_pb2.Hours(
                    day=[hours_pb2.DayOfWeek.Value("SUNDAY")],
                    time_range=[
                        hours_pb2.TimeRange(**{"from": 21 * 60 * 60, "to": 2 * 60 * 60})
                    ],
                )
            ],
            [(0, 7200), (594000, 604800)],
        ),
        (
            [
                hours_pb2.Hours(
                    day=[
                        hours_pb2.DayOfWeek.Value("TUESDAY"),
                        hours_pb2.DayOfWeek.Value("WEDNESDAY"),
                        hours_pb2.DayOfWeek.Value("SATURDAY"),
                        hours_pb2.DayOfWeek.Value("SUNDAY"),
                    ],
                    time_range=[hours_pb2.TimeRange(**{"all_day": True})],
                )
            ],
            [(86400, 259200), (432000, 604800)],
        ),
    ],
)
async def test_returns_valid_open_hours(
    client,
    mock_resolve_org,
    make_response,
    business_go_meta,
    pb_open_hours,
    expected_open_hours,
):
    business_go_meta.open_hours.CopyFrom(
        hours_pb2.OpenHours(hours=pb_open_hours, text="Текст про рабочее время")
    )
    mock_resolve_org(make_response())

    result = await client.resolve_org("12345")

    assert result.open_hours == expected_open_hours


async def test_open_hours_optional(
    client, mock_resolve_org, make_response, business_go_meta
):
    business_go_meta.open_hours.CopyFrom(
        hours_pb2.OpenHours(hours=[], text="Текст про рабочее время")
    )
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.open_hours is None
