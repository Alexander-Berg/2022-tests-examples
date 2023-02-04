#include <maps/factory/libs/geometry/multipoint.h>

#include <maps/factory/libs/unittest/tests_common.h>

#include <maps/libs/tile/include/tile.h>

namespace maps::factory::geometry::tests {
using namespace maps::factory::tests;

Y_UNIT_TEST_SUITE(multipoint_should) {

Y_UNIT_TEST(cast)
{
    const MultiPoint2d a{{1.2, 3.4}, {5.6, 7.8}};
    const MultiPoint2i b{{1, 3}, {5, 7}};
    const MultiPoint2d c{{1.0, 3.0}, {5.0, 7.0}};
    EXPECT_EQ(a.cast<int>(), b);
    EXPECT_EQ(b.cast<double>(), c);
}

Y_UNIT_TEST(round)
{
    const MultiPoint2d a{{1.2, 3.4}, {5.6, 7.8}};
    const MultiPoint2i b{{1, 3}, {6, 8}};
    const MultiPoint2d c{{1.0, 3.0}, {6.0, 8.0}};
    EXPECT_EQ(a.round<int>(), b);
    EXPECT_EQ(a.round(), c);
}

Y_UNIT_TEST(round_coords)
{
    MultiPoint3d mp{{1.2, 3.4, 8.9}, {5.6, 7.8, 2.3}};
    mp.roundCoords();
    const MultiPoint3d expected{{1, 3, 8.9}, {6, 8, 2.3}};
    EXPECT_EQ(mp, expected);
}

Y_UNIT_TEST(add_and_remove_z)
{
    const MultiPoint2d a{{1.2, 3.4}, {5.6, 7.8}};
    const MultiPoint3d b{{1.2, 3.4, 10.0}, {5.6, 7.8, 10.0}};
    EXPECT_EQ(a.withZ(10), b);
    EXPECT_EQ(b.withoutZ(), a);
}

Y_UNIT_TEST(get_bbox)
{
    const MultiPoint3d a{{1.2, 3.4, 10.0}, {5.6, 7.8, 11.0}, {9.0, 1.2, 12.0}};
    Box2d box(Point2d(1.2, 1.2), Point2d(9.0, 7.8));
    EXPECT_THAT(a.bbox(), EigEq(box));
}

Y_UNIT_TEST(make_geometry_2d)
{
    const MultiPoint2d mp = MultiPoint2d::random(5);
    const Geometry geom = mp.geometry();
    const MultiPoint2d res = MultiPoint2d::fromGeometry(*geom);
    EXPECT_EQ(res, mp);
}

Y_UNIT_TEST(make_geometry_3d)
{
    const MultiPoint3d mp = MultiPoint3d::random(5);
    const Geometry geom = mp.geometry();
    const MultiPoint3d res = MultiPoint3d::fromGeometry(*geom);
    EXPECT_EQ(res, mp);
}

Y_UNIT_TEST(load_empty_from_any_empty_wkb)
{
    const std::string_view emptyPoint =
        "\x01\x01\x00\x00\x00\x00\x00\x00\x00\x00\x00\xf8\x7f\x00\x00\x00\x00\x00\x00\xf8\x7f"sv;
    const std::string_view emptyMultiPoint =
        "\x01\x04\x00\x00\x00\x00\x00\x00\x00"sv;
    const std::string_view emptyPointZ =
        "\x01\xE9\x03\x00\x00\x00\x00\x00\x00\x00\x00\xf8\x7f\x00\x00\x00\x00\x00\x00\xf8\x7f\x00\x00\x00\x00\x00\x00\xf8\x7f"sv;
    const std::string_view emptyMultiPointZ =
        "\x01\xEC\x03\x00\x00\x00\x00\x00\x00"sv;
    const std::string_view emptyCollection =
        "\x01\x07\x00\x00\x00\x00\x00\x00\x00"sv;
    const std::string_view emptyCollectionZ =
        "\x01\xEF\x03\x00\x00\x00\x00\x00\x00"sv;

    EXPECT_THAT(MultiPoint3d::fromWkb(emptyPoint), IsEmpty());
    EXPECT_THAT(MultiPoint3d::fromWkb(emptyMultiPoint), IsEmpty());
    EXPECT_THAT(MultiPoint3d::fromWkb(emptyPointZ), IsEmpty());
    EXPECT_THAT(MultiPoint3d::fromWkb(emptyMultiPointZ), IsEmpty());
    EXPECT_THAT(MultiPoint3d::fromWkb(emptyCollection), IsEmpty());
    EXPECT_THAT(MultiPoint3d::fromWkb(emptyCollectionZ), IsEmpty());

    EXPECT_THAT(MultiPoint2d::fromWkb(emptyPoint), IsEmpty());
    EXPECT_THAT(MultiPoint2d::fromWkb(emptyMultiPoint), IsEmpty());
    EXPECT_THAT(MultiPoint2d::fromWkb(emptyPointZ), IsEmpty());
    EXPECT_THAT(MultiPoint2d::fromWkb(emptyMultiPointZ), IsEmpty());
    EXPECT_THAT(MultiPoint2d::fromWkb(emptyCollection), IsEmpty());
    EXPECT_THAT(MultiPoint2d::fromWkb(emptyCollectionZ), IsEmpty());
}

Y_UNIT_TEST(save_to_wkb_3d)
{
    const MultiPoint3d mp = MultiPoint3d::random(5);
    const std::string wkb = mp.wkbString();
    const std::string expected = mp.geometry().wkbString();
    EXPECT_EQ(wkb, expected);
}

Y_UNIT_TEST(save_empty_to_wkb_3d)
{
    const MultiPoint3d mp;
    const std::string wkb = mp.wkbString();
    const std::string expected = mp.geometry().wkbString();
    EXPECT_EQ(wkb, expected);
}

Y_UNIT_TEST(load_from_wkb_3d)
{
    const MultiPoint3d mp = MultiPoint3d::random(5);
    const std::string wkb = mp.geometry().wkbString();
    const MultiPoint3d result = MultiPoint3d::fromWkb(wkb);
    EXPECT_EQ(result, mp);
}

Y_UNIT_TEST(load_from_wkb_3d_to_2d)
{
    const MultiPoint3d mp = MultiPoint3d::random(5);
    const std::string wkb = mp.geometry().wkbString();
    const MultiPoint2d result = MultiPoint2d::fromWkb(wkb);
    EXPECT_EQ(result, mp.withoutZ());
}

Y_UNIT_TEST(save_to_wkb_2d)
{
    const MultiPoint2d mp = MultiPoint2d::random(5);
    const std::string wkb = mp.wkbString();
    const std::string expected = mp.geometry().wkbString();
    EXPECT_EQ(wkb, expected);
}

Y_UNIT_TEST(load_from_wkb_2d)
{
    const MultiPoint2d mp = MultiPoint2d::random(5);
    const std::string wkb = mp.geometry().wkbString();
    const MultiPoint2d result = MultiPoint2d::fromWkb(wkb);
    EXPECT_EQ(result, mp);
}

Y_UNIT_TEST(load_from_wkb_2d_to_3d)
{
    const MultiPoint2d mp = MultiPoint2d::random(5);
    const std::string wkb = mp.geometry().wkbString();
    const MultiPoint3d result = MultiPoint3d::fromWkb(wkb);
    EXPECT_EQ(result, mp.withZ(0));
}

Y_UNIT_TEST(save_to_wkb_3i)
{
    const MultiPoint3i mp = MultiPoint3i::random(5);
    const std::string wkb = mp.wkbString();
    const std::string expected = mp.geometry().wkbString();
    EXPECT_EQ(wkb, expected);
}

Y_UNIT_TEST(load_from_wkb_3i)
{
    const MultiPoint3i mp = MultiPoint3i::random(5);
    const std::string wkb = mp.geometry().wkbString();
    const MultiPoint3i result = MultiPoint3i::fromWkb(wkb);
    EXPECT_EQ(result, mp);
}

Y_UNIT_TEST(save_to_wkt_3d)
{
    const MultiPoint3d mp{{1.2, 3.4, 5.6}, {7.8, -9.01, -0.1}, {1000000.01, -0.0000001, 1234.56789}};
    const std::string wkt = mp.wkt();
    const std::string expected = mp.geometry().wkt();
    EXPECT_EQ(wkt, expected);
}

Y_UNIT_TEST(save_one_to_wkt_3d)
{
    const MultiPoint3d mp{{0, 0, 0}};
    const std::string wkt = mp.wkt();
    const std::string expected = mp.geometry().wkt();
    EXPECT_EQ(wkt, expected);
}

Y_UNIT_TEST(save_empty_to_wkt_3d)
{
    const MultiPoint3d mp;
    const std::string wkt = mp.wkt();
    const std::string expected = mp.geometry().wkt();
    EXPECT_EQ(wkt, expected);
}

Y_UNIT_TEST(load_from_wkt_3d)
{
    const MultiPoint3d mp{{1.2, 3.4, 5.6}, {7.8, -9, -0.1}, {1000000, -0.0000001, 1234.56789}};
    const std::string wkt = mp.geometry().wkt();
    const MultiPoint3d result = MultiPoint3d::fromWkt(wkt);
    EXPECT_THAT(result, EigEq(mp, 1e-10));
}

Y_UNIT_TEST(load_one_from_wkt_3d)
{
    const MultiPoint3d mp{{0, 0, 0}};
    const std::string wkt = mp.geometry().wkt();
    const MultiPoint3d result = MultiPoint3d::fromWkt(wkt);
    EXPECT_EQ(result, mp);
}

Y_UNIT_TEST(load_empty_from_wkt_3d)
{
    const MultiPoint3d mp;
    const std::string wkt = mp.geometry().wkt();
    const MultiPoint3d result = MultiPoint3d::fromWkt(wkt);
    EXPECT_THAT(result, EigEq(mp, 1e-10));
}

Y_UNIT_TEST(save_to_wkt_2d)
{
    const MultiPoint2d mp{{1.2, 3.4}, {7.8, -9.01}, {1000000.01, -0.0000001}};
    const std::string wkt = mp.wkt();
    const std::string expected = mp.geometry().wkt();
    EXPECT_EQ(wkt, expected);
}

Y_UNIT_TEST(load_from_wkt_2d)
{
    const MultiPoint2d mp = MultiPoint2d::random(5);
    const std::string wkt = mp.geometry().wkt();
    const MultiPoint2d result = MultiPoint2d::fromWkt(wkt);
    EXPECT_THAT(result, EigEq(mp, 1e-10));
}

Y_UNIT_TEST(save_to_wkt_3i)
{
    const MultiPoint3i mp = MultiPoint3i::random(5);
    const std::string wkt = mp.wkt();
    const std::string expected = mp.geometry().wkt();
    EXPECT_EQ(wkt, expected);
}

Y_UNIT_TEST(load_from_wkt_3i)
{
    const MultiPoint3i mp = MultiPoint3i::random(5);
    const std::string wkt = mp.geometry().wkt();
    const MultiPoint3i result = MultiPoint3i::fromWkt(wkt);
    EXPECT_EQ(result, mp);
}

Y_UNIT_TEST(unite)
{
    const auto unite = [](const MultiPoint3d& mp, const MultiPoint3d& top) { return top | mp; };
    const Point3d a(1, 2, 3);
    const Point3d b(1, 2, 30);
    const Point3d c(2, 2, 100);
    EXPECT_THAT(unite({}, {}), IsEmpty());
    EXPECT_THAT(unite({a}, {a}), UnorderedElementsAre(a));
    EXPECT_THAT(unite({a}, {}), UnorderedElementsAre(a));
    EXPECT_THAT(unite({}, {a}), UnorderedElementsAre(a));
    EXPECT_THAT(unite({a}, {b}), UnorderedElementsAre(b));
    EXPECT_THAT(unite({b}, {a}), UnorderedElementsAre(a));
    EXPECT_THAT(unite({a, c}, {b, c}), UnorderedElementsAre(b, c));
    EXPECT_THAT(unite({a, c}, {b}), UnorderedElementsAre(b, c));
    EXPECT_THAT(unite({c}, {a}), UnorderedElementsAre(c, a));
    EXPECT_THAT(unite({b}, {c}), UnorderedElementsAre(b, c));
}

Y_UNIT_TEST(print_to_stream)
{
    std::stringstream ss;
    const MultiPoint3d mp{{1.2, 2.3, 3.4}};
    ss << mp;
    EXPECT_EQ(ss.str(), "MULTIPOINT (1.2 2.3 3.4)");
}

Y_UNIT_TEST(identity_affine_transform)
{
    const MultiPoint3d mp = MultiPoint3d::random(5);
    const Affine2d tr = Affine2d::Identity();
    const MultiPoint3d result = tr * mp;
    EXPECT_THAT(result, EigEq(mp, 1e-10));
}

Y_UNIT_TEST(identity_coordinate_transform)
{
    const MultiPoint3d mp = MultiPoint3d::random(5);
    const CoordinateTransformation tr{};
    const MultiPoint3d result = tr * mp;
    EXPECT_THAT(result, EigEq(mp, 1e-10));
}

Y_UNIT_TEST(add)
{
    const MultiPoint3d mp{{1, 2, 3}};
    const Point3d p{10, 20, 30};
    const MultiPoint3d expected{{11, 22, 33}};
    MultiPoint3d result = mp;
    result += p;
    EXPECT_THAT(result, EigEq(expected, 1e-10));
    EXPECT_THAT(mp + p, EigEq(expected, 1e-10));
    EXPECT_THAT(p + mp, EigEq(expected, 1e-10));
}

Y_UNIT_TEST(multiply)
{
    const MultiPoint3d mp{{1, 2, 3}};
    const Point3d p{10, 20, 30};
    const MultiPoint3d expected{{10, 40, 90}};
    MultiPoint3d result = mp;
    result *= p;
    EXPECT_THAT(result, EigEq(expected, 1e-10));
    EXPECT_THAT(mp * p, EigEq(expected, 1e-10));
    EXPECT_THAT(p * mp, EigEq(expected, 1e-10));
}

Y_UNIT_TEST(filter_intersected)
{
    const Box2d box{Point2d{0, 0}, Point2d{1, 1}};
    const MultiPoint3d points{{0, 0, 1}, {1, 1, 2}, {2, 1, 3}, {1, 2, 4}, {0.5, 0.5, 5}};
    const MultiPoint3d result = points.intersected(box);
    const MultiPoint3d expected{{0, 0, 1}, {1, 1, 2}, {0.5, 0.5, 5}};
    EXPECT_EQ(result, expected);
}

Y_UNIT_TEST(filter_disjoint)
{
    const Box2d box{Point2d{0, 0}, Point2d{1, 1}};
    const MultiPoint3d points{{0, 0, 1}, {1, 1, 2}, {2, 1, 3}, {1, 2, 4}, {0.5, 0.5, 5}};
    const MultiPoint3d result = points.disjointed(box);
    const MultiPoint3d expected{{2, 1, 3}, {1, 2, 4}};
    EXPECT_EQ(result, expected);
}

Y_UNIT_TEST(append)
{
    MultiPoint2d mp{{0, 1}, {0, 2}};
    mp.append(MultiPoint2d{{0, 3}, {0, 4}});
    const MultiPoint2d expected{{0, 1}, {0, 2}, {0, 3}, {0, 4}};
    EXPECT_EQ(mp, expected);
}

} // suite
} // namespace maps::factory::geometry::tests
