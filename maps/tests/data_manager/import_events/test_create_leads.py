from datetime import datetime

import pytest
from smb.common.testing_utils import Any, dt

from maps_adv.geosmb.promoter.server.lib.enums import (
    EventDataSource,
    EventType,
    LeadDataSource,
    Source,
)
from maps_adv.geosmb.promoter.server.tests import (
    make_import_event_data,
    make_import_events_generator,
    make_single_import_event_generator,
)

pytestmark = [pytest.mark.asyncio]


async def test_creates_lead_for_not_matched_import_event(con, dm):
    await dm.import_events_from_generator(make_single_import_event_generator())

    rows = await con.fetch("SELECT * FROM leads")
    assert [dict(row) for row in rows] == [
        dict(
            id=Any(int),
            biz_id=123,
            passport_uid="11",
            device_id="111",
            yandex_uid="1111",
            data_source=LeadDataSource.YT,
            name="ivan45",
            source=Source.EXTERNAL_ADVERT,
            created_at=Any(datetime),
        )
    ]


@pytest.mark.parametrize(
    ("source", "expected_source"),
    [
        ("EXTERNAL_ADVERT", Source.EXTERNAL_ADVERT),
        ("STRAIGHT", Source.STRAIGHT),
        ("DISCOVERY_ADVERT", Source.DISCOVERY_ADVERT),
        ("DISCOVERY_NO_ADVERT", Source.DISCOVERY_NO_ADVERT),
    ],
)
async def test_creates_lead_with_source_from_first_event(
    con, dm, source, expected_source
):
    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(
                    source="EXTERNAL_ADVERT", event_timestamp=dt("2020-01-01 12:00:00")
                ),
                make_import_event_data(
                    source=source,
                    event_timestamp=dt("2020-01-01 11:00:00"),
                ),
                make_import_event_data(
                    source="DISCOVERY_ADVERT", event_timestamp=dt("2020-01-01 13:00:00")
                ),
            ]
        )
    )

    result = await con.fetchval("SELECT source FROM leads")
    assert result == expected_source


@pytest.mark.parametrize(
    "sibling_events_params",
    [
        # sibling by passport_uid
        (
            dict(passport_uid="11", device_id="111", yandex_uid="1111"),
            dict(passport_uid="11", device_id="111", yandex_uid="1111"),
            dict(passport_uid="11", device_id="111", yandex_uid=None),
            dict(passport_uid="11", device_id=None, yandex_uid="1111"),
            dict(passport_uid="11", device_id=None, yandex_uid=None),
            dict(passport_uid="11", device_id="111", yandex_uid="1111"),
        ),
        # sibling by device_id
        (
            dict(passport_uid=None, device_id="111", yandex_uid="1111"),
            dict(passport_uid=None, device_id="111", yandex_uid="1111"),
            dict(passport_uid=None, device_id="111", yandex_uid=None),
        ),
        # sibling by yandex_uid
        (
            dict(passport_uid=None, device_id=None, yandex_uid="1111"),
            dict(passport_uid=None, device_id=None, yandex_uid="1111"),
        ),
    ],
)
async def test_creates_single_lead_for_not_matched_sibling_import_events(
    factory, con, dm, sibling_events_params
):
    await dm.import_events_from_generator(
        make_import_events_generator(
            [make_import_event_data(**params) for params in sibling_events_params]
        )
    )

    assert await con.fetchval("SELECT COUNT(*) FROM leads") == 1


async def test_creates_separate_leads_for_identical_import_events_with_different_biz_ids(  # noqa
    factory, con, dm
):
    await dm.import_events_from_generator(
        make_import_events_generator(
            [make_import_event_data(biz_id=123), make_import_event_data(biz_id=456)]
        )
    )

    assert await con.fetchval("SELECT COUNT(*) FROM leads") == 2


@pytest.mark.parametrize(
    "events_params",
    [
        (
            dict(passport_uid=None, device_id="111", yandex_uid="1111"),
            dict(passport_uid=None, device_id="222", yandex_uid="2222"),
        ),
        (
            dict(passport_uid=None, device_id=None, yandex_uid="1111"),
            dict(passport_uid=None, device_id=None, yandex_uid="2222"),
        ),
    ],
)
async def test_creates_separate_leads_for_import_events_not_matched_by_null_ids(
    factory, con, dm, events_params
):
    await dm.import_events_from_generator(
        make_import_events_generator(
            [make_import_event_data(**event_params) for event_params in events_params]
        )
    )

    assert await con.fetchval("SELECT COUNT(*) FROM leads") == 2


async def test_creates_lead_with_last_not_null_ids_of_matched_by_passport_uid_import_events(  # noqa
    factory, con, dm
):
    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(
                    passport_uid="11",
                    device_id="111",
                    yandex_uid="1111",
                    event_timestamp=dt("2020-01-01 01:01:01"),
                ),
                make_import_event_data(
                    passport_uid="11",
                    device_id="222",
                    yandex_uid="2222",
                    event_timestamp=dt("2020-02-02 02:02:02"),
                ),
                make_import_event_data(
                    passport_uid="11",
                    device_id=None,
                    yandex_uid=None,
                    event_timestamp=dt("2020-03-03 03:03:03"),
                ),
            ]
        )
    )

    rows = await con.fetch("SELECT passport_uid, device_id, yandex_uid FROM leads")
    assert [dict(row) for row in rows] == [
        dict(passport_uid="11", device_id="222", yandex_uid="2222")
    ]


async def test_creates_lead_with_name_by_the_last_of_matched_import_events(
    factory, con, dm
):
    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(
                    passport_uid="11",
                    user_nickname="ivan1",
                    event_timestamp=dt("2020-01-01 01:01:01"),
                ),
                make_import_event_data(
                    passport_uid="11",
                    user_nickname="ivan2",
                    event_timestamp=dt("2020-03-03 03:03:03"),
                ),
                make_import_event_data(
                    passport_uid="11",
                    user_nickname="ivan3",
                    event_timestamp=dt("2020-02-02 02:02:02"),
                ),
            ]
        )
    )

    rows = await con.fetch("SELECT name FROM leads")
    assert [dict(row) for row in rows] == [dict(name="ivan2")]


async def test_creates_lead_revision_for_not_matched_import_event(factory, con, dm):
    await dm.import_events_from_generator(make_single_import_event_generator())

    rows = await con.fetch("SELECT * FROM lead_revisions")
    assert [dict(row) for row in rows] == [
        dict(
            id=Any(int),
            lead_id=Any(int),
            biz_id=123,
            passport_uid="11",
            device_id="111",
            yandex_uid="1111",
            data_source=LeadDataSource.YT,
            name="ivan45",
            created_at=Any(datetime),
        )
    ]


@pytest.mark.parametrize(
    "sibling_events_params",
    [
        # sibling by passport_uid
        (
            dict(passport_uid="11", device_id="111", yandex_uid="1111"),
            dict(passport_uid="11", device_id="111", yandex_uid="1111"),
            dict(passport_uid="11", device_id="111", yandex_uid=None),
            dict(passport_uid="11", device_id=None, yandex_uid="1111"),
            dict(passport_uid="11", device_id=None, yandex_uid=None),
        ),
        # sibling by device_id
        (
            dict(passport_uid=None, device_id="111", yandex_uid="1111"),
            dict(passport_uid=None, device_id="111", yandex_uid="1111"),
            dict(passport_uid=None, device_id="111", yandex_uid=None),
        ),
        # sibling by yandex_uid
        (
            dict(passport_uid=None, device_id=None, yandex_uid="1111"),
            dict(passport_uid=None, device_id=None, yandex_uid="1111"),
        ),
    ],
)
async def test_creates_single_revision_for_sibling_import_events(
    factory, con, dm, sibling_events_params
):
    await dm.import_events_from_generator(
        make_import_events_generator(
            [make_import_event_data(**params) for params in sibling_events_params]
        )
    )

    assert await con.fetchval("SELECT COUNT(*) FROM lead_revisions") == 1


async def test_creates_event_for_not_matched_import_event(factory, dm):
    await dm.import_events_from_generator(make_single_import_event_generator())

    assert await factory.list_events() == [
        dict(
            id=Any(int),
            lead_id=Any(int),
            biz_id=123,
            event_type=EventType.REVIEW,
            event_value="5",
            events_amount=2,
            event_timestamp=dt("2020-01-01 00:00:00"),
            data_source=EventDataSource.MOBILE,
            source=Source.EXTERNAL_ADVERT,
            created_at=Any(datetime),
        )
    ]


@pytest.mark.parametrize(
    "sibling_events_params",
    [
        # sibling by passport_uid
        (
            dict(passport_uid="11", device_id="111", yandex_uid="1111"),
            dict(passport_uid="11", device_id="111", yandex_uid="1111"),
            dict(passport_uid="11", device_id="111", yandex_uid=None),
            dict(passport_uid="11", device_id=None, yandex_uid="1111"),
            dict(passport_uid="11", device_id=None, yandex_uid=None),
        ),
        # sibling by device_id
        (
            dict(passport_uid=None, device_id="111", yandex_uid="1111"),
            dict(passport_uid=None, device_id="111", yandex_uid="1111"),
            dict(passport_uid=None, device_id="111", yandex_uid=None),
        ),
        # sibling by yandex_uid
        (
            dict(passport_uid=None, device_id=None, yandex_uid="1111"),
            dict(passport_uid=None, device_id=None, yandex_uid="1111"),
        ),
    ],
)
async def test_creates_event_for_each_of_sibling_import_events(
    factory, con, dm, sibling_events_params
):
    await dm.import_events_from_generator(
        make_import_events_generator(
            [make_import_event_data(**params) for params in sibling_events_params]
        )
    )

    assert await con.fetchval("SELECT COUNT(*) FROM lead_events") == len(
        sibling_events_params
    )


async def test_not_creates_duplicates_passport_uid_leads_on_secondary_param_match(
    factory, dm
):
    lead_id = await factory.create_lead(passport_uid=None, device_id="222")

    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(passport_uid="11", device_id="111"),
                make_import_event_data(passport_uid="11", device_id="222"),
            ]
        )
    )

    assert len(await factory.list_leads()) == 1
    assert len(await factory.list_lead_events(lead_id)) == 2


async def test_not_creates_duplicates_passport_uid_leads_on_secondary_param_match2(
    factory, dm
):
    await factory.create_lead(passport_uid=None, device_id="111")
    await factory.create_lead(passport_uid=None, device_id="222")
    await factory.create_lead(passport_uid=None, device_id="333")

    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(passport_uid="11", device_id="111"),
                make_import_event_data(passport_uid="11", device_id="222"),
                make_import_event_data(passport_uid="11", device_id="333"),
            ]
        )
    )

    leads = await factory.list_leads()
    assert len(leads) == 1
    assert len(await factory.list_lead_events(leads[0]["id"])) == 3


async def test_refreshes_precalced_for_created_lead(factory, dm):
    await dm.import_events_from_generator(make_single_import_event_generator())

    assert await factory.fetch_all_events_stat() == [
        {
            "id": Any(int),
            "biz_id": 123,
            "last_activity_timestamp": dt("2020-01-01 00:00:00"),
            "last_3_events_timestamp": None,
            "last_review_rating": "5",
            "lead_id": Any(int),
            "total_clicks_on_phone": 0,
            "total_make_routes": 0,
            "total_site_opens": 0,
            "total_view_working_hours": 0,
            "total_view_entrances": 0,
            "total_showcase_product_click": 0,
            "total_promo_to_site": 0,
            "total_location_sharing": 0,
            "total_geoproduct_button_click": 0,
            "total_favourite_click": 0,
            "total_cta_button_click": 0,
            "total_booking_section_interaction": 0,
        }
    ]
