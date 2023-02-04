#include <maps/factory/libs/common/debug_mutex.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::tests {

Y_UNIT_TEST_SUITE(debug_metex_should) {

Y_UNIT_TEST(try_lock)
{
    DebugMutex<std::timed_mutex> m{"test", 1};
    std::lock_guard lock{m};
    EXPECT_FALSE(m.try_lock_for(std::chrono::nanoseconds(1)));
}

} // suite

Y_UNIT_TEST_SUITE(_should) {

Y_UNIT_TEST(_)
{
}

} // suite

} // namespace maps::factory::tests
