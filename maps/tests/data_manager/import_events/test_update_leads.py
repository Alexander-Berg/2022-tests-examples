from datetime import datetime, timezone

import pytest
from freezegun import freeze_time
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


@pytest.mark.parametrize(
    "lead_ids",
    [
        dict(passport_uid="11", device_id="222", yandex_uid="2222"),
        dict(passport_uid=None, device_id="111", yandex_uid="2222"),
        dict(passport_uid=None, device_id=None, yandex_uid="1111"),
    ],
)
@pytest.mark.parametrize(
    ("field", "value"), [("device_id", "111"), ("yandex_uid", "1111")]
)
async def test_updates_lead_with_matched_import_event_ids(
    factory, dm, lead_ids, field, value
):
    lead_id = await factory.create_lead(**lead_ids)

    await dm.import_events_from_generator(
        make_single_import_event_generator(passport_uid="11", **{field: value})
    )

    lead = await factory.retrieve_lead(lead_id)
    assert lead["passport_uid"] == "11"
    assert lead[field] == value


async def test_updates_lead_with_last_not_null_ids_of_matched_by_passport_uid_import_events(  # noqa
    factory, dm
):
    lead_id = await factory.create_lead(
        passport_uid="11", device_id="000", yandex_uid="0000"
    )

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

    lead = await factory.retrieve_lead(lead_id)
    assert lead["device_id"] == "222"
    assert lead["yandex_uid"] == "2222"


@pytest.mark.parametrize(
    "event_source",
    [
        "EXTERNAL_ADVERT",
        "STRAIGHT",
        "DISCOVERY_ADVERT",
        "DISCOVERY_NO_ADVERT",
    ],
)
async def test_does_not_update_lead_source(factory, dm, event_source):
    lead_id = await factory.create_lead(
        passport_uid="11", source=Source.DISCOVERY_NO_ADVERT
    )

    await dm.import_events_from_generator(
        make_single_import_event_generator(passport_uid="11", source=event_source)
    )

    lead = await factory.retrieve_lead(lead_id)
    assert lead["source"] == Source.DISCOVERY_NO_ADVERT


@pytest.mark.parametrize(
    "lead_ids, matched_by_biz_id_event_params, not_matched_by_biz_id_event_params",
    [
        (
            dict(passport_uid="11", device_id="000", yandex_uid="0000"),
            dict(passport_uid="11", device_id="111", yandex_uid="1111"),
            dict(passport_uid="11", device_id="222", yandex_uid="2222"),
        ),
        (
            dict(passport_uid=None, device_id="111", yandex_uid="0000"),
            dict(passport_uid=None, device_id="111", yandex_uid="1111"),
            dict(passport_uid=None, device_id="111", yandex_uid="2222"),
        ),
        (
            dict(passport_uid=None, device_id=None, yandex_uid="1111"),
            dict(passport_uid=None, device_id=None, yandex_uid="1111"),
            dict(passport_uid=None, device_id=None, yandex_uid="1111"),
        ),
    ],
)
async def test_lead_update_is_not_affected_by_import_event_with_different_biz_id(
    factory,
    con,
    dm,
    lead_ids,
    matched_by_biz_id_event_params,
    not_matched_by_biz_id_event_params,
):
    lead_id = await factory.create_lead(biz_id=123, name="lead_1", **lead_ids)

    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(
                    biz_id=123,
                    user_nickname="ivan11",
                    event_timestamp=dt("2020-01-01 01:01:01"),
                    **matched_by_biz_id_event_params,
                ),
                make_import_event_data(
                    biz_id=456,
                    user_nickname="ivan22",
                    event_timestamp=dt("2020-02-02 02:02:02"),
                    **not_matched_by_biz_id_event_params,
                ),
            ]
        )
    )

    lead = await factory.retrieve_lead(lead_id)
    assert lead["passport_uid"] == matched_by_biz_id_event_params["passport_uid"]
    assert lead["device_id"] == matched_by_biz_id_event_params["device_id"]
    assert lead["yandex_uid"] == matched_by_biz_id_event_params["yandex_uid"]
    assert lead["name"] == "ivan11"


async def test_updates_lead_name_by_the_last_of_matched_import_events(factory, dm):
    lead_id = await factory.create_lead(passport_uid="11", name="lead_1")

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

    lead = await factory.retrieve_lead(lead_id)
    assert lead["name"] == "ivan2"


@pytest.mark.parametrize(
    "import_event_ids",
    [
        dict(passport_uid="88", device_id=None, yandex_uid=None),
        dict(passport_uid=None, device_id="888", yandex_uid=None),
        dict(passport_uid=None, device_id=None, yandex_uid="8888"),
    ],
)
async def test_does_not_update_lead_ids_if_they_are_not_set_in_matched_import_event(
    factory, dm, import_event_ids
):
    lead_id = await factory.create_lead(
        passport_uid="88", device_id="888", yandex_uid="8888"
    )

    await dm.import_events_from_generator(
        make_single_import_event_generator(**import_event_ids)
    )

    lead = await factory.retrieve_lead(lead_id)
    assert lead["passport_uid"] == "88"
    assert lead["device_id"] == "888"
    assert lead["yandex_uid"] == "8888"


@freeze_time("2020-01-01 00:00:00", tick=True)
@pytest.mark.parametrize(
    (
        "event_device_id",
        "event_yandex_uid",
        "expected_revision_device_id",
        "expected_revision_yandex_uid",
    ),
    [("222", None, "222", "1111"), (None, "2222", "111", "2222")],
)
async def test_creates_lead_revision_if_matches_with_import_event(
    factory,
    dm,
    event_device_id,
    event_yandex_uid,
    expected_revision_device_id,
    expected_revision_yandex_uid,
):
    lead_id = await factory.create_lead(
        passport_uid="11", device_id="111", yandex_uid="1111"
    )

    await dm.import_events_from_generator(
        make_single_import_event_generator(
            passport_uid="11", device_id=event_device_id, yandex_uid=event_yandex_uid
        )
    )

    lead_revision = await factory.retrieve_last_revision(lead_id)
    assert lead_revision == dict(
        lead_id=lead_id,
        biz_id=123,
        passport_uid="11",
        device_id=expected_revision_device_id,
        yandex_uid=expected_revision_yandex_uid,
        data_source=LeadDataSource.YT,
        name="ivan45",
    )


async def test_creates_single_revision_for_multiple_matched_import_events(
    factory, con, dm
):
    await factory.create_lead(passport_uid="11", device_id="111", yandex_uid="1111")

    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(
                    passport_uid="11", device_id="111", yandex_uid="1111"
                ),
                make_import_event_data(
                    passport_uid="11", device_id="111", yandex_uid=None
                ),
                make_import_event_data(
                    passport_uid="11", device_id=None, yandex_uid="1111"
                ),
                make_import_event_data(
                    passport_uid="11", device_id=None, yandex_uid=None
                ),
            ]
        )
    )

    # 1 existed + 1 created
    assert await con.fetchval("SELECT COUNT(*) FROM lead_revisions") == 2


@pytest.mark.parametrize(
    "lead_ids, import_event_ids",
    [
        (
            dict(passport_uid="11", device_id="222", yandex_uid="2222"),
            dict(passport_uid="11", device_id="111", yandex_uid="1111"),
        ),
        # lead has null ids
        (
            dict(passport_uid=None, device_id="111", yandex_uid="2222"),
            dict(passport_uid="11", device_id="111", yandex_uid="1111"),
        ),
        (
            dict(passport_uid=None, device_id=None, yandex_uid="1111"),
            dict(passport_uid="11", device_id="111", yandex_uid="1111"),
        ),
        # imported event has null ids
        (
            dict(passport_uid="88", device_id="888", yandex_uid="8888"),
            dict(passport_uid="88", device_id=None, yandex_uid=None),
        ),
    ],
)
async def test_creates_event_for_matched_import_event(
    factory, dm, lead_ids, import_event_ids
):
    lead_id = await factory.create_lead(**lead_ids)

    await dm.import_events_from_generator(
        make_single_import_event_generator(**import_event_ids)
    )

    events = await factory.list_lead_events(lead_id)
    assert events == [
        dict(
            id=Any(int),
            lead_id=lead_id,
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


async def test_creates_event_for_each_of_matched_import_events(factory, con, dm):
    await factory.create_lead(passport_uid="11")

    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(
                    passport_uid="11", device_id="111", yandex_uid="1111"
                ),
                make_import_event_data(
                    passport_uid="11", device_id="111", yandex_uid="1111"
                ),
                make_import_event_data(
                    passport_uid="11", device_id="111", yandex_uid=None
                ),
                make_import_event_data(
                    passport_uid="11", device_id=None, yandex_uid="1111"
                ),
                make_import_event_data(
                    passport_uid="11", device_id=None, yandex_uid=None
                ),
            ]
        )
    )

    assert len(await factory.list_events()) == 5


@pytest.mark.parametrize(
    "lead_fields",
    [
        dict(biz_id=123, passport_uid="22", device_id=None, yandex_uid=None),
        dict(biz_id=888, passport_uid="11", device_id="111", yandex_uid="1111"),
    ],
)
async def test_does_not_affect_lead_if_no_matches(factory, con, dm, lead_fields):
    lead_significant_fields = dict(name="lead_1", **lead_fields)
    lead_id = await factory.create_lead(**lead_significant_fields)

    await dm.import_events_from_generator(
        make_single_import_event_generator(
            biz_id=123, passport_uid="11", device_id="111", yandex_uid="1111"
        )
    )

    lead_row = await con.fetchrow(
        "SELECT biz_id, name, passport_uid, device_id, yandex_uid "
        "FROM leads WHERE id = $1",
        lead_id,
    )
    assert dict(lead_row) == lead_significant_fields
    assert (
        await con.fetchval(
            "SELECT COUNT(*) FROM lead_revisions WHERE lead_id = $1", lead_id
        )
        == 1
    )
    assert await con.fetchval(
        """
        SELECT NOT EXISTS (
            SELECT 1 FROM lead_events WHERE lead_id = $1
        )
        """,
        lead_id,
    )


async def test_refreshes_precalced_for_updated_leads(factory, dm):
    lead_id = await factory.create_lead(
        passport_uid=None, device_id=None, yandex_uid="1111"
    )

    now = datetime.now(tz=timezone.utc)
    await dm.import_events_from_generator(
        make_single_import_event_generator(
            passport_uid="11",
            device_id="111",
            yandex_uid="1111",
            event_type="CLICK_ON_PHONE",
            event_timestamp=now,
        )
    )

    assert await factory.fetch_all_events_stat() == [
        {
            "id": Any(int),
            "biz_id": 123,
            "last_activity_timestamp": now,
            "last_3_events_timestamp": None,
            "last_review_rating": None,
            "lead_id": lead_id,
            "total_clicks_on_phone": 2,
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
