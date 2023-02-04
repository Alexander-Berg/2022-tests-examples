import os
import logging
import pytest

import yatest

from yt.wrapper import TablePath
from yt.wrapper.ypath import ypath_dirname

from maps.garden.sdk.core import DataValidationWarning, Version
from maps.garden.sdk.yt import YtTableResource
from maps.garden.sdk.yt import utils as yt_utils

from maps.garden.sdk import test_utils
from maps.garden.sdk.extensions import mutagen
from maps.garden.modules.ymapsdf import defs

from maps.garden.modules.yellow_zones_bundle.lib import graph as yellow_zones_bundle
from maps.garden.modules.ymapsdf.lib.merge_poi import (
    constants as merge_poi_constants,
    graph as merge_poi,
)
from maps.garden.modules.ymapsdf.lib.parent_finder import (
    parent_finder as parent_finder_yt
)
from maps.garden.modules.ymapsdf.lib.translation import (
    translate as translation
)
from maps.garden.modules.ymapsdf.lib.ymapsdf_tester import (
    constants,
    ft_geom_conflicts,
)


STAGE_RESOURCE_NAMER = defs.FINAL_STAGE
SCHEMA_MANAGER = test_utils.ymapsdf_schema.YmapsdfSchemaManager()

TEST_DATA_DIR = yatest.common.test_source_path("data_conflicts")

logger = logging.getLogger("garden.tasks.ymapsdf_tester")

YT_SERVER = "plato"


TEST_FT_POINT_TILES_SCHEMA = [
    {"name": "ft_id", "type_v3": "int64"},
    {"name": "shape", "type_v3": "string"},
    {"name": "tile_x", "type_v3": "int64"},
    {"name": "tile_y", "type_v3": "int64"},
    {"name": "ft_type_id", "type_v3": "int64"}
]

TEST_FT_POINT_TILES_PREPARED_SCHEMA = [
    {"name": "x", "type_v3": "int64", "sort_order": "ascending"},
    {"name": "y", "type_v3": "int64", "sort_order": "ascending"},
    {
        "name": "indoor_level_id",
        "type_v3": {"type_name": "optional", "item": "int64"},
        "sort_order": "ascending"
    },
    {"name": "need_no_conflicts_calc", "type_v3": "bool", "sort_order": "ascending"},
    {"name": "in_yellow_zone", "type_v3": "bool"},
    {"name": "is_geo_product", "type_v3": "bool"},
    {"name": "from_poi_export", "type_v3": "bool"},
    {"name": "is_recently_protected", "type_v3": "bool"},
    {"name": "ft_id", "type_v3": "int64"},
    {"name": "shape", "type_v3": "string"},
]

TEST_YELLOW_ZONES_SCHEMA = [
    {"name": "permalink", "type_v3": "int64"},
]

TEST_GEO_PRODUCT_FT_IDS = {6, 7, 8}
TEST_EXPORT_GEO_PRODUCT_FT_IDS = {7, 8}
TEST_RECENTLY_PROTECTED_FT_IDS = {6, 8}
TEST_INDOOR_POI_FT_IDS = {8}
TEST_YELLOW_ZONE_FT_IDS = {1}
TEST_DISP_CLASS_10_FT_IDS = {4}
YELLOW_ZONES_PERMALINKS_DATA = [{"permalink": 123}]


def generate_geo_product_conflicts_dataset():
    def produce_row(ft_id):
        row = {
            ft_geom_conflicts.conflicts_column_name(zoom): 1
            for zoom in ft_geom_conflicts.CONFLICT_ZOOMS
        }
        row["from_poi_export"] = False
        row["is_indoor_poi"] = ft_id in TEST_INDOOR_POI_FT_IDS
        row["is_recently_protected"] = ft_id in TEST_RECENTLY_PROTECTED_FT_IDS
        row["is_geo_product"] = True
        row["in_yellow_zone"] = ft_id in TEST_YELLOW_ZONE_FT_IDS
        row["ft_id"] = ft_id
        return row

    return [
        produce_row(ft_id)
        for ft_id in range(1, ft_geom_conflicts.GEO_PRODUCT_MIN_COUNT + 1)
    ]


def generate_resource(name, schema, environment_settings):
    resource = YtTableResource(
        name,
        path_template="/" + name,
        server=YT_SERVER)
    if schema:
        resource.set_schema(schema)
    resource.version = Version()
    resource.load_environment_settings(environment_settings)
    return resource


def generate_input_resource(name, schema, data, environment_settings):
    input_resource = generate_resource(name, schema, environment_settings)
    input_resource.write_table(data)
    input_resource.logged_commit()
    input_resource.calculate_size()
    return input_resource


def _prepare_yellow_zones(cook):
    YELLOW_ZONES_PATH = "//home/maps/poi/yellow_zones/export_data/20.12.12-0/permalinks"

    yellow_zones_resource = cook.create_input_resource("yellow_zones_permalinks")
    yellow_zones_resource.version = Version(properties={
        "release_name": "2020.12.12",
        "yt_path": YELLOW_ZONES_PATH
    })

    yt_client = yt_utils.get_yt_client(
        yt_utils.get_server_settings(
            yt_utils.get_yt_settings(cook.environment_settings),
            server=YT_SERVER))
    yt_client.create("map_node", ypath_dirname(YELLOW_ZONES_PATH), recursive=True)
    yt_client.write_table(
        TablePath(YELLOW_ZONES_PATH, attributes={"schema": TEST_YELLOW_ZONES_SCHEMA}),
        YELLOW_ZONES_PERMALINKS_DATA)


def read_jsonl_table(name, schema, environment_settings):
    table_resource_data = test_utils.geometry.convert_wkt_to_wkb_in_jsonl_file(
        os.path.join(TEST_DATA_DIR, name) + ".jsonl",
        schema=schema)

    return generate_input_resource(
        name,
        schema=schema,
        data=table_resource_data,
        environment_settings=environment_settings)


def generate_output_resource(name, environment_settings):
    # method exists for consistency
    # output table schema is determined by tasks
    return generate_resource(name, None, environment_settings)


@pytest.mark.use_local_yt_yql
def test_all_tasks(environment_settings):
    cook = test_utils.GraphCook(environment_settings)

    translation.fill_graph(
        cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)
    parent_finder_yt.fill_graph(
        cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)

    merge_poi.fill_graph(
        cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)

    yellow_zones_bundle.YT_SERVER = YT_SERVER
    yellow_zones_bundle.fill_graph(
        cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)

    ft_geom_conflicts.fill_graph(
        mutagen.create_region_vendor_mutagen(
            cook.target_builder(),
            test_utils.ymapsdf.TEST_REGION,
            test_utils.ymapsdf.TEST_VENDOR,
            external_resources=["yellow_zones_permalinks"]))

    test_utils.ymapsdf.create_final_resources(cook, ["ft", "ft_source"])

    test_utils.data.create_yt_resource(
        cook,
        STAGE_RESOURCE_NAMER.resource_name(
            "ft_point_tiles",
            test_utils.ymapsdf.TEST_REGION,
            test_utils.ymapsdf.TEST_VENDOR),
        test_utils.ymapsdf.TEST_PROPERTIES,
        TEST_FT_POINT_TILES_SCHEMA
    )

    test_utils.data.create_yt_resource(
        cook,
        constants.YT_MERGE_POI_STAGE.resource_name(
            "extra_poi_geoproduct_tmp",
            test_utils.ymapsdf.TEST_REGION,
            test_utils.ymapsdf.TEST_VENDOR),
        test_utils.ymapsdf.TEST_PROPERTIES,
        merge_poi_constants.EXTRA_POI_GEOPRODUCT_TMP_SCHEMA
    )

    _prepare_yellow_zones(cook)

    test_utils.execute(cook)


@pytest.mark.use_local_yt_yql
def test_prepare_ft_point_tiles(environment_settings):
    ft = read_jsonl_table(
        "ft",
        schema=SCHEMA_MANAGER.schema_for_table("ft").make_yt_schema(),
        environment_settings=environment_settings
    )

    ft_point_tiles = read_jsonl_table(
        "ft_point_tiles",
        schema=TEST_FT_POINT_TILES_SCHEMA,
        environment_settings=environment_settings)

    ft_source = read_jsonl_table(
        "ft_source",
        schema=SCHEMA_MANAGER.schema_for_table("ft_source").make_yt_schema(),
        environment_settings=environment_settings)

    geoproduct = read_jsonl_table(
        "extra_poi_geoproduct_tmp",
        schema=merge_poi_constants.EXTRA_POI_GEOPRODUCT_TMP_SCHEMA,
        environment_settings=environment_settings)

    ft_prepared_tiles = generate_output_resource(
        ft_geom_conflicts._YMAPSDF_FT_POINT_TILES_PREPARED,
        environment_settings)

    yellow_zones_permalinks = generate_input_resource(
        "yellow_zones_permalinks",
        TEST_YELLOW_ZONES_SCHEMA,
        YELLOW_ZONES_PERMALINKS_DATA,
        environment_settings)

    task = ft_geom_conflicts.PrepareFtPointTiles()
    task.load_environment_settings(environment_settings)

    task(
        # input resources
        extra_poi_geoproduct_tmp=geoproduct,
        ft=ft,
        ft_source=ft_source,
        ft_point_tiles=ft_point_tiles,
        yellow_zones_permalinks=yellow_zones_permalinks,
        # output resource
        ft_prepared_tiles=ft_prepared_tiles)
    ft_prepared_tiles.logged_commit()

    def cut_schema(column):
        cut_schema_column = {
            'type_v3': column['type_v3'],
            'name': column['name']
        }
        if column.get('sort_order'):
            cut_schema_column['sort_order'] = column['sort_order']
        return cut_schema_column

    ft_prepared_tiles_schema = sorted(
        map(
            # takes from schema type_v3 and name only
            cut_schema,
            ft_prepared_tiles.schema()),
        # sorts it by name
        key=lambda column: column['name'])

    ft_prepared_tiles_schema_expected = sorted(
        TEST_FT_POINT_TILES_PREPARED_SCHEMA,
        key=lambda column: column['name'])

    assert ft_prepared_tiles_schema == ft_prepared_tiles_schema_expected

    for row in ft_prepared_tiles.read_table():
        assert row["need_no_conflicts_calc"] != (row["ft_id"] in TEST_GEO_PRODUCT_FT_IDS or row["ft_id"] in TEST_YELLOW_ZONE_FT_IDS)
        assert row["is_geo_product"] == (row["ft_id"] in TEST_GEO_PRODUCT_FT_IDS)
        assert row["in_yellow_zone"] == (row["ft_id"] in TEST_YELLOW_ZONE_FT_IDS)
        assert row["from_poi_export"] == (row["ft_id"] in TEST_EXPORT_GEO_PRODUCT_FT_IDS)
        assert row["is_recently_protected"] == (row["ft_id"] in TEST_RECENTLY_PROTECTED_FT_IDS)
        assert row["ft_id"] not in TEST_DISP_CLASS_10_FT_IDS


@pytest.mark.use_local_yt_yql
def test_conflicts_calculator(environment_settings):
    def conflicts_assert(ft_id, result, expectation):
        assert result == expectation, (
            f'Unexpected value, for ft_id = {ft_id} '
            f'got {result} but expected {expectation}'
        )

    ft_point_tiles_data = test_utils.geometry.convert_wkt_to_wkb_in_jsonl_file(
        os.path.join(TEST_DATA_DIR, "ft_point_tiles") + ".jsonl",
        schema=TEST_FT_POINT_TILES_SCHEMA)

    ft_point_tiles_prepared_data = []
    for row in ft_point_tiles_data:
        ft_point_tiles_prepared_data.append({
            "x": row["tile_x"], "y": row["tile_y"],
            "need_no_conflicts_calc": row["ft_id"] not in TEST_GEO_PRODUCT_FT_IDS,
            "in_yellow_zone": row["ft_id"] in TEST_YELLOW_ZONE_FT_IDS,
            "is_geo_product": row["ft_id"] in TEST_GEO_PRODUCT_FT_IDS,
            "from_poi_export": row["ft_id"] in TEST_EXPORT_GEO_PRODUCT_FT_IDS,
            "indoor_level_id": 0 if row["ft_id"] in TEST_INDOOR_POI_FT_IDS else None,
            "is_recently_protected": row["ft_id"] in TEST_RECENTLY_PROTECTED_FT_IDS,
            "ft_id": row["ft_id"],
            "shape": row['shape']
        })

    ft_point_tiles = generate_input_resource(
        ft_geom_conflicts._YMAPSDF_FT_POINT_TILES_PREPARED,
        schema=TEST_FT_POINT_TILES_PREPARED_SCHEMA,
        data=ft_point_tiles_prepared_data,
        environment_settings=environment_settings)

    geo_product_confclits_ft_ids_tmp = generate_output_resource(
        ft_geom_conflicts._YMAPSDF_CONFLICTS_FT_IDS_TMP,
        environment_settings)

    task = ft_geom_conflicts.CalcConflictedPoi()
    task.load_environment_settings(environment_settings)

    task(
        output_resource=geo_product_confclits_ft_ids_tmp,
        input_resource=ft_point_tiles)
    geo_product_confclits_ft_ids_tmp.logged_commit()

    conflicts = {}
    for row in geo_product_confclits_ft_ids_tmp.read_table():
        assert row["ft_id"] in TEST_GEO_PRODUCT_FT_IDS, (
            f'Encountered non-geoproduct: {row["ft_id"]} '
            f'while geoproduct ft_ids is {TEST_GEO_PRODUCT_FT_IDS}'
        )
        assert row[ft_geom_conflicts.COUNT_COLUMN] == 1, (
            f"Encountered value != 1 in count column: {row}"
        )

        indicators = {
            "from_poi_export": TEST_EXPORT_GEO_PRODUCT_FT_IDS,
            "is_indoor_poi": TEST_INDOOR_POI_FT_IDS,
            "is_recently_protected": TEST_RECENTLY_PROTECTED_FT_IDS,
            "is_geo_product": TEST_GEO_PRODUCT_FT_IDS,
        }
        for field, ft_ids in indicators.items():
            assert row[field] == (row["ft_id"] in ft_ids), (
                f'Encountered invalid value for ft_id {row["ft_id"]}, '
                f'{field} is {row[field]} but should be '
                f'{row["ft_id"] in ft_ids}'
            )

        conflicts[row["ft_id"]] = max(
            filter(lambda x: x[0] != "ft_id", row.items()),
            key=lambda x: (x[1], x[0]))[0]

    assert set(conflicts.keys()) == TEST_GEO_PRODUCT_FT_IDS, (
        f"Output table should contain all geoproduct ft_ids: "
        f"{conflicts}"
    )
    conflicts_assert(
        6, conflicts[6], 'is_recently_protected')
    conflicts_assert(
        7, conflicts[7], ft_geom_conflicts.conflicts_column_name(21))


@pytest.mark.use_local_yt_yql
def test_conflicts_fraction_check_triggers(environment_settings):
    conflicting_ft_ids_tmp = generate_input_resource(
        name=ft_geom_conflicts._YMAPSDF_CONFLICTS_FT_IDS_TMP,
        schema=ft_geom_conflicts.CONFLICTS_SCHEMA,
        data=generate_geo_product_conflicts_dataset(),
        environment_settings=environment_settings)

    geo_product_confclicts_ft_ids = generate_output_resource(
        ft_geom_conflicts._YMAPSDF_CONFLICTS_FT_IDS,
        environment_settings)
    conflicts_counts = generate_output_resource(
        ft_geom_conflicts.YMAPSDF_CONFLICTS_COUNT,
        environment_settings)

    task = ft_geom_conflicts.CheckConflictsLevel()
    task.load_environment_settings(environment_settings)

    with pytest.raises(DataValidationWarning):
        task(
            conflicting_ft_ids_tmp=conflicting_ft_ids_tmp,
            conflicting_ft_ids=geo_product_confclicts_ft_ids,
            conflicts_counts=conflicts_counts)
