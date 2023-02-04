#include <library/cpp/testing/unittest/registar.h>

#include <maps/wikimap/gpstiles_realtime/libs/index/include/rotated_index.h>

#include "../helpers.h"

namespace maps {
namespace gpstiles_realtime {
namespace index {
namespace test {

using TestRotatedIndex = RotatedIndex<TestPoint, geolib3::Point2>;

Y_UNIT_TEST_SUITE(rotated_index_tests)
{

Y_UNIT_TEST(test_construction)
{
    TestRotatedIndex(
        std::chrono::minutes(1),
        std::chrono::seconds(2),
        g_testBuilder);
}

} // test suite end

} // namespace test
} // namespace index
} // namespace gpstiles_realtime
} // namespace maps
