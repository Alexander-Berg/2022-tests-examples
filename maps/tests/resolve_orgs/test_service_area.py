import pytest
from yandex.maps.proto.search import experimental_pb2

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "pb_items, exected_service_area",
    [
        (
            [
                experimental_pb2.ExperimentalStorage.Item(
                    key="data_vendor", value="yandex"
                ),
                experimental_pb2.ExperimentalStorage.Item(
                    key="online_snippets/1.x",
                    value='{"is_online":true,"hide_address":true,"service_radius_km":700,"description":[]}',  # noqa
                ),
            ],
            {"service_radius_km": 700},
        ),
        (
            [
                experimental_pb2.ExperimentalStorage.Item(
                    key="data_vendor", value="yandex"
                ),
                experimental_pb2.ExperimentalStorage.Item(
                    key="online_snippets/1.x",
                    value='{"is_online": true, "hide_address": true, "geo_ids": [75], "toponym_ids": [53067690], "description": []}',  # noqa
                ),
            ],
            {"geo_ids": [75]},
        ),
    ],
)
async def test_returns_service_area(
    pb_items,
    exected_service_area,
    client,
    mock_resolve_orgs,
    make_multi_response,
    experimental_go_meta_multi,
):
    experimental_go_meta_multi[0].experimental_storage.ClearField("item")
    experimental_go_meta_multi[0].experimental_storage.item.extend(pb_items)
    mock_resolve_orgs(make_multi_response())

    result = await client.resolve_orgs([12345, 23456])

    assert [r.service_area for r in result] == [
        exected_service_area,
        {"service_radius_km": 100},
    ]


async def test_returns_none_if_org_has_no_online_snippets(
    client,
    mock_resolve_orgs,
    make_multi_response,
    experimental_go_meta_multi,
):
    experimental_go_meta_multi[0].experimental_storage.ClearField("item")
    experimental_go_meta_multi[0].experimental_storage.item.extend(
        [experimental_pb2.ExperimentalStorage.Item(key="data_vendor", value="yandex")]
    )
    mock_resolve_orgs(make_multi_response())

    result = await client.resolve_orgs([12345, 23456])

    assert result[0].service_area is None


async def test_experimental_extension_is_optional(
    client, mock_resolve_orgs, make_multi_response
):
    response = make_multi_response()
    for metadata in response.reply.geo_object[0].metadata:
        metadata.ClearExtension(experimental_pb2.GEO_OBJECT_METADATA)
    mock_resolve_orgs(response)

    result = await client.resolve_orgs([12345, 23456])

    assert result[0].service_area is None


async def test_experimental_storage_in_experimental_extension_is_optional(
    client,
    mock_resolve_org,
    make_multi_response,
    experimental_go_meta_multi,
):
    experimental_go_meta_multi[0].ClearField("experimental_storage")
    mock_resolve_org(make_multi_response())

    result = await client.resolve_orgs([12345, 23456])

    assert result[0].service_area is None
