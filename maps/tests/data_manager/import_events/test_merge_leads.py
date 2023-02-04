from datetime import datetime, timezone
from operator import itemgetter

import pytest
from freezegun import freeze_time
from smb.common.testing_utils import Any, dt

from maps_adv.geosmb.promoter.server.lib.enums import LeadDataSource, Source
from maps_adv.geosmb.promoter.server.tests import (
    make_import_event_data,
    make_import_events_generator,
)

pytestmark = [pytest.mark.asyncio]

get_lead_ids = itemgetter("passport_uid", "device_id", "yandex_uid")


async def test_merges_leads_with_common_params(factory, dm):
    await factory.create_lead(passport_uid=None, device_id="111")
    await factory.create_lead(passport_uid=None, yandex_uid="1111")

    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(passport_uid="11", device_id="111"),
                make_import_event_data(passport_uid="11", yandex_uid="1111"),
            ]
        )
    )

    leads = await factory.list_leads()
    assert len(leads) == 1


async def test_moves_duplicated_lead_revisions_revisions_to_head_leads(
    factory, con, dm
):
    await factory.create_lead(passport_uid=None, device_id="111")
    await factory.create_lead(passport_uid=None, yandex_uid="1111")

    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(passport_uid="11", device_id="111"),
                make_import_event_data(passport_uid="11", yandex_uid="1111"),
            ]
        )
    )

    assert await con.fetchval("SELECT COUNT(*) AS cnt FROM lead_revisions") == 3
    assert (
        await con.fetchval("SELECT COUNT(DISTINCT lead_id) AS cnt FROM lead_revisions")
        == 1
    )


async def test_does_not_merge_leads_with_different_biz_ids(factory, con, dm):
    lead_id_1 = await factory.create_lead(
        biz_id=123, passport_uid=None, device_id="111"
    )
    lead_id_2 = await factory.create_lead(
        biz_id=456, passport_uid=None, yandex_uid="1111"
    )

    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(biz_id=456, passport_uid="11", device_id="111"),
                make_import_event_data(
                    biz_id=123, passport_uid="11", yandex_uid="1111"
                ),
            ]
        )
    )

    for lead_id in (lead_id_1, lead_id_2):
        assert await con.fetchval(
            "SELECT EXISTS (SELECT 1 FROM leads WHERE id = $1)", lead_id
        )
        assert (
            await con.fetchval(
                "SELECT COUNT(*) FROM lead_revisions WHERE lead_id = $1", lead_id
            )
            == 2
        )
        assert (
            await con.fetchval(
                "SELECT COUNT(*) FROM lead_events WHERE lead_id = $1", lead_id
            )
            == 1
        )


async def test_updates_head_lead_fields_when_merging(factory, dm):
    await factory.create_lead(passport_uid=None, device_id="111")
    await factory.create_lead(passport_uid=None, yandex_uid="1111")

    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(passport_uid="11", device_id="111"),
                make_import_event_data(passport_uid="11", yandex_uid="1111"),
            ]
        )
    )

    leads = await factory.list_leads()
    assert len(leads) == 1
    assert get_lead_ids(leads[0]) == ("11", "111", "1111")


async def test_sets_first_event_source_as_head_lead_source(factory, dm):
    await factory.create_lead(
        passport_uid=None,
        device_id="111",
        created_at=dt("2020-02-02 13:00:00"),
        source=Source.EXTERNAL_ADVERT,
    )
    await factory.create_lead(
        passport_uid=None,
        yandex_uid="1111",
        created_at=dt("2020-01-01 11:00:00"),
        source=Source.DISCOVERY_NO_ADVERT,
    )

    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(passport_uid="11", device_id="111"),
                make_import_event_data(passport_uid="11", yandex_uid="1111"),
            ]
        )
    )

    lead = (await factory.list_leads())[0]
    assert lead["source"] == Source.DISCOVERY_NO_ADVERT


@freeze_time("2020-01-01 00:00:00", tick=True)
async def test_creates_new_revision_for_remaining_lead(factory, con, dm):
    lead_id1 = await factory.create_lead(
        passport_uid=None, device_id="111", name="ivan_1"
    )
    lead_id2 = await factory.create_lead(
        passport_uid=None, yandex_uid="1111", name="ivan_2"
    )

    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(
                    passport_uid="11", device_id="111", user_nickname="ivan_3"
                ),
                make_import_event_data(
                    passport_uid="11", yandex_uid="1111", user_nickname="ivan_4"
                ),
            ]
        )
    )

    lead_revision = await factory.retrieve_last_revision(
        lead_id1
    ) or await factory.retrieve_last_revision(lead_id2)

    assert lead_revision == dict(
        lead_id=Any(int),
        biz_id=123,
        passport_uid="11",
        device_id="111",
        yandex_uid="1111",
        data_source=LeadDataSource.YT,
        name="ivan_4",
    )


async def test_moves_events_of_duplicate_lead_to_head(factory, con, dm):
    await factory.create_lead_with_events(
        passport_uid=None, device_id="111", name="ivan_1", make_routes=2
    )
    await factory.create_lead_with_events(
        passport_uid=None, yandex_uid="1111", name="ivan_2", make_routes=3
    )

    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(passport_uid="11", device_id="111"),
                make_import_event_data(passport_uid="11", yandex_uid="1111"),
            ]
        )
    )

    assert (
        await con.fetchval("SELECT COUNT(*) FROM lead_events") == 7
    )  # 5 existed + 2 imported
    assert await con.fetchval("SELECT COUNT(DISTINCT lead_id) FROM lead_events") == 1


async def test_moves_new_events_of_duplicated_lead_to_head(factory, con, dm):
    await factory.create_lead(passport_uid="11", device_id=None)
    await factory.create_lead(passport_uid=None, device_id="111")

    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(passport_uid="11", device_id=None),
                make_import_event_data(passport_uid=None, device_id="111"),
                make_import_event_data(passport_uid="11", device_id="111"),
            ]
        )
    )

    assert await con.fetchval("SELECT COUNT(*) FROM lead_events") == 3
    assert await con.fetchval("SELECT COUNT(DISTINCT lead_id) FROM lead_events") == 1


async def test_processed_chain_merges(factory, dm):
    await factory.create_lead(passport_uid="22", device_id="111", yandex_uid=None)
    await factory.create_lead(passport_uid=None, device_id="111", yandex_uid=None)
    await factory.create_lead(passport_uid="33", device_id="222", yandex_uid=None)

    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(
                    passport_uid="33",
                    device_id="111",
                    yandex_uid=None,
                    event_timestamp=dt("2020-01-01 00:00:00"),
                ),
                make_import_event_data(
                    passport_uid="22",
                    device_id="111",
                    yandex_uid=None,
                    event_timestamp=dt("2020-01-02 00:00:00"),
                ),
            ]
        )
    )

    leads = await factory.list_leads()
    assert len(leads) == 1
    assert get_lead_ids(leads[0]) == ("22", "111", None)


async def test_refreshes_precalced_for_merged_leads(factory, dm):
    await factory.create_lead_with_events(
        passport_uid=None, device_id="111", name="ivan_1", make_routes=2
    )
    await factory.create_lead_with_events(
        passport_uid=None, yandex_uid="1111", name="ivan_2", make_routes=3
    )

    now = datetime.now(tz=timezone.utc)
    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(
                    passport_uid="11",
                    device_id="111",
                    event_amount=1,
                    event_timestamp=now,
                ),
                make_import_event_data(
                    passport_uid="11",
                    yandex_uid="1111",
                    event_amount=2,
                    event_timestamp=now,
                ),
            ]
        )
    )

    lead = (await factory.list_leads())[0]
    assert await factory.fetch_all_events_stat() == [
        {
            "id": Any(int),
            "biz_id": 123,
            "last_activity_timestamp": Any(datetime),
            "last_3_events_timestamp": Any(datetime),
            "last_review_rating": "5",
            "lead_id": lead["id"],
            "total_clicks_on_phone": 0,
            "total_make_routes": 5,
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
