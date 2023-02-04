from yatest.common import test_source_path
from maps.garden.sdk.core import Version
from maps.garden.sdk.test_utils import task_tester
from maps.garden.modules.osm_borders_src.defs import COUNTRIES_GEOM_SCHEMA, COUNTRIES_COVERAGE_FILE
from maps.garden.modules.osm_borders_src.lib.countries_coverage import CreateCountriesCoverageFile
from maps.garden.modules.osm_src import defs as osm_src_defs
from maps.garden.modules.osm_to_yt import defs as osm_to_yt_defs


YT_CLUSTER = "plato"
TEST_REGION = "cis1"

INPUT_OSM_RESOURCES_PROPERTIES = {
    "shipping_date": "20211014",
    "region": TEST_REGION,
    "vendor": osm_src_defs.VENDOR,
}


def get_countries_coverage(environment_settings: dict) -> bytearray:
    task_executor = task_tester.TestTaskExecutor(
        environment_settings,
        properties=INPUT_OSM_RESOURCES_PROPERTIES,
        test_data_path=task_tester.TestDataPath(
            schemas_path=None,
            input_path=test_source_path("data"),
            output_path="",
        ),
    )
    output_resource = task_executor.create_coverage_resource(
        "countries_coverage",
        "countries.mms.1",
    )
    input_resource = task_executor.create_custom_input_yt_table_resource("countries_geom", schema=COUNTRIES_GEOM_SCHEMA)
    task_executor.execute_task(
        task=CreateCountriesCoverageFile(),
        input_resources={"countries_geom": input_resource},
        output_resources={"coverage_file": output_resource},
    )
    return output_resource.read_file().read()


def create_countries_coverage_resource(cook, yt_client, environment_settings: dict):
    resource_name = osm_to_yt_defs.OSM_TO_YT.resource_name(
        COUNTRIES_COVERAGE_FILE, region=TEST_REGION, vendor=osm_src_defs.VENDOR
    )
    resource = cook.create_input_resource(resource_name)
    resource.version = Version(properties=INPUT_OSM_RESOURCES_PROPERTIES)
    resource.server = YT_CLUSTER
    resource.load_environment_settings(cook.environment_settings)
    yt_client.create("file", path=resource.path, recursive=True, ignore_existing=True)
    yt_client.write_file(resource.path, get_countries_coverage(environment_settings))
