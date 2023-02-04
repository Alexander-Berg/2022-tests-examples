#include "maps/b2bgeo/libs/jwt/include/errors.h"
#include "maps/b2bgeo/libs/jwt/include/types.h"

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::b2bgeo::jwt {

TEST(TypesTest, EmptyStringsAreNotAllowed)
{
    using TestType = StringValue<struct TestTypeTag>;
    EXPECT_THROW(TestType(""), EmptyStringError);
}

} // namespace maps::b2bgeo::jwt
