import pytest

from maps_adv.geosmb.promoter.proto.leads_pb2 import (
    SearchLeadsForGdprInput,
    SearchLeadsForGdprOutput,
)

pytestmark = [pytest.mark.asyncio]

url = "/internal/v1/search_leads_for_gdpr/"


async def test_returns_true_if_matched_by_passport(factory, api):
    await factory.create_lead(passport_uid="12345")

    got = await api.post(
        url,
        proto=SearchLeadsForGdprInput(passport_uid=12345),
        decode_as=SearchLeadsForGdprOutput,
        expected_status=200,
    )

    assert got == SearchLeadsForGdprOutput(leads_exist=True)


async def test_returns_false_if_is_not_matched_by_passport(factory, api):
    await factory.create_lead(passport_uid="111")

    got = await api.post(
        url,
        proto=SearchLeadsForGdprInput(passport_uid=222),
        decode_as=SearchLeadsForGdprOutput,
        expected_status=200,
    )

    assert got == SearchLeadsForGdprOutput(leads_exist=False)
