#include "data.h"

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/analyzer/libs/masstransit/time_prediction/features.h>
#include <maps/analyzer/libs/masstransit/time_prediction/model_config.h>
#include <maps/analyzer/libs/masstransit/time_prediction/util.h>

#include <string>

using namespace maps::masstransit::time_prediction;

constexpr double EPS = 1e-5;

Y_UNIT_TEST_SUITE(TestMasstransitFeatures) {
    const auto cfg = ModelConfig::fromString(R"({
        "features": {
            "common": {
                "categorial": [
                    "THREAD",
                    "ROUTE",
                    "VEHICLE_TYPE"
                ],
                "numerical": []
            },
            "signal": {
                "categorial": [],
                "numerical": [
                    "LOCAL_DAY_TYPE",
                    "LOCAL_DAY_HOUR",
                    "REGION"
                ]
            },
            "passed_track_part": {
                "categorial": [],
                "numerical": [
                    "LENGTH",
                    "SPEED",
                    "STOPS_COUNT"
                ]
            },
            "predicted_segment": {
                "categorial": [
                    "TYPE"
                ],
                "numerical": [
                    "LENGTH",
                    "CURVATIVE",
                    "LAT",
                    "LON",
                    "FORECAST_OFFSET",
                    "DISTANCE_FROM_PREV_STOP",
                    "DISTANCE_TO_NEXT_STOP",
                    "STOPS_FROM_FORECAST",
                    "RG_COVERED_RATIO",
                    "RG_MTLANES_LENGTH",
                    "RG_TRAFFIC_LIGHTS_COUNT",
                    "RG_DEFAULT_SPEED",
                    "RG_SPEED_LIMIT",
                    "JAMS_SPEED",
                    "JAMS_COVERAGE"
                ]
            }
        },
        "normalization": "LOG_SPKM",
        "part_lengths": [100, 200],
        "max_forecast_horizon": 1800,
        "stop_duration": 7
    })");

    Y_UNIT_TEST(TestFeatures) {
        auto storage = ml::FeaturesStorage::fromConfig(cfg);

        ml::calcFeatures<Common>(
            storage, cfg,
            Common::Data{
                threadId,
                threadData.data.routeId(),
                threadData.data.vehicleType()
            }
        );

        ml::calcFeatures<Signal>(
            storage, cfg,
            Signal::Data{
                1600000000,
                43,
                geobase,
                &calendar
            }
        );

        TrackPartsSequence passedTrack{
            threadData,
            cfg.partsLengthLimits
        };

        passedTrack.pushBack({{0, 0., 1.}, 7.});
        passedTrack.pushBack({{1, 0., 1.}, 5.});
        passedTrack.pushBack({{2, 0., .3}, 10.});
        passedTrack.pushBack({{2, .3, 1.}, 25.});
        passedTrack.pushBack({{3, 0., .1}, 5.});
        passedTrack.pushBack({{3, .1, .9}, 40.});
        passedTrack.pushBack({{3, .9, 1.}, 10.});

        calcPassedTrackPartsSequenceFeatures(
            storage,
            cfg,
            passedTrack,
            threadData
        );

        const EdgeRgTraits rgTraits {
            .coveredRatio = 0.5,
            .mtLanesLength = 25,
            .trafficLightsCount = 0,
            .defaultSpeed = 10,
            .speedLimit = std::nullopt
        };

        const EdgeJam jams {
            .speed = 5,
            .coverage = 0.7
        };

        ml::calcFeatures<PredictedSegment>(
            storage, cfg,
            PredictedSegment::Data{
                EdgePart{4, 0., 1.},
                ThreadBoundPoint{{4, 0.}, 1600000000},
                threadData,
                nullptr, // statistical data
                &calendar,
                rgTraits,
                jams,
                0,
                0
            }
        );

        const std::vector<std::string> expectedCats{
            "43A_57_minibus_default",
            "43_57_minibus_default",
            "minibus",
            "StopSegment"
        };

        const std::vector<double> expectedNums{
            6., // local day type
            15., // local day hour
            43., // region
            100., // passed track pt0 length
            3.74417, // passed track pt0 speed
            0., // passed pt0 stops count
            200., // passed track pt1 length
            5.11573, // passed track pt1 speed
            0., // passed pt1 stops count
            25.01951, // predicted segment length
            0., // predicted segment curvative
            55.84, // predicted segment lat
            49.03, // predicted segment lon
            0., // forecast offest
            485.65586, // distance from previous stop
            0., // distance to next stop
            0., // stops from current position
            0.5, // road graph covered ratio
            25, // road graph masstransit lanes length
            0, // road graph traffic lights count
            10, // road graph default tspeed
            -1, // road graph speed limit
            5, // jams speed
            0.7 // jams coverage
        };

        EXPECT_EQ(storage.cats.size(), expectedCats.size());
        for (std::size_t i = 0; i < expectedCats.size(); ++i) {
            EXPECT_EQ(storage.cats[i], expectedCats[i]);
        }

        EXPECT_EQ(storage.nums.size(), expectedNums.size());
        for (std::size_t i = 0; i < expectedNums.size(); ++i) {
            EXPECT_NEAR(storage.nums[i], expectedNums[i], EPS);
        }
    }
}
