#include <maps/factory/libs/tileindex/tools/lib/lua_tile_filter.h>

#include <maps/factory/libs/tileindex/tests/testing_common.h>

using namespace testing;

namespace maps {
namespace tileindex {
namespace impl {
namespace tests {

Y_UNIT_TEST_SUITE(LuaTileFilter_Should)
{
Y_UNIT_TEST(call_function_returing_constant)
{
    EXPECT_THAT(LuaTileFilter("true").isPassed(0), IsTrue());
    EXPECT_THAT(LuaTileFilter("false").isPassed(0), IsFalse());
    EXPECT_THAT(LuaTileFilter("1 == 1").isPassed(0), IsTrue());
    EXPECT_THAT(LuaTileFilter("1 == 2").isPassed(0), IsFalse());
}

Y_UNIT_TEST(get_tile_parts)
{
    const auto packed = packTile(IssuedTile{Tile{5, 4, 7}, 42});
    const auto check = [&](const std::string& str) {
        return LuaTileFilter{str}.isPassed(packed);
    };
    EXPECT_THAT(check("x() == 5"), IsTrue());
    EXPECT_THAT(check("x() == 4"), IsFalse());
    EXPECT_THAT(check("y() == 5"), IsFalse());
    EXPECT_THAT(check("y() == 4"), IsTrue());
    EXPECT_THAT(check("zoom() == 7"), IsTrue());
    EXPECT_THAT(check("zoom() == 8"), IsFalse());
    EXPECT_THAT(check("issue() == 42"), IsTrue());
    EXPECT_THAT(check("issue() == 0"), IsFalse());
}

Y_UNIT_TEST(call_user_function)
{
    uint64_t in1{}, in2{};
    LuaTileFilter::Predicates fns{
        {"foo",
            [&](uint64_t p) {
                in1 = p;
                return true;
            }},
        {"bar",
            [&](uint64_t p) {
                in2 = p;
                return false;
            }},
    };
    EXPECT_THAT(LuaTileFilter("foo()", fns).isPassed(123u), IsTrue());
    EXPECT_THAT(in1, Eq(123u));
    EXPECT_THAT(LuaTileFilter("bar()", fns).isPassed(456u), IsFalse());
    EXPECT_THAT(in2, Eq(456u));
    EXPECT_THAT(LuaTileFilter("foo() and bar()", fns).isPassed(678u),
        IsFalse());
    EXPECT_THAT(in1, Eq(678u));
    EXPECT_THAT(in2, Eq(678u));
}
}

} // namespace tests
} // namespace impl
} // namespace tileindex
} // namespace maps
