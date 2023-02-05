#!/usr/bin/python
# -*- coding: utf-8 -*-
import math
import pickle
import unittest
from io import BytesIO
from base64 import b64encode, b64decode
from builtins import range

from shapely import wkt, wkb

from yandex.maps.geolib3 import (
    AffineTransform2,
    BoundingBox,
    ControlPoint2,
    GeometryType,
    MultiPolygon2,
    Orientation,
    Point2,
    Polygon2,
    Polyline2,
    PolylinesVector,
    PolynomialTransform2,
    Segment2,
    SimpleGeometryTransform2,
    SpatialReference,
    SplineTransform2,
    TransformDirection,
    Vector2,
    angle,
    bearing,
    contains,
    cross_product,
    degrees_to_radians,
    distance,
    equidistant,
    expand,
    fast_geodistance,
    geo_to_mercator,
    geodistance,
    get_geometry_type_from_wkb,
    inner_product,
    intersects,
    markup_orientations,
    mercator_to_geo,
    radians_to_degrees,
    resize_by_ratio,
    resize_by_value,
    rotate,
    rotate_by_90,
    signed_angle,
    signed_area,
    simplify,
    squared_distance
)


class Geolib3Test(unittest.TestCase):
    def __test_pickle(self, obj):
        bytes_io = BytesIO()
        pickle.dump(obj, bytes_io)
        bytes_io.seek(0)
        unpickled = pickle.load(bytes_io)
        bytes_io.close()
        self.assertEqual(obj, unpickled)

    def test_point(self):
        p = Point2(1.0, 2.0)
        self.assertEqual(p, p)
        self.assertEqual(p.x, 1.0)
        self.assertEqual(p.lon, 1.0)
        self.assertEqual(p.y, 2.0)
        self.assertEqual(p.lat, 2.0)
        self.assertEqual(p, Point2(1.0, 2.0))
        self.assertEqual(p - Point2(0.0, 1.0), Vector2(1.0, 1.0))
        self.assertNotEqual(p, Point2(2.0, 1.0))
        self.assertEqual(p.to_WKT(), "POINT (1.0000000000000000 2.0000000000000000)")
        self.assertEqual(str(p), "POINT (1.0000000000000000 2.0000000000000000)")
        self.assertEqual(p, Point2.from_WKT(p.to_WKT()))
        self.assertEqual(p, Point2.from_WKT("POINT (1.0 2.0)"))

        self.assertEqual(bearing(Point2(37.689211, 55.67461), Point2(37.689211, 56.67461)), 0.0)
        self.assertEqual(distance(Point2(1.0, 3.0), Point2(-2.0, 7.0)), 5.0)
        self.assertEqual(squared_distance(Point2(1.0, 3.0), Point2(-2.0, 7.0)), 25.0)

        bbox = p.bounding_box()
        self.assertEqual(bbox.center(), p)
        self.assertEqual(bbox.area(), 0)

        self.assertEqual(Point2.from_WKB(p.to_WKB()), p)
        self.assertEqual(
            get_geometry_type_from_wkb(p.to_WKB()),
            GeometryType.Point
        )
        self.assertEqual(b64encode(p.to_WKB()), b"AQEAAAAAAAAAAADwPwAAAAAAAABA")
        self.assertEqual(Point2.from_WKB(b64decode(b"AQEAAAAAAAAAAADwPwAAAAAAAABA")), p)

        self.assertEqual(Point2.from_EWKB(p.to_EWKB(SpatialReference.Epsg4326)), p)

        self.__test_pickle(p)

    def test_geodistance(self):
        self.assertAlmostEqual(
            geodistance(Point2(49.10237599, 55.8012004), Point2(49.10269776, 55.8909913)),
            9997.254687, places=5)
        self.assertAlmostEqual(
            geodistance(Point2(49.102, 55.801), Segment2(Point2(49.103, 55.821), Point2(49.104, 55.831))),
            2227.650848, places=5)
        self.assertAlmostEqual(
            fast_geodistance(Point2(49.10237599, 55.8012004), Point2(49.10269776, 55.8909913)),
            9997., places=-3)
        self.assertAlmostEqual(
            fast_geodistance(Point2(49.102, 55.801), Segment2(Point2(49.103, 55.821), Point2(49.104, 55.831))),
            2227, places=-3)

        point = Point2(49.102, 55.801)
        poly = Polyline2()
        self.assertRaises(Exception, geodistance, point, poly)
        self.assertRaises(Exception, fast_geodistance, point, poly)

        poly.add(Point2(49.102, 55.802))
        self.assertAlmostEqual(
            geodistance(point, poly),
            111.338245, places=5)
        self.assertAlmostEqual(
            fast_geodistance(point, poly),
            111.3, places=0)

        poly.add(Point2(49.103, 55.801))
        self.assertAlmostEqual(
            geodistance(point, poly),
            54.641568, places=4)
        self.assertAlmostEqual(
            fast_geodistance(point, poly),
            54.6, places=0)

    def test_vector(self):
        v = Vector2(4.0, 3.0)
        v3 = 3.0*v
        v90 = rotate(v, math.pi/2.0)

        self.assertEqual(v.x, 4.0)
        self.assertEqual(v.y, 3.0)
        self.assertEqual(v.length(), 5)
        self.assertEqual(v.squared_length(), 25)

        self.assertEqual(Point2(1.0, 1.0)+v, Point2(5.0, 4.0))
        self.assertEqual(v - v, Vector2(0.0, 0.0))
        self.assertEqual(v.unit().length(), 1.0)

        self.assertEqual(angle(v, v3), 0.0)
        self.assertEqual(v3.length(), 3.0*v.length())
        self.assertEqual((v3/3).length(), v.length())
        self.assertEqual((v + (-v)).length(), 0.0)

        self.assertEqual(v, Vector2(4.0, 3.0))
        self.assertNotEqual(v, v3)

        self.assertTrue(signed_angle(v, -v90) + math.pi/2.00 < 1e-6)
        self.assertEqual(cross_product(v, v3), 0.0)
        self.assertTrue(inner_product(v, v90) < 1e-6)

        other_v90 = rotate_by_90(v, Orientation.Counterclockwise)
        self.assertAlmostEqual(other_v90.x, v90.x, places=6)
        self.assertAlmostEqual(other_v90.y, v90.y, places=6)

        self.__test_pickle(v)

    def test_segment(self):
        self.assertTrue(Segment2().is_degenerate)

        segment = Segment2(Point2(30.31759895, 59.95082219), Vector2(-0.00040770, -0.00041976))
        self.assertEqual(segment.start, Point2(30.31759895, 59.95082219))
        self.assertEqual(segment.end, Point2(30.31719125, 59.95040243))
        self.assertEqual(int(segment.geolength()), 52)

        segment = Segment2(Point2(1.0, 3.0), Point2(-2.0, 7.0))
        self.assertEqual(segment.length(), 5.0)
        self.assertEqual(segment.squared_length(), 25.0)
        self.assertEqual(segment.midpoint(), Point2(-0.5, 5.0))
        self.assertEqual(segment.point_by_position(0.25), Point2(0.25, 4.0))
        self.assertEqual(segment.vector(), Vector2(-3.0, 4.0))

        shift_segment = segment + Vector2(1.0, 1.0)
        self.assertEqual(shift_segment.start, Point2(2.0, 4.0))
        self.assertEqual(shift_segment.end, Point2(-1.0, 8.0))

        bbox = segment.bounding_box()
        self.assertEqual(bbox, BoundingBox(Point2(1.0, 3.0), Point2(-2.0, 7.0)))

        self.__test_pickle(segment)

    def test_bbox(self):
        bbox = BoundingBox(Point2(1.0, 3.0), Point2(-2.0, 7.0))
        self.assertFalse(bbox.is_degenerate)
        self.assertTrue(BoundingBox().is_degenerate)

        self.assertEqual(bbox.min_x, -2.0)
        self.assertEqual(bbox.min_y, 3.0)
        self.assertEqual(bbox.max_x, 1.0)
        self.assertEqual(bbox.max_y, 7.0)
        self.assertEqual(bbox.width, 3.0)
        self.assertEqual(bbox.height, 4.0)

        self.assertEqual(bbox.lower_corner, Point2(-2.0, 3.0))
        self.assertEqual(bbox.upper_corner, Point2(1.0, 7.0))
        self.assertEqual(bbox.center(), Point2(-0.5, 5.0))
        self.assertEqual(bbox.area(), 12.0)

        self.assertFalse(contains(bbox, Point2(0.0, 0.0)))
        self.assertTrue(contains(bbox, Point2(0.0, 5.0)))

        bbox = BoundingBox(Point2(0.0, 3.0), 4.0, 2.0)
        self.assertEqual(bbox.center(), Point2(0.0, 3.0))
        self.assertEqual(bbox, BoundingBox(Point2(-2.0, 2.0), Point2(2.0, 4.0)))

        bbox = expand(bbox, BoundingBox(Point2(-1.0, 0.0), Point2(3.0, 7.0)))
        self.assertEqual(bbox, BoundingBox(Point2(-2.0, 0.0), Point2(3.0, 7.0)))
        self.assertTrue(contains(bbox, BoundingBox(Point2(-1.0, 0.0), Point2(3.0, 7.0))))

        bbox = expand(bbox, Point2(1.0, 1.0))
        self.assertEqual(bbox, BoundingBox(Point2(-2.0, 0.0), Point2(3.0, 7.0)))

        bbox = expand(bbox, Segment2(Point2(2.0, 0.0), Point2(2.0, 8.0)))
        self.assertEqual(bbox, BoundingBox(Point2(-2.0, 0.0), Point2(3.0, 8.0)))

        poly = Polyline2()
        poly.add(Point2(0.0, 0.0))
        poly.add(Point2(3.0, 4.0))
        poly.add(Point2(4.0, 7.0))
        bbox = expand(bbox, poly)
        self.assertEqual(bbox.upper_corner, Point2(4.0, 8.0))

        bbox = BoundingBox(Point2(1.0, 2.0), Point2(0.0, -3.0))
        bbox = resize_by_value(bbox, 4)
        self.assertEqual(bbox, BoundingBox(Point2(-4.0, -7.0), Point2(5.0, 6.0)))

        self.assertEqual(bbox.lower_corner, Point2(-4.0, -7.0))
        self.assertEqual(bbox.upper_corner, Point2(5.0, 6.0))
        self.assertEqual(bbox.center(), Point2(0.5, -0.5))

        bbox = resize_by_ratio(bbox, 2.0)
        self.assertEqual(bbox.center(), Point2(0.5, -0.5))
        self.assertEqual(bbox.lower_corner, Point2(-8.5, -13.5))
        self.assertEqual(bbox.upper_corner, Point2(9.5, 12.5))

        self.__test_pickle(bbox)

    def test_polyline(self):
        poly = Polyline2()
        poly.add(Point2(0.0, 0.0))
        poly.add(Point2(3.0, 4.0))
        poly.add(Point2(3.0, 7.0))

        self.assertEqual(list(poly), [Point2(0.0, 0.0),
                                      Point2(3.0, 4.0),
                                      Point2(3.0, 7.0)])

        self.assertEqual(poly.points_number, 3)
        self.assertEqual(len(poly), 3)
        self.assertEqual(poly[1], Point2(3.0, 4.0))
        self.assertEqual(poly.point_at(2), Point2(3.0, 7.0))
        self.assertRaises(IndexError, poly.point_at, -1)
        self.assertRaises(IndexError, poly.point_at, 3)

        self.assertEqual(poly.segments_number, 2)
        self.assertEqual(poly.segment_at(1).start, Point2(3.0, 4.0))
        self.assertEqual(poly.segment_at(1).end, Point2(3.0, 7.0))
        self.assertEqual(list(poly.segments()), [
            Segment2(Point2(0.0, 0.0), Point2(3.0, 4.0)),
            Segment2(Point2(3.0, 4.0), Point2(3.0, 7.0))
        ])

        self.assertEqual(poly.to_WKT(), "LINESTRING ("
                         "0.0000000000000000 0.0000000000000000, "
                         "3.0000000000000000 4.0000000000000000, "
                         "3.0000000000000000 7.0000000000000000)")
        self.assertEqual(str(poly), "LINESTRING ("
                         "0.0000000000000000 0.0000000000000000, "
                         "3.0000000000000000 4.0000000000000000, "
                         "3.0000000000000000 7.0000000000000000)")
        self.assertEqual(poly, Polyline2.from_WKT(poly.to_WKT()))
        self.assertEqual(poly, Polyline2.from_WKT(
            "LINESTRING (0.0 0.0, 3.0 4.0, 3.0 7.0)"
        ))

        self.assertEqual(poly.length(), 8.0)
        self.assertEqual(simplify(poly, 3.0).points_number, 2)

        ed = equidistant(poly, 1.0, Orientation.Clockwise)
        self.assertEqual(
            [ed[p] for p in range(ed.points_number)],
            [Point2(0.8, -0.6), Point2(4.0, 3.6666666), Point2(4.0, 7.0)]
        )

        bbox = poly.bounding_box()
        self.assertEqual(bbox, BoundingBox(Point2(0.0, 0.0), Point2(3.0, 7.0)))

        self.assertEqual(Polyline2.from_WKB(poly.to_WKB()), poly)
        self.assertEqual(
            b64encode(poly.to_WKB()),
            b"AQIAAAADAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAhAAAAAAAAAEEAAAAAAAAAIQAAAAAAAABxA")
        self.assertEqual(Polyline2.from_WKB(b64decode(
            b"AQIAAAADAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAhAAAAAAAAAEEAAAAAAAAAIQAAAAAAAABxA")), poly)

        self.__test_pickle(poly)

        poly = Polyline2()
        poly.add(Point2(30.31759895, 59.95082219))
        poly.add(Point2(30.31719125, 59.95040243))
        self.assertEqual(int(poly.geolength()), 52)

    def test_polylines_vector(self):
        polylines_vector = PolylinesVector.from_WKT(
            "MULTILINESTRING ("
            "(10.00 10.00, 20.00 20.00, 10.00 40.00), "
            "(40.00 40.00, 30.00 30.00, 40.00 20.00, 30.00 10.00))"
        )

        self.assertEqual(len(polylines_vector), 2)
        self.assertEqual(polylines_vector[0].to_WKT(), "LINESTRING ("
                         "10.0000000000000000 10.0000000000000000, "
                         "20.0000000000000000 20.0000000000000000, "
                         "10.0000000000000000 40.0000000000000000)")
        self.assertEqual(polylines_vector[1].to_WKT(), "LINESTRING ("
                         "40.0000000000000000 40.0000000000000000, "
                         "30.0000000000000000 30.0000000000000000, "
                         "40.0000000000000000 20.0000000000000000, "
                         "30.0000000000000000 10.0000000000000000)")

        bbox = polylines_vector.bounding_box()
        self.assertEqual(bbox, BoundingBox(Point2(10, 10), Point2(40, 40)))
        from_wkt = wkt.loads(polylines_vector.to_WKT())
        from_wkb = wkb.loads(polylines_vector.to_WKB())
        self.assertTrue(from_wkt.equals(from_wkb))

    def test_polygon(self):
        polygon = Polygon2.from_WKT(
            "POLYGON (("
            "0.000 0.000, "
            "1.000 0.000, "
            "0.000 1.000, "
            "0.000 0.000))")
        self.assertEqual(polygon.interior_rings_number(), 0)
        bbox = polygon.bounding_box()
        self.assertEqual(bbox, BoundingBox(Point2(0, 0), Point2(1, 1)))

        from_wkt = wkt.loads(polygon.to_WKT())
        from_wkb = wkb.loads(polygon.to_WKB())
        self.assertTrue(from_wkt.equals(from_wkb))

        ring = polygon.exterior_ring()
        self.assertEqual(ring.points_number, 3)
        self.assertEqual(len(ring), 3)
        self.assertEqual(ring[0], Point2(0.0, 0.0))
        self.assertEqual(ring[1], Point2(1.0, 0.0))
        self.assertEqual(ring[2], Point2(0.0, 1.0))
        polyline = ring.to_polyline()
        self.assertEqual(list(polyline), [
            Point2(0.0, 0.0),
            Point2(1.0, 0.0),
            Point2(0.0, 1.0),
            Point2(0.0, 0.0)])

    def test_multipolygon(self):
        mp = MultiPolygon2.from_WKT(
            "MULTIPOLYGON ((("
            "0.000 0.000, "
            "1.000 0.000, "
            "0.000 1.000, "
            "0.000 0.000)))")
        self.assertEqual(mp.polygon_number, 1)
        bbox = mp.bounding_box()
        self.assertEqual(bbox, BoundingBox(Point2(0, 0), Point2(1, 1)))
        from_wkt = wkt.loads(mp.to_WKT())
        from_wkb = wkb.loads(mp.to_WKB())
        self.assertTrue(from_wkt.equals(from_wkb))

    def test_conversions(self):
        self.assertEqual(degrees_to_radians(90.0), math.pi/2.0)
        self.assertEqual(radians_to_degrees(math.pi), 180.0)

        self.assertEqual(
            geo_to_mercator(Point2(30.31759895, 59.95082219)),
            Point2(3374939.6771886, 8351776.2102531))
        self.assertEqual(
            mercator_to_geo(Point2(3374939.6771886, 8351776.2102531)),
            Point2(30.31759895, 59.95082219))

    def test_intersections(self):
        bb1 = BoundingBox(Point2(0, 0), Point2(1, 1))
        bb2 = BoundingBox(Point2(0.5, 0.5), Point2(1.5, 1.5))
        bb3 = BoundingBox(Point2(-1.5, -1.5), Point2(-0.5, -0.5))
        self.assertEqual(intersects(bb1, bb2), True)
        self.assertEqual(intersects(bb1, bb3), False)

    def create_control_points(self):
        control_points = []
        for i in range(0, 5):
            for j in range(0, 5):
                control_points.append(
                    ControlPoint2(Point2(i, j), Point2(i**3, j**3)))
        return control_points

    def test_polynomial(self):
        control_points = self.create_control_points()
        transform3 = PolynomialTransform2(control_points)
        element_transform = SimpleGeometryTransform2(transform3)

        mp = MultiPolygon2.from_WKT(
            "MULTIPOLYGON ((("
            "0.000 0.000, "
            "2.000 0.000, "
            "0.000 2.000, "
            "0.000 0.000)))")

        transformed_mp = element_transform(mp, TransformDirection.Forward)

        expected_mp3 = wkt.loads(
            "MULTIPOLYGON ((("
            "0.000 0.000, "
            "8.000 0.000, "
            "0.000 8.000, "
            "0.000 0.000)))")

        self.assertTrue(expected_mp3.almost_equals(wkt.loads(transformed_mp.to_WKT())))

        transform2 = PolynomialTransform2(control_points, 2)
        element_transform = SimpleGeometryTransform2(transform2)
        transformed_mp = element_transform(mp, TransformDirection.Forward)
        expected_mp2 = wkt.loads(
            "MULTIPOLYGON ((("
            "1.1999999999999533 1.1999999999999880, "
            "7.9999999999999929 1.2000000000000042, "
            "1.2000000000000159 7.9999999999999751, "
            "1.1999999999999533 1.1999999999999880)))")

        self.assertTrue(expected_mp2.almost_equals(wkt.loads(transformed_mp.to_WKT())))

    def test_polynomial_huge(self):
        control_points = [
            ControlPoint2(Point2(c[0], c[1]), Point2(c[2], c[3]))
            for c in [
                [4514456.33380, 6243976.93721, 4514093.25791, 6246537.57766],
                [4510691.81016, 6243518.31504, 4510462.49907, 6245639.44257],
                [4505532.31075, 6242180.66704, 4504557.73864, 6244664.87046],
                [4512621.84512, 6243824.06315, 4512373.42478, 6246442.03137],
                [4516462.80579, 6244072.48349, 4515851.30956, 6240804.80053],
                [4511570.83598, 6245868.75366, 4511819.25632, 6241664.71710],
                [4509048.41405, 6246652.23321, 4509507.03622, 6242620.17996],
                [4507118.37908, 6242601.07071, 4507175.70685, 6245104.38338]
            ]
        ]

        transform = PolynomialTransform2(control_points)
        original = Point2(4514456.3338, 6243976.93721)
        result = transform(original, TransformDirection.Forward)

        MERCATOR_MAX = 20037508.342789244
        self.assertTrue(
            abs(result.x) < MERCATOR_MAX and
            abs(result.y) < MERCATOR_MAX
        )

    def test_spline(self):
        control_points = self.create_control_points()
        tr = SplineTransform2(control_points)
        etr = SimpleGeometryTransform2(tr)

        mp = MultiPolygon2.from_WKT(
            "MULTIPOLYGON ((("
            "0.000 0.000, "
            "2.000 0.000, "
            "0.000 2.000, "
            "0.000 0.000)))")

        transformed_mp = etr(mp, TransformDirection.Forward)
        shapely_transformed = wkb.loads(transformed_mp.to_WKB())

        shapely_expected = wkt.loads(
            "MULTIPOLYGON ((("
            "0.000 0.000, "
            "8.000 0.000, "
            "0.000 8.000, "
            "0.000 0.000)))")

        self.assertTrue(shapely_transformed.almost_equals(shapely_expected))

    def test_affine(self):
        tr = AffineTransform2([10, 100, 3, 100, 10, 3])
        etr = SimpleGeometryTransform2(tr)

        mp = MultiPolygon2.from_WKT(
            "MULTIPOLYGON ((("
            "0.000 0.000, "
            "1.000 0.000, "
            "0.000 1.000, "
            "0.000 0.000)))")

        transformed_mp = etr(mp, TransformDirection.Forward)

        expected_mp = MultiPolygon2.from_WKT(
            "MULTIPOLYGON ((("
            "3.000 3.000, "
            "13.000 103.000, "
            "103.000 13.000, "
            "3.000 3.000)))")

        self.assertEqual(transformed_mp.to_WKB(), expected_mp.to_WKB())

    def test_signed_area(self):
        area = signed_area([
            Point2(0, 0),
            Point2(1, 0),
            Point2(1, 1),
            Point2(0, 1),
            Point2(0, 0)
        ])
        self.assertEqual(area, 1.0)

    def test_markup_orientations(self):
        pts = [
            [
                (0, 0),
                (0, 1),
                (1, 1)
            ],
            [
                (0, 0),
                (1, 0),
                (1, 1)
            ]
        ]
        polylines = []
        for point_set in pts:
            pl = Polyline2()
            for pt in point_set:
                pl.add(Point2(pt[0], pt[1]))
            polylines.append(pl)

        ors = markup_orientations(polylines)
        self.assertEqual(len(ors), 2)
        self.assertEqual(ors[0], Orientation.Clockwise)
        self.assertEqual(ors[1], Orientation.Counterclockwise)


if __name__ == '__main__':
    unittest.main()
