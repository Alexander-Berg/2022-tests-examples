import pytest
from yandex.maps.proto.search import hours_pb2

from maps_adv.geosmb.landlord.proto import organization_details_pb2, schedule_pb2

pytestmark = [pytest.mark.asyncio]

URL = "/external/fetch_landing_data/"


@pytest.mark.parametrize(
    ("open_hours_pb", "expected_schedule"),
    [
        (
            hours_pb2.OpenHours(
                hours=[
                    hours_pb2.Hours(
                        day=[hours_pb2.DayOfWeek.Value("MONDAY")],
                        time_range=[
                            hours_pb2.TimeRange(
                                **{"from": 9 * 60 * 60, "to": 18 * 60 * 60}
                            )
                        ],
                    ),
                    hours_pb2.Hours(
                        day=[hours_pb2.DayOfWeek.Value("TUESDAY")],
                        time_range=[
                            hours_pb2.TimeRange(
                                **{"from": 11 * 60 * 60, "to": 17 * 60 * 60}
                            )
                        ],
                    ),
                ],
                text="Текст про рабочее время",
                tz_offset=10800,
                state=hours_pb2.State(
                    text="Сейчас оно работает",
                    short_text="Да",
                ),
            ),
            schedule_pb2.Schedule(
                tz_offset=10800,
                schedule=[
                    schedule_pb2.ScheduleItem(
                        day=schedule_pb2.ScheduleItem.DayOfWeek.Value("MONDAY"),
                        opens_at=9 * 60 * 60,
                        closes_at=18 * 60 * 60,
                    ),
                    schedule_pb2.ScheduleItem(
                        day=schedule_pb2.ScheduleItem.DayOfWeek.Value("TUESDAY"),
                        opens_at=11 * 60 * 60,
                        closes_at=17 * 60 * 60,
                    ),
                ],
                work_now_text="Сейчас оно работает",
            ),
        ),
        (
            hours_pb2.OpenHours(
                hours=[
                    hours_pb2.Hours(
                        day=[
                            hours_pb2.DayOfWeek.Value("MONDAY"),
                            hours_pb2.DayOfWeek.Value("TUESDAY"),
                        ],
                        time_range=[
                            hours_pb2.TimeRange(
                                **{"from": 9 * 60 * 60, "to": 18 * 60 * 60}
                            )
                        ],
                    ),
                ],
                text="Текст про рабочее время",
                tz_offset=2020,
                state=hours_pb2.State(
                    text="Сейчас оно не работает",
                    short_text="Нет",
                ),
            ),
            schedule_pb2.Schedule(
                tz_offset=2020,
                schedule=[
                    schedule_pb2.ScheduleItem(
                        day=schedule_pb2.ScheduleItem.DayOfWeek.Value("MONDAY"),
                        opens_at=9 * 60 * 60,
                        closes_at=18 * 60 * 60,
                    ),
                    schedule_pb2.ScheduleItem(
                        day=schedule_pb2.ScheduleItem.DayOfWeek.Value("TUESDAY"),
                        opens_at=9 * 60 * 60,
                        closes_at=18 * 60 * 60,
                    ),
                ],
                work_now_text="Сейчас оно не работает",
            ),
        ),
        (
            hours_pb2.OpenHours(
                hours=[
                    hours_pb2.Hours(
                        day=[
                            hours_pb2.DayOfWeek.Value("MONDAY"),
                        ],
                        time_range=[hours_pb2.TimeRange(all_day=True)],
                    ),
                ],
                text="Текст про рабочее время",
                tz_offset=2020,
                state=hours_pb2.State(
                    text="Сегодня работает весь день",
                    short_text="Да",
                ),
            ),
            schedule_pb2.Schedule(
                tz_offset=2020,
                schedule=[
                    schedule_pb2.ScheduleItem(
                        day=schedule_pb2.ScheduleItem.DayOfWeek.Value("MONDAY"),
                        opens_at=0,
                        closes_at=24 * 60 * 60,
                    ),
                ],
                work_now_text="Сегодня работает весь день",
            ),
        ),
        (
            hours_pb2.OpenHours(
                hours=[
                    hours_pb2.Hours(
                        day=[
                            hours_pb2.DayOfWeek.Value("MONDAY"),
                        ],
                        time_range=[hours_pb2.TimeRange(all_day=True)],
                    ),
                ],
            ),
            schedule_pb2.Schedule(
                schedule=[
                    schedule_pb2.ScheduleItem(
                        day=schedule_pb2.ScheduleItem.DayOfWeek.Value("MONDAY"),
                        opens_at=0,
                        closes_at=24 * 60 * 60,
                    ),
                ],
            ),
        ),
    ],
)
async def test_uses_geosearch_client_to_fetch_schedule(
    factory, api, geosearch, open_hours_pb, expected_schedule
):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", stable_version=data_id, published=True
    )
    geosearch.resolve_org.coro.return_value.metas[
        "business"
    ].open_hours.CopyFrom(open_hours_pb)

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.schedule == expected_schedule


async def test_does_not_return_schedule_if_no_open_hours_in_geosearch_response(
    factory, api, geosearch
):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", stable_version=data_id, published=True
    )
    geosearch.resolve_org.coro.return_value.metas["business"].ClearField(
        "open_hours"
    )
    geosearch.resolve_org.coro.return_value.open_hours = None

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert not got.HasField("schedule")
