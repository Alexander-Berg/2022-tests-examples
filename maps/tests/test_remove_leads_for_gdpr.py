import pytest
from aiohttp.web import Response

from maps_adv.geosmb.promoter.proto import leads_pb2

pytestmark = [pytest.mark.asyncio]


output_pb = leads_pb2.RemoveLeadsForGdprOutput(
    removed_leads=[
        leads_pb2.RemoveLeadsForGdprOutput.RemovedLead(lead_id=1, biz_id=111),
        leads_pb2.RemoveLeadsForGdprOutput.RemovedLead(lead_id=2, biz_id=222),
    ]
)


async def test_sends_correct_request(client, mock_remove_leads_for_gdpr):
    request_path = None
    request_body = None

    async def _handler(request):
        nonlocal request_path, request_body
        request_path = request.path
        request_body = await request.read()
        return Response(status=200, body=output_pb.SerializeToString())

    mock_remove_leads_for_gdpr(_handler)

    await client.remove_leads_for_gdpr(passport_uid=54321)

    assert request_path == "/internal/v1/remove_leads_for_gdpr/"
    proto_body = leads_pb2.RemoveLeadsForGdprInput.FromString(request_body)
    assert proto_body == leads_pb2.RemoveLeadsForGdprInput(passport_uid=54321)


async def test_parses_response_correctly(client, mock_remove_leads_for_gdpr):
    mock_remove_leads_for_gdpr(
        lambda _: Response(status=200, body=output_pb.SerializeToString())
    )

    got = await client.remove_leads_for_gdpr(passport_uid=123)

    assert got == [{"lead_id": 1, "biz_id": 111}, {"lead_id": 2, "biz_id": 222}]
