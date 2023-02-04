import pytest

pytestmark = [pytest.mark.asyncio]


async def test_deletes_all_lead_data_from_all_tables_for_all_businesses(
    factory, dm, con
):
    lead_id1 = await factory.create_lead_with_events(
        passport_uid="12345", biz_id=111, clicks_on_phone=5, cta_button_click=3
    )
    lead_id2 = await factory.create_lead_with_events(
        passport_uid="12345", biz_id=222, location_sharing=2, promo_to_site=1
    )

    await dm.delete_leads_data_by_passport(passport_uid="12345")

    assert await factory.list_lead_events(lead_id1) == []
    assert await factory.list_lead_events(lead_id2) == []
    assert await factory.retrieve_last_revision(lead_id1) is None
    assert await factory.retrieve_last_revision(lead_id2) is None
    assert await factory.retrieve_lead(lead_id1) is None
    assert await factory.retrieve_lead(lead_id2) is None
    assert (
        await con.fetchval(
            f"""SELECT EXISTS(
                SELECT *
                FROM events_stat_precalced
                WHERE lead_id IN ({lead_id1}, {lead_id2})
            )"""
        )
        is False
    )


async def test_returns_removed_leads_details(factory, dm):
    lead_id1 = await factory.create_lead_with_events(
        passport_uid="12345", biz_id=111, clicks_on_phone=5, cta_button_click=3
    )
    lead_id2 = await factory.create_lead_with_events(
        passport_uid="12345", biz_id=222, location_sharing=2, promo_to_site=1
    )

    got = await dm.delete_leads_data_by_passport(passport_uid="12345")

    assert got == [
        dict(lead_id=lead_id1, biz_id=111),
        dict(lead_id=lead_id2, biz_id=222),
    ]


async def test_does_not_affect_other_leads(factory, dm):
    await factory.create_lead_with_events(
        passport_uid="12345", biz_id=111, clicks_on_phone=5, cta_button_click=3
    )
    not_matched_lead = await factory.create_lead_with_events(
        passport_uid="54321", biz_id=111, location_sharing=2, promo_to_site=1
    )

    await dm.delete_leads_data_by_passport(passport_uid="12345")

    assert await factory.list_lead_events(not_matched_lead) != []
    assert await factory.retrieve_last_revision(not_matched_lead) is not None
    assert await factory.retrieve_lead(not_matched_lead) is not None


async def test_returns_empty_list_if_no_match_found(factory, dm):
    # not_matched_lead
    await factory.create_lead_with_events(
        passport_uid="54321", biz_id=111, location_sharing=2, promo_to_site=1
    )

    got = await dm.delete_leads_data_by_passport(passport_uid="12345")

    assert got == []
