from nile.api.v1 import Record
from qb2.api.v1 import typing as qt


# -------- Input --------

ENTRANCES = [
    Record(
        bld_id=1,
        bld_lat=51.1,
        bld_lon=41.1,
        entrances=[
            {b"lat": 51.0, b"lon": 41.0, b"name": b"1"},
            {b"lat": 52.0, b"lon": 42.0},
        ],
        ghash6=b"ucj4xq",
    ),
    Record(
        bld_id=2,
        bld_lat=52.1,
        bld_lon=42.1,
        entrances=[
            {b"lat": 53.0, b"lon": 43.0, b"name": b"3"},
        ],
        ghash6=b"ucmbqh",
    ),
    Record(
        bld_id=3,
        bld_lat=53.1,
        bld_lon=43.1,
        entrances=[
            {b"lat": 54.0, b"lon": 44.0, b"name": b"4"},
            {b"lat": 55.0, b"lon": 45.0, b"name": b"5"},
            {b"lat": 56.0, b"lon": 46.0, b"name": b"6"},
        ],
        ghash6=b"ucqwhf",
    ),
]

ENTRANCES_SCHEMA = {
    "bld_id": qt.Int64,
    "bld_lat": float,
    "bld_lon": float,
    "entrances": qt.Yson,
    "ghash6": qt.String,
}

USERS = [
    Record(uid=b"1", ghash6=b"ucj4xq", dwell_lat=51.1, dwell_lon=41.1, application=b"a"),
    Record(uid=b"2", ghash6=b"ucmbqh", dwell_lat=52.1, dwell_lon=42.1, application=b"b"),
    Record(uid=b"3", ghash6=b"ucqwhf", dwell_lat=53.1, dwell_lon=43.1, application=b"c"),
    # Same geohash, but the distance is bigger than threshold.
    Record(uid=b"4", ghash6=b"ucqwhf", dwell_lat=53.102, dwell_lon=43.11, application=b"d"),
    # A dwell in a random place.
    Record(uid=b"5", ghash6=b"bzzbzz", dwell_lat=88.766, dwell_lon=-135.005, application=b"e"),
    # Two dwells near one building, we need to leave only one.
    Record(uid=b"6", ghash6=b"ucj4xq", dwell_lat=51.1, dwell_lon=41.1, application=b"f"),
    Record(uid=b"6", ghash6=b"ucj4xq", dwell_lat=51.1, dwell_lon=41.10001, application=b"g"),
    # Two dwells near two different buildings,
    # for pushes we need to leave the dwell closest to corresponding building.
    Record(uid=b"7", ghash6=b"ucj4xq", dwell_lat=51.1, dwell_lon=41.10001, application=b"h"),
    Record(uid=b"7", ghash6=b"ucmbqh", dwell_lat=52.1, dwell_lon=42.1, application=b"i"),
]


# -------- Output --------

PUSHES_OUTPUT = [
    Record(application=b"a", assignment_id=b"entrances_edit:1", lat=51.1, lon=41.1, uid=b"1"),
    Record(application=b"b", assignment_id=b"entrances_edit:2", lat=52.1, lon=42.1, uid=b"2"),
    Record(application=b"c", assignment_id=b"entrances_edit:3", lat=53.1, lon=43.1, uid=b"3"),
    Record(application=b"f", assignment_id=b"entrances_edit:1", lat=51.1, lon=41.1, uid=b"6"),
    Record(application=b"i", assignment_id=b"entrances_edit:2", lat=52.1, lon=42.1, uid=b"7"),
]

ASSIGNMENTS_OUTPUT = [
    Record(
        assignment_id=b"entrances_edit:1",
        entrances=[
            {b"lat": 51.0, b"lon": 41.0, b"name": b"1"},
            {b"lat": 52.0, b"lon": 42.0},
        ],
        lat=51.1,
        lon=41.1,
        uids=[b"1", b"6", b"7"],
    ),
    Record(
        assignment_id=b"entrances_edit:2",
        entrances=[
            {b"lat": 53.0, b"lon": 43.0, b"name": b"3"},
        ],
        lat=52.1,
        lon=42.1,
        uids=[b"2", b"7"],
    ),
    Record(
        assignment_id=b"entrances_edit:3",
        entrances=[
            {b"lat": 54.0, b"lon": 44.0, b"name": b"4"},
            {b"lat": 55.0, b"lon": 45.0, b"name": b"5"},
            {b"lat": 56.0, b"lon": 46.0, b"name": b"6"},
        ],
        lat=53.1,
        lon=43.1,
        uids=[b"3"],
    ),
]
