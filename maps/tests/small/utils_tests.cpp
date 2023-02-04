#include <maps/indoor/libs/radiomap_metrics/utils.h>

#include <library/cpp/testing/unittest/registar.h>
#include <maps/libs/enum_io/include/enum_io.h>


namespace maps::mirc::radiomap_metrics::tests {

namespace {

constexpr char ONE[] = "one";
constexpr char TWO[] = "two";
constexpr char THREE[] = "three";

enum class TestEnum {
    One,
    Two,
    Three,
};
constexpr maps::enum_io::Representations<TestEnum> TEST_ENUM_REPRESENTATIONS {
    {TestEnum::One, ONE},
    {TestEnum::Two, TWO},
    {TestEnum::Three, THREE},

    // legacy names
    {TestEnum::One, "1"},
    {TestEnum::Two, "2"},
    {TestEnum::Three, "3"},
};

DEFINE_ENUM_IO(TestEnum, TEST_ENUM_REPRESENTATIONS);

} // namespace

Y_UNIT_TEST_SUITE(radiomap_metrics_utils)
{

Y_UNIT_TEST(enum_strings_size)
{
    const auto values = enum_io::enumerateValues<TestEnum>();
    const auto strs = getAllStringValues<TestEnum>();

    UNIT_ASSERT_EQUAL(values.size(), strs.size());
}

Y_UNIT_TEST(enum_strings)
{
    const auto strs = getAllStringValues<TestEnum>();

    UNIT_ASSERT_EQUAL(strs[0], ONE);
    UNIT_ASSERT_EQUAL(strs[1], TWO);
    UNIT_ASSERT_EQUAL(strs[2], THREE);
}

}

} // namespace maps::mirc::radiomap_metrics::tests
