import os
import os.path
import pytest
import tarfile

import yatest.common

from maps.garden.libs.road_graph_builder import common
from maps.garden.modules.pedestrian_graph.lib import graph as pedestrian_graph
from maps.garden.sdk import test_utils
from maps.garden.sdk.test_utils import ymapsdf
from yandex.maps.road_graph import road_graph as rg


_RELEASE = "20.03.01-3"

_GEODATA6_BINARY_PATH = yatest.common.binary_path(
    "geobase/data/v6/geodata6.bin")
_TZDATA_ARCHIVE_BINARY_PATH = yatest.common.binary_path(
    "maps/data/test/tzdata/tzdata.tar.gz")

_TZDATA_PATH = yatest.common.binary_path("maps/data/test/tzdata")
_TZDATA_ZONES_BIN_PATH = os.path.join(_TZDATA_PATH, "zones_bin")
_YMAPSDF_TEST_DATA_PATH = yatest.common.work_path(
    "extract/home/maps/core/garden/prod/ymapsdf/latest/cis1")


def unpack_tzdata_to(target_path):
    tzdata_archive_path = _TZDATA_ARCHIVE_BINARY_PATH
    with tarfile.open(tzdata_archive_path) as tzdata_tar_file:
        tzdata_tar_file.extractall(target_path)


def prepare_config(config):
    config['validation']['config_path'] = 'pedestrian_graph/validation_config_test.json'
    config['l6a_preprocess'] = 'pedestrian_graph/l6a_preprocess_test.cfg'


def check_road_graph(road_graph: rg.RoadGraph):
    assert road_graph.vertices_number == 6688
    assert road_graph.edges_number == 16312
    assert road_graph.roads_number == 95


@pytest.mark.use_local_yt_yql
def test_module_tomat(environment_settings, yt_client, mocker):
    unpack_tzdata_to(_TZDATA_PATH)
    mocker.patch("maps.garden.libs.road_graph_builder.common.PATH_TO_GEODATA", _GEODATA6_BINARY_PATH)
    mocker.patch("maps.garden.libs.road_graph_builder.common.PATH_TO_TZDATA_ZONES_BIN", _TZDATA_ZONES_BIN_PATH)

    cook = test_utils.GraphCook(environment_settings)
    ymapsdf.fill_graph(cook.input_builder(), ymapsdf.TEST_REGIONS)
    prepare_config(pedestrian_graph.config)
    pedestrian_graph.fill_graph(cook.target_builder(), ymapsdf.TEST_REGIONS)

    cook.create_build_params_resource(properties={"release": _RELEASE})

    ymapsdf.create_final_resources(
        cook,
        table_names=[t.value for t in common.YmapsdfTables],
        data_dir=_YMAPSDF_TEST_DATA_PATH)

    result = test_utils.execute(cook)

    resource_maker = common.pedestrian_resource_maker()
    road_graph_file_resource = None
    for output_resource in result.values():
        if output_resource.name == resource_maker.file_resource_name(
                common.Files.COMPACT):
            road_graph_file_resource = output_resource
            break
    assert road_graph_file_resource is not None

    road_graph_file_resource.ensure_available()
    road_graph = rg.RoadGraph(road_graph_file_resource.path())
    check_road_graph(road_graph)
