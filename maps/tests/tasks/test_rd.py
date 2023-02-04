import pytest
from shapely import wkb, wkt

from maps.garden.sdk.test_utils.canonization import canonize_yt_tables
from maps.garden.libs.osm.osm_object.osm_object_id import OsmObjectId, OsmObjectType
from maps.garden.modules.ymapsdf_osm.lib.constants import TmpTable, YmapsdfTable
from maps.garden.modules.ymapsdf_osm.lib import rd
from maps.garden.modules.ymapsdf_osm.lib.osm_object_tags import OBJECT_INDEX, ObjectType

from .utils import get_task_executor


TEST_RD_EL_WITH_NAMES = [
    {
        "rd_el_id": OsmObjectId(type=OsmObjectType.WAY, id=1).to_ymapsdf_id(index=0),
        "shape": "LINESTRING (54.7813307 -127.1682148, 54.7807704 -127.1694562, 54.7801989 -127.1707225, 54.7796445 -127.1719509)",
        "name": "Main Street"
    },
    {
        "rd_el_id": OsmObjectId(type=OsmObjectType.WAY, id=2).to_ymapsdf_id(index=0),
        "shape": "LINESTRING (54.7818522 -127.1669505, 54.7818138 -127.1670970, 54.7813307 -127.1682148)",
        "name": "Main Street"
    },
    {
        "rd_el_id": OsmObjectId(type=OsmObjectType.WAY, id=5).to_ymapsdf_id(index=0),
        "shape": "LINESTRING (43.027376 47.282961, 43.025964 47.286008, 43.024372 47.289656)",
        "name": "Main Street"
    },
    {
        "rd_el_id": OsmObjectId(type=OsmObjectType.WAY, id=4).to_ymapsdf_id(index=0),
        "shape": "LINESTRING (43.020545 47.298331, 43.019619 47.300440)",
        "name": "Main Street"
    },
    {
        "rd_el_id": OsmObjectId(type=OsmObjectType.WAY, id=3).to_ymapsdf_id(index=0),
        "shape": "LINESTRING (54.7819078 -127.1667020, 54.7818522 -127.1669505)",
        "name": "Main Street"
    },
]


def test_get_different_roads():
    for row in TEST_RD_EL_WITH_NAMES:
        geom = wkt.loads(row["shape"])
        row["shape"] = wkb.dumps(geom, hex=True)
    rd_id_to_rd_els = rd._merge_roads_by_rd_el_id(TEST_RD_EL_WITH_NAMES)
    different_roads = rd._get_different_roads(rd_id_to_rd_els)

    roads = [road for road in different_roads]
    # expect roads [1, 2, 3] and [4, 5]
    assert len(roads) == 2

    rd_ids = [element.rd_id for element in roads[0]]
    rd_id = min(rd_ids)
    assert rd_id == OsmObjectId(OsmObjectType.WAY, 1).to_ymapsdf_id(OBJECT_INDEX[ObjectType.RD])

    rd_ids = [element.rd_id for element in roads[1]]
    rd_id = min(rd_ids)
    assert rd_id == OsmObjectId(OsmObjectType.WAY, 4).to_ymapsdf_id(OBJECT_INDEX[ObjectType.RD])


@pytest.fixture
def task_executor(environment_settings):
    return get_task_executor(environment_settings, source_folder="test_rd")


@pytest.mark.use_local_yt("hahn")
def test_rd_tmp(task_executor):
    input_table_name = TmpTable.RD_EL_WITH_NAMES
    input_resources = {
        input_table_name: task_executor.create_custom_input_yt_table_resource(input_table_name),
    }

    output_resources = {
        TmpTable.RD_WITHOUT_ISOCODES: task_executor.create_yt_table_resource(TmpTable.RD_WITHOUT_ISOCODES),
        YmapsdfTable.RD_CENTER: task_executor.create_yt_table_resource(YmapsdfTable.RD_CENTER),
        YmapsdfTable.RD_GEOM: task_executor.create_yt_table_resource(YmapsdfTable.RD_GEOM),
        YmapsdfTable.RD_RD_EL: task_executor.create_yt_table_resource(YmapsdfTable.RD_RD_EL),
        TmpTable.RD_NM_GEOM: task_executor.create_yt_table_resource(TmpTable.RD_NM_GEOM),
        TmpTable.RD_NODE: task_executor.create_yt_table_resource(TmpTable.RD_NODE),
        TmpTable.RD_TAGS: task_executor.create_yt_table_resource(TmpTable.RD_TAGS),
    }

    task_executor.execute_task(
        task=rd.MakeRdTables(),
        input_resources=input_resources,
        output_resources=output_resources,
    )

    return canonize_yt_tables(
        yt_table_resources=output_resources
    )
