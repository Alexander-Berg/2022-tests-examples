import maps.analyzer.modules.matcher.matcher.tools.debug_matcher.lib.decode_matched_path as decode
import yandex.maps.proto.common2.geometry_pb2 as proto
import yandex.maps.proto.matcher.matched_path2_pb2 as mp2
import math

"""
            0       1       2       3       4
points:     *       *       *       *       *
geometry:   |--|--|-|---------------|--|--|-|
sections:   |-----|-|---------------|-------|
"""

POINTS = (
    {"x": 0, "y": -1, "t": 10, "pos": 0.67, "ind": 32},
    {"x": 1, "y": -3, "t": 11, "pos": 0.0, "ind": 27},
    {"x": 2, "y": -5, "t": 15, "pos": None, "ind": None},
    {"x": 3, "y": -7, "t": 17, "pos": 1.0, "ind": 21},
    {"x": 4, "y": -11, "t": 23, "pos": 1.0, "ind": 56},
)
GEOMETRY = [(0, -1), (0.2, -2), (0.4, -2), (1, -3), (3, -7), (3.2, -7.5), (3.8, -7.9), (4, -9)]
SECTIONS = (
    {
        "count": 2,
        "length": 12,
        "road_class": 1,
        "speedLimit": 60,
        "is_rugged_road": True,
        "is_toll_road": False
    }, {
        "count": 1,
        "length": 3,
        "road_class": 3,
        "speedLimit": 10,
        "is_rugged_road": False,
        "is_toll_road": False
    }, {
        "count": 1,
        "length": 40,
        "road_class": None,
        "speedLimit": None,
        "is_rugged_road": None,
        "is_toll_road": None
    }, {
        "count": 3,
        "length": 27,
        "road_class": 0,
        "speedLimit": 90,
        "is_rugged_road": True,
        "is_toll_road": False
    }
)


def convert_coord(coord):
    return math.floor(coord * 1e6 + 0.5)


def coord_sequence(coords):
    sequence = proto.CoordSequence()
    sequence.first = convert_coord(coords[0])
    last_coord = coords[0]
    for c in coords[1:]:
        sequence.deltas.append(convert_coord(c - last_coord))
        last_coord = c
    return sequence


def encode_polyline(points):
    polyline = proto.Polyline()
    polyline.lons.CopyFrom(coord_sequence([point[0] for point in points]))
    polyline.lats.CopyFrom(coord_sequence([point[1] for point in points]))
    return polyline


def create_matched_path():
    matched_path = mp2.MatchedPath()
    for p in POINTS:
        point = matched_path.points.add()
        point.timestamp = p["t"]
        point.point.lon = p["x"]
        point.point.lat = p["y"]
        if p["pos"] is not None and p["ind"] is not None:
            point.position.segment_index = p["ind"]
            point.position.segment_position = p["pos"]
    matched_path.geometry.CopyFrom(encode_polyline(GEOMETRY))
    for s in SECTIONS:
        section = matched_path.sections.section.add()
        section.count = s["count"]
        section.length = s["length"]
        if s.get("road_class") is not None:
            section.attributes.road_class = s["road_class"]
            section.attributes.speedLimit = s["speedLimit"]
            section.attributes.is_rugged_road = s["is_rugged_road"]
            section.attributes.is_toll_road = s["is_toll_road"]
    return matched_path


def check(decoded_path: decode.MatchedPath):
    points = decoded_path.points()
    assert len(points) == len(POINTS)
    for actual, expected in zip(points, POINTS):
        assert actual.position.x == expected["x"]
        assert actual.position.y == expected["y"]
        assert actual.timestamp == expected["t"]
        assert actual.segment_position == expected["pos"]
        assert actual.segment_index == expected["ind"]

    polyline = decoded_path.polyline()
    assert len(polyline.points) == len(GEOMETRY)
    for actual, expected in zip(polyline.points, GEOMETRY):
        assert actual.x == expected[0]
        assert actual.y == expected[1]

    sections = list(decoded_path.sections())
    assert len(sections) == len(SECTIONS)
    for actual, expected in zip(sections, SECTIONS):
        actual_count = actual.end_point_index - actual.begin_point_index
        assert actual_count == expected["count"]
        assert actual.length == expected["length"]
        if actual.attributes is None:
            assert expected["road_class"] is None
            continue
        expected_road_class = \
            decode.Section._road_class_str.get(expected["road_class"])
        assert actual.attributes["road_class"] == expected_road_class
        assert actual.attributes["speed_limit"] == expected["speedLimit"]
        assert actual.attributes["is_rugged_road"] == expected["is_rugged_road"]
        assert actual.attributes["is_toll_road"] == expected["is_toll_road"]


matched_path = create_matched_path()
decoded_path = decode.decode(matched_path.SerializeToString())
check(decoded_path)
