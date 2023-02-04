#include <library/cpp/testing/unittest/registar.h>
#include <maps/analyzer/libs/realtime_jams/include/target.h>

#include <type_traits>

namespace maps {
namespace analyzer {
namespace realtime_jams {

Y_UNIT_TEST_SUITE(TestTarget)
{
    Y_UNIT_TEST(TestTypes)
    {
        UNIT_ASSERT(std::is_floating_point<Target>::value);
    }
};

} // realtime_jams
} // analyzer
} // maps
