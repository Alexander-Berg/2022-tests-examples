#include <maps/factory/libs/geometry/geodesic.h>
#include <maps/factory/libs/geometry/geolib.h>
#include <maps/factory/libs/geometry/line.h>

#include <maps/factory/libs/common/hash.h>

#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/geolib/include/const.h>
#include <maps/libs/geolib/include/conversion.h>
#include <maps/libs/geolib/include/multipolygon.h>
#include <maps/libs/geolib/include/polygon.h>
#include <maps/libs/geolib/include/serialization.h>

#include <maps/factory/libs/unittest/tests_common.h>

#include <contrib/libs/gdal/ogr/ogr_geometry.h>

namespace maps::factory::geometry::tests {
using namespace maps::factory::tests;

Y_UNIT_TEST_SUITE(geometry_should) {

Geometry testPolygon(double dx = 0)
{
    OGRLinearRing ring;
    ring.addPoint(0 + dx, 0);
    ring.addPoint(1 + dx, 0);
    ring.addPoint(0 + dx, 1);
    ring.addPoint(0 + dx, 0); // Geolib adds closing point.
    TypedGeometry<OGRPolygon> geom;
    geom->addRing(&ring);
    return std::move(geom);
}

Geometry testMercatorMultiPolygon()
{
    // Prod mosaic source id 20164140
    const auto wkt =
        "MULTIPOLYGON(((2867929.39141203 -2796489.1681162,2877237.22803811 -2796708.0530086,2877237.28870325 -2796708.05397494,2877388.88565312 -2796709.31885416,2877389.39327315 -2796709.29078018,2877389.89323213 -2796709.19854541,2877390.37745366 -2796709.04363981,2877390.83811556 -2796708.82856575,2877504.65596847 -2796646.59119183,2884982.83717956 -2796898.08752749,2885025.09472 -2824465.86475184,2877278.85023224 -2823969.48709361,2877280.07162805 -2823824.38466731,2877280.04401392 -2823823.88059853,2877279.95312992 -2823823.38402227,2877279.80042364 -2823822.90284801,2877279.58832739 -2823822.44473988,2877279.32021944 -2823822.01699463,2877279.00037021 -2823821.62642537,2877278.63387425 -2823821.27925309,2877278.22656911 -2823820.98100755,2877277.78494234 -2823820.73643921,2877277.31602818 -2823820.54944354,2877276.82729547 -2823820.42299901,2877276.32652876 -2823820.35911962,2877275.4445791 -2823820.30283419,2877275.34170813 -2823820.22006,2877274.87423545 -2823819.93913635,2877274.37289402 -2823819.72441424,2877273.84700407 -2823819.57988549,2877273.30634217 -2823819.50823696,2874107.36644342 -2823618.11823808,2867853.03589426 -2823218.97110789,2867900.27932513 -2806501.2398371,2867929.39141203 -2796489.1681162)))";
    return Geometry::fromWkt(wkt).withSpatialReference(mercatorSr());
}

Geometry testMercatorPolygon()
{
    // Prod mosaic source id 20164838
    const auto wkt =
        "POLYGON((5363223.68823801 8339937.71971692,5385537.219534 8339944.36512319,5385538.74022289 8326692.99131625,5385538.22375969 8319112.0253373,5371243.57044706 8319109.32002051,5363227.29700524 8319109.22404944,5363224.90967184 8327596.8117626,5363223.68823801 8339937.71971692))";
    return Geometry::fromWkt(wkt).withSpatialReference(mercatorSr());
}

geolib3::Polygon2 testGeolibPolygon()
{
    geolib3::PointsVector ring;
    ring.emplace_back(0, 0);
    ring.emplace_back(1, 0);
    ring.emplace_back(0, 1);
    return geolib3::Polygon2{ring};
}

Y_UNIT_TEST(export_geometry_to_wkt)
{
    auto geom = testPolygon();
    EXPECT_EQ(geom.wkt(), "POLYGON ((0 0,1 0,0 1,0 0))");
    EXPECT_EQ(Geometry::fromWkt(geom.wkt()).wkt(), "POLYGON ((0 0,1 0,0 1,0 0))");
}

Y_UNIT_TEST(export_geometry_to_wkb)
{
    auto geom = testPolygon();
    auto expected = geolib3::WKB::toBytes(testGeolibPolygon());
    EXPECT_EQ(geom.wkb(), expected);
    EXPECT_EQ(Geometry::fromWkb(geom.wkb()).wkb(), expected);
}

Y_UNIT_TEST(export_geometry_to_json)
{
    auto geom = testPolygon();
    const auto json =
        R"({ "type": "Polygon", "coordinates": [ [ [ 0.0, 0.0 ], [ 1.0, 0.0 ], [ 0.0, 1.0 ], [ 0.0, 0.0 ] ] ] })";
    EXPECT_EQ(geom.geoJson(), json);
    EXPECT_EQ(Geometry::fromGeoJson(json), geom);
}

Y_UNIT_TEST(union_polygons)
{
    auto geom = testPolygon().unionWith(*testPolygon(0.5));
    EXPECT_EQ(geom.wkt(), "POLYGON ((0.5 0.0,0 0,0 1,0.5 0.5,0.5 1.0,1.5 0.0,1 0,0.5 0.0))");
}

Y_UNIT_TEST(cascaded_union_polygons)
{
    auto geom = Geometry::cascadedUnion({testPolygon(), testPolygon(0.5)});
    EXPECT_EQ(geom.wkt(), "POLYGON ((0.5 0.0,0 0,0 1,0.5 0.5,0.5 1.0,1.5 0.0,1 0,0.5 0.0))");
}

Y_UNIT_TEST(cascaded_union_not_overlaping_polygons)
{
    auto geom = Geometry::cascadedUnion({testPolygon(5), testPolygon()});
    EXPECT_EQ(geom.wkt(), "MULTIPOLYGON (((5 0,6 0,5 1,5 0)),((0 0,1 0,0 1,0 0)))");
}

Y_UNIT_TEST(intersection_polygons)
{
    auto geom = testPolygon().intersectionWith(*testPolygon(0.5));
    EXPECT_EQ(geom.wkt(), "POLYGON ((1 0,0.5 0.0,0.5 0.5,1 0))");
}

Y_UNIT_TEST(convert_to_multipolygon)
{
    auto convert = [](const std::string& wkt) { return toMultiPolygon(Geometry::fromWkt(wkt)).wkt(); };
    EXPECT_EQ(convert("MULTIPOLYGON EMPTY"), "MULTIPOLYGON EMPTY");
    EXPECT_EQ(convert("POLYGON ((0 0,1 0,0 1,0 0))"),
        "MULTIPOLYGON (((0 0,1 0,0 1,0 0)))");
    EXPECT_EQ(convert("MULTIPOLYGON (((5 0,6 0,5 1,5 0)),((0 0,1 0,0 1,0 0)))"),
        "MULTIPOLYGON (((5 0,6 0,5 1,5 0)),((0 0,1 0,0 1,0 0)))");
    EXPECT_EQ(convert("GEOMETRYCOLLECTION (POLYGON ((0 0,1 0,0 1,0 0)),MULTIPOLYGON (((5 0,6 0,5 1,5 0))))"),
        "MULTIPOLYGON (((0 0,1 0,0 1,0 0)),((5 0,6 0,5 1,5 0)))");
    EXPECT_THROW(convert("POINT (30 10)"), RuntimeError);
}

Y_UNIT_TEST(convert_to_polygon)
{
    auto convert = [](const std::string& wkt) { return toPolygon(Geometry::fromWkt(wkt)).wkt(); };
    EXPECT_EQ(convert("POLYGON ((0 0,1 0,0 1,0 0))"), "POLYGON ((0 0,1 0,0 1,0 0))");
    EXPECT_THROW(convert("MULTIPOLYGON (((0 0,1 0,0 1,0 0)))"), RuntimeError);
    EXPECT_THROW(convert("MULTIPOLYGON (((5 0,6 0,5 1,5 0)),((0 0,1 0,0 1,0 0)))"), RuntimeError);
    EXPECT_THROW(convert("GEOMETRYCOLLECTION (POLYGON ((0 0,1 0,0 1,0 0)))"), RuntimeError);
    EXPECT_THROW(convert("POINT (30 10)"), RuntimeError);
}

Y_UNIT_TEST(check_type)
{
    {
        const auto geom = Geometry::fromWkt("POLYGON ((0 0,1 0,0 1,0 0))");
        EXPECT_TRUE(geom.isPolygon());
        EXPECT_FALSE(geom.isMultiPolygon());
        EXPECT_FALSE(geom.isPoint());
        EXPECT_FALSE(geom.isCollection());
        EXPECT_TRUE(geom.isSurface());
    }
    {
        const auto geom = Geometry::fromWkt("MULTIPOLYGON (((5 0,6 0,5 1,5 0)),((0 0,1 0,0 1,0 0)))");
        EXPECT_FALSE(geom.isPolygon());
        EXPECT_TRUE(geom.isMultiPolygon());
        EXPECT_FALSE(geom.isPoint());
        EXPECT_TRUE(geom.isCollection());
        EXPECT_FALSE(geom.isSurface());
    }
    {
        const auto geom = Geometry::fromWkt("POINT (30 10)");
        EXPECT_FALSE(geom.isPolygon());
        EXPECT_FALSE(geom.isMultiPolygon());
        EXPECT_TRUE(geom.isPoint());
        EXPECT_FALSE(geom.isCollection());
        EXPECT_FALSE(geom.isSurface());
    }
    {
        const auto geom = Geometry::fromWkt("GEOMETRYCOLLECTION (POLYGON ((0 0,1 0,0 1,0 0)))");
        EXPECT_FALSE(geom.isPolygon());
        EXPECT_FALSE(geom.isMultiPolygon());
        EXPECT_FALSE(geom.isPoint());
        EXPECT_TRUE(geom.isCollection());
        EXPECT_FALSE(geom.isSurface());
    }
}

Y_UNIT_TEST(convert_polygon_to_geolib)
{
    constexpr auto expectedMp =
        "MULTIPOLYGON ((("
        "0.0000000000000000 0.0000000000000000, "
        "1.0000000000000000 0.0000000000000000, "
        "0.0000000000000000 1.0000000000000000, "
        "0.0000000000000000 0.0000000000000000"
        ")))";
    constexpr auto expectedP =
        "POLYGON (("
        "0.0000000000000000 0.0000000000000000, "
        "1.0000000000000000 0.0000000000000000, "
        "0.0000000000000000 1.0000000000000000, "
        "0.0000000000000000 0.0000000000000000"
        "))";
    const Geometry poly = testPolygon();

    EXPECT_EQ(geolib3::WKT::toString(toGeolibMultiPolygon(poly)), expectedMp);
    EXPECT_EQ(geolib3::WKT::toString(toGeolibPolygon(poly)), expectedP);
    EXPECT_EQ(geolib3::WKT::toString(toGeolib<geolib3::MultiPolygon2>(poly)), expectedMp);
    EXPECT_EQ(geolib3::WKT::toString(toGeolib<geolib3::Polygon2>(poly)), expectedP);
    const geolib3::MultiPolygon2 mp = GeomConvert(poly);
    const geolib3::Polygon2 p = GeomConvert(poly);
    EXPECT_EQ(geolib3::WKT::toString(mp), expectedMp);
    EXPECT_EQ(geolib3::WKT::toString(p), expectedP);
}

Y_UNIT_TEST(convert_geolib_to_geometry)
{
    constexpr auto expectedMp = "MULTIPOLYGON (((0 0,1 0,0 1,0 0)))";
    constexpr auto expectedP = "POLYGON ((0 0,1 0,0 1,0 0))";
    const auto p = testGeolibPolygon();
    geolib3::MultiPolygon2 mp{{p}};

    EXPECT_EQ(toGeom(mp).wkt(), expectedMp);
    EXPECT_EQ(toGeom(p).wkt(), expectedP);

    const Geometry gmp = GeomConvert(mp);
    const Geometry gp = GeomConvert(p);
    EXPECT_EQ(gmp.wkt(), expectedMp);
    EXPECT_EQ(gp.wkt(), expectedP);
}

Y_UNIT_TEST(export_bbox_to_geolib_polygon)
{
    const Box2d box{Array2d{10, 20}, Array2d{30, 40}};
    const auto poly = toGeolibPolygon(box);
    EXPECT_DOUBLE_EQ(poly.area(), box.volume());
    EXPECT_EQ(geolib3::WKT::toString(poly),
        "POLYGON (("
        "10.0000000000000000 20.0000000000000000, "
        "30.0000000000000000 20.0000000000000000, "
        "30.0000000000000000 40.0000000000000000, "
        "10.0000000000000000 40.0000000000000000, "
        "10.0000000000000000 20.0000000000000000"
        "))");
}

Y_UNIT_TEST(prepare_geometry)
{
    const Box2d box{Array2d{0, 0}, Array2d{10, 20}};
    const auto poly = makePolygonGeometry(box);
    const auto prep = prepare(poly);
    const Box2d boxes[]{
        {Array2d{0, 0}, Array2d{10, 20}},
        {Array2d{20, 0}, Array2d{30, 20}},
        {Array2d{2, 2}, Array2d{8, 18}},
        {Array2d{-10, -10}, Array2d{20, 30}},
    };
    for (auto& other: boxes) {
        const auto otherGeom = toGeolibPolygon(other);
        EXPECT_EQ(spatialRelation(prep, otherGeom, geolib3::SpatialRelation::Contains),
            box.contains(other));
        EXPECT_EQ(spatialRelation(prep, otherGeom, geolib3::SpatialRelation::Intersects),
            box.intersects(other));
    }
}

Y_UNIT_TEST(calculate_area)
{
    EXPECT_NEAR(testPolygon().area(), 0.5, 1e-5);
    EXPECT_NEAR(testMercatorMultiPolygon().area(), 464771018.69496918, 1e-2);
    EXPECT_NEAR(testMercatorPolygon().area(), 464801952.73246765, 1e-2);
    EXPECT_NEAR(testMercatorMultiPolygon().transformedTo(geodeticSr()).area(), 0.03428838795259768, 1e-6);
    EXPECT_NEAR(testMercatorPolygon().transformedTo(geodeticSr()).area(), 0.018870844852603819, 1e-6);
}

Y_UNIT_TEST(calculate_geodesic_area)
{
    const double r = geolib3::WGS84_MAJOR_SEMIAXIS;
    const double dlon = 1.0, lat = 55.0, dlat = 2.0, toRad = M_PI / 180;
    const double approxArea = (r * std::sin((90 - lat) * toRad) * dlon * toRad) * (r * dlat * toRad);
    constexpr auto poly1 = "POLYGON((37 56,38 56,38 54,37 54,37 56))";
    constexpr auto poly2 = "POLYGON((67 56,68 56,68 54,67 54,67 56))"; // Shift longitude

    EXPECT_NEAR(geodesicArea(Geometry::fromWkt(poly1).withSpatialReference(geodeticSr())),
        geodesicArea(Geometry::fromWkt(poly2).withSpatialReference(geodeticSr())), 0.01);

    EXPECT_NEAR(geodesicArea(Geometry::fromWkt(poly1).withSpatialReference(geodeticSr())),
        approxArea, approxArea / 10);

    EXPECT_NEAR(geodesicArea(testMercatorMultiPolygon().transformedTo(geodeticSr())),
        384606213.69917637, 0.01);

    EXPECT_NEAR(geodesicArea(testMercatorPolygon().transformedTo(geodeticSr())),
        117864879.607492, 0.01);
}

Y_UNIT_TEST(transform_coordinates)
{
    auto p = testMercatorPolygon();
    auto geoP = p.transformedTo(geodeticSr());
    EXPECT_EQ(geoP.wkt(),
        "POLYGON ((48.1786581129607 59.8974365999422,48.3791039750194 59.8974665915351,48.3791176356001 59.8376073824392,48.3791129961323 59.8033139482011,48.250701940613 59.8033017040449,48.1786905310683 59.8033012696836,48.1786690852875 59.8416935631191,48.1786581129607 59.8974365999422))");
}

Y_UNIT_TEST(transform_geo_point_to_mercator)
{
    const Vector2d pointGeo(48.1786581129607, 59.8974365999408);
    const Vector2d expected = toVector2d(geolib3::geoPoint2Mercator(toGeolibPoint(pointGeo)));
    const CoordinateTransformation geoToMer(geodeticSr(), mercatorSr());
    const Vector2d result = geoToMer * pointGeo;
    EXPECT_THAT(result, EigEq(expected, 1e-9));
}

Y_UNIT_TEST(transform_mercator_point_to_geo)
{
    const Vector2d pointMer(5363223.68823801, 8339937.71971692);
    const Vector2d expected = toVector2d(geolib3::mercator2GeoPoint(toGeolibPoint(pointMer)));
    const CoordinateTransformation merToGeo(mercatorSr(), geodeticSr());
    const Vector2d result = merToGeo * pointMer;
    EXPECT_THAT(result, EigEq(expected, 1e-3));
}

Y_UNIT_TEST(create_polygon_from_box)
{
    const Box2d box{Array2d{10, 20}, Array2d{30, 40}};
    const auto poly = makePolygonGeometry(box);
    EXPECT_DOUBLE_EQ(poly.area(), box.volume());
    EXPECT_EQ(poly.wkt(),
        "POLYGON ((10 20,30 20,30 40,10 40,10 20))");
}

Y_UNIT_TEST(iterate_over_points)
{
    const Box2d box{Array2d{10, 20}, Array2d{30, 40}};
    auto poly = makePolygonGeometry(box);
    int count = 0;
    poly.forEachPoint([&](int n, double*, double*) { count += n; });
    EXPECT_EQ(count, 5);
    count = 0;
    poly.segmentize(1);
    poly.forEachPoint([&](int n, double*, double*) { count += n; });
    EXPECT_EQ(count, 81);
}

Y_UNIT_TEST(copy_spatial_ref)
{
    SpatialRef sr{MERCATOR_WKT};
    {
        SpatialRef srCopy;
        srCopy = sr;
        SpatialRef srCopy2(sr);
        EXPECT_EQ(srCopy, sr);
        EXPECT_EQ(srCopy2, sr);
    }
    EXPECT_EQ(sr, mercatorSr());
}

Y_UNIT_TEST(similarity)
{
    auto p = testPolygon(0);
    EXPECT_DOUBLE_EQ(p.similarity(*p), 1.0);
    EXPECT_DOUBLE_EQ(p.similarity(*testPolygon(0.5)), 0.125 / (1 - 0.125));
    EXPECT_DOUBLE_EQ(p.similarity(*testPolygon(1)), 0.0);
    EXPECT_DOUBLE_EQ(p.similarity(*testPolygon(100)), 0.0);
}

Y_UNIT_TEST(intersection_area)
{
    auto p = testPolygon(0);
    EXPECT_DOUBLE_EQ(p.intersectionArea(*p), 0.5);
    EXPECT_LT(p.intersectionArea(*testPolygon(0.5)), 0.5);
    EXPECT_GT(p.intersectionArea(*testPolygon(0.5)), 0.0);
    EXPECT_DOUBLE_EQ(p.intersectionArea(*testPolygon(1)), 0.0);
    EXPECT_DOUBLE_EQ(p.intersectionArea(*testPolygon(100)), 0.0);
}

Y_UNIT_TEST(affine_transform_points)
{
    {
        Affine2d affine = Affine2d::Identity();
        affine.matrix() <<
            2, 0, 3,
            0, 4, 5,
            0, 0, 1;
        auto polygon = testPolygon();
        polygon.transform(*CoordinateTransformation(affine));
        EXPECT_EQ(polygon.wkt(), "POLYGON ((3 5,5 5,3 9,3 5))"); // Was POLYGON ((0 0,1 0,0 1,0 0))
    }
    {
        Affine2d affine = Affine2d::Identity();
        affine.matrix() <<
            0, 10, 0,
            20, 0, 0,
            0, 0, 1;
        auto polygon = testPolygon();
        polygon.transform(*CoordinateTransformation(affine));
        EXPECT_EQ(polygon.wkt(), "POLYGON ((0 0,0 20,10 0,0 0))"); // Was POLYGON ((0 0,1 0,0 1,0 0))
    }
}

Y_UNIT_TEST(get_points_count)
{
    EXPECT_EQ(testPolygon().pointsCount(), 4u);
    EXPECT_EQ(testMercatorMultiPolygon().pointsCount(), 35u);
    EXPECT_EQ(testMercatorPolygon().pointsCount(), 8u);
}

Y_UNIT_TEST(simplify)
{
    const auto p1 = testPolygon();
    const auto p2 = testMercatorMultiPolygon();
    const auto p3 = testMercatorPolygon();

    EXPECT_EQ(p1.simplified(0), p1);
    EXPECT_EQ(p1.simplified(0.1), p1);

    EXPECT_NEAR(p2.simplified(0).area(), p2.area(), 0);
    EXPECT_NEAR(p2.simplified(0.01).area(), p2.area(), 0.05);
    EXPECT_NEAR(p2.simplified(0.1).area(), p2.area(), 0.5);

    EXPECT_EQ(toMultiPolygon(p2.simplified(0.1)),
        toGeom(simplify(toGeolibMultiPolygon(p2), 0.1, geolib3::Validate::Yes)));
    EXPECT_EQ(toMultiPolygon(p2.simplified(0.5)),
        toGeom(simplify(toGeolibMultiPolygon(p2), 0.5, geolib3::Validate::Yes)));

    EXPECT_NEAR(p3.simplified(0).area(), p3.area(), 0);
    EXPECT_NEAR(p3.simplified(0.1).area(), p3.area(), 0);
    EXPECT_NEAR(p3.simplified(0.5).area(), p3.area(), 0);

}

Y_UNIT_TEST(create_line)
{
    const Line2d line2d(Vector2d::Random(), Vector2d::Random());
    const Line3d line3d(Vector3d::Random(), Vector3d::Random());
    const Geometry line2dGeom = makeLineGeometry(line2d);
    const Geometry line3dGeom = makeLineGeometry(line3d);
    EXPECT_TRUE(line2d.isApprox(line2dFromGeometry(*line2dGeom)));
    EXPECT_TRUE(line3d.isApprox(line3dFromGeometry(*line3dGeom)));
}

Y_UNIT_TEST(create_point)
{
    const Point2d point2d(Vector2d::Random());
    const Point3d point3d(Vector3d::Random());
    const Geometry point2dGeom = makePointGeometry(point2d);
    const Geometry point3dGeom = makePointGeometry(point3d);
    EXPECT_TRUE(point2d.isApprox(point2dFromGeometry(*point2dGeom)));
    EXPECT_TRUE(point3d.isApprox(point3dFromGeometry(*point3dGeom)));
}

Y_UNIT_TEST(calculate_hash)
{
    const auto p1 = testPolygon();
    const auto p2 = testMercatorMultiPolygon();
    const auto p3 = testMercatorPolygon();
    const auto p4 = testPolygon();
    Hash128 h1{}, h2{}, h3{}, h4{};
    p1.hash(h1);
    p2.hash(h2);
    p3.hash(h3);
    p4.hash(h4);
    EXPECT_EQ(h1, h4);
    EXPECT_NE(h1, h2);
    EXPECT_NE(h1, h3);
    EXPECT_NE(h2, h3);
}

Y_UNIT_TEST(iterate_multi_point_z)
{
    std::vector<Vector3d> points;
    Geometry geom = Geometry::fromWkt("MULTIPOINT (1 2 3,4 5 6)");
    geom.forEachPointZ([&](int n, double* x, double* y, double* z) {
        for (int i = 0; i < n; ++i) {
            points.emplace_back(x[i], y[i], z[i]);
        }
    });
    EXPECT_THAT(points, ElementsAre(Vector3d(1, 2, 3), Vector3d(4, 5, 6)));
}

Y_UNIT_TEST(remove_holes_from_empty_polygon)
{
    Geometry geometry = Geometry::fromWkt(
        "POLYGON EMPTY");
    EXPECT_EQ(geometry.withoutHoles(), geometry);
}

Y_UNIT_TEST(remove_holes_from_empty_multipolygon)
{
    Geometry geometry = Geometry::fromWkt(
        "MULTIPOLYGON EMPTY");
    EXPECT_EQ(geometry.withoutHoles(), geometry);
}

Y_UNIT_TEST(remove_holes_from_polygon_without_holes)
{
    Geometry geometry = Geometry::fromWkt(
        "POLYGON ((0 0,1 0,0 1,0 0))");
    EXPECT_EQ(geometry.withoutHoles(), geometry);
}

Y_UNIT_TEST(remove_holes_from_polygon_with_holes)
{
    Geometry geometry = Geometry::fromWkt(
        "POLYGON ((35 10, 10 20, 15 40, 45 45, 35 10),(20 30, 35 35, 30 20, 20 30))");
    Geometry expected = Geometry::fromWkt(
        "POLYGON ((35 10, 10 20, 15 40, 45 45, 35 10))");
    EXPECT_EQ(geometry.withoutHoles(), expected);
}

Y_UNIT_TEST(remove_holes_from_multipolygon_with_holes)
{
    Geometry geometry = Geometry::fromWkt(
        "MULTIPOLYGON (((35 10, 10 20, 15 40, 45 45, 35 10),(20 30, 35 35, 30 20, 20 30)),((0 0,1 0,0 1,0 0)))");
    Geometry expected = Geometry::fromWkt(
        "MULTIPOLYGON (((35 10, 10 20, 15 40, 45 45, 35 10)),((0 0,1 0,0 1,0 0)))");
    EXPECT_EQ(geometry.withoutHoles(), expected);
}

Y_UNIT_TEST(remove_holes_save_spatial_ref)
{
    Geometry poly = Geometry::fromWkt(
        "POLYGON ((0 0,1 0,0 1,0 0))");
    Geometry mPoly = Geometry::fromWkt(
        "MULTIPOLYGON (((0 0,1 0,0 1,0 0)))");
    poly.assignSpatialReference(mercatorSr());
    mPoly.assignSpatialReference(geodeticSr());
    EXPECT_EQ(poly.withoutHoles().spatialReference(), mercatorSr());
    EXPECT_EQ(mPoly.withoutHoles().spatialReference(), geodeticSr());
}

} // suite

} // namespace maps::factory::geometry::tests
