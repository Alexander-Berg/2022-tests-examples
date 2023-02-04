import pytest
from yandex.maps.proto.search import metrika_pb2

pytestmark = [pytest.mark.asyncio]


async def test_metrika_counter(
    client, mock_resolve_org, make_response, metrika_go_meta
):
    metrika_go_meta.counter = "counter_code_101"
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.metrika_counter == "counter_code_101"


async def test_metrika_counter_is_none_if_no_data(
    client, mock_resolve_org, make_response, metrika_go_meta
):
    metrika_go_meta.ClearField("counter")
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.metrika_counter is None


async def test_metrika_snippet_is_optional(client, mock_resolve_org, make_response):
    response = make_response()
    for metadata in response.reply.geo_object[0].metadata:
        metadata.ClearExtension(metrika_pb2.GEO_OBJECT_METADATA)
    mock_resolve_org(response)

    result = await client.resolve_org(12345)

    assert result.metrika_counter is None
