#include "../test_tools.h"

#include <library/cpp/testing/unittest/registar.h>
#include <maps/analyzer/services/jams_analyzer/modules/segmentshandler/lib/dump_checker.h>
#include <maps/analyzer/libs/manoeuvres/include/types.h>


using namespace std;
using boost::posix_time::ptime;

using maps::analyzer::VehicleId;
using maps::analyzer::manoeuvres::ManoeuvreId;

using maps::road_graph::EdgeId;
using maps::road_graph::SegmentIndex;
using maps::road_graph::SegmentId;


#define CHECK_THROW_DUMP_ERROR(a) UNIT_ASSERT_EXCEPTION(a, DumpError)


const DumpChecker& getDumpChecker()
{
    static const DumpChecker checker{getGraph()};
    return checker;
}


Y_UNIT_TEST_SUITE(DumpCheckerTest)
{
    Y_UNIT_TEST(CheckSegmentId)
    {
        // Of course, there are no arithmetic operations on EdgeId;
        // here they are performed only because of tests with DumpChecker
        const auto& checker = getDumpChecker();
        const auto& graph = getGraph();
        UNIT_ASSERT(checker.checkEdgeId(EdgeId(0)));
        UNIT_ASSERT(checker.checkEdgeId(graph.edgesNumber() - EdgeId(1)));
        UNIT_ASSERT(!checker.checkEdgeId(graph.edgesNumber()));
        UNIT_ASSERT(!checker.checkEdgeId(EdgeId(-1)));

        UNIT_ASSERT(checker.checkSegmentId(seg(0, 0)));
        UNIT_ASSERT(checker.checkSegmentId(
                    seg(graph.edgesNumber().value() - 1, 0)));
        size_t segmentsNumber = graph.edgeData(EdgeId(0)).geometry().segmentsNumber();
        UNIT_ASSERT(checker.checkSegmentId(seg(0, segmentsNumber - 1)));

        UNIT_ASSERT(!checker.checkSegmentId(
                    seg(graph.edgesNumber(), 0)));

        UNIT_ASSERT(!checker.checkSegmentId(seg(0, segmentsNumber)));
        UNIT_ASSERT(!checker.checkSegmentId(
                    seg(0, segmentsNumber * 2)));
    }

    Y_UNIT_TEST(CheckDouble)
    {
        const auto& checker = getDumpChecker();
        UNIT_ASSERT(checker.checkNonNegativeDouble(0));
        UNIT_ASSERT(checker.checkNonNegativeDouble(5.3));
        UNIT_ASSERT(checker.checkNonNegativeDouble(0.5));

        UNIT_ASSERT(!checker.checkNonNegativeDouble(-10));
        UNIT_ASSERT(!checker.checkNonNegativeDouble(-5.3));
        UNIT_ASSERT(!checker.checkNonNegativeDouble(
                    std::numeric_limits<double>::quiet_NaN()));
    }

    Y_UNIT_TEST(CheckSegment)
    {
        const auto& checker = getDumpChecker();
        SegmentId okId = seg(0, 0);
        SegmentId badId = seg(getGraph().edgesNumber(), 0);
        VehicleId vId("0", "9");

        UNIT_ASSERT_NO_EXCEPTION(checker.checkSegment(
            createTravelTime(okId, maps::nowUtc(), vId, 0, 0, ManoeuvreId(123)), {.edgeId = EdgeId(0), .manoeuvreId = ManoeuvreId(123)}));
        UNIT_ASSERT_NO_EXCEPTION(checker.checkSegment(
            createTravelTime(okId, maps::nowUtc(), vId, 4, 10), {.edgeId = EdgeId(0)}));
        UNIT_ASSERT_NO_EXCEPTION(checker.checkSegment(
            createTravelTime(okId, maps::nowUtc(), vId, 2.5, 3.3), {.edgeId = EdgeId(0)}));

        CHECK_THROW_DUMP_ERROR(checker.checkSegment(
            createTravelTime(okId, maps::nowUtc(), vId, 0, 0), {.edgeId = EdgeId(0), .manoeuvreId = ManoeuvreId(123)}));
        CHECK_THROW_DUMP_ERROR(checker.checkSegment(
            createTravelTime(okId, maps::nowUtc(), vId, 0, 0, ManoeuvreId(234)), {.edgeId = EdgeId(0), .manoeuvreId = ManoeuvreId(123)}));
        CHECK_THROW_DUMP_ERROR(checker.checkSegment(
                createTravelTime(okId, maps::nowUtc(),
                    vId, 2.5, 3.3), {.edgeId = EdgeId(1)}));
        CHECK_THROW_DUMP_ERROR(checker.checkSegment(
                createTravelTime(badId, maps::nowUtc(),
                                      vId, 2.5, 3.3), {.edgeId = EdgeId(0)}));
        CHECK_THROW_DUMP_ERROR(checker.checkSegment(
                createTravelTime(okId,
                                      boost::posix_time::not_a_date_time,
                                      vId, 2.5, 3.3), {.edgeId = EdgeId(0)}));
        CHECK_THROW_DUMP_ERROR(checker.checkSegment(
                createTravelTime(okId, maps::nowUtc(), vId, -2.5, 3.3), {.edgeId = EdgeId(0)}));
        CHECK_THROW_DUMP_ERROR(checker.checkSegment(
                createTravelTime(okId, maps::nowUtc(),
                                      vId, 2.5, -3.3), {.edgeId = EdgeId(0)}));

        UNIT_ASSERT_NO_EXCEPTION(checker.checkSegments(
            SegmentsQueue(5, createTravelTime(okId, maps::nowUtc(),
                    vId, 2.5, 3.3)), {.edgeId = EdgeId(0)}));
        CHECK_THROW_DUMP_ERROR(checker.checkSegments(
            SegmentsQueue(5, createTravelTime(okId, maps::nowUtc(),
                    vId, 2.5, 3.3)), {.edgeId = EdgeId(1)}));
        CHECK_THROW_DUMP_ERROR(checker.checkSegments(
            SegmentsQueue(5, createTravelTime(badId, maps::nowUtc(),
                    vId, 2.5, 3.3)), {.edgeId = EdgeId(0)}));
        SegmentsQueue queue(5, createTravelTime(okId, maps::nowUtc(),
                    vId, 2.5, 3.3));
        queue.push_back(createTravelTime(okId, maps::nowUtc(),
                    vId, 2.5, -3.3));
        CHECK_THROW_DUMP_ERROR(checker.checkSegments(queue, {.edgeId = EdgeId(0)}));
    }

    Y_UNIT_TEST(CheckTask)
    {
        const auto& checker = getDumpChecker();
        UNIT_ASSERT_NO_EXCEPTION(checker.checkTask(
            InterpolateTask({.edgeId = EdgeId(0), .manoeuvreId = ManoeuvreId(123)}, maps::nowUtc(), maps::nowUtc())));

        CHECK_THROW_DUMP_ERROR(checker.checkTask(
            InterpolateTask({.edgeId = EdgeId(-1)}, maps::nowUtc(), maps::nowUtc())));
        boost::posix_time::ptime nadt;
        CHECK_THROW_DUMP_ERROR(checker.checkTask(
            InterpolateTask({.edgeId = EdgeId(0)}, nadt, maps::nowUtc())));
        CHECK_THROW_DUMP_ERROR(checker.checkTask(
            InterpolateTask({.edgeId = EdgeId(0)}, maps::nowUtc(), nadt)));
        CHECK_THROW_DUMP_ERROR(checker.checkTask(
            InterpolateTask::createMarkerTask(
                {.edgeId = EdgeId(0)}, maps::nowUtc(), maps::nowUtc())));

        {
            SegmentsQueue queue(
                5,
                createStandingSegment(
                    seg(0, 0), maps::nowUtc(), VehicleId("0", "1")
                )
            );
            InterpolateTask task({.edgeId = EdgeId(0)}, maps::nowUtc(), &queue, maps::nowUtc());
            UNIT_ASSERT_NO_EXCEPTION(checker.checkTask(task));
        }
        {
            SegmentsQueue queue(
                5,
                createTravelTime(
                    seg(0, 0),
                    maps::nowUtc(),
                    VehicleId("0", "1"), -2.5,
                    3.3
                )
            );
            InterpolateTask task({.edgeId = EdgeId(0)}, maps::nowUtc(), &queue, maps::nowUtc());
            CHECK_THROW_DUMP_ERROR(checker.checkTask(task));
        }
    }
}
