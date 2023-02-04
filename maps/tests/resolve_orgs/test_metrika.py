import pytest
from yandex.maps.proto.search import metrika_pb2

pytestmark = [pytest.mark.asyncio]


async def test_metrika_counter(
    client, mock_resolve_orgs, make_multi_response, metrika_go_meta_multi
):
    metrika_go_meta_multi[0].counter = "counter_code_101"
    metrika_go_meta_multi[1].counter = "counter_code_202"
    mock_resolve_orgs(make_multi_response())

    result = await client.resolve_orgs([12345, 23456])

    assert [r.metrika_counter for r in result] == [
        "counter_code_101",
        "counter_code_202",
    ]


async def test_metrika_counter_is_none_if_no_data(
    client, mock_resolve_orgs, make_multi_response, metrika_go_meta_multi
):
    metrika_go_meta_multi[0].ClearField("counter")
    mock_resolve_orgs(make_multi_response())

    result = await client.resolve_orgs([12345, 23456])

    assert result[0].metrika_counter is None


async def test_metrika_snippet_is_optional(
    client, mock_resolve_orgs, make_multi_response
):
    response = make_multi_response()
    for metadata in response.reply.geo_object[0].metadata:
        metadata.ClearExtension(metrika_pb2.GEO_OBJECT_METADATA)
    mock_resolve_orgs(response)

    result = await client.resolve_orgs([12345, 23456])

    assert result[0].metrika_counter is None
