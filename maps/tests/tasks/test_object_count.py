import pytest
from yt.wrapper import ypath_join
from yt.wrapper.schema import TableSchema
import typing as tp

from yatest.common import test_source_path
from maps.garden.sdk.extensions import mutagen
from maps.garden.sdk import test_utils
from maps.garden.sdk.test_utils.canonization import canonize_str
from maps.garden.sdk.yt import utils as yt_utils

from maps.garden.libs.ymapsdf.lib.collect_statistics.abandoned_objects_statistics import StatisticsRow

from maps.garden.modules.osm_src import defs as osm_src
from maps.garden.modules.ymapsdf_osm.lib.constants import YmapsdfTable
from maps.garden.modules.ymapsdf_osm.lib.utils import YmapsdfOsmGraphBuild
from maps.garden.modules.ymapsdf_osm.lib import collect_statistics
from maps.garden.modules.ymapsdf_osm.defs import YMAPSDF_OSM
from maps.garden.modules.ymapsdf_osm.lib.validation import object_count
from .utils import TEST_PROPERTIES


SOURCE_FOLDER = "test_object_count"


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

    test_yt_path = _get_resource_path(garden_prefix, TEST_PROPERTIES["shipping_date"])
    yt_client.remove(test_yt_path, recursive=True, force=True)

    cook = test_utils.GraphCook(environment_settings)

    input_builder = YmapsdfOsmGraphBuild(mutagen.create_region_vendor_mutagen(
        cook.input_builder(),
        TEST_PROPERTIES["region"],
        osm_src.VENDOR
    ))

    collect_statistics.fill_graph(input_builder)

    regional_builder = mutagen.create_region_vendor_mutagen(
        cook.target_builder(),
        TEST_PROPERTIES["region"],
        osm_src.VENDOR
    )

    graph_builder = YmapsdfOsmGraphBuild(regional_builder)
    object_count.fill_graph(graph_builder)

    test_utils.data.create_yt_resource(
        cook,
        YMAPSDF_OSM.resource_name(YmapsdfTable.STATISTICS, TEST_PROPERTIES["region"], osm_src.VENDOR),
        properties=TEST_PROPERTIES,
        schema=list(TableSchema.from_row_type(StatisticsRow).to_yson_type()),
        filepath=_get_source_path("statistics.jsonl", "old_stat"),
    )
    return cook


@pytest.mark.use_local_yt_yql
def test_validate_statistic_task(graph_cook, yt_client, garden_prefix):
    yt_path_for_old_data = _get_resource_path(garden_prefix, TEST_PROPERTIES["old_shipping_date"], "statistics")

    test_utils.data.create_table(
        yt_client,
        yt_path_for_old_data,
        list(TableSchema.from_row_type(StatisticsRow).to_yson_type()),
        _get_source_path("statistics.jsonl", "new_stat"),
    )

    with pytest.raises(test_utils.internal.task_handler.TaskError) as exc_info:
        test_utils.execute(graph_cook)

    return canonize_str(
        data=str(exc_info.value.original_exception),
        file_name="data"
    )
