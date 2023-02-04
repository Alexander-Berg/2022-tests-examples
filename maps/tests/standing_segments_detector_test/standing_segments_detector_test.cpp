#define BOOST_TEST_MAIN

#include <maps/analyzer/services/jams_analyzer/modules/usershandler/lib/config.h>
#include <maps/analyzer/services/jams_analyzer/modules/usershandler/lib/graphs_wrapper.h>
#include <maps/analyzer/services/jams_analyzer/modules/usershandler/lib/standing_segments_detector.h>
#include <maps/analyzer/services/jams_analyzer/modules/usershandler/tests/test_tools/include/test_tools.h>

#include <library/cpp/testing/common/env.h>

#include <boost/test/unit_test.hpp>
#include <boost/optional/optional_io.hpp>

#include <set>
#include <string>

namespace ma = maps::analyzer;
namespace mg = maps::geolib3;
namespace mad = maps::analyzer::data;
namespace pt = boost::posix_time;
namespace mrg = maps::road_graph;

const std::string CONFIG = "usershandler.conf";

const std::string GOOD_CLID = "ru.yandex.mobile.navigator";
const std::string BAD_CLID = "fake";
const mg::Point2 LAST_POINT = mg::Point2(54.83805, 37.46212);
const pt::ptime LAST_TIME = maps::nowUtc();
const mrg::SegmentId GOOD_SEGMENT{mrg::EdgeId(0), mrg::SegmentIndex(0)};
const mrg::SegmentId BAD_SEGMENT{mrg::EdgeId(6), mrg::SegmentIndex(0)};


namespace std {

template <typename T>
ostream& operator<< (ostream& ostr, const optional<T>& value) {
    if (!value) {
        return ostr << "nullopt";
    }
    return ostr << "{" << value.value() << "}";
}

} // std


struct TestFixture {
    Config config;
    TestConsumer consumer;
    GraphsWrapper graphsWrapper;
    StandingSegmentsDetector detector;

    TestFixture():
        config(makeUsershandlerConfig(CONFIG)),
        graphsWrapper(config.roadGraphConfig(), config.cacheConfig(), false),
        detector(
            config.standingSegmentDetectorConfig(),
            consumer,
            graphsWrapper
        )
    {
    }

    bool hasPersistentEdgeId(const maps::analyzer::data::SegmentTravelTime& data) {
        auto expectedLongId = graphsWrapper.edgeLongId(data.segmentId().edgeId);

        auto foundLongId = data.persistentEdgeId();
        return foundLongId && *foundLongId == expectedLongId;
    }

    std::set<PointWithTime> simpleHistory(const pt::time_duration& maxDuration) {
        return {
            { LAST_POINT, LAST_TIME + pt::seconds(1) },
            { LAST_POINT, LAST_TIME + maxDuration }
        };
    }
};

BOOST_FIXTURE_TEST_SUITE( StandingSegmentsDetectorTestSuite, TestFixture );

BOOST_AUTO_TEST_CASE( test_constants )
{
    BOOST_CHECK(config.standingSegmentDetectorConfig().allowedClid(GOOD_CLID));
    BOOST_CHECK(!config.standingSegmentDetectorConfig().allowedClid(BAD_CLID));
    BOOST_CHECK(
        graphsWrapper.edgeCategory(GOOD_SEGMENT.edgeId) <=
        config.standingSegmentDetectorConfig().maxCategory()
    );
    BOOST_CHECK(
        graphsWrapper.edgeCategory(BAD_SEGMENT.edgeId) >
        config.standingSegmentDetectorConfig().maxCategory()
    );
}

BOOST_AUTO_TEST_CASE( success )
{
    detector.push(
        ma::VehicleId(GOOD_CLID, "uuid"),
        simpleHistory(config.standingSegmentDetectorConfig().minDuration()),
        { LAST_POINT, LAST_TIME},
        GOOD_SEGMENT
    );
    BOOST_REQUIRE_EQUAL(consumer.segmentVector.size(), 1);
    BOOST_CHECK_EQUAL(consumer.segmentVector[0].standingStartTime(), LAST_TIME);
    assert(consumer.segmentVector[0].segmentId() == GOOD_SEGMENT);
    BOOST_CHECK(hasPersistentEdgeId(consumer.segmentVector[0]));
}

BOOST_AUTO_TEST_CASE( success_with_far_point )
{
    const auto farPoint = mg::Point2(LAST_POINT.x() + 1, LAST_POINT.y() + 1);
    detector.push(
        ma::VehicleId(GOOD_CLID, "uuid"),
        {
            { LAST_POINT, LAST_TIME + pt::seconds(1) },
            { LAST_POINT, LAST_TIME + config.standingSegmentDetectorConfig().minDuration() },
            { farPoint, LAST_TIME + config.standingSegmentDetectorConfig().minDuration() + pt::seconds(1) }
        },
        { LAST_POINT, LAST_TIME},
        GOOD_SEGMENT
    );
    BOOST_REQUIRE_EQUAL(consumer.segmentVector.size(), 1);
    BOOST_CHECK_EQUAL(consumer.segmentVector[0].standingStartTime(), LAST_TIME);
    assert(consumer.segmentVector[0].segmentId() == GOOD_SEGMENT);
    BOOST_CHECK(hasPersistentEdgeId(consumer.segmentVector[0]));
}

BOOST_AUTO_TEST_CASE( bad_duration )
{
    detector.push(
        ma::VehicleId(GOOD_CLID, "uuid"),
        simpleHistory(config.standingSegmentDetectorConfig().minDuration() - pt::seconds(1)),
        { LAST_POINT, LAST_TIME},
        GOOD_SEGMENT
    );
    BOOST_CHECK(consumer.segmentVector.empty());
}

BOOST_AUTO_TEST_CASE( bad_clid )
{
    detector.push(
        ma::VehicleId(BAD_CLID, "uuid"),
        simpleHistory(config.standingSegmentDetectorConfig().minDuration()),
        { LAST_POINT, LAST_TIME},
        GOOD_SEGMENT
    );
    BOOST_CHECK(consumer.segmentVector.empty());
}

BOOST_AUTO_TEST_CASE( big_radius )
{
    const auto farPoint = mg::Point2(LAST_POINT.x() + 1, LAST_POINT.y() + 1);
    detector.push(
        ma::VehicleId(GOOD_CLID, "uuid"),
        {
            { LAST_POINT, LAST_TIME + pt::seconds(1) },
            { farPoint, LAST_TIME + config.standingSegmentDetectorConfig().minDuration() }
        },
        { LAST_POINT, LAST_TIME},
        GOOD_SEGMENT
    );
    BOOST_CHECK(consumer.segmentVector.empty());
}

BOOST_AUTO_TEST_CASE( bad_segment )
{
    detector.push(
        ma::VehicleId(GOOD_CLID, "uuid"),
        simpleHistory(config.standingSegmentDetectorConfig().minDuration()),
        { LAST_POINT, LAST_TIME},
        BAD_SEGMENT
    );
    BOOST_CHECK(consumer.segmentVector.empty());
}

BOOST_AUTO_TEST_SUITE_END()
