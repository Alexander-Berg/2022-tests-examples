#include "../test_tools.h"

#include <boost/none.hpp>
#include <library/cpp/testing/unittest/registar.h>
#include <maps/analyzer/libs/realtime_jams/include/feature_indices.h>
#include <maps/analyzer/libs/realtime_jams/include/features.h>
#include <maps/analyzer/libs/realtime_jams/include/model.h>
#include <maps/analyzer/libs/time_interpolator/include/types.h>
#include <maps/analyzer/services/jams_analyzer/modules/segmentshandler/lib/config.h>
#include <maps/analyzer/services/jams_analyzer/modules/segmentshandler/lib/segment_history.h>
#include <yandex/maps/mms/holder2.h>


namespace pt = boost::posix_time;
namespace ma = maps::analyzer;
namespace mati = maps::analyzer::time_interpolator;
namespace realtime_jams = maps::analyzer::realtime_jams;
using namespace std;

using ma::data::SegmentTravelTime;

const double EPS = 1e-4;


struct SegmentHistoryTestFixture : NUnitTest::TBaseFixture
{
    SegmentHistoryTestFixture():
        defaultConfig(makeSegmentshandlerConfig("segmentshandler.conf")),
        noUuidsDefaultFilter(UuidFilter(noUuids, JamsType::DEFAULT, true))
    {}

    const Config defaultConfig;
    const std::unordered_set<std::string> noUuids;
    const UuidFilter noUuidsDefaultFilter;
};

inline mad::SegmentTravelTime createTravelTime(
    const pt::ptime time,
    size_t userIndex,
    double travelTime)
{
    return createTravelTime(
        seg(0, 0),
        time,
        ma::VehicleId("a", boost::lexical_cast<std::string>(userIndex)),
        travelTime,
        1.0
    );
}

inline pt::ptime createTime(size_t hours, size_t minutes)
{
    return pt::from_iso_string("20170501T000000") + pt::hours(hours) + pt::minutes(minutes);
}

// Mock matrixnet, just doubles time per km
class MockModel : public realtime_jams::Model
{
public:
    double DoCalcRelev(const float* features) const override
    {
        auto timePerKm = features[realtime_jams::TRAVEL_TIMES_EXPONENTIAL_AVERAGE];
        if (timePerKm == realtime_jams::ABSENT_FEATURE) {
            auto segmentLength = static_cast<double>(features[realtime_jams::SEGMENT_LENGTH]);
            // return secs per km such that travel time will be 100
            return 100.0 / segmentLength * 1000.0;
        } else {
            return static_cast<double>(timePerKm * 2.0);
        }
    }
};

Y_UNIT_TEST_SUITE_F(SegmentHistoryTest, SegmentHistoryTestFixture)
{
    Y_UNIT_TEST(SegmentHistoryInterpolation)
    {
        SegmentHistory segmentHistory;
        realtime_jams::RealtimeJamsEngine engine{makeEngineConfig()};
        engine.setModel(std::make_unique<MockModel>());
        engine.setCommonRatio(1);
        engine.setMaxSignalCountForCalculationWithModel(4);

        Config config(makeSegmentshandlerConfig("segmentshandler.conf"));
        config.setInterpolationWindow(pt::hours(4));

        auto  segmentId = seg(0, 0);
        const double segmentLength = maps::road_graph::segmentGeoLength(getGraph(), segmentId);

        std::vector<double> correctInterpolationTimes {
            15.0 * 2.0, // avg(10, 20) * 2 for mx
            25.0 * 2.0, // avg(10, 20 .. 40) * 2 for mx
            35.0, // avg(10, 20 .. 60), enough signals (>= 5) for pure interpolation
            45.0, // avg(10, 20 .. 80), enough signals
            55.0, // avg(30, 40 .. 80), first two deleted, enough signals
            65.0 * 2.0, // avg(50, 60 .. 80) * 2 for mx, only 4 signals left
            75.0 * 2.0, // avg(70, 80) * 2
            100.0 // no signals
        };

        // start from 0 hour
        // add two signals in each hour before 4:00
        // get interpolation for each hour in [1:00, 8:00]
        size_t noMoreSignals = 4;
        size_t interpolateUntil = 8;
        for (size_t i = 0; i < interpolateUntil; ++i) {
            if (i < noMoreSignals) {
                segmentHistory.add(createTravelTime(
                    createTime(i, 20),
                    i * 2,
                    static_cast<mati::TravelTime>(i) * 20.0 + 10.0
                ));
                segmentHistory.add(createTravelTime(
                    createTime(i, 40),
                    i * 2 + 1,
                    static_cast<mati::TravelTime>(i) * 20.0 + 20.0
                ));
            }
            auto interpolation = segmentHistory.interpolate(
                createTime(i + 1, 0),
                segmentId,
                config,
                noUuidsDefaultFilter,
                getGraph(),
                engine,
                getGeobaseLookup()
            );
            UNIT_ASSERT_DOUBLES_EQUAL(
                segmentLength / interpolation->averageSpeed,
                correctInterpolationTimes[i],
                EPS
            );
        }
    }

    Y_UNIT_TEST(SegmentHistoryInterpolateFilter) {
        SegmentHistory segmentHistory;
        pt::time_iterator timeIterator(
            pt::from_iso_string("20130316T150000"),
            pt::seconds(60)
        );

        realtime_jams::RealtimeJamsEngine engine{makeEngineConfig()};
        engine.setCommonRatio(1);

        const auto segmentId = seg(9, 0);
        const double segmentLength = maps::road_graph::segmentGeoLength(getGraph(), segmentId);
        const std::unordered_set<std::string> uuids = {"2", "4"};
        const auto defaultFilter = UuidFilter(uuids, JamsType::DEFAULT, true);
        const auto massTransitFilter = UuidFilter(uuids, JamsType::MASS_TRANSIT, true);

        std::vector<double> travelTimes{ 10, 20, 30, 60, 50 };
        for (size_t i = 0; i < travelTimes.size(); ++i) {
            segmentHistory.add(createTravelTime(
                segmentId, *timeIterator, ma::VehicleId("clid", std::to_string(i)),
                travelTimes[i], 1.0
            ));
            ++timeIterator;
        }

        const auto defaultResult = segmentHistory.interpolate(
            *timeIterator + pt::minutes(1), segmentId,
            defaultConfig, defaultFilter, getGraph(), engine, getGeobaseLookup()
        );
        const auto massTransitResult = segmentHistory.interpolate(
            *timeIterator + pt::minutes(1), segmentId,
            defaultConfig, massTransitFilter, getGraph(), engine, getGeobaseLookup()
        );
        UNIT_ASSERT_EQUAL(segmentLength / defaultResult->averageSpeed, 30);
        UNIT_ASSERT_EQUAL(defaultResult->usersNumber, 3);
        UNIT_ASSERT_EQUAL(segmentLength / massTransitResult->averageSpeed, 40);
        UNIT_ASSERT_EQUAL(massTransitResult->usersNumber, 2);
    }

    Y_UNIT_TEST(Passings)
    {
        const auto segmentId = seg(0, 0);
        const auto time = maps::nowUtc();

        SegmentHistory segmentHistory;
        segmentHistory.add(createTravelTime(
            segmentId, time - pt::seconds(60), ma::VehicleId("a", "a"), 30, 1
        ));
        UNIT_ASSERT_EQUAL(segmentHistory.passings(time, noUuidsDefaultFilter), 0u);
        segmentHistory.add(createTravelTime(
            segmentId, time - pt::seconds(60), ma::VehicleId("a", "b"), 70, 1
        ));
        UNIT_ASSERT_EQUAL(segmentHistory.passings(time, noUuidsDefaultFilter), 1u);
        segmentHistory.add(createTravelTime(
            segmentId, time + pt::seconds(10), ma::VehicleId("a", "c"), 30, 1
        ));
        UNIT_ASSERT_EQUAL(segmentHistory.passings(time, noUuidsDefaultFilter), 2u);
    }

    Y_UNIT_TEST(PassingsFilter)
    {
        const auto segmentId = seg(0, 0);
        const std::unordered_set<std::string> uuids = {"c"};
        const auto defaultFilter = UuidFilter(uuids, JamsType::DEFAULT, true);
        const auto massTransitFilter = UuidFilter(uuids, JamsType::MASS_TRANSIT, true);
        const auto time = maps::nowUtc();

        SegmentHistory segmentHistory;
        segmentHistory.add(createTravelTime(
            segmentId, time + pt::seconds(60), ma::VehicleId("a", "a"), 30, 1
        ));
        UNIT_ASSERT_EQUAL(segmentHistory.passings(time, defaultFilter), 1u);
        UNIT_ASSERT_EQUAL(segmentHistory.passings(time, massTransitFilter), 0u);
        segmentHistory.add(createTravelTime(
            segmentId, time + pt::seconds(60), ma::VehicleId("a", "c"), 30, 1
        ));
        UNIT_ASSERT_EQUAL(segmentHistory.passings(time, defaultFilter), 1u);
        UNIT_ASSERT_EQUAL(segmentHistory.passings(time, massTransitFilter), 1u);
    }
}
