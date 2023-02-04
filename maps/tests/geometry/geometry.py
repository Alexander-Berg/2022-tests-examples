from nile.api.v1 import Record
from yandex.maps.geolib3 import (
    get_geometry_type_from_wkb, mercator_to_geo,
    GeometryType, Point2, Polyline2, Polygon2
)

import logging


_ATLANTIS_COORDS = Point2(0.0, 0.0)


def _get_bbox(hex_encoded_geom):
    '''
    Converts a geometry described in hex-encoded WKB/EWKB to bounding box
    '''
    if hex_encoded_geom is None:
        return None

    try:
        geom = bytes.fromhex(hex_encoded_geom.decode())

        geom_type = get_geometry_type_from_wkb(geom)

        if geom_type == GeometryType.Point:
            return Point2.from_WKB(geom).bounding_box()
        elif geom_type == GeometryType.LineString:
            return Polyline2.from_WKB(geom).bounding_box()
        elif geom_type == GeometryType.Polygon:
            return Polygon2.from_WKB(geom).bounding_box()

        elif geom_type == GeometryType.EwkbPoint:
            return Point2.from_EWKB(geom).bounding_box()
        elif geom_type == GeometryType.EwkbLineString:
            return Polyline2.from_EWKB(geom).bounding_box()
        elif geom_type == GeometryType.EwkbPolygon:
            return Polygon2.from_EWKB(geom).bounding_box()

        else:
            logging.warning(f'Unsupported geometry: "{hex_encoded_geom.decode()}".')

    except:
        logging.warning(f'Wrong geometry: "{hex_encoded_geom.decode()}".')

    return None


def geo_wkb_geometry_to_geo_bbox(hex_encoded_geo_wkb):
    '''
    Converts a geometry described in hex-encoded WKB (geodetic, EPSG:4326) to
    bounding box (geodetic, EPSG:4326).
    '''
    geo_lower_left_corner = _ATLANTIS_COORDS
    geo_upper_right_corner = _ATLANTIS_COORDS

    geo_bbox = _get_bbox(hex_encoded_geo_wkb)
    if geo_bbox is not None:
        geo_lower_left_corner = geo_bbox.lower_corner
        geo_upper_right_corner = geo_bbox.upper_corner

    return {
        'lon_min': geo_lower_left_corner.lon,
        'lat_min': geo_lower_left_corner.lat,
        'lon_max': geo_upper_right_corner.lon,
        'lat_max': geo_upper_right_corner.lat
    }


def mercator_ewkb_geometry_to_geo_bbox(hex_encoded_mercator_ewkb):
    '''
    Converts a geometry described in hex-encoded EWKB (mercator, EPSG:3395) to
    bounding box (geodetic, EPSG:4326).
    '''
    geo_lower_left_corner = _ATLANTIS_COORDS
    geo_upper_right_corner = _ATLANTIS_COORDS

    mercator_bbox = _get_bbox(hex_encoded_mercator_ewkb)
    if mercator_bbox is not None:
        geo_lower_left_corner = mercator_to_geo(mercator_bbox.lower_corner)
        geo_upper_right_corner = mercator_to_geo(mercator_bbox.upper_corner)

    return {
        'lon_min': geo_lower_left_corner.lon,
        'lat_min': geo_lower_left_corner.lat,
        'lon_max': geo_upper_right_corner.lon,
        'lat_max': geo_upper_right_corner.lat
    }


def append_bbox(table, geometry_column, geometry_to_geo_bbox_func):
    '''
    Appends columns with coordinates of bounding box of a geometry from column
    `geometry_column`. The geometry is converted to bounding box by means of
    function `geometry_to_geo_bbox_func()`. This function must return dict with
    following keys: `lat_min`, `lon_min`, `lat_max` and `lon_max`.

    table:
    | geometry_column | ... |
    |-----------------+-----|
    | ...             | ... |

    Result:
    | geometry_column | ... | lat_min | lon_min | lat_max | lon_max |
    |-----------------+-----+---------+---------+---------+---------|
    | ...             | ... | ...     | ...     | ...     | ...     |
    '''
    def append_bbox_mapper(records):
        for record in records:
            yield Record(record, geometry_to_geo_bbox_func(record.get(geometry_column)))

    return table.map(append_bbox_mapper)
