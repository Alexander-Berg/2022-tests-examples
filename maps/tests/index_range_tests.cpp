#include <maps/factory/libs/tileindex/tools/lib/index_range.h>

#include <maps/factory/libs/tileindex/tests/testing_common.h>

using namespace testing;

namespace maps {
namespace tileindex {
namespace impl {
namespace tests {

Y_UNIT_TEST_SUITE(IndexRange_Should)
{
Y_UNIT_TEST(normalize_empty)
{
    EXPECT_THAT(IndexRange(10, 10), Eq(IndexRange{}));
    EXPECT_THAT(IndexRange(1, 1), Eq(IndexRange{}));
    EXPECT_THAT(IndexRange(), Eq(IndexRange{}));
}

Y_UNIT_TEST(parse_strings)
{
    const size_t size = 10;
    const auto check = [&](std::string str, size_t from, size_t to) {
        EXPECT_THAT(IndexRange::parse(str, size),
            Eq(IndexRange{from, to}))
                        << '"' << str << '"';
    };
    check("", 0, 0);
    check("0", 0, 1);
    check("1", 1, 2);
    check("0:1", 0, 1);
    check("0:4", 0, 4);
    check("3:4", 3, 4);
    check("4:4", 4, 4);
    check(":4", 0, 4);
    check("4:", 4, 10);
    check(":", 0, 10);
    check("0:10", 0, 10);
    check("10:10", 10, 10);
    check("-0", 0, 1);
    check("-1", 9, 10);
    check("-4", 6, 7);
    check("-4:", 6, 10);
    check(":-4", 0, 6);
    check("-7:-4", 3, 6);
}

Y_UNIT_TEST(throw_when_string_is_ill_formed)
{
    EXPECT_ANY_THROW(IndexRange::parse("1:2", 0));
    EXPECT_ANY_THROW(IndexRange::parse("1:2", 1));
    EXPECT_ANY_THROW(IndexRange::parse("1:12", 10));
    EXPECT_ANY_THROW(IndexRange::parse("12", 10));
    EXPECT_ANY_THROW(IndexRange::parse("-12", 10));
    EXPECT_ANY_THROW(IndexRange::parse("11:", 10));
    EXPECT_ANY_THROW(IndexRange::parse(":11", 10));
    EXPECT_ANY_THROW(IndexRange::parse("-11:", 10));
    EXPECT_ANY_THROW(IndexRange::parse(":-11", 10));
    EXPECT_ANY_THROW(IndexRange::parse("foo", 10));
    EXPECT_ANY_THROW(IndexRange::parse("foo:10", 10));
    EXPECT_ANY_THROW(IndexRange::parse("1;2", 10));
    EXPECT_ANY_THROW(IndexRange::parse("1, 2", 10));
    EXPECT_ANY_THROW(IndexRange::parse("1:2 foo", 10));
}
}

} // namespace tests
} // namespace impl
} // namespace tileindex
} // namespace maps
