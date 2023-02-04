#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <mapreduce/yt/interface/client.h>

#include <maps/libs/geolib/include/const.h>
#include <maps/libs/geolib/include/polygon.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>

#include <maps/wikimap/mapspro/services/autocart/libs/geometry/include/hex_wkb.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/mpro/include/region.h>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

Y_UNIT_TEST_SUITE(region_tests)
{

Y_UNIT_TEST(yt_node_test)
{
    const geolib3::Polygon2 geoGeom({{0., 0.}, {1., 0.}, {1., 1.}, {0., 1.}});
    const NYT::TNode REGION_NODE = NYT::TNode()
        ("bld_recognition_region:name", "name")
        ("bld_recognition_region:shape", TString(polygonToHexWKB(geoGeom)))
        ("bld_recognition_region:use_dwellplaces", false)
        ("bld_recognition_region:use_tolokers", true)
        ("bld_recognition_region:use_assessors", false)
        ("bld_recognition_region:is_active", true)
        ("bld_recognition_region:object_id", 7111917u)
        ("bld_recognition_region:commit_id", 8111917u);

    BldRecognitionRegion region = BldRecognitionRegion::fromYTNode(REGION_NODE);

    NYT::TNode testRegionNode = region.toYTNode();

    EXPECT_EQ(
        REGION_NODE["bld_recognition_region:name"].AsString(),
        testRegionNode["bld_recognition_region:name"].AsString()
    );
    EXPECT_TRUE(
        geolib3::test_tools::approximateEqual(
            hexWKBToPolygon(REGION_NODE["bld_recognition_region:shape"].AsString()),
            hexWKBToPolygon(testRegionNode["bld_recognition_region:shape"].AsString()),
            geolib3::EPS
        )
    );
    EXPECT_EQ(
        REGION_NODE["bld_recognition_region:use_dwellplaces"].AsBool(),
        testRegionNode["bld_recognition_region:use_dwellplaces"].AsBool()
    );
    EXPECT_EQ(
        REGION_NODE["bld_recognition_region:use_tolokers"].AsBool(),
        testRegionNode["bld_recognition_region:use_tolokers"].AsBool()
    );
    EXPECT_EQ(
        REGION_NODE["bld_recognition_region:use_assessors"].AsBool(),
        testRegionNode["bld_recognition_region:use_assessors"].AsBool()
    );
    EXPECT_EQ(
        REGION_NODE["bld_recognition_region:is_active"].AsBool(),
        testRegionNode["bld_recognition_region:is_active"].AsBool()
    );
    EXPECT_EQ(
        REGION_NODE["bld_recognition_region:object_id"].AsUint64(),
        testRegionNode["bld_recognition_region:object_id"].AsUint64()
    );
    EXPECT_EQ(
        REGION_NODE["bld_recognition_region:commit_id"].AsUint64(),
        testRegionNode["bld_recognition_region:commit_id"].AsUint64()
    );
}

Y_UNIT_TEST(remove_inactive_test)
{
    const geolib3::Polygon2 geoGeom({{0., 0.}, {1., 0.}, {1., 1.}, {0., 1.}});
    const std::vector<NYT::TNode> regionNodes{
        NYT::TNode()
            ("bld_recognition_region:name", "name1")
            ("bld_recognition_region:shape", TString(polygonToHexWKB(geoGeom)))
            ("bld_recognition_region:use_dwellplaces", false)
            ("bld_recognition_region:use_tolokers", true)
            ("bld_recognition_region:use_assessors", false)
            ("bld_recognition_region:is_active", true)
            ("bld_recognition_region:object_id", 7111917u)
            ("bld_recognition_region:commit_id", 8111917u),
        NYT::TNode()
            ("bld_recognition_region:name", "name2")
            ("bld_recognition_region:shape", TString(polygonToHexWKB(geoGeom)))
            ("bld_recognition_region:use_dwellplaces", false)
            ("bld_recognition_region:use_tolokers", true)
            ("bld_recognition_region:use_assessors", false)
            ("bld_recognition_region:is_active", false)
            ("bld_recognition_region:object_id", 7111917u)
            ("bld_recognition_region:commit_id", 8111917u),
        NYT::TNode()
            ("bld_recognition_region:name", "name3")
            ("bld_recognition_region:shape", TString(polygonToHexWKB(geoGeom)))
            ("bld_recognition_region:use_dwellplaces", false)
            ("bld_recognition_region:use_tolokers", true)
            ("bld_recognition_region:use_assessors", false)
            ("bld_recognition_region:is_active", true)
            ("bld_recognition_region:object_id", 7111917u)
            ("bld_recognition_region:commit_id", 8111917u),
    };

    BldRecognitionRegions regions;
    for (const NYT::TNode& regionNode : regionNodes) {
        regions.push_back(BldRecognitionRegion::fromYTNode(regionNode));
    }

    removeInactiveRegions(regions);

    EXPECT_EQ(regions.size(), 2u);
    EXPECT_EQ(regions[0].name(), "name1");
    EXPECT_EQ(regions[1].name(), "name3");
}

Y_UNIT_TEST(remove_used_test)
{
    const geolib3::Polygon2 geoGeom({{0., 0.}, {1., 0.}, {1., 1.}, {0., 1.}});
    const std::vector<NYT::TNode> regionNodes{
        NYT::TNode()
            ("bld_recognition_region:name", "name1")
            ("bld_recognition_region:shape", TString(polygonToHexWKB(geoGeom)))
            ("bld_recognition_region:use_dwellplaces", false)
            ("bld_recognition_region:use_tolokers", true)
            ("bld_recognition_region:use_assessors", false)
            ("bld_recognition_region:is_active", true)
            ("bld_recognition_region:object_id", 7111917u)
            ("bld_recognition_region:commit_id", 8111917u),
        NYT::TNode()
            ("bld_recognition_region:name", "name2")
            ("bld_recognition_region:shape", TString(polygonToHexWKB(geoGeom)))
            ("bld_recognition_region:use_dwellplaces", false)
            ("bld_recognition_region:use_tolokers", true)
            ("bld_recognition_region:use_assessors", false)
            ("bld_recognition_region:is_active", false)
            ("bld_recognition_region:object_id", 7111917u)
            ("bld_recognition_region:commit_id", 8111917u),
        NYT::TNode()
            ("bld_recognition_region:name", "name3")
            ("bld_recognition_region:shape", TString(polygonToHexWKB(geoGeom)))
            ("bld_recognition_region:use_dwellplaces", false)
            ("bld_recognition_region:use_tolokers", true)
            ("bld_recognition_region:use_assessors", false)
            ("bld_recognition_region:is_active", true)
            ("bld_recognition_region:object_id", 7111917u)
            ("bld_recognition_region:commit_id", 8111917u),
    };

    BldRecognitionRegions regions;
    for (const NYT::TNode& regionNode : regionNodes) {
        regions.push_back(BldRecognitionRegion::fromYTNode(regionNode));
    }

    const std::set<std::string> usedRegionNames{regions[2].name()};

    removeUsedRegions(regions, usedRegionNames);

    EXPECT_EQ(regions.size(), 2u);
    EXPECT_EQ(regions[0].name(), "name1");
    EXPECT_EQ(regions[1].name(), "name2");
}

} // Y_UNIT_TEST_SUITE(region_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline
