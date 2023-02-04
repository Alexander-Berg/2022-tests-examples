import pytest

from maps_adv.geosmb.promoter.proto.leads_pb2 import (
    RemoveLeadsForGdprInput,
    RemoveLeadsForGdprOutput,
)

pytestmark = [pytest.mark.asyncio]

url = "/internal/v1/remove_leads_for_gdpr/"


async def test_deletes_all_lead_data_from_all_tables_for_all_businesses(
    factory, api, con
):
    lead_id1 = await factory.create_lead_with_events(
        passport_uid="12345", biz_id=111, clicks_on_phone=5, cta_button_click=3
    )
    lead_id2 = await factory.create_lead_with_events(
        passport_uid="12345", biz_id=222, location_sharing=2, promo_to_site=1
    )

    await api.post(
        url,
        proto=RemoveLeadsForGdprInput(passport_uid=12345),
        decode_as=RemoveLeadsForGdprOutput,
        expected_status=200,
    )

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


async def test_returns_removed_leads_details(factory, api):
    lead_id1 = await factory.create_lead_with_events(
        passport_uid="12345", biz_id=111, clicks_on_phone=5, cta_button_click=3
    )
    lead_id2 = await factory.create_lead_with_events(
        passport_uid="12345", biz_id=222, location_sharing=2, promo_to_site=1
    )

    got = await api.post(
        url,
        proto=RemoveLeadsForGdprInput(passport_uid=12345),
        decode_as=RemoveLeadsForGdprOutput,
        expected_status=200,
    )

    assert got == RemoveLeadsForGdprOutput(
        removed_leads=[
            RemoveLeadsForGdprOutput.RemovedLead(lead_id=lead_id1, biz_id=111),
            RemoveLeadsForGdprOutput.RemovedLead(lead_id=lead_id2, biz_id=222),
        ]
    )


async def test_does_not_affect_other_leads(factory, api):
    await factory.create_lead_with_events(
        passport_uid="12345", biz_id=111, clicks_on_phone=5, cta_button_click=3
    )
    not_matched_lead = await factory.create_lead_with_events(
        passport_uid="54321", biz_id=111, location_sharing=2, promo_to_site=1
    )

    await api.post(
        url,
        proto=RemoveLeadsForGdprInput(passport_uid=12345),
        decode_as=RemoveLeadsForGdprOutput,
        expected_status=200,
    )

    assert await factory.list_lead_events(not_matched_lead) != []
    assert await factory.retrieve_last_revision(not_matched_lead) is not None
    assert await factory.retrieve_lead(not_matched_lead) is not None


async def test_returns_empty_list_if_no_match_found(factory, api):
    # not_matched_lead
    await factory.create_lead_with_events(
        passport_uid="54321", biz_id=111, location_sharing=2, promo_to_site=1
    )

    got = await api.post(
        url,
        proto=RemoveLeadsForGdprInput(passport_uid=12345),
        decode_as=RemoveLeadsForGdprOutput,
        expected_status=200,
    )

    assert got == RemoveLeadsForGdprOutput(removed_leads=[])
