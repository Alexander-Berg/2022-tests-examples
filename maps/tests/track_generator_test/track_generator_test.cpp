#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <maps/analyzer/libs/track_generator/include/test_generator_standing.h>
#include <maps/analyzer/libs/track_generator/include/generators_factory.h>
#include <maps/analyzer/libs/track_generator/include/path_generator_config.h>
#include <maps/analyzer/libs/track_generator/include/driving_config.h>
#include <maps/analyzer/libs/track_generator/include/test_config.h>
#include <maps/analyzer/libs/track_generator/include/test_data.h>

#include <maps/libs/geolib/include/bounding_box.h>
#include <maps/libs/geolib/include/distance.h>
#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/polyline.h>
#include <maps/libs/geolib/include/spatial_relation.h>
#include <maps/libs/road_graph/include/graph.h>
#include <maps/libs/xml/include/xml.h>

#include <string>
#include <fstream>
#include <vector>


using namespace maps::analyzer::track_generator;
using namespace maps::analyzer::shortest_path;
using maps::analyzer::data::GpsSignal;
using maps::analyzer::data::GpsSignals;


const std::string ROAD_GRAPH_PATH = static_cast<std::string>(BinaryPath("maps/data/test/graph3/road_graph.fb"));
const std::string RTREE_PATH = static_cast<std::string>(BinaryPath("maps/data/test/graph3/rtree.fb"));
const std::string TEST_DATA_ROOT = ArcadiaSourceRoot() + "/maps/analyzer/libs/track_generator/tests/data/";


class TrackGeneratorTester: public ::NUnitTest::TBaseFixture {
public:
    void checkPathSatisfiesPathConfig()
    {
        maps::road_graph::Graph graph(ROAD_GRAPH_PATH);
        maps::succinct_rtree::Rtree rtree(RTREE_PATH, graph);

        constexpr double LL_X = 37.5, LL_Y = 55.6;
        constexpr double RU_X = 38.5, RU_Y = 56.8;
        constexpr double MIN_PATH_LENGTH = 300;
        constexpr double MAX_PATH_LENGTH = 2000;
        // we make indention, because path can leave original
        // bounding box
        PathGeneratorConfig pathConfig{
            .minPathLength = MIN_PATH_LENGTH,
            .maxPathLength = MAX_PATH_LENGTH,
            .boundingBox = {
                maps::geolib3::Point2{LL_X, LL_Y},
                maps::geolib3::Point2{RU_X, RU_Y},
            },
        };

        std::cerr << "Setting config..." << std::endl;
        maps::xml3::Doc doc(TEST_DATA_ROOT + "driving_config.xml", maps::xml3::Doc::File);
        auto drivingConfig = DrivingConfig::fromXml(doc.root());
        checkExceptions(drivingConfig);

        constexpr std::size_t TESTS_NUMBER = 10;

        TestConfig config{
            .pathGeneratorConfig = pathConfig,
            .drivingConfig = drivingConfig,
            .testsNumber = TESTS_NUMBER,
            .trackType = "edge_speed_limit",
        };

        constexpr double INDENT_FOR_BB_EDGE_SPEED = 0.05000; //5 km
        constexpr double INDENT_FOR_BB_STANDING = 0.00300; // 0.3 km

        checkGenerator(config, graph, rtree, INDENT_FOR_BB_EDGE_SPEED, 1);
        config.trackType = "standing";
        checkGenerator(config, graph, rtree, INDENT_FOR_BB_STANDING, 2);
        config.trackType = "no_speed_bounds";
        checkGenerator(config, graph, rtree, INDENT_FOR_BB_EDGE_SPEED, 3);
    }

private:
    void checkExceptions(DrivingConfig config) {
        std::cerr << "Checking exceptions..." << std::endl;

        const auto validateSet = [&](auto&& fn) {
            auto cfg = config;
            fn(cfg);
            cfg.validate();
        };

        EXPECT_THROW(validateSet([](auto& cfg) { cfg.minTimeInterval = -1; }), maps::Exception);
        EXPECT_THROW(validateSet([](auto& cfg) { cfg.minTimeInterval = 0; }), maps::Exception);
        config.maxTimeInterval = 9;
        EXPECT_THROW(validateSet([](auto& cfg) { cfg.minTimeInterval = 10; }), maps::Exception);
        config.minTimeInterval = 9;
        EXPECT_THROW(validateSet([](auto& cfg) { cfg.maxTimeInterval = 8; }), maps::Exception);
        EXPECT_THROW(validateSet([](auto& cfg) { cfg.startTimeInterval = 0; }), maps::Exception);
        config.startSpeed = 0.0;
        EXPECT_THROW(validateSet([](auto& cfg) { cfg.startSpeed = -0.001; }), maps::Exception);
        EXPECT_THROW(validateSet([](auto& cfg) { cfg.minVehicleSpeed = -0.001; }), maps::Exception);
        EXPECT_THROW(validateSet([](auto& cfg) { cfg.maxVehicleSpeed = -0.001; }), maps::Exception);
        config.minVehicleSpeed = 10;
        EXPECT_THROW(validateSet([](auto& cfg) { cfg.maxVehicleSpeed = 1; }), maps::Exception);
        config.maxVehicleSpeed = 10;
        EXPECT_THROW(validateSet([](auto& cfg) { cfg.minVehicleSpeed = 11; }), maps::Exception);
    }

    bool isWithinBoundingBox(
        const Track& track,
        const maps::geolib3::BoundingBox& boundingBox
    ) {
        const GpsSignals& signals = track.gpsSignals;
        for (std::size_t i = 0; i < signals.size(); ++i) {
            if (!maps::geolib3::spatialRelation(boundingBox, signals[i].point(), maps::geolib3::Contains)) {
                return false;
            }
        }
        return true;
    }

    void checkWithinPathLengthInterval(
        const PathGeneratorConfig& pathConfig,
        const TestData& test, const maps::road_graph::Graph&
    ) {
        const MatchedPaths& matching = test.correctMatching;
        double len = 0.0;
        for (const auto& matched: matching) {
            if (matched.path.has_value()) {
                len += matched.path.value().info.length;
            }
        }
        constexpr double ERROR = 5;
        // metres, because of transformations between 2d and sphere coordinates

        EXPECT_TRUE(pathConfig.minPathLength < len + ERROR);
        EXPECT_TRUE(pathConfig.maxPathLength + ERROR > len);
    }

    const maps::geolib3::BoundingBox resizeByValue(
        const maps::geolib3::BoundingBox& box, double value
    ) {
        const double newWidth = box.width() + 2.0 * value;
        const double newHeight = box.height() + 2.0 * value;
        REQUIRE(
            newWidth >= 0.0 && newHeight >= 0.0,
            "Resizing bounding box by value led to negative width (" << newWidth << ") or height (" << newHeight << ")"
        );
        return maps::geolib3::BoundingBox(box.center(), newWidth, newHeight);
    }

    void checkGenerator(
        const TestConfig& config,
        const maps::road_graph::Graph& graph,
        const maps::succinct_rtree::Rtree& rtree,
        const double indent, uint32_t seed
    ) {
        boost::mt19937 randEngine(seed);
        GeneratorsFactory factory(graph, rtree);
        auto tgen = factory.generator(config, randEngine);
        TestDataSet testset;
        std::cerr << "Generating tests for " << config.trackType << std::endl;
        tgen->gen(&testset);

        const maps::geolib3::BoundingBox resizedBoudingBox = resizeByValue(
            config.pathGeneratorConfig.boundingBox, indent
        );
        for (const auto& t: testset.tests) {
            if (config.trackType != "standing") {
                checkWithinPathLengthInterval(
                    config.pathGeneratorConfig,
                    t, graph
                );
            }
            EXPECT_TRUE(isWithinBoundingBox(t.track, resizedBoudingBox));
        }
    }
};

Y_UNIT_TEST_SUITE_F(TrackGenerator, TrackGeneratorTester) {
    Y_UNIT_TEST(PathSatisfiesPathConfig) {
        checkPathSatisfiesPathConfig();
    }
}
