#include <maps/automotive/libs/test_helpers/helpers.h>
#include <maps/automotive/updater/test_helpers/mocks.h>

namespace maps::automotive::updater {

using namespace ::testing;

TEST_F(Fixture, scheduleSession)
{
    EXPECT_CALL(
        drive(),
        scheduleServiceSession("123", _, _));
    ASSERT_EQ(204, mockPost("/drive/schedule?headid=123").status);
}

TEST_F(Fixture, cancelSession)
{
    EXPECT_CALL(drive(), cancelServiceSession("234"));
    ASSERT_EQ(204, mockDelete("/drive/schedule?headid=234").status);
}

}
