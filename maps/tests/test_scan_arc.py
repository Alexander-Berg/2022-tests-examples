from typing import Any
import unittest.mock

from arc.api.public.shared_pb2 import Commit

from maps.garden.sdk.resources.scanners.common \
    import BuildExternalResource, SourceDataset, scan_arc


@unittest.mock.patch(
    "maps.garden.sdk.resources.scanners.common.ArcClient", autospec=True)
def test_scan_arc_resources(MockArcClient):
    mock_arc_token = "mock arc token"
    mock_environment_settings = {"arcanum": {"token": mock_arc_token}}
    mock_file_path = "mock/file/path"
    mock_resource_name = "mock_resource_name"
    commit_limit = 10
    commits = [
        Commit(Oid="r1"),
        Commit(Oid="r2"),
        Commit(Oid="r3"),
    ]

    mock_arc_client = MockArcClient.return_value

    def commit_properties(commit: Commit) -> dict[str, Any]:
        return {
            "shipping_date": commit.Timestamp.ToDatetime().strftime(
                "%Y-%m-%d - %H:%M:%S"),
            "path": mock_file_path,
            "revision": commit.Oid,
        }

    mock_arc_client.get_log.return_value = commits
    datasets = list(scan_arc(
        environment_settings=mock_environment_settings,
        path=mock_file_path,
        get_garden_resource_properties_func=commit_properties,
        get_garden_resource_name_func=lambda commit: mock_resource_name,
        commit_limit=commit_limit))
    MockArcClient.assert_called_with(oauth_token=mock_arc_token)
    mock_arc_client.get_log.assert_called_with(
        path=mock_file_path, limit=commit_limit)

    assert len(datasets) == len(commits)
    assert all(isinstance(d, SourceDataset) for d in datasets)
    for dataset, commit in zip(datasets, commits):
        assert dataset.foreign_key == {
            "path": mock_file_path,
            "revision": commit.Oid,
        }
        assert len(dataset.resources) == 1
        resource = dataset.resources[0]
        assert isinstance(resource, BuildExternalResource)
        assert resource.resource_name == mock_resource_name
        assert resource.properties == commit_properties(commit)
