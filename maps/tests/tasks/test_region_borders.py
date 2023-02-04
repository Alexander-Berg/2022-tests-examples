import pytest
import json

from maps.garden.sdk.test_utils.canonization import canonize_str
from maps.garden.sdk.test_utils.geometry import convert_wkb_to_wkt
from maps.garden.modules.osm_borders_src.defs import REGIONS_GEOM_SCHEMA, REGIONS_GEOM_TABLE
from maps.garden.modules.ymapsdf_osm.defs import YT_CLUSTER
from maps.garden.modules.ymapsdf_osm.lib.constants import YmapsdfTable
from maps.garden.modules.ymapsdf_osm.lib import region_borders
from .utils import get_task_executor


@pytest.fixture
def task_executor(environment_settings):
    return get_task_executor(environment_settings, source_folder="test_region_borders")


@pytest.mark.use_local_yt(YT_CLUSTER)
def test_region_borders_task(task_executor):
    regions_geom_resource = task_executor.create_custom_input_yt_table_resource(
        table_name=REGIONS_GEOM_TABLE,
        schema=REGIONS_GEOM_SCHEMA,
    )

    input_resources = {
        REGIONS_GEOM_TABLE: regions_geom_resource,
    }

    output_resources = {
        YmapsdfTable.META: task_executor.create_yt_table_resource(YmapsdfTable.META),
        YmapsdfTable.META_PARAM: task_executor.create_yt_table_resource(YmapsdfTable.META_PARAM),
    }

    task_executor.execute_task(
        task=region_borders.RegionBorders(),
        input_resources=input_resources,
        output_resources=output_resources,
    )

    return [
        canonize_str(json.dumps(convert_wkb_to_wkt(list(resource.read_table())), indent=4), name)
        for name, resource in output_resources.items()
    ]
