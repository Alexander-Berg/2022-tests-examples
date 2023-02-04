#include <maps/wikimap/mapspro/libs/flat_range/include/range.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::flat_range::tests {

Y_UNIT_TEST_SUITE(range_tests)
{

Y_UNIT_TEST(distribution_oom_guard)
{
    UNIT_ASSERT_EXCEPTION(
        parse("1-60000"),
        ParseException);
}

Y_UNIT_TEST(parse)
{
    UNIT_ASSERT_EQUAL(parse(" 1 ").size(), 1);
    UNIT_ASSERT_EQUAL(parse(" 1, 1").size(), 2);
    UNIT_ASSERT_EQUAL(parse(" 1, 1-1").size(), 2);
    UNIT_ASSERT_EQUAL(parse("\"1-2\"").size(), 1);
    UNIT_ASSERT_EQUAL(parse(" 1-3, 5-9").size(), 2);
    UNIT_ASSERT_EQUAL(parse(" -1-3, 5--9").size(), 2);
    UNIT_ASSERT_EQUAL(parse(" 1a-3a, 7.5-7.9").size(), 2);
    UNIT_ASSERT_EQUAL(parse(" 1a-3a, 7.5-7.-9").size(), 2);
    UNIT_ASSERT_EXCEPTION(parse("1-"), ParseException);
    UNIT_ASSERT_EXCEPTION(parse("a-c"), ParseException);
    UNIT_ASSERT_EXCEPTION(parse("1a1-2b2"), ParseException);
    UNIT_ASSERT_EXCEPTION(parse("a1a-b2b"), ParseException);
}

Y_UNIT_TEST(parse_quoted)
{
    auto parsed = parse("\"1 2\", 100, \"5,   8\"");
    UNIT_ASSERT_VALUES_EQUAL(parsed.size(), 3);
    auto last = parsed.back();
    UNIT_ASSERT_VALUES_EQUAL(last.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(last.first(), "\"5,   8\"");
}

Y_UNIT_TEST(iterate)
{
    auto ranges = parse(" Ж1-Ж3, 7.5-7.-9");
    UNIT_ASSERT_EQUAL(ranges.size(), 2);

    auto range13 = ranges[0];
    UNIT_ASSERT_EQUAL(range13.size(), 3);
    UNIT_ASSERT_EQUAL(range13.value(0), "Ж1");
    UNIT_ASSERT_EQUAL(range13.value(1), "Ж2");
    UNIT_ASSERT_EQUAL(range13.value(2), "Ж3");

    auto range5_9 = ranges[1];
    UNIT_ASSERT_EQUAL(range5_9.size(), 15);
    UNIT_ASSERT_VALUES_EQUAL(range5_9.value(0), "7.5");
    UNIT_ASSERT_VALUES_EQUAL(range5_9.value(4), "7.1");
    UNIT_ASSERT_VALUES_EQUAL(range5_9.value(14), "7.-9");

    auto ranges1G_12G = parse("1Г-12Г");
    UNIT_ASSERT_EQUAL(ranges1G_12G.size(), 1);
    UNIT_ASSERT_EQUAL(ranges1G_12G[0].size(), 12);
    UNIT_ASSERT_VALUES_EQUAL(ranges1G_12G[0].last(), "12Г");
    auto rangesG1_G12 = parse("Г1-Г12");
    UNIT_ASSERT_EQUAL(rangesG1_G12.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(rangesG1_G12[0].size(), 12);
    UNIT_ASSERT_VALUES_EQUAL(rangesG1_G12[0].value(10), "Г11");
    UNIT_ASSERT_VALUES_EQUAL(rangesG1_G12[0].last(), "Г12");

    auto rangesThroughZero = parse("-1-1");
    UNIT_ASSERT_EQUAL(rangesThroughZero.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(rangesThroughZero[0].value(1), "0");

    auto rangesDot = parse(" .5-.9, .0-.-9");
    UNIT_ASSERT_EQUAL(rangesDot.size(), 2);

    auto rangeDot5_9 = rangesDot[0];
    UNIT_ASSERT_EQUAL(rangeDot5_9.size(), 5);
    UNIT_ASSERT_EQUAL(rangeDot5_9.value(0), ".5");
    UNIT_ASSERT_EQUAL(rangeDot5_9.value(1), ".6");
    UNIT_ASSERT_EQUAL(rangeDot5_9.value(2), ".7");

    auto rangeDot0_9 = rangesDot[1];
    UNIT_ASSERT_EQUAL(rangeDot0_9.size(), 10);
    UNIT_ASSERT_VALUES_EQUAL(rangeDot0_9.first(), ".0");
    UNIT_ASSERT_VALUES_EQUAL(rangeDot0_9.last(), ".-9");

    auto rangesMinusAlpha = parse("1a--1a, a1-a-1");
    UNIT_ASSERT_EQUAL(rangesMinusAlpha.size(), 2);
    UNIT_ASSERT_VALUES_EQUAL(rangesMinusAlpha[0].first(), "1a");
    UNIT_ASSERT_VALUES_EQUAL(rangesMinusAlpha[0].value(1), "0a");
    UNIT_ASSERT_VALUES_EQUAL(rangesMinusAlpha[0].last(), "-1a");

    UNIT_ASSERT_VALUES_EQUAL(rangesMinusAlpha[1].first(), "a1");
    UNIT_ASSERT_VALUES_EQUAL(rangesMinusAlpha[1].value(1), "a0");
    UNIT_ASSERT_VALUES_EQUAL(rangesMinusAlpha[1].last(), "a-1");
}

Y_UNIT_TEST(min_max)
{
    UNIT_ASSERT_VALUES_EQUAL(parse("1-10")[0].min(), "1");
    UNIT_ASSERT_VALUES_EQUAL(parse("1-10")[0].max(), "10");

    UNIT_ASSERT_VALUES_EQUAL(parse("10-1")[0].min(), "1");
    UNIT_ASSERT_VALUES_EQUAL(parse("10-1")[0].max(), "10");

    UNIT_ASSERT_VALUES_EQUAL(parse("1.1-1.10")[0].min(), "1.1");
    UNIT_ASSERT_VALUES_EQUAL(parse("1.1-1.10")[0].max(), "1.10");

    UNIT_ASSERT_VALUES_EQUAL(parse("10.A-1.A")[0].min(), "1.A");
    UNIT_ASSERT_VALUES_EQUAL(parse("10.A-1.A")[0].max(), "10.A");
}

} // Y_UNIT_TEST_SUITE(range_tests)

} // namespace maps::wiki::flat_range::tests
