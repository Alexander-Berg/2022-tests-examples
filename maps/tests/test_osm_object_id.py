import pytest

from maps.garden.sdk.core import GardenError

from maps.garden.libs.osm.osm_object.osm_object_id import OsmObjectId, OsmObjectType


OFFSET_STEP = 5_000_000_000_000
RELATION_INDEX_STEP = 100_000_000
WAY_INDEX_STEP = 4_000_000_000
NODE_INDEX_STEP = 50_000_000_000
COASTLINE_INDEX_STEP = 100_000
RELATION_ID_FACTOR = 1
WAY_ID_FACTOR = 2
NODE_ID_FACTOR = 3
COASTLINE_ID_FACTOR = 4
RELATION_ID_OFFSET = RELATION_ID_FACTOR * OFFSET_STEP
WAY_ID_OFFSET = WAY_ID_FACTOR * OFFSET_STEP
NODE_ID_OFFSET = NODE_ID_FACTOR * OFFSET_STEP
COASTLINE_ID_OFFSET = COASTLINE_ID_FACTOR * OFFSET_STEP
MAX_OSM_ID = {
    OsmObjectType.NODE: NODE_INDEX_STEP - 1,
    OsmObjectType.WAY: WAY_INDEX_STEP - 1,
    OsmObjectType.RELATION: RELATION_INDEX_STEP - 1,
    OsmObjectType.COASTLINE: COASTLINE_INDEX_STEP - 1,
}
MAX_INDEX = {
    OsmObjectType.NODE: int(OFFSET_STEP / NODE_INDEX_STEP) - 1,
    OsmObjectType.WAY: int(OFFSET_STEP / WAY_INDEX_STEP) - 1,
    OsmObjectType.RELATION: int(OFFSET_STEP / RELATION_INDEX_STEP) - 1,
    OsmObjectType.COASTLINE: int(OFFSET_STEP / COASTLINE_INDEX_STEP) - 1,
}


@pytest.mark.parametrize(
    ("osm_id", "osm_type"),
    [
        (1, "NODE"),
        (2, "WAY"),
        (3, "RELATION"),
        (4, OsmObjectType.COASTLINE),
        (0, "node"),
        (123, "node"),
        (MAX_OSM_ID["node"], "node"),
        (0, "way"),
        (123, "way"),
        (MAX_OSM_ID["way"], "way"),
        (0, "relation"),
        (123, "relation"),
        (MAX_OSM_ID["relation"], "relation"),
        (0, OsmObjectType.COASTLINE),
        (123, OsmObjectType.COASTLINE),
        (MAX_OSM_ID[OsmObjectType.COASTLINE], OsmObjectType.COASTLINE),
    ],
)
def test_create_osm_object_id(osm_id, osm_type):
    id = OsmObjectId(osm_type, osm_id)
    assert id.id == osm_id
    assert id.type == osm_type.lower()
    link = id.make_link()
    assert link.startswith("https://www.openstreetmap.org/")
    assert str(osm_id) in link
    assert osm_type.lower() in link


@pytest.mark.parametrize(
    ("osm_id", "osm_type", "index", "expected_ymapsdf_id"),
    [
        # without index
        (0, "node", 0, NODE_ID_OFFSET),
        (31, "node", 0, NODE_ID_OFFSET + 31),
        (MAX_OSM_ID["node"], "node", 0, NODE_ID_OFFSET + MAX_OSM_ID["node"]),
        (0, "way", 0, WAY_ID_OFFSET),
        (31, "way", 0, WAY_ID_OFFSET + 31),
        (MAX_OSM_ID["way"], "way", 0, WAY_ID_OFFSET + MAX_OSM_ID["way"]),
        (0, "relation", 0, RELATION_ID_OFFSET),
        (31, "relation", 0, RELATION_ID_OFFSET + 31),
        (MAX_OSM_ID["relation"], "relation", 0, RELATION_ID_OFFSET + MAX_OSM_ID["relation"]),
        (0, "coastline", 0, COASTLINE_ID_OFFSET),
        (31, "coastline", 0, COASTLINE_ID_OFFSET + 31),
        (MAX_OSM_ID["coastline"], "coastline", 0, COASTLINE_ID_OFFSET + MAX_OSM_ID["coastline"]),
        # node with index
        (1, "node", 1, NODE_ID_OFFSET + NODE_INDEX_STEP + 1),
        (13, "node", 3, NODE_ID_OFFSET + NODE_INDEX_STEP * 3 + 13),
        (1, "node", MAX_INDEX["node"], NODE_ID_OFFSET + NODE_INDEX_STEP * MAX_INDEX["node"] + 1),
        (MAX_OSM_ID["node"], "node", MAX_INDEX["node"],
            NODE_ID_OFFSET + NODE_INDEX_STEP * MAX_INDEX["node"] + MAX_OSM_ID["node"]),
        # way with index
        (1, "way", 1, WAY_ID_OFFSET + WAY_INDEX_STEP + 1),
        (13, "way", 31, WAY_ID_OFFSET + WAY_INDEX_STEP * 31 + 13),
        (1, "way", MAX_INDEX["way"], WAY_ID_OFFSET + WAY_INDEX_STEP * MAX_INDEX["way"] + 1),
        (MAX_OSM_ID["way"], "way", MAX_INDEX["way"],
            WAY_ID_OFFSET + WAY_INDEX_STEP * MAX_INDEX["way"] + MAX_OSM_ID["way"]),
        # relation with index
        (1, "relation", 1, RELATION_ID_OFFSET + RELATION_INDEX_STEP + 1),
        (13, "relation", 31, RELATION_ID_OFFSET + RELATION_INDEX_STEP * 31 + 13),
        (1, "relation", MAX_INDEX["relation"], RELATION_ID_OFFSET + RELATION_INDEX_STEP * MAX_INDEX["relation"] + 1),
        (MAX_OSM_ID["relation"], "relation", MAX_INDEX["relation"],
            RELATION_ID_OFFSET + RELATION_INDEX_STEP * MAX_INDEX["relation"] + MAX_OSM_ID["relation"]),
        # coastline with index
        (1, "coastline", 1, COASTLINE_ID_OFFSET + COASTLINE_INDEX_STEP + 1),
        (13, "coastline", 31, COASTLINE_ID_OFFSET + COASTLINE_INDEX_STEP * 31 + 13),
        (1, "coastline", MAX_INDEX["coastline"], COASTLINE_ID_OFFSET + COASTLINE_INDEX_STEP * MAX_INDEX["coastline"] + 1),
        (MAX_OSM_ID["coastline"], "coastline", MAX_INDEX["coastline"],
            COASTLINE_ID_OFFSET + COASTLINE_INDEX_STEP * MAX_INDEX["coastline"] + MAX_OSM_ID["coastline"]),
    ],
)
def test_to_ymapsdf_id(osm_id, osm_type, index, expected_ymapsdf_id):
    assert OsmObjectId(type=osm_type, id=osm_id).to_ymapsdf_id(index) == expected_ymapsdf_id


@pytest.mark.parametrize(
    ("ymapsdf_id", "expected_osm_id", "expected_osm_type"),
    [
        # without index
        (NODE_ID_OFFSET, 0, "node"),
        (NODE_ID_OFFSET + 123, 123, "node"),
        (NODE_ID_OFFSET + MAX_OSM_ID["node"], MAX_OSM_ID["node"], "node"),
        (WAY_ID_OFFSET, 0, "way"),
        (WAY_ID_OFFSET + 123, 123, "way"),
        (WAY_ID_OFFSET + MAX_OSM_ID["way"], MAX_OSM_ID["way"], "way"),
        (RELATION_ID_OFFSET, 0, "relation"),
        (RELATION_ID_OFFSET + 123, 123, "relation"),
        (RELATION_ID_OFFSET + MAX_OSM_ID["relation"], MAX_OSM_ID["relation"], "relation"),
        (COASTLINE_ID_OFFSET, 0, "coastline"),
        (COASTLINE_ID_OFFSET + 123, 123, "coastline"),
        (COASTLINE_ID_OFFSET + MAX_OSM_ID["coastline"], MAX_OSM_ID["coastline"], "coastline"),
        # with index
        (NODE_ID_OFFSET + NODE_INDEX_STEP, 0, "node"),
        (NODE_ID_OFFSET + NODE_INDEX_STEP * 13 + 123, 123, "node"),
        (NODE_ID_OFFSET + NODE_INDEX_STEP * MAX_INDEX["node"] + MAX_OSM_ID["node"], MAX_OSM_ID["node"], "node"),
        (WAY_ID_OFFSET + WAY_INDEX_STEP, 0, "way"),
        (WAY_ID_OFFSET + WAY_INDEX_STEP * 13 + 123, 123, "way"),
        (WAY_ID_OFFSET + WAY_INDEX_STEP * MAX_INDEX["way"] + MAX_OSM_ID["way"], MAX_OSM_ID["way"], "way"),
        (RELATION_ID_OFFSET + RELATION_INDEX_STEP, 0, "relation"),
        (RELATION_ID_OFFSET + RELATION_INDEX_STEP * 13 + 123, 123, "relation"),
        (RELATION_ID_OFFSET + RELATION_INDEX_STEP * MAX_INDEX["relation"] + MAX_OSM_ID["relation"],
            MAX_OSM_ID["relation"], "relation"),
        (COASTLINE_ID_OFFSET + COASTLINE_INDEX_STEP, 0, "coastline"),
        (COASTLINE_ID_OFFSET + COASTLINE_INDEX_STEP * 13 + 123, 123, "coastline"),
        (COASTLINE_ID_OFFSET + COASTLINE_INDEX_STEP * MAX_INDEX["coastline"] + MAX_OSM_ID["coastline"], MAX_OSM_ID["coastline"], "coastline"),
    ],
)
def test_from_ymapsdf_id(ymapsdf_id, expected_osm_id, expected_osm_type):
    id = OsmObjectId.from_ymapsdf_id(ymapsdf_id)
    assert id.id == expected_osm_id
    assert id.type == expected_osm_type


@pytest.mark.parametrize(
    ("osm_id", "osm_type"),
    [
        (MAX_OSM_ID["node"] + 1, "node"),
        (MAX_OSM_ID["way"] + 1, "way"),
        (MAX_OSM_ID["relation"] + 1, "relation"),
        (MAX_OSM_ID["coastline"] + 1, "coastline"),
    ],
)
def test_osm_id_overflow(osm_id, osm_type):
    with pytest.raises(GardenError):
        OsmObjectId(osm_type, osm_id)


@pytest.mark.parametrize(
    ("osm_id", "index", "osm_type"),
    [
        (0, MAX_INDEX["node"] + 1, "node"),
        (0, MAX_INDEX["way"] + 1, "way"),
        (0, MAX_INDEX["relation"] + 1, "relation"),
        (0, MAX_INDEX["coastline"] + 1, "coastline"),
        (123, MAX_INDEX["node"] + 1, "node"),
        (123, MAX_INDEX["way"] + 1, "way"),
        (123, MAX_INDEX["relation"] + 1, "relation"),
        (123, MAX_INDEX["coastline"] + 1, "coastline"),
    ],
)
def test_ymapsdf_index_overflow(osm_id, index, osm_type):
    with pytest.raises(GardenError):
        OsmObjectId(osm_type, osm_id).to_ymapsdf_id(index)


@pytest.mark.parametrize(
    "osm_type",
    [
        "FACE",
        "edge",
        "",
    ],
)
def test_invalid_osm_type(osm_type):
    with pytest.raises(GardenError):
        OsmObjectId(osm_type, 1)
