import os.path
import pytest
import tarfile

from yt.wrapper import ypath
import yatest.common

from maps.garden.libs.road_graph_builder import common
from maps.garden.modules.pedestrian_graph.lib import graph as pedestrian_graph
from maps.garden.modules.ymapsdf import defs as ymapsdf_defs
from maps.garden.sdk import test_utils
from maps.garden.sdk.test_utils import ymapsdf
from maps.garden.sdk.yt import YtTableResource
from maps.garden.sdk.yt import utils as yt_utils

import data


_RELEASE = "20.03.01-3"
YT_CLUSTER = "plato"

_GEODATA6_BINARY_PATH = yatest.common.binary_path(
    "geobase/data/v6/geodata6.bin")
_TZDATA_ARCHIVE_BINARY_PATH = yatest.common.binary_path(
    "maps/data/test/tzdata/tzdata.tar.gz")

_TZDATA_PATH = yatest.common.binary_path("maps/data/test/tzdata")
_TZDATA_ZONES_BIN_PATH = os.path.join(_TZDATA_PATH, "zones_bin")


def unpack_tzdata_to(target_path):
    tzdata_archive_path = _TZDATA_ARCHIVE_BINARY_PATH
    with tarfile.open(tzdata_archive_path) as tzdata_tar_file:
        tzdata_tar_file.extractall(target_path)


@pytest.fixture
def yt_client(environment_settings):
    yt_server_settings = yt_utils.get_server_settings(
        yt_utils.get_yt_settings(environment_settings),
        server=YT_CLUSTER)
    return yt_utils.get_yt_client(yt_server_settings)


def prepare_config(config):
    config['validation']['config_path'] = 'pedestrian_graph/validation_config_test.json'
    config['l6a_preprocess'] = 'pedestrian_graph/l6a_preprocess_test.cfg'


@pytest.mark.use_local_yt_yql
def test_module(environment_settings, yt_client):
    unpack_tzdata_to(_TZDATA_PATH)
    common.PATH_TO_GEODATA = _GEODATA6_BINARY_PATH
    common.PATH_TO_TZDATA_ZONES_BIN = _TZDATA_ZONES_BIN_PATH

    cook = test_utils.GraphCook(environment_settings)

    ymapsdf.fill_graph(cook.input_builder(), ymapsdf.TEST_REGIONS)
    prepare_config(pedestrian_graph.config)
    pedestrian_graph.fill_graph(cook.target_builder(), ymapsdf.TEST_REGIONS)

    cook.create_build_params_resource(properties={"release": _RELEASE})

    # we might not use all the tables in common.YmapsdfTables
    # so let's filter them to get rid of assert in create_final_resources
    def table_is_used(table):
        resource_name = common.get_ymapsdf_resource_name(table, ymapsdf.TEST_REGION, ymapsdf.TEST_VENDOR, ymapsdf_defs.FINAL_STAGE)
        return resource_name in cook.target_builder().input_resources()
    table_names = [t.value for t in common.YmapsdfTables if table_is_used(t)]

    ymapsdf.create_final_resources(cook, table_names=table_names)

    ymapsdf_directory = ypath.ypath_dirname(
        ymapsdf.construct_abs_yt_path(
            environment_settings,
            "edge",
            ymapsdf.TEST_REGION,
            ymapsdf.TEST_VENDOR))
    for resource in cook.input_resources():
        if not isinstance(resource, YtTableResource):
            continue
        assert ypath.ypath_dirname(resource.path) == ymapsdf_directory

    data.create_test_graph(yt_client, ymapsdf_directory)

    test_utils.execute(cook)
