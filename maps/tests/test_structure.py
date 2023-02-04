from maps.garden.sdk.resources.scanners import BuildExternalResource, SourceDataset


def test_build_external_resource():
    struct = BuildExternalResource(
        resource_name="test_name",
        properties={"key1": "value1", "key2": "value2"}
    )

    assert struct == BuildExternalResource.from_proto(struct.to_proto())


def test_source_dataset():
    ext_resources = [
        BuildExternalResource(
            resource_name="test_name",
            properties={"key1": "value1", "key2": "value2"}
        ),
        BuildExternalResource(
            resource_name="test_name2",
            properties={"key1": "value1", "key2": "value2"}
        )
    ]

    struct = SourceDataset(
        resources=ext_resources,
        foreign_key={"foreign": "key"}
    )

    assert struct == SourceDataset.from_proto(struct.to_proto())
