#include "../include/RasterizerTest.h"
#include <yandex/maps/renderer5/rasterizer/ClipBoxPipeline.h>

#include <maps/renderer/libs/base/include/geom/vertices.h>
#include <maps/renderer/libs/base/include/geom/box.h>

#include <algorithm>

using namespace boost::unit_test;
using namespace maps::renderer::base;

namespace maps {
namespace renderer5 {
namespace rasterizer {

namespace {
    struct DashHandler
    {
        void newLine(double distanceFromStart)
        {
            lineStarts.push_back(distanceFromStart);
        }

        std::list<double> lineStarts;
    };
}

// general clip box pipeline test
void testClipBoxPipelineGeneral()
{
    BoxD box(0, 0, 256, 256);
    AggVertexSource avs;
    avs.addMoveTo({100, -500});
    avs.addLineTo({100,  500});
    avs.addLineTo({200,  500});
    avs.addLineTo({200,  100});

    AggVertexSource compare;
    compare.addMoveTo({100,   0});
    compare.addLineTo({100, 256});
    compare.addMoveTo({200, 256});
    compare.addLineTo({200, 100});
    compare.rewind();

    std::list<double> refList;
    refList.push_back(500.0);
    refList.push_back(1344.0);

    DashHandler dashHandler;

    ClipBoxPipeline<AggVertexSource, DashHandler>  cb(avs, box, &dashHandler);

    cb.rewind();
    double x, y, xc = 0, yc = 0;
    int cmd;

    for (;;)
    {
        cmd = cb.vertex(&x, &y);
        BOOST_CHECK(cmd == compare.vertex(&xc, &yc));
        if (agg::is_stop(cmd))
            break;
        BOOST_CHECK(x == xc && y == yc);
    }

    BOOST_CHECK(
        std::equal(
            refList.begin(), refList.end(),
            dashHandler.lineStarts.begin()));
}


// fixes MAPSCORE-2401
void testClipBoxPipeline()
{
    BoxD box(0, 0, 256, 256);
    AggVertexSource avs;
    avs.addMoveTo({10, 10});
    avs.addLineTo({20, 10});
    avs.addLineTo({20, 20});
    ClipBoxPipeline<AggVertexSource> cb(avs, box);

    cb.rewind();
    double x, y;
    int cmd;
    int moves = 0;
    while (cmd = cb.vertex(&x, &y))
    {
        if (cmd == agg::path_cmd_move_to)
            ++moves;
    }
    BOOST_CHECK(moves == 1);
}

} // namespace rasterizer
} // namespace renderer5
} // namespace maps

namespace maps {
namespace renderer5 {
namespace test {
namespace rasterizer {

test_suite * init_suite()
{
    test_suite * suite = BOOST_TEST_SUITE("Rasterizer test suite");

    suite->add(
        BOOST_TEST_CASE(&maps::renderer5::rasterizer::testClipBoxPipelineGeneral));
    suite->add(
        BOOST_TEST_CASE(&maps::renderer5::rasterizer::testClipBoxPipeline));

    return suite;
}

} // namespace rasterizer
} // namespace test
} // namespace renderer5
} // namespace maps
