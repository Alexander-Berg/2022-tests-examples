#include "../visitors.h"
#include "../test_tools/io_std.h"

#include "../tree/search_tree.h"
#include "../tree/cut_tree_builder.h"

#include <yandex/maps/coverage5/region.h>

#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/polyline.h>

#include <yandex/maps/mms/writer.h>
#include <yandex/maps/mms/holder2.h>

#include <boost/test/unit_test.hpp>
#include <boost/test/unit_test_log.hpp>
#include <boost/test/test_tools.hpp>

#include <fstream>

using namespace maps::coverage5;
using maps::coverage5::tree::CutTree;

using maps::geolib3::EPS;
using maps::geolib3::Point2;
using maps::geolib3::Polyline2;
using maps::geolib3::Contains;

typedef std::vector<RegionId> IDVector;

const char* g_mmsTreeFile = "acc_test_tree.mms";

void checkPointsNearbyCut(const CutTree<mms::Mmapped, RegionId>* tree,
    const size_t NUM_STEPS, const double STEP_SIZE, double dx, double dy)
{
    for (size_t i = 1; i < NUM_STEPS; ++i) {
        for (size_t j = 1; j < NUM_STEPS; ++j) {
            Point2 point(i * STEP_SIZE + dx, j * STEP_SIZE + dy);
            tree::DummyCheck<mms::Mmapped, RegionId> check;
            AllPointVisitor visitor(point, check);
            tree->traverse(visitor);
            IDVector res(visitor.result());
            BOOST_CHECK(res.size() == 1 && res.front() == 1);
        }
    }
}

/**
 * Shoots artificially built tree to check whether
 * points or polylines lying on cut line are found correctly.
 */
BOOST_AUTO_TEST_CASE(test_objects_on_cut)
{
    using namespace maps;
    using namespace maps::geolib3;

    const double STEP_SIZE = 1e-3;
    const size_t NUM_STEPS = 2048;
    const double MIN_BOX_SIZE = 5e-4;
    const double MAX_BOX_SIZE = 360.0;
    const size_t MAX_POINTS = 10;

    PointsVector v;
    for (size_t i = 0; i < NUM_STEPS; ++i) {
        v.push_back(Point2(i * STEP_SIZE, 0.0));
    }
    for (size_t i = 0; i < NUM_STEPS; ++i) {
        v.push_back(Point2(NUM_STEPS * STEP_SIZE, i * STEP_SIZE));
    }
    v.push_back(Point2(NUM_STEPS * STEP_SIZE, NUM_STEPS * STEP_SIZE));
    v.push_back(Point2(0.0, NUM_STEPS * STEP_SIZE));

    geom::StandaloneLinearRing ring(v, false);
    geom::StandalonePolygon poly(ring, geom::Validate::No);
    geom::StandaloneMultiPolygon mpoly(poly, geom::Validate::Yes);

    tree::CutTree<mms::Standalone, RegionId> tree;
    tree::CutTreeBuilder<RegionId>::MultiPolygonsMap map;
    map.insert(tree::CutTreeBuilder<RegionId>::MultiPolygonsMap::value_type(
        1, mpoly));
    tree.build<tree::CutTreeBuilder<RegionId> >(map, MIN_BOX_SIZE, MAX_BOX_SIZE,
        MAX_POINTS, SpatialRefSystem::Geodetic);

    std::ofstream out(g_mmsTreeFile);
    mms::Writer w(out);
    mms::safeWrite<tree::CutTree<mms::Standalone, RegionId> >(w, tree);
    out.close();

    mms::Holder2<tree::CutTree<mms::Mmapped, RegionId> >
        treeHldr(g_mmsTreeFile);
    const tree::CutTree<mms::Mmapped, RegionId>* treePtr = treeHldr.get();

    // check points nearby cut

    for (int i = -1; i <= 1; ++i) {
        for (int j = -1; j <= 1; ++j) {
            checkPointsNearbyCut(treePtr,
                NUM_STEPS, STEP_SIZE,  i * EPS,  j * EPS);
        }
    }
}
