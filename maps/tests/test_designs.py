from unittest import mock
import os
import shutil
import tarfile

import io

from yatest.common import test_source_path
from maps.garden.sdk import test_utils as garden_test_utils
from maps.garden.modules.design_bundles.lib.graph import (
    merge_designs_config,
    fill_graph,
    JAMS_DESIGN_DATASET_RESOURCE_NAME,
    ROAD_EVENTS_DESIGN_DATASET_RESOURCE_NAME,
    STV_DESIGN_DATASET_RESOURCE_NAME,
    STV_DESIGN_VERSION_DATASET_RESOURCE_NAME,
    MRC_DESIGN_DATASET_RESOURCE_NAME)

from maps.garden.modules.renderer_map_stable_bundle.lib import (
    graph as stable_map_bundle_graph,
    bundle_info as stable_map_bundle_info)
from maps.garden.modules.renderer_navi_stable_bundle.lib import (
    graph as stable_navi_bundle_graph,
    bundle_info as stable_navi_bundle_info)
from maps.garden.modules.stylerepo_map_design_src.lib import (
    graph as testing_map_bundle_graph,
    bundle_info as testing_map_bundle_info)
from maps.garden.modules.stylerepo_navi_design_src.lib import (
    graph as testing_navi_bundle_graph,
    bundle_info as testing_navi_bundle_info)
from maps.garden.modules.stylerepo_experiments_src.lib import \
    graph as experiments_graph

from maps.garden.libs.stylerepo.test_utils import \
    test_utils as stylerepo_test_utils


RELEASE_NAME = '20.10.10-1'


def prepare_merge_designs_config():
    icons_tario = io.BytesIO()
    path = test_source_path("data/frozen_road_events_design/icons.tar")
    with tarfile.open(fileobj=icons_tario, mode='w') as icons_tar:
        for dirpath, dirnames, filenames in os.walk(path):
            for filename in filenames:
                full_name = os.path.join(dirpath, filename)
                icons_tar.add(os.path.join(path, full_name),
                              arcname=os.path.relpath(full_name, path))

    merge_designs_config['frozen_trfe_design_json'] = 'frozen_trfe_design_json_test'
    merge_designs_config['frozen_trfe_design_icons_object'] = icons_tario.getvalue()


def read_dir(root_dir):
    dir_content = {}
    nonunique_names = []  # tar can contain several members with the same name
    for dirpath, dirnames, filenames in os.walk(root_dir):
        for filename in filenames:
            filepath = os.path.join(dirpath, filename)
            rel_path = os.path.relpath(filepath, root_dir)
            if filename.endswith('.tar'):
                tar = tarfile.open(filepath)
                for file_info in tar.getmembers():
                    name = os.path.join(rel_path, file_info.name)
                    content = tar.extractfile(file_info)
                    dir_content[name] = content.read().decode()
                    nonunique_names.append(name)
            else:
                with open(filepath) as f:
                    dir_content[rel_path] = f.read()
                    nonunique_names.append(rel_path)
    return dir_content, nonunique_names


def assert_dirs_equal(expected_dir, actual_dir):
    expected_files, expected_names = read_dir(expected_dir)
    actual_files, actual_names = read_dir(actual_dir)
    assert '\n'.join(sorted(expected_files.keys())) == '\n'.join(sorted(actual_names))
    for name in actual_files.keys():
        expected = name + '\n' + expected_files[name]
        actual = name + '\n' + actual_files[name]
        assert expected == actual


def canonize_dir(path, test_result):
    shutil.rmtree(path, ignore_errors=True)
    files, names = read_dir(test_result)
    for rel_path, content in files.items():
        filename = os.path.join(path, rel_path)
        os.makedirs(os.path.dirname(filename), exist_ok=True)
        with open(filename, 'w') as f:
            f.write(content)


def assert_properties(resource, stable_revision, testing_revision):
    assert resource.version.properties['release_name'] == RELEASE_NAME
    assert resource.version.properties['stable_revisions'] == stable_revision
    assert resource.version.properties['testing_revisions'] == testing_revision


def check_dir(expected, actual):
    if os.getenv('YA_MAPS_CANONIZE_TESTS'):
        canonize_dir(expected, actual)
    try:
        assert_dirs_equal(expected, actual)
    except AssertionError:
        assert False, 'Call `YA_MAPS_CANONIZE_TESTS=1 ya make -t` to update canonical test data'


def check_trf_design(resources):
    jams_design_dataset = resources[JAMS_DESIGN_DATASET_RESOURCE_NAME]
    assert jams_design_dataset.dataset_name == 'yandex-maps-jams-design'
    assert jams_design_dataset.dataset_version == '20.10.10-1-1234-5678'
    assert_properties(jams_design_dataset, '1234-5678', '1111-2222')

    trf_design_dir = resources['trf_design_dir']
    assert_properties(trf_design_dir, '1234-5678', '1111-2222')

    expected_trf_design = test_source_path('data/expected_trf_design')
    check_dir(expected_trf_design, trf_design_dir.path())


def check_trfe_design(resources):
    road_events_design_dataset = resources[ROAD_EVENTS_DESIGN_DATASET_RESOURCE_NAME]
    assert road_events_design_dataset.dataset_name == 'yandex-maps-road-events-design'
    assert road_events_design_dataset.dataset_version == '20.10.10-1-1234'
    assert_properties(road_events_design_dataset, '1234', '2222')

    trfe_design_dir = resources['trfe_design_dir']
    assert_properties(trfe_design_dir, '1234', '2222')

    expected_trfe_design = test_source_path('data/expected_trfe_design')
    check_dir(expected_trfe_design, trfe_design_dir.path())


def check_stv_design(resources):
    stv_design_dataset = resources[STV_DESIGN_DATASET_RESOURCE_NAME]
    assert stv_design_dataset.dataset_name == 'yandex-maps-streetview-design'
    assert stv_design_dataset.dataset_version == '20.10.10-1-1234'
    assert_properties(stv_design_dataset, '1234', '2222')

    stv_design_dir = resources['stv_design_dir']
    assert_properties(stv_design_dir, '1234', '2222')

    stv_design_version = resources[STV_DESIGN_VERSION_DATASET_RESOURCE_NAME]
    assert_properties(stv_design_version, '1234', '2222')
    assert stv_design_version.dataset_name == 'yandex-maps-streetview-design-version'
    assert stv_design_version.dataset_version == '20.10.10-1-1234'

    expected_stv_design = test_source_path('data/expected_stv_design')
    check_dir(expected_stv_design, stv_design_dir.path())


def check_mrc_design(resources):
    mrc_design_dataset = resources[MRC_DESIGN_DATASET_RESOURCE_NAME]
    assert mrc_design_dataset.dataset_name == 'yandex-maps-mrc-design'
    assert mrc_design_dataset.dataset_version == '20.10.10-1-1234'
    assert_properties(mrc_design_dataset, '1234', '2222')

    mrc_design_dir = resources['mrc_design_dir']
    assert_properties(mrc_design_dir, '1234', '2222')

    expected_mrc_design = test_source_path('data/expected_mrc_design')
    check_dir(expected_mrc_design, mrc_design_dir.path())


@mock.patch(
    'maps.garden.sdk.ecstatic.tasks.UploadDatasetTask.__call__',
    new=mock.MagicMock())
def test_designs(environment_settings):
    prepare_merge_designs_config()

    cook = garden_test_utils.GraphCook(environment_settings)

    stable_map_bundle_graph.fill_graph(cook.input_builder(), regions=None)
    stable_navi_bundle_graph.fill_graph(cook.input_builder(), regions=None)
    testing_map_bundle_graph.fill_graph(cook.input_builder(), regions=None)
    testing_navi_bundle_graph.fill_graph(cook.input_builder(), regions=None)
    experiments_graph.fill_graph(cook.input_builder(), regions=None)
    fill_graph(cook.target_builder(), regions=None)

    cook.create_build_params_resource(
        properties={'release_name': RELEASE_NAME})

    stylerepo_test_utils.create_design_resources(
        cook,
        stable_map_bundle_info.get_map_bundle_info(),
        stable_navi_bundle_info.get_navi_bundle_info(),
        testing_map_bundle_info.get_map_design_bundle_info(),
        testing_navi_bundle_info.get_navi_design_bundle_info())

    result_resources = garden_test_utils.execute(cook)

    for resource in result_resources.values():
        resource.ensure_available()

    check_trf_design(result_resources)
    check_trfe_design(result_resources)
    check_stv_design(result_resources)
    check_mrc_design(result_resources)
