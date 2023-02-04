import pytest

from yt.wrapper.ypath import ypath_join, ypath_split

from maps.garden.sdk.resources.scanners import BuildExternalResource, SourceDataset, scan_yt

YT_TABLE_TO_RESOURSE_NAME = {
    'entrance_flat_range': 'entrance_flat_range',
    'entrance_level_flat_range': 'entrance_level_flat_range',
}
MAX_FLATS_BUNDLES = 2
YT_PATH_FLATS_SCHEMA = '//home/maps/poi/flats/{environment}/export_data'
YT_PATH_SYMLINK = 'latest'
YT_SERVER = 'hahn'
ENV="stable"


def _get_internal_paths(yt_path: str):
    for table in YT_TABLE_TO_RESOURSE_NAME.keys():
        yield ypath_join(yt_path, table)


def _get_resource_properties(yt_path: str):
    dataset_yt_parht, _ = ypath_split(yt_path)
    _, release_name = ypath_split(dataset_yt_parht)
    return {
        "release_name": release_name,
        "yt_path": yt_path,
    }


def _get_resource_name(yt_path: str):
    _, table_name = ypath_split(yt_path)
    return YT_TABLE_TO_RESOURSE_NAME[table_name]


def _scan_resources(environment_settings):
    return scan_yt(
        environment_settings=environment_settings,
        yt_base_path=YT_PATH_FLATS_SCHEMA.format(environment=ENV),
        yt_nodes_filter=lambda node: node != YT_PATH_SYMLINK,
        yt_nodes_limit=MAX_FLATS_BUNDLES,
        yt_get_internal_paths=_get_internal_paths,
        get_garden_resource_properties_func=_get_resource_properties,
        get_garden_resource_name_func=_get_resource_name
    )


@pytest.mark.use_local_yt(YT_SERVER)
def test_scan_yt_resources(environment_settings, yt_client):
    flats_releases = [YT_PATH_SYMLINK, '2021-10-23', '2021-10-24', '2021-10-25']

    for release_name in flats_releases:
        yt_client.create(
            'map_node',
            ypath_join(YT_PATH_FLATS_SCHEMA.format(environment=ENV), release_name),
            recursive=True)
        for table in YT_TABLE_TO_RESOURSE_NAME:
            yt_client.create(
                'table',
                ypath_join(YT_PATH_FLATS_SCHEMA.format(environment=ENV), release_name, table),
                recursive=True)

    datasets = list(_scan_resources(environment_settings))
    assert len(datasets) == 2

    for dataset in datasets:
        assert isinstance(dataset, SourceDataset)
        foreign_key = dataset.foreign_key
        resources = dataset.resources
        assert len(resources) == len(YT_TABLE_TO_RESOURSE_NAME)
        for resource in resources:
            assert isinstance(resource, BuildExternalResource)
            yt_dir, table = ypath_split(resource.properties["yt_path"])
            assert '2021-10-23' not in yt_dir
            assert YT_PATH_SYMLINK not in yt_dir
            assert foreign_key == {'yt_path': yt_dir}
            assert table in YT_TABLE_TO_RESOURSE_NAME
