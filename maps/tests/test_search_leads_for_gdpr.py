import pytest
from aiohttp.web import Response

from maps_adv.geosmb.promoter.proto import leads_pb2

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(client, mock_search_leads_for_gdpr):
    request_path = None
    request_body = None

    async def _handler(request):
        nonlocal request_path, request_body
        request_path = request.path
        request_body = await request.read()
        return Response(
            status=200,
            body=leads_pb2.SearchLeadsForGdprOutput(
                leads_exist=True
            ).SerializeToString(),
        )

    mock_search_leads_for_gdpr(_handler)

    await client.search_leads_for_gdpr(passport_uid=54321)

    assert request_path == "/internal/v1/search_leads_for_gdpr/"
    proto_body = leads_pb2.SearchLeadsForGdprInput.FromString(request_body)
    assert proto_body == leads_pb2.SearchLeadsForGdprInput(passport_uid=54321)


@pytest.mark.parametrize("result", [True, False])
async def test_parses_response_correctly(result, client, mock_search_leads_for_gdpr):
    mock_search_leads_for_gdpr(
        lambda _: Response(
            status=200,
            body=leads_pb2.SearchLeadsForGdprOutput(
                leads_exist=result
            ).SerializeToString(),
        )
    )

    got = await client.search_leads_for_gdpr(passport_uid=123)

    assert got is result
