#include <maps/analyzer/libs/external/geo/include/geo.h>
#include <maps/analyzer/libs/external/geo/include/types.h>
#include <maps/analyzer/libs/mapmatching_likelihoods/core/include/likelihoods.h>
#include <maps/analyzer/libs/mapmatching_likelihoods/core/tests/conf/likelihoods_config.h>
#include <maps/libs/geolib/include/segment.h>
#include <maps/libs/geolib/include/units.h>
#include <maps/libs/geolib/include/vector.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <string>


using namespace maps::analyzer::mapmatching_likelihoods;
using namespace maps::analyzer::external::geo;

const std::string LIKELIHOODS_CONFIG_XML = ArcadiaSourceRoot() +
    "/maps/analyzer/libs/mapmatching_likelihoods/load/tests/conf/likelihoods_conf.xml";

const std::string LIKELIHOODS_CONFIG_JSON = ArcadiaSourceRoot() +
    "/maps/analyzer/libs/mapmatching_likelihoods/load/tests/conf/likelihoods_conf.json";

inline double oldDirectional(
    double direction,
    double speed,
    const maps::geolib3::Segment2& segment,
    const DirectionalConfig& cfg
) {
    if (!cfg.enabled || cfg.k == 0)
        return 1.0;
    if (direction < 0 || direction > 360 || speed <= 1.0)
        return 1.0;
    if (segment.isDegenerate())
        return 1.0;
    auto segmentDir = segment.vector();
    const double cosLat = std::cos(maps::geolib3::degreesToRadians(segment.start().y()));
    segmentDir = maps::geolib3::Vector2(segmentDir.x() * cosLat, segmentDir.y());
    direction = maps::geolib3::degreesToRadians(direction);
    const auto moveDir = maps::geolib3::Vector2(std::sin(direction), std::cos(direction));
    const double cosAlpha = maps::geolib3::innerProduct(segmentDir, moveDir) / maps::geolib3::length(segmentDir);
    return std::pow((1 + cosAlpha) / 2, cfg.k);
}

inline double oldJump(
    const maps::geolib3::Point2& prevCandPt,
    const maps::geolib3::Point2& prevSignPt,
    const maps::geolib3::Point2& newCandPt,
    const maps::geolib3::Point2& newSignPt,
    const JumpConfig& cfg
) {
    maps::geolib3::Vector2 prevSigToCand = prevCandPt - prevSignPt;
    maps::geolib3::Vector2 newSigToCand = newCandPt - newSignPt;

    const double linDiff = maps::geolib3::geoLength(maps::geolib3::Segment2(prevSignPt, prevSigToCand - newSigToCand));

    const double magic = exp(-linDiff / cfg.norm);
    const double res = (1.0 - cfg.min) * magic + cfg.min;

    REQUIRE(0.0 <= res && res <= 1.0, "Incorrect jump probability");
    return res;
}

void test(const LikelihoodsConfig& lhoodConfig) {
    const auto geom = geometricLikelihood(10., lhoodConfig.geometric);
    EXPECT_GT(geom, 0);

    EXPECT_FALSE(lhoodConfig.directional.enabled);
    EXPECT_EQ(lhoodConfig.directional.k, 0.0);

    const auto oldDir = oldDirectional(
        26, 15,
        {position(31.2514, 58.5265), position(31.2514, 58.5265)},
        lhoodConfig.directional
    );

    const auto directional = directionalLikelihood(
        26, 15,
        {position(31.2514, 58.5265), position(31.2514, 58.5265)},
        lhoodConfig.directional
    );
    EXPECT_EQ(directional, oldDir);


    const auto skipped = skippedLikelihood(2, lhoodConfig.skipped);
    EXPECT_GT(skipped, 0);
    const auto temporal = temporalLikelihood(100, 10, 12, 1, lhoodConfig.temporal);
    EXPECT_GE(temporal, 0);
    const auto speed = speedLikelihood(100, 1, 30, 25, lhoodConfig.speed);
    EXPECT_GT(speed, 0);
    const auto curvative = curvativeLikelihood(12, 100, 1, lhoodConfig.curvative);
    EXPECT_GT(curvative, 0);
    const auto transmission = transmissionLikelihood(100, 20, lhoodConfig.transmission);
    EXPECT_GT(transmission, 0);

    const auto jump = jumpLikelihood(
        position(31.254358, 58.528625),
        position(31.254649, 58.528996),
        position(31.253984, 58.528149),
        position(31.253649, 58.527723),
        lhoodConfig.jump
    );

    const auto oldJumpProb = oldJump(
        position(31.254358, 58.528625),
        position(31.254649, 58.528996),
        position(31.253984, 58.528149),
        position(31.253649, 58.527723),
        lhoodConfig.jump
    );
    EXPECT_EQ(jump, oldJumpProb);
}

TEST(CalculatorTest, Calculator) {
    test(likelihoodsConfig);
}
