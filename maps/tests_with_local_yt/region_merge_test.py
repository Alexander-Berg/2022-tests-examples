import os
import tarfile

import pytest

from mapreduce.yt.python.yt_stuff import YtConfig
import yatest.common

from yandex.maps.geolib3 import Point2, Polyline2, SpatialReference
from maps.libs.road_graph_import_yt.pylib.graph_import import build_topology

SPATIAL_REFERENCE = SpatialReference.Epsg4326

GEODATA_6_BIN_PATH = yatest.common.binary_path(
    "geobase/data/v6/geodata6.bin")
TZDATA_TAR_GZ_PATH = yatest.common.binary_path(
    "maps/data/test/tzdata/tzdata.tar.gz")
TZDATA_PATH = yatest.common.output_path("tzdata")
TZDATA_ZONES_BIN_PATH = os.path.join(TZDATA_PATH, "zones_bin")


@pytest.fixture(scope="module")
def yt_config(request):
    return YtConfig(
        enable_debug_logging=True,
    )


def encode(geometry):
    return geometry.to_EWKB(SPATIAL_REFERENCE).hex().upper()


def decode_point(hex_encoded_ewkb):
    return Point2.from_EWKB(bytes.fromhex(hex_encoded_ewkb))


def decode_polyline(hex_encoded_ewkb):
    return Polyline2.from_EWKB(bytes.fromhex(hex_encoded_ewkb))


def make_rd_jc_dict(rd_jc_id, point):
    return {
        "rd_jc_id": rd_jc_id,
        "shape": encode(point),
        "x": point.x,
        "y": point.y,
    }


def pt(x, y):
    """Create a point on a grid.

    Points created this way are relatively close to each other. So, the
    polylines built around them are probably not going to be split in segments.
    """

    return Point2(0.0001 * x, 0.0001 * y)


def make_rd_el_dict(
        rd_el_id, f_rd_jc_id, t_rd_jc_id, isocode, source_point, target_point):
    polyline = Polyline2()
    polyline.add(source_point)
    polyline.add(target_point)

    return {
        "rd_el_id": rd_el_id,
        "f_rd_jc_id": f_rd_jc_id,
        "t_rd_jc_id": t_rd_jc_id,
        "fc": 3,
        "fow": 0,
        "speed_cat": 5,
        "speed_limit": 60,
        "f_zlev": 0,
        "t_zlev": 0,
        "oneway": "B",
        "access_id": 63,
        "back_bus": 0,
        "forward_bus": 0,
        "back_taxi": 0,
        "forward_taxi": 0,
        "residential": 0,
        "restricted_for_trucks": 0,
        "paved": 1,
        "poor_condition": 0,
        "stairs": 0,
        "sidewalk": "B",
        "struct_type": 0,
        "ferry": 0,
        "dr": 0,
        "toll": 0,
        "srv_ra": 0,
        "srv_uc": 0,
        "isocode": isocode,
        "subcode": None,
        "shape": encode(polyline),
        "xmin": polyline.bounding_box().min_x,
        "xmax": polyline.bounding_box().max_x,
        "ymin": polyline.bounding_box().min_y,
        "ymax": polyline.bounding_box().max_y,
        "speed_limit_f": None,
        "speed_limit_t": None,
        "speed_limit_truck_f": None,
        "speed_limit_truck_t": None,
    }


#
# Region 1   Region 2     Result
#  (old)      (new)
#
#            [2]-[6]        [2]-[6]
#             |            / |
# [4]-[2]     |         [4]  |
#      |      |              |
# [3]-[1]    [1]-[5]    [3]-[1]-[5]
#
# isocode    isocode    isocode
#   RU         ZZ         RU for 1-2, 1-3, 2-4
#                         ZZ for 1-5, 2-6
#
# Edge 1-2 is imported with new geometry, but "old" isocode.
#

def prepare_data(yt_client):
    yt_client.create("table", "//ymapsdf/region_1/rd_jc", recursive=True)
    yt_client.write_table(
        "//ymapsdf/region_1/rd_jc",
        [
            make_rd_jc_dict(rd_jc_id=1, point=pt(0, 0)),
            make_rd_jc_dict(rd_jc_id=2, point=pt(0, 1)),
            make_rd_jc_dict(rd_jc_id=3, point=pt(-1, 0)),
            make_rd_jc_dict(rd_jc_id=4, point=pt(-1, 1)),
        ])

    yt_client.create("table", "//ymapsdf/region_1/rd_el", recursive=True)
    yt_client.write_table(
        "//ymapsdf/region_1/rd_el",
        [
            make_rd_el_dict(1, 1, 2, "RU", pt(0, 0), pt(0, 1)),
            make_rd_el_dict(2, 1, 3, "RU", pt(0, 0), pt(-1, 0)),
            make_rd_el_dict(3, 2, 4, "RU", pt(0, 1), pt(-1, 1)),
        ])

    yt_client.create("table", "//ymapsdf/region_2/rd_jc", recursive=True)
    yt_client.write_table(
        "//ymapsdf/region_2/rd_jc",
        [
            make_rd_jc_dict(rd_jc_id=1, point=pt(0, 0)),
            make_rd_jc_dict(rd_jc_id=2, point=pt(0, 2)),
            make_rd_jc_dict(rd_jc_id=5, point=pt(1, 0)),
            make_rd_jc_dict(rd_jc_id=6, point=pt(1, 2)),
        ])

    yt_client.create("table", "//ymapsdf/region_2/rd_el", recursive=True)
    yt_client.write_table(
        "//ymapsdf/region_2/rd_el",
        [
            make_rd_el_dict(1, 1, 2, "ZZ", pt(0, 0), pt(0, 2)),
            make_rd_el_dict(4, 1, 5, "ZZ", pt(0, 0), pt(1, 0)),
            make_rd_el_dict(5, 2, 6, "ZZ", pt(0, 2), pt(1, 2)),
        ])


def test_region_merge(yt_stuff):
    with tarfile.open(TZDATA_TAR_GZ_PATH) as tzdata_archive:
        tzdata_archive.extractall(TZDATA_PATH)

    yt_client = yt_stuff.get_yt_client()
    prepare_data(yt_client)

    os.environ["YT_PROXY"] = yt_stuff.get_server()
    build_topology(
        desiredAccessId=["auto", "taxi", "truck"],
        geodata6Path=GEODATA_6_BIN_PATH,
        tzdataZonesBinPath=TZDATA_ZONES_BIN_PATH,
        rdElTables=[
            "//ymapsdf/region_2/rd_el",
            "//ymapsdf/region_1/rd_el",
        ],
        rdJcTables=[
            "//ymapsdf/region_2/rd_jc",
            "//ymapsdf/region_1/rd_jc",
        ],
        outputEdgesTable="//output/edges",
        outputVerticesTable="//output/vertices",
        outputProblematicRdJcTable="//output/problematic_rd_jc")

    points = {}
    for row in yt_client.read_table("//output/vertices"):
        points[row["rd_jc_id"]] = decode_point(row["shape"])

    assert len(points) == 6

    assert points[1] == pt(0, 0)
    assert points[2] == pt(0, 2)
    assert points[3] == pt(-1, 0)
    assert points[4] == pt(-1, 1)
    assert points[5] == pt(1, 0)
    assert points[6] == pt(1, 2)

    problematic_points = {}
    for row in yt_client.read_table("//output/problematic_rd_jc"):
        problematic_points[row["rd_jc_id"]] = decode_point(row["shape"])

    assert len(problematic_points) == 1
    assert problematic_points[2] == pt(0, 2)

    forward_edges = {}
    reverse_edges = {}
    for row in yt_client.read_table("//output/edges"):
        if row["reversed"]:
            reverse_edges[row["rd_el_id"]] = row
        else:
            forward_edges[row["rd_el_id"]] = row

    assert len(forward_edges) == 5
    assert len(reverse_edges) == 5

    def check_rd_el(rd_el_id, f_rd_jc_id, t_rd_jc_id, isocode):
        assert forward_edges[rd_el_id]["f_rd_jc_id"] == f_rd_jc_id
        assert forward_edges[rd_el_id]["t_rd_jc_id"] == t_rd_jc_id
        assert reverse_edges[rd_el_id]["f_rd_jc_id"] == t_rd_jc_id
        assert reverse_edges[rd_el_id]["t_rd_jc_id"] == f_rd_jc_id

        forward_polyline = Polyline2()
        forward_polyline.add(points[f_rd_jc_id])
        forward_polyline.add(points[t_rd_jc_id])

        reverse_polyline = Polyline2()
        reverse_polyline.add(points[t_rd_jc_id])
        reverse_polyline.add(points[f_rd_jc_id])

        assert forward_edges[rd_el_id]["shape"] == encode(forward_polyline)
        assert reverse_edges[rd_el_id]["shape"] == encode(reverse_polyline)

        assert forward_edges[rd_el_id]["isocode"] == isocode
        assert reverse_edges[rd_el_id]["isocode"] == isocode

    check_rd_el(1, 1, 2, "RU")
    check_rd_el(2, 1, 3, "RU")
    check_rd_el(3, 2, 4, "RU")
    check_rd_el(4, 1, 5, "ZZ")
    check_rd_el(5, 2, 6, "ZZ")
