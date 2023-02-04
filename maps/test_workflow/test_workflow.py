import yatest.common

from maps.garden.sdk import test_utils
from maps.garden.sdk.test_utils import ymapsdf
from maps.garden.modules.geocoder_export.lib import geocoder_export
from maps.garden.modules.renderer_denormalization.tests.lib import geocoder, common
from maps.garden.modules.renderer_denormalization_osm.lib import graph
from maps.garden.modules.ymapsdf_osm.defs import YMAPSDF_OSM


DATA_PATH = yatest.common.source_path("maps/renderer/denormalization/test_data")

# Hardcoded with use_local_yt_yql
YT_SERVER = "plato"


@common.setup_workflow
def test_workflow(sandbox_upload_mock, geobase_mock, environment_settings):
    cook = test_utils.GraphCook(environment_settings)

    ymapsdf.fill_graph(cook.input_builder(), ymapsdf.TEST_REGIONS, namer=YMAPSDF_OSM)
    geocoder_export.fill_graph(cook.input_builder(), ymapsdf.TEST_REGIONS)
    graph.fill_graph(cook.target_builder(), ymapsdf.TEST_REGIONS)

    ymapsdf.create_resources(
        cook,
        stage=YMAPSDF_OSM,
        table_names=common.used_ymapsdf_tables(),
        data_dir=DATA_PATH,
    )
    geocoder.prepare_data(cook, yt_server=YT_SERVER, data_dir=DATA_PATH)

    test_utils.execute(cook)
