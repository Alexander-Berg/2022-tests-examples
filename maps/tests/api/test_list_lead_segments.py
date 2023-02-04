from datetime import datetime, timedelta, timezone

import pytest

from maps_adv.geosmb.promoter.proto.errors_pb2 import Error
from maps_adv.geosmb.promoter.proto.list_lead_segments_pb2 import (
    BusinessSegments,
    ListLeadSegmentsInput,
    ListLeadSegmentsOutput,
)
from maps_adv.geosmb.promoter.proto.segments_pb2 import SegmentType
from maps_adv.geosmb.promoter.server.lib.enums import EventType

pytestmark = [pytest.mark.asyncio]

url = "/v1/list_lead_segments/"

now = datetime.now(tz=timezone.utc)


@pytest.mark.parametrize(
    "input_params",
    [
        dict(passport_uid="some_passport_uid"),
        dict(yandex_uid="some_ya_uid"),
        dict(device_id="some_device_id"),
        dict(yandex_uid="some_ya_uid", device_id="some_device_id"),
    ],
)
async def test_returns_list_of_segments_for_businesses_to_which_lead_belongs_to(
    input_params, factory, api
):
    # prospective, loyal
    lead_id_1 = await factory.create_lead_with_events(
        biz_id=100,
        passport_uid="some_passport_uid",
        yandex_uid="some_ya_uid",
        device_id="some_device_id",
        site_opens=1,
        review_rating=5,
        last_activity_timestamp=now - timedelta(days=10),
    )
    # lost, disloyal
    lead_id_2 = await factory.create_lead_with_events(
        biz_id=200,
        passport_uid="some_passport_uid",
        yandex_uid="some_ya_uid",
        device_id="some_device_id",
        clicks_on_phone=3,
        review_rating=2,
        last_activity_timestamp=now - timedelta(days=100),
    )

    got = await api.post(
        url,
        proto=ListLeadSegmentsInput(**input_params),
        decode_as=ListLeadSegmentsOutput,
        expected_status=200,
    )

    assert got == ListLeadSegmentsOutput(
        biz_segments=[
            BusinessSegments(
                lead_id=lead_id_2,
                biz_id=200,
                segments=[SegmentType.LOST, SegmentType.DISLOYAL],
            ),
            BusinessSegments(
                lead_id=lead_id_1,
                biz_id=100,
                segments=[SegmentType.PROSPECTIVE, SegmentType.LOYAL],
            ),
        ]
    )


async def test_returns_all_business_to_which_lead_belongs_to_by_any_id_match(
    factory, api
):
    await factory.create_lead_with_events(
        biz_id=100, yandex_uid="some_ya_uid", site_opens=1
    )
    await factory.create_lead_with_events(
        biz_id=200, device_id="some_device_id", clicks_on_phone=3
    )
    await factory.create_lead_with_events(
        biz_id=300, passport_uid="some_passport_uid", clicks_on_phone=3
    )

    got = await api.post(
        url,
        proto=ListLeadSegmentsInput(
            passport_uid="some_passport_uid",
            yandex_uid="some_ya_uid",
            device_id="some_device_id",
        ),
        decode_as=ListLeadSegmentsOutput,
        expected_status=200,
    )

    assert [g.biz_id for g in got.biz_segments] == [300, 200, 100]


async def test_filter_by_biz_id_if_passed(factory, api):
    await factory.create_lead_with_events(
        biz_id=100, yandex_uid="some_ya_uid", site_opens=1
    )
    await factory.create_lead_with_events(
        biz_id=200, device_id="some_device_id", clicks_on_phone=3
    )
    await factory.create_lead_with_events(
        biz_id=300, passport_uid="some_passport_uid", clicks_on_phone=3
    )

    got = await api.post(
        url,
        proto=ListLeadSegmentsInput(
            passport_uid="some_passport_uid",
            yandex_uid="some_ya_uid",
            device_id="some_device_id",
            biz_id=200,
        ),
        decode_as=ListLeadSegmentsOutput,
        expected_status=200,
    )

    assert [g.biz_id for g in got.biz_segments] == [200]


async def test_segments_correctly_each_lead_in_business(api, factory):
    # prospective
    lead_id_1 = await factory.create_lead_with_events(
        passport_uid="111",
        yandex_uid="some_ya_uid",
        site_opens=1,
        last_activity_timestamp=now - timedelta(days=10),
    )
    # lost, loyal
    lead_id_2 = await factory.create_lead_with_events(
        passport_uid="222",
        device_id="some_device_id",
        review_rating=5,
        last_activity_timestamp=now - timedelta(days=100),
    )
    # active, disloyal
    lead_id_3 = await factory.create_lead_with_events(
        passport_uid="some_passport_uid",
        review_rating=2,
        clicks_on_phone=5,
        last_activity_timestamp=now - timedelta(days=10),
    )

    got = await api.post(
        url,
        proto=ListLeadSegmentsInput(
            passport_uid="some_passport_uid",
            yandex_uid="some_ya_uid",
            device_id="some_device_id",
        ),
        decode_as=ListLeadSegmentsOutput,
        expected_status=200,
    )

    assert len(got.biz_segments) == 3
    assert got == ListLeadSegmentsOutput(
        biz_segments=[
            BusinessSegments(
                lead_id=lead_id_3,
                biz_id=123,
                segments=[SegmentType.ACTIVE, SegmentType.DISLOYAL],
            ),
            BusinessSegments(
                lead_id=lead_id_2,
                biz_id=123,
                segments=[SegmentType.LOST, SegmentType.LOYAL],
            ),
            BusinessSegments(
                lead_id=lead_id_1, biz_id=123, segments=[SegmentType.PROSPECTIVE]
            ),
        ]
    )


async def test_returns_empty_list_if_no_match_found(factory, api):
    await factory.create_lead_with_events(
        biz_id=100, yandex_uid="another_ya_uid", site_opens=1
    )
    await factory.create_lead_with_events(
        biz_id=200, device_id="another_device_id", clicks_on_phone=3
    )
    await factory.create_lead_with_events(
        biz_id=300, passport_uid="another_passport_uid", clicks_on_phone=3
    )

    got = await api.post(
        url,
        proto=ListLeadSegmentsInput(
            yandex_uid="some_ya_uid", device_id="some_device_id"
        ),
        decode_as=ListLeadSegmentsOutput,
        expected_status=200,
    )

    assert got == ListLeadSegmentsOutput(biz_segments=[])


async def test_returns_all_lead_segments(factory, api):
    await factory.create_lead_with_events(
        yandex_uid="some_ya_uid",
        review_rating=3,
        clicks_on_phone=2,
        site_opens=3,
        last_activity_timestamp=now - timedelta(days=50),
    )

    got = await api.post(
        url,
        proto=ListLeadSegmentsInput(yandex_uid="some_ya_uid"),
        decode_as=ListLeadSegmentsOutput,
        expected_status=200,
    )

    lead = got.biz_segments[0]
    assert lead.segments == [SegmentType.ACTIVE, SegmentType.DISLOYAL]


async def test_segments_as_prospective_if_less_than_3_events_in_business_in_last_90_days(  # noqa
    factory, api
):
    await factory.create_lead_with_events(
        yandex_uid="some_ya_uid",
        clicks_on_phone=1,
        site_opens=1,
        last_activity_timestamp=now - timedelta(days=89),
    )

    got = await api.post(
        url,
        proto=ListLeadSegmentsInput(yandex_uid="some_ya_uid"),
        decode_as=ListLeadSegmentsOutput,
        expected_status=200,
    )

    lead = got.biz_segments[0]
    assert SegmentType.PROSPECTIVE in lead.segments


async def test_segments_as_active_if_3_or_more_events_in_business_in_last_90_days(
    factory, api
):
    await factory.create_lead_with_events(
        yandex_uid="some_ya_uid",
        review_rating=5,
        clicks_on_phone=1,
        site_opens=1,
        last_activity_timestamp=now - timedelta(days=89),
    )

    got = await api.post(
        url,
        proto=ListLeadSegmentsInput(yandex_uid="some_ya_uid"),
        decode_as=ListLeadSegmentsOutput,
        expected_status=200,
    )

    lead = got.biz_segments[0]
    assert SegmentType.ACTIVE in lead.segments


async def test_segments_as_lost_if_more_90_days_since_last_activity_in_business(
    factory, api
):
    await factory.create_lead_with_events(
        yandex_uid="some_ya_uid",
        review_rating=5,
        clicks_on_phone=1,
        site_opens=1,
        last_activity_timestamp=now - timedelta(days=91),
    )

    got = await api.post(
        url,
        proto=ListLeadSegmentsInput(yandex_uid="some_ya_uid"),
        decode_as=ListLeadSegmentsOutput,
        expected_status=200,
    )

    lead = got.biz_segments[0]
    assert SegmentType.LOST in lead.segments


async def test_does_not_segment_as_lost_for_history_if_has_any_recent_event(
    factory, api
):
    # events older than 90 days
    lead_id = await factory.create_lead_with_events(
        yandex_uid="some_ya_uid",
        clicks_on_phone=1,
        site_opens=1,
        last_activity_timestamp=now - timedelta(days=91),
    )
    # event in last 90 days
    await factory.create_event(
        lead_id=lead_id,
        event_type=EventType.MAKE_ROUTE,
        event_timestamp=now - timedelta(days=80),
    )

    got = await api.post(
        url,
        proto=ListLeadSegmentsInput(yandex_uid="some_ya_uid"),
        decode_as=ListLeadSegmentsOutput,
        expected_status=200,
    )

    lead = got.biz_segments[0]
    assert SegmentType.LOST not in lead.segments


@pytest.mark.parametrize("review_rating", [4, 5, 6])
async def test_segments_as_loyal_if_business_review_rating_is_greater_or_equal_4(
    review_rating, factory, api
):
    await factory.create_lead_with_events(
        yandex_uid="some_ya_uid", review_rating=review_rating
    )

    got = await api.post(
        url,
        proto=ListLeadSegmentsInput(yandex_uid="some_ya_uid"),
        decode_as=ListLeadSegmentsOutput,
        expected_status=200,
    )

    lead = got.biz_segments[0]
    assert SegmentType.LOYAL in lead.segments


@pytest.mark.parametrize("review_rating", [1, 2, 3])
async def test_segments_as_disloyal_if_business_review_rating_is_under_4(
    review_rating, factory, api
):
    await factory.create_lead_with_events(
        yandex_uid="some_ya_uid", review_rating=review_rating
    )

    got = await api.post(
        url,
        proto=ListLeadSegmentsInput(yandex_uid="some_ya_uid"),
        decode_as=ListLeadSegmentsOutput,
        expected_status=200,
    )

    lead = got.biz_segments[0]
    assert SegmentType.DISLOYAL in lead.segments


@pytest.mark.parametrize(
    "review_rating, expected_segment, unexpected_segment",
    [
        (2, SegmentType.DISLOYAL, SegmentType.LOYAL),
        (5, SegmentType.LOYAL, SegmentType.DISLOYAL),
    ],
)
async def test_does_not_segment_lead_as_loyal_and_disloyal_simultaneously_in_business(
    review_rating, expected_segment, unexpected_segment, factory, api
):
    await factory.create_lead_with_events(
        yandex_uid="some_ya_uid", review_rating=review_rating
    )

    got = await api.post(
        url,
        proto=ListLeadSegmentsInput(yandex_uid="some_ya_uid"),
        decode_as=ListLeadSegmentsOutput,
        expected_status=200,
    )

    lead = got.biz_segments[0]
    assert expected_segment in lead.segments
    assert unexpected_segment not in lead.segments


@pytest.mark.parametrize(
    "event_details, expected_segment, unexpected_segments",
    [
        (
            dict(site_opens=1, last_activity_timestamp=now - timedelta(days=80)),
            SegmentType.PROSPECTIVE,
            [SegmentType.ACTIVE, SegmentType.LOST],
        ),
        (
            dict(site_opens=5, last_activity_timestamp=now - timedelta(days=80)),
            SegmentType.ACTIVE,
            [SegmentType.PROSPECTIVE, SegmentType.LOST],
        ),
        (
            dict(site_opens=1, last_activity_timestamp=now - timedelta(days=100)),
            SegmentType.LOST,
            [SegmentType.ACTIVE, SegmentType.PROSPECTIVE],
        ),
        (
            dict(site_opens=5, last_activity_timestamp=now - timedelta(days=100)),
            SegmentType.LOST,
            [SegmentType.ACTIVE, SegmentType.PROSPECTIVE],
        ),
    ],
)
async def test_does_not_segment_lead_as_active_prospective_and_lost_simultaneously_in_business(  # noqa
    event_details, expected_segment, unexpected_segments, factory, api
):
    await factory.create_lead_with_events(yandex_uid="some_ya_uid", **event_details)

    got = await api.post(
        url,
        proto=ListLeadSegmentsInput(yandex_uid="some_ya_uid"),
        decode_as=ListLeadSegmentsOutput,
        expected_status=200,
    )

    lead = got.biz_segments[0]
    assert expected_segment in lead.segments
    for unexpected_segment in unexpected_segments:
        assert unexpected_segment not in lead.segments


async def test_does_not_considers_another_lead_events_in_business(factory, api):
    lead_id_1 = await factory.create_lead_with_events(
        passport_uid="111",
        yandex_uid="some_ya_uid",
        review_rating=3,
        last_activity_timestamp=now - timedelta(days=100),
    )
    lead_id_2 = await factory.create_lead_with_events(
        passport_uid="222",
        device_id="some_device_id",
        site_opens=2,
        clicks_on_phone=4,
        last_activity_timestamp=now - timedelta(days=80),
    )

    got = await api.post(
        url,
        proto=ListLeadSegmentsInput(
            yandex_uid="some_ya_uid", device_id="some_device_id"
        ),
        decode_as=ListLeadSegmentsOutput,
        expected_status=200,
    )

    lead_1 = [lead for lead in got.biz_segments if lead.lead_id == lead_id_1][0]
    assert lead_1.segments == [SegmentType.LOST, SegmentType.DISLOYAL]

    lead_2 = [lead for lead in got.biz_segments if lead.lead_id == lead_id_2][0]
    assert lead_2.segments == [SegmentType.ACTIVE]


@pytest.mark.parametrize(
    "input_params, expected_error",
    [
        (
            dict(passport_uid="", yandex_uid="some_ya_uid", device_id="some_device_id"),
            "passport_uid: ['Shorter than minimum length 1.']",
        ),
        (
            dict(
                passport_uid="some_passport_uid",
                yandex_uid="",
                device_id="some_device_id",
            ),
            "yandex_uid: ['Shorter than minimum length 1.']",
        ),
        (
            dict(
                passport_uid="some_passport_uid", yandex_uid="some_ya_uid", device_id=""
            ),
            "device_id: ['Shorter than minimum length 1.']",
        ),
        (
            dict(
                passport_uid="some_passport_uid",
                yandex_uid="some_ya_uid",
                device_id="some_device_id",
                biz_id=0,
            ),
            "biz_id: ['Must be at least 1.']",
        ),
    ],
)
async def test_returns_error_for_wrong_params_input(input_params, expected_error, api):
    got = await api.post(
        url,
        proto=ListLeadSegmentsInput(**input_params),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(code=Error.VALIDATION_ERROR, description=expected_error)


async def test_returns_error_if_no_id_fields_passed(api):
    got = await api.post(
        url,
        proto=ListLeadSegmentsInput(passport_uid=None, yandex_uid=None, device_id=None),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.VALIDATION_ERROR,
        description="_schema: ['At least one id field should be listed: passport_uid, yandex_uid or device_id']",  # noqa
    )
