#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/zigzag/include/zigzag.h>

using namespace yandex::maps;

Y_UNIT_TEST_SUITE(ZigZag) {
    Y_UNIT_TEST(Test) {
        for (int64_t i = -1000000; i <= 1000000; ++i) {
            UNIT_ASSERT_EQUAL(i, zigzagDecode(zigzagEncode(i)));
        }
    }
};
