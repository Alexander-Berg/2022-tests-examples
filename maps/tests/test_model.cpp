#include <library/cpp/testing/unittest/registar.h>
#include <maps/analyzer/libs/realtime_jams/include/model.h>
#include <util/generic/yexception.h>

namespace maps {
namespace analyzer {
namespace realtime_jams {

Y_UNIT_TEST_SUITE(TestModel)
{
    Y_UNIT_TEST(TestMakeModel)
    {
        UNIT_CHECK_GENERATED_EXCEPTION(
            makeModel("/some/path/to/some/file/that/does/not/exist"),
            yexception
        );
    }
};

} // realtime_jams
} // analyzer
} // maps
