import os
import logging
import pytest
import typing as tp
import json
from unittest import mock

from yatest.common import test_source_path

from maps.libs.ymapsdf.py import ymapsdf

from maps.garden.sdk import test_utils
from maps.garden.sdk.test_utils import ymapsdf_schema
from maps.garden.sdk.test_utils.canonization import canonize_expected_yt_tables
from maps.garden.sdk.test_utils.data import create_yt_resource
from maps.garden.sdk.extensions import get_full_resource_name

from maps.garden.modules.altay.lib import altay
from maps.garden.modules.altay import defs as altay_defs
from maps.garden.modules.osm_borders_src import defs as osm_borders_src_defs
from maps.garden.modules.osm_src import defs as osm_src_defs
from maps.garden.modules.osm_to_yt import defs as osm_to_yt_defs
from maps.garden.modules.osm_to_yt.lib import graph as osm_to_yt_graph
from maps.garden.modules.osm_coastlines_src import defs as osm_coastlines_src_defs
from maps.garden.modules.osm_coastlines_src.lib import graph as osm_coastlines_src_graph
from maps.garden.modules.ymapsdf_osm.defs import YMAPSDF_OSM
from maps.garden.modules.ymapsdf_osm.lib import ymapsdf_osm, dict_tables

from .utils import create_countries_coverage_resource, TEST_REGION, INPUT_OSM_RESOURCES_PROPERTIES, YT_CLUSTER

INPUT_ALTAY_RESOURCES_PROPERTIES = {
    "shipping_date": "20211013"
}

INPUT_COASTLINES_RESOURCES_PROPERTIES = {
    "shipping_date": "20211014"
}

logger = logging.getLogger("ymapsdf_osm.test")


def _load_schema(table_name: str) -> list[dict[str, tp.Any]]:
    schema_path = test_source_path(f"schemas/{table_name}.json")
    with open(schema_path) as f:
        return json.load(f)


def _with_region_vendor(resource_name):
    return get_full_resource_name(resource_name, TEST_REGION, osm_src_defs.VENDOR)


@pytest.mark.use_local_yt_yql
@mock.patch("maps.garden.modules.osm_src.defs.REGIONS", [TEST_REGION])
def test_ymapsdf_osm(mocker, environment_settings, yt_client):
    mocker.patch("maps.garden.modules.ymapsdf_osm.lib.utils.YT_CLUSTER", "plato")
    mocker.patch("maps.garden.modules.ymapsdf_osm.lib.validation.ad.ValidateCountriesCompleteness.__call__")
    mocker.patch("maps.garden.modules.ymapsdf_osm.lib.validation.locality.fill_graph")
    mocker.patch("maps.garden.libs.ymapsdf.lib.collect_statistics.collect_statistics.UpdateAllStatisticsTable._append_to_all_statistics_table")

    cook = test_utils.GraphCook(environment_settings)

    osm_to_yt_graph.fill_graph(cook.input_builder())
    altay.fill_graph(cook.input_builder())
    osm_coastlines_src_graph.fill_graph(cook.input_builder())
    ymapsdf_osm.fill_graph(cook.target_builder())

    logger.info("Create resource for countries_coverage")
    create_countries_coverage_resource(
        cook=cook,
        yt_client=yt_client,
        environment_settings=environment_settings,
    )

    for table_name in osm_to_yt_defs.OUTPUT_TABLES:
        logger.info(f"Create resource for {table_name=}")
        create_yt_resource(
            cook=cook,
            resource_name=_with_region_vendor(osm_to_yt_defs.OSM_TO_YT.resource_name(table_name)),
            properties=INPUT_OSM_RESOURCES_PROPERTIES,
            schema=_load_schema(table_name),
            filepath=os.path.join(test_source_path("data"), table_name + ".jsonl")
        )

    logger.info(f"Create resource for table_name={osm_borders_src_defs.COUNTRIES_GEOM_TABLE}")
    create_yt_resource(
        cook=cook,
        resource_name=_with_region_vendor(osm_to_yt_defs.OSM_TO_YT.resource_name(osm_borders_src_defs.COUNTRIES_GEOM_TABLE)),
        properties=INPUT_OSM_RESOURCES_PROPERTIES,
        schema=osm_borders_src_defs.COUNTRIES_GEOM_SCHEMA,
        filepath=os.path.join(test_source_path("data"), osm_borders_src_defs.COUNTRIES_GEOM_TABLE + ".jsonl")
    )

    create_yt_resource(
        cook=cook,
        resource_name=osm_coastlines_src_defs.OSM_COASTLINES_SRC.resource_name(
            osm_coastlines_src_defs.OSM_COASTLINES_TABLE_NAME
        ),
        properties=INPUT_COASTLINES_RESOURCES_PROPERTIES,
        schema=osm_coastlines_src_defs.OSM_COASTLINES_TABLE_SCHEMA,
        filepath=os.path.join(
            test_source_path("data"),
            osm_coastlines_src_defs.OSM_COASTLINES_TABLE_NAME + ".jsonl"
        )
    )

    create_yt_resource(
        cook=cook,
        resource_name=altay_defs.RUBRICS_FOR_OSM,
        properties=INPUT_ALTAY_RESOURCES_PROPERTIES,
        schema=altay_defs.RUBRICS_FOR_OSM_SCHEMA,
        filepath=os.path.join(test_source_path("data"), altay_defs.RUBRICS_FOR_OSM + ".jsonl")
    )

    create_yt_resource(
        cook=cook,
        resource_name=_with_region_vendor(
            osm_to_yt_defs.OSM_TO_YT.resource_name(osm_borders_src_defs.REGIONS_GEOM_TABLE)
        ),
        properties=INPUT_OSM_RESOURCES_PROPERTIES,
        schema=osm_borders_src_defs.REGIONS_GEOM_SCHEMA,
        filepath=os.path.join(test_source_path("data"), f"{osm_borders_src_defs.REGIONS_GEOM_TABLE}.json")
    )

    out_resources = test_utils.execute(cook)

    ymapsdf_schema.validate(
        environment_settings,
        server=YT_CLUSTER,
        namer=YMAPSDF_OSM,
        properties=INPUT_OSM_RESOURCES_PROPERTIES,
        output_tables=ymapsdf.all_tables(),
    )

    return canonize_expected_yt_tables(
        all_out_resources=out_resources,
        expecting_tables_names=[
            name for name in ymapsdf.all_tables()
            if name not in dict_tables.OUTPUT_TABLES_NAMES  # Do not canonize static tables
        ],
        table_name_to_resource_name_converter=lambda name: _with_region_vendor(YMAPSDF_OSM.resource_name(name))
    )
