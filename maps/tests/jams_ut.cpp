#include <yandex/maps/jams/common/jams.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::jams::common::tests {

Y_UNIT_TEST_SUITE(jams_tests)
{
    Y_UNIT_TEST(cats)
    {
        size_t sum = 0;
        for (size_t cat: CATS) {
            sum += cat;
        }
        EXPECT_EQ(sum, 55u);
        EXPECT_EQ(CATS.size(), CATS_COUNT);
    }
} // Y_UNIT_TEST_SUITE

} // maps::jams::common::tests
