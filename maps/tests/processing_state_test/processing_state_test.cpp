#include "../test_tools.h"

#include <library/cpp/testing/unittest/registar.h>
#include <maps/analyzer/services/jams_analyzer/modules/segmentshandler/lib/processing_state.h>
#include <maps/analyzer/services/jams_analyzer/modules/segmentshandler/lib/config.h>
#include <maps/libs/deprecated/boost_time/utils.h>

using boost::posix_time::ptime;

using maps::road_graph::EdgeId;

const auto first_seg = seg(0, 0);


Y_UNIT_TEST_SUITE(ProcessingStateTest)
{
    Y_UNIT_TEST(ModeTest)
    {
        {
            ProcessingState state(Config::Offline);
            UNIT_ASSERT_EQUAL(state.mode(), Config::Offline);
        }
        {
            ProcessingState state(Config::Realtime);
            UNIT_ASSERT_EQUAL(state.mode(), Config::Realtime);
        }
    }

    Y_UNIT_TEST(RealtimeTest)
    {
        ProcessingState state(Config::Realtime);
        UNIT_ASSERT_EQUAL(state.mode(), Config::Realtime);
        UNIT_ASSERT(state.maxLeaveTime().is_not_a_date_time());
        ptime start = maps::nowUtc();
        state.requestAdded({.edgeId = EdgeId(0)}, createTravelTime(start, first_seg));
        UNIT_ASSERT(state.maxLeaveTime().is_not_a_date_time());
        state.requestAdded({.edgeId = EdgeId(0)}, createTravelTime(start - pt::seconds(1000),
                    first_seg));
        UNIT_ASSERT(state.maxLeaveTime().is_not_a_date_time());
        state.requestAdded({.edgeId = EdgeId(0)}, createTravelTime(start + pt::seconds(1000),
                    first_seg));
        UNIT_ASSERT(state.maxLeaveTime().is_not_a_date_time());
    }

    Y_UNIT_TEST(OfflineTest)
    {
        ProcessingState state(Config::Offline);
        UNIT_ASSERT_EQUAL(state.mode(), Config::Offline);
        UNIT_ASSERT(state.maxLeaveTime().is_not_a_date_time());
        ptime start = maps::nowUtc();
        state.requestAdded({.edgeId = EdgeId(0)}, createTravelTime(start - pt::seconds(1001),
                    first_seg));
        UNIT_ASSERT_EQUAL(state.maxLeaveTime(), start - pt::seconds(1000));

        // leave time exactly now
        state.requestAdded({.edgeId = EdgeId(0)}, createTravelTime(start - pt::seconds(1), first_seg));
        UNIT_ASSERT_EQUAL(state.maxLeaveTime(), start);

        //past: signals, older than last ones
        state.requestAdded({.edgeId = EdgeId(0)}, createTravelTime(start - pt::seconds(600),
                    first_seg));
        UNIT_ASSERT_EQUAL(state.maxLeaveTime(), start);

        //future
        state.requestAdded({.edgeId = EdgeId(0)}, createTravelTime(start + pt::seconds(600),
                    first_seg));
        UNIT_ASSERT_EQUAL(state.maxLeaveTime(), start);
    }
}
