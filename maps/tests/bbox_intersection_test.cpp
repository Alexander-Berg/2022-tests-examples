#include "../geom/linear_ring.h"
#include "../geom/polygon.h"
#include "../geom/multipolygon.h"

#include "../test_tools/io_std.h"
#include "../test_tools/geom_transform.h"

#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/bounding_box.h>
#include <maps/libs/common/include/exception.h>

#include <boost/test/unit_test.hpp>

#include <vector>
#include <algorithm>

using namespace maps::coverage5;
using namespace maps::geolib3;

typedef geom::StandaloneLinearRing LinearRing;
typedef geom::StandalonePolygon Polygon;
typedef geom::StandaloneMultiPolygon MultiPolygon;

struct BBoxTest {
    BoundingBox bbox;
    bool intersects;
};

typedef std::vector<BBoxTest> Tests;

template <class Geom>
struct IntersectionTest {
    Geom geom;
    Tests tests;
};

std::vector< IntersectionTest<LinearRing> > g_linearRingsTests {
    IntersectionTest<LinearRing> {
        LinearRing {{0, 0}, {1, 0}, {1, 1}, {0, 1}},
        Tests {
            {BoundingBox{{0, 0}, {1, 1}}, true},
            {BoundingBox{{0.5, 0.5}, {0.8, 0.8}}, false},
            {BoundingBox{{-0.1, -0.1}, {1.1, 1.1}}, true},
            {BoundingBox{{1.1, 1.1}, {2, 2}}, false},
            {BoundingBox{{-0.1, -1.1}, {1.1, -0.1}}, false},
            {BoundingBox{{-0.1, -0.1}, {0.5, 0.5}}, true},
            {BoundingBox{{0.5, 0.5}, {1.5, 0.9}}, true},
            {BoundingBox{{-0.5, 0.5}, {1.5, 0.9}}, true}
        }
    },
    IntersectionTest<LinearRing> {
        LinearRing {{4, 1}, {6, 3}, {2, 3}, {0, 1}, {-2, 3}, {-2, 1}, {-3, 0},
                    {-5, 0}, {-4, -2}, {-2, -2}, {-1, -4}, {4, -4}, {6, -2}},
        Tests {
             {BoundingBox{{-5, 1}, {-3, 3}}, false},
             {BoundingBox{{-4, -5}, {-2, -3}}, false},
             {BoundingBox{{1, -3}, {4, -1}}, false},
             {BoundingBox{{2, 2}, {4, 4}}, true},
             {BoundingBox{{-5.01, -4.01}, {6.01, 3.01}}, true},
             {BoundingBox{{-4.99, -3.99}, {5.99, 2.99}}, true},
             {BoundingBox{{2, 0}, {6, 2}}, true},
             {BoundingBox{{-3, -3}, {-1, 2}}, true},
             {BoundingBox{{5, -3}, {8, -1}}, true},
             {BoundingBox{{-6, -2}, {-3, 0}}, true},
             {BoundingBox{{2, 1}, {7, 5}}, true},
             {BoundingBox{{-3, 1}, {0, 4}}, true},
             {BoundingBox{{-2, 1}, {0, 4}}, true},
             {BoundingBox{{-6, 1}, {0, 4}}, true},
             {BoundingBox{{-6, -6}, {-3, 0}}, true},
             {BoundingBox{{0, 1}, {6, 6}}, true},
             {BoundingBox{{0, 1}, {6, 8}}, true},
             {BoundingBox{{-6, -2}, {-2, 1}}, true}
        }
    }
};

std::vector< IntersectionTest<Polygon> > g_polygonsTests {
    IntersectionTest<Polygon> {
        Polygon {
            LinearRing {{0, 0}, {1, 0}, {1, 1}, {0, 1}}
        },
        Tests {
            {BoundingBox{{0.5, 0.5}, {0.8, 0.8}}, true},
            {BoundingBox{{-0.1, -0.1}, {1.1, 1.1}}, true},
            {BoundingBox{{0, 0}, {1, 1}}, true},
            {BoundingBox{{0.5, 0.5}, {1.8, 1.8}}, true},
            {BoundingBox{{0.5, 0.5}, {1.8, 0.9}}, true},
            {BoundingBox{{-0.5, 0.5}, {0.5, 0.9}}, true},
            {BoundingBox{{-1, 0.0001}, {0.0001, 0.9999}}, true},
            {BoundingBox{{-1, -0.0001}, {0.0001, 1.0001}}, true}
        }
    },
    IntersectionTest<Polygon> {
        Polygon {
            LinearRing {{0, 0}, {3, 0}, {3, 3}, {0, 3}},
            LinearRing {{1, 1}, {2, 1}, {2, 2}, {1, 2}}
        },
        Tests {
            {BoundingBox{{1.1, 1.1}, {1.9, 1.9}}, false},
            {BoundingBox{{0.9, 0.9}, {2.1, 2.1}}, true},
            {BoundingBox{{0.0001, 0.0001}, {2.9999, 2.9999}}, true},
            {BoundingBox{{0, 0}, {3, 3}}, true},
            {BoundingBox{{-0.0001, -0.0001}, {3.0001, 3.0001}}, true},
            {BoundingBox{{1.5, 1.5}, {2.1, 2.1}}, true},
            {BoundingBox{{0.5, 1.5}, {2.1, 2.1}}, true},
            {BoundingBox{{1.5, 1.5}, {3.1, 3.1}}, true},
            {BoundingBox{{1.5, 1.5}, {3.1, 1.9}}, true}
        }
    },
    IntersectionTest<Polygon> {
        Polygon {
            LinearRing {{4, 2}, {8, 5}, {6, 8}, {1, 10}, {-4, 10}, {-6, 4},
                        {-10, 7}, {-6, 0}, {-10, -1}, {-10, -3}, {-7, -3},
                        {-7, -6}, {3, -6}, {3, -2}, {9, -2}},
            LinearRing {{1, -4}, {0, -1}, {-2, 2}, {-4, 1}, {-3, -1}, {-5, -1}, {-4, -4}},
            LinearRing {{3, 4}, {2, 8}, {0, 9}, {-2, 6}, {-1, 5}, {1, 6}}
        },
        Tests {
            {BoundingBox{{-6, -5}, {2, 3}}, true},
            {BoundingBox{{-2.5, 4.5}, {3.5, 9.5}}, true},
            {BoundingBox{{-4.9999, -3.9999}, {0.9999, 1.9999}}, true},
            {BoundingBox{{-5, -4}, {1, 2}}, true},
            {BoundingBox{{-5.0001, -4.0001}, {1.0001, 2.0001}}, true},
            {BoundingBox{{-4, -3}, {-1, 0}}, true},
            {BoundingBox{{-2.5, -3.5}, {-0.5, -1.5}}, false},

            {BoundingBox{{-2.5, 4.5}, {3.5, 9.5}}, true},
            {BoundingBox{{-2.0001, 5.0001}, {2.9999, 8.9999}}, true},
            {BoundingBox{{-2, 5}, {3, 9}}, true},
            {BoundingBox{{-1.9999, 4.9999}, {3.0001, 9.0001}}, true},
            {BoundingBox{{-0.5, 5.9999}, {1.5, 7.5}}, true},
            {BoundingBox{{-0.5, 6.0001}, {1.5, 7.5}}, false},

            {BoundingBox{{0, 0}, {3, 4}}, true},
            {BoundingBox{{0, 0}, {7, 4}}, true},
            {BoundingBox{{-3, -1}, {2, 8}}, true},
            {BoundingBox{{-4, 4}, {-1, 7}}, true},
            {BoundingBox{{-11, -7}, {10, 11}}, true},
            {BoundingBox{{-10, -6}, {9, 10}}, true},
            {BoundingBox{{-9, -5}, {8, 9}}, true}
        }
    }
};

std::vector< IntersectionTest<MultiPolygon> > g_multiPolygonsTests {
    IntersectionTest<MultiPolygon> {
        MultiPolygon {
            Polygon {
                LinearRing {{-1, 4}, {-4, 6}, {-6, 3}, {-3, 1}}
            },
            Polygon {
                LinearRing {{6, -1}, {6, 3}, {2, 3}, {2, -1}}
            }
        },
        Tests {
            {BoundingBox{{-2, 0}, {1, 2}}, false},
            {BoundingBox{{0, 3.5}, {6, 7}}, false},
            {BoundingBox{{-7, 0}, {0, 7}}, true},
            {BoundingBox{{1, -2}, {7, 4}}, true},
            {BoundingBox{{-8, -2}, {7, 7}}, true},
            {BoundingBox{{-6, -1}, {6, 6}}, true},
            {BoundingBox{{-5, 0}, {5, 6}}, true},
            {BoundingBox{{0, 2}, {6, 7}}, true},
            {BoundingBox{{-3, 0}, {-1, 2}}, true},
            {BoundingBox{{-5, -3}, {-1, 0}}, false},
            {BoundingBox{{8, 3}, {11, 6}}, false}
        }
    }
};

void testLinearRing(const LinearRing& ring,
    const BoundingBox& bbox, bool intersects)
{
    for (size_t shf = 0; shf < ring.pointsNumber(); ++shf) {
        LinearRing shRing(ring);
        shRing.scroll(shf);
        BOOST_CHECK(shRing.intersects(bbox) == intersects);
        shRing.reverse();
        BOOST_CHECK(shRing.intersects(bbox) == intersects);
    }
}

void testPolygon(const Polygon& polygon,
    const BoundingBox& bbox, bool intersects)
{
    BOOST_CHECK(polygon.intersects(bbox) == intersects);
    Polygon normPolygon(polygon);
    normPolygon.normalize();
    BOOST_CHECK(normPolygon.intersects(bbox) == intersects);
}

void testMultiPolygon(const MultiPolygon& mpolygon,
    const BoundingBox& bbox, bool intersects)
{
    BOOST_CHECK(mpolygon.intersects(bbox) == intersects);
    for (size_t i = 1; i < mpolygon.polygonsNumber(); ++ i) {
        MultiPolygon::Polygons polygons(mpolygon.polygons());
        std::rotate(polygons.begin(), polygons.begin() + i, polygons.end());
        MultiPolygon rotated(polygons, geom::Validate::No);
        BOOST_CHECK(rotated.intersects(bbox) == intersects);
    }
}

template <class Geometry>
void testTransformedGeometry(const IntersectionTest<Geometry>& test,
    void (*testGeometry)(const Geometry&, const BoundingBox&, bool),
    const test::Transform& transform)
{
    Geometry transG(test.geom);
    transform(transG);
    for (auto bboxTest: test.tests) {
        BoundingBox trBBox(bboxTest.bbox);
        transform(trBBox);
        testGeometry(transG, trBBox, bboxTest.intersects);
    }
}

template <class Geometry>
void testBBoxes(const IntersectionTest<Geometry>& test,
    void (*testGeometry)(const Geometry&, const BoundingBox&, bool))
{
    const Geometry& g = test.geom;
    cut::CutLine xAxis(geom::X,
        (g.boundingBox().minX() + g.boundingBox().maxX()) / 2.0);
    cut::CutLine yAxis(geom::Y,
        (g.boundingBox().minY() + g.boundingBox().maxY()) / 2.0);
    test::IdentityTransform idTransform(xAxis);
    test::SwitchXYTransform transpose(xAxis);
    test::ReflectionTransform xReflect(xAxis);
    test::ReflectionTransform yReflect(yAxis);
    testTransformedGeometry<Geometry>(test, testGeometry, idTransform);
    testTransformedGeometry<Geometry>(test, testGeometry, transpose);
    testTransformedGeometry<Geometry>(test, testGeometry, xReflect);
    testTransformedGeometry<Geometry>(test, testGeometry, yReflect);
}

BOOST_AUTO_TEST_CASE(ring_bbox_intersection_test)
{
    for (auto test: g_linearRingsTests) {
        testBBoxes<LinearRing>(test, &testLinearRing);
    }
}

BOOST_AUTO_TEST_CASE(polygon_bbox_intersection_test)
{
    for (auto test: g_polygonsTests) {
        testBBoxes<Polygon>(test, &testPolygon);
    }
}

BOOST_AUTO_TEST_CASE(multipolygon_bbox_intersection_test)
{
    for (auto test: g_multiPolygonsTests) {
        testBBoxes<MultiPolygon>(test, &testMultiPolygon);
    }
}

BOOST_AUTO_TEST_CASE(accuracy_polygon_intersection_test)
{
    IntersectionTest<Polygon> accTest;
    accTest.geom = Polygon{LinearRing{{0, 0}, {1, 0}, {1, 1}, {0, 1}}};
    const double minx = accTest.geom.boundingBox().minX();
    const double miny = accTest.geom.boundingBox().minY();
    const double maxx = accTest.geom.boundingBox().maxX();
    const double maxy = accTest.geom.boundingBox().maxY();
    const double eps = 1e-7;
    Tests tests;
    for (int dminx = -2; dminx <= 2; ++dminx) {
        for (int dmaxx = -2; dmaxx <= 2; ++dmaxx) {
            for (int dminy = -2; dminy <= 2; ++dminy) {
                for (int dmaxy = -2; dmaxy <= 2; ++dmaxy) {
                    BBoxTest test;
                    test.bbox = BoundingBox(
                        Point2(minx + dminx * eps, miny + dminy * eps),
                        Point2(maxx + dmaxx * eps, maxy + dmaxy * eps));
                    test.intersects = true;
                    accTest.tests.push_back(test);
                }
            }
        }
    }
    testBBoxes<Polygon>(accTest, &testPolygon);
}
