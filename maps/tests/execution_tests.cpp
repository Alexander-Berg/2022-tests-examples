#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

#include <maps/wikimap/mapspro/services/autocart/libs/geometry/include/hex_wkb.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/include/execution.h>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

Y_UNIT_TEST_SUITE(execution_tests)
{

Y_UNIT_TEST(remove_uptodate_regions_test)
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

    const Release lastRelease{42, 42};
    const std::unordered_map<TString, ProcessedIssue> regionToIssue{
        {
            "name1",
            {
                41, // issue id
                chrono::TimePoint::clock::now() - std::chrono::hours(3 * 24 /* 3 days */)
            }
        },
        {
            "name2",
            {
                42, // issue id
                chrono::TimePoint::clock::now() - std::chrono::hours(3 * 24 /* 3 days */)
            }
        },
        {
            "name3",
            {
                40, // issue id
                chrono::TimePoint::clock::now() - std::chrono::hours(24 /* 1 days */)
            }
        },
    };
    const chrono::TimePoint dumpDate = chrono::TimePoint::clock::now() - std::chrono::hours(2);

    removeUptodateRegions(regions, lastRelease, regionToIssue, dumpDate);

    EXPECT_EQ(regions.size(), 1u);
    EXPECT_EQ(regions[0].name(), "name1");
}

} // Y_UNIT_TEST_SUITE(execution_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline
