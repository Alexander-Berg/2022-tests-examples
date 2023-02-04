import shutil
import yatest.common

from maps.garden.sdk.core import Version
from maps.garden.sdk import test_utils
from maps.garden.sdk.test_utils import ymapsdf
from maps.garden.modules.masstransit_static.lib import masstransit_static
from maps.garden.modules.geocoder_export.lib import geocoder_export
from maps.garden.modules.renderer_denormalization.lib import graph
from maps.garden.modules.renderer_denormalization.tests.lib import geocoder, common


DATA_PATH = yatest.common.source_path("maps/renderer/denormalization/test_data")
DATA_BUILD_PATH = yatest.common.binary_path("maps/renderer/denormalization/test_data")

# Hardcoded with use_local_yt_yql
YT_SERVER = "plato"


def _prepare_masstransit_data(cook):
    resource_data_path = DATA_BUILD_PATH + "/masstransit_data"

    resource = cook.create_input_resource("masstransit_data")
    resource.version = Version(properties={"release_name": "0.0.0-0"})
    resource.load_environment_settings(cook.environment_settings)

    shutil.copytree(
        resource_data_path, resource.path(), dirs_exist_ok=True)

    resource.logged_commit()
    resource.calculate_size()


@common.setup_workflow
def test_workflow(sandbox_upload_mock, geobase_mock, environment_settings):
    cook = test_utils.GraphCook(environment_settings)

    ymapsdf.fill_graph(cook.input_builder(), ymapsdf.TEST_REGIONS)
    masstransit_static.fill_graph(cook.input_builder())
    geocoder_export.fill_graph(cook.input_builder(), ymapsdf.TEST_REGIONS)
    graph.fill_graph(cook.target_builder(), ymapsdf.TEST_REGIONS)

    ymapsdf.create_final_resources(
        cook, table_names=common.used_ymapsdf_tables(), data_dir=DATA_PATH)
    geocoder.prepare_data(cook, yt_server=YT_SERVER, data_dir=DATA_PATH)

    _prepare_masstransit_data(cook)

    test_utils.execute(cook)
