#include <yandex/maps/wiki/common/batch.h>

#include <library/cpp/testing/unittest/registar.h>

#include <set>
#include <vector>

namespace maps::wiki::common::tests {

Y_UNIT_TEST_SUITE(other) {

Y_UNIT_TEST(test_reserve)
{
    UNIT_ASSERT(HasReserve<std::vector<int>>::value);
    UNIT_ASSERT(!HasReserve<std::set<int>>::value);

    const size_t DESIRED_CAPACITY = 10;

    using Container = std::vector<int>;
    Container container;

    if constexpr (HasReserve<Container>::value) {
        container.reserve(DESIRED_CAPACITY);
    }

    UNIT_ASSERT_VALUES_EQUAL(container.capacity(), DESIRED_CAPACITY);
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::common::tests
