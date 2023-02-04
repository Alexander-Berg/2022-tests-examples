#include "../test_tools.h"

#include <library/cpp/testing/unittest/registar.h>
#include <maps/analyzer/services/jams_analyzer/modules/segmentshandler/lib/task_scheduler.h>
#include <maps/analyzer/services/jams_analyzer/modules/segmentshandler/lib/task_processor.h>
#include <maps/analyzer/services/jams_analyzer/modules/segmentshandler/lib/result_cache.h>
#include <yandex/maps/mms/holder2.h>

namespace pt = boost::posix_time;

void updateMaxLeaveTime(ProcessingState& processingState, const pt::ptime time)
{
    processingState.requestAdded({.edgeId = maps::road_graph::EdgeId(0)},
            createTravelTime(time, seg(0, 0)));
}

Y_UNIT_TEST_SUITE(TaskSchedulerTest)
{
    Y_UNIT_TEST(ComplexTest)
    {
        ProcessingState processingState(Config::Offline);
        pt::ptime start = maps::nowUtc() - pt::seconds(1000);
        updateMaxLeaveTime(processingState, start);

        Config config(makeSegmentshandlerConfig("segmentshandler.conf"));
        TaskScheduler scheduler(
                config.processWaiting(),
                config.reinterpolateWaiting(),
                config);
        scheduler.setGlobalState(&processingState);

        checkUnfinish(processingState.finishResult());

        const auto id = seg(7, 0);
        SegmentsQueue segments(1, createTravelTime(start, id));
        //Create first task
        InterpolateTask first = scheduler.create({.edgeId = id.edgeId}, &segments);
        UNIT_ASSERT_EQUAL(first.time(), maps::nowUtc() + config.processWaiting());
        UNIT_ASSERT(!first.isMarker());
        UNIT_ASSERT_EQUAL(first.id().edgeId, id.edgeId);
        UNIT_ASSERT_EQUAL(first.interpolateTime(), start + pt::seconds(1));
        UNIT_ASSERT_EQUAL(first.segments().size(), 1);

        UNIT_ASSERT_EQUAL(segments.size(), 0);
        checkUnfinish(processingState.finishResult());

        //Update task
        InterpolateTask updated = first;
        segments.push_back(createTravelTime(start, id));
        updateMaxLeaveTime(processingState, start + pt::seconds(1));
        scheduler.update(&updated, 0, &segments);

        UNIT_ASSERT_EQUAL(first.time(), maps::nowUtc() + config.processWaiting());
        UNIT_ASSERT(!first.isMarker());
        UNIT_ASSERT_EQUAL(updated.id().edgeId, id.edgeId);
        UNIT_ASSERT_EQUAL(updated.interpolateTime(), start + pt::seconds(2));
        UNIT_ASSERT_EQUAL(updated.segments().size(), 2);

        UNIT_ASSERT_EQUAL(segments.size(), 0);
        checkUnfinish(processingState.finishResult());

        segments.push_back(createTravelTime(start, id));
        EdgeHistory edgeHistory;
        //Complete task, but new info added while processing
        std::optional<InterpolateTask> next =
            scheduler.next(updated, edgeHistory, &segments);

        UNIT_ASSERT(next);
        UNIT_ASSERT_EQUAL(next->time(), maps::nowUtc() + config.processWaiting());
        UNIT_ASSERT(!next->isMarker());
        UNIT_ASSERT_EQUAL(next->id().edgeId, id.edgeId);
        UNIT_ASSERT_EQUAL(next->interpolateTime(), start + pt::seconds(2));
        UNIT_ASSERT_EQUAL(next->segments().size(), 1);

        UNIT_ASSERT_EQUAL(segments.size(), 0);
        checkUnfinish(processingState.finishResult());

        //Complete task, no new info added
        std::optional<InterpolateTask> newNext =
            scheduler.next(*next, edgeHistory, &segments);
        UNIT_ASSERT(newNext);
        UNIT_ASSERT(newNext->isMarker());
        UNIT_ASSERT_EQUAL(newNext->id().edgeId, id.edgeId);
        UNIT_ASSERT_EQUAL(newNext->interpolateTime(), start + pt::seconds(2));
        UNIT_ASSERT_EQUAL(newNext->segments().size(), 0);
        checkFinished(processingState.finishResult(), start + pt::seconds(2));

        //Add one more task.
        segments.push_back(createTravelTime(start, id));
        InterpolateTask oneMore = scheduler.create({.edgeId = id.edgeId}, &segments);
        checkUnfinish(processingState.finishResult());

        //Complete marker task once more
        UNIT_ASSERT(scheduler.next(*newNext, edgeHistory, &segments));
        checkUnfinish(processingState.finishResult());

        //Complete second task
        auto processedMore = scheduler.next(oneMore, edgeHistory, &segments);
        UNIT_ASSERT(processedMore);
        checkFinished(processingState.finishResult(), start + pt::seconds(2));

        //Complete marker task
        UNIT_ASSERT(segments.empty());
        auto afterMarkerTask = scheduler.next(*processedMore, edgeHistory, &segments);
        checkFinished(processingState.finishResult(), start + pt::seconds(2));

        //Complete marker task with new info, so unfinish
        segments.push_back(createTravelTime(start, id));
        auto afterMarkerTaskWithInfo = scheduler.next(*processedMore, edgeHistory, &segments);
        checkUnfinish(processingState.finishResult());
    }
}
