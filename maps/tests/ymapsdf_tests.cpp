#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <mapreduce/yt/interface/client.h>
#include <mapreduce/yt/util/ypath_join.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/include/ymapsdf.h>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

Y_UNIT_TEST_SUITE(ymasdf_tests)
{

Y_UNIT_TEST(basic_test)
{
    NYT::IClientPtr client = NYT::NTesting::CreateTestClient();
    client->Create(
        NYT::JoinYPaths(LATEST_YMAPSDF_PATH, "cis1"), NYT::NT_TABLE,
        NYT::TCreateOptions()
            .Recursive(true)
            .IgnoreExisting(false)
    );
    client->Create(
        NYT::JoinYPaths(LATEST_YMAPSDF_PATH, "cis2"), NYT::NT_TABLE,
        NYT::TCreateOptions()
            .Recursive(true)
            .IgnoreExisting(false)
    );

    chrono::TimePoint now = chrono::TimePoint::clock::now();
    chrono::TimePoint date = YMapsDFYTTable::getDate(client);
    EXPECT_TRUE(
        std::chrono::duration_cast<std::chrono::minutes>(now - date).count() < 2
    );

    const std::vector<TString> gtRoadsTablePaths{
        "//home/maps/core/garden/stable/ymapsdf/latest/cis1/rd_el",
        "//home/maps/core/garden/stable/ymapsdf/latest/cis2/rd_el"
    };

    const std::vector<TString> gtBldsTablePaths{
        "//home/maps/core/garden/stable/ymapsdf/latest/cis1/bld_geom",
        "//home/maps/core/garden/stable/ymapsdf/latest/cis2/bld_geom"
    };

    const std::vector<TString> gtFTTablePaths{
        "//home/maps/core/garden/stable/ymapsdf/latest/cis1/ft",
        "//home/maps/core/garden/stable/ymapsdf/latest/cis2/ft"
    };

    const std::vector<TString> gtFTGeomTablePaths{
        "//home/maps/core/garden/stable/ymapsdf/latest/cis1/ft_geom",
        "//home/maps/core/garden/stable/ymapsdf/latest/cis2/ft_geom"
    };

    std::vector<TString> testRoadsTablePaths
        = YMapsDFYTTable::getRoadsYTTablePaths(client);

    EXPECT_TRUE(
        std::is_permutation(
            gtRoadsTablePaths.begin(), gtRoadsTablePaths.end(),
            testRoadsTablePaths.begin()
        )
    );


    std::vector<TString> testBldsTablePaths
        = YMapsDFYTTable::getBuildingsYTTablePaths(client);

    EXPECT_TRUE(
        std::is_permutation(
            gtBldsTablePaths.begin(), gtBldsTablePaths.end(),
            testBldsTablePaths.begin()
        )
    );


    std::vector<TString> testFTTablePaths
        = YMapsDFYTTable::getFTYTTablePaths(client);

    EXPECT_TRUE(
        std::is_permutation(
            gtFTTablePaths.begin(), gtFTTablePaths.end(),
            testFTTablePaths.begin()
        )
    );


    std::vector<TString> testFTGeomTablePaths
        = YMapsDFYTTable::getFTGeomYTTablePaths(client);

    EXPECT_TRUE(
        std::is_permutation(
            gtFTGeomTablePaths.begin(), gtFTGeomTablePaths.end(),
            testFTGeomTablePaths.begin()
        )
    );
}

} // Y_UNIT_TEST_SUITE(ymapsdf_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline
