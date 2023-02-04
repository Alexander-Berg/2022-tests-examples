#include <maps/wikimap/mapspro/libs/common/include/yandex/maps/wiki/common/attr_value.h>

#include <maps/libs/enum_io/include/enum_io.h>

#include <library/cpp/testing/unittest/registar.h>

#include <string>
#include <string_view>

namespace maps::wiki::common::tests {

Y_UNIT_TEST_SUITE(attr_value) {

Y_UNIT_TEST(simple_cast)
{
    UNIT_ASSERT_VALUES_EQUAL(AttrValue("test", "test").as<std::string>(), "test");

    UNIT_ASSERT_VALUES_EQUAL(AttrValue("test", "-1").as<int>(), -1);
    UNIT_ASSERT_VALUES_EQUAL(static_cast<int>(AttrValue("test", "-1")), -1);
    UNIT_ASSERT_VALUES_EQUAL(AttrValue("test", "").as<int>(0), 0);

    UNIT_ASSERT_VALUES_EQUAL(static_cast<bool>(AttrValue("test", "1")), true);
    UNIT_ASSERT_VALUES_EQUAL(static_cast<bool>(AttrValue("test", "")), false);
}

Y_UNIT_TEST(operators)
{
    UNIT_ASSERT(AttrValue("test", "test") == AttrValue("test", "test"));
    UNIT_ASSERT(AttrValue("test", "one") != AttrValue("test", "two"));

    UNIT_ASSERT(AttrValue("test", "1") == 1);
    UNIT_ASSERT(AttrValue("test", "-1") != 1);

    UNIT_ASSERT(1 == AttrValue("test", "1"));
    UNIT_ASSERT(1 != AttrValue("test", "-1"));

    UNIT_ASSERT(AttrValue("test", "1") == "1");
    UNIT_ASSERT(AttrValue("test", "-1") != "1");

    UNIT_ASSERT("1" == AttrValue("test", "1"));
    UNIT_ASSERT("1" != AttrValue("test", "-1"));

    const std::string_view string_view_one = "1";

    UNIT_ASSERT(AttrValue("test", "1") == string_view_one);
    UNIT_ASSERT(AttrValue("test", "-1") != string_view_one);

    UNIT_ASSERT(string_view_one == AttrValue("test", "1"));
    UNIT_ASSERT(string_view_one != AttrValue("test", "-1"));

    const std::string string_one = "1";

    UNIT_ASSERT(AttrValue("test", "1") == string_one);
    UNIT_ASSERT(AttrValue("test", "-1") != string_one);

    UNIT_ASSERT(string_one == AttrValue("test", "1"));
    UNIT_ASSERT(string_one != AttrValue("test", "-1"));

    UNIT_ASSERT_VALUES_EQUAL(!AttrValue("test", "1"), false);
    UNIT_ASSERT_VALUES_EQUAL(!AttrValue("test", ""), true);
}

enum  class Number { One = 1, Two = 2 };

const enum_io::Representations<Number> NUMBER_ENUM_REPRESENTATION {
    {Number::One, "one"},
    {Number::Two, "two"},
};

DEFINE_ENUM_IO(Number, NUMBER_ENUM_REPRESENTATION);

Y_UNIT_TEST(enum_cast)
{
    UNIT_ASSERT_VALUES_EQUAL(AttrValue("test", "").as<Number>(Number::One), Number::One);
    UNIT_ASSERT_VALUES_EQUAL(AttrValue("test", "2").as<Number>(), Number::Two);
}

Y_UNIT_TEST(rd_direciton_cast)
{
    using Direction = ymapsdf::rd::Direction;

    UNIT_ASSERT_VALUES_EQUAL(AttrValue("test", "F").as<Direction>(), Direction::Forward);
    UNIT_ASSERT_VALUES_EQUAL(AttrValue("test", "T").as<Direction>(), Direction::Backward);
    UNIT_ASSERT_VALUES_EQUAL(AttrValue("test", "B").as<Direction>(), Direction::Both);
}

static const AttrsWrap::StringMap ATTRIBUTES {
    {"one", "1|2|3"},
    {"two", "value"},
};

Y_UNIT_TEST(wrap_construction)
{
    AttrsWrap wrap(ATTRIBUTES);

    UNIT_ASSERT(!wrap["none"]);
    UNIT_ASSERT(wrap["one"]);
    UNIT_ASSERT(wrap["two"]);
}

Y_UNIT_TEST(range)
{
    const AttrsWrap wrap(ATTRIBUTES);

    UNIT_ASSERT_VALUES_EQUAL(wrap.range("one").size(), 3u);
    UNIT_ASSERT_VALUES_EQUAL(wrap.range("one").front().as<int>(), 1);
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::common::tests
