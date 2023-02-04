from maps.garden.libs.masstransit_resources.common import MASSTRANSIT_DATA_ON_YT_RESOURCE
from maps.garden.sdk import test_utils
from maps.garden.sdk.core import Version
from maps.garden.modules.masstransit_tester.lib import masstransit_data_validation
from maps.garden.sdk.yt import YtFileResource
from maps.garden.sdk.yt.utils import get_yt_settings, get_server_settings, get_garden_prefix
from yt.wrapper.ypath import ypath_join
from yatest.common import binary_path
from unittest import mock
import pytest

import os


MASSTRANSIT_DATA_PATH_TMPL = "/masstransit_data/{release}"


def prepare_data_in_yt(yt_client, yt_prefix, release):
    local_data_path = binary_path("maps/masstransit/data/test_mtrouter_fb_data")

    masstransit_data_path = ypath_join(yt_prefix, MASSTRANSIT_DATA_PATH_TMPL.format(release=release))
    yt_client.create("map_node", masstransit_data_path, recursive=True, ignore_existing=True)
    with open(os.path.join(local_data_path, "static.fb"), "rb") as f:
        yt_client.write_file(ypath_join(masstransit_data_path, "static.fb"), f)
    symlink_path = ypath_join(yt_prefix, MASSTRANSIT_DATA_PATH_TMPL.format(release="latest"))
    yt_client.link(masstransit_data_path, symlink_path, force=True)


@mock.patch("maps.garden.modules.masstransit_tester.lib.masstransit_data_validation.upload_report_to_sandbox")
@mock.patch("maps.garden.sdk.yt.geobase.get_tzdata_zones_bin")
@mock.patch("maps.garden.sdk.yt.geobase.get_geobase6")
@pytest.mark.use_local_yt("hahn")
def test_module(mocked_get_geobase6, mocked_get_tzdata_zones_bin, mocked_upload_report_to_sandbox, environment_settings):
    mocked_get_geobase6.return_value = "geodata6.bin"
    mocked_get_tzdata_zones_bin.return_value = binary_path("maps/masstransit/data/yatest_env/zones_bin")
    mocked_upload_report_to_sandbox.return_value = "https://proxy.sandbox.yandex-team.ru"
    cook = test_utils.GraphCook(environment_settings)
    cook.input_builder().add_resource(YtFileResource(
        MASSTRANSIT_DATA_ON_YT_RESOURCE, MASSTRANSIT_DATA_PATH_TMPL, server="hahn"))
    masstransit_data_validation.fill_graph(cook.target_builder())

    release = "19.08.05-0"
    input_properties = {"release": release}
    masstransit_data_resource = cook.create_input_resource(MASSTRANSIT_DATA_ON_YT_RESOURCE)
    masstransit_data_resource.version = Version(properties=input_properties)

    yt_prefix = get_garden_prefix(get_server_settings(
        get_yt_settings(environment_settings), server="hahn"))
    prepare_data_in_yt(masstransit_data_resource.get_yt_client(), yt_prefix, release)

    test_utils.execute(cook)
