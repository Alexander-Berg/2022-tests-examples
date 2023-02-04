#include "../visitors.h"
#include "../test_tools/io_std.h"

#include "../tree/search_tree.h"
#include "../tree/cut_tree_builder.h"

#include "../geom/linear_ring.h"
#include "../geom/polygon.h"
#include "../geom/multipolygon.h"

#include <maps/libs/geolib/include/point.h>

#include <yandex/maps/mms/writer.h>
#include <yandex/maps/mms/holder2.h>
#include <yandex/maps/mms/vector.h>

#include <boost/test/unit_test.hpp>

#include <fstream>
#include <map>
#include <initializer_list>

const char* g_mmsTreeFile = "test_tree.mms";

using namespace maps::coverage5;
using namespace maps::geolib3;

typedef geom::StandaloneLinearRing LinearRing;
typedef geom::StandalonePolygon Polygon;
typedef geom::StandaloneMultiPolygon MultiPolygon;

struct PointTreeTest {
    Point2 point;
    std::vector<RegionId> ids;
};

struct BBoxTreeTest {
    BoundingBox bbox;
    std::set<RegionId> ids;
};

typedef tree::CutTreeBuilder<int>::MultiPolygonsMap MultiPolygonIDMap;
typedef std::vector<PointTreeTest> PointTreeTests;
typedef std::vector<BBoxTreeTest> BBoxTreeTests;

struct SimpleTreeTestData {
    double minBoxSize;
    double maxBoxSize;
    size_t maxVertices;
    MultiPolygonIDMap map;
    PointTreeTests pointTests;
    BBoxTreeTests bboxTests;
};

MultiPolygonIDMap g_polygonMap = {
    {
        1,
        MultiPolygon {
            Polygon {
                LinearRing {
                    {3., -1.}, {5., 2.}, {3., 5.}, {2., 3.}, {4., 2.},
                    {2., 1.}
                }
            }
        }
    },
    {
        2,
        MultiPolygon {
            Polygon {
                LinearRing {
                    {-3., -2.}, {-3., -3.}, {-1., -4.}, {3., -4.},
                    {3., 7.}, {-1., 7.}, {-1., 6.}, {0., 6.},
                    {2., 5.}, {-2., 4.}, {-3., 3.}, {-2., 2.},
                    {1., 2.}, {-1., 1.}, {-1., -1.}
                },
                LinearRing {
                    {2, -3}, {0, -3}, {0, -1}, {2, -1}
                }
            }
        }
    }
};

PointTreeTests g_pointTreeTests = {
    PointTreeTest { Point2 {0.0, 0.0}, {2} },
    PointTreeTest { Point2 {2.5, 0.7}, {1, 2} },
    PointTreeTest { Point2 {1.0, 1.499999999}, {2} },
    PointTreeTest { Point2 {4.0, 1.499999999}, {1} },
    PointTreeTest { Point2 {0.0, 3.0}, {2} },
    PointTreeTest { Point2 {-2.0, -3.0}, {2} },
    PointTreeTest { Point2 {-2.0, 3.0}, {2} },
    PointTreeTest { Point2 {2.00001, -3.00001}, {2} },
    PointTreeTest { Point2 {1.5, -2.5}, {} },
    PointTreeTest { Point2 {3.5, 3.5}, {1} },
    PointTreeTest { Point2 {4.5, 2}, {1} },
    PointTreeTest { Point2 {2.5, 3.5}, {1, 2} },
    PointTreeTest { Point2 {2.0, 6.0}, {2} },
    PointTreeTest { Point2 {0.99999, 1.50000001}, {2} },
    PointTreeTest { Point2 {-0.5, 6.5}, {2} },
    PointTreeTest { Point2 {-0.5, -2}, {2} },
    PointTreeTest { Point2 {1, -3.5}, {2} },
    PointTreeTest { Point2 {1, -2}, {} },
    PointTreeTest { Point2 {1, 5}, {} },
    PointTreeTest { Point2 {-1, 5}, {} },
    PointTreeTest { Point2 {-0.5, 1.75}, {} },
    PointTreeTest { Point2 {-3, 1}, {} },
    PointTreeTest { Point2 {3.5, 2}, {} },
    PointTreeTest { Point2 {4, 5}, {} },
    PointTreeTest { Point2 {4, -2}, {} },
    PointTreeTest { Point2 {1, 8}, {} },
    PointTreeTest { Point2 {2, -4.0001}, {} }
};

BBoxTreeTests g_bbox_tree_tests = {
    BBoxTreeTest { BoundingBox {{1, -1}, {4, 2}}, {1, 2} },
    BBoxTreeTest { BoundingBox {{-2, 4}, {2, 6}}, {2} },
    BBoxTreeTest { BoundingBox {{1, -4}, {4, -1.5}}, {2} },
    BBoxTreeTest { BoundingBox {{3.5, 2}, {5, 5}}, {1} },
    BBoxTreeTest { BoundingBox {{0, 1.5}, {2, 2.5}}, {2} },
    BBoxTreeTest { BoundingBox {{-2, -4}, {5, 1.5}}, {1, 2} },
    BBoxTreeTest { BoundingBox {{-1, -5}, {1, 4}}, {2} },
    BBoxTreeTest { BoundingBox {{-0.5, -3.5}, {2.3, -0.7}}, {2} },
    BBoxTreeTest { BoundingBox {{-4, 2}, {-2, 4.25}}, {2} },
    BBoxTreeTest { BoundingBox {{-3, -4}, {5, 7}}, {1, 2} },
    BBoxTreeTest { BoundingBox {{3.5, 2}, {5, 5}}, {1} },
    BBoxTreeTest { BoundingBox {{-1, 4.7}, {0, 5.5}}, {} },
    BBoxTreeTest { BoundingBox {{3.1, 6}, {5, 7}}, {} },
    BBoxTreeTest { BoundingBox {{4, -4}, {6, 0}}, {} },
    BBoxTreeTest { BoundingBox {{-4, -1}, {-1.5, 1}}, {} },
    BBoxTreeTest { BoundingBox {{0.5, -2.5}, {1.5, -1.5}}, {} }
};

SimpleTreeTestData g_simpleTestData[] {
    SimpleTreeTestData {
        3.0,
        360.0,
        4,
        g_polygonMap,
        g_pointTreeTests,
        g_bbox_tree_tests
    },
    SimpleTreeTestData {
        3.0,
        11.0,
        4,
        g_polygonMap,
        g_pointTreeTests,
        g_bbox_tree_tests
    }
};

BOOST_AUTO_TEST_CASE(test_cut_tree)
{
    for (const SimpleTreeTestData& testData: g_simpleTestData) {
        tree::CutTree<mms::Standalone, RegionId> tree;
        tree.build<tree::CutTreeBuilder<RegionId> >(
            testData.map,
            testData.minBoxSize,
            testData.maxBoxSize,
            testData.maxVertices,
            SpatialRefSystem::Geodetic);

        std::ofstream out(g_mmsTreeFile);
        mms::Writer w(out);
        mms::safeWrite<tree::CutTree<mms::Standalone, RegionId> >(w, tree);
        out.close();

        mms::Holder2<tree::CutTree<mms::Mmapped, RegionId> >
            treeHldr(g_mmsTreeFile);
        const tree::CutTree<mms::Mmapped, RegionId>* treePtr = treeHldr.get();

        for (const PointTreeTest& pointTest: testData.pointTests) {
            tree::DummyCheck<mms::Mmapped, RegionId> check;
            AllPointVisitor visitor(pointTest.point, check);
            treePtr->traverse(visitor);
            std::vector<RegionId> res(visitor.result());
            BOOST_CHECK_MESSAGE(res == pointTest.ids,
                "\nreceived: " << res <<
                "\nexpected: " << pointTest.ids << "\n");
        }

        for (const BBoxTreeTest& bboxTest: testData.bboxTests) {
            tree::DummyCheck<mms::Mmapped, RegionId> check;
            AllBBoxVisitor visitor(bboxTest.bbox, check);
            treePtr->traverse(visitor);
            std::set<RegionId> res;
            std::unordered_set<RegionId> ures = visitor.result();
            for (auto id: ures) {
                res.insert(id);
            }
            BOOST_CHECK_MESSAGE(res == bboxTest.ids,
                "\nreceived: " << res <<
                "\nexpected: " << bboxTest.ids << "\n");
        }
    }
}
