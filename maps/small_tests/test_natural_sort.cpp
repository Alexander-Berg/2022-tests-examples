#include <yandex/maps/wiki/common/natural_sort.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::common::tests {

Y_UNIT_TEST_SUITE(natural_sort) {

Y_UNIT_TEST(test_less)
{
    natural_sort lesser;
    UNIT_ASSERT(!lesser("", ""));
    UNIT_ASSERT(lesser("", "b"));
    UNIT_ASSERT(lesser("aa", "bb"));
    UNIT_ASSERT(lesser("a", "b"));
    UNIT_ASSERT(lesser("a", "c"));
    UNIT_ASSERT(!lesser("c", "a"));
    UNIT_ASSERT(!lesser("b", "a"));
    UNIT_ASSERT(!lesser("10", "10"));
    UNIT_ASSERT(lesser("10", "11"));
    UNIT_ASSERT(!lesser("11", "10"));
    UNIT_ASSERT(!lesser("011", "10"));
    UNIT_ASSERT(lesser("011", "0012"));
    UNIT_ASSERT(lesser("a011", "a0012"));
    UNIT_ASSERT(lesser("011", "a0010"));
    UNIT_ASSERT(lesser("1a", "1b"));
    UNIT_ASSERT(lesser("1a", "2"));
    UNIT_ASSERT(lesser("z2", "z11"));
    UNIT_ASSERT(lesser("z2", "z11"));
    UNIT_ASSERT(lesser("a", "B"));
    UNIT_ASSERT(lesser("A", "b"));
    UNIT_ASSERT(lesser("A", "B"));
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::common::tests
