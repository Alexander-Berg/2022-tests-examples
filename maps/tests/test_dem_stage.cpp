#include <maps/factory/libs/common/dem_stage.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::factory::tests {

using namespace maps::factory::literals;

Y_UNIT_TEST_SUITE(test_dem_stage) {

Y_UNIT_TEST(test_format_name)
{
    DemStage southWest{1_w, 2_s};
    DemStage northEast{3_e, 4_n};

    EXPECT_EQ(
        southWest.formatNameInJson(),
        "[-1,-2]"
    );
    EXPECT_EQ(
        northEast.formatNameInJson(),
        "[3,4]"
    );
    EXPECT_EQ(
        southWest.formatNameWithZeroes(),
        "w001_s002"
    );
    EXPECT_EQ(
        northEast.formatNameWithZeroes(),
        "e003_n004"
    );
}

Y_UNIT_TEST(test_missing_dem_stages)
{
    DemStage southWestCorner(180_w, 90_s);
    DemStage northEastCorner(180_e, 89_n);
    DemStage moscow(37_e, 55_n);
    DemStage murmansk(33_e, 68_n);

    EXPECT_TRUE(southWestCorner.isMissingIn<DemType::Srtm>());
    EXPECT_TRUE(northEastCorner.isMissingIn<DemType::Srtm>());
    EXPECT_FALSE(moscow.isMissingIn<DemType::Srtm>());
    EXPECT_TRUE(murmansk.isMissingIn<DemType::Srtm>());

    EXPECT_TRUE(southWestCorner.isMissingIn<DemType::Aster>());
    EXPECT_TRUE(northEastCorner.isMissingIn<DemType::Aster>());
    EXPECT_FALSE(moscow.isMissingIn<DemType::Aster>());
    EXPECT_FALSE(murmansk.isMissingIn<DemType::Aster>());
}

} //Y_UNIT_TEST_SUITE

} //namespace maps::factory::tests
