import pytest
from yandex.maps.proto.search import business_pb2

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    ("snippet", "expected"),
    [
        (
            business_pb2.Snippet(feature_ref=["feature_id_1"]),
            [{"id": "feature_id_1", "value": True}],
        ),
        (
            business_pb2.Snippet(feature_ref=["feature_id_3"]),
            [{"id": "feature_id_3", "value": ["text"]}],
        ),
        (
            business_pb2.Snippet(feature_ref=["feature_id_3", "feature_id_1"]),
            [
                {"id": "feature_id_3", "value": ["text"]},
                {"id": "feature_id_1", "value": True},
            ],
        ),
        (
            business_pb2.Snippet(feature_ref=["feature_id_1", "feature_id_4"]),
            [{"id": "feature_id_1", "value": True}],
        ),
        (business_pb2.Snippet(feature_ref=["feature_id_4"]), []),
        (business_pb2.Snippet(feature_ref=[]), []),
    ],
)
async def test_snippet_features(
    client, mock_resolve_org, make_response, business_go_meta, snippet, expected
):
    business_go_meta.ClearField("feature")
    business_go_meta.feature.extend(
        [
            business_pb2.Feature(
                id="feature_id_1", value=business_pb2.Feature.Value(boolean_value=True)
            ),
            business_pb2.Feature(
                id="feature_id_2", value=business_pb2.Feature.Value(boolean_value=False)
            ),
            business_pb2.Feature(
                id="feature_id_3", value=business_pb2.Feature.Value(text_value=["text"])
            ),
        ]
    )
    business_go_meta.snippet.CopyFrom(snippet)
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.snippet_features == expected


@pytest.mark.parametrize(
    ("pb_feature_value", "expected_feature_value"),
    [
        (business_pb2.Feature.Value(boolean_value=True), True),
        (
            business_pb2.Feature.Value(text_value=["string1", "string2"]),
            ["string1", "string2"],
        ),
        (
            business_pb2.Feature.Value(
                enum_value=[
                    business_pb2.EnumItem(id="enum_value1", name="Значение 1"),
                    business_pb2.EnumItem(
                        id="enum_value2",
                        name="Значение 2",
                        image_url_template="https://images.com/enum_value_2",
                    ),
                ]
            ),
            [
                {"id": "enum_value1", "name": "Значение 1"},
                {
                    "id": "enum_value2",
                    "name": "Значение 2",
                    "image_url_template": "https://images.com/enum_value_2",
                },
            ],
        ),
    ],
)
async def test_snippet_features_types(
    client,
    mock_resolve_org,
    make_response,
    business_go_meta,
    pb_feature_value,
    expected_feature_value,
):
    business_go_meta.ClearField("feature")
    business_go_meta.feature.extend(
        [business_pb2.Feature(id="some_id", value=pb_feature_value)]
    )
    business_go_meta.snippet.CopyFrom(business_pb2.Snippet(feature_ref=["some_id"]))
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.snippet_features[0]["value"] == expected_feature_value


async def test_snippet_features_optional(
    client, mock_resolve_org, make_response, business_go_meta
):
    business_go_meta.ClearField("snippet")
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.snippet_features == []
    assert result.features == [
        {"id": "feature_11", "name": "Оплата картой", "value": True, "aref": "#yes.ru"},
        {"id": "feature_22", "value": False},
    ]


async def test_features(client, mock_resolve_org, make_response):
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.features == [
        {"id": "feature_11", "name": "Оплата картой", "value": True, "aref": "#yes.ru"},
        {"id": "feature_22", "value": False},
    ]


@pytest.mark.parametrize(
    ("pb_feature_value", "expected_feature_value"),
    [
        (business_pb2.Feature.Value(boolean_value=True), True),
        (
            business_pb2.Feature.Value(text_value=["string1", "string2"]),
            ["string1", "string2"],
        ),
        (
            business_pb2.Feature.Value(
                enum_value=[
                    business_pb2.EnumItem(id="enum_value1", name="Значение 1"),
                    business_pb2.EnumItem(
                        id="enum_value2",
                        name="Значение 2",
                        image_url_template="https://images.com/enum_value_2",
                    ),
                ]
            ),
            [
                {"id": "enum_value1", "name": "Значение 1"},
                {
                    "id": "enum_value2",
                    "name": "Значение 2",
                    "image_url_template": "https://images.com/enum_value_2",
                },
            ],
        ),
    ],
)
async def test_features_types(
    client,
    mock_resolve_org,
    make_response,
    business_go_meta,
    pb_feature_value,
    expected_feature_value,
):
    business_go_meta.ClearField("feature")
    business_go_meta.feature.extend(
        [business_pb2.Feature(id="some_id", value=pb_feature_value)]
    )
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.features[0]["value"] == expected_feature_value


@pytest.mark.parametrize(
    ("snippet", "expected"),
    [
        (
            business_pb2.Snippet(feature_ref=["feature_id_3"]),
            [
                {"id": "feature_id_3", "value": ["text"]},
                {"id": "feature_id_1", "value": True},
                {"id": "feature_id_2", "value": False},
            ],
        ),
        (
            business_pb2.Snippet(feature_ref=["feature_id_2", "feature_id_1"]),
            [
                {"id": "feature_id_2", "value": False},
                {"id": "feature_id_1", "value": True},
                {"id": "feature_id_3", "value": ["text"]},
            ],
        ),
        (
            business_pb2.Snippet(feature_ref=["feature_id_2", "feature_id_4"]),
            [
                {"id": "feature_id_2", "value": False},
                {"id": "feature_id_1", "value": True},
                {"id": "feature_id_3", "value": ["text"]},
            ],
        ),
        (
            business_pb2.Snippet(feature_ref=["feature_id_4"]),
            [
                {"id": "feature_id_1", "value": True},
                {"id": "feature_id_2", "value": False},
                {"id": "feature_id_3", "value": ["text"]},
            ],
        ),
        (
            business_pb2.Snippet(feature_ref=[]),
            [
                {"id": "feature_id_1", "value": True},
                {"id": "feature_id_2", "value": False},
                {"id": "feature_id_3", "value": ["text"]},
            ],
        ),
    ],
)
async def test_features_snippet_features_listed_first(
    client, mock_resolve_org, make_response, business_go_meta, snippet, expected
):
    business_go_meta.ClearField("feature")
    business_go_meta.feature.extend(
        [
            business_pb2.Feature(
                id="feature_id_1", value=business_pb2.Feature.Value(boolean_value=True)
            ),
            business_pb2.Feature(
                id="feature_id_2", value=business_pb2.Feature.Value(boolean_value=False)
            ),
            business_pb2.Feature(
                id="feature_id_3", value=business_pb2.Feature.Value(text_value=["text"])
            ),
        ]
    )
    business_go_meta.snippet.CopyFrom(snippet)
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.features == expected
