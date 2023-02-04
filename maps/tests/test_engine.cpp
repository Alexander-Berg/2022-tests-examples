#include <library/cpp/json/json_reader.h>
#include <library/cpp/json/writer/json_value.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/resource/resource.h>
#include <maps/analyzer/libs/consts/include/all.h>
#include <maps/analyzer/libs/realtime_jams/include/model.h>
#include <maps/analyzer/libs/realtime_jams/include/engine.h>
#include <maps/analyzer/libs/realtime_jams/include/feature_indices.h>
#include <maps/analyzer/libs/realtime_jams/include/features.h>
#include <maps/analyzer/libs/realtime_jams/include/target.h>
#include <maps/analyzer/libs/time_interpolator/include/iterative.h>
#include <util/generic/string.h>
#include <util/stream/file.h>
#include <util/stream/str.h>

#include <boost/date_time/posix_time/posix_time_types.hpp>

#include <algorithm>
#include <cmath>
#include <cstdio>
#include <exception>
#include <fstream>
#include <iostream>
#include <iterator>
#include <limits>
#include <memory>
#include <string>

namespace maps {
namespace analyzer {
namespace realtime_jams {

namespace pt = boost::posix_time;

const double PRECISION = 1e-4;

const auto GEOBASE_BIN_PATH = static_cast<std::string>(BinaryPath("maps/data/test/geobase/geodata5.bin"));
const TString DEFAULT_CONFIG = NResource::Find("/maps/analyzer/realtime_jams/engine_config.json");
const TString ALTERNATIVE_CONFIG_STRING = R"(
{
    "common_ratio": 0.5,
    "max_signal_count_for_calculation_with_model": 6,
    "apply_log_to_speeds": false,
    "take_timezone_into_account": false,
    "bin_bounds": [60, 120, 180, 240, 300, 360, 420],
    "common_ratio_inside_bins": 0.5,
    "min_spkm": 10.0,
    "max_spkm": 1000.0
})";

NJson::TJsonValue getDefaultConfig(const bool dropModel)
{
    TStringStream str(DEFAULT_CONFIG);
    auto config = NJson::ReadJsonTree(&str, true);
    if (dropModel) {
        config.EraseValue("model_path");
    }
    return config;
}

NJson::TJsonValue getAlternativeConfig()
{
    TStringInput str(ALTERNATIVE_CONFIG_STRING);
    return NJson::ReadJsonTree(&str, true);
}

const auto DEFAULT_CONFIG_WITHOUT_MODEL = getDefaultConfig(true);
const auto ALTERNATIVE_CONFIG = getAlternativeConfig();

const RealtimeJamsEngine& getDefaultEngineWithoutModel()
{
    static const RealtimeJamsEngine engine{DEFAULT_CONFIG_WITHOUT_MODEL};
    return engine;
}

const RealtimeJamsEngine& getAlternativeEngine()
{
    static const RealtimeJamsEngine engine{ALTERNATIVE_CONFIG};
    return engine;
}

void checkEngineFields(const RealtimeJamsEngine& engine, const NJson::TJsonValue& config)
{
    UNIT_ASSERT_EQUAL(
        config["common_ratio"].GetDouble(),
        engine.commonRatio()
    );
    UNIT_ASSERT(config.Has("model_path") ? engine.hasModel() : !engine.hasModel());
    UNIT_ASSERT_EQUAL(
        config["max_signal_count_for_calculation_with_model"].GetUInteger(),
        engine.maxSignalCountForCalculationWithModel()
    );
    UNIT_ASSERT_EQUAL(
        config["apply_log_to_speeds"].GetBoolean(),
        engine.applyLogToSpeeds()
    );
    UNIT_ASSERT_EQUAL(
        config["take_timezone_into_account"].GetBoolean(),
        engine.takeTimezoneIntoAccount()
    );
    const auto& binBounds = config["bin_bounds"].GetArray();
    for (size_t i = 0; i < binBounds.size(); ++i) {
        UNIT_ASSERT_EQUAL(pt::seconds(binBounds[i].GetUInteger()), engine.binBounds()[i]);
    }
    UNIT_ASSERT_EQUAL(
        config["common_ratio_inside_bins"].GetDouble(),
        engine.commonRatioInsideBins()
    );
    UNIT_ASSERT_EQUAL(
        config["min_spkm"].GetDouble(),
        engine.minSpkm()
    );
    UNIT_ASSERT_EQUAL(
        config["max_spkm"].GetDouble(),
        engine.maxSpkm()
    );
}

Y_UNIT_TEST_SUITE(TestConstructors)
{
    Y_UNIT_TEST(TestJsonConstructor)
    {
        const RealtimeJamsEngine engine{ALTERNATIVE_CONFIG};
        checkEngineFields(engine, ALTERNATIVE_CONFIG);
    }

    Y_UNIT_TEST(TestJsonConfigConstructor)
    {
        const std::string path = std::tmpnam(nullptr);
        try {
            std::ofstream file(path);
            file.write(ALTERNATIVE_CONFIG_STRING.data(), ALTERNATIVE_CONFIG_STRING.size());
            file.close();
            const RealtimeJamsEngine engine{path};
            checkEngineFields(engine, ALTERNATIVE_CONFIG);
        } catch (std::exception& err) {
            std::cerr << "[UNPLANNED EXCEPTION WAS THROWN] " << err.what() << std::endl;
            std::remove(path.c_str());
            throw;
        }
    }
};

Features makeStaticFeatures()
{
    // This is taken from maps/test/data/graph4 and /var/cache/geobase/geodata5.bin
    // for edge_id == 0 && segment_index == 0
    auto features = makeFeaturesTemplate();

    features[EDGE_ENDS_WITH_TRAFFIC_LIGHT] = false;
    features[EDGE_HAS_MASSTRANSIT_LANE] = false;
    features[EDGE_IS_TOLL] = false;
    features[EDGE_LENGTH] = 78.59088897705078;
    features[EDGE_SEGMENTS_NUMBER] = 1;
    features[EDGE_SPEED] = 4;
    features[EDGE_SPEED_LIMIT] = -1;
    features[EDGE_CATEGORY] = 8;
    features[EDGE_TYPE_0] = 1;
    features[EDGE_TYPE_1] = 0;
    features[EDGE_TYPE_2] = 0;
    features[EDGE_TYPE_3] = 0;
    features[EDGE_TYPE_4] = 0;
    features[EDGE_TYPE_5] = 0;
    features[EDGE_TYPE_6] = 0;
    features[EDGE_TYPE_7] = 0;
    features[EDGE_STRUCT_TYPE_0] = 1;
    features[EDGE_STRUCT_TYPE_1] = 0;
    features[EDGE_STRUCT_TYPE_2] = 0;

    features[SEGMENT_LENGTH] = 78.59183734186023;
    features[SEGMENT_START_LON] = 37.750149965286255;
    features[SEGMENT_START_LAT] = 55.59099138714373;

    // region_id == 119705
    features[REGION_TYPE] = 7;
    features[REGION_IS_MAIN] = true;
    features[REGION_LON_SIZE] = 0.034576;
    features[REGION_LAT_SIZE] = 0.016518;
    features[REGION_LON_SIZE_MILTIPLIED_BY_LAT_SIZE] = 571.126368;
    features[REGION_POPULATION] = 9778;
    features[REGION_POPULATION_DENSITY_APPROXIMATION] = 17.12055430786904;
    features[REGION_IS_ALMATY] = false;
    features[REGION_IS_ANKARA] = false;
    features[REGION_IS_MOSCOW] = false;
    features[REGION_IS_MOSCOW_REGION] = false;
    features[REGION_IS_OTHER] = true;
    features[REGION_IS_SAINT_PETERSBURG] = false;
    features[REGION_COUNTRY_IS_BELORUSSIA] = false;
    features[REGION_COUNTRY_IS_KAZAKHSTAN] = false;
    features[REGION_COUNTRY_IS_OTHER] = false;
    features[REGION_COUNTRY_IS_RUSSIA] = true;
    features[REGION_COUNTRY_IS_TURKEY] = false;
    features[REGION_COUNTRY_IS_UKRAINE] = false;

    return features;
}

const Features& getStaticFeatures()
{
    static const auto features = makeStaticFeatures();
    return features;
}

const NGeobase::TLookup& getGeobaseLookup()
{
    static const NGeobase::TLookup lookup(GEOBASE_BIN_PATH);
    return lookup;
}

// the function returs measurements sorted in the reversed order!
std::vector<travel_time::TravelTimeMeasurement> makeMeasurements(const pt::ptime queryTime)
{
    std::vector<travel_time::TravelTimeMeasurement> measurements;
    // bin_0
    measurements.push_back({queryTime - pt::seconds(45), 999999});
    // bin_1
    measurements.push_back({queryTime - pt::seconds(90), 20});
    measurements.push_back({queryTime - pt::seconds(105), 30});
    // bin_2 (empty)
    // bin_3
    measurements.push_back({queryTime - pt::seconds(190), 10});
    measurements.push_back({queryTime - pt::seconds(200), 20});
    measurements.push_back({queryTime - pt::seconds(210), 30});
    // bin_4 (empty)
    // bin_5
    measurements.push_back({queryTime - pt::seconds(310), 10});
    measurements.push_back({queryTime - pt::seconds(320), 20});
    // bin_6 (empty)
    return measurements;
}

std::vector<double> makeTravelTimesSpkm(
    const RealtimeJamsEngine& engine,
    const std::vector<travel_time::TravelTimeMeasurement>& measurements,
    const double geolength,
    const bool useClamp = true)
{
    std::vector<double> travelTimesSpkm;
    travelTimesSpkm.reserve(measurements.size());
    for (const auto& x: measurements) {
        auto spkm = x.travelTime / geolength * consts::METRES_PER_KILOMETRE;
        if (useClamp) {
            spkm = std::clamp(spkm, engine.minSpkm(), engine.maxSpkm());
        }
        travelTimesSpkm.push_back(spkm);
    }
    return travelTimesSpkm;
}

// expects travelTimesSpkm from measurements sorted in the reversed order
double makeInterpolated(
    const RealtimeJamsEngine& engine,
    const std::vector<double>& travelTimesSpkm)
{
    return time_interpolator::interpolateIterative(
        std::rbegin(travelTimesSpkm), std::rend(travelTimesSpkm), engine.commonRatio()
    );
}

Y_UNIT_TEST_SUITE(TestFeaturesCalculation)
{
    Y_UNIT_TEST(TestFillGraphEdgeFeatures)
    {
        auto features = makeFeaturesTemplate();
        const auto& etalonFeatures = getStaticFeatures();
        const auto& engine = getAlternativeEngine();
        engine.fillGraphEdgeFeatures(
            etalonFeatures[EDGE_ENDS_WITH_TRAFFIC_LIGHT],
            etalonFeatures[EDGE_HAS_MASSTRANSIT_LANE],
            etalonFeatures[EDGE_IS_TOLL],
            etalonFeatures[EDGE_LENGTH],
            etalonFeatures[EDGE_SEGMENTS_NUMBER],
            etalonFeatures[EDGE_SPEED],
            etalonFeatures[EDGE_SPEED_LIMIT],
            8,
            0,
            0,
            &features
        );
        #define CHECK_FEATURE(x) \
            UNIT_ASSERT_DOUBLES_EQUAL(features[x], etalonFeatures[x], PRECISION);
        CHECK_FEATURE(EDGE_ENDS_WITH_TRAFFIC_LIGHT);
        CHECK_FEATURE(EDGE_HAS_MASSTRANSIT_LANE);
        CHECK_FEATURE(EDGE_IS_TOLL);
        CHECK_FEATURE(EDGE_LENGTH);
        CHECK_FEATURE(EDGE_SEGMENTS_NUMBER);
        CHECK_FEATURE(EDGE_SPEED);
        CHECK_FEATURE(EDGE_SPEED_LIMIT);
        CHECK_FEATURE(EDGE_CATEGORY);
        CHECK_FEATURE(EDGE_TYPE_0);
        CHECK_FEATURE(EDGE_STRUCT_TYPE_0);
        #undef CHECK_FEATURE
    }

    Y_UNIT_TEST(TestFillGraphSegmentFeatures)
    {
        auto features = makeFeaturesTemplate();
        const auto& etalonFeatures = getStaticFeatures();
        const auto& engine = getAlternativeEngine();
        engine.fillGraphSegmentFeatures(
            etalonFeatures[SEGMENT_LENGTH],
            etalonFeatures[SEGMENT_START_LON],
            etalonFeatures[SEGMENT_START_LAT],
            &features
        );
        #define CHECK_FEATURE(x) \
            UNIT_ASSERT_DOUBLES_EQUAL(features[x], etalonFeatures[x], PRECISION);
        CHECK_FEATURE(SEGMENT_LENGTH);
        CHECK_FEATURE(SEGMENT_START_LON);
        CHECK_FEATURE(SEGMENT_START_LAT);
        #undef CHECK_FEATURE
    }

    Y_UNIT_TEST(TestFillRegionFeatures)
    {
        auto features = makeFeaturesTemplate();
        const auto& etalonFeatures = getStaticFeatures();
        const auto& engine = getAlternativeEngine();
        engine.fillRegionFeatures(119705, getGeobaseLookup(), &features);
        #define CHECK_FEATURE(x) \
            UNIT_ASSERT_DOUBLES_EQUAL(features[x], etalonFeatures[x], PRECISION);
        CHECK_FEATURE(REGION_TYPE);
        CHECK_FEATURE(REGION_IS_MAIN);
        CHECK_FEATURE(REGION_LON_SIZE);
        CHECK_FEATURE(REGION_LAT_SIZE);
        CHECK_FEATURE(REGION_LON_SIZE_MILTIPLIED_BY_LAT_SIZE);
        CHECK_FEATURE(REGION_POPULATION);
        CHECK_FEATURE(REGION_POPULATION_DENSITY_APPROXIMATION);
        CHECK_FEATURE(REGION_IS_ALMATY);
        CHECK_FEATURE(REGION_IS_ANKARA);
        CHECK_FEATURE(REGION_IS_MOSCOW);
        CHECK_FEATURE(REGION_IS_MOSCOW_REGION);
        CHECK_FEATURE(REGION_IS_OTHER);
        CHECK_FEATURE(REGION_IS_SAINT_PETERSBURG);
        CHECK_FEATURE(REGION_COUNTRY_IS_BELORUSSIA);
        CHECK_FEATURE(REGION_COUNTRY_IS_KAZAKHSTAN);
        CHECK_FEATURE(REGION_COUNTRY_IS_OTHER);
        CHECK_FEATURE(REGION_COUNTRY_IS_RUSSIA);
        CHECK_FEATURE(REGION_COUNTRY_IS_TURKEY);
        CHECK_FEATURE(REGION_COUNTRY_IS_UKRAINE);
        #undef CHECK_FEATURE
    }

    Y_UNIT_TEST(TestFillQueryTimeFeatures)
    {
        auto features = makeFeaturesTemplate();
        // two engines that treat the parameter 'offset' differently
        const auto& alternativeEngine = getAlternativeEngine();
        const auto& defaultEngine = getDefaultEngineWithoutModel();

        const pt::ptime queryTime(
            boost::gregorian::date(2018, boost::date_time::Apr, 1), pt::hours(2)
        );
        const auto offset = pt::hours(1);

        alternativeEngine.fillQueryTimeFeatures(queryTime, offset, &features);
        UNIT_ASSERT_DOUBLES_EQUAL(features[TIME_MINUTES_COS], 0.8660254037844387, PRECISION);
        UNIT_ASSERT_DOUBLES_EQUAL(features[TIME_MINUTES_SIN], 0.5, PRECISION);
        UNIT_ASSERT_EQUAL(features[TIME_WEEKDAY], 0);

        features = makeFeaturesTemplate();
        defaultEngine.fillQueryTimeFeatures(queryTime, offset, &features);
        UNIT_ASSERT_DOUBLES_EQUAL(features[TIME_MINUTES_COS], 0.9659258262890683, PRECISION);
        UNIT_ASSERT_DOUBLES_EQUAL(features[TIME_MINUTES_SIN], 0.25881904510252074, PRECISION);
        UNIT_ASSERT_EQUAL(features[TIME_WEEKDAY], 0);
    }

    Y_UNIT_TEST(TestFillTravelTimesFeatures)
    {
        const auto& engine = getAlternativeEngine();
        auto mayBeLog = [&engine](const double x) {
            return engine.applyLogToSpeeds() ? std::log(x) : x;
        };

        auto features = makeFeaturesTemplate();
        const double geolength = 100;

        const pt::ptime queryTime(
            boost::gregorian::date(2018, boost::date_time::Apr, 1), pt::hours(2)
        );
        auto measurements = makeMeasurements(queryTime);
        const auto travelTimesSpkm = makeTravelTimesSpkm(engine, measurements, geolength);
        std::sort(std::begin(measurements), std::end(measurements));

        engine.fillTravelTimesFeatures(queryTime, measurements, geolength, &features);

        UNIT_ASSERT_EQUAL(features[TRAVEL_TIMES_FRESHNESS], 45);
        UNIT_ASSERT_EQUAL(features[TRAVEL_TIMES_COUNT], measurements.size());

        const auto exponentialAverage = makeInterpolated(engine, travelTimesSpkm);
        UNIT_ASSERT_DOUBLES_EQUAL(
            features[TRAVEL_TIMES_EXPONENTIAL_AVERAGE],
            mayBeLog(exponentialAverage),
            PRECISION
        );

        const auto alpha = engine.commonRatioInsideBins();
        UNIT_ASSERT_DOUBLES_EQUAL(features[TRAVEL_TIMES_BIN_SPEED_0], engine.maxSpkm(), PRECISION);
        UNIT_ASSERT_DOUBLES_EQUAL(
            features[TRAVEL_TIMES_BIN_SPEED_1],
            mayBeLog((alpha * travelTimesSpkm[2] + travelTimesSpkm[1]) / (alpha + 1)),
            PRECISION
        );
        UNIT_ASSERT_EQUAL(features[TRAVEL_TIMES_BIN_SPEED_2], -1);
        UNIT_ASSERT_DOUBLES_EQUAL(
            features[TRAVEL_TIMES_BIN_SPEED_3],
            mayBeLog(
                (alpha * (alpha * travelTimesSpkm[5] + travelTimesSpkm[4]) + travelTimesSpkm[3]) /
                (alpha * (alpha + 1) + 1)
            ),
            PRECISION
        );
        UNIT_ASSERT_EQUAL(features[TRAVEL_TIMES_BIN_SPEED_4], -1);
        UNIT_ASSERT_DOUBLES_EQUAL(
            features[TRAVEL_TIMES_BIN_SPEED_5],
            mayBeLog((alpha * travelTimesSpkm[7] + travelTimesSpkm[6]) / (alpha + 1)),
            PRECISION
        );
        UNIT_ASSERT_EQUAL(features[TRAVEL_TIMES_BIN_SPEED_6], -1);

        UNIT_ASSERT_EQUAL(features[TRAVEL_TIMES_EMPTY_BIN_COUNT], 3);
        UNIT_ASSERT_EQUAL(features[TRAVEL_TIMES_BIN_SIZE_0], 1);
        UNIT_ASSERT_EQUAL(features[TRAVEL_TIMES_BIN_SIZE_1], 2);
        UNIT_ASSERT_EQUAL(features[TRAVEL_TIMES_BIN_SIZE_2], 0);
        UNIT_ASSERT_EQUAL(features[TRAVEL_TIMES_BIN_SIZE_3], 3);
        UNIT_ASSERT_EQUAL(features[TRAVEL_TIMES_BIN_SIZE_4], 0);
        UNIT_ASSERT_EQUAL(features[TRAVEL_TIMES_BIN_SIZE_5], 2);
        UNIT_ASSERT_EQUAL(features[TRAVEL_TIMES_BIN_SIZE_6], 0);
    }
}

class DummyModel : public Model {
public:
    static constexpr double value = 23;

    double DoCalcRelev(const float*) const override
    {
        return value;
    }
};

Y_UNIT_TEST_SUITE(TestCalculations)
{
    Y_UNIT_TEST(TestCalculateWithModel)
    {
        RealtimeJamsEngine engine{ALTERNATIVE_CONFIG};
        engine.setModel(std::make_unique<DummyModel>());

        auto features = makeFeaturesTemplate();
        const double geolength = 100;

        const auto travelTime = DummyModel::value / consts::METRES_PER_KILOMETRE * geolength;

        UNIT_ASSERT_DOUBLES_EQUAL(
            travelTime, engine.calculateWithModel(features, geolength), PRECISION
        );
    }

    Y_UNIT_TEST(TestCalculateWithInterpolation)
    {
        const auto& engine = getAlternativeEngine();

        const auto geolength = getStaticFeatures()[SEGMENT_LENGTH];

        const pt::ptime queryTime(
            boost::gregorian::date(2018, boost::date_time::Apr, 1), pt::hours(2)
        );
        auto measurements = makeMeasurements(queryTime);
        const auto travelTimesSpkm = makeTravelTimesSpkm(engine, measurements, geolength, false);
        std::sort(std::begin(measurements), std::end(measurements));

        const auto exponentialAverage = makeInterpolated(engine, travelTimesSpkm);
        const auto travelTime = exponentialAverage / consts::METRES_PER_KILOMETRE * geolength;

        UNIT_ASSERT_DOUBLES_EQUAL(
            travelTime, engine.calculateWithInterpolation(measurements), PRECISION
        );
    }
}

Y_UNIT_TEST_SUITE(TestOtherMethods)
{
    Y_UNIT_TEST(TestSetModel)
    {
        RealtimeJamsEngine engine{ALTERNATIVE_CONFIG};
        UNIT_ASSERT(!engine.hasModel());
        engine.setModel(std::make_unique<DummyModel>());
        UNIT_ASSERT(engine.hasModel());
    }

    Y_UNIT_TEST(TestSetCommonRatio)
    {
        RealtimeJamsEngine engine{ALTERNATIVE_CONFIG};
        engine.setCommonRatio(0);
        UNIT_ASSERT_EQUAL(engine.commonRatio(), 0);
        engine.setCommonRatio(1);
        UNIT_ASSERT_EQUAL(engine.commonRatio(), 1);
    }

    Y_UNIT_TEST(TestSetMaxSignalCountForCalculationWithModel)
    {
        RealtimeJamsEngine engine{ALTERNATIVE_CONFIG};
        engine.setMaxSignalCountForCalculationWithModel(3);
        UNIT_ASSERT_EQUAL(engine.maxSignalCountForCalculationWithModel(), 3);
        engine.setMaxSignalCountForCalculationWithModel(4);
        UNIT_ASSERT_EQUAL(engine.maxSignalCountForCalculationWithModel(), 4);
    }

    Y_UNIT_TEST(TestBuildTarget)
    {
        const auto& alternativeEngine = getAlternativeEngine();
        const auto& defaultEngine = getDefaultEngineWithoutModel();

        UNIT_ASSERT_DOUBLES_EQUAL(
            alternativeEngine.buildTarget(10, 100), 100, PRECISION
        );
        UNIT_ASSERT_DOUBLES_EQUAL(
            defaultEngine.buildTarget(10, 100), std::log(100), PRECISION
        );

        UNIT_ASSERT_DOUBLES_EQUAL(
            alternativeEngine.buildTarget(999999, 100), alternativeEngine.maxSpkm(), PRECISION
        );
        UNIT_ASSERT_DOUBLES_EQUAL(
            defaultEngine.buildTarget(999999, 100), std::log(defaultEngine.maxSpkm()), PRECISION
        );

        UNIT_ASSERT_DOUBLES_EQUAL(
            alternativeEngine.buildTarget(0.001, 100), alternativeEngine.minSpkm(), PRECISION
        );
        UNIT_ASSERT_DOUBLES_EQUAL(
            defaultEngine.buildTarget(0.001, 100), std::log(defaultEngine.minSpkm()), PRECISION
        );
    }

    Y_UNIT_TEST(TestIsSampleSuitableForTraining)
    {
        const auto& engine = getAlternativeEngine();
        auto features = makeFeaturesTemplate();
        UNIT_ASSERT(
            engine.isSampleSuitableForTraining(engine.applyLogToSpeeds() ? 4.6 : 100, features)
        );
        features[TRAVEL_TIMES_COUNT] = 9999;
        UNIT_ASSERT(
            !engine.isSampleSuitableForTraining(engine.applyLogToSpeeds() ? 4.6 : 100, features)
        );
        features = makeFeaturesTemplate();
        UNIT_ASSERT(
            !engine.isSampleSuitableForTraining(0, features)
        );
        UNIT_ASSERT(
            !engine.isSampleSuitableForTraining(std::numeric_limits<Target>::max(), features)
        );
    }
};

} // realtime_jams
} // analyzer
} // maps
