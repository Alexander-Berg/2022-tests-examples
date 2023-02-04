import copy
import typing as tp

from yatest.common import test_source_path
from maps.garden.sdk.yt import YtFileResource
from maps.garden.sdk.test_utils import task_tester
from maps.garden.modules.osm_borders_src.defs import COUNTRIES_GEOM_SCHEMA
from maps.garden.modules.osm_borders_src.lib.countries_coverage import CreateCountriesCoverageFile
from maps.garden.modules.ymapsdf_osm.lib.build_coverage import BuildFullCoverage
from maps.garden.modules.ymapsdf_osm.lib.constants import TmpTable, TmpFile


TEST_PROPERTIES = {
    "shipping_date": "20211014",
    "region": "cis1",
    "vendor": "osm",
}
TEST_PROPERTIES["old_shipping_date"] = str(int(TEST_PROPERTIES["shipping_date"]) - 1)


def get_full_ad_coverage(task_executor: task_tester.TestTaskExecutor) -> YtFileResource:
    output_resource = task_executor.create_coverage_resource(
        TmpFile.FULL_AD_COVERAGE,
        TmpFile.FULL_AD_COVERAGE_FILENAME,
    )
    input_resource = task_executor.create_custom_input_yt_table_resource(TmpTable.AD_GEOM_FOR_COVERAGE)
    task_executor.execute_task(
        task=BuildFullCoverage(),
        input_resources={"geom_for_coverage": input_resource},
        output_resources={"full_ad_coverage": output_resource},
    )
    return output_resource


def get_task_executor(
    environment_settings: dict[str, tp.Any],
    source_folder: str,
    schema_folder: tp.Optional[str] = "schemas",
    test_properties: tp.Optional[dict] = None,
) -> task_tester.TestTaskExecutor:
    return task_tester.TestTaskExecutor(
        environment_settings,
        properties=test_properties if test_properties else TEST_PROPERTIES,
        test_data_path=task_tester.TestDataPath(
            schemas_path=test_source_path(schema_folder) if schema_folder else None,
            input_path=test_source_path(f"data/{source_folder}"),
            output_path="",
        ),
    )


def get_countries_coverage(task_executor: task_tester.TestTaskExecutor) -> YtFileResource:
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
    return output_resource


def make_sorted(schema: dict, ascending: bool = True) -> dict:
    schema_copy = copy.deepcopy(schema)
    schema_copy[0]["sort_order"] = "ascending" if ascending else "descending"
    return schema_copy
