# -*- coding: utf-8 -*-

import binascii
import copy
import csv

from yandex.maps import geolib3


MIN_X = 0.0
MIN_Y = 0.0
MAX_X = 12.0
MAX_Y = 34.0

CENTER_X = 1.0
CENTER_Y = 2.0

BINARY_DATA = b"Binary Data with special characters \t \\t and \n \\n and \0 and even \\"

YT_TABLE_SCHEMA = [
    {"name": "bigint_field", "type": "int64", "required": True},
    {"name": "double_field", "type": "double", "required": True},
    {"name": "text_field", "type": "string", "required": True},
    {"name": "boolean_field", "type": "boolean", "required": True},
    {"name": "null_field", "type": "int64", "required": False},
    {"name": "kmz", "type": "string", "required": True},
    {"name": "shape", "type": "string", "required": True},
    {"name": "center", "type": "string", "required": True},
]

EXTENDED_TABLE_SCHEMA = {
    "columns": [
        {"name": "bigint_field", "yt_type": "int64", "pg_type": "bigint", "required": True},
        {"name": "double_field", "yt_type": "double", "pg_type": "double precision", "required": True},
        {"name": "text_field", "yt_type": "string", "pg_type": "text", "required": True},
        {"name": "boolean_field", "yt_type": "boolean", "pg_type": "boolean", "required": True},
        {"name": "null_field", "yt_type": "int64", "pg_type": "bigint", "required": False},
        {"name": "kmz", "yt_type": "string", "pg_type": "bytea", "required": True},
        {"name": "shape", "yt_type": "string", "pg_type": "geometry(LineString,4326)", "geometry_type": "LINESTRING", "required": True},
        {"name": "center", "yt_type": "string", "pg_type": "geometry(Point,4326)", "geometry_type": "POINT", "required": True}
    ],
    "key_columns": []
}


def _create_test_polyline_ewkb():
    polyline = geolib3.Polyline2()
    polyline.add(geolib3.Point2(MIN_X, MIN_Y))
    polyline.add(geolib3.Point2(MAX_X, MAX_Y))
    wkb = polyline.to_EWKB(geolib3.SpatialReference.Epsg4326)
    return binascii.hexlify(wkb).decode().upper()


def _create_test_point_ewkb():
    point = geolib3.Point2(CENTER_X, CENTER_Y)
    wkb = point.to_EWKB(geolib3.SpatialReference.Epsg4326)
    return binascii.hexlify(wkb).decode().upper()


def _convert_bytes_to_pg_format(raw):
    return r'\x' + binascii.hexlify(raw).decode()


TABLE_DATA = [
    {
        b"bigint_field": 123,
        b"double_field": 3.1415,
        b"text_field": u"Hello, world! Привет, мир! Привет, 'Аполлон-11'! \t \\t \n \\n \\",
        b"boolean_field": True,
        b"null_field": None,
        b"kmz": BINARY_DATA,
        b"shape": _create_test_polyline_ewkb(),
        b"center": _create_test_point_ewkb(),
    },
    {
        b"bigint_field": 345,
        b"double_field": -123.0,
        b"text_field": "",
        b"boolean_field": False,
        b"null_field": 222,
        b"kmz": BINARY_DATA,
        b"shape": _create_test_polyline_ewkb(),
        b"center": _create_test_point_ewkb(),
    }
]


def convert_to_pg_format(data):
    converted = copy.deepcopy(data)
    for row in converted:
        row[b"kmz"] = _convert_bytes_to_pg_format(row[b"kmz"])
    return converted


def convert_to_yt_format(data):
    converted = []
    for row in data:
        converted_row = {}
        for key, value in row.items():
            if isinstance(value, str):
                converted_row[key] = value.encode()
            else:
                converted_row[key] = value
        converted.append(converted_row)
    return converted


def dump_table_to_csv(data, csv_filename):
    with open(csv_filename, "wt") as csv_file:
        writer = csv.writer(csv_file)
        for row in convert_to_pg_format(data):
            writer.writerow(row.values())
