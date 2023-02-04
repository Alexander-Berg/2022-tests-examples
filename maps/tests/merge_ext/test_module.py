import os
import pytest
import yatest.common

from maps.garden.sdk import test_utils
from maps.garden.sdk.test_utils import data as data_utils
from maps.garden.sdk.test_utils import ymapsdf, ymapsdf_schema

from maps.garden.modules.ymapsdf.lib.merge_ext import common as merge_ext_common
from maps.garden.modules.ymapsdf.lib.merge_ext import graph as merge_ext_graph
from maps.garden.modules.ymapsdf.lib.merge_flats import merge_flats
from maps.garden.modules.ymapsdf.lib.merge_poi import graph as merge_poi
from maps.garden.modules.ymapsdf.lib.merge_stops import merge_stops
from maps.garden.modules.ymapsdf.lib.merge_yellow_zones import merge_yellow_zones


_YT_SERVER = "hahn"


def create_merge_phase_resources(cook, merge_phase, data_dir):
    schema_manager = ymapsdf_schema.YmapsdfSchemaManager()
    data_dir = os.path.join(data_dir, merge_phase.name)

    for table_name in merge_phase.output_tables:
        resource_name = merge_phase.resource_namer.resource_name(
            table_name,
            ymapsdf.TEST_REGION,
            ymapsdf.TEST_VENDOR)

        schema = schema_manager.yt_schema_for_sorted_table(table_name)
        filepath = os.path.join(data_dir, table_name + ".jsonl") if data_dir else None

        data_utils.create_yt_resource(cook, resource_name, ymapsdf.TEST_PROPERTIES, schema, filepath)


@pytest.mark.use_local_yt(_YT_SERVER)
def test_merge_success(environment_settings):
    cook = test_utils.GraphCook(environment_settings)

    merge_poi.fill_graph(cook.input_builder(), ymapsdf.TEST_REGIONS)
    merge_yellow_zones.fill_graph(cook.input_builder(), ymapsdf.TEST_REGIONS)
    merge_flats.fill_graph(cook.input_builder(), ymapsdf.TEST_REGIONS)
    merge_stops.fill_graph(cook.input_builder(), ymapsdf.TEST_REGIONS)

    merge_ext_graph.fill_graph(cook.target_builder(), ymapsdf.TEST_REGIONS)

    data_dir = yatest.common.test_source_path("data")

    for merge_phase in merge_ext_common.MERGE_PHASES:
        create_merge_phase_resources(cook, merge_phase, data_dir)

    ymapsdf.create_resources(
        cook,
        merge_ext_common.INPUT_STAGE,
        data_dir=os.path.join(data_dir, "input_ymapsdf"))

    test_utils.execute(cook)

    data_utils.validate_data(
        environment_settings,
        _YT_SERVER,
        merge_ext_graph.OUTPUT_STAGE,
        ymapsdf.TEST_PROPERTIES,
        os.path.join(data_dir, "output_ymapsdf"),
        merge_ext_graph.ALL_MERGED_TABLES
    )

    ymapsdf_schema.validate(
        environment_settings,
        _YT_SERVER,
        merge_ext_graph.OUTPUT_STAGE,
        ymapsdf.TEST_PROPERTIES,
        merge_ext_graph.ALL_MERGED_TABLES
    )


def test_consistency():
    assert len(merge_ext_common.MERGE_PHASES) <= merge_ext_common.MERGE_PHASES_COUNT
