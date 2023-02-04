import distutils.dir_util
from unittest import mock
import yatest.common

from maps.garden.sdk.core import Version
from maps.garden.sdk.resources import DirResource
from maps.garden.sdk import test_utils
from maps.garden.modules.masstransit_predictor_data.lib.mms_data import mk_data
from .common import create_data_mms


@mock.patch("maps.garden.sdk.yt.geobase.get_tzdata_zones_bin")
@mock.patch("maps.garden.sdk.yt.geobase.get_geobase5")
def test_mk_data(mocked_get_geobase5, mocked_get_tzdata_zones_bin, environment_settings):
    mocked_get_geobase5.return_value = "geodata5.bin"
    mocked_get_tzdata_zones_bin.return_value = yatest.common.binary_path("maps/masstransit/data/yatest_env/zones_bin")

    cook = test_utils.GraphCook(environment_settings)

    build_params = cook.create_build_params_resource(properties={"release_name": "1.0.0-0"})

    masstransit_data = DirResource("masstransit_data", "masstransit_data")
    masstransit_data.version = Version(properties={"release_name": "0.0.0-0"})
    masstransit_data.load_environment_settings(environment_settings)
    distutils.dir_util.copy_tree(
        yatest.common.binary_path(
            "maps/garden/modules/masstransit_predictor_data/tests/data/masstransit_data_0.0.0-0"
        ),
        masstransit_data.path()
    )

    data_mms = create_data_mms(environment_settings)

    task = mk_data.MkDataTask()
    task.load_environment_settings(environment_settings)
    task(
        build_params=build_params,
        masstransit_data=masstransit_data,
        data_mms=data_mms
    )
