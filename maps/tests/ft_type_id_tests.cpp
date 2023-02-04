#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/ft_type_id.h>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

Y_UNIT_TEST_SUITE(ft_type_id_tests)
{

Y_UNIT_TEST(compatibility_test)
{
    EXPECT_EQ(compatibilityFTTypeId(FTTypeId::URBAN_GOV),
              FTTypeIdCompatibility::ALL);
    EXPECT_EQ(compatibilityFTTypeId(FTTypeId::URBAN_LEISURE),
              FTTypeIdCompatibility::BLD);
    EXPECT_TRUE(isCompatibleWithBld(FTTypeId::URBAN_GOV));
    EXPECT_TRUE(isCompatibleWithBld(FTTypeId::URBAN_LEISURE));
    EXPECT_TRUE(isCompatibleWithArea(FTTypeId::URBAN_GOV));
    EXPECT_FALSE(isCompatibleWithArea(FTTypeId::URBAN_LEISURE));
}

} // Y_UNIT_TEST_SUITE(ft_type_id_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline
