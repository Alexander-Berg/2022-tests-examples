import pytest
from yt.wrapper import ypath_join
import typing as tp

from yatest.common import test_source_path
from maps.garden.sdk.extensions import mutagen
from maps.garden.sdk import test_utils
from maps.garden.sdk.test_utils.canonization import canonize_str
from maps.garden.sdk.yt import utils as yt_utils
from maps.garden.modules.osm_src import defs as osm_src
from maps.garden.modules.ymapsdf_osm.lib.validation import ad
from maps.garden.modules.ymapsdf_osm.lib.build_coverage import add_find_parent_for_ad_task
from maps.garden.modules.ymapsdf_osm.lib.constants import YmapsdfTable
from maps.garden.modules.ymapsdf_osm.lib.utils import YmapsdfOsmGraphBuild
from maps.garden.modules.ymapsdf_osm.lib import build_ad
from maps.garden.modules.ymapsdf_osm.lib import names
from maps.garden.modules.ymapsdf_osm.defs import YMAPSDF_OSM
from .utils import TEST_PROPERTIES


SOURCE_FOLDER = "test_ad"


def _get_resource_path(garden_prefix, shipping_date, table_name: tp.Optional[str] = None):
    output_resource_path = YMAPSDF_OSM.yt_path_prefix_template.format(
        shipping_date=shipping_date,
        region=TEST_PROPERTIES["region"]
    )
    if table_name:
        output_resource_path += table_name
    return ypath_join(garden_prefix, output_resource_path)


def _get_source_path(file_name, test_name='base'):
    return test_source_path(f"data/{SOURCE_FOLDER}/{test_name}/{file_name}")


@pytest.fixture
def garden_prefix(environment_settings):
    yt_settings = yt_utils.get_server_settings(yt_utils.get_yt_settings(environment_settings), server="plato")
    return yt_utils.get_garden_prefix(yt_settings)


@pytest.fixture
def graph_cook(mocker, environment_settings, yt_client, garden_prefix):
    mocker.patch("maps.garden.modules.ymapsdf_osm.lib.utils.YT_CLUSTER", "plato")
    mocker.patch("maps.garden.modules.ymapsdf_osm.lib.validation.ad.ValidateCountriesCompleteness.__call__")

    test_yt_path = _get_resource_path(garden_prefix, TEST_PROPERTIES["shipping_date"])
    yt_client.remove(test_yt_path, recursive=True, force=True)

    cook = test_utils.GraphCook(environment_settings)

    input_builder = YmapsdfOsmGraphBuild(mutagen.create_region_vendor_mutagen(
        cook.input_builder(),
        TEST_PROPERTIES["region"],
        osm_src.VENDOR
    ))

    add_find_parent_for_ad_task(input_builder)
    build_ad.fill_graph(input_builder)
    names.fill_graph(input_builder)

    regional_builder = mutagen.create_region_vendor_mutagen(
        cook.target_builder(),
        TEST_PROPERTIES["region"],
        osm_src.VENDOR
    )

    graph_builder = YmapsdfOsmGraphBuild(regional_builder)
    ad.fill_graph(graph_builder)

    schema_manager = test_utils.ymapsdf_schema.YmapsdfSchemaManager()

    test_utils.data.create_yt_resource(
        cook,
        YMAPSDF_OSM.resource_name(YmapsdfTable.AD, TEST_PROPERTIES["region"], osm_src.VENDOR),
        properties=TEST_PROPERTIES,
        schema=schema_manager.yt_schema_for_sorted_table("ad"),
        filepath=_get_source_path("ad.jsonl"),
    )

    test_utils.data.create_yt_resource(
        cook,
        YMAPSDF_OSM.resource_name(YmapsdfTable.AD_GEOM, TEST_PROPERTIES["region"], osm_src.VENDOR),
        properties=TEST_PROPERTIES,
        schema=schema_manager.yt_schema_for_sorted_table("ad_geom"),
        filepath=_get_source_path("ad_geom.jsonl"),
    )
    return cook


@pytest.mark.use_local_yt_yql
def test_delete_in_compute_ad_diff_task(graph_cook, yt_client, garden_prefix):
    yt_path_for_old_data = _get_resource_path(garden_prefix, TEST_PROPERTIES["old_shipping_date"], "ad")
    schema_manager = test_utils.ymapsdf_schema.YmapsdfSchemaManager()

    test_utils.data.create_table(
        yt_client,
        yt_path_for_old_data,
        schema_manager.yt_schema_for_sorted_table("ad"),
        _get_source_path("ad.jsonl", "test_delete"),
    )

    with pytest.raises(test_utils.internal.task_handler.TaskError) as exc_info:
        test_utils.execute(graph_cook)

    return canonize_str(
        data=str(exc_info.value.original_exception),
        file_name="data"
    )


@pytest.mark.use_local_yt_yql
def test_add_in_compute_ad_diff_task(graph_cook, yt_client, garden_prefix):
    yt_path_for_old_data = _get_resource_path(garden_prefix, TEST_PROPERTIES["old_shipping_date"], "ad")
    schema_manager = test_utils.ymapsdf_schema.YmapsdfSchemaManager()

    test_utils.data.create_table(
        yt_client,
        yt_path_for_old_data,
        schema_manager.yt_schema_for_sorted_table("ad"),
        _get_source_path("ad.jsonl", "test_add"),
    )

    with pytest.raises(test_utils.internal.task_handler.TaskError) as exc_info:
        test_utils.execute(graph_cook)

    return canonize_str(
        data=str(exc_info.value.original_exception),
        file_name="data"
    )


@pytest.mark.use_local_yt_yql
def test_move_in_compute_ad_diff_task(graph_cook, yt_client, garden_prefix):
    yt_path_for_old_data = _get_resource_path(garden_prefix, TEST_PROPERTIES["old_shipping_date"], "ad")
    schema_manager = test_utils.ymapsdf_schema.YmapsdfSchemaManager()

    test_utils.data.create_table(
        yt_client,
        yt_path_for_old_data,
        schema_manager.yt_schema_for_sorted_table("ad"),
        _get_source_path("ad.jsonl", "test_move"),
    )

    with pytest.raises(test_utils.internal.task_handler.TaskError) as exc_info:
        test_utils.execute(graph_cook)

    return canonize_str(
        data=str(exc_info.value.original_exception),
        file_name="data"
    )
