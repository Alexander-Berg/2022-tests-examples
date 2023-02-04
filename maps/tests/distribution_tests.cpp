#include <maps/wikimap/mapspro/libs/flat_range/include/distribution.h>

#include <library/cpp/testing/unittest/registar.h>
#include <yandex/maps/wiki/common/string_utils.h>

#include <sstream>

template <>
void Out<maps::wiki::flat_range::DistributionResult>(
    IOutputStream& os,
    const maps::wiki::flat_range::DistributionResult& res)
{
    os << res.isComplete
       << maps::wiki::common::join(
            res.levelsWithFlats,
            [](const maps::wiki::flat_range::LevelWithFlats& lwf) {
                return
                    lwf.levelName +
                    ":" +
                    maps::wiki::common::join(
                        lwf.flatRanges,
                        [](const maps::wiki::flat_range::Range& range) {
                            return range.first() + "-" + range.last();
                        },
                        ",");
            },
            " ");
}

namespace maps::wiki::flat_range::tests {

Y_UNIT_TEST_SUITE(distribution_tests)
{

Y_UNIT_TEST(normal_distribution)
{
    DistributionResult expectedResult = {{
        {"5", {Range("17", "20")}},
        {"4", {Range("13", "16")}},
        {"3", {Range("9", "12")}},
        {"2", {Range("5", "8")}},
        {"1", {Range("1", "4")}}
    }, true};
    UNIT_ASSERT_VALUES_EQUAL(
        distributeFlatsByLevels({{"1-20", "1-5"}}),
        expectedResult);
}

Y_UNIT_TEST(excluded_flats)
{
    DistributionResult expectedResult = {{
        {"5", {Range("13", "16")}},
        {"4", {Range("9", "12")}},
        {"3", {Range("5", "8")}},
        {"2", {Range("2", "4")}}
    }, true};
    UNIT_ASSERT_VALUES_EQUAL(
        distributeFlatsByLevels({{"2-16", "2-5"}}),
        expectedResult);
}

Y_UNIT_TEST(multiple_ranges)
{
    DistributionResult expectedResult = {{
        {"5", {Range("17", "20")}},
        {"4", {Range("13", "16")}},
        {"3", {Range("9", "12")}},
        {"2", {Range("5", "8")}},
        {"1", {Range("1", "4")}}
    }, true};
    UNIT_ASSERT_VALUES_EQUAL(
        distributeFlatsByLevels({{"1-8", "1-2"}, {"9-20", "3-5"}}),
        expectedResult);
}

Y_UNIT_TEST(mixed_ranges)
{
    DistributionResult expectedResult = {{
        {"5", {Range("17", "20"), Range("12А"), Range("12Б")}},
        {"4", {Range("13", "16")}},
        {"3", {Range("9", "12")}},
        {"2", {Range("5", "8")}},
        {"1", {Range("1", "4")}}
    }, true};
    UNIT_ASSERT_VALUES_EQUAL(
        distributeFlatsByLevels({{"1-20", "1-5"}, {"12А,12Б", "5"}}),
        expectedResult);
}

Y_UNIT_TEST(letter_level)
{
    DistributionResult expectedResult = {{
        {"А", {Range("2", "4")}}
    }, true};
    UNIT_ASSERT_VALUES_EQUAL(
        distributeFlatsByLevels({{"2-4", "А"}}),
        expectedResult);
}

Y_UNIT_TEST(letter_prefix_suffix)
{
    DistributionResult expectedResult = {{
        {"G4", {Range("13Г", "16Г")}},
        {"G3", {Range("9Г", "12Г")}},
        {"G2", {Range("5Г", "8Г")}},
        {"G1", {Range("1Г", "4Г")}}
    }, true};
    UNIT_ASSERT_VALUES_EQUAL(
        distributeFlatsByLevels({{"1Г-16Г", "G1-G4"}}),
        expectedResult);
}

Y_UNIT_TEST(number_prefix_suffix)
{
    DistributionResult expectedResult = {{
        {"4.12", {Range("1.13", "1.16")}},
        {"3.12", {Range("1.9", "1.12")}},
        {"2.12", {Range("1.5", "1.8")}},
        {"1.12", {Range("1.1", "1.4")}}
    }, true};
    UNIT_ASSERT_VALUES_EQUAL(
        distributeFlatsByLevels({{"1.1-1.16", "1.12-4.12"}}),
        expectedResult);
}

Y_UNIT_TEST(multi_level_flat)
{
    DistributionResult expectedResult = {{
        {"5", {Range("20А")}}
    }, true};
    UNIT_ASSERT_VALUES_EQUAL(
        distributeFlatsByLevels({{"20А", "1-5"}}),
        expectedResult);
}

Y_UNIT_TEST(missed_levels)
{
    DistributionResult expectedResult = {{}, false};
    UNIT_ASSERT_VALUES_EQUAL(
        distributeFlatsByLevels({{"1-10", ""}}),
        expectedResult);
}

Y_UNIT_TEST(ambiguous_multiple_ranges)
{
    DistributionResult expectedResult = {{}, false};
    UNIT_ASSERT_VALUES_EQUAL(
        distributeFlatsByLevels({{"1-8,9-21", "1-5"}}),
        expectedResult);
}

Y_UNIT_TEST(partial_distribution)
{
    DistributionResult expectedResult = {{
        {"4", {Range("10", "12")}},
        {"3", {Range("7", "9")}},
        {"2", {Range("4", "6")}},
        {"1", {Range("1", "3")}}
    }, false};
    UNIT_ASSERT_VALUES_EQUAL(
        distributeFlatsByLevels({{"1-12", "1-4"}, {"13-18", ""}}),
        expectedResult);
}

Y_UNIT_TEST(too_many_flats)
{
    DistributionResult expectedResult = {{}, false};
    UNIT_ASSERT_VALUES_EQUAL(
        distributeFlatsByLevels({{"1-10000", "1-10"}}),
        expectedResult);
}

} // Y_UNIT_TEST_SUITE(distribution_tests)

} // namespace maps::wiki::flat_range::tests
